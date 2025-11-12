package com.assistant.ai.rpc.client;

import com.assistant.ai.rpc.api.KnowledgeDocRpcService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Primary;

/**
 * 知识文档 额外的调用 接口客户端
 * 仅作为参考示例
 * TODO 如需启用dubbo 请移除@Component的注释
 *
 * @author endcy
 * @date 2025/6/14 21:01:17
 */
@Slf4j
@Primary
//@Component
@RequiredArgsConstructor
public class KnowledgeDocApiClient implements KnowledgeDocRpcService {

    //在properties或编码指定url 路由中心会失效 仅用于测试  url = "${dubbo-energy-admin-api.url:}"
    @DubboReference(interfaceClass = KnowledgeDocRpcService.class, version = "1.0.0", url = "${dubbo-energy-admin-api.url:}")
    private KnowledgeDocRpcService knowledgeDocRpcService;

    @Override
    @CircuitBreaker(name = "defaultInstance", fallbackMethod = "getDocInfoFallback")
    public CommonResMsgDTO<Object> getDocInfo(Long docId) {
        return knowledgeDocRpcService.getDocInfo(docId);
    }

    public CommonResMsgDTO<Object> getDocInfoFallback(Long docId) {
        return CommonResMsgDTO.errorDeviceRes("rpc error");
    }


}
