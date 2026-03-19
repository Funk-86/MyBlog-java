package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.dto.*;

import java.util.List;

@Mapper
public interface NotificationMapper {

    /**
     * 点赞通知：别人赞了当前用户的帖子
     */
    @Select("""
            SELECT pl.id, pl.user_id AS actorId, pl.post_id AS targetId, pl.post_id AS postId, pl.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl
            FROM post_like pl
            JOIN post p ON p.id = pl.post_id AND p.user_id = #{userId}
            LEFT JOIN `user` u ON u.id = pl.user_id
            LEFT JOIN user_profile up ON up.user_id = pl.user_id
            WHERE pl.user_id != #{userId}
            ORDER BY pl.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyLikeDTO> listPostLikeNotify(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 点赞通知：别人赞了当前用户的评论
     */
    @Select("""
            SELECT cl.id, cl.user_id AS actorId, cl.comment_id AS targetId, c.post_id AS postId, c.id AS commentId,
                   c.content AS commentContent, cl.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl
            FROM comment_like cl
            JOIN comment c ON c.id = cl.comment_id AND c.user_id = #{userId}
            JOIN post p ON p.id = c.post_id
            LEFT JOIN `user` u ON u.id = cl.user_id
            LEFT JOIN user_profile up ON up.user_id = cl.user_id
            WHERE cl.user_id != #{userId}
            ORDER BY cl.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyLikeDTO> listCommentLikeNotify(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 回复通知：别人回复了当前用户的评论
     */
    @Select("""
            SELECT c.id, c.user_id AS actorId, c.post_id AS postId, c.id AS commentId,
                   c.root_id AS rootId, c.content, c.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   COALESCE(myUp.nickname, myU.username) AS myUserName,
                   root.content AS myCommentContent,
                   (root.status = 1) AS myCommentDeleted,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl,
                   (p.status = 2 OR p.status = 3) AS postDeleted
            FROM comment c
            JOIN comment root ON (root.id = c.parent_id OR root.id = c.root_id) AND root.user_id = #{userId}
            JOIN post p ON p.id = c.post_id
            LEFT JOIN `user` u ON u.id = c.user_id
            LEFT JOIN user_profile up ON up.user_id = c.user_id
            LEFT JOIN `user` myU ON myU.id = #{userId}
            LEFT JOIN user_profile myUp ON myUp.user_id = #{userId}
            WHERE c.user_id != #{userId} AND c.status = 0
            ORDER BY c.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyReplyDTO> listReplyNotify(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 首评通知：别人直接评论了当前用户的帖子（首评，parent_id IS NULL）
     */
    @Select("""
            SELECT c.id, c.user_id AS actorId, c.post_id AS postId, c.id AS commentId,
                   c.root_id AS rootId, c.content, c.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   COALESCE(myUp.nickname, myU.username) AS myUserName,
                   NULL AS myCommentContent,
                   0 AS myCommentDeleted,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl,
                   (p.status = 2 OR p.status = 3) AS postDeleted
            FROM comment c
            JOIN post p ON p.id = c.post_id AND p.user_id = #{userId}
            LEFT JOIN `user` u ON u.id = c.user_id
            LEFT JOIN user_profile up ON up.user_id = c.user_id
            LEFT JOIN `user` myU ON myU.id = #{userId}
            LEFT JOIN user_profile myUp ON myUp.user_id = #{userId}
            WHERE c.user_id != #{userId} AND c.status = 0 AND c.parent_id IS NULL
            ORDER BY c.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyReplyDTO> listFirstCommentOnPostNotify(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * @我的：评论内容中实际包含 @当前用户昵称 或 @当前用户名的提及
     */
    @Select("""
            SELECT c.id, c.user_id AS actorId, c.post_id AS postId, c.id AS commentId,
                   c.content, c.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl
            FROM comment c
            JOIN post p ON p.id = c.post_id
            LEFT JOIN `user` u ON u.id = c.user_id
            LEFT JOIN user_profile up ON up.user_id = c.user_id
            WHERE c.user_id != #{userId} AND c.status = 0
              AND (
                c.content LIKE CONCAT('%@', (SELECT COALESCE(up2.nickname, u2.username) FROM `user` u2 LEFT JOIN user_profile up2 ON up2.user_id = u2.id WHERE u2.id = #{userId}), '%')
                OR c.content LIKE CONCAT('%@', (SELECT username FROM `user` WHERE id = #{userId}), '%')
              )
            ORDER BY c.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyAtDTO> listAtNotify(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * @我的：帖子正文（title/content）中包含 @当前用户昵称 或 @当前用户名的提及
     */
    @Select("""
            SELECT p.id, p.user_id AS actorId, p.id AS postId, NULL AS commentId,
                   p.content, p.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   p.title AS postTitle,
                   (SELECT MIN(url) FROM post_media WHERE post_id = p.id AND media_type = 1) AS firstImageUrl
            FROM post p
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            WHERE p.user_id != #{userId} AND p.status = 0
              AND (
                p.content LIKE CONCAT('%@', (SELECT COALESCE(up2.nickname, u2.username) FROM `user` u2 LEFT JOIN user_profile up2 ON up2.user_id = u2.id WHERE u2.id = #{userId}), '%')
                OR p.content LIKE CONCAT('%@', (SELECT username FROM `user` WHERE id = #{userId}), '%')
                OR p.title LIKE CONCAT('%@', (SELECT COALESCE(up2.nickname, u2.username) FROM `user` u2 LEFT JOIN user_profile up2 ON up2.user_id = u2.id WHERE u2.id = #{userId}), '%')
                OR p.title LIKE CONCAT('%@', (SELECT username FROM `user` WHERE id = #{userId}), '%')
              )
            ORDER BY p.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyAtDTO> listAtNotifyFromPost(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 粉丝通知：谁关注了当前用户
     */
    @Select("""
            SELECT uf.id, uf.follower_id AS actorId, uf.created_at AS createdAt,
                   COALESCE(up.nickname, u.username) AS actorName, up.avatar_url AS actorAvatar,
                   EXISTS (SELECT 1 FROM user_follow uf2 WHERE uf2.follower_id = #{userId}
                     AND uf2.followee_id = uf.follower_id AND uf2.status = 0) AS followedByMe
            FROM user_follow uf
            LEFT JOIN `user` u ON u.id = uf.follower_id
            LEFT JOIN user_profile up ON up.user_id = uf.follower_id
            WHERE uf.followee_id = #{userId} AND uf.status = 0
            ORDER BY uf.created_at DESC
            LIMIT #{limit}
            """)
    List<NotifyFansDTO> listFansNotify(@Param("userId") Long userId, @Param("limit") int limit);
}
