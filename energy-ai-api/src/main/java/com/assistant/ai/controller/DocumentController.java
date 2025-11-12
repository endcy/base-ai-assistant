package com.assistant.ai.controller;

import com.assistant.ai.manager.VectorStoreManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/energy-ai")
public class DocumentController {

    private final VectorStoreManager vectorStoreManager;


    /**
     * 同步刷新书库文档内容到向量库
     */
    @GetMapping("/document/refresh")
    public String refreshDocumentVector() {
        vectorStoreManager.refreshDbKnowledgeDocument();
        return "success";
    }


}
