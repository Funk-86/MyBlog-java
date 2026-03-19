package org.example.myblog.serverl;

import org.example.myblog.entiy.Post;

import java.util.List;

/**
 * 帖子热度服务：Redis 实时计数 + 热度计算
 */
public interface PostHotService {

    /**
     * 增加浏览量（Redis 实时 + 异步更新热度 ZSet）
     */
    void incrementView(Long postId);

    /**
     * 重新计算单个帖子的热度分数，更新到 Redis ZSet
     */
    void recalculateHotScore(Long postId);

    /**
     * 获取热门帖子 ID 列表（从 Redis ZSet 按分数倒序）
     */
    List<Long> getHotPostIds(long offset, long limit);

    /**
     * 定时任务：将 Redis 浏览量同步到 MySQL，并全量重算 hot_score 更新到 ZSet
     */
    void syncViewCountAndRefreshHot();
}
