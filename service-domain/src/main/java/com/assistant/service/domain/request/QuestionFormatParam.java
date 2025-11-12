package com.assistant.service.domain.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

/**
 * ...
 *
 * @author endcy
 * @date 2025/10/9 20:24:56
 */
@Data
public class QuestionFormatParam implements Serializable {
    private static final long serialVersionUID = -7870925708532898416L;

    @NotEmpty(message = "答案格式不能为空")
    private String answerFormat;

    @NotEmpty(message = "问题内容不能为空")
    private String question;

}
