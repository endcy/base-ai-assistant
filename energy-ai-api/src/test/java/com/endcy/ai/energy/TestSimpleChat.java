package com.endcy.ai.energy;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * OpenAI 兼容模式调用 qwen 模型。
 *
 * @author endcy
 * @date 2025/10/23
 */
public class TestSimpleChat {
    /**
     * qwen的在线搜索远不及 DeepSeek 考虑弃用
     * openai不支持自定义是否联网的配置 考虑url调用或者sdk调用
     */
    public static void main(String[] args) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                                                .apiKey(TestApiKey.API_KEY)
                                                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                                                .build();

        String car = "极氪7X 2025款 75kWh 后驱智驾版";
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                                                                      .addUserMessage("请检索车型 " + car + " ，生产日期是什么时候，答案仅输出日期 yyyy-MM-01 格式即可")
                                                                      .model("qwen-plus")
                                                                      .build();

        try {
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            System.out.println(chatCompletion);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
