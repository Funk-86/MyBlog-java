package org.example.myblog.storage;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * 阿里云 OSS 通用上传（头像、帖子图、视频），与 {@link AvatarStorageService} 共用同一 Bucket。
 */
public class AliyunOssClientFacade implements AutoCloseable {

    private final OSS ossClient;
    private final String bucket;
    private final String publicBaseUrl;

    public AliyunOssClientFacade(
            String endpoint,
            String accessKeyId,
            String accessKeySecret,
            String bucket,
            String publicBaseUrl) {
        if (endpoint == null || endpoint.isBlank() || accessKeyId == null || accessKeySecret == null
                || bucket == null || bucket.isBlank() || publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "阿里云 OSS 未配置完整：endpoint / access-key-id / access-key-secret / bucket / public-base-url");
        }
        this.ossClient = new OSSClient(endpoint.trim(), accessKeyId.trim(), accessKeySecret.trim());
        this.bucket = bucket.trim();
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
    }

    /** 修正环境变量里误写的 https//、http//（少写冒号会导致前端无法识别为绝对 URL） */
    static String normalizePublicBaseUrl(String publicBaseUrl) {
        String base = publicBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.startsWith("https//")) {
            base = "https://" + base.substring("https//".length());
        } else if (base.startsWith("http//")) {
            base = "http://" + base.substring("http//".length());
        }
        return base;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public String publicUrlForKey(String key) {
        return publicBaseUrl + "/" + key;
    }

    /**
     * 从完整公开 URL 还原 object key（须为本 Bucket publicBaseUrl 前缀）。
     */
    public String keyFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        String u = publicUrl.trim();
        if (u.startsWith("https//")) {
            u = "https://" + u.substring("https//".length());
        } else if (u.startsWith("http//")) {
            u = "http://" + u.substring("http//".length());
        }
        int q = u.indexOf('?');
        if (q >= 0) {
            u = u.substring(0, q);
        }
        if (!u.startsWith(publicBaseUrl)) {
            return null;
        }
        String rest = u.substring(publicBaseUrl.length());
        while (rest.startsWith("/")) {
            rest = rest.substring(1);
        }
        return rest.isEmpty() ? null : rest;
    }

    /**
     * Multipart 上传到指定 key，返回完整 HTTPS URL。
     */
    public String uploadMultipartToKey(MultipartFile file, String key) throws IOException {
        long len = file.getSize();
        String ct = file.getContentType();
        return putObjectWithRetry(() -> file.getInputStream(), len >= 0 ? len : 0, ct, key);
    }

    /**
     * 本地文件上传到指定 key，返回完整 HTTPS URL。
     */
    public String uploadLocalFile(Path localPath, String key, String contentType) throws IOException {
        if (localPath == null || !Files.exists(localPath)) {
            throw new IOException("local file missing");
        }
        long len = Files.size(localPath);
        return putObjectWithRetry(() -> Files.newInputStream(localPath), len, contentType, key);
    }

    @FunctionalInterface
    private interface StreamSupplier {
        InputStream get() throws IOException;
    }

    private String putObjectWithRetry(
            StreamSupplier streamSupplier,
            long contentLength,
            String contentType,
            String key) throws IOException {
        IOException lastIo = null;
        OSSException lastOss = null;
        ClientException lastClient = null;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (InputStream in = streamSupplier.get()) {
                ObjectMetadata m = new ObjectMetadata();
                if (contentLength > 0) {
                    m.setContentLength(contentLength);
                }
                if (contentType != null && !contentType.isBlank()) {
                    m.setContentType(contentType);
                }
                ossClient.putObject(bucket, key, in, m);
                return publicUrlForKey(key);
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

    /** 公网 GET 下载到临时文件（用于 ffmpeg；转码后需再上传 OSS） */
    public static Path downloadHttpToTempFile(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();
        HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        Path tmp = Files.createTempFile("oss-dl-", ".bin");
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(tmp)) {
            in.transferTo(out);
        }
        return tmp;
    }

    private static boolean isTransientIo(IOException e) {
        if (e instanceof SocketTimeoutException || e instanceof SocketException) {
            return true;
        }
        String m = e.getMessage();
        return m != null && (m.contains("reset") || m.contains("timed out") || m.contains("Broken pipe"));
    }

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
