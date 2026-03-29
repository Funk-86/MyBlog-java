package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserBlockMapper {

    @Insert("""
            INSERT IGNORE INTO user_block (blocker_id, blocked_id, created_at)
            VALUES (#{blockerId}, #{blockedId}, NOW())
            """)
    int insertIgnore(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Delete("""
            DELETE FROM user_block
            WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}
            """)
    int delete(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Select("""
            SELECT COUNT(*) FROM user_block
            WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}
            """)
    int countPair(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * 当前浏览者 viewer 被哪些用户拉黑（这些用户的帖子应对 viewer 隐藏）
     */
    @Select("SELECT blocker_id FROM user_block WHERE blocked_id = #{viewerUserId}")
    List<Long> listBlockerIdsWhoHideFrom(@Param("viewerUserId") Long viewerUserId);
}
