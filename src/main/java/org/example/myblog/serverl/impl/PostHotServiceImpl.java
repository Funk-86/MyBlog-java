package org.example.myblog.serverl.impl;

import org.example.myblog.config.HotProperties;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.serverl.PostHotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PostHotServiceImpl implements PostHotService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private HotProperties hotProperties;

    private static final String VIEW_PREFIX = "post:view:";
    private static final String ZSET_KEY = "zset:post:hot";
    private static final String READ50_COUNT_PREFIX = "post:read50:count:";
    private static final String READ90_COUNT_PREFIX = "post:read90:count:";

    @Override
    public void incrementView(Long postId) {
        if (postId == null) return;
        String key = VIEW_PREFIX + postId;
        redisTemplate.opsForValue().increment(key, 1);
        recalculateHotScore(postId);
    }

    @Override
    @Async
    public void recalculateHotScore(Long postId) {
        if (postId == null) return;
        try {
            Post post = postMapper.selectById(postId);
            if (post == null) return;
            long view = getRedisViewCount(postId);
            int like = post.getLikeCount() != null ? post.getLikeCount() : 0;
            int comment = post.getCommentCount() != null ? post.getCommentCount() : 0;
            int favorite = post.getFavoriteCount() != null ? post.getFavoriteCount() : 0;
            long read50 = getRedisLong(READ50_COUNT_PREFIX + postId);
            long read90 = getRedisLong(READ90_COUNT_PREFIX + postId);
            double score = calculateHotScore(view, like, comment, favorite, read50, read90, post.getCreatedAt());
            redisTemplate.opsForZSet().add(ZSET_KEY, "post:" + postId, score);
        } catch (Exception ignored) {
        }
    }

    private long getRedisViewCount(Long postId) {
        try {
            String val = redisTemplate.opsForValue().get(VIEW_PREFIX + postId);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private long getRedisLong(String key) {
        try {
            String val = redisTemplate.opsForValue().get(key);
            return val != null ? Long.parseLong(val) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 热度 = 基础热度 × (1 + Wilson权重 × Wilson下界)
     * 基础热度：log(互动量) / 时间衰减；Wilson 下界基于「点赞数/浏览量」的二项置信下界，让点赞率好但曝光少的帖子有机会上浮。
     */
    private double calculateHotScore(long view, int like, int comment, int favorite, long read50, long read90, LocalDateTime createdAt) {
        var w = hotProperties.getWeights();
        double interaction = view * w.getView()
                + like * w.getLike()
                + comment * w.getComment()
                + favorite * w.getFavorite()
                + read50 * w.getRead50()
                + read90 * w.getRead90();
        double logScore = Math.log10(Math.max(interaction, 1));
        long ageHours = createdAt != null
                ? (System.currentTimeMillis() - createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / (1000 * 3600)
                : 1;
        ageHours = Math.max(1, ageHours);
        double rawScore = logScore / Math.pow(ageHours + 2, hotProperties.getTimeDecay());

        double wilsonWeight = hotProperties.getWilsonWeight();
        if (wilsonWeight <= 0) return rawScore;

        // 以「浏览量」为曝光 n，「点赞数」为成功数，算点赞率的 Wilson 下界
        long n = Math.max(view, 0);
        long successes = Math.min(Math.max(like, 0), n);
        double wilsonLb = wilsonLowerBound(successes, n, hotProperties.getWilsonZ());
        return rawScore * (1 + wilsonWeight * wilsonLb);
    }

    /**
     * Wilson 得分下界：二项比例 p = successes/n 的置信下界，小样本时更保守，避免纯点赞率把新帖刷爆
     * 公式: (p + z²/(2n) - z * sqrt((p(1-p) + z²/(4n))/n)) / (1 + z²/n)
     */
    private static double wilsonLowerBound(long successes, long n, double z) {
        if (n <= 0) return 0;
        double p = (double) successes / n;
        double z2 = z * z;
        double denom = 1 + z2 / n;
        double centre = p + z2 / (2 * n);
        double variance = (p * (1 - p) + z2 / (4 * n)) / n;
        if (variance < 0) variance = 0;
        double margin = z * Math.sqrt(variance);
        double lower = (centre - margin) / denom;
        if (lower < 0) return 0;
        if (lower > 1) return 1;
        return lower;
    }

    @Override
    public List<Long> getHotPostIds(long offset, long limit) {
        try {
            Set<String> set = redisTemplate.opsForZSet().reverseRange(ZSET_KEY, offset, offset + limit - 1);
            if (set == null || set.isEmpty()) return List.of();
            List<Long> ids = new ArrayList<>();
            for (String s : set) {
                if (s.startsWith("post:")) {
                    try {
                        ids.add(Long.parseLong(s.substring(5)));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public void syncViewCountAndRefreshHot() {
        try {
            Set<String> keys = redisTemplate.keys(VIEW_PREFIX + "*");
            if (keys != null) {
                for (String key : keys) {
                    try {
                        String val = redisTemplate.opsForValue().get(key);
                        if (val == null) continue;
                        long postId = Long.parseLong(key.substring(VIEW_PREFIX.length()));
                        int v = Integer.parseInt(val);
                        postMapper.updateViewCount(postId, v);
                        Post post = postMapper.selectById(postId);
                        if (post != null) {
                            long read50 = getRedisLong(READ50_COUNT_PREFIX + postId);
                            long read90 = getRedisLong(READ90_COUNT_PREFIX + postId);
                            double score = calculateHotScore(v, post.getLikeCount(), post.getCommentCount(),
                                    post.getFavoriteCount(), read50, read90, post.getCreatedAt());
                            postMapper.updateHotScore(postId, score);
                            redisTemplate.opsForZSet().add(ZSET_KEY, "post:" + postId, score);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
