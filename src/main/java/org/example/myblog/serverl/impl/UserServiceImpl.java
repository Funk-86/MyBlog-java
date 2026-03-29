package org.example.myblog.serverl.impl;

import org.example.myblog.constant.UserConstants;
import org.example.myblog.dto.FollowUserDTO;
import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.User;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.myblog.mapper.UserMapper;
import org.example.myblog.mapper.UserFollowMapper;
import org.example.myblog.mapper.UserProfileMapper;
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

    @Autowired
    private UserProfileMapper userProfileMapper;

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
        if (UserConstants.isReservedSystemNoticeName(username)) return null;
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
    public void assertUserCanPostOrComment(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new RuntimeException("USER_NOT_FOUND");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            return;
        }
        LocalDateTime until = u.getBannedUntil();
        LocalDateTime now = LocalDateTime.now();
        if (until != null && now.isAfter(until)) {
            userMapper.updateStatus(userId, 0);
            return;
        }
        String detail;
        if (until == null) {
            detail = "您的账号已被永久封禁，暂时无法发帖或评论，仍可浏览内容。";
        } else {
            long days = ChronoUnit.DAYS.between(now.toLocalDate(), until.toLocalDate());
            if (days < 0) {
                days = 0;
            }
            if (days <= 0) {
                detail = "您的账号仍在封禁中，将于今日内解封，暂时无法发帖或评论，仍可浏览内容。";
            } else {
                detail = "您的账号已被封禁，剩余解封时间还有约 " + days + " 天，暂时无法发帖或评论，仍可浏览内容。";
            }
        }
        throw new RuntimeException("USER_BANNED:" + detail);
    }

    @Override
    public Map<String, Object> updateUserByAdmin(Long id, String username, String email, Integer role,
                                                 String nickname, String bio, String newPassword, String avatarUrl) {
        Map<String, Object> result = new HashMap<>();
        if (id == null) {
            result.put("success", false);
            result.put("message", "用户 id 不能为空");
            return result;
        }
        if (username == null || username.isBlank()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }
        if (email == null || email.isBlank()) {
            result.put("success", false);
            result.put("message", "邮箱不能为空");
            return result;
        }
        User current = userMapper.selectById(id);
        if (current == null) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        String u = username.trim();
        String em = email.trim();
        if (UserConstants.isReservedSystemNoticeName(u)) {
            result.put("success", false);
            result.put("message", "该用户名为系统保留，不可使用");
            return result;
        }
        User otherName = userMapper.selectByUsername(u);
        if (otherName != null && !otherName.getId().equals(id)) {
            result.put("success", false);
            result.put("message", "用户名已被占用");
            return result;
        }
        User otherEmail = userMapper.selectByEmail(em);
        if (otherEmail != null && !otherEmail.getId().equals(id)) {
            result.put("success", false);
            result.put("message", "邮箱已被占用");
            return result;
        }
        int r = role != null && role == 1 ? 1 : 0;
        userMapper.updateUserBasic(id, u, em, r);
        String nick = (nickname == null || nickname.isBlank()) ? null : nickname.trim();
        String bioStr = (bio == null || bio.isBlank()) ? null : bio.trim();
        userProfileMapper.upsertProfile(id, nick, bioStr, null);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            userProfileMapper.upsertAvatar(id, avatarUrl.trim());
        }
        if (newPassword != null && !newPassword.isBlank()) {
            userMapper.updatePasswordById(id, newPassword.trim(), null);
        }
        result.put("success", true);
        return result;
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
