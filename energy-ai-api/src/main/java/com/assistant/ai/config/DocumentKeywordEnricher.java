package com.assistant.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AI 的文档元信息增强器（为文档补充元信息）
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentKeywordEnricher {

    private final ChatModel dashscopeChatModel;

    public List<Document> enrichDocuments(List<Document> documents) {
        if (!enableEnrichedDocs()) {
            return documents;
        }
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashscopeChatModel, 5);
        return keywordMetadataEnricher.apply(documents);
    }

    public boolean enableEnrichedDocs() {
        return false;
    }
}
