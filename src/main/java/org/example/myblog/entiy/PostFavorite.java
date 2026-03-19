package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子收藏 post_favorite
 */
@Data
@Entity
@Table(name = "post_favorite",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 收藏人
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 被收藏的帖子
     */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * 收藏时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

