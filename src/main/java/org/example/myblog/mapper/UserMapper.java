package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.User;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    /**
     * 根据邮箱和密码查询用户（邮箱登录）
     */
    @Select("""
            SELECT id,
                   username,
                   email,
                   role,
                   password_hash AS passwordHash,
                   salt,
                   status,
                   banned_until AS bannedUntil,
                   last_login_at AS lastLoginAt
            FROM `user`
            WHERE email = #{email}
              AND password_hash = #{passwordHash}
            """)
    User selectByEmailAndPassword(@Param("email") String email,
                                  @Param("passwordHash") String passwordHash);

    /**
     * 根据用户名和密码查询用户（账号登录）
     */
    @Select("""
            SELECT id,
                   username,
                   email,
                   role,
                   password_hash AS passwordHash,
                   salt,
                   status,
                   banned_until AS bannedUntil,
                   last_login_at AS lastLoginAt
            FROM `user`
            WHERE username = #{username}
              AND password_hash = #{passwordHash}
            """)
    User selectByUsernameAndPassword(@Param("username") String username,
                                     @Param("passwordHash") String passwordHash);

    /**
     * 根据邮箱查询用户
     */
    @Select("""
            SELECT id,
                   username,
                   email,
                   password_hash AS passwordHash,
                   salt,
                   status,
                   last_login_at AS lastLoginAt
            FROM `user`
            WHERE email = #{email}
            """)
    User selectByEmail(@Param("email") String email);

    /**
     * 根据主键查询用户（聊天等场景使用）
     */
    @Select("""
            SELECT id,
                   username,
                   email,
                   role,
                   status,
                   banned_until AS bannedUntil,
                   last_login_at AS lastLoginAt
            FROM `user`
            WHERE id = #{id}
            """)
    User selectById(@Param("id") Long id);

    /**
     * 根据主键查询用户基本信息 + 头像昵称（聊天发送者展示用）
     */
    @Select("""
            SELECT u.id, u.username,
                   up.nickname,
                   up.avatar_url AS avatarUrl
            FROM `user` u
            LEFT JOIN user_profile up ON up.user_id = u.id
            WHERE u.id = #{id}
            """)
    Map<String, Object> selectByIdWithProfile(@Param("id") Long id);

    /**
     * 插入新用户（用于邮箱注册）
     */
    @Insert("""
            INSERT INTO `user` (username, email, password_hash, salt, status)
            VALUES (#{username}, #{email}, #{passwordHash}, #{salt}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 根据邮箱更新密码
     */
    @Update("""
            UPDATE `user`
            SET password_hash = #{passwordHash}, salt = #{salt}
            WHERE email = #{email}
            """)
    int updatePasswordByEmail(@Param("email") String email,
                              @Param("passwordHash") String passwordHash,
                              @Param("salt") String salt);

    /**
     * 查询个人空间信息：基本资料 + 各种统计
     */
    @Select("""
            SELECT
                u.id,
                u.username,
                u.email,
                DATE_FORMAT(u.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
                up.nickname,
                up.avatar_url      AS avatarUrl,
                up.bio,
                up.gender,
                -- 我关注的人数
                (SELECT COUNT(*)
                 FROM user_follow uf
                 WHERE uf.follower_id = u.id
                   AND uf.status = 0)               AS followCount,
                -- 粉丝数
                (SELECT COUNT(*)
                 FROM user_follow uf
                 WHERE uf.followee_id = u.id
                   AND uf.status = 0)               AS fansCount,
                -- 发帖数
                (SELECT COUNT(*)
                 FROM post p
                 WHERE p.user_id = u.id
                   AND p.status = 0)                AS postCount,
                -- 回复数
                (SELECT COUNT(*)
                 FROM comment c
                 WHERE c.user_id = u.id
                   AND c.status = 0)                AS replyCount,
                -- 收藏数
                (SELECT COUNT(*)
                 FROM post_favorite pf
                 WHERE pf.user_id = u.id)           AS favoriteCount,
                -- 点赞数
                (SELECT COUNT(*)
                 FROM post_like pl
                 WHERE pl.user_id = u.id)           AS likeCount
            FROM `user` u
                     LEFT JOIN user_profile up ON up.user_id = u.id
            WHERE u.id = #{userId}
            """)
    UserSpaceDTO selectUserSpace(@Param("userId") Long userId);

    /**
     * 管理端：用户列表
     */
    @Select("""
            SELECT u.id, u.username, u.email, u.role, u.status, u.banned_until AS bannedUntil, u.created_at AS createdAt,
                   up.nickname, up.bio, up.avatar_url AS avatarUrl
            FROM `user` u
            LEFT JOIN user_profile up ON up.user_id = u.id
            ORDER BY u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listForAdmin(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 管理端：用户列表（按关键词搜索用户名、昵称、邮箱）
     */
    @Select("""
            <script>
            SELECT u.id, u.username, u.email, u.role, u.status, u.banned_until AS bannedUntil, u.created_at AS createdAt,
                   up.nickname, up.bio, up.avatar_url AS avatarUrl
            FROM `user` u
            LEFT JOIN user_profile up ON up.user_id = u.id
            <where>
              <if test="keyword != null and keyword != ''">
                (u.username LIKE CONCAT('%', #{keyword}, '%')
                 OR up.nickname LIKE CONCAT('%', #{keyword}, '%')
                 OR u.email LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            </where>
            ORDER BY u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Map<String, Object>> listForAdminWithKeyword(@Param("offset") int offset, @Param("limit") int limit, @Param("keyword") String keyword);

    /**
     * 管理端：更新用户状态（0正常 2注销），并清空 banned_until
     */
    @Update("UPDATE `user` SET status = #{status}, banned_until = NULL WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") int status);

    /**
     * 管理端：批量更新用户状态（0解封 2注销），并清空 banned_until
     */
    @Update("""
            <script>
            UPDATE `user` SET status = #{status}, banned_until = NULL WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    int updateStatusBatch(@Param("ids") List<Long> ids, @Param("status") int status);

    /**
     * 管理端：批量封禁用户，设置封禁截止时间（null 表示永久）
     */
    @Update("""
            <script>
            UPDATE `user` SET status = 1, banned_until = #{bannedUntil} WHERE id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    int updateBanBatch(@Param("ids") List<Long> ids, @Param("bannedUntil") java.time.LocalDateTime bannedUntil);
}
