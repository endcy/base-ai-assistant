package com.endcy.ai.rag;

import cn.hutool.core.collection.CollUtil;
import com.endcy.ai.config.ChatRagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.*;

/**
 * Parent-Child retriever —— 使用 child chunks 检索，通过 parent_id 回填 parent chunks。
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
public class ParentChildRetriever implements DocumentRetriever {

    private final VectorStore vectorStore;
    private final ChatRagProperties chatRagProperties;

    public ParentChildRetriever(VectorStore vectorStore, ChatRagProperties chatRagProperties) {
        this.vectorStore = vectorStore;
        this.chatRagProperties = chatRagProperties;
    }

    @Override
    public List<Document> retrieve(Query query) {
        if (query == null || query.text() == null) {
            return Collections.emptyList();
        }

        int topK = chatRagProperties.getSimilarityTopK();
        double threshold = chatRagProperties.getSimilarityThreshold();

        SearchRequest.Builder builder = SearchRequest.builder()
                                                     .query(query.text())
                                                     .topK(topK)
                                                     .similarityThreshold(threshold);

        List<Document> childDocs;
        try {
            builder.filterExpression("chunk_level == 'CHILD'");
            childDocs = vectorStore.similaritySearch(builder.build());
        } catch (Exception e) {
            log.debug("ParentChild: chunk_level 过滤失败，降级普通检索: {}", e.getMessage());
            childDocs = vectorStore.similaritySearch(SearchRequest.builder()
                                                                  .query(query.text())
                                                                  .topK(topK)
                                                                  .similarityThreshold(threshold)
                                                                  .build());
        }

        if (CollUtil.isEmpty(childDocs)) {
            return Collections.emptyList();
        }

        Set<String> parentIds = new LinkedHashSet<>();
        for (Document doc : childDocs) {
            Object parentId = doc.getMetadata().get("parent_id");
            if (parentId != null) {
                parentIds.add(parentId.toString());
            }
        }

        if (parentIds.isEmpty()) {
            return childDocs;
        }

        List<Document> result = new ArrayList<>();
        for (String pid : parentIds) {
            try {
                SearchRequest pr = SearchRequest.builder()
                                                .query("")
                                                .topK(1)
                                                .filterExpression("id == '" + pid + "'")
                                                .build();
                List<Document> parents = vectorStore.similaritySearch(pr);
                result.addAll(parents);
            } catch (Exception e) {
                log.debug("ParentChild: parent 查询失败 pid={}: {}", pid, e.getMessage());
            }
        }

        Map<String, Document> unique = new LinkedHashMap<>();
        for (Document d : result) {
            unique.putIfAbsent(d.getId(), d);
        }

        log.info("ParentChild: {} child → {} parent", childDocs.size(), unique.size());
        return unique.isEmpty() ? childDocs : new ArrayList<>(unique.values());
    }
}
