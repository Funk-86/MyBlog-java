package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户表 user
 */
@Data
@Entity
@Table(name = "`user`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String salt;

    @Column(length = 100, unique = true)
    private String email;

    @Column(length = 20, unique = true)
    private String mobile;

    /**
     * 0=普通用户,1=管理员
     */
    @Column(nullable = false)
    private Integer role;

    /**
     * 0=正常,1=封禁,2=注销
     */
    @Column(nullable = false)
    private Integer status;

    /** 封禁截止时间，仅当 status=1 时有效；null 表示永久封禁 */
    @Column(name = "banned_until")
    private LocalDateTime bannedUntil;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}