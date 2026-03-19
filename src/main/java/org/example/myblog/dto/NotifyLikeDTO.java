package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞通知：谁 赞了你的帖子/回复
 */
@Data
public class NotifyLikeDTO {
    private Long id;
    private Long actorId;       // 点赞人
    private String actorName;   // 点赞人昵称
    private String actorAvatar;
    private boolean isPost;     // true=赞帖子, false=赞评论
    private Long targetId;      // postId 或 commentId
    private Long postId;        // 帖子 ID（用于跳转）
    private Long commentId;     // 被赞的评论 ID（赞评论时有值，用于定位）
    private String postTitle;   // 帖子标题
    private String firstImageUrl; // 帖子首图
    private String commentContent; // 被赞的评论内容（赞评论时有值）
    private LocalDateTime createdAt;
}
