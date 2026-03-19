package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.Comment;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommentMapper {

    @Insert("""
            INSERT INTO comment (post_id, user_id, content, parent_id, root_id, status, like_count, created_at, updated_at)
            VALUES (#{postId}, #{userId}, #{content}, #{parentId}, #{rootId}, #{status}, #{likeCount}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    @Select("""
            SELECT id,
                   post_id    AS postId,
                   user_id    AS userId,
                   content,
                   parent_id  AS parentId,
                   root_id    AS rootId,
                   status,
                   like_count AS likeCount,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM comment
            WHERE id = #{id}
            """)
    Comment selectById(@Param("id") Long id);

    @Select("""
            SELECT c.id,
                   c.post_id    AS postId,
                   c.user_id    AS userId,
                   c.content,
                   c.parent_id  AS parentId,
                   c.root_id    AS rootId,
                   c.status,
                   c.like_count AS likeCount,
                   c.created_at AS createdAt,
                   c.updated_at AS updatedAt,
                   COALESCE(c.is_pinned, 0) AS isPinned,
                   u.username,
                   up.nickname,
                   up.avatar_url AS avatarUrl
            FROM comment c
                     LEFT JOIN `user` u ON u.id = c.user_id
                     LEFT JOIN user_profile up ON up.user_id = c.user_id
            WHERE c.post_id = #{postId}
              AND c.status = 0
            ORDER BY COALESCE(c.is_pinned, 0) DESC, c.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Comment> listByPost(@Param("postId") Long postId,
                             @Param("offset") int offset,
                             @Param("limit") int limit);

    /**
     * 评论点赞数 +1
     */
    @Update("""
            UPDATE comment
            SET like_count = like_count + 1
            WHERE id = #{commentId}
            """)
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 评论点赞数 -1（不低于 0）
     */
    @Update("""
            UPDATE comment
            SET like_count = GREATEST(0, like_count - 1)
            WHERE id = #{commentId}
            """)
    int decrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 更新评论状态（0正常,1删除,2屏蔽）
     */
    @Update("""
            UPDATE comment
            SET status = #{status}, updated_at = NOW()
            WHERE id = #{commentId}
            """)
    int updateStatus(@Param("commentId") Long commentId, @Param("status") int status);

    /**
     * 取消该帖子下所有评论的置顶
     */
    @Update("UPDATE comment SET is_pinned = 0 WHERE post_id = #{postId}")
    int unpinAllByPost(@Param("postId") Long postId);

    /**
     * 置顶指定评论
     */
    @Update("UPDATE comment SET is_pinned = 1 WHERE id = #{commentId} AND post_id = #{postId}")
    int pinComment(@Param("commentId") Long commentId, @Param("postId") Long postId);

    /** 评论总数（status=0 正常） */
    @Select("SELECT COUNT(*) FROM comment WHERE status = 0")
    long countAll();

    /** 昨日新增评论数 */
    @Select("SELECT COUNT(*) FROM comment WHERE status = 0 AND DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    long countYesterdayNew();

    /** 近 N 天每日评论数 */
    @Select("""
            SELECT DATE(created_at) AS statDate, COUNT(*) AS cnt
            FROM comment
            WHERE status = 0 AND created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE(created_at)
            ORDER BY statDate ASC
            """)
    List<Map<String, Object>> listCountByDay(@Param("days") int days);
}

