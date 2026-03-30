package org.example.myblog.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 容器本地 user_img 目录（需持久卷或仅开发环境）
 */
public class LocalAvatarStorageService implements AvatarStorageService {

    private final String uploadBasePath;

    public LocalAvatarStorageService(String uploadBasePath) {
        this.uploadBasePath = uploadBasePath == null || uploadBasePath.isEmpty() ? "." : uploadBasePath;
    }

    @Override
    public String saveAvatar(MultipartFile file, Long userId) throws IOException {
        Path base = Paths.get(uploadBasePath).toAbsolutePath().normalize();
        Path uploadDir = base.resolve("user_img");
        Files.createDirectories(uploadDir);

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = userId + "_" + UUID.randomUUID() + ext;
        Path targetPath = uploadDir.resolve(fileName);
        file.transferTo(targetPath);
        return "/user_img/" + fileName;
    }
}
