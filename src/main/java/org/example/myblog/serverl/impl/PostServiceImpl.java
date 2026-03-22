package org.example.myblog.serverl.impl;

import com.alibaba.fastjson2.JSON;
import org.example.myblog.config.HotProperties;
import org.example.myblog.dto.SendMessageRequest;
import org.example.myblog.entiy.Post;
import org.example.myblog.entiy.PostMedia;
import org.example.myblog.mapper.PostFavoriteMapper;
import org.example.myblog.mapper.PostLikeMapper;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.PostMediaMapper;
import org.example.myblog.mapper.TopicMapper;
import org.example.myblog.serverl.ChatService;
import org.example.myblog.serverl.AliyunGreenService;
import org.example.myblog.serverl.ContentModerationService;
import org.example.myblog.serverl.PostHotService;
import org.example.myblog.serverl.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PostServiceImpl implements PostService {

    /** 用于系统通知的管理员账号 ID（请确保数据库中存在该用户） */
    private static final long SYSTEM_ADMIN_ID = 6L;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostMediaMapper postMediaMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostFavoriteMapper postFavoriteMapper;

    @Autowired(required = false)
    private PostHotService postHotService;

    @Autowired(required = false)
    private ChatService chatService;

    @Autowired(required = false)
    private HotProperties hotProperties;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired(required = false)
    private AliyunGreenService aliyunGreenService;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private ContentModerationService contentModerationService;

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    @Override
    public Post createPost(Long userId,
                           String content,
                           Integer type,
                           Long categoryId1,
                           Long categoryId2) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setType(type);
        // 默认审核中，由管理员审核后再发布
        post.setStatus(1);
        // 设置分区（允许为 null）
        post.setCategoryId1(categoryId1);
        post.setCategoryId2(categoryId2);
        postMapper.insert(post);
        return post;
    }

    @Override
    public List<Post> listByCategory(Long categoryId) {
        return postMapper.listByCategory(categoryId);
    }

    @Override
    public List<Post> listRandom(int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        // 做一个上限保护，避免一次查太多
        if (limit > 50) {
            limit = 50;
        }
        return postMapper.listRandom(limit);
    }

    @Override
    public List<Post> listFollowedPosts(Long followerId, int limit) {
        if (followerId == null) {
            return List.of();
        }
        if (limit <= 0) {
            limit = 10;
        }
        if (limit > 50) {
            limit = 50;
        }
        return postMapper.listByFollowedUsers(followerId, limit);
    }

    @Override
    public Post createPostWithImages(Long userId,
                                     String title,
                                     String content,
                                     List<String> imageUrls,
                                     Long categoryId1,
                                     Long categoryId2,
                                     List<String> topics,
                                     String videoUrl,
                                     String videoCoverUrl,
                                     Integer videoDurationSeconds,
                                     Integer visibility) {
        // 本地敏感词分级策略：高风险拦截，中风险人工审核，低风险提醒
        String toCheck = ((title == null ? "" : title) + "\n" + (content == null ? "" : content)).trim();
        ContentModerationService.ModerationResult reviewResult =
                contentModerationService != null ? contentModerationService.moderateText(toCheck) : ContentModerationService.ModerationResult.none();
        if (reviewResult.getAction() == ContentModerationService.ModerationAction.BLOCK) {
            throw new RuntimeException("POST_FORBIDDEN");
        }
        if (reviewResult.getAction() == ContentModerationService.ModerationAction.REVIEW) {
            throw new RuntimeException("POST_REVIEW_REQUIRED");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        boolean hasVideo = videoUrl != null && !videoUrl.isEmpty();
        boolean hasImages = imageUrls != null && !imageUrls.isEmpty();
        post.setType(hasVideo ? 2 : (hasImages ? 1 : 0)); // 2=视频, 1=图文, 0=文字
        // 默认审核中，待管理员审核
        post.setStatus(1);
        post.setCategoryId1(categoryId1);
        post.setCategoryId2(categoryId2);
        post.setVisibility(visibility != null ? visibility : 0); // 0所有人看 1仅个人查看
        postMapper.insert(post);
        if (hasVideo) {
            PostMedia m = new PostMedia();
            m.setPostId(post.getId());
            m.setMediaType(2);
            m.setUrl(videoUrl);
            m.setCoverUrl(videoCoverUrl != null && !videoCoverUrl.isEmpty() ? videoCoverUrl : null);
            m.setSortOrder(0);
            m.setDurationSec(videoDurationSeconds != null ? videoDurationSeconds : 0);
            postMediaMapper.insert(m);
        }
        if (imageUrls != null) {
            int sort = 0;
            for (String url : imageUrls) {
                PostMedia m = new PostMedia();
                m.setPostId(post.getId());
                m.setMediaType(1);
                m.setUrl(url);
                m.setSortOrder(sort++);
                postMediaMapper.insert(m);
            }
        }
        // 绑定话题：优先前端显式传入，其次从标题/正文解析 #话题
        List<String> bindList = new ArrayList<>();
        if (topics != null) {
            for (String t : topics) {
                if (t == null) continue;
                String v = t.trim();
                if (v.startsWith("#")) v = v.substring(1).trim();
                if (!v.isEmpty()) bindList.add(v);
            }
        }
        bindList.addAll(extractTopics(title));
        bindList.addAll(extractTopics(content));
        bindTopicsForPost(post.getId(), bindList);

        // 异步走 AI 审核（文本 + 图片 + 视频），审核通过会自动将 status 从 1 改为 0，
        // 审核不通过则改为 3；review 则继续保持 1，由人工在后台审核列表处理。
        if (aliyunGreenService != null) {
            aliyunGreenService.asyncCheckPostContent(post, imageUrls != null ? imageUrls : List.of(), videoUrl);
        }

        // 发送系统通知：帖子正在审核中（仅发给发帖用户）
        sendSystemNotify(userId, buildPendingText(title));
        return post;
    }


    @Override
    public Post getPostDetail(Long id) {
        if (id == null) {
            return null;
        }
        try {
            return postMapper.selectDetailById(id);
        } catch (Exception e) {
            return postMapper.selectById(id);
        }
    }

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        if (postLikeMapper.countByUserAndPost(userId, postId) > 0) return;
        postLikeMapper.insert(userId, postId, LocalDateTime.now());
        postMapper.incrementLikeCount(postId);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        int deleted = postLikeMapper.deleteByUserAndPost(userId, postId);
        if (deleted > 0) {
            postMapper.decrementLikeCount(postId);
        }
    }

    @Override
    @Transactional
    public void favoritePost(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        if (postFavoriteMapper.countByUserAndPost(userId, postId) > 0) return;
        postFavoriteMapper.insert(userId, postId, LocalDateTime.now());
        postMapper.incrementFavoriteCount(postId);
    }

    @Override
    @Transactional
    public void unfavoritePost(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        int deleted = postFavoriteMapper.deleteByUserAndPost(userId, postId);
        if (deleted > 0) {
            postMapper.decrementFavoriteCount(postId);
        }
    }

    @Override
    public boolean isPostLiked(Long postId, Long userId) {
        if (postId == null || userId == null) return false;
        return postLikeMapper.countByUserAndPost(userId, postId) > 0;
    }

    @Override
    public boolean isPostFavorited(Long postId, Long userId) {
        if (postId == null || userId == null) return false;
        return postFavoriteMapper.countByUserAndPost(userId, postId) > 0;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        if (postId == null || userId == null) return;
        Post p = postMapper.selectById(postId);
        if (p == null || !userId.equals(p.getUserId())) return;
        postMapper.updateStatus(postId, 2); // 2=删除
    }

    @Override
    public List<Post> listHotPosts(int page, int size) {
        return listHotPostsByCategory(null, page, size);
    }

    @Override
    public List<Post> listHotPostsByCategory(Long categoryId, int page, int size) {
        if (size <= 0) size = 10;
        if (size > 50) size = 50;
        int pageIndex = (page <= 0 ? 0 : page - 1);

        // 取更大的候选池做混合排序，避免只在一页内调整顺序
        int poolSize = Math.max(size * 3, 60);
        if (poolSize > 200) poolSize = 200;

        List<Post> candidates = new ArrayList<>();
        if (categoryId != null) {
            candidates = postMapper.listByHotScoreWithCategory(categoryId, 0, poolSize);
        } else if (postHotService != null) {
            List<Long> ids = postHotService.getHotPostIds(0, poolSize);
            if (!ids.isEmpty()) {
                for (Long id : ids) {
                    Post p = postMapper.selectDetailById(id);
                    if (p != null && p.getStatus() != null && p.getStatus() == 0) {
                        candidates.add(p);
                    }
                }
            }
            if (candidates.isEmpty()) {
                candidates = postMapper.listByHotScore(0, poolSize);
            }
        } else {
            candidates = postMapper.listByHotScore(0, poolSize);
        }
        if (candidates.isEmpty()) return List.of();
        return applyMixedHotAndRecency(candidates, pageIndex, size);
    }

    @Override
    public List<Post> listByUserId(Long userId, int page, int size) {
        if (userId == null) return List.of();
        if (size <= 0) size = 20;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByUserId(userId, offset, size);
    }

    @Override
    public List<Post> listFavoritePosts(Long userId, int page, int size) {
        if (userId == null) return List.of();
        if (size <= 0) size = 20;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByUserFavorites(userId, offset, size);
    }

    @Override
    public List<Post> listLikedPosts(Long userId, int page, int size) {
        if (userId == null) return List.of();
        if (size <= 0) size = 20;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByUserLikes(userId, offset, size);
    }

    @Override
    public List<Post> searchPosts(String keyword, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.searchByKeyword(keyword, offset, size);
    }

    @Override
    @Transactional
    public void approvePost(Long postId) {
        if (postId == null) return;
        Post post = postMapper.selectDetailById(postId);
        if (post == null) return;
        postMapper.updateStatus(postId, 0);
        sendSystemNotify(post.getUserId(), buildApprovedText(post.getTitle()));
    }

    @Override
    @Transactional
    public void rejectPost(Long postId) {
        if (postId == null) return;
        Post post = postMapper.selectDetailById(postId);
        if (post == null) return;
        postMapper.updateStatus(postId, 2);
        sendSystemNotify(post.getUserId(), buildRejectedText(post.getTitle()));
    }

    private void sendSystemNotify(Long toUserId, String content) {
        if (chatService == null || toUserId == null || content == null || content.isBlank()) return;
        try {
            Long to = toUserId;
            String text = content;
            if (transactionManager != null) {
                // 在独立事务中发送，避免 sendMessage 失败导致 approvePost 事务被标记 rollback-only
                TransactionTemplate tt = new TransactionTemplate(transactionManager);
                tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tt.executeWithoutResult(status -> {
                    SendMessageRequest req = new SendMessageRequest();
                    req.setFromUserId(SYSTEM_ADMIN_ID);
                    req.setToUserId(to);
                    req.setContent(text);
                    req.setContentType(0);
                    chatService.sendMessage(req);
                });
            } else {
                SendMessageRequest req = new SendMessageRequest();
                req.setFromUserId(SYSTEM_ADMIN_ID);
                req.setToUserId(to);
                req.setContent(text);
                req.setContentType(0);
                chatService.sendMessage(req);
            }
        } catch (Exception ignored) {
        }
    }

    private String buildPendingText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "已提交审核，审核通过后会自动发布。";
    }

    private String buildApprovedText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "已通过审核，现已发布。";
    }

    private String buildRejectedText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "未通过审核，请根据平台规范修改后重新发布。";
    }

    @Override
    public List<Post> listRecommended(Long userId, int page, int size) {
        if (size <= 0) size = 10;
        if (size > 50) size = 50;
        int from = (page <= 0 ? 0 : page - 1) * size;

        // 未登录或冷启动：直接按热度
        if (userId == null) {
            return listHotPosts(page, size);
        }

        List<Long> authorIds = postMapper.selectInteractedAuthorIds(userId, 100);
        List<Long> categoryIds = postMapper.selectInteractedCategoryIds(userId, 50);
        if (authorIds.isEmpty() && categoryIds.isEmpty()) {
            return listHotPosts(page, size);
        }

        Set<Long> authorSet = new HashSet<>(authorIds);
        Set<Long> categorySet = new HashSet<>(categoryIds);

        // 取较多候选再重排，保证分页有足够数据
        int poolSize = Math.max(size * 5, 100);
        if (poolSize > 300) poolSize = 300;
        List<Post> candidates = postMapper.listByHotScore(0, poolSize);
        if (candidates.isEmpty()) return List.of();

        candidates.sort((a, b) -> Double.compare(
                effectiveRecommendScore(b, authorSet, categorySet),
                effectiveRecommendScore(a, authorSet, categorySet)));

        if (from >= candidates.size()) return List.of();
        int to = Math.min(from + size, candidates.size());
        return new ArrayList<>(candidates.subList(from, to));
    }

    /**
     * 推荐得分 = 热度 × 偏好权重（互动过的作者 +0.5，互动过的分区 +0.3）
     */
    private double effectiveRecommendScore(Post p, Set<Long> authorSet, Set<Long> categorySet) {
        double base = (p.getHotScore() != null && p.getHotScore() > 0) ? p.getHotScore() : 0.01;
        double w = 1.0;
        if (p.getUserId() != null && authorSet.contains(p.getUserId())) w += 0.5;
        if (p.getCategoryId1() != null && categorySet.contains(p.getCategoryId1())) w += 0.3;
        if (p.getCategoryId2() != null && categorySet.contains(p.getCategoryId2())) w += 0.3;
        return base * w;
    }

    // -------------- 话题解析与绑定 --------------

    private static final java.util.regex.Pattern TOPIC_PATTERN =
            java.util.regex.Pattern.compile("#([^#\\s]{1,20})");

    private List<String> extractTopics(String text) {
        List<String> list = new ArrayList<>();
        if (text == null) return list;
        var m = TOPIC_PATTERN.matcher(text);
        while (m.find()) {
            String name = m.group(1).trim();
            if (!name.isEmpty() && !list.contains(name)) {
                list.add(name);
            }
        }
        return list;
    }

    private String toSlug(String name) {
        String s = name.trim().toLowerCase().replaceAll("\\s+", "-");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    private void bindTopicsForPost(Long postId, List<String> topicNames) {
        if (postId == null || topicNames == null || topicNames.isEmpty()) return;
        for (String raw : topicNames) {
            String name = raw.trim();
            if (name.isEmpty()) continue;
            org.example.myblog.entiy.Topic topic = topicMapper.selectByName(name);
            if (topic == null) {
                topic = new org.example.myblog.entiy.Topic();
                topic.setName(name);
                topic.setSlug(toSlug(name));
                topicMapper.insert(topic);
            }
            topicMapper.insertPostTopic(postId, topic.getId());
            topicMapper.incrPostCount(topic.getId());
        }
    }

    /**
     * 热度 + 新鲜度混合排序：
     * finalScore = α * hotNorm + (1-α) * recencyScore
     * - hotNorm: 在候选集合内对 hotScore 做 min-max 归一化
     * - recencyScore: 按发布时间的 1/(1+ageHours) 计算，越新越接近 1
     */
    private List<Post> applyMixedHotAndRecency(List<Post> candidates, int pageIndex, int size) {
        double alpha = 0.7;
        if (hotProperties != null) {
            alpha = hotProperties.getMixAlpha();
        }
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;

        long now = System.currentTimeMillis();

        double minHot = Double.POSITIVE_INFINITY;
        double maxHot = Double.NEGATIVE_INFINITY;
        for (Post p : candidates) {
            double hs = p.getHotScore() != null ? p.getHotScore() : 0.0;
            if (hs < minHot) minHot = hs;
            if (hs > maxHot) maxHot = hs;
        }
        if (!Double.isFinite(minHot)) {
            minHot = 0;
            maxHot = 1;
        }
        double range = maxHot - minHot;
        if (range <= 0) range = 1;

        Map<Long, Double> scoreMap = new HashMap<>();
        for (Post p : candidates) {
            double hs = p.getHotScore() != null ? p.getHotScore() : 0.0;
            double hotNorm = (hs - minHot) / range;

            long ageHours = 0;
            if (p.getCreatedAt() != null) {
                long createdMs = p.getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                long diff = now - createdMs;
                if (diff > 0) {
                    ageHours = diff / (1000 * 3600);
                }
            }
            if (ageHours < 0) ageHours = 0;
            double recency = 1.0 / (1.0 + ageHours);

            double finalScore = alpha * hotNorm + (1 - alpha) * recency;
            scoreMap.put(p.getId(), finalScore);
        }

        candidates.sort((a, b) -> {
            double sb = scoreMap.getOrDefault(b.getId(), 0.0);
            double sa = scoreMap.getOrDefault(a.getId(), 0.0);
            int cmp = Double.compare(sb, sa);
            if (cmp != 0) return cmp;
            // 次级排序：更近的时间排前
            LocalDateTime tb = b.getCreatedAt();
            LocalDateTime ta = a.getCreatedAt();
            if (tb != null && ta != null) {
                return tb.compareTo(ta) * -1;
            }
            return 0;
        });

        int from = pageIndex * size;
        if (from >= candidates.size()) return List.of();
        int to = Math.min(from + size, candidates.size());
        return new ArrayList<>(candidates.subList(from, to));
    }

    @Override
    public List<Post> listRelatedPosts(Long postId, Long userId, int size) {
        if (size <= 0) size = 6;
        if (size > 20) size = 20;

        // 冷启动：没有 postId 就走推荐流（未登录则热度）
        if (postId == null) {
            return listRecommended(userId, 1, size);
        }

        String cacheKey = "post:related:" + postId + ":" + (userId == null ? 0 : userId);
        List<Post> cached = getCachedRelated(cacheKey, size);
        if (!cached.isEmpty()) return cached;

        Post current = postMapper.selectDetailById(postId);
        if (current == null) {
            List<Post> fallback = listRecommended(userId, 1, size);
            cacheRelated(cacheKey, fallback);
            return fallback;
        }

        int pool = Math.max(size * 6, 60);
        if (pool > 200) pool = 200;

        Set<Long> seen = new HashSet<>();
        List<Post> merged = new ArrayList<>();
        seen.add(postId);

        // 1) 同话题
        List<String> topics = new ArrayList<>();
        if (current.getTopicNames() != null && !current.getTopicNames().isBlank()) {
            topics = Arrays.stream(current.getTopicNames().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
        int topicPool = Math.min(pool, 80);
        for (int i = 0; i < Math.min(2, topics.size()); i++) {
            appendFiltered(merged, postMapper.listByTopic(topics.get(i), 0, topicPool), seen);
        }

        // 2) 同分区（主分区优先）
        if (current.getCategoryId1() != null) {
            appendFiltered(merged, postMapper.listByHotScoreWithCategory(current.getCategoryId1(), 0, pool), seen);
        }
        if (current.getCategoryId2() != null) {
            appendFiltered(merged, postMapper.listByHotScoreWithCategory(current.getCategoryId2(), 0, pool / 2), seen);
        }

        // 3) 标题关键词（1-2 个）
        List<String> keywords = extractKeywords(current.getTitle(), topics);
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            appendFiltered(merged, postMapper.searchByKeyword(kw, 0, pool / 2), seen);
        }

        // 4) 用户行为推荐兜底（“看了这篇的人也看了”）
        if (merged.size() < size) {
            appendFiltered(merged, listRecommended(userId, 1, Math.max(size * 3, 20)), seen);
        }

        List<Post> ranked = rankRelated(current, topics, merged);
        List<Post> result = ranked.size() > size ? ranked.subList(0, size) : ranked;
        cacheRelated(cacheKey, result);
        return result;
    }

    private List<Post> getCachedRelated(String key, int size) {
        if (redisTemplate == null || key == null) return List.of();
        try {
            String s = redisTemplate.opsForValue().get(key);
            if (s == null || s.isBlank()) return List.of();
            List<Post> list = JSON.parseArray(s, Post.class);
            if (list == null || list.isEmpty()) return List.of();
            return list.size() > size ? list.subList(0, size) : list;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void cacheRelated(String key, List<Post> list) {
        if (redisTemplate == null || key == null || list == null) return;
        try {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(list), Duration.ofMinutes(30));
        } catch (Exception ignored) {
        }
    }

    private void appendFiltered(List<Post> out, List<Post> in, Set<Long> seen) {
        if (out == null || in == null || seen == null) return;
        for (Post p : in) {
            if (p == null || p.getId() == null) continue;
            if (seen.contains(p.getId())) continue;
            if (p.getStatus() != null && p.getStatus() != 0) continue;
            if (p.getVisibility() != null && p.getVisibility() != 0) continue;
            seen.add(p.getId());
            out.add(p);
        }
    }

    private List<String> extractKeywords(String title, List<String> topics) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (topics != null) {
            for (String t : topics) {
                if (t != null && !t.isBlank()) set.add(t.trim());
                if (set.size() >= 2) break;
            }
        }
        String s = title == null ? "" : title.trim();
        if (!s.isBlank()) {
            try {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u4e00-\\u9fa5]{2,6}").matcher(s);
                while (m.find() && set.size() < 2) {
                    set.add(m.group());
                }
            } catch (Exception ignored) {
            }
            for (String w : s.split("\\s+")) {
                if (set.size() >= 2) break;
                if (w == null) continue;
                String v = w.trim();
                if (v.length() >= 2 && v.length() <= 12) set.add(v);
            }
        }
        return new ArrayList<>(set);
    }

    private List<Post> rankRelated(Post current, List<String> topics, List<Post> candidates) {
        if (candidates == null || candidates.isEmpty()) return List.of();
        Set<String> topicSet = new HashSet<>();
        if (topics != null) {
            for (String t : topics) {
                if (t != null && !t.isBlank()) topicSet.add(t.trim());
            }
        }
        Long c1 = current.getCategoryId1();
        Long c2 = current.getCategoryId2();
        long nowMs = System.currentTimeMillis();

        candidates.sort(Comparator.comparingDouble((Post p) -> scoreRelated(p, topicSet, c1, c2, nowMs)).reversed());
        return candidates;
    }

    private double scoreRelated(Post p, Set<String> topicSet, Long c1, Long c2, long nowMs) {
        if (p == null) return 0;
        double score = 0;
        if (p.getHotScore() != null) score += p.getHotScore();
        if (p.getCreatedAt() != null) {
            long createdMs = p.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long days = Math.max(0, (nowMs - createdMs) / (24L * 3600_000L));
            score += Math.max(0, 30 - days);
        }
        if (c1 != null && (c1.equals(p.getCategoryId1()) || c1.equals(p.getCategoryId2()))) score += 50;
        if (c2 != null && (c2.equals(p.getCategoryId1()) || c2.equals(p.getCategoryId2()))) score += 20;
        if (topicSet != null && !topicSet.isEmpty() && p.getTopicNames() != null) {
            for (String t : p.getTopicNames().split(",")) {
                String tt = t == null ? "" : t.trim();
                if (!tt.isBlank() && topicSet.contains(tt)) {
                    score += 80;
                    break;
                }
            }
        }
        return score;
    }
}

