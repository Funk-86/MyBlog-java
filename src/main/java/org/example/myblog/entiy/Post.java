package org.example.myblog.entiy;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子 post
 */
@Data
@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 帖子标题
     */
    @Column(name = "title", length = 255)
    private String title;

    /**
     * 主分区 ID
     */
    @Column(name = "category_id_1")
    private Long categoryId1;

    /**
     * 副分区 ID
     */
    @Column(name = "category_id_2")
    private Long categoryId2;

    @Lob
    private String content;

    /**
     * 0文字,1图文,2视频
     */
    @Column(nullable = false)
    private Integer type;

    /**
     * 0正常,1审核中,2删除,3屏蔽
     */
    @Column(nullable = false)
    private Integer status;

    /**
     * 0=所有人看 1=仅个人查看（仅自己可见的帖子不在公开列表展示）
     */
    @Column(name = "visibility", nullable = false)
    private Integer visibility = 0;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount;

    @Column(name = "comment_count", nullable = false)
    private Integer commentCount;

    @Column(name = "share_count", nullable = false)
    private Integer shareCount;

    /**
     * 浏览量（可定期从 Redis 同步）
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;

    /**
     * 收藏数（可定期从 post_favorite 同步）
     */
    @Column(name = "favorite_count")
    private Integer favoriteCount = 0;

    /**
     * 热度分数（定期重算，用于排序）
     */
    @Column(name = "hot_score")
    private Double hotScore = 0.0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 下面三个字段用于前端展示（MyBatis 查询时联表填充），
     * 不参与 JPA 持久化。
     */
    @Transient
    private String username;

    @Transient
    private String nickname;

    @Transient
    private String avatarUrl;

    /**
     * 首张图片地址（来自 post_media.url）
     */
    @Transient
    private String firstImageUrl;

    /**
     * 当前帖子所有图片地址，使用英文逗号拼接，
     * 由 MyBatis 使用 GROUP_CONCAT(url) 聚合得到。
     * 前端可根据需要拆分为最多 9 张图片。
     */
    @Transient
    private String imageUrls;

    /** 主分区名称（联表 category 填充，列表展示用） */
    @Transient
    private String categoryName1;

    /** 副分区名称（联表 category 填充） */
    @Transient
    private String categoryName2;

    /** 首条视频地址（type=2 时由列表/详情查询填充） */
    @Transient
    private String firstVideoUrl;

    /** 视频封面图地址（列表展示用，image 可渲染） */
    @Transient
    private String firstVideoCoverUrl;

    /** 视频时长（秒），列表展示用 */
    @Transient
    private Integer firstVideoDuration;

    /**
     * 话题名称列表，使用英文逗号拼接，
     * 由 MyBatis 在 topic 关联查询时聚合得到。
     */
    @Transient
    private String topicNames;
}