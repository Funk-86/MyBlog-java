package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.dto.FollowUserDTO;

import java.util.List;

@Mapper
public interface UserFollowMapper {

    /**
     * 查询关注状态
     *
     * @return status: 0=正常关注, 1=取关; null 表示不存在记录
     */
    @Select("""
            SELECT status
            FROM user_follow
            WHERE follower_id = #{followerId}
              AND followee_id = #{followeeId}
            """)
    Integer selectStatus(@Param("followerId") Long followerId,
                         @Param("followeeId") Long followeeId);

    /**
     * 关注（存在则更新为关注，不存在则插入）
     */
    @Insert("""
            INSERT INTO user_follow (follower_id, followee_id, status)
            VALUES (#{followerId}, #{followeeId}, 0)
            ON DUPLICATE KEY UPDATE status = 0
            """)
    int follow(@Param("followerId") Long followerId,
               @Param("followeeId") Long followeeId);

    /**
     * 取消关注（将 status 置为 1）
     */
    @Update("""
            UPDATE user_follow
            SET status = 1
            WHERE follower_id = #{followerId}
              AND followee_id = #{followeeId}
            """)
    int unfollow(@Param("followerId") Long followerId,
                 @Param("followeeId") Long followeeId);

    /**
     * 我关注的人列表（用于发私信）
     */
    @Select("""
            SELECT u.id, COALESCE(up.nickname, u.username) AS nickname, up.avatar_url AS avatarUrl
            FROM user_follow uf
            JOIN `user` u ON u.id = uf.followee_id
            LEFT JOIN user_profile up ON up.user_id = u.id
            WHERE uf.follower_id = #{followerId} AND uf.status = 0
            ORDER BY uf.created_at DESC
            LIMIT #{limit}
            """)
    List<FollowUserDTO> listFollowees(@Param("followerId") Long followerId, @Param("limit") int limit);

    /**
     * 我的粉丝列表（关注我的人，用于 @ 只能 @ 粉丝）
     */
    @Select("""
            SELECT u.id, COALESCE(up.nickname, u.username) AS nickname, up.avatar_url AS avatarUrl
            FROM user_follow uf
            JOIN `user` u ON u.id = uf.follower_id
            LEFT JOIN user_profile up ON up.user_id = u.id
            WHERE uf.followee_id = #{followeeId} AND uf.status = 0
            ORDER BY uf.created_at DESC
            LIMIT #{limit}
            """)
    List<FollowUserDTO> listFollowers(@Param("followeeId") Long followeeId, @Param("limit") int limit);
}

