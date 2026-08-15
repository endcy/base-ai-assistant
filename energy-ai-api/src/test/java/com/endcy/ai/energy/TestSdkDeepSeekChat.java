package com.endcy.ai.energy;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.SSEResponseModel;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.lkeap.v20240522.LkeapClient;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsRequest;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsResponse;
import com.tencentcloudapi.lkeap.v20240522.models.Message;

/**
 * 腾讯原子能力 DeepSeek联网搜索
 *
 * <p>密钥从环境变量读取：TENCENT_SECRET_ID / TENCENT_SECRET_KEY。</p>
 *
 * @author endcy
 * @date 2025/10/23
 */
public class TestSdkDeepSeekChat {

    public static void main(String[] args) {
        try {
            String secretId = System.getenv().getOrDefault("TENCENT_SECRET_ID", "${TENCENT_SECRET_ID}");
            String secretKey = System.getenv().getOrDefault("TENCENT_SECRET_KEY", "${TENCENT_SECRET_KEY}");
            Credential cred = new Credential(secretId, secretKey);
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setEndpoint("lkeap.tencentcloudapi.com");
            // 实例化一个client选项，可选的，没有特殊需求可以跳过
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setHttpProfile(httpProfile);
            // 实例化要请求产品的client对象,clientProfile是可选的
            LkeapClient client = new LkeapClient(cred, "ap-guangzhou", clientProfile);
            // 实例化一个请求对象,每个接口都会对应一个request对象
            ChatCompletionsRequest req = new ChatCompletionsRequest();
            req.setModel("deepseek-v3.1");
            Message[] messages1 = new Message[2];
            Message message1 = new Message();
            message1.setRole("system");
            String ansFormat = "{\"launchDate\":\"yyyy-MM-dd\",\"url\":\"参考的url\"}";
            message1.setContent("你是一个支持联网搜索的AI助手，根据 " + ansFormat + "的json格式，简洁回答输入的问题");
            messages1[0] = message1;

            Message message2 = new Message();
            message2.setRole("user");
            message2.setContent("请使用互联网在线搜索车型 [岚图梦想家 2025款 EV 四驱旗舰乾崑版]，精准获取该车型最可能的生产日期是什么时候和参考的url，根据格式输出问题答案");
            messages1[1] = message2;

            req.setMessages(messages1);

            req.setStream(false);
            req.setEnableSearch(true);
            req.setSkipSign(false);
            // 返回的resp是一个ChatCompletionsResponse的实例，与请求对象对应
            ChatCompletionsResponse resp = client.ChatCompletions(req);
            // 输出json格式的字符串回包
            if (resp.isStream()) { // 流式响应
                for (SSEResponseModel.SSE e : resp) {
                    System.out.println(e.Data);
                }
            } else { // 非流式响应
                String content = resp.getChoices()[0].getMessage().getContent();
                System.out.println(content);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
