package com.endcy.ai.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Fault reporting tool — submits fault repair work orders for equipment/devices.
 *
 * <p>Currently a stub implementation that generates simulated work order numbers.
 * Will be replaced with real RPC calls once the interface is ready.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
public class ReportFaultTool {

    @Tool(description = "Submit an equipment fault repair work order. Use this tool when a user reports equipment malfunction, abnormality, " +
            "or service interruption. Requires station ID, device number, fault description, etc. " +
            "After creation, a work order number is generated for the user to track progress. " +
            "Applicable scenarios: user says 'equipment broken', 'device not working', 'service fault', etc.")
    public String reportFault(
            @ToolParam(description = "Station ID (e.g. ST-A-001)") String stationId,
            @ToolParam(description = "Device number (e.g. 3)") String deviceNo,
            @ToolParam(description = "Fault description (detailed description of the fault)") String faultDescription,
            @ToolParam(description = "Reporter name") String reporterName,
            @ToolParam(description = "Reporter contact phone") String reporterPhone) {

        log.info("Fault report: stationId={}, deviceNo={}, description={}", stationId, deviceNo, faultDescription);

        // Generate work order number
        String ticketId = "TICKET-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        StringBuilder sb = new StringBuilder();
        sb.append("Fault repair work order created successfully!\n\n");
        sb.append("[Work Order Info]\n");
        sb.append("Work order number: ").append(ticketId).append("\n");
        sb.append("Station: ").append(stationId).append("\n");
        sb.append("Device number: ").append(deviceNo).append("\n");
        sb.append("Fault description: ").append(faultDescription).append("\n");
        sb.append("Reporter: ").append(reporterName).append("\n");
        sb.append("Contact phone: ").append(reporterPhone).append("\n");
        sb.append("Created at: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        sb.append("Our operations team will contact you within 2 hours to confirm the fault. Please keep your phone available.\n");
        sb.append("You can use work order number ").append(ticketId).append(" to check the processing progress.");

        return sb.toString();
    }
}
