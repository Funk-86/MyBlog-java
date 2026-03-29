package org.example.myblog.entiy;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏夹（MyBatis 用，非 JPA 实体）
 */
@Data
public class FavoriteFolder {

    private Long id;
    private Long userId;
    private String name;
    private Integer isDefault = 0;
    private Integer sortOrder = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
