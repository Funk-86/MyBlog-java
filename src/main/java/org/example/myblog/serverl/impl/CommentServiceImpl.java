package org.example.myblog.serverl.impl;

import org.example.myblog.dto.UserSpaceDTO;
import org.example.myblog.entiy.Comment;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.CommentLikeMapper;
import org.example.myblog.mapper.CommentMapper;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.SensitiveWordMapper;
import org.example.myblog.serverl.CommentService;
import org.example.myblog.serverl.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private PostMapper postMapper;
    @Autowired(required = false)
    private UserService userService;
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private SensitiveWordMapper sensitiveWordMapper;

    @Override
    @Transactional
    public Comment createComment(Long postId, Long userId, String content, Long parentId, Long rootId) {
        // 本地敏感词拦截
        if (content != null && !content.isBlank() && sensitiveWordMapper != null) {
            List<String> words = sensitiveWordMapper.listActiveWords();
            if (words != null && !words.isEmpty()) {
                for (String w : words) {
                    if (w == null || w.isBlank()) {
                        continue;
                    }
                    if (content.contains(w)) {
                        throw new RuntimeException("COMMENT_FORBIDDEN");
                    }
                }
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setRootId(rootId != null ? rootId : parentId);
        comment.setStatus(0);
        comment.setLikeCount(0);
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);

        // 评论成功后，让对应帖子的评论数 +1
        if (postId != null) {
            postMapper.incrementCommentCount(postId);
        }

        // 填充评论者昵称、头像，便于前端发送后立即展示
        if (userService != null && userId != null) {
            UserSpaceDTO user = userService.getUserSpace(userId);
            if (user != null) {
                comment.setUsername(user.getUsername());
                comment.setNickname(user.getNickname());
                comment.setAvatarUrl(user.getAvatarUrl());
            }
        }
        // 异步刷新缓存
        cacheCommentAsync(comment);
        return comment;
    }

    @Override
    public List<Comment> listComments(Long postId, int page, int size, Integer offsetParam) {
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;

        int offset = offsetParam != null ? offsetParam : (page - 1) * size;
        return commentMapper.listByPost(postId, offset, size);
    }

    @Override
    @Transactional
    public void likeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return;
        }
        if (commentLikeMapper.countByUserAndComment(userId, commentId) > 0) {
            return; // 已赞过，幂等
        }
        commentLikeMapper.insert(userId, commentId, LocalDateTime.now());
        commentMapper.incrementLikeCount(commentId);
    }

    @Override
    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return;
        }
        int deleted = commentLikeMapper.deleteByUserAndComment(userId, commentId);
        if (deleted > 0) {
            commentMapper.decrementLikeCount(commentId);
        }
    }

    @Override
    public boolean isCommentLiked(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            return false;
        }
        return commentLikeMapper.countByUserAndComment(userId, commentId) > 0;
    }

    @Override
    public List<Long> getLikedCommentIds(Long postId, Long userId) {
        if (postId == null || userId == null) {
            return List.of();
        }
        return commentLikeMapper.listLikedCommentIdsByPost(userId, postId);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long operatorUserId) {
        if (commentId == null || operatorUserId == null) return;
        Comment c = commentMapper.selectById(commentId);
        if (c == null) return;
        Post post = postMapper.selectDetailById(c.getPostId());
        if (post == null || !operatorUserId.equals(post.getUserId())) return; // 仅帖子作者可删
        commentMapper.updateStatus(commentId, 1); // 1=删除
        postMapper.decrementCommentCount(c.getPostId());
    }

    @Override
    @Transactional
    public void pinComment(Long commentId, Long operatorUserId) {
        if (commentId == null || operatorUserId == null) return;
        Comment c = commentMapper.selectById(commentId);
        if (c == null) return;
        Post post = postMapper.selectDetailById(c.getPostId());
        if (post == null || !operatorUserId.equals(post.getUserId())) return; // 仅帖子作者可置顶
        commentMapper.unpinAllByPost(c.getPostId());
        commentMapper.pinComment(commentId, c.getPostId());
    }

    @Async
    protected void cacheCommentAsync(Comment comment) {
        if (redisTemplate == null || comment == null) {
            return;
        }
        String detailKey = "comment:detail:" + comment.getId();
        String listKey = "post:comments:" + comment.getPostId();

        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(comment.getId()));
        map.put("postId", String.valueOf(comment.getPostId()));
        map.put("userId", String.valueOf(comment.getUserId()));
        map.put("content", comment.getContent());
        map.put("status", String.valueOf(comment.getStatus()));
        map.put("likeCount", String.valueOf(comment.getLikeCount()));
        map.put("createdAt", comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : "");
        map.put("updatedAt", comment.getUpdatedAt() != null ? comment.getUpdatedAt().toString() : "");
        // 用户展示信息（可能为空）
        if (comment.getUsername() != null) {
            map.put("username", comment.getUsername());
        }
        if (comment.getNickname() != null) {
            map.put("nickname", comment.getNickname());
        }
        if (comment.getAvatarUrl() != null) {
            map.put("avatarUrl", comment.getAvatarUrl());
        }

        redisTemplate.opsForHash().putAll(detailKey, map);

        long ts = comment.getCreatedAt() != null
                ? comment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();
        redisTemplate.opsForZSet().add(listKey, String.valueOf(comment.getId()), ts);
    }

    private Comment mapToComment(Map<Object, Object> map) {
        Comment c = new Comment();
        c.setId(Long.valueOf((String) map.get("id")));
        c.setPostId(Long.valueOf((String) map.get("postId")));
        c.setUserId(Long.valueOf((String) map.get("userId")));
        c.setContent((String) map.get("content"));
        c.setStatus(Integer.valueOf((String) map.getOrDefault("status", "0")));
        c.setLikeCount(Integer.valueOf((String) map.getOrDefault("likeCount", "0")));
        Object username = map.get("username");
        if (username instanceof String) {
            c.setUsername((String) username);
        }
        Object nickname = map.get("nickname");
        if (nickname instanceof String) {
            c.setNickname((String) nickname);
        }
        Object avatarUrl = map.get("avatarUrl");
        if (avatarUrl instanceof String) {
            c.setAvatarUrl((String) avatarUrl);
        }
        // createdAt / updatedAt 如有需要可在这里解析字符串为 LocalDateTime
        return c;
    }
}

