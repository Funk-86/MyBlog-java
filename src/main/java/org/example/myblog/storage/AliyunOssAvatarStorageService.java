package org.example.myblog.storage;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.UUID;

/**
 * 阿里云 OSS：需在控制台创建 Bucket，并配置公开读或 CDN 域名到 {@link #publicBaseUrl}
 */
public class AliyunOssAvatarStorageService implements AvatarStorageService {

    private final OSS ossClient;
    private final String bucket;
    /** 不含末尾 /，例如 https://your-bucket.oss-cn-hangzhou.aliyuncs.com 或自定义域名 https://img.example.com */
    private final String publicBaseUrl;
    private final String objectKeyPrefix;

    public AliyunOssAvatarStorageService(
            String endpoint,
            String accessKeyId,
            String accessKeySecret,
            String bucket,
            String publicBaseUrl,
            String objectKeyPrefix) {
        if (endpoint == null || endpoint.isBlank() || accessKeyId == null || accessKeySecret == null
                || bucket == null || bucket.isBlank() || publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "阿里云 OSS 未配置完整：myblog.storage.aliyun.endpoint / access-key-id / access-key-secret / bucket / public-base-url");
        }
        this.ossClient = new OSSClient(endpoint.trim(), accessKeyId.trim(), accessKeySecret.trim());
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

        ObjectMetadata meta = new ObjectMetadata();
        long len = file.getSize();
        if (len >= 0) {
            meta.setContentLength(len);
        }
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            meta.setContentType(file.getContentType());
        }
        IOException lastIo = null;
        OSSException lastOss = null;
        ClientException lastClient = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (InputStream in = file.getInputStream()) {
                ossClient.putObject(bucket, key, in, meta);
                return publicBaseUrl + "/" + key;
            } catch (OSSException e) {
                lastOss = e;
                if (!isTransientOss(e) || attempt == maxAttempts) {
                    throw e;
                }
                sleepBackoff(attempt);
            } catch (ClientException e) {
                lastClient = e;
                if (attempt == maxAttempts) {
                    throw e;
                }
                sleepBackoff(attempt);
            } catch (IOException e) {
                lastIo = e;
                if (!isTransientIo(e) || attempt == maxAttempts) {
                    throw e;
                }
                sleepBackoff(attempt);
            }
        }
        if (lastOss != null) {
            throw lastOss;
        }
        if (lastClient != null) {
            throw lastClient;
        }
        if (lastIo != null) {
            throw lastIo;
        }
        throw new IOException("OSS upload failed after retries");
    }

    private static boolean isTransientIo(IOException e) {
        if (e instanceof SocketTimeoutException || e instanceof SocketException) {
            return true;
        }
        String m = e.getMessage();
        return m != null && (m.contains("reset") || m.contains("timed out") || m.contains("Broken pipe"));
    }

    /** SDK 2.x 以 ErrorCode 为准；服务端/限流/超时类可重试 */
    private static boolean isTransientOss(OSSException e) {
        String c = e.getErrorCode();
        if (c == null) {
            return true;
        }
        switch (c) {
            case "InternalError":
            case "ServiceUnavailable":
            case "SlowDown":
            case "RequestTimeout":
            case "OperationTimeout":
            case "MaxRetryReached":
                return true;
            case "AccessDenied":
            case "NoSuchBucket":
            case "InvalidAccessKeyId":
            case "SignatureDoesNotMatch":
            case "InvalidArgument":
                return false;
            default:
                return false;
        }
    }

    private static void sleepBackoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
