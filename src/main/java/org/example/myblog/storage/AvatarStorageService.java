package org.example.myblog.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 用户头像存储：本地磁盘或对象存储，返回值写入 user_profile.avatar_url。
 * 完整 HTTPS 地址可直接被客户端使用；以 / 开头的相对路径需配合 API 域名拼接。
 */
public interface AvatarStorageService extends AutoCloseable {

    String saveAvatar(MultipartFile file, Long userId) throws IOException;

    @Override
    default void close() {
        // 本地存储无资源释放
    }
}
