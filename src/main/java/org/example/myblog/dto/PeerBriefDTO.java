package org.example.myblog.dto;

import lombok.Data;

/**
 * 会话对端用户展示信息（避免 Map 结果集列名大小写不一致导致取不到昵称/头像）
 */
@Data
public class PeerBriefDTO {

    private Long id;

    private String username;

    /** COALESCE(profile.nickname, user.username) */
    private String displayName;

    private String avatarUrl;
}
