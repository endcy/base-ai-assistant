package com.endcy.ai.integration.notify;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Notification service.
 *
 * <p>Sends notifications on task completion / awaiting approval / failure. Currently a log placeholder;
 * future versions integrate DingTalk/WeCom/Email webhooks.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Service
public class NotificationService {

    @Value("${ai.notify.dingtalk.webhook:}")
    private String dingtalkWebhook;

    @Value("${ai.notify.wecom.webhook:}")
    private String wecomWebhook;

    @Value("${ai.notify.enabled:false}")
    private boolean enabled;

    /**
     * Send task completion notification.
     */
    public void notifyTaskCompleted(String taskId, String summary) {
        send("任务完成", "taskId=" + taskId + "\n摘要: " + StrUtil.maxLength(summary, 200));
    }

    /**
     * Send awaiting-approval notification.
     */
    public void notifyWaitingApproval(String taskId, String toolName, String reason) {
        send("待审批", "taskId=" + taskId + "\n工具: " + toolName + "\n原因: " + reason);
    }

    /**
     * Send task failure notification.
     */
    public void notifyTaskFailed(String taskId, String error) {
        send("任务失败", "taskId=" + taskId + "\n错误: " + StrUtil.maxLength(error, 300));
    }

    private void send(String title, String content) {
        if (!enabled) {
            log.info("[通知-占位] {}: {}", title, content.replace("\n", " | "));
            return;
        }
        if (StrUtil.isNotBlank(dingtalkWebhook)) {
            log.info("[钉钉通知] {} -> {}", title, dingtalkWebhook);
        }
        if (StrUtil.isNotBlank(wecomWebhook)) {
            log.info("[企微通知] {} -> {}", title, wecomWebhook);
        }
    }
}
