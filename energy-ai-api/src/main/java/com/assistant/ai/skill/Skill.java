package com.assistant.ai.skill;

import lombok.Data;

/**
 * 技能模型
 * 由 Markdown 文件的 YAML Front Matter 解析而来
 *
 * @author endcy
 * @date 2026/04/22
 */
@Data
public class Skill {

    private String name;
    private String description;
    private String promptTemplate;

    /**
     * 将技能渲染为 ToolCallback 的描述
     */
    public String toDescription() {
        return description;
    }

    /**
     * 将技能渲染为 Prompt，填充用户输入
     */
    public String render(String userInput) {
        if (promptTemplate == null) {
            return userInput;
        }
        return promptTemplate.replace("{{input}}", userInput)
                             .replace("{{name}}", name);
    }
}
