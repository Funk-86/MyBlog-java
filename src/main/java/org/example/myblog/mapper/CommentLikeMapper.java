package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentLikeMapper {

    /**
     * 插入评论点赞记录
     */
    @Insert("""
            INSERT INTO comment_like (user_id, comment_id, created_at)
            VALUES (#{userId}, #{commentId}, #{createdAt})
            """)
    int insert(@Param("userId") Long userId,
               @Param("commentId") Long commentId,
               @Param("createdAt") java.time.LocalDateTime createdAt);

    /**
     * 取消点赞：按用户+评论删除
     */
    @Delete("""
            DELETE FROM comment_like
            WHERE user_id = #{userId} AND comment_id = #{commentId}
            """)
    int deleteByUserAndComment(@Param("userId") Long userId,
                               @Param("commentId") Long commentId);

    /**
     * 查询当前用户是否已赞该评论
     */
    @Select("""
            SELECT COUNT(*) FROM comment_like
            WHERE user_id = #{userId} AND comment_id = #{commentId}
            """)
    int countByUserAndComment(@Param("userId") Long userId,
                              @Param("commentId") Long commentId);

    /**
     * 查询用户在该帖子下已赞的评论 id 列表
     */
    @Select("""
            SELECT cl.comment_id FROM comment_like cl
            JOIN comment c ON c.id = cl.comment_id
            WHERE cl.user_id = #{userId} AND c.post_id = #{postId}
            """)
    List<Long> listLikedCommentIdsByPost(@Param("userId") Long userId,
                                         @Param("postId") Long postId);
}
