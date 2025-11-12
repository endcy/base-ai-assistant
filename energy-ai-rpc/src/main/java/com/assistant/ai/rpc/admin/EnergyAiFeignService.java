package com.assistant.ai.rpc.admin;

import com.assistant.ai.rpc.domain.base.CommonResMsgDTO;

/**
 * @author endcy
 * @date 2024/12/12 21:19:35
 * @implNote 实现命名规约
 * 客户端生产方(调用方) 定义实现类名为XXXClient
 * 消息处理服务消费方 定义实现类名为XXXProcessor
 */
public interface EnergyAiFeignService {

    CommonResMsgDTO<String> callAiQa(String content);

}
