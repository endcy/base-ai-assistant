package com.assistant.ai.repository.domain.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchImportResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功导入数量
     */
    private Integer successCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 跳过数量（已存在）
     */
    private Integer skipCount;

    /**
     * 总处理数量
     */
    private Integer totalCount;

    /**
     * 成功导入的文件列表
     */
    private List<String> successFiles;

    /**
     * 失败的文件列表（包含错误信息）
     */
    private List<FailedFile> failedFiles;

    /**
     * 跳过的文件列表
     */
    private List<String> skippedFiles;

    /**
     * 失败的文件信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedFile implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String filePath;
        private String errorMessage;
    }

    /**
     * 添加成功文件
     */
    public void addSuccess(String filePath) {
        if (successFiles == null) {
            successFiles = new ArrayList<>();
        }
        successFiles.add(filePath);
        successCount = successCount != null ? successCount + 1 : 1;
    }

    /**
     * 添加失败文件
     */
    public void addFailed(String filePath, String errorMessage) {
        if (failedFiles == null) {
            failedFiles = new ArrayList<>();
        }
        failedFiles.add(new FailedFile(filePath, errorMessage));
        failCount = failCount != null ? failCount + 1 : 1;
    }

    /**
     * 添加跳过文件
     */
    public void addSkipped(String filePath) {
        if (skippedFiles == null) {
            skippedFiles = new ArrayList<>();
        }
        skippedFiles.add(filePath);
        skipCount = skipCount != null ? skipCount + 1 : 1;
    }

    /**
     * 计算总数
     */
    public Integer getTotalCount() {
        return (successCount != null ? successCount : 0) +
               (failCount != null ? failCount : 0) +
               (skipCount != null ? skipCount : 0);
    }
}
