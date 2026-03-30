package org.example.myblog.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * 头像：使用 {@link AliyunOssClientFacade} 上传到 user_img/ 前缀。
 */
public class AliyunOssAvatarStorageService implements AvatarStorageService {

    private final AliyunOssClientFacade facade;
    private final String objectKeyPrefix;

    public AliyunOssAvatarStorageService(AliyunOssClientFacade facade, String objectKeyPrefix) {
        this.facade = facade;
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
        return facade.uploadMultipartToKey(file, key);
    }

    @Override
    public void close() {
        // AliyunOssClientFacade 由 Spring 单例关闭
    }
}
