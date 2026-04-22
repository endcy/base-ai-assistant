package com.assistant.ai.skill;

import com.assistant.ai.tools.InnerTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 技能工具
 * 将 SkillManager 中加载的所有技能转换为 ToolCallback 注册到 Agent
 * LLM 在对话中根据 description 自主判断是否需要调用某个技能
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillTool implements InnerTool {

    private final SkillManager skillManager;
    private final ChatClient commonChatClient;

    @Override
    public List<ToolCallback> loadToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (Skill skill : skillManager.getAllSkills()) {
            ToolCallback callback = FunctionToolCallback.builder(
                                                                "skill_" + skill.getName(),
                                                                (String input) -> executeSkill(skill, input))
                                                        .description(skill.getDescription())
                                                        .build();
            callbacks.add(callback);
        }
        log.info("Registered {} skill tools", callbacks.size());
        return callbacks;
    }

    private String executeSkill(Skill skill, String input) {
        log.info("Executing skill: {} with input length: {}", skill.getName(), input != null ? input.length() : 0);
        String renderedPrompt = skill.render(input != null ? input : "");

        try {
            ChatResponse response = commonChatClient.prompt().user(renderedPrompt).call().chatResponse();
            if (response != null) {
                return response.getResult().getOutput().getText();
            }
            return "技能执行失败：无响应";
        } catch (Exception e) {
            log.error("Skill execution error: {}", e.getMessage());
            return "技能执行异常: " + e.getMessage();
        }
    }
}
