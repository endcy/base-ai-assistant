package com.endcy.ai.rpc.utils;

import com.endcy.ai.rpc.constant.RpcConfigConstant;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.response.AIAnswerRet;
import com.endcy.ai.rpc.enums.ApiResStatus;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;

/**
 * WebClient响应解析工具类
 *
 * @author endcy
 * @since 2025/12/20 11:46:59
 */
@Slf4j
public class ClientUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> CommonResMsgDTO<T> resolveTipsData(CommonResMsgDTO<T> ret, String requestType) {
        if (ret == null) {
            log.error(">>>>>>> EnergyAi {} request error", requestType);
            return CommonResMsgDTO.failureDeviceRes(null, RpcConfigConstant.BUSINESS_ERROR_TIPS);
        }
        if (ret.getStatus() != ApiResStatus.SUCCESS) {
            log.warn(">>>>>>> EnergyAi {} receive msg error {}", requestType, ret.getMsg());
            AIAnswerRet res = new AIAnswerRet();
            res.setText(RpcConfigConstant.ERROR_TIPS);
        }
        if (log.isDebugEnabled()) {
            log.debug(">>>>>>> EnergyAi {} receive msg proc {}", requestType, System.currentTimeMillis());
        }
        return ret;
    }

    public static <T> CommonResMsgDTO<T> parseResponse(String body, Class<T> clazz) {
        try {
            JavaType type = mapper.getTypeFactory()
                                  .constructParametricType(CommonResMsgDTO.class, clazz);
            return mapper.readValue(body, type);
        } catch (Exception e) {
            log.error("反序列化失败", e);
            throw new RuntimeException("反序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> CommonResMsgDTO<T> parseResponse(String body, ParameterizedTypeReference<CommonResMsgDTO<T>> typeRef) {
        try {
            JavaType type = mapper.getTypeFactory()
                                  .constructType(typeRef.getType());
            return (CommonResMsgDTO<T>) mapper.readValue(body, type);
        } catch (Exception e) {
            log.error("反序列化失败", e);
            throw new RuntimeException("反序列化失败", e);
        }
    }
}
