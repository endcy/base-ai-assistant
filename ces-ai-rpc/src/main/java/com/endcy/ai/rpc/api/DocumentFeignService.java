package com.endcy.ai.rpc.api;

import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentActionParam;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentMatchParam;
import com.endcy.ai.rpc.domain.request.KnowledgeDocumentParam;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentMatchRet;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentStatusItem;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentStatusRet;

/**
 * 文档 Feign 接口
 *
 * @author endcy
 * @since 2024/12/12 21:19:35
 */
public interface DocumentFeignService {

    CommonResMsgDTO<KnowledgeDocumentStatusItem> modify(KnowledgeDocumentParam document);

    CommonResMsgDTO<KnowledgeDocumentStatusRet> action(KnowledgeDocumentActionParam query);

    CommonResMsgDTO<KnowledgeDocumentMatchRet> match(KnowledgeDocumentMatchParam query);
}
