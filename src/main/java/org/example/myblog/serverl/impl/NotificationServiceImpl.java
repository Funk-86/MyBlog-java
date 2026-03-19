package org.example.myblog.serverl.impl;

import org.example.myblog.dto.*;
import org.example.myblog.mapper.NotificationMapper;
import org.example.myblog.serverl.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Override
    public List<NotifyLikeDTO> listLikeNotify(Long userId, int limit) {
        if (userId == null) return List.of();
        if (limit <= 0) limit = 20;
        List<NotifyLikeDTO> postLikes = notificationMapper.listPostLikeNotify(userId, limit);
        List<NotifyLikeDTO> commentLikes = notificationMapper.listCommentLikeNotify(userId, limit);
        postLikes.forEach(d -> d.setPost(true));
        commentLikes.forEach(d -> d.setPost(false));
        List<NotifyLikeDTO> merged = new ArrayList<>(postLikes);
        merged.addAll(commentLikes);
        merged.sort(Comparator.comparing(NotifyLikeDTO::getCreatedAt).reversed());
        return merged.stream().limit(limit).toList();
    }

    @Override
    public List<NotifyReplyDTO> listReplyNotify(Long userId, int limit) {
        if (userId == null) return List.of();
        if (limit <= 0) limit = 20;
        List<NotifyReplyDTO> replies = notificationMapper.listReplyNotify(userId, limit);
        replies.forEach(d -> d.setIsPostComment(false));
        List<NotifyReplyDTO> firstComments = notificationMapper.listFirstCommentOnPostNotify(userId, limit);
        firstComments.forEach(d -> d.setIsPostComment(true));
        List<NotifyReplyDTO> merged = new ArrayList<>(replies);
        merged.addAll(firstComments);
        merged.sort(Comparator.comparing(NotifyReplyDTO::getCreatedAt).reversed());
        return merged.stream().limit(limit).toList();
    }

    @Override
    public List<NotifyAtDTO> listAtNotify(Long userId, int limit) {
        if (userId == null) return List.of();
        if (limit <= 0) limit = 20;
        List<NotifyAtDTO> fromComment = notificationMapper.listAtNotify(userId, limit);
        List<NotifyAtDTO> fromPost = notificationMapper.listAtNotifyFromPost(userId, limit);
        List<NotifyAtDTO> merged = new ArrayList<>(fromComment);
        merged.addAll(fromPost);
        merged.sort(Comparator.comparing(NotifyAtDTO::getCreatedAt).reversed());
        return merged.stream().limit(limit).toList();
    }

    @Override
    public List<NotifyFansDTO> listFansNotify(Long userId, int limit) {
        if (userId == null) return List.of();
        if (limit <= 0) limit = 20;
        return notificationMapper.listFansNotify(userId, limit);
    }
}
