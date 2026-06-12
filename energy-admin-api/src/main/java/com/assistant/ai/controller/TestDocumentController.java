package com.assistant.ai.controller;

import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam;
import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.ai.rpc.enums.ApiResStatus;
import com.assistant.service.common.base.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 文档管理测试控制器
 * <p>
 * 直接调用 Repository 层服务进行文档管理操作（增删改查），
 * 用于开发和联调阶段验证文档管理功能，不走 RPC 链路。
 * </p>
 * <p>无前端界面，仅 REST API 调用（测试/调试用途）</p>
 *
 * @author endcy
 * @date 2026/6/10 20:44:50
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/document")
public class TestDocumentController {
    private final KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping("/modify")
    public CommonResMsgDTO<KnowledgeDocumentDTO> modify(@Validated @RequestBody KnowledgeDocumentDTO document) {
        CommonResMsgDTO<KnowledgeDocumentDTO> ret;
        log.info(">>>>>>> EnergyAi document receive msg proc {}", System.currentTimeMillis());
        if (document.getId() != null) {
            int count = knowledgeDocumentService.updateById(document);
            ret = count > 0
                    ? CommonResMsgDTO.successDeviceRes(document)
                    : CommonResMsgDTO.failureDeviceRes(null);
        } else {
            int count = knowledgeDocumentService.insert(document);
            ret = count > 0
                    ? CommonResMsgDTO.successDeviceRes(document)
                    : CommonResMsgDTO.failureDeviceRes(null);
        }
        if (ret.getStatus() != ApiResStatus.SUCCESS) {
            log.error(">>>>>>> EnergyAi document receive msg error {}", ret.getMsg());
        }
        return ret;
    }

    @GetMapping("/get")
    public CommonResMsgDTO<KnowledgeDocumentDTO> get(@RequestParam Long id) {
        CommonResMsgDTO<KnowledgeDocumentDTO> ret = CommonResMsgDTO.successDeviceRes(knowledgeDocumentService.getById(id));
        log.info(">>>>>>> EnergyAi document get receive msg proc {}", System.currentTimeMillis());
        return ret;
    }

    @GetMapping("/query")
    public CommonResMsgDTO<PageInfo<KnowledgeDocumentDTO>> query(KnowledgeDocumentQueryParam query, Pageable pageable) {
        CommonResMsgDTO<PageInfo<KnowledgeDocumentDTO>> ret = CommonResMsgDTO.successDeviceRes(knowledgeDocumentService.queryAll(query, pageable));
        log.info(">>>>>>> EnergyAi document query receive msg proc {}", System.currentTimeMillis());
        return ret;
    }

}
