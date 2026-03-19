package org.example.myblog.dto;

import lombok.Data;

/**
 * 我关注的人（用于发私信选择联系人）
 */
@Data
public class FollowUserDTO {
    private Long id;
    private String nickname;
    private String avatarUrl;
}
