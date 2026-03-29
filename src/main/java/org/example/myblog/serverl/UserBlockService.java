package org.example.myblog.serverl;

public interface UserBlockService {

    void block(Long blockerId, Long blockedId);

    void unblock(Long blockerId, Long blockedId);

    boolean isBlocked(Long blockerId, Long blockedId);
}
