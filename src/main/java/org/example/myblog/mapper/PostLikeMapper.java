package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PostLikeMapper {

    @Insert("""
            INSERT INTO post_like (user_id, post_id, created_at)
            VALUES (#{userId}, #{postId}, #{createdAt})
            """)
    int insert(@Param("userId") Long userId,
               @Param("postId") Long postId,
               @Param("createdAt") java.time.LocalDateTime createdAt);

    @Delete("""
            DELETE FROM post_like
            WHERE user_id = #{userId} AND post_id = #{postId}
            """)
    int deleteByUserAndPost(@Param("userId") Long userId,
                            @Param("postId") Long postId);

    @Select("""
            SELECT COUNT(*) FROM post_like
            WHERE user_id = #{userId} AND post_id = #{postId}
            """)
    int countByUserAndPost(@Param("userId") Long userId,
                           @Param("postId") Long postId);
}
