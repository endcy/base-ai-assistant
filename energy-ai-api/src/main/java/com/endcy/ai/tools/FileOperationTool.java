package com.endcy.ai.tools;

import cn.hutool.core.io.FileUtil;
import com.endcy.ai.util.ToolSecurityUtils;
import com.endcy.service.common.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 文件操作工具类（提供文件读写功能）。
 * <p>
 * 文件名由 LLM 生成，不可完全信任，读写前通过 {@link ToolSecurityUtils#checkFilePath} 做路径穿越校验，
 * 确保目标文件始终位于 {@link FileConstant#FILE_SAVE_DIR}/file 目录内。
 * </p>
 *
 * @author endcy
 */
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        String check = ToolSecurityUtils.checkFilePath(FILE_DIR, fileName);
        if (check != null) {
            return check;
        }
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content
    ) {
        String check = ToolSecurityUtils.checkFilePath(FILE_DIR, fileName);
        if (check != null) {
            return check;
        }
        String filePath = FILE_DIR + "/" + fileName;

        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}
