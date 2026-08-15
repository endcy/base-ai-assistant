package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * AI回答内容分片
 *
 * @author endcy
 * @since 2025/12/13 13:52:18
 */
@Data
public class AIAnswerTextChunkRet implements Serializable {
    private static final long serialVersionUID = 5907522177808257386L;

    private String text;

    private boolean isFinal;
}
