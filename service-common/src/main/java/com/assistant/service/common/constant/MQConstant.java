package com.assistant.service.common.constant;

/**
 * MQ常量
 */
public interface MQConstant {

    /**
     * 延迟消息路由关键字
     */
    String DELAYED_EXCHANGE_KEYWORD = "_delayed";

    /**
     * admin-api MQ路由
     */
    String MSG_TO_ADMIN_API = "ai_admin_delayed_exchange";

    /**
     * energy-ai-api MQ路由
     */
    String MSG_TO_AI_API = "ai_api_delayed_exchange";


    /* MQ topic 定义 */
    String MQ_ADMIN_REPORT_PROCESS = "msg_to_report_process";

    String MQ_API_REFRESH_DUCOMENT = "msg_to_api_refresh_document";

}
