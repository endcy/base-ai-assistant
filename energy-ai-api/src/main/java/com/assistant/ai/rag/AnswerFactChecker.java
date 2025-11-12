package com.assistant.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 尽可能规避AI幻觉，答案检索回查，事实检测
 * 搜索本地向量或pd向量，未找到关联文档应该取消回答
 *
 * @author endcy
 * @date 2025/10/23 20:57:37
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerFactChecker {

}
