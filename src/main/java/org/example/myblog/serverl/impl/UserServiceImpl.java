package org.example.myblog.serverl.impl;

import org.example.myblog.dto.FollowUserDTO;
import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.User;

import java.util.List;
import org.example.myblog.mapper.UserMapper;
import org.example.myblog.mapper.UserFollowMapper;
import org.example.myblog.serverl.EmailCodeService;
import org.example.myblog.serverl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired(required = false)
    private UserFollowMapper userFollowMapper;

    @Override
    public User login(String account, String rawPassword) {
        String passwordHash = rawPassword;
        // 账号含 @ 则按邮箱登录，否则按用户名登录
        if (account != null && account.contains("@")) {
            return userMapper.selectByEmailAndPassword(account.trim(), passwordHash);
        }
        return userMapper.selectByUsernameAndPassword(account != null ? account.trim() : "", passwordHash);
    }

    @Override
    public User registerByEmail(String email, String code, String rawPassword) {
        // 校验验证码
        boolean ok = emailCodeService.verifyRegisterCode(email, code);
        if (!ok) {
            return null;
        }
        // 判断邮箱是否已注册
        User exist = userMapper.selectByEmail(email);
        if (exist != null) {
            return null;
        }
        User user = new User();
        // 简单处理：用户名直接使用邮箱
        user.setUsername(email);
        user.setEmail(email);
        // 同样这里直接使用原始密码作为 passwordHash
        user.setPasswordHash(rawPassword);
        // 0=普通用户
        user.setRole(0);
        // 0=正常
        user.setStatus(0);
        userMapper.insert(user);
        return user;
    }

    @Override
    public User createUserByAdmin(String username, String email, String rawPassword, Integer role) {
        if (username == null || username.trim().isEmpty()) return null;
        if (email == null || email.trim().isEmpty()) return null;
        if (rawPassword == null || rawPassword.isEmpty()) return null;
        if (userMapper.selectByUsername(username.trim()) != null) return null;
        if (userMapper.selectByEmail(email.trim()) != null) return null;
        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPasswordHash(rawPassword);
        user.setSalt(null);
        user.setRole(role != null && role == 1 ? 1 : 0);
        user.setStatus(0);
        userMapper.insert(user);
        return user;
    }

    @Override
    public boolean resetPasswordByEmail(String email, String code, String newRawPassword) {
        boolean ok = emailCodeService.verifyResetCode(email, code);
        if (!ok) {
            return false;
        }
        String passwordHash = newRawPassword;
        int rows = userMapper.updatePasswordByEmail(email, passwordHash, null);
        return rows > 0;
    }

    @Override
    public UserSpaceDTO getUserSpace(Long userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectUserSpace(userId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null || userFollowMapper == null) {
            return false;
        }
        Integer status = userFollowMapper.selectStatus(followerId, followeeId);
        return status != null && status == 0;
    }

    @Override
    public boolean follow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null || userFollowMapper == null) {
            return false;
        }
        return userFollowMapper.follow(followerId, followeeId) > 0;
    }

    @Override
    public boolean unfollow(Long followerId, Long followeeId) {
        if (followerId == null || followeeId == null || userFollowMapper == null) {
            return false;
        }
        return userFollowMapper.unfollow(followerId, followeeId) > 0;
    }

    @Override
    public List<FollowUserDTO> listFollowing(Long userId, int limit) {
        if (userId == null || userFollowMapper == null) {
            return List.of();
        }
        if (limit <= 0) limit = 50;
        return userFollowMapper.listFollowees(userId, limit);
    }

    @Override
    public List<FollowUserDTO> listFollowers(Long userId, int limit) {
        if (userId == null || userFollowMapper == null) {
            return List.of();
        }
        if (limit <= 0) limit = 100;
        return userFollowMapper.listFollowers(userId, limit);
    }
}
