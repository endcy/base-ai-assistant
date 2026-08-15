package com.endcy.ai.energy;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.Arrays;

/**
 * 阿里云灵积 AI SDK 调用
 *
 * @author endcy
 * @date 2025/10/23
 */
public class SdkAiInvoke {

    public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                                   .role(Role.SYSTEM.getValue())
                                   .content("You are a helpful assistant.")
                                   .build();
        Message userMsg = Message.builder()
                                 .role(Role.USER.getValue())
                                 .content("你好，我是智慧能源AI助手，这是综合能源服务 AI 超级智能体")
                                 .build();
        GenerationParam param = GenerationParam.builder()
                                               // 从环境变量读取 API Key（DASHSCOPE_API_KEY）
                                               .apiKey(TestApiKey.API_KEY)
                                               // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                                               .model("qwen-plus")
                                               .messages(Arrays.asList(systemMsg, userMsg))
                                               .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                                               .build();
        return gen.call(param);
    }

    public static void main(String[] args) {
        try {
            GenerationResult result = callWithMessage();
            System.out.println(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 使用日志框架记录异常信息
            System.err.println("An error occurred while calling the generation service: " + e.getMessage());
        }
        System.exit(0);
    }
}
