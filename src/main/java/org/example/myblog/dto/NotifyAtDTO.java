package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @我的 通知
 */
@Data
public class NotifyAtDTO {
    private Long id;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private Long postId;
    private Long commentId;
    private String content;
    private String postTitle;
    private String firstImageUrl;
    private LocalDateTime createdAt;
}
