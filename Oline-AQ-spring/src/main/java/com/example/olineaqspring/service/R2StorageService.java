package com.example.olineaqspring.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

// 🔧 R2 连接问题排查：
// 1. 确认 Endpoint 格式正确
// 2. 确认 Access Key ID / Secret Access Key 在 R2 面板生成且未过期
// 3. 确认 Bucket Name 在 R2 中已创建
// 4. 若仍有问题，查看后端控制台日志获取完整异常堆栈
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;

@Service
public class R2StorageService {
    private final ConfigService configService;

    public R2StorageService(ConfigService configService) {
        this.configService = configService;
    }

    public boolean isEnabled() {
        return "r2".equals(configService.get("storage.type"));
    }

    private S3Client buildClient() {
        String endpoint = configService.get("r2.endpoint");
        String accessKey = configService.get("r2.access_key_id");
        String secretKey = configService.get("r2.secret_access_key");

        if (endpoint == null || accessKey == null || secretKey == null) {
            throw new RuntimeException("R2 存储配置不完整，请先在系统设置中配置");
        }

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .region(Region.of("auto"))
                .forcePathStyle(true)
                .build();
    }

    public void upload(String key, byte[] bytes, String contentType) {
        String bucket = configService.get("r2.bucket_name");
        if (bucket == null) {
            throw new RuntimeException("R2 存储配置不完整，请先在系统设置中配置");
        }
        S3Client s3 = buildClient();
        try {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();
            s3.putObject(putReq, RequestBody.fromBytes(bytes));
        } finally {
            s3.close();
        }
    }

    public String testConnection() {
        String bucket = configService.get("r2.bucket_name");
        if (bucket == null) {
            return "缺少 Bucket Name";
        }
        S3Client s3 = buildClient();
        try {
            s3.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).maxKeys(1).build());
            return "ok";
        } catch (Exception e) {
            return "连接失败: " + e.getMessage();
        } finally {
            s3.close();
        }
    }
}
