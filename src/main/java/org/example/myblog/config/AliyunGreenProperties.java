package org.example.myblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云内容安全 Green 配置
 */
@Component
@ConfigurationProperties(prefix = "aliyun.green")
public class AliyunGreenProperties {

    private String accessKeyId;
    private String accessKeySecret;
    /**
     * 与内容安全控制台接入地域一致，例如 cn-shenzhen、cn-shanghai。
     * 香港/海外机房优先尝试 cn-shenzhen（深圳），跨境路由通常优于华东。
     */
    private String regionId;

    /**
     * 文本审核 Plus 接入域名（不含 https://），如 green-cip.cn-shenzhen.aliyuncs.com。
     * 不填则自动：green-cip.{region-id}.aliyuncs.com
     */
    private String endpoint;

    /** 连接阿里云 API 超时（毫秒），过短易在弱网/跨境下出现 Connect timed out */
    private int connectTimeoutMs = 15000;
    /** 读取响应超时（毫秒） */
    private int readTimeoutMs = 20000;
    /** 文本审核失败（网络类）时重试次数，含首次请求 */
    private int textMaxAttempts = 3;
    /** 图片审核详细日志开关（关闭可显著减少日志量与内存抖动） */
    private boolean imageVerboseLog = false;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /** RegionId，未配置时默认深圳（便于香港/海外连通） */
    public String getRegionIdOrDefault() {
        if (regionId != null && !regionId.isBlank()) {
            return regionId.trim();
        }
        return "cn-shenzhen";
    }

    /**
     * 文本审核增强版 Plus 的接入 host（文档中的 green-cip.*.aliyuncs.com）。
     */
    public String resolveGreenCipEndpointHost() {
        if (endpoint != null && !endpoint.isBlank()) {
            String e = endpoint.trim();
            if (e.startsWith("https://")) {
                e = e.substring(8);
            } else if (e.startsWith("http://")) {
                e = e.substring(7);
            }
            int slash = e.indexOf('/');
            if (slash > 0) {
                e = e.substring(0, slash);
            }
            return e;
        }
        return "green-cip." + getRegionIdOrDefault() + ".aliyuncs.com";
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs > 0 ? connectTimeoutMs : 15000;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs > 0 ? readTimeoutMs : 20000;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public int getTextMaxAttempts() {
        return textMaxAttempts > 0 ? textMaxAttempts : 3;
    }

    public void setTextMaxAttempts(int textMaxAttempts) {
        this.textMaxAttempts = textMaxAttempts;
    }

    public boolean isImageVerboseLog() {
        return imageVerboseLog;
    }

    public void setImageVerboseLog(boolean imageVerboseLog) {
        this.imageVerboseLog = imageVerboseLog;
    }
}

