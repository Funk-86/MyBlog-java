package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 回复通知：谁 回复了你的xxx
 */
@Data
public class NotifyReplyDTO {
    private Long id;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private Long postId;
    private Long commentId;      // 对方的回复评论 id（用于点赞、回复）
    private Long rootId;
    private String content;      // 对方回复的内容
    private LocalDateTime createdAt;
    private String myUserName;   // 你的昵称（被回复人）
    private String myCommentContent; // 你的原回复内容
    private Boolean myCommentDeleted; // 你的原回复是否已删除
    private String postTitle;    // 帖子标题
    private String firstImageUrl; // 帖子首图
    private Boolean postDeleted; // 帖子是否已删除
    /** true=评论了你的帖子（首评），false=回复了你的评论 */
    private Boolean isPostComment;
}
