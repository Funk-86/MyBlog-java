package org.example.myblog.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 个人空间信息 DTO
 */
@Data
public class UserSpaceDTO {

    private Long id;
    private String username;
    private String email;

    /** 0正常 1封禁 2注销；客户端用于展示封禁提示 */
    private Integer status;
    /** 封禁截止时间，null 表示永久封禁 */
    private LocalDateTime bannedUntil;

    // 资料
    private String nickname;
    private String avatarUrl;
    private String bio;
    /** 0未知,1男,2女 */
    private Integer gender;

    // 统计信息
    private Long followCount;      // 我关注了多少人
    private Long fansCount;        // 有多少人关注我
    private Long postCount;        // 我的发帖数
    private Long replyCount;       // 我的回复数
    private Long favoriteCount;    // 收藏数量
    private Long likeCount;        // 点赞数量

    /** 注册时间（用于计算吧龄） */
    private String createdAt;
}

