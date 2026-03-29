package org.example.myblog.serverl.impl;

import org.example.myblog.mapper.UserBlockMapper;
import org.example.myblog.serverl.UserBlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserBlockServiceImpl implements UserBlockService {

    @Autowired
    private UserBlockMapper userBlockMapper;

    @Override
    @Transactional
    public void block(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new IllegalArgumentException("参数无效");
        }
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("不能拉黑自己");
        }
        userBlockMapper.insertIgnore(blockerId, blockedId);
    }

    @Override
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) return;
        userBlockMapper.delete(blockerId, blockedId);
    }

    @Override
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) return false;
        return userBlockMapper.countPair(blockerId, blockedId) > 0;
    }
}
