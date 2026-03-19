package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子媒体 post_media
 */
@Data
@Entity
@Table(name = "post_media")
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * 1图片,2视频
     */
    @Column(name = "media_type", nullable = false)
    private Integer mediaType;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(name = "cover_url", length = 255)
    private String coverUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /** 视频时长（秒），仅 media_type=2 时有值 */
    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}