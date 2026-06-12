package com.assistant.ai.rpc.processor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.assistant.ai.manager.KnowledgeDocumentManager;
import com.assistant.ai.manager.VectorStoreManager;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.service.KnowledgeDocumentService;
import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;
import com.assistant.ai.rpc.domain.request.KnowledgeDocumentActionParam;
import com.assistant.ai.rpc.domain.request.KnowledgeDocumentMatchParam;
import com.assistant.ai.rpc.domain.request.KnowledgeDocumentParam;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchRet;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentStatusItem;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentStatusRet;
import com.assistant.ai.rpc.enums.DocumentActionType;
import com.assistant.service.common.annotation.LogReqRes;
import com.assistant.service.common.constant.GlobalConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档管理 RPC 处理器
 * 提供文档的增改、启停、刷新和匹配检索等接口
 *
 * @author endcy
 * @date 2025/6/5 20:16:10
 */
@LogReqRes("log.enable.rpc.DocumentFeignProcessor")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/document")
public class DocumentFeignProcessor {
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final VectorStoreManager vectorStoreManager;
    private final KnowledgeDocumentManager knowledgeDocumentManager;

    /**
     * 新增或修改文档
     * 先更新数据库，再刷新向量库
     */
    @PostMapping("/modify")
    public CommonResMsgDTO<KnowledgeDocumentStatusItem> modify(@Validated @RequestBody KnowledgeDocumentParam document) {
        KnowledgeDocumentStatusItem ret = new KnowledgeDocumentStatusItem();
        ret.setId(document.getId());
        ret.setLoaded(false);
        ret.setEnabled(false);

        //先插入或更新数据
        document.setExpiredTime(ObjectUtil.defaultIfNull(document.getExpiredTime(), GlobalConstant.DEFAULT_EXPIRED_DATE));
        KnowledgeDocumentDTO res = BeanUtil.copyProperties(document, KnowledgeDocumentDTO.class);
        res.setLoaded(false);
        int udpRows = 0;
        try {
            if (BooleanUtil.isTrue(document.getUpdateDoc())) {
                udpRows = knowledgeDocumentService.updateById(res);
            } else {
                udpRows = knowledgeDocumentService.insert(res);
            }
        } catch (Exception e) {
            log.warn("document modify error:{}", e.getMessage());
        }
        if (udpRows <= 0) {
            return CommonResMsgDTO.failureDeviceRes(ret, "更新或新增数据失败:文档id重复或参数缺失，请检查参数");
        }
        //刷新向量数据库
        vectorStoreManager.refreshDbKnowledgeDocument();
        res = knowledgeDocumentService.getById(document.getId());
        if (res != null) {
            ret.setLoaded(res.getLoaded());
            ret.setEnabled(res.getEnabled());
        }
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    /**
     * 文档操作（启用、禁用、刷新、删除）
     */
    @PostMapping("/action")
    public CommonResMsgDTO<KnowledgeDocumentStatusRet> action(@Validated @RequestBody KnowledgeDocumentActionParam query) {
        DocumentActionType actionType = DocumentActionType.getByCode(query.getAction());
        if (actionType == null) {
            return CommonResMsgDTO.errorDeviceRes("无效的操作类型");
        }
        KnowledgeDocumentStatusRet ret = new KnowledgeDocumentStatusRet();

        boolean needRefresh = false;
        switch (actionType) {
            case DISABLED -> {
                //更新知识库，同时清除向量库对应文档
                if (CollUtil.isNotEmpty(query.getIds())) {
                    knowledgeDocumentService.updateDocumentEnabledStatus(query.getIds(), false);
                }
            }
            case ENABLED -> {
                if (CollUtil.isNotEmpty(query.getIds())) {
                    knowledgeDocumentService.updateDocumentEnabledStatus(query.getIds(), true);
                    needRefresh = true;
                }
            }
            case REFRESH -> {
                needRefresh = true;
            }
            case DELETE -> {
                //更新知识库，同时清除向量库对应文档
                if (CollUtil.isNotEmpty(query.getIds())) {
                    knowledgeDocumentService.removeByIds(CollUtil.newHashSet(query.getIds()));
                }
            }
        }
        //刷新向量
        if (needRefresh) {
            vectorStoreManager.refreshDbKnowledgeDocument();
        }

        //查询知识库状态
        List<KnowledgeDocumentDTO> docList = knowledgeDocumentService.queryAll(
                buildQueryParam(query.getScopeType(), query.getBusinessType()));
        //如果指定了文档id列表，按id过滤
        if (CollUtil.isNotEmpty(query.getIds())) {
            HashSet<Long> idSet = CollUtil.newHashSet(query.getIds());
            docList = docList.stream().filter(doc -> idSet.contains(doc.getId())).collect(Collectors.toList());
        }
        ret.setDocStatusList(convertDocStatusList(docList));
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    private List<KnowledgeDocumentStatusItem> convertDocStatusList(List<KnowledgeDocumentDTO> docList) {
        List<KnowledgeDocumentStatusItem> docStatusList = CollUtil.newArrayList();
        docList.forEach(doc -> {
            KnowledgeDocumentStatusItem docStatus = new KnowledgeDocumentStatusItem();
            docStatus.setId(doc.getId());
            docStatus.setLoaded(doc.getLoaded());
            docStatus.setEnabled(doc.getEnabled());
            docStatusList.add(docStatus);
        });
        return docStatusList;
    }

    /**
     * 文档匹配检索
     */
    @PostMapping("/match")
    public CommonResMsgDTO<KnowledgeDocumentMatchRet> match(@Validated @RequestBody KnowledgeDocumentMatchParam query) {
        DocumentQueryContext queryContext = new DocumentQueryContext();
        queryContext.setScopeType(query.getScopeType());
        queryContext.setBusinessType(query.getBusinessType());
        queryContext.setOriginalQuestion(query.getQuestion());
        queryContext.setReReadingQuestion(query.getQuestion());
        queryContext.setEnablePublic(query.getEnablePublic());
        List<KnowledgeDocumentMatchItem> docMatchList = knowledgeDocumentManager.match(queryContext, query.getSimilarityTopK());
        return CommonResMsgDTO.successDeviceRes(new KnowledgeDocumentMatchRet(docMatchList));
    }

    /**
     * 构建文档查询参数
     */
    private com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam buildQueryParam(
            String scopeType, String businessType) {
        com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam queryParam =
                new com.assistant.ai.repository.domain.request.KnowledgeDocumentQueryParam();
        queryParam.setScopeType(scopeType);
        queryParam.setBusinessType(businessType);
        return queryParam;
    }
}
