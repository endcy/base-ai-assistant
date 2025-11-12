package com.assistant.ai.rpc.api;

import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author endcy
 * @date 2025/06/14 21:00:35
 */
public interface KnowledgeDocRpcService {
    Logger log = LoggerFactory.getLogger(KnowledgeDocRpcService.class);

    CommonResMsgDTO<Object> getDocInfo(Long docId);

}
