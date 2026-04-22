package com.assistant.ai.command;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 命令管理器
 * 启动时扫描 classpath:command/*.md，加载纯 Prompt 模板到内存
 * 文件名即为命令名，用户通过 REST API 主动指定命令名来执行
 *
 * @author endcy
 * @date 2026/04/22
 */
@Getter
@Slf4j
@Component
public class CommandManager {

    private static final String COMMAND_PATTERN_PATH = "classpath:command/*.md";

    private final Map<String, String> commands = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(COMMAND_PATTERN_PATH);
            for (Resource resource : resources) {
                try {
                    String name = resource.getFilename();
                    if (name != null && name.endsWith(".md")) {
                        name = name.substring(0, name.length() - 3);
                    }
                    String content = resource.getContentAsString(StandardCharsets.UTF_8);
                    commands.put(name, content.trim());
                    log.info("Loaded command: {}", name);
                } catch (Exception e) {
                    log.warn("Failed to parse command file: {}", resource.getFilename(), e);
                }
            }
            log.info("Total commands loaded: {}", commands.size());
        } catch (IOException e) {
            log.warn("Failed to scan command directory: {}", COMMAND_PATTERN_PATH, e);
        }
    }

    /**
     * 根据名称获取命令模板
     */
    public String getCommand(String name) {
        return commands.get(name);
    }

    /**
     * 获取所有已加载的命令名
     */
    public List<String> getAllCommandNames() {
        return List.copyOf(commands.keySet());
    }

    /**
     * 检查是否存在指定命令
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }

    /**
     * 执行命令：将模板中的 {{input}} 替换为实际输入
     */
    public String executeCommand(String name, String input) {
        String template = commands.get(name);
        if (template == null) {
            return "命令不存在: " + name;
        }
        return template.replace("{{input}}", input);
    }
}
