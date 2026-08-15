package com.endcy.ai.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * OSS 上传服务
 *
 * @author endcy
 * @since 2026-08-11
 */
@Slf4j
@Service
public class OssUploadManager {

    @Value("${oss.file.endpoint:}")
    private String endpoint;

    @Value("${oss.file.accessKeyId:}")
    private String accessKeyId;

    @Value("${oss.file.accessKeySecret:}")
    private String accessKeySecret;

    @Value("${oss.file.bucketName:}")
    private String bucketName;

    @Value("${oss.file.domain:}")
    private String domain;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        if (endpoint.isBlank() || accessKeyId.isBlank()) {
            log.warn("OSS 配置缺失，OssUploadManager 不可用");
            return;
        }
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            log.info("OSS 客户端初始化成功 endpoint={} bucket={}", endpoint, bucketName);
        } catch (Exception e) {
            log.error("OSS 客户端初始化失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            try {
                ossClient.shutdown();
            } catch (Exception ignore) {
            }
        }
    }

    public boolean isAvailable() {
        return ossClient != null;
    }

    public String uploadBytes(byte[] data, String fileName, String contentType) {
        if (!isAvailable())
            throw new IllegalStateException("OSS 未配置");
        String objectKey = buildObjectKey(fileName);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            if (contentType != null && !contentType.isBlank()) {
                metadata.setContentType(contentType);
            }
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey,
                    new ByteArrayInputStream(data), metadata);
            PutObjectResult result = ossClient.putObject(putRequest);
            log.info("OSS 上传成功 objectKey={} etag={}", objectKey, result.getETag());
            return buildAccessUrl(objectKey);
        } catch (Exception e) {
            log.error("OSS 上传失败 objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("OSS 上传失败: " + e.getMessage(), e);
        }
    }

    public String uploadFile(File file, String contentType) {
        if (!isAvailable())
            throw new IllegalStateException("OSS 未配置");
        String objectKey = buildObjectKey(file.getName());
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            if (contentType != null && !contentType.isBlank()) {
                metadata.setContentType(contentType);
            }
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey, file, metadata);
            PutObjectResult result = ossClient.putObject(putRequest);
            log.info("OSS 文件上传成功 objectKey={} etag={}", objectKey, result.getETag());
            return buildAccessUrl(objectKey);
        } catch (Exception e) {
            log.error("OSS 文件上传失败 objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("OSS 上传失败: " + e.getMessage(), e);
        }
    }

    public String uploadStream(InputStream inputStream, String fileName, String contentType, long contentLength) {
        if (!isAvailable())
            throw new IllegalStateException("OSS 未配置");
        String objectKey = buildObjectKey(fileName);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            if (contentType != null && !contentType.isBlank()) {
                metadata.setContentType(contentType);
            }
            PutObjectRequest putRequest = new PutObjectRequest(bucketName, objectKey, inputStream, metadata);
            PutObjectResult result = ossClient.putObject(putRequest);
            log.info("OSS 流上传成功 objectKey={} etag={}", objectKey, result.getETag());
            return buildAccessUrl(objectKey);
        } catch (Exception e) {
            log.error("OSS 流上传失败 objectKey={}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("OSS 流上传失败: " + e.getMessage(), e);
        }
    }

    private String buildObjectKey(String originalName) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0)
                ext = originalName.substring(dot);
        }
        return "agent/files/" + date + "/" + UUID.randomUUID() + ext;
    }

    private String buildAccessUrl(String objectKey) {
        String d = domain != null ? domain.trim() : "";
        if (d.isEmpty()) {
            return String.format("https://%s.%s/%s", bucketName, endpoint.replace("https://", "").replace("http://", ""), objectKey);
        }
        if (!d.endsWith("/"))
            d = d + "/";
        return d + objectKey;
    }
}
