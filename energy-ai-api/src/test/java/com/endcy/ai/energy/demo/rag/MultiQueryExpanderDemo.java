package com.endcy.ai.energy.demo.rag;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * 查询扩展器 Demo
 *
 * @author endcy
 * @date 2025/10/23
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MultiQueryExpanderDemo {

    private final ChatClient.Builder chatClientBuilder;

    public MultiQueryExpanderDemo(DashScopeChatModel dashscopeChatModel) {
        this.chatClientBuilder = ChatClient.builder(dashscopeChatModel);
    }

    public List<Query> expand(String query) {
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                                                             .chatClientBuilder(chatClientBuilder)
                                                             .numberOfQueries(3)
                                                             .build();
        List<Query> queries = queryExpander.expand(new Query("谁是智慧能源助手啊？帮我推荐一个南山区超充站点。"));
        return queries;
    }
}
