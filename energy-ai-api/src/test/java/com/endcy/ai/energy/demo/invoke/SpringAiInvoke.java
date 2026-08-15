package com.endcy.ai.energy.demo.invoke;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Spring AI 框架调用 AI 大模型（阿里）
 *
 * @author endcy
 * @date 2025/10/23
 */
// 取消注释后，项目启动时会执行
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private DashScopeChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，我是智慧能源AI助手"))
                                                              .getResult()
                                                              .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
