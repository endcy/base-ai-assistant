package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 更新启用状态请求 DTO
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
public class UpdateEnabledRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> ids;

    private Boolean enabled;
}
