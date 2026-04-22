package com.assistant.ai.controller;

import com.assistant.ai.command.CommandManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 命令执行控制器
 * 用户通过 REST API 主动指定命令名来执行 Prompt 模板
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class CommandController {

    private final CommandManager commandManager;
    private final ChatClient commonChatClient;

    /**
     * 执行命令
     * 用户指定命令名和输入内容，将模板渲染后发送给 LLM 获取响应
     */
    @PostMapping("/execute")
    public CommandResponse executeCommand(@RequestBody CommandRequest request) {
        String commandName = request.getCommand();
        String input = request.getInput();

        if (!commandManager.hasCommand(commandName)) {
            throw new IllegalArgumentException("命令不存在: " + commandName);
        }

        String prompt = commandManager.executeCommand(commandName, input);
        log.info("Executing command: {} with input length: {}", commandName, input != null ? input.length() : 0);

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(prompt)
                .call()
                .chatResponse();

        String result = chatResponse != null ? chatResponse.getResult().getOutput().getText() : "请求异常";

        CommandResponse response = new CommandResponse();
        response.setCommand(commandName);
        response.setResult(result);
        return response;
    }

    /**
     * 获取所有可用命令列表
     */
    @GetMapping("/list")
    public List<String> listCommands() {
        return commandManager.getAllCommandNames();
    }

    @Data
    public static class CommandRequest {
        private String command;
        private String input;
    }

    @Data
    public static class CommandResponse {
        private String command;
        private String result;
    }
}
