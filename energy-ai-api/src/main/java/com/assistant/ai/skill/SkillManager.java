package com.assistant.ai.skill;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能管理器
 * 启动时扫描 classpath:skill/*.md，解析 YAML Front Matter 元数据
 * 供 SkillTool 将每个技能转换为 ToolCallback 注册到 Agent
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
@Component
public class SkillManager {

    private static final String SKILL_PATTERN_PATH = "classpath:skill/*.md";
    private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^---\\s*\\n([\\s\\S]*?)\\n---\\s*\\n([\\s\\S]*)$", Pattern.MULTILINE);
    private static final Pattern YAML_FIELD_PATTERN = Pattern.compile("^(\\w+):\\s*(.+)$", Pattern.MULTILINE);

    @Getter
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(SKILL_PATTERN_PATH);
            for (Resource resource : resources) {
                try {
                    Skill skill = parseSkill(resource);
                    if (skill != null && StrUtil.isNotBlank(skill.getName())) {
                        skills.put(skill.getName(), skill);
                        log.info("Loaded skill: {} - {}", skill.getName(), skill.getDescription());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse skill file: {}", resource.getFilename(), e);
                }
            }
            log.info("Total skills loaded: {}", skills.size());
        } catch (IOException e) {
            log.warn("Failed to scan skill directory: {}", SKILL_PATTERN_PATH, e);
        }
    }

    /**
     * 根据名称获取技能
     */
    public Skill getSkill(String name) {
        return skills.get(name);
    }

    /**
     * 获取所有已加载的技能
     */
    public List<Skill> getAllSkills() {
        return List.copyOf(skills.values());
    }

    /**
     * 检查是否存在指定技能
     */
    public boolean hasSkill(String name) {
        return skills.containsKey(name);
    }

    private Skill parseSkill(Resource resource) throws IOException {
        try (InputStream is = resource.getInputStream()) {
            String content = IoUtil.read(is, StandardCharsets.UTF_8);
            Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);

            if (!matcher.find()) {
                log.warn("Skill file {} has no YAML front matter, skipping", resource.getFilename());
                return null;
            }

            String yamlContent = matcher.group(1);
            String promptTemplate = matcher.group(2).trim();

            Skill skill = new Skill();
            skill.setPromptTemplate(promptTemplate);

            // 解析 YAML 字段
            Matcher fieldMatcher = YAML_FIELD_PATTERN.matcher(yamlContent);
            while (fieldMatcher.find()) {
                String key = fieldMatcher.group(1);
                String value = fieldMatcher.group(2).trim();
                switch (key) {
                    case "name" -> skill.setName(value);
                    case "description" -> skill.setDescription(value);
                }
            }

            return skill;
        }
    }
}
