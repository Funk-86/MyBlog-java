package org.example.myblog.serverl;

import org.example.myblog.dto.FollowUserDTO;

import java.util.List;

public interface UserBlockService {

    void block(Long blockerId, Long blockedId);

    void unblock(Long blockerId, Long blockedId);

    boolean isBlocked(Long blockerId, Long blockedId);

    List<FollowUserDTO> listBlockedUsers(Long blockerId, int limit);
}
