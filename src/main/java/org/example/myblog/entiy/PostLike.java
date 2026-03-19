package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子点赞 post_like
 */
@Data
@Entity
@Table(name = "post_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}))
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

