package org.example.myblog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 阅读进度上报（用于算法信号）
 */
@Controller
@RequestMapping("/post")
public class PostReadProgressController {

    private static final String USER_PROGRESS_PREFIX = "post:read:progress:"; // user:{uid}:post:{pid}
    private static final String READ50_USERS_PREFIX = "post:read50:users:";   // postId -> set(userId)
    private static final String READ90_USERS_PREFIX = "post:read90:users:";   // postId -> set(userId)
    private static final String READ50_COUNT_PREFIX = "post:read50:count:";   // postId -> count
    private static final String READ90_COUNT_PREFIX = "post:read90:count:";   // postId -> count

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    /**
     * 上报阅读进度
     * POST /post/readProgress
     * Body: { postId, userId?, progress(0~1), event("p50"|"p90"|"close") }
     */
    @PostMapping("/readProgress")
    @ResponseBody
    public Map<String, Object> report(@RequestBody Map<String, Object> body) {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        if (redisTemplate == null || body == null) return res;

        Long postId = toLong(body.get("postId"));
        Long userId = toLong(body.get("userId"));
        Double progress = toDouble(body.get("progress"));
        String event = body.get("event") != null ? body.get("event").toString() : "";
        if (postId == null || postId <= 0) return res;
        if (progress == null) progress = 0.0;
        if (progress < 0) progress = 0.0;
        if (progress > 1) progress = 1.0;

        // 记录用户维度进度（用于“关闭时进度”/重进续读等），匿名不记
        if (userId != null && userId > 0) {
            String key = USER_PROGRESS_PREFIX + "user:" + userId + ":post:" + postId;
            try {
                redisTemplate.opsForHash().put(key, "progress", String.valueOf(progress));
                redisTemplate.opsForHash().put(key, "event", event);
                redisTemplate.opsForHash().put(key, "ts", String.valueOf(System.currentTimeMillis()));
                redisTemplate.expire(key, Duration.ofDays(7));
            } catch (Exception ignored) {
            }
        }

        // 计数：50% / 90% 完读信号（按 userId 去重）
        if (userId != null && userId > 0) {
            if ("p50".equalsIgnoreCase(event)) {
                markUnique(userId, postId, true);
            } else if ("p90".equalsIgnoreCase(event)) {
                markUnique(userId, postId, false);
            }
        }
        return res;
    }

    private void markUnique(Long userId, Long postId, boolean is50) {
        String usersKey = (is50 ? READ50_USERS_PREFIX : READ90_USERS_PREFIX) + postId;
        String countKey = (is50 ? READ50_COUNT_PREFIX : READ90_COUNT_PREFIX) + postId;
        try {
            Long added = redisTemplate.opsForSet().add(usersKey, String.valueOf(userId));
            redisTemplate.expire(usersKey, Duration.ofDays(14));
            if (added != null && added > 0) {
                redisTemplate.opsForValue().increment(countKey, 1);
                redisTemplate.expire(countKey, Duration.ofDays(14));
            }
        } catch (Exception ignored) {
        }
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static Double toDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }
}

