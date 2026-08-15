package com.endcy.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.endcy.ai.util.ToolSecurityUtils;
import com.endcy.service.common.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

/**
 * 资源下载工具。
 * <p>
 * 下载 URL 由 LLM 生成，下载前通过 {@link ToolSecurityUtils#checkUrl} 做 SSRF 校验（仅允许 http/https，
 * 拒绝内网/环回/云元数据地址），保存文件名通过 {@link ToolSecurityUtils#checkFilePath} 做路径穿越校验。
 * </p>
 *
 * @author endcy
 */
public class ResourceDownloadTool {

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url,
                                   @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {
        String urlCheck = ToolSecurityUtils.checkUrl(url);
        if (urlCheck != null) {
            return urlCheck;
        }
        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String pathCheck = ToolSecurityUtils.checkFilePath(fileDir, fileName);
        if (pathCheck != null) {
            return pathCheck;
        }
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 使用 Hutool 的 downloadFile 方法下载资源
            HttpUtil.downloadFile(url, new File(filePath));
            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
