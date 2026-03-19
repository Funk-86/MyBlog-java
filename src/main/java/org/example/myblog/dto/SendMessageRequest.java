package org.example.myblog.dto;

import lombok.Data;

@Data
public class SendMessageRequest {

    private Long conversationId;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    /**
     * 消息类型：0=文本，1=图片，后续可扩展
     */
    private Integer contentType;
}

