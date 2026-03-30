package org.example.myblog.config;

import org.example.myblog.storage.AliyunOssAvatarStorageService;
import org.example.myblog.storage.AvatarStorageService;
import org.example.myblog.storage.LocalAvatarStorageService;
import org.example.myblog.storage.S3CompatibleAvatarStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 头像存储：local（默认）| aliyun-oss | s3（R2/S3/MinIO 等）
 */
@Configuration
public class AvatarStorageConfiguration {

    @Bean(destroyMethod = "close")
    public AvatarStorageService avatarStorageService(
            @Value("${myblog.storage.mode:local}") String mode,
            @Value("${upload.base-path:.}") String uploadBasePath,
            @Value("${myblog.storage.aliyun.endpoint:}") String aliyunEndpoint,
            @Value("${myblog.storage.aliyun.access-key-id:}") String aliyunAccessKeyId,
            @Value("${myblog.storage.aliyun.access-key-secret:}") String aliyunAccessKeySecret,
            @Value("${myblog.storage.aliyun.bucket:}") String aliyunBucket,
            @Value("${myblog.storage.aliyun.public-base-url:}") String aliyunPublicBaseUrl,
            @Value("${myblog.storage.aliyun.key-prefix:user_img/}") String aliyunKeyPrefix,
            @Value("${myblog.storage.s3.endpoint:}") String s3Endpoint,
            @Value("${myblog.storage.s3.region:us-east-1}") String s3Region,
            @Value("${myblog.storage.s3.access-key:}") String s3AccessKey,
            @Value("${myblog.storage.s3.secret-key:}") String s3SecretKey,
            @Value("${myblog.storage.s3.bucket:}") String s3Bucket,
            @Value("${myblog.storage.s3.public-base-url:}") String s3PublicBaseUrl,
            @Value("${myblog.storage.s3.key-prefix:user_img/}") String s3KeyPrefix,
            @Value("${myblog.storage.s3.path-style:false}") boolean s3PathStyle) {
        String m = mode == null ? "local" : mode.trim().toLowerCase();
        return switch (m) {
            case "aliyun-oss", "oss" -> new AliyunOssAvatarStorageService(
                    aliyunEndpoint,
                    aliyunAccessKeyId,
                    aliyunAccessKeySecret,
                    aliyunBucket,
                    aliyunPublicBaseUrl,
                    aliyunKeyPrefix);
            case "s3", "r2" -> new S3CompatibleAvatarStorageService(
                    s3Endpoint,
                    s3Region,
                    s3AccessKey,
                    s3SecretKey,
                    s3Bucket,
                    s3PublicBaseUrl,
                    s3KeyPrefix,
                    s3PathStyle);
            default -> new LocalAvatarStorageService(uploadBasePath);
        };
    }
}
