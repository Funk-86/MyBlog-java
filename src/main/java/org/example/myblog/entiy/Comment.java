package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论 comment
 */
@Data
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "root_id")
    private Long rootId;

    /**
     * 0正常,1删除,2屏蔽
     */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "is_pinned")
    private Integer isPinned;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 下面三个字段用于评论列表展示的用户信息，
     * MyBatis 联表查询时填充，不参与持久化。
     */
    @Transient
    private String username;

    @Transient
    private String nickname;

    @Transient
    private String avatarUrl;
}

