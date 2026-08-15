package com.endcy.ai.energy.utils;

import cn.hutool.core.io.FileUtil;
import org.junit.Test;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件路径解析测试
 *
 * @author endcy
 * @date 2025/10/9 21:44:08
 */
public class FileResolveUtilTest {

    @Test
    public void success_resolve_file_path() {
        // 使用相对路径指向项目内资源目录
        File[] subFiles = FileUtil.ls("src/main/resources/document-exp");
        for (File subFile : subFiles) {
            if (!FileUtil.isDirectory(subFile)) {
                continue;
            }
            String docInfoType = FileUtil.getName(subFile);
            File[] docs = subFile.listFiles();
            Resource[] resources = new Resource[docs.length];
            for (int i = 0; i < docs.length; i++) {
                File fullPath = docs[i];
                resources[i] = new PathResource(fullPath.toPath());
            }
            System.out.println(docInfoType);
            System.out.println();
            System.out.println(resources);
        }
    }

    @Test
    public void success_resolve_json() {
        String tmp = "根据搜索结果，特斯拉Model Y 2023款高性能全轮驱动版的上市日期最可能为**2023年10月1日**。该信息源自多个来源，均指向2023年10月1日是2023款Model Y系列的正式上市日期，高性能全轮驱动版作为其中一款配置同步推出。\n" +
                "\n" +
                "{\n" +
                "\"launchDate\": \"2023-10-01\",\n" +
                "\"url\": \"https://new.qq.com/rain/a/20231001A01U9Z00\"\n" +
                "}";
        String content = extractJsonFromText(tmp);
        System.out.println(content);
    }

    public static String extractJsonFromText(String mixedText) {
        // 匹配{...}结构的正则表达式
        String regex = "\\{[^}]*}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mixedText);

        if (matcher.find()) {
            String potentialJson = matcher.group();
            try {
                // 尝试解析提取到的字符串
                return potentialJson;
            } catch (Exception e) {
                System.out.println("提取的字符串不是有效JSON: " + potentialJson);
            }
        }
        return null;
    }
}
