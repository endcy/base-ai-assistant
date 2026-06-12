package com.assistant.ai.rpc.processor;

import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.ai.rpc.api.KnowledgeDocRpcService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.service.common.annotation.LogReqRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 知识文档 Dubbo 服务端处理器
 * <p>仅在 ai.rpc.enabled=true 时启用，开源单机部署可关闭</p>
 *
 * @author endcy
 * @date 2025/6/14 21:09:10
 */
@Slf4j
@LogReqRes("log.enable.rpc.KnowledgeDocRpcService")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.rpc.enabled", havingValue = "true")
@DubboService(version = "1.0.0", timeout = 10000)
public class KnowledgeDocRpcProcessor implements KnowledgeDocRpcService {

    private final KnowledgeDocumentService knowledgeDocumentService;

    @Override
    public CommonResMsgDTO<Object> getDocInfo(Long docId) {
        return CommonResMsgDTO.successDeviceRes(knowledgeDocumentService.getById(docId));
    }

}
