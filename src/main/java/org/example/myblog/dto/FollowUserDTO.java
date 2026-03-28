package org.example.myblog.dto;

import lombok.Data;

/**
 * 我关注的人（用于发私信选择联系人）
 */
@Data
public class FollowUserDTO {
    private Long id;
    /** 登录名，昵称缺失时前端可展示 */
    private String username;
    private String nickname;
    private String avatarUrl;
}
