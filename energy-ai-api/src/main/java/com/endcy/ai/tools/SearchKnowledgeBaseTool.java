package com.endcy.ai.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.endcy.ai.manager.KnowledgeDocumentManager;
import com.endcy.ai.repository.domain.context.DocumentQueryContext;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Search-as-a-tool — lets the agent autonomously decide when to search the knowledge base.
 *
 * <p>Added in Step 2.2, integrated into the tool list of {@code DefaultAgentExecutor.executeAgentic()}.</p>
 *
 * <p>When the model determines that a user question requires knowledge base retrieval
 * (rather than directly calling tools or replying with chitchat),
 * it invokes this tool via function calling, passing query (user question/keyword) and topK (expected result count).</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchKnowledgeBaseTool {

    private static final int DEFAULT_TOP_K = 3;
    private static final int MIN_TOP_K = 1;
    private static final int MAX_TOP_K = 10;

    private final KnowledgeDocumentManager knowledgeDocumentManager;

    @Tool(description = "Search the knowledge base for document content relevant to your question. Input a keyword or question, " +
            "returns the most relevant document snippets (with scores). " +
            "Applicable: when the user asks about specific business details, operational procedures, troubleshooting plans, etc. " +
            "Not applicable: simple greetings, off-topic chitchat, or factual questions with known answers.")
    public String searchKnowledgeBase(
            @ToolParam(description = "Query statement for retrieving the knowledge base (Chinese keywords or complete question)") String query,
            @ToolParam(description = "Expected number of documents to return (default 3, max 10)") Integer topK) {

        if (StrUtil.isBlank(query)) {
            return "Query is empty, please provide valid search keywords";
        }

        int k = topK != null ? Math.min(Math.max(MIN_TOP_K, topK), MAX_TOP_K) : DEFAULT_TOP_K;

        DocumentQueryContext ctx = new DocumentQueryContext();
        ctx.setOriginalQuestion(query);
        ctx.setReReadingQuestion(query);
        // Default search scope is "customer service"; can be extended as a parameter
        ctx.setScopeType("用户客服");

        List<KnowledgeDocumentMatchItem> matches;
        try {
            matches = knowledgeDocumentManager.match(ctx, k);
        } catch (Exception e) {
            log.error("Knowledge base search failed: query={}, topK={}", query, k, e);
            return "Knowledge base search failed, please try again later";
        }

        if (CollUtil.isEmpty(matches)) {
            return "No relevant documents found";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            KnowledgeDocumentMatchItem m = matches.get(i);
            sb.append("[Document ").append(i + 1).append("]");
            if (StrUtil.isNotBlank(m.getTitle())) {
                sb.append("Title: ").append(m.getTitle()).append("; ");
            }
            if (StrUtil.isNotBlank(m.getSource())) {
                sb.append("Source: ").append(m.getSource()).append("; ");
            }
            sb.append("Similarity: ").append(String.format("%.2f", m.getScore() != null ? m.getScore() : 0));
            sb.append("\n");
        }
        return sb.toString();
    }
}
