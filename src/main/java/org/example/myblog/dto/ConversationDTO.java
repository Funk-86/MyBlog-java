package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDTO {

    private Long conversationId;

    private Long peerId;

    private String peerName;

    private String peerAvatar;

    private String lastMsgContent;

    private LocalDateTime lastMsgTime;

    private Integer unreadCount;
}

