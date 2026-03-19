package com.assistant.ai.domain.context;

import lombok.Data;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * 请求级 RAG 上下文
 *
 * @author endcy
 * @date 2025/12/16 20:11:17
 */
@Data
@Component
@RequestScope
public class RequestRagContext {
    private Long chatId;

    private List<Document> relatedDocuments;
}
