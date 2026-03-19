package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 举报 report
 */
@Data
@Entity
@Table(name = "report")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    /**
     * 1用户,2帖子
     */
    @Column(name = "target_type", nullable = false)
    private Integer targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * 预定义原因编码
     */
    @Column(name = "reason_code", nullable = false)
    private Integer reasonCode;

    @Column(length = 500)
    private String description;

    /**
     * 0待处理,1已处理,2忽略
     */
    @Column(nullable = false)
    private Integer status;

    @Column(name = "handler_id")
    private Long handlerId;

    @Column(length = 255)
    private String result;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

