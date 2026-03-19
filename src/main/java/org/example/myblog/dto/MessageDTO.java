package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String senderName;

    private String senderAvatar;

    private Integer contentType;

    private String content;

    private LocalDateTime createdAt;

    /**
     * 是否为当前用户自己发送的消息（便于前端判断左右气泡）
     */
    private Boolean mine;
}

