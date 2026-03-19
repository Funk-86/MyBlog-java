package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资料 user_profile
 */
@Data
@Entity
@Table(name = "user_profile")
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(length = 255)
    private String bio;

    /**
     * 0未知,1男,2女
     */
    private Integer gender;

    private LocalDate birthday;

    @Column(length = 100)
    private String location;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}