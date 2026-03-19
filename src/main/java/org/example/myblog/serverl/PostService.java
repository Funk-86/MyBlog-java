package org.example.myblog.serverl;

import org.example.myblog.entiy.Post;

import java.util.List;

public interface PostService {

    /**
     * 发表帖子（支持主/副两个分区）
     *
     * @param userId      发帖用户
     * @param content     帖子内容
     * @param type        类型：0文字,1图文,2视频
     * @param categoryId1 主分区，可以为 null
     * @param categoryId2 副分区，可以为 null
     */
    Post createPost(Long userId,
                    String content,
                    Integer type,
                    Long categoryId1,
                    Long categoryId2);

    /**
     * 根据分区查询帖子列表（命中主/副任意一个分区都返回）
     */
    List<Post> listByCategory(Long categoryId);

    /**
     * 随机获取帖子列表
     *
     * @param limit 一次返回的帖子数量
     */
    List<Post> listRandom(int limit);

    /**
     * 查询当前用户已关注用户的帖子列表
     *
     * @param followerId 当前用户 ID
     * @param limit      一次返回的帖子数量
     */
    List<Post> listFollowedPosts(Long followerId, int limit);

    /**
     * 发帖并保存图片或视频（支持最多 2 个分区）
     * 若 videoUrl 非空则发视频帖（type=2），否则按 imageUrls 发图文（type=1）或文字（type=0）
     */
    Post createPostWithImages(Long userId,
                              String title,
                              String content,
                              List<String> imageUrls,
                              Long categoryId1,
                              Long categoryId2,
                              List<String> topics,
                              String videoUrl,
                              String videoCoverUrl,
                              Integer videoDurationSeconds,
                              Integer visibility);

    /**
     * 根据 ID 查询帖子详情（含用户信息、图片）
     */
    Post getPostDetail(Long id);

    /** 帖子点赞 */
    void likePost(Long postId, Long userId);

    /** 取消帖子点赞 */
    void unlikePost(Long postId, Long userId);

    /** 帖子收藏 */
    void favoritePost(Long postId, Long userId);

    /** 取消帖子收藏 */
    void unfavoritePost(Long postId, Long userId);

    /** 删除帖子（仅作者可操作，软删除 status=2） */
    void deletePost(Long postId, Long userId);

    /** 是否已赞该帖子 */
    boolean isPostLiked(Long postId, Long userId);

    /** 是否已收藏该帖子 */
    boolean isPostFavorited(Long postId, Long userId);

    /**
     * 按热度获取帖子列表（优先 Redis ZSet，降级 MySQL）
     */
    List<Post> listHotPosts(int page, int size);

    /** 按分区筛选的热点列表（categoryId 为 null 时同 listHotPosts） */
    List<Post> listHotPostsByCategory(Long categoryId, int page, int size);

    /**
     * 按用户查询帖子（个人空间动态/投稿）
     */
    List<Post> listByUserId(Long userId, int page, int size);

    /** 用户收藏的帖子列表 */
    List<Post> listFavoritePosts(Long userId, int page, int size);

    /** 用户点赞的帖子列表 */
    List<Post> listLikedPosts(Long userId, int page, int size);

    /**
     * 关键字搜索帖子（按标题 / 内容模糊匹配）
     */
    List<Post> searchPosts(String keyword, int page, int size);

    /** 审核通过帖子：status 置为 0，并发送系统通知 */
    void approvePost(Long postId);

    /** 审核拒绝帖子：status 置为 2，并发送系统通知 */
    void rejectPost(Long postId);

    /**
     * 个性化推荐流：热度 × 用户偏好加权，冷启动时退化为热度排序
     * @param userId 当前用户 ID，可为 null（未登录则按热度）
     * @param page 页码，从 1 开始
     * @param size 每页条数
     */
    List<Post> listRecommended(Long userId, int page, int size);

    /**
     * 帖子详情页相关推荐（异步加载用）
     * - 优先：同话题/同分区 + 关键词匹配
     * - 辅助：用户行为推荐（协同过滤风格，冷启动退化为热度）
     */
    List<Post> listRelatedPosts(Long postId, Long userId, int size);
}

