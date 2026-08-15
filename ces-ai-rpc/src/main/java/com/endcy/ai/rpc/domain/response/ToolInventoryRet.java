package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 工具清单返回
 *
 * @author endcy
 * @since 2026/08/08
 */
@Data
public class ToolInventoryRet implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer total;

    private List<ToolInfo> tools;

    @Data
    public static class ToolInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private String description;
        private String businessType;
        private String riskLevel;
        private Boolean requiresApproval;
    }
}
