package org.example.myblog.storage;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * S3 兼容协议：Cloudflare R2、AWS S3、MinIO、腾讯云 COS（S3 兼容端点）等。
 * R2：endpoint 填 https://&lt;账户ID&gt;.r2.cloudflarestorage.com，region 可填 us-east-1，
 * public-base-url 填公开访问前缀（如 R2 公共开发域名或自定义域名）。
 */
public class S3CompatibleAvatarStorageService implements AvatarStorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;
    private final String objectKeyPrefix;

    public S3CompatibleAvatarStorageService(
            String endpoint,
            String region,
            String accessKey,
            String secretKey,
            String bucket,
            String publicBaseUrl,
            String objectKeyPrefix,
            boolean pathStyleAccess) {
        if (endpoint == null || endpoint.isBlank() || accessKey == null || secretKey == null
                || bucket == null || bucket.isBlank() || publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "S3 兼容存储未配置完整：myblog.storage.s3.endpoint / access-key / secret-key / bucket / public-base-url");
        }
        String reg = region == null || region.isBlank() ? "us-east-1" : region.trim();
        S3Configuration s3conf = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint.trim()))
                .region(Region.of(reg))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim())))
                .serviceConfiguration(s3conf)
                .build();
        this.bucket = bucket.trim();
        String base = publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.publicBaseUrl = base;
        String prefix = objectKeyPrefix == null ? "user_img/" : objectKeyPrefix.trim();
        if (!prefix.isEmpty() && !prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        this.objectKeyPrefix = prefix;
    }

    @Override
    public String saveAvatar(MultipartFile file, Long userId) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = userId + "_" + UUID.randomUUID() + ext;
        String key = objectKeyPrefix + fileName;

        PutObjectRequest.Builder b = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            b.contentType(file.getContentType());
        }
        PutObjectRequest req = b.build();
        try (var in = file.getInputStream()) {
            s3Client.putObject(req, RequestBody.fromInputStream(in, file.getSize()));
        }
        return publicBaseUrl + "/" + key;
    }

    @Override
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
    }
}
