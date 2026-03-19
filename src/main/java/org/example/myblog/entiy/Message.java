package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息 message
 */
@Data
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    /**
     * 0文本,1图片,2视频,3系统
     */
    @Column(name = "content_type", nullable = false)
    private Integer contentType;

    @Lob
    @Column(nullable = false)
    private String content;

    /**
     * 0正常,1撤回,2删除
     */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

