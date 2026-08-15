package com.endcy.ai.domain.context;

import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * ...
 *
 * @author endcy
 * @date 2026/6/13 16:08:05
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatConfigResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 383579097253459196L;

    private String rewrittenMessage;
    private ContextUserRecordDTO userRecord;
    private List<Message> existingMessages;
    private List<Advisor> dataResourceAdvisors;
    private String mediaAnalysisText;

}
