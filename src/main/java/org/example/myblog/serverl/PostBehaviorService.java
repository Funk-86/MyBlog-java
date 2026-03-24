package org.example.myblog.serverl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostBehaviorService {

    private static final String USER_RECENT_VIEW_PREFIX = "post:behavior:recent:";
    private static final String POST_COVIEW_PREFIX = "post:behavior:coview:";
    private static final String VIEW_DEDUPE_PREFIX = "post:behavior:viewdedupe:";
    private static final String SIGNAL_DEDUPE_PREFIX = "post:behavior:signaldedupe:";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    public void recordView(Long userId, Long postId) {
        if (!isReady(userId, postId)) return;
        String dedupeKey = VIEW_DEDUPE_PREFIX + "user:" + userId + ":post:" + postId;
        try {
            Boolean fresh = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofMinutes(30));
            if (Boolean.FALSE.equals(fresh)) return;
            updateCoViewGraph(userId, postId, 1.0);
        } catch (Exception ignored) {
        }
    }

    public void recordReadSignal(Long userId, Long postId, String event) {
        if (!isReady(userId, postId)) return;
        double weight = switch (event == null ? "" : event.toLowerCase()) {
            case "p50" -> 2.0;
            case "p90" -> 3.0;
            default -> 0.0;
        };
        if (weight <= 0) return;
        String dedupeKey = SIGNAL_DEDUPE_PREFIX + event + ":user:" + userId + ":post:" + postId;
        try {
            Boolean fresh = redisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", Duration.ofDays(14));
            if (Boolean.FALSE.equals(fresh)) return;
            updateCoViewGraph(userId, postId, weight);
        } catch (Exception ignored) {
        }
    }

    public Map<Long, Double> listAlsoViewedScores(Long postId, int limit) {
        if (redisTemplate == null || postId == null || postId <= 0 || limit <= 0) return Map.of();
        LinkedHashMap<Long, Double> result = new LinkedHashMap<>();
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(POST_COVIEW_PREFIX + postId, 0, Math.max(limit * 3L, 19L));
            if (tuples == null || tuples.isEmpty()) return result;
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                if (tuple == null || tuple.getValue() == null) continue;
                Long relatedId = parseLong(tuple.getValue());
                if (relatedId == null || relatedId <= 0 || relatedId.equals(postId)) continue;
                result.put(relatedId, tuple.getScore() == null ? 0.0 : tuple.getScore());
                if (result.size() >= limit) break;
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private void updateCoViewGraph(Long userId, Long postId, double weight) {
        if (redisTemplate == null) return;
        String recentKey = USER_RECENT_VIEW_PREFIX + userId;
        try {
            List<String> recent = redisTemplate.opsForList().range(recentKey, 0, 19);
            if (recent != null) {
                for (String raw : recent) {
                    Long otherId = parseLong(raw);
                    if (otherId == null || otherId <= 0 || otherId.equals(postId)) continue;
                    zincr(POST_COVIEW_PREFIX + postId, otherId, weight);
                    zincr(POST_COVIEW_PREFIX + otherId, postId, weight);
                }
            }

            redisTemplate.opsForList().remove(recentKey, 0, String.valueOf(postId));
            redisTemplate.opsForList().leftPush(recentKey, String.valueOf(postId));
            redisTemplate.opsForList().trim(recentKey, 0, 19);
            redisTemplate.expire(recentKey, Duration.ofDays(30));
        } catch (Exception ignored) {
        }
    }

    private void zincr(String key, Long relatedPostId, double weight) {
        try {
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(relatedPostId), weight);
            redisTemplate.expire(key, Duration.ofDays(30));
        } catch (Exception ignored) {
        }
    }

    private boolean isReady(Long userId, Long postId) {
        return redisTemplate != null && userId != null && userId > 0 && postId != null && postId > 0;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }
}
