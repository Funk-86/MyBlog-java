package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 敏感词 sensitive_word
 */
@Data
@Entity
@Table(name = "sensitive_word")
public class SensitiveWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String word;

    /**
     * 1提示,2拦截,3人工审核
     */
    @Column(nullable = false)
    private Integer level;

    /**
     * 0启用,1禁用
     */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

