package com.endcy.ai.energy;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * OpenAI 兼容模式调用 DeepSeek（腾讯 LKEAP）。
 *
 * @author endcy
 * @date 2025/10/23
 */
public class TestDeepSeekChat {
    /**
     * qwen的在线搜索准确性远不及DeepSeek 考虑弃用
     */
    public static void main(String[] args) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                                                .apiKey(System.getenv().getOrDefault("LKEAP_API_KEY", "${LKEAP_API_KEY}"))
                                                .baseUrl("https://api.lkeap.cloud.tencent.com/v1")
                                                .build();

        String car = "岚图梦想家 2025款 EV 四驱旗舰乾崑版";
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                                                                      .addUserMessage("请检索车型 " + car + " ，生产日期是什么时候，答案仅输出日期 yyyy-MM-dd 和最佳参考url")
                                                                      .putAdditionalQueryParam("enable_search", "true")
                                                                      .putAdditionalQueryParam("forced_search", "true")
                                                                      .model("deepseek-v3")
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
