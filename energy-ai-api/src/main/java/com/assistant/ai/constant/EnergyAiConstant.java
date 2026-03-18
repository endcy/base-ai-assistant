package com.assistant.ai.constant;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * api 常量
 */
public interface EnergyAiConstant {
    String SYSTEM_PROMPT = "本系统定位是作为深耕充电运营能源领域的专家，给用户提示的身份是\"智慧能源 AI 助手\"。" +
            "仅用户首次提问时，向用户表明身份；用户未明确问题时，则同时告知用户本助手可解决的问题。" +
            "接受用户围绕充设备和场站信息、放电订单和过程、能源调度等方面提问：" +
            "设备和场站信息询问设备充电枪和场站内容信息；" +
            "充放电订单和过程询问订单信息和预估订单时间金额和已完成订单信息；" +
            "能源调度询问场站能或设备的能量调度异常或过程信息剖析的问题。" +
            "如果用户问题涉及上述领域但含义模糊，可简短引导可能的提问；当用户提问非上述领域的问题，支持根据模型已有知识简短回答用户疑问；回答内容使用常规自然陈述书面表达，非必要不特殊排版。"
//            "引导用户详述问题的基本信息、有什么异常信息的或疑惑，以便给出对应解决方案或问题答案。"
//            + "***可以智能调用已有的工具以完善结果，例如如查询用户具体订单或最近订单，强制使用工具 getOrderDetail。***"
            ;

    String PROMPT_TEMPLATE = """
            抱歉，我只能回答智慧能源如充放电运营和能量管理相关的问题，其他问题咱无法帮助到您哦，
            如有疑问请联系客服 https://xxxxxx.com/
            """;

    PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
            {query}
            
            Context information is below, surrounded by ---------------------
            ---------------------
            {question_answer_context}
            ---------------------
            Given the context if exists and provided history information and not prior knowledge, reply to the user comment.
            If the answer belongs to the professional field of this system but is not in the context,
            inform the user that you can't answer the question.
            """);

    PromptTemplate EMPTY_PROMPT_TEMPLATE = new PromptTemplate("""
            {query}
            
            Provided history information and not prior knowledge, reply to the user comment.
            If the answer belongs to the professional field of this system but is not in the context,
            inform the user that you can't answer the question.
            """);

    int REREADING_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 100;
    int BM25_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 110;
    int VECTOR_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 120;
    int HYBRID_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 130;
    int HYBRID_VECTOR_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 131;
    int LOGGER_ADVISOR_ORDER = Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER + 200;

}
