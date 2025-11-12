package com.assistant.ai.tools;

import com.assistant.ai.config.AiWebSearchApiProperties;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.SSEResponseModel;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.lkeap.v20240522.LkeapClient;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsRequest;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsResponse;
import com.tencentcloudapi.lkeap.v20240522.models.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 腾讯原子能力 DeepSeek联网搜索
 * <a href="https://console.cloud.tencent.com/cam/capi">密钥可前往官网控制台</a> 进行获取
 * 注意：这个接口有点小贵，除了token消耗，还需要web搜索的次数消耗，验证三千个车型上市日期的搜索花了接近85块钱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepSeekWebSearchTool {

    private final AiWebSearchApiProperties aiWebSearchApiProperties;

    @Tool(description = "search answer from Web DeepSeek")
    public String searchQuestion(@ToolParam(description = "Search result format") String answerJsonFormat,
                                 @ToolParam(description = "Search question content") String question) {
        String fixQuestion = question + " 根据示例格式 " + answerJsonFormat + "输出问题答案";
        return searchInWebDeepSeek(answerJsonFormat, fixQuestion);
    }

    private String searchInWebDeepSeek(String ansFormat, String question) {
        ChatCompletionsRequest req = new ChatCompletionsRequest();
        //用这个综合模型
        req.setModel("deepseek-v3.1");
        Message reqPrompt = new Message();
        reqPrompt.setRole("system");
        String tips = "你是一个支持联网搜索的AI助手，根据示例的格式，简洁回答用户问题";
        reqPrompt.setContent(tips);
        Message[] message = new Message[2];
        message[0] = reqPrompt;

        Message reqMsg = new Message();
        reqMsg.setRole("user");
        reqMsg.setContent(question);
        message[1] = reqMsg;

        req.setMessages(message);

        req.setStream(false);
        req.setEnableSearch(true);
        req.setSkipSign(false);

        Credential cred = new Credential(aiWebSearchApiProperties.getTencentApiSecretId(), aiWebSearchApiProperties.getTencentApiSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("lkeap.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        LkeapClient client = new LkeapClient(cred, "ap-guangzhou", clientProfile);

        // resp为ChatCompletionsResponse的实例，与请求对象对应
        ChatCompletionsResponse resp = null;
        try {
            resp = client.ChatCompletions(req);
            // 输出json格式的字符串回包
            if (resp.isStream()) {
                for (SSEResponseModel.SSE e : resp) {
                    System.out.println(e.Data);
                }
            } else {
                //log.info(AbstractModel.toJsonString(resp));
                String content = resp.getChoices()[0].getMessage().getContent();
                log.info(extractJsonFromText(content));
                return content;
            }
        } catch (Exception e) {
            log.error("searchInWebDeepSeek failed", e);
        } finally {
            try {
                if (resp != null) {
                    resp.close();
                }
            } catch (Exception e) {
                //do nothing
            }
        }

        return null;
    }


    private static String extractJsonFromText(String mixedText) {
        // 简单匹配{...}结构的正则表达式
        String regex = "\\{[^}]*}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mixedText);
        if (matcher.find()) {
            String potentialJson = matcher.group();
            try {
                // 尝试解析提取到的字符串
                return potentialJson;
            } catch (Exception e) {
                log.error("not matched correct json：{}", potentialJson);
            }
        }
        return null;
    }

}
