package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系 user_follow
 */
@Data
@Entity
@Table(name = "user_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followee_id"}))
public class UserFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followee_id", nullable = false)
    private Long followeeId;

    /**
     * 0正常,1取关
     */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

