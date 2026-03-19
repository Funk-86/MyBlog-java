package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞 comment_like
 */
@Data
@Entity
@Table(name = "comment_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"}))
public class CommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
