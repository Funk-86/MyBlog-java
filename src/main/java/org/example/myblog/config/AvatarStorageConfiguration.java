package org.example.myblog.config;

import org.example.myblog.storage.AliyunOssAvatarStorageService;
import org.example.myblog.storage.AliyunOssClientFacade;
import org.example.myblog.storage.AvatarStorageService;
import org.example.myblog.storage.LocalAvatarStorageService;
import org.example.myblog.storage.S3CompatibleAvatarStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 头像存储：local（默认）| aliyun-oss | s3（R2/S3/MinIO 等）
 * aliyun-oss/oss 模式下同时注册 {@link AliyunOssClientFacade}，供帖子图/视频上传复用。
 */
@Configuration
public class AvatarStorageConfiguration {

    @Bean(name = "aliyunOssClientFacade", destroyMethod = "close")
    @ConditionalOnProperty(name = "myblog.storage.mode", havingValue = "aliyun-oss")
    public AliyunOssClientFacade aliyunOssClientFacadeAliyun(
            @Value("${myblog.storage.aliyun.endpoint:}") String aliyunEndpoint,
            @Value("${myblog.storage.aliyun.access-key-id:}") String aliyunAccessKeyId,
            @Value("${myblog.storage.aliyun.access-key-secret:}") String aliyunAccessKeySecret,
            @Value("${myblog.storage.aliyun.bucket:}") String aliyunBucket,
            @Value("${myblog.storage.aliyun.public-base-url:}") String aliyunPublicBaseUrl) {
        return new AliyunOssClientFacade(
                aliyunEndpoint, aliyunAccessKeyId, aliyunAccessKeySecret, aliyunBucket, aliyunPublicBaseUrl);
    }

    @Bean(name = "aliyunOssClientFacade", destroyMethod = "close")
    @ConditionalOnProperty(name = "myblog.storage.mode", havingValue = "oss")
    public AliyunOssClientFacade aliyunOssClientFacadeOss(
            @Value("${myblog.storage.aliyun.endpoint:}") String aliyunEndpoint,
            @Value("${myblog.storage.aliyun.access-key-id:}") String aliyunAccessKeyId,
            @Value("${myblog.storage.aliyun.access-key-secret:}") String aliyunAccessKeySecret,
            @Value("${myblog.storage.aliyun.bucket:}") String aliyunBucket,
            @Value("${myblog.storage.aliyun.public-base-url:}") String aliyunPublicBaseUrl) {
        return new AliyunOssClientFacade(
                aliyunEndpoint, aliyunAccessKeyId, aliyunAccessKeySecret, aliyunBucket, aliyunPublicBaseUrl);
    }

    @Bean(destroyMethod = "close")
    public AvatarStorageService avatarStorageService(
            @Value("${myblog.storage.mode:local}") String mode,
            @Value("${upload.base-path:.}") String uploadBasePath,
            @Value("${myblog.storage.aliyun.key-prefix:user_img/}") String aliyunKeyPrefix,
            @Value("${myblog.storage.s3.endpoint:}") String s3Endpoint,
            @Value("${myblog.storage.s3.region:us-east-1}") String s3Region,
            @Value("${myblog.storage.s3.access-key:}") String s3AccessKey,
            @Value("${myblog.storage.s3.secret-key:}") String s3SecretKey,
            @Value("${myblog.storage.s3.bucket:}") String s3Bucket,
            @Value("${myblog.storage.s3.public-base-url:}") String s3PublicBaseUrl,
            @Value("${myblog.storage.s3.key-prefix:user_img/}") String s3KeyPrefix,
            @Value("${myblog.storage.s3.path-style:false}") boolean s3PathStyle,
            @Autowired(required = false) AliyunOssClientFacade ossFacade) {
        String m = mode == null ? "local" : mode.trim().toLowerCase();
        return switch (m) {
            case "aliyun-oss", "oss" -> {
                if (ossFacade == null) {
                    throw new IllegalStateException("aliyun-oss/oss 模式需要 AliyunOssClientFacade Bean");
                }
                yield new AliyunOssAvatarStorageService(ossFacade, aliyunKeyPrefix);
            }
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
