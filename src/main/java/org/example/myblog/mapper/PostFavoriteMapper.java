package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PostFavoriteMapper {

    @Insert("""
            INSERT INTO post_favorite (user_id, post_id, created_at)
            VALUES (#{userId}, #{postId}, #{createdAt})
            """)
    int insert(@Param("userId") Long userId,
               @Param("postId") Long postId,
               @Param("createdAt") java.time.LocalDateTime createdAt);

    @Delete("""
            DELETE FROM post_favorite
            WHERE user_id = #{userId} AND post_id = #{postId}
            """)
    int deleteByUserAndPost(@Param("userId") Long userId,
                            @Param("postId") Long postId);

    @Select("""
            SELECT COUNT(*) FROM post_favorite
            WHERE user_id = #{userId} AND post_id = #{postId}
            """)
    int countByUserAndPost(@Param("userId") Long userId,
                           @Param("postId") Long postId);
}
