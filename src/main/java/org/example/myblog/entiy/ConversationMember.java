package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话成员 conversation_member
 */
@Data
@Entity
@Table(name = "conversation_member",
        uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "user_id"}))
public class ConversationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 0普通,1管理员
     */
    @Column(nullable = false)
    private Integer role;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;
}

