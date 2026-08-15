package com.endcy.ai.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 工具调用安全校验工具类。
 * <p>
 * 用于 Agent 可调用工具（文件操作、资源下载、网页抓取等）在接收 LLM 生成的参数时，
 * 对 URL、文件路径等做安全校验，防止 SSRF、路径遍历、命令注入等攻击。
 * 由于这些工具的参数直接来自大模型输出，不可完全信任，必须做防御性校验。
 * </p>
 *
 * @author endcy
 * @since 2026/08/15
 */
@Slf4j
public final class ToolSecurityUtils {

    private ToolSecurityUtils() {
    }

    /**
     * 校验 URL 是否安全可访问（防 SSRF）。
     * <p>规则：</p>
     * <ul>
     *   <li>仅允许 http / https 协议</li>
     *   <li>host 不能为空</li>
     *   <li>禁止访问环回地址（127.0.0.0/8、::1、localhost）</li>
     *   <li>禁止访问内网地址段（10/8、172.16/12、192.168/16、169.254/16 云元数据）</li>
     * </ul>
     *
     * @param url 待校验的 URL
     * @return 校验通过返回 null，否则返回拒绝原因描述
     */
    public static String checkUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "URL 为空";
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return "URL 格式非法";
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return "URL 缺少协议（仅允许 http/https）";
        }
        String lowerScheme = scheme.toLowerCase();
        if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
            return "禁止的协议: " + scheme;
        }
        String host = uri.getHost();
        if (StrUtil.isBlank(host)) {
            return "URL 缺少主机名";
        }
        if (isBlockedHost(host)) {
            return "禁止访问内网或环回地址: " + host;
        }
        return null;
    }

    /**
     * 判断主机名是否属于被禁止访问的地址（环回/内网/链路本地）。
     */
    private static boolean isBlockedHost(String host) {
        String lowerHost = host.toLowerCase();
        if ("localhost".equals(lowerHost) || lowerHost.endsWith(".localhost")) {
            return true;
        }
        // 直接解析 IPv6 环回 / 链路本地字面量
        if (lowerHost.equals("::1") || lowerHost.startsWith("[::1]")) {
            return true;
        }
        if (lowerHost.startsWith("fe80:") || lowerHost.startsWith("169.254.")) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()) {
                return true;
            }
        } catch (UnknownHostException e) {
            // 无法解析的主机名按安全处理，直接拒绝（避免 DNS rebinding 等绕过手段）
            log.warn("无法解析主机名，拒绝访问: {}", host);
            return true;
        }
        return false;
    }

    /**
     * 校验文件路径是否安全落在指定基础目录内（防路径遍历）。
     * <p>文件名不得包含 {@code ..} 穿越段，规范化后的绝对路径必须仍以 baseDir 为前缀。</p>
     *
     * @param baseDir  基础目录（绝对路径）
     * @param fileName 用户提供的文件名（可能包含相对路径）
     * @return 校验通过返回 null，否则返回拒绝原因描述
     */
    public static String checkFilePath(String baseDir, String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return "文件名为空";
        }
        String raw = fileName.replace('\\', '/');
        // 禁止路径穿越段
        for (String segment : raw.split("/")) {
            if ("..".equals(segment)) {
                return "非法文件路径（目录穿越）: " + fileName;
            }
        }
        Path base = Paths.get(baseDir).toAbsolutePath().normalize();
        Path target = base.resolve(raw).normalize();
        if (!target.startsWith(base)) {
            return "非法文件路径（越出允许目录）: " + fileName;
        }
        return null;
    }
}
