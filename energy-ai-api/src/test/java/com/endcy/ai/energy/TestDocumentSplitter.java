package com.endcy.ai.energy;

import com.alibaba.cloud.ai.transformer.splitter.SentenceSplitter;
import com.endcy.ai.rag.ChineseEnhancedTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.core.io.FileSystemResource;

import java.util.Collections;
import java.util.List;

/**
 * 文档分割器测试
 *
 * @author endcy
 * @date 2025/10/23 21:26:56
 */
@Slf4j
public class TestDocumentSplitter {

    @Test
    public void success_split_document() {
//        TextSplitter splitter = new SentenceSplitter(5);
//        TextSplitter splitter = new TokenTextSplitter(false);
        TextSplitter splitter = new ChineseEnhancedTextSplitter(false);
        // 使用相对路径指向测试资源目录下的示例文档
        String filePath = "src/main/resources/document-exp/设备和场站信息/深圳测试站点.md";
        FileSystemResource resource = new FileSystemResource(filePath);
        TextReader textReader = new TextReader(resource);
        List<Document> documents = textReader.get();
        List<Document> allDocuments = splitter.apply(documents);
        log.info("1 allDocuments: {}", allDocuments.size());
        for (Document document : allDocuments) {
            log.info("1-- document: {}", document.getText() != null ? document.getText().length() : 0);
        }
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                                                                          .withHorizontalRuleCreateDocument(true)
                                                                          .withIncludeCodeBlock(false)
                                                                          .withIncludeBlockquote(false)
                                                                          .withAdditionalMetadata("type", "test")
                                                                          .withAdditionalMetadata("filename", "深圳测试站点")
                                                                          .withAdditionalMetadata("status", "站点和设备信息")
                                                                          .build();
        MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
        documents = markdownDocumentReader.get();
        allDocuments = splitter.apply(documents);
        log.info("2 allDocuments: {}", allDocuments.size());
        for (Document document : allDocuments) {
            log.info("2-- document: {}", document.getText() != null ? document.getText().length() : 0);
        }

        String text = "This is a test. This is another test. And this is a third test.";
        Document doc = new Document(text);
        allDocuments = splitter.apply(Collections.singletonList(doc));
        log.info("3 allDocuments: {}", allDocuments.size());
        for (Document document : allDocuments) {
            log.info("3-- document: {}", document.getText() != null ? document.getText().length() : 0);
        }
    }

    @Test
    public void success_split_document2() {
        TextSplitter splitter = new SentenceSplitter(5);
        String text = "This is a test. This is another test. And this is a third test.";
        Document doc = new Document(text);
        List<Document> allDocuments = splitter.apply(Collections.singletonList(doc));
        log.info("1 allDocuments: {}", allDocuments.size());
        for (Document document : allDocuments) {
            log.info("1-- document: {}", document.getText() != null ? document.getText().length() : 0);
        }
    }

}
