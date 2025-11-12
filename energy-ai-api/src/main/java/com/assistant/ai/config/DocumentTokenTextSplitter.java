package com.assistant.ai.config;

import com.assistant.ai.rag.ChineseEnhancedTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义基于 Token 的文档分片器
 *
 * @implNote 分片器直接影响语料的质量
 * SentenceSplitter 分片器有点拉胯，基于格式严格的英文的文档效果好点，中文文档参数基本没效
 * TokenTextSplitter 基于 Token 的分片器，指定token上下浮动分片，英文的文档效果好点，中文文档基本有效
 * ChineseEnhancedTextSplitter 改进TokenTextSplitter的分片器，中文支持好一点
 */
@Slf4j
@Component
public class DocumentTokenTextSplitter implements InitializingBean {
    private TextSplitter splitter;

    public List<Document> splitDocuments(List<Document> documents) {
        return splitter.apply(documents);
    }


    @Override
    public void afterPropertiesSet() {
        try {
            this.splitter = new ChineseEnhancedTextSplitter(false);
        } catch (Exception e) {
            log.error(">>>>>> SentenceSplitter init failed, use TokenTextSplitter splitter", e);
            this.splitter = new TokenTextSplitter(200, 100, 10, 2048, true);
        }
    }
}
