package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.Post;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {

    /**
     * 新增帖子（支持两个分区）
     */
    @Insert("""
            INSERT INTO post (user_id, title, category_id_1, category_id_2, content, type, status, visibility)
            VALUES (#{userId}, #{title}, #{categoryId1}, #{categoryId2}, #{content}, #{type}, #{status}, #{visibility})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    /**
     * 根据主键查询帖子
     */
    @Select("""
            SELECT id,
                   user_id       AS userId,
                   title,
                   category_id_1 AS categoryId1,
                   category_id_2 AS categoryId2,
                   content,
                   type,
                   status,
                   COALESCE(visibility, 0) AS visibility,
                   like_count     AS likeCount,
                   comment_count  AS commentCount,
                   share_count    AS shareCount,
                   view_count     AS viewCount,
                   favorite_count AS favoriteCount,
                   hot_score      AS hotScore,
                   created_at     AS createdAt,
                   updated_at     AS updatedAt
            FROM post
            WHERE id = #{id}
            """)
    Post selectById(@Param("id") Long id);

    /**
     * 根据分区查询帖子（主分区或副分区命中都算）
     */
    @Select("""
            SELECT id,
                   user_id       AS userId,
                   title,
                   category_id_1 AS categoryId1,
                   category_id_2 AS categoryId2,
                   content,
                   type,
                   status,
                   COALESCE(visibility, 0) AS visibility,
                   like_count     AS likeCount,
                   comment_count  AS commentCount,
                   share_count    AS shareCount,
                   view_count     AS viewCount,
                   favorite_count AS favoriteCount,
                   hot_score      AS hotScore,
                   created_at     AS createdAt,
                   updated_at     AS updatedAt
            FROM post
            WHERE category_id_1 = #{categoryId}
               OR category_id_2 = #{categoryId}
            ORDER BY created_at DESC
            """)
    List<Post> listByCategory(@Param("categoryId") Long categoryId);

    /**
     * 随机获取指定数量的帖子
     * 用于首页推荐、下拉刷新、上拉加载更多
     * 同时联表查询用户名和头像
     */
    @Select("""
            SELECT p.id,
                   p.user_id       AS userId,
                   p.title,
                   p.category_id_1 AS categoryId1,
                   p.category_id_2 AS categoryId2,
                   p.content,
                   p.type,
                   p.status,
                   p.like_count     AS likeCount,
                   p.comment_count  AS commentCount,
                   p.share_count    AS shareCount,
                   p.view_count     AS viewCount,
                   p.favorite_count AS favoriteCount,
                   p.hot_score      AS hotScore,
                   p.created_at     AS createdAt,
                   p.updated_at     AS updatedAt,
                   u.username,
                   up.nickname,
                   up.avatar_url   AS avatarUrl,
                   img.firstImageUrl,
                   img.imageUrls,
                   c1.name AS categoryName1,
                   c2.name AS categoryName2,
                   vid.firstVideoUrl,
                   vid.cover_url AS first_video_cover_url,
                   vid.firstVideoDuration
            FROM post p
                     LEFT JOIN `user` u ON u.id = p.user_id
                     LEFT JOIN user_profile up ON up.user_id = p.user_id
                     LEFT JOIN (
                        SELECT pm.post_id,
                               MIN(CASE WHEN pm.media_type = 1 THEN pm.url END)                                                  AS firstImageUrl,
                               GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                        FROM post_media pm
                        GROUP BY pm.post_id
                     ) img ON img.post_id = p.id
                     LEFT JOIN (
                        SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                        FROM post_media WHERE media_type = 2 GROUP BY post_id
                     ) vid ON vid.post_id = p.id
                     LEFT JOIN category c1 ON c1.id = p.category_id_1
                     LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE p.status = 0 AND COALESCE(p.visibility, 0) = 0
            ORDER BY p.created_at DESC
            LIMIT #{limit}
            """)
    List<Post> listRandom(@Param("limit") int limit);

    /**
     * 关键字搜索帖子（按发布时间倒序）
     * 标题或内容包含 keyword 即可命中
     */
    @Select("""
            <script>
            SELECT p.id,
                   p.user_id       AS userId,
                   p.title,
                   p.category_id_1 AS categoryId1,
                   p.category_id_2 AS categoryId2,
                   p.content,
                   p.type,
                   p.status,
                   p.like_count     AS likeCount,
                   p.comment_count  AS commentCount,
                   p.share_count    AS shareCount,
                   p.view_count     AS viewCount,
                   p.favorite_count AS favoriteCount,
                   p.hot_score      AS hotScore,
                   p.created_at     AS createdAt,
                   p.updated_at     AS updatedAt,
                   u.username,
                   up.nickname,
                   up.avatar_url   AS avatarUrl,
                   img.firstImageUrl,
                   img.imageUrls,
                   c1.name AS categoryName1,
                   c2.name AS categoryName2,
                   vid.firstVideoUrl,
                   vid.cover_url AS first_video_cover_url,
                   vid.firstVideoDuration
            FROM post p
                     LEFT JOIN `user` u ON u.id = p.user_id
                     LEFT JOIN user_profile up ON up.user_id = p.user_id
                     LEFT JOIN (
                        SELECT pm.post_id,
                               MIN(CASE WHEN pm.media_type = 1 THEN pm.url END)                                                  AS firstImageUrl,
                               GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                        FROM post_media pm
                        GROUP BY pm.post_id
                     ) img ON img.post_id = p.id
                     LEFT JOIN (
                        SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                        FROM post_media WHERE media_type = 2 GROUP BY post_id
                     ) vid ON vid.post_id = p.id
                     LEFT JOIN category c1 ON c1.id = p.category_id_1
                     LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE p.status = 0 AND COALESCE(p.visibility, 0) = 0
              <if test="keyword != null and keyword != ''">
                AND (p.title LIKE CONCAT('%', #{keyword}, '%')
                  OR p.content LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<Post> searchByKeyword(@Param("keyword") String keyword,
                               @Param("offset") int offset,
                               @Param("limit") int limit);

    /**
     * 查询当前用户已关注用户的帖子，同时联表用户名和头像
     */
    @Select("""
            SELECT p.id,
                   p.user_id       AS userId,
                   p.title,
                   p.category_id_1 AS categoryId1,
                   p.category_id_2 AS categoryId2,
                   p.content,
                   p.type,
                   p.status,
                   p.like_count     AS likeCount,
                   p.comment_count  AS commentCount,
                   p.share_count    AS shareCount,
                   p.view_count     AS viewCount,
                   p.favorite_count AS favoriteCount,
                   p.hot_score      AS hotScore,
                   p.created_at     AS createdAt,
                   p.updated_at     AS updatedAt,
                   u.username,
                   up.nickname,
                   up.avatar_url   AS avatarUrl,
                   img.firstImageUrl,
                   img.imageUrls,
                   c1.name AS categoryName1,
                   c2.name AS categoryName2,
                   vid.firstVideoUrl,
                   vid.cover_url AS first_video_cover_url,
                   vid.firstVideoDuration
            FROM post p
                     JOIN user_follow f ON p.user_id = f.followee_id
                     LEFT JOIN `user` u ON u.id = p.user_id
                     LEFT JOIN user_profile up ON up.user_id = p.user_id
                     LEFT JOIN (
                        SELECT pm.post_id,
                               MIN(CASE WHEN pm.media_type = 1 THEN pm.url END)                                                  AS firstImageUrl,
                               GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                        FROM post_media pm
                        GROUP BY pm.post_id
                     ) img ON img.post_id = p.id
                     LEFT JOIN (
                        SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                        FROM post_media WHERE media_type = 2 GROUP BY post_id
                     ) vid ON vid.post_id = p.id
                     LEFT JOIN category c1 ON c1.id = p.category_id_1
                     LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE f.follower_id = #{followerId}
              AND f.status = 0
              AND p.status = 0
              AND COALESCE(p.visibility, 0) = 0
            ORDER BY p.created_at DESC
            LIMIT #{limit}
            """)
    List<Post> listByFollowedUsers(@Param("followerId") Long followerId,
                                   @Param("limit") int limit);

    /**
     * 按用户查询帖子（个人空间动态/投稿）
     */
    @Select("""
            SELECT p.id,
                   p.user_id       AS userId,
                   p.title,
                   p.category_id_1 AS categoryId1,
                   p.category_id_2 AS categoryId2,
                   p.content,
                   p.type,
                   p.status,
                   p.like_count     AS likeCount,
                   p.comment_count  AS commentCount,
                   p.view_count     AS viewCount,
                   p.created_at     AS createdAt,
                   u.username,
                   up.nickname,
                   up.avatar_url   AS avatarUrl,
                   img.firstImageUrl,
                   img.imageUrls,
                   vid.firstVideoUrl,
                   vid.cover_url AS first_video_cover_url,
                   vid.firstVideoDuration
            FROM post p
                     LEFT JOIN `user` u ON u.id = p.user_id
                     LEFT JOIN user_profile up ON up.user_id = p.user_id
                     LEFT JOIN (
                        SELECT pm.post_id,
                               MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                               GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                        FROM post_media pm
                        GROUP BY pm.post_id
                     ) img ON img.post_id = p.id
                     LEFT JOIN (
                        SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                        FROM post_media WHERE media_type = 2 GROUP BY post_id
                     ) vid ON vid.post_id = p.id
            WHERE p.user_id = #{userId} AND p.status = 0
            ORDER BY p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByUserId(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 用户收藏的帖子列表（含分类、视频，与首页展示一致）
     */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.content, p.type, p.status,
                   p.like_count AS likeCount, p.comment_count AS commentCount, p.view_count AS viewCount,
                   p.created_at AS createdAt,
                   u.username, up.nickname, up.avatar_url AS avatarUrl,
                   img.firstImageUrl, img.imageUrls,
                   vid.firstVideoUrl, vid.cover_url AS firstVideoCoverUrl, vid.firstVideoDuration,
                   c1.name AS categoryName1, c2.name AS categoryName2
            FROM post_favorite pf
            JOIN post p ON p.id = pf.post_id AND p.status = 0
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN (
                SELECT pm.post_id,
                       MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                       GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                FROM post_media pm
                GROUP BY pm.post_id
            ) img ON img.post_id = p.id
            LEFT JOIN (
                SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                FROM post_media WHERE media_type = 2 GROUP BY post_id
            ) vid ON vid.post_id = p.id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE pf.user_id = #{userId}
            ORDER BY pf.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByUserFavorites(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 用户点赞的帖子列表（含分类、视频，与首页展示一致）
     */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.content, p.type, p.status,
                   p.like_count AS likeCount, p.comment_count AS commentCount, p.view_count AS viewCount,
                   p.created_at AS createdAt,
                   u.username, up.nickname, up.avatar_url AS avatarUrl,
                   img.firstImageUrl, img.imageUrls,
                   vid.firstVideoUrl, vid.cover_url AS firstVideoCoverUrl, vid.firstVideoDuration,
                   c1.name AS categoryName1, c2.name AS categoryName2
            FROM post_like pl
            JOIN post p ON p.id = pl.post_id AND p.status = 0
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN (
                SELECT pm.post_id,
                       MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                       GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                FROM post_media pm
                GROUP BY pm.post_id
            ) img ON img.post_id = p.id
            LEFT JOIN (
                SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                FROM post_media WHERE media_type = 2 GROUP BY post_id
            ) vid ON vid.post_id = p.id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE pl.user_id = #{userId}
            ORDER BY pl.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByUserLikes(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 帖子详情：包含用户名、头像、多图
     */
    @Select("""
            SELECT p.id,
                   p.user_id       AS userId,
                   p.title,
                   p.category_id_1 AS categoryId1,
                   p.category_id_2 AS categoryId2,
                   p.content,
                   p.type,
                   p.status,
                   p.like_count     AS likeCount,
                   p.comment_count  AS commentCount,
                   p.share_count    AS shareCount,
                   p.view_count     AS viewCount,
                   p.favorite_count AS favoriteCount,
                   p.hot_score      AS hotScore,
                   p.created_at     AS createdAt,
                   p.updated_at     AS updatedAt,
                   u.username,
                   up.nickname,
                   up.avatar_url   AS avatarUrl,
                   img.firstImageUrl,
                   img.imageUrls,
                   vid.firstVideoUrl,
                   vid.cover_url AS first_video_cover_url,
                   vid.firstVideoDuration,
                   c1.name AS categoryName1,
                   c2.name AS categoryName2,
                   tp.topicNames AS topicNames
            FROM post p
                     LEFT JOIN `user` u ON u.id = p.user_id
                     LEFT JOIN user_profile up ON up.user_id = p.user_id
                     LEFT JOIN (
                        SELECT pm.post_id,
                               MIN(CASE WHEN pm.media_type = 1 THEN pm.url END)                                                  AS firstImageUrl,
                               GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
                        FROM post_media pm
                        GROUP BY pm.post_id
                     ) img ON img.post_id = p.id
                     LEFT JOIN (
                        SELECT post_id, MIN(url) AS firstVideoUrl, MIN(cover_url) AS cover_url, MIN(duration_sec) AS firstVideoDuration
                        FROM post_media WHERE media_type = 2 GROUP BY post_id
                     ) vid ON vid.post_id = p.id
                     LEFT JOIN category c1 ON c1.id = p.category_id_1
                     LEFT JOIN category c2 ON c2.id = p.category_id_2
                     LEFT JOIN (
                        SELECT pt.post_id,
                               GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',') AS topicNames
                        FROM post_topic pt
                        JOIN topic t ON t.id = pt.topic_id
                        GROUP BY pt.post_id
                     ) tp ON tp.post_id = p.id
            WHERE p.id = #{id}
            """)
    Post selectDetailById(@Param("id") Long id);

    /**
     * 评论数 +1
     */
    @Update("""
            UPDATE post
            SET comment_count = comment_count + 1
            WHERE id = #{postId}
            """)
    int incrementCommentCount(@Param("postId") Long postId);

    @Update("""
            UPDATE post
            SET comment_count = GREATEST(0, comment_count - 1)
            WHERE id = #{postId}
            """)
    int decrementCommentCount(@Param("postId") Long postId);

    /**
     * 更新帖子状态
     * status: 0正常,1审核中,2删除,3屏蔽
     */
    @Update("""
            UPDATE post
            SET status = #{status}, updated_at = NOW()
            WHERE id = #{postId}
            """)
    int updateStatus(@Param("postId") Long postId, @Param("status") int status);

    /**
     * 点赞数 +1
     */
    @Update("""
            UPDATE post
            SET like_count = like_count + 1
            WHERE id = #{postId}
            """)
    int incrementLikeCount(@Param("postId") Long postId);

    /**
     * 点赞数 -1（不低于 0）
     */
    @Update("""
            UPDATE post
            SET like_count = GREATEST(0, like_count - 1)
            WHERE id = #{postId}
            """)
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 浏览量同步：直接设置 view_count
     */
    @Update("""
            UPDATE post SET view_count = #{viewCount} WHERE id = #{postId}
            """)
    int updateViewCount(@Param("postId") Long postId, @Param("viewCount") Integer viewCount);

    /**
     * 热度分数同步
     */
    @Update("""
            UPDATE post SET hot_score = #{hotScore} WHERE id = #{postId}
            """)
    int updateHotScore(@Param("postId") Long postId, @Param("hotScore") Double hotScore);

    /**
     * 收藏数 +1
     */
    @Update("""
            UPDATE post SET favorite_count = favorite_count + 1 WHERE id = #{postId}
            """)
    int incrementFavoriteCount(@Param("postId") Long postId);

    /**
     * 收藏数 -1（不低于 0）
     */
    @Update("""
            UPDATE post SET favorite_count = GREATEST(0, favorite_count - 1) WHERE id = #{postId}
            """)
    int decrementFavoriteCount(@Param("postId") Long postId);

    /**
     * 按热度倒序分页查询（MySQL 降级）
     */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.category_id_1 AS categoryId1, p.category_id_2 AS categoryId2,
                   p.content, p.type, p.status, p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.share_count AS shareCount, p.view_count AS viewCount, p.favorite_count AS favoriteCount,
                   p.hot_score AS hotScore, p.created_at AS createdAt, p.updated_at AS UpdatedAt,
                   u.username, up.nickname, up.avatar_url AS avatarUrl, img.firstImageUrl, img.imageUrls,
                   c1.name AS categoryName1, c2.name AS categoryName2,
                   vid.firstVideoUrl, vid.cover_url AS first_video_cover_url, vid.firstVideoDuration,
                   tp.topicNames AS topicNames
            FROM post p
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN (
              SELECT pm.post_id,
                MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
              FROM post_media pm GROUP BY pm.post_id
            ) img ON img.post_id = p.id
            LEFT JOIN (
              SELECT post_id,
                     MIN(url) AS firstVideoUrl,
                     MIN(cover_url) AS cover_url,
                     MIN(duration_sec) AS firstVideoDuration
              FROM post_media
              WHERE media_type = 2
              GROUP BY post_id
            ) vid ON vid.post_id = p.id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            LEFT JOIN (
              SELECT pt.post_id,
                     GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',') AS topicNames
              FROM post_topic pt
              JOIN topic t ON t.id = pt.topic_id
              GROUP BY pt.post_id
            ) tp ON tp.post_id = p.id
            WHERE p.status = 0 AND COALESCE(p.visibility, 0) = 0
            ORDER BY p.hot_score DESC, p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByHotScore(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 按分区 + 热度倒序分页（热点页点击分区时用）
     */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.category_id_1 AS categoryId1, p.category_id_2 AS categoryId2,
                   p.content, p.type, p.status, p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.share_count AS shareCount, p.view_count AS viewCount, p.favorite_count AS favoriteCount,
                   p.hot_score AS hotScore, p.created_at AS createdAt, p.updated_at AS updatedAt,
                   u.username, up.nickname, up.avatar_url AS avatarUrl, img.firstImageUrl, img.imageUrls,
                   c1.name AS categoryName1, c2.name AS categoryName2,
                   vid.firstVideoUrl, vid.cover_url AS first_video_cover_url, vid.firstVideoDuration,
                   tp.topicNames AS topicNames
            FROM post p
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN (
              SELECT pm.post_id,
                MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
              FROM post_media pm GROUP BY pm.post_id
            ) img ON img.post_id = p.id
            LEFT JOIN (
              SELECT post_id,
                     MIN(url) AS firstVideoUrl,
                     MIN(cover_url) AS cover_url,
                     MIN(duration_sec) AS firstVideoDuration
              FROM post_media
              WHERE media_type = 2
              GROUP BY post_id
            ) vid ON vid.post_id = p.id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            LEFT JOIN (
              SELECT pt.post_id,
                     GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',') AS topicNames
              FROM post_topic pt
              JOIN topic t ON t.id = pt.topic_id
              GROUP BY pt.post_id
            ) tp ON tp.post_id = p.id
            WHERE p.status = 0 AND COALESCE(p.visibility, 0) = 0
              AND (p.category_id_1 = #{categoryId} OR p.category_id_2 = #{categoryId})
            ORDER BY p.hot_score DESC, p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByHotScoreWithCategory(@Param("categoryId") Long categoryId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 个性化推荐：用户通过点赞/收藏/评论互动过的帖子作者 ID 列表（用于偏好加权）
     */
    @Select("""
            (SELECT DISTINCT p.user_id FROM post_like pl JOIN post p ON p.id = pl.post_id WHERE pl.user_id = #{userId} AND p.user_id IS NOT NULL)
            UNION
            (SELECT DISTINCT p.user_id FROM post_favorite pf JOIN post p ON p.id = pf.post_id WHERE pf.user_id = #{userId} AND p.user_id IS NOT NULL)
            UNION
            (SELECT DISTINCT p.user_id FROM comment c JOIN post p ON p.id = c.post_id WHERE c.user_id = #{userId} AND p.user_id IS NOT NULL)
            LIMIT #{limit}
            """)
    List<Long> selectInteractedAuthorIds(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 个性化推荐：用户互动过的帖子所属分区 ID 列表（主/副分区合并去重）
     */
    @Select("""
            (SELECT DISTINCT p.category_id_1 FROM post_like pl JOIN post p ON p.id = pl.post_id WHERE pl.user_id = #{userId} AND p.category_id_1 IS NOT NULL)
            UNION (SELECT DISTINCT p.category_id_2 FROM post_like pl JOIN post p ON p.id = pl.post_id WHERE pl.user_id = #{userId} AND p.category_id_2 IS NOT NULL)
            UNION (SELECT DISTINCT p.category_id_1 FROM post_favorite pf JOIN post p ON p.id = pf.post_id WHERE pf.user_id = #{userId} AND p.category_id_1 IS NOT NULL)
            UNION (SELECT DISTINCT p.category_id_2 FROM post_favorite pf JOIN post p ON p.id = pf.post_id WHERE pf.user_id = #{userId} AND p.category_id_2 IS NOT NULL)
            UNION (SELECT DISTINCT p.category_id_1 FROM comment c JOIN post p ON p.id = c.post_id WHERE c.user_id = #{userId} AND p.category_id_1 IS NOT NULL)
            UNION (SELECT DISTINCT p.category_id_2 FROM comment c JOIN post p ON p.id = c.post_id WHERE c.user_id = #{userId} AND p.category_id_2 IS NOT NULL)
            LIMIT #{limit}
            """)
    List<Long> selectInteractedCategoryIds(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 按话题名称查询帖子列表
     */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.category_id_1 AS categoryId1, p.category_id_2 AS categoryId2,
                   p.content, p.type, p.status, p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.share_count AS shareCount, p.view_count AS viewCount, p.favorite_count AS favoriteCount,
                   p.hot_score AS hotScore, p.created_at AS createdAt, p.updated_at AS updatedAt,
                   u.username, up.nickname, up.avatar_url AS avatarUrl,
                   img.firstImageUrl, img.imageUrls,
                   c1.name AS categoryName1, c2.name AS categoryName2,
                   vid.firstVideoUrl, vid.cover_url AS first_video_cover_url, vid.firstVideoDuration,
                   tp.topicNames AS topicNames
            FROM post p
            JOIN post_topic pt ON pt.post_id = p.id
            JOIN topic t ON t.id = pt.topic_id AND t.name = #{topicName}
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN (
              SELECT pm.post_id,
                     MIN(CASE WHEN pm.media_type = 1 THEN pm.url END) AS firstImageUrl,
                     GROUP_CONCAT(CASE WHEN pm.media_type = 1 THEN pm.url END ORDER BY pm.sort_order SEPARATOR ',') AS imageUrls
              FROM post_media pm GROUP BY pm.post_id
            ) img ON img.post_id = p.id
            LEFT JOIN (
              SELECT post_id,
                     MIN(url) AS firstVideoUrl,
                     MIN(cover_url) AS cover_url,
                     MIN(duration_sec) AS firstVideoDuration
              FROM post_media
              WHERE media_type = 2
              GROUP BY post_id
            ) vid ON vid.post_id = p.id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            LEFT JOIN (
              SELECT pt2.post_id,
                     GROUP_CONCAT(t2.name ORDER BY t2.id SEPARATOR ',') AS topicNames
              FROM post_topic pt2
              JOIN topic t2 ON t2.id = pt2.topic_id
              GROUP BY pt2.post_id
            ) tp ON tp.post_id = p.id
            WHERE p.status = 0 AND COALESCE(p.visibility, 0) = 0
            ORDER BY p.hot_score DESC, p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Post> listByTopic(@Param("topicName") String topicName,
                           @Param("offset") int offset,
                           @Param("limit") int limit);

    /** 文章总数（正常 + 审核中） */
    @Select("SELECT COUNT(*) FROM post WHERE status IN (0, 1)")
    long countAll();

    /** 首页/热门列表用：可见且正常文章总数（status=0，visibility=0） */
    @Select("""
            SELECT COUNT(*)
            FROM post
            WHERE status = 0
              AND COALESCE(visibility, 0) = 0
            """)
    long countVisible();

    /** 指定分类下文章总数（可见且正常，主/副分区任一命中即可） */
    @Select("""
            SELECT COUNT(*)
            FROM post
            WHERE status = 0
              AND COALESCE(visibility, 0) = 0
              AND (category_id_1 = #{categoryId} OR category_id_2 = #{categoryId})
            """)
    long countByCategory(@Param("categoryId") Long categoryId);

    /** 待审核帖子数（status=1 审核中） */
    @Select("SELECT COUNT(*) FROM post WHERE status = 1")
    long countPending();

    /** 管理端：待审核/AI拦截/已通过帖子列表（status=0,1,3），按创建时间倒序 */
    @Select("""
            SELECT p.id, p.user_id AS userId, p.title, p.category_id_1 AS categoryId1, p.category_id_2 AS categoryId2,
                   p.content, p.type, p.status, p.like_count AS likeCount, p.comment_count AS commentCount,
                   p.view_count AS viewCount, p.created_at AS createdAt, p.updated_at AS updatedAt,
                   u.username, up.nickname, c1.name AS categoryName1, c2.name AS categoryName2
            FROM post p
            LEFT JOIN `user` u ON u.id = p.user_id
            LEFT JOIN user_profile up ON up.user_id = p.user_id
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            LEFT JOIN category c2 ON c2.id = p.category_id_2
            WHERE p.status IN (0, 1, 3)
            ORDER BY p.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listPendingPosts(@Param("offset") int offset, @Param("limit") int limit);

    /** 今日新增文章数 */
    @Select("SELECT COUNT(*) FROM post WHERE DATE(created_at) = CURDATE()")
    long countTodayNew();

    /** 总浏览量 */
    @Select("SELECT COALESCE(SUM(COALESCE(view_count, 0)), 0) FROM post")
    long sumViewCount();

    /** 昨日新增文章数 */
    @Select("SELECT COUNT(*) FROM post WHERE DATE(created_at) = DATE_SUB(CURDATE(), INTERVAL 1 DAY)")
    long countYesterdayNew();

    /** 近 N 天每日文章发布数 */
    @Select("""
            SELECT DATE(created_at) AS statDate, COUNT(*) AS cnt
            FROM post
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY)
            GROUP BY DATE(created_at)
            ORDER BY statDate ASC
            """)
    List<Map<String, Object>> listPostCountByDay(@Param("days") int days);

    /** 按评论数排序 Top N 文章 */
    @Select("""
            SELECT p.id, p.title, p.comment_count AS commentCount, p.view_count AS viewCount,
                   p.created_at AS createdAt, c1.name AS categoryName1
            FROM post p
            LEFT JOIN category c1 ON c1.id = p.category_id_1
            WHERE p.status = 0
            ORDER BY p.comment_count DESC, p.view_count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listTopByCommentCount(@Param("limit") int limit);

    /** 各分类文章数（按主分区） */
    @Select("""
            SELECT COALESCE(c.name, '未分类') AS name, COUNT(p.id) AS value
            FROM post p
            LEFT JOIN category c ON c.id = p.category_id_1
            WHERE p.status IN (0, 1)
            GROUP BY p.category_id_1, c.name
            ORDER BY value DESC
            """)
    List<Map<String, Object>> listCategoryPostCount();
}

