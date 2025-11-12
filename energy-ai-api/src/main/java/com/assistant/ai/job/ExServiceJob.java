package com.assistant.ai.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 扫描任务
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExServiceJob {


    /**
     * 示例扫描任务
     */
    @XxlJob("exServiceScanDataJob")
    public void exServiceScanDataJob() {
        String jobParam = XxlJobHelper.getJobParam();

    }


}
