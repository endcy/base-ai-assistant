package com.assistant.ai.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.config.DocumentKeywordEnricher;
import com.assistant.ai.config.DocumentTokenTextSplitter;
import com.assistant.ai.rag.AiDocumentFileLoader;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.ai.repository.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TODO
 * VectorStore的内容移除、添加等操作
 *
 * @author endcy
 * @date 2025/8/6 21:39:14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorStoreManager implements InitializingBean {

    private final AiDocumentFileLoader aiDocumentFileLoader;
    private final DocumentTokenTextSplitter tokenTextSplitter;
    private final DocumentKeywordEnricher keywordEnricher;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final ChatRagProperties chatRagProperties;
    private final VectorStoreService vectorStoreService;
    //pg向量知识库
    private final PgVectorStore pgVectorVectorStore;
    //本地文档知识库
    private final SimpleVectorStore localVectorStore;

    /**
     * 刷新本地文档向量库
     * 由于localVectorStore对应SimpleVectorStore 每次重启都会清空向量数据，所以每次重启需要重新加载本地文档向量
     * 尽量不使用SimpleVectorStore，文档数据较多时，本地服务启动时间、内存资源占用，和最关键的嵌入式向量模型的调用费用扛不住的
     */
    public void refreshLocalDocument() {
        if (!BooleanUtil.isTrue(chatRagProperties.getEnableLocalDocument())) {
            return;
        }
        // 加载本地文档
        List<Document> documentList = aiDocumentFileLoader.loadMarkdowns();
        // 自主切分文档
        List<Document> splitDocuments = tokenTextSplitter.splitDocuments(documentList);
        splitDocuments.removeIf(item -> filterInvalidDocument(localVectorStore, item));
        if (CollUtil.isEmpty(splitDocuments)) {
            return;
        }
        // 自动补充关键词元信息
        List<Document> enrichedDocs = keywordEnricher.enrichDocuments(splitDocuments);
        //按每25个文档一组
        List<List<Document>> documentGroups = ListUtil.partition(enrichedDocs, 25);
        for (List<Document> documentGroup : documentGroups) {
            localVectorStore.add(documentGroup);
        }
        log.info("refreshLocalVectorStore successful");
    }

    /**
     * 刷新pg向量库
     */
    public void refreshDbKnowledgeDocument() {
        //待优化为分页查询
        List<KnowledgeDocumentDTO> unloadedDocs = knowledgeDocumentService.getUnloadedDocuments();
        // 转换为Spring AI的Document对象
        List<Document> documents = CollUtil.newArrayList();
        unloadedDocs.forEach(item -> documents.add(convertToDocument(item)));
        if (CollUtil.isEmpty(documents)) {
            return;
        }
        // 分块并增强
        List<Document> splitDocs = tokenTextSplitter.splitDocuments(documents);
        splitDocs.removeIf(item -> filterInvalidDocument(pgVectorVectorStore, item));
        if (CollUtil.isEmpty(splitDocs)) {
            return;
        }
        List<Document> enrichedDocs = keywordEnricher.enrichDocuments(splitDocs);

        //加载到向量库 1356维度按每25个文档一组 1024维度最大10个一组
        List<List<Document>> documentGroups = ListUtil.partition(enrichedDocs, 10);
        for (List<Document> documentGroup : documentGroups) {
            pgVectorVectorStore.add(documentGroup);
        }

        // 更新加载状态
        List<Long> loadedIds = unloadedDocs.stream()
                                           .map(KnowledgeDocumentDTO::getId)
                                           .collect(Collectors.toList());
        knowledgeDocumentService.updateDocumentLoadedStatus(loadedIds, true);
        log.info("refreshDbKnowledgeDocument successful");
    }

    /**
     * 过滤掉已经加载的到向量库的文档
     * 先使用id匹配
     * 相似度>0.9999视为同文档
     * 最佳实践：线程池多线程处理文档列表，逐个匹配可能有点慢，考虑到文档量其实有限，可视需求优化
     *
     * @return .
     */
    private boolean filterInvalidDocument(VectorStore vectorStore, Document document) {
        if (StrUtil.isBlank(document.getText())) {
            return true;
        }
        try {
            Long id = (Long) document.getMetadata().get("id");
            if (id != null && vectorStore instanceof PgVectorStore) {
                return vectorStoreService.isExistsInVector(id);
            } else if (id != null && vectorStore instanceof SimpleVectorStore) {
                List<Document> allLocalDocs = vectorStore.similaritySearch("");
                return CollUtil.isNotEmpty(allLocalDocs)
                        && allLocalDocs.stream().anyMatch(item -> id.equals(item.getMetadata().get("id")));
            }
        } catch (Exception e) {
            log.warn("query id from document in vector store failed, metaData:{}", document.getMetadata());
        }
        // 文档内容相似度兜底查询
        SearchRequest searchRequest = SearchRequest.builder()
                                                   .query(document.getText())
                                                   .similarityThreshold(0.9999)
                                                   .topK(1)
                                                   .build();
        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
        return CollUtil.isNotEmpty(similarDocuments);
    }

    /**
     * 将KnowledgeDocumentDTO转换为Document对象
     */
    private Document convertToDocument(KnowledgeDocumentDTO doc) {
        //只赋需要的元数据字段，转map时忽略其他属性
        KnowledgeDocumentDTO targetDoc = BeanUtil.copyProperties(doc, KnowledgeDocumentDTO.class, KnowledgeDocumentDTO.RAG_META_IGNORE_FIELDS);
        Map<String, Object> metadata = BeanUtil.beanToMap(targetDoc, false, true);
        return new Document(doc.getContent(), metadata);
    }

    @Override
    public void afterPropertiesSet() {
        refreshLocalDocument();
        refreshDbKnowledgeDocument();
    }
}
