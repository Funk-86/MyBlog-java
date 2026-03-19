package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核记录 content_review_log
 */
@Data
@Entity
@Table(name = "content_review_log")
public class ContentReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 1帖子,2评论
     */
    @Column(name = "content_type", nullable = false)
    private Integer contentType;

    @Column(name = "content_id", nullable = false)
    private Long contentId;

    /**
     * 命中敏感词列表
     */
    @Column(name = "trigger_words", length = 255)
    private String triggerWords;

    /**
     * 0放行,1打回,2人工审核
     */
    @Column(nullable = false)
    private Integer action;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(length = 255)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

