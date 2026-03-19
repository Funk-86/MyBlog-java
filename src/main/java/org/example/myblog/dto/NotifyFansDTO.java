package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 粉丝通知：谁 关注了你
 */
@Data
public class NotifyFansDTO {
    private Long id;
    private Long actorId;
    private String actorName;
    private String actorAvatar;
    private LocalDateTime createdAt;
    /** 当前用户是否已回关（是否已关注该粉丝） */
    private Boolean followedByMe;
}
