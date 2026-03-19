package org.example.myblog.serverl;

import org.example.myblog.entiy.Comment;

import java.util.List;

public interface CommentService {

    /**
     * 发表评论（写入 MySQL，并异步刷新 Redis）
     * @param parentId 回复的评论 id，为 null 表示一级评论
     * @param rootId 根评论 id，为 null 时若 parentId 存在则取 parentId
     */
    Comment createComment(Long postId, Long userId, String content, Long parentId, Long rootId);

    /**
     * 查询帖子评论列表（优先走 Redis，未命中回源 MySQL）
     * @param offset 可选，指定时按 offset+size 分页，忽略 page
     */
    List<Comment> listComments(Long postId, int page, int size, Integer offset);

    /**
     * 评论点赞
     */
    void likeComment(Long commentId, Long userId);

    /**
     * 取消评论点赞
     */
    void unlikeComment(Long commentId, Long userId);

    /**
     * 当前用户是否已赞该评论
     */
    boolean isCommentLiked(Long commentId, Long userId);

    /**
     * 获取用户在该帖子下已赞的评论 id 列表
     */
    List<Long> getLikedCommentIds(Long postId, Long userId);

    /**
     * 删除评论（仅帖子作者可操作，软删除 status=1）
     */
    void deleteComment(Long commentId, Long operatorUserId);

    /**
     * 置顶评论（仅帖子作者可操作，每帖最多置顶一条）
     */
    void pinComment(Long commentId, Long operatorUserId);
}

