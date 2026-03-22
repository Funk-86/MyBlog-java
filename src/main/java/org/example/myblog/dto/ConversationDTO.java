package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDTO {

    private Long conversationId;

    private Long peerId;

    /** 对端登录名 user.username，用于识别系统账号「系统通知」等 */
    private String peerUsername;

    private String peerName;

    private String peerAvatar;

    private String lastMsgContent;

    private LocalDateTime lastMsgTime;

    private Integer unreadCount;
}

