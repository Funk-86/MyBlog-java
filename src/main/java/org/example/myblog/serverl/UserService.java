package org.example.myblog.serverl;

import org.example.myblog.dto.FollowUserDTO;
import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.User;

import java.util.List;

public interface UserService {

    /**
     * 用户登录（支持账号 username 或邮箱 email）
     *
     * @param account      账号（username）或邮箱
     * @param rawPassword  原始密码（未加密）
     * @return 登录成功返回用户信息，失败返回 null
     */
    User login(String account, String rawPassword);

    /**
     * 通过邮箱 + 验证码注册
     *
     * @return 注册成功返回新用户，失败返回 null
     */
    User registerByEmail(String email, String code, String rawPassword);

    /**
     * 通过邮箱 + 验证码重置密码
     *
     * @return 成功返回 true，失败返回 false
     */
    boolean resetPasswordByEmail(String email, String code, String newRawPassword);

    /**
     * 查询个人空间信息（基本信息 + 统计）
     */
    UserSpaceDTO getUserSpace(Long userId);

    /**
     * 是否已关注
     */
    boolean isFollowing(Long followerId, Long followeeId);

    /**
     * 关注
     */
    boolean follow(Long followerId, Long followeeId);

    /**
     * 取消关注
     */
    boolean unfollow(Long followerId, Long followeeId);

    /**
     * 我关注的人列表（用于发私信）
     */
    List<FollowUserDTO> listFollowing(Long userId, int limit);

    /**
     * 我的粉丝列表（关注我的人，@ 只能 @ 粉丝）
     */
    List<FollowUserDTO> listFollowers(Long userId, int limit);
}
