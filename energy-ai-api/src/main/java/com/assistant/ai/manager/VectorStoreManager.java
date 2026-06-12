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
import com.assistant.service.common.constant.GlobalConstant;
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
 * VectorStore 管理器
 * 负责向量库的刷新和增量更新
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
    //pg 向量知识库
    private final PgVectorStore pgVectorVectorStore;
    //本地文档知识库
    private final SimpleVectorStore localVectorStore;

    /**
     * 刷新本地文档向量库
     * 由于 localVectorStore 对应 SimpleVectorStore 每次重启都会清空向量数据，所以每次重启需要重新加载本地文档向量
     * 尽量不使用 SimpleVectorStore，文档数据较多时，本地服务启动时间、内存资源占用，和最关键的嵌入式向量模型的调用费用扛不住的
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
        //按每 25 个文档一组
        List<List<Document>> documentGroups = ListUtil.partition(enrichedDocs, 25);
        for (List<Document> documentGroup : documentGroups) {
            localVectorStore.add(documentGroup);
        }
        log.info("refreshLocalVectorStore successful");
    }

    /**
     * 刷新 pg 向量库
     * 采用分页查询和增量更新策略，避免内存溢出和提高效率
     */
    public void refreshDbKnowledgeDocument() {
        int page = 0;
        int pageSize = 100;
        int totalProcessed = 0;
        int totalInserted = 0;

        while (true) {
            // 分页查询未加载的文档
            List<KnowledgeDocumentDTO> unloadedDocs = knowledgeDocumentService.getUnloadedDocuments(page, pageSize);

            if (CollUtil.isEmpty(unloadedDocs)) {
                log.info("refreshDbKnowledgeDocument completed: totalProcessed={}, totalInserted={}", totalProcessed, totalInserted);
                break;
            }

            // 转换为 Spring AI 的 Document 对象
            List<Document> documents = CollUtil.newArrayList();
            unloadedDocs.forEach(item -> documents.add(convertToDocument(item)));

            if (CollUtil.isNotEmpty(documents)) {
                // 分块并增强
                List<Document> splitDocs = tokenTextSplitter.splitDocuments(documents);
                splitDocs.removeIf(item -> filterInvalidDocument(pgVectorVectorStore, item));

                if (CollUtil.isNotEmpty(splitDocs)) {
                    List<Document> enrichedDocs = keywordEnricher.enrichDocuments(splitDocs);

                    // 加载到向量库 1356 维度按每 25 个文档一组 1024 维度最大 10 个一组
                    List<List<Document>> documentGroups = ListUtil.partition(enrichedDocs, 10);
                    for (List<Document> documentGroup : documentGroups) {
                        pgVectorVectorStore.add(documentGroup);
                    }
                    totalInserted += splitDocs.size();
                }
            }

            // 更新加载状态
            List<Long> loadedIds = unloadedDocs.stream()
                                               .map(KnowledgeDocumentDTO::getId)
                                               .collect(Collectors.toList());
            knowledgeDocumentService.updateDocumentLoadedStatus(loadedIds, true);

            totalProcessed += unloadedDocs.size();
            page++;

            // 每处理 500 条记录输出一次日志
            if (totalProcessed % 500 == 0) {
                log.info("refreshDbKnowledgeDocument progress: processed={}, inserted={}", totalProcessed, totalInserted);
            }
        }
    }

    /**
     * 增量更新单个文档的向量
     *
     * @param docId 文档 ID
     */
    public void incrementUpdateDocumentVector(Long docId) {
        try {
            KnowledgeDocumentDTO doc = knowledgeDocumentService.getById(docId);
            if (doc == null) {
                log.warn("Document not found: docId={}", docId);
                return;
            }

            Document document = convertToDocument(doc);
            List<Document> splitDocs = tokenTextSplitter.splitDocuments(List.of(document));
            splitDocs.removeIf(item -> filterInvalidDocument(pgVectorVectorStore, item));

            if (CollUtil.isNotEmpty(splitDocs)) {
                List<Document> enrichedDocs = keywordEnricher.enrichDocuments(splitDocs);
                pgVectorVectorStore.add(enrichedDocs);
                knowledgeDocumentService.updateDocumentLoadedStatus(List.of(doc.getId()), true);
                log.info("Incremental update document vector success: docId={}", docId);
            }
        } catch (Exception e) {
            log.error("Incremental update document vector failed: docId={}", docId, e);
        }
    }

    /**
     * 从向量库移除文档
     *
     * @param docId 文档 ID
     */
    public void removeDocumentVector(Long docId) {
        try {
            vectorStoreService.removeByDocIds(CollUtil.newHashSet(docId));
            log.info("Remove document vector success: docId={}", docId);
        } catch (Exception e) {
            log.error("Remove document vector failed: docId={}", docId, e);
        }
    }

    /**
     * 过滤掉已经加载的到向量库的文档
     * 先使用 id 匹配
     * 相似度>0.9999 视为同文档
     * 最佳实践：线程池多线程处理文档列表，逐个匹配可能有点慢，考虑到文档量其实有限，可视需求优化
     *
     * @return .
     */
    private boolean filterInvalidDocument(VectorStore vectorStore, Document document) {
        if (StrUtil.isBlank(document.getText())) {
            return true;
        }
        try {
            Long id = (Long) document.getMetadata().get(GlobalConstant.DOC_ID_MARK);
            if (id != null && vectorStore instanceof PgVectorStore) {
                return vectorStoreService.isExistsInVector(id);
            } else if (id != null && vectorStore instanceof SimpleVectorStore) {
                List<Document> allLocalDocs = vectorStore.similaritySearch("");
                return CollUtil.isNotEmpty(allLocalDocs)
                        && allLocalDocs.stream().anyMatch(item -> id.equals(item.getMetadata().get(GlobalConstant.DOC_ID_MARK)));
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
     * 将 KnowledgeDocumentDTO 转换为 Document 对象
     */
    private Document convertToDocument(KnowledgeDocumentDTO doc) {
        //只赋需要的元数据字段，转 map 时忽略其他属性
        KnowledgeDocumentDTO targetDoc = BeanUtil.copyProperties(doc, KnowledgeDocumentDTO.class, KnowledgeDocumentDTO.RAG_META_IGNORE_FIELDS);
        Map<String, Object> metadata = BeanUtil.beanToMap(targetDoc, false, true);
        //务必将标题加入文档内容中 增加匹配准确性
        return new Document(doc.getTitle() + "。" + doc.getContent(), metadata);
    }

    @Override
    public void afterPropertiesSet() {
        refreshLocalDocument();
        refreshDbKnowledgeDocument();
    }
}
