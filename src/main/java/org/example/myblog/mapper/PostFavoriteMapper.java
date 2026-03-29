package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PostFavoriteMapper {

    @Insert("""
            INSERT INTO post_favorite (user_id, post_id, folder_id, created_at)
            VALUES (#{userId}, #{postId}, #{folderId}, #{createdAt})
            """)
    int insert(@Param("userId") Long userId,
               @Param("postId") Long postId,
               @Param("folderId") Long folderId,
               @Param("createdAt") java.time.LocalDateTime createdAt);

    @Update("""
            UPDATE post_favorite
            SET folder_id = #{folderId}
            WHERE user_id = #{userId} AND post_id = #{postId}
            """)
    int updateFolderId(@Param("userId") Long userId,
                       @Param("postId") Long postId,
                       @Param("folderId") Long folderId);

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
