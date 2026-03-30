package org.example.myblog.controller;

import org.example.myblog.dto.AdminBatchPostIdsRequest;
import org.example.myblog.dto.CreatePostRequest;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.serverl.PostBehaviorService;
import org.example.myblog.serverl.PostHotService;
import org.example.myblog.serverl.PostService;
import org.example.myblog.serverl.VideoProcessingService;
import org.example.myblog.storage.AliyunOssClientFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 帖子相关接口
 */
@Controller
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired(required = false)
    private PostHotService postHotService;

    @Autowired(required = false)
    private PostBehaviorService postBehaviorService;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private VideoProcessingService videoProcessingService;

    @Autowired(required = false)
    private AliyunOssClientFacade aliyunOssClientFacade;

    /**
     * 视频上传“额定大小”（超过后自动转码降画质）
     * 单位：MB
     */
    @Value("${video.upload.rated-size-mb:50}")
    private long videoRatedSizeMb;

    /** 转码时：最长边（竖/横都按最长边裁剪到该值） */
    @Value("${video.upload.transcode.max-side:720}")
    private int videoTranscodeMaxSide;

    /** 转码时：CRF（值越大画质越低、体积越小；例如 28-32） */
    @Value("${video.upload.transcode.crf:28}")
    private int videoTranscodeCrf;

    /** 转码时：音频码率 kbps */
    @Value("${video.upload.transcode.audio-bitrate-k:96}")
    private int videoTranscodeAudioBitrateK;

    /** 转码最长等待（秒）；超时则直接返回“原视频回退”，避免网关/客户端超时 */
    @Value("${video.upload.transcode.timeout-seconds:20}")
    private int videoTranscodeTimeoutSeconds;

    @Value("${video.upload.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

    @Value("${upload.base-path:.}")
    private String uploadBasePath;

    private Path resolveUploadDir(String subdir) {
        Path base = Paths.get(uploadBasePath == null || uploadBasePath.isEmpty() ? "." : uploadBasePath).toAbsolutePath().normalize();
        return base.resolve(subdir);
    }

    private static volatile Boolean FFMPEG_AVAILABLE = null;

    private boolean isFfmpegAvailable() {
        if (FFMPEG_AVAILABLE != null) return FFMPEG_AVAILABLE;
        synchronized (PostController.class) {
            if (FFMPEG_AVAILABLE != null) return FFMPEG_AVAILABLE;
            try {
                ProcessBuilder pb = new ProcessBuilder(ffmpegBin, "-version");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                boolean ok = p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0;
                FFMPEG_AVAILABLE = ok;
            } catch (Exception e) {
                FFMPEG_AVAILABLE = false;
            }
        }
        return FFMPEG_AVAILABLE;
    }

    /**
     * 管理端：审核列表（含已通过/审核中/AI拦截，分页与帖子列表一致）
     * GET /post/admin/pending?page=1&size=20&status=1 可选 status：0已通过 1审核中 3AI拦截
     */
    @GetMapping("/admin/pending")
    @ResponseBody
    public Map<String, Object> adminPendingPosts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) Integer status) {
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        int offset = Math.max(0, page - 1) * size;
        if (status != null && status != 0 && status != 1 && status != 3) {
            status = null;
        }
        List<Map<String, Object>> list = postMapper.listPendingPosts(offset, size, status);
        long total = postMapper.countPendingAdminList(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    /**
     * 随机获取帖子列表
     * 前端可以这样用：
     * - 进入首页：直接请求一次 size=10
     * - 上拉触底：再请求一次 size=10，追加到列表末尾
     * - 下拉刷新：请求一次 size=10，替换当前列表
     *
     * GET /post/random?size=10
     */
    @GetMapping("/random")
    @ResponseBody
    public List<Post> randomPosts(@RequestParam(value = "size", defaultValue = "10") int size) {
        return postService.listRandom(size);
    }

    /**
     * 帖子详情
     * GET /post/detail?id=1
     * 会记录一次浏览量（Redis 实时 + 异步更新热度）
     */
    @GetMapping("/detail")
    @ResponseBody
    public Post detail(@RequestParam("id") Long id,
                       @RequestParam(value = "userId", required = false) Long userId) {
        Post post = postService.getPostDetail(id, userId);
        if (post != null && postHotService != null) {
            postHotService.incrementView(id);
        }
        if (post != null && postBehaviorService != null) {
            postBehaviorService.recordView(userId, id);
        }
        return post;
    }

    /**
     * 帖子详情页：相关推荐（异步加载）
     * GET /post/related?postId=1&userId=1&size=6
     */
    @GetMapping("/related")
    @ResponseBody
    public List<Post> related(@RequestParam(value = "postId", required = false) Long postId,
                              @RequestParam(value = "userId", required = false) Long userId,
                              @RequestParam(value = "size", defaultValue = "6") int size) {
        return postService.listRelatedPosts(postId, userId, size);
    }

    /**
     * 热门帖子列表（按热度倒序）；可选按分区筛选
     * GET /post/hot?page=1&size=10 或 /post/hot?page=1&size=10&categoryId=2
     */
    @GetMapping("/hot")
    @ResponseBody
    public List<Post> hotPosts(@RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @RequestParam(value = "categoryId", required = false) Long categoryId,
                               @RequestParam(value = "viewerUserId", required = false) Long viewerUserId) {
        return postService.listHotPostsByCategory(categoryId, page, size, viewerUserId);
    }

    /**
     * 管理端：帖子列表（带总数，用于表格分页）
     * GET /post/admin/list?page=1&size=10[&status=0][&categoryId=2][&year=2026][&month=3][&keyword=xxx]
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public Map<String, Object> adminPostList(@RequestParam(value = "page", defaultValue = "1") int page,
                                             @RequestParam(value = "size", defaultValue = "10") int size,
                                             @RequestParam(value = "status", required = false) Integer status,
                                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                                             @RequestParam(value = "year", required = false) Integer year,
                                             @RequestParam(value = "month", required = false) Integer month,
                                             @RequestParam(value = "keyword", required = false) String keyword) {
        if (size <= 0) {
            size = 10;
        }
        if (size > 100) {
            size = 100;
        }
        int offset = Math.max(0, page - 1) * size;
        String kw = keyword != null ? keyword.trim() : null;
        if (kw != null && kw.isEmpty()) {
            kw = null;
        }
        if (month != null && (month < 1 || month > 12)) {
            month = null;
        }
        if (status != null && (status < 0 || status > 3)) {
            status = null;
        }
        List<Post> list = postMapper.listAdminPostPage(offset, size, status, categoryId, year, month, kw);
        long total = postMapper.countAdminPostPage(status, categoryId, year, month, kw);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    /**
     * 管理端：帖子详情（不计浏览量）
     * GET /post/admin/detail?id=1
     */
    @GetMapping("/admin/detail")
    @ResponseBody
    public Post adminDetail(@RequestParam("id") Long id) {
        return postService.getPostDetailForAdmin(id);
    }

    /**
     * 管理端：屏蔽帖子（status=3，App 端不可见）
     * POST /post/admin/shield?postId=1
     */
    @PostMapping("/admin/shield")
    @ResponseBody
    public Map<String, Object> adminShield(@RequestParam("postId") Long postId) {
        postService.adminShieldPost(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 管理端：取消屏蔽（恢复为正常）
     * POST /post/admin/unshield?postId=1
     */
    @PostMapping("/admin/unshield")
    @ResponseBody
    public Map<String, Object> adminUnshield(@RequestParam("postId") Long postId) {
        postService.adminUnshieldPost(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 关键字搜索帖子
     * GET /post/search?keyword=xxx&page=1&size=20
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Post> search(@RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             @RequestParam(value = "size", defaultValue = "20") int size,
                             @RequestParam(value = "viewerUserId", required = false) Long viewerUserId) {
        return postService.searchPosts(keyword, viewerUserId, page, size);
    }

    /**
     * 个性化推荐流（猜你喜欢）
     * 有 userId 且有过互动时：热度 × 偏好加权（互动过的作者/分区加权）；否则按热度
     * GET /post/recommend?userId=1&page=1&size=10
     */
    @GetMapping("/recommend")
    @ResponseBody
    public List<Post> recommend(@RequestParam(value = "userId", required = false) Long userId,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            return postService.listRecommended(userId, page, size);
        } catch (Exception e) {
            // 线上环境若互动相关表/字段不完整，推荐流降级为随机流，避免前端直接 500
            return postService.listRandom(size);
        }
    }

    /**
     * 获取当前用户已关注用户的帖子
     *
     * GET /post/follow?userId=1&size=10
     */
    @GetMapping("/follow")
    @ResponseBody
    public List<Post> followPosts(@RequestParam("userId") Long userId,
                                  @RequestParam(value = "size", defaultValue = "10") int size) {
        return postService.listFollowedPosts(userId, size);
    }

    /**
     * 用户帖子列表（个人空间动态/投稿）
     * GET /post/user?userId=1&page=1&size=20
     */
    @GetMapping("/user")
    @ResponseBody
    public List<Post> userPosts(@RequestParam("userId") Long userId,
                                @RequestParam(value = "page", defaultValue = "1") int page,
                                @RequestParam(value = "size", defaultValue = "20") int size,
                                @RequestParam(value = "viewerUserId", required = false) Long viewerUserId) {
        return postService.listByUserId(userId, viewerUserId, page, size);
    }

    /**
     * 用户收藏的帖子列表 GET /post/favorites?userId=1&page=1&size=20
     * 可选 folderId：不传则全部收藏（「内容」）；传则仅该收藏夹内帖子
     */
    @GetMapping("/favorites")
    @ResponseBody
    public List<Post> favoritePosts(@RequestParam("userId") Long userId,
                                    @RequestParam(value = "folderId", required = false) Long folderId,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return postService.listFavoritePosts(userId, folderId, page, size);
    }

    /**
     * 用户点赞的帖子列表 GET /post/likes?userId=1&page=1&size=20
     */
    @GetMapping("/likes")
    @ResponseBody
    public List<Post> likedPosts(@RequestParam("userId") Long userId,
                                 @RequestParam(value = "page", defaultValue = "1") int page,
                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        return postService.listLikedPosts(userId, page, size);
    }

    /**
     * 分区帖子列表（类似热点列表，但强制筛选某个分区）
     * GET /post/category?categoryId=1&page=1&size=10
     */
    @GetMapping("/category")
    @ResponseBody
    public List<Post> categoryPosts(@RequestParam("categoryId") Long categoryId,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "size", defaultValue = "10") int size,
                                    @RequestParam(value = "viewerUserId", required = false) Long viewerUserId) {
        if (size <= 0) size = 10;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByHotScoreWithCategory(categoryId, offset, size, viewerUserId);
    }

    /**
     * 发布帖子（支持多张图片）
     * POST /post/create
     * Body: JSON { userId, title, content, images: ["...","..."] }
     */
    @PostMapping("/create")
    @ResponseBody
    public Object create(@RequestBody CreatePostRequest req) {
        try {
            Post created = postService.createPostWithImages(
                    req.getUserId(),
                    req.getTitle(),
                    req.getContent(),
                    req.getImages(),
                    req.getCategoryId1(),
                    req.getCategoryId2(),
                    req.getTopics(),
                    req.getVideoUrl(),
                    req.getVideoCoverUrl(),
                    req.getVideoDurationSeconds(),
                    req.getVisibility()
            );
            boolean hasVideo = req.getVideoUrl() != null && !req.getVideoUrl().isBlank();
            boolean hasCover = req.getVideoCoverUrl() != null && !req.getVideoCoverUrl().isBlank();
            if (hasVideo && !hasCover && created != null && created.getId() != null) {
                videoProcessingService.enqueueExtractFirstFrameCover(created.getId(), req.getVideoUrl());
            }
            return created;
        } catch (RuntimeException e) {
            if ("POST_FORBIDDEN".equals(e.getMessage())) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", "POST_FORBIDDEN");
                result.put("message", "帖子内容包含敏感词，已被拦截");
                return result;
            }
            if ("POST_REVIEW_REQUIRED".equals(e.getMessage())) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", "POST_REVIEW_REQUIRED");
                result.put("message", "帖子内容疑似风险，已进入人工审核");
                return result;
            }
            String msg = e.getMessage();
            if (msg != null && msg.startsWith("USER_BANNED:")) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("code", "USER_BANNED");
                result.put("message", msg.substring("USER_BANNED:".length()));
                return result;
            }
            throw e;
        }
    }

    /**
     * 管理端：审核通过帖子
     * POST /post/admin/approve?postId=1
     */
    @PostMapping("/admin/approve")
    @ResponseBody
    public Map<String, Object> approve(@RequestParam("postId") Long postId) {
        postService.approvePost(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 管理端：审核拒绝帖子
     * POST /post/admin/reject?postId=1
     */
    @PostMapping("/admin/reject")
    @ResponseBody
    public Map<String, Object> reject(@RequestParam("postId") Long postId) {
        postService.rejectPost(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 管理端：删除帖子（软删除，不校验作者）
     * POST /post/admin/delete?postId=1
     */
    @PostMapping("/admin/delete")
    @ResponseBody
    public Map<String, Object> adminDelete(@RequestParam("postId") Long postId) {
        postService.adminDeletePost(postId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 管理端：批量软删除
     * POST /post/admin/deleteBatch  Body: { "postIds": [1,2,3] }
     */
    @PostMapping("/admin/deleteBatch")
    @ResponseBody
    public Map<String, Object> adminDeleteBatch(@RequestBody AdminBatchPostIdsRequest req) {
        Map<String, Object> result = new HashMap<>();
        if (req == null || req.getPostIds() == null || req.getPostIds().isEmpty()) {
            result.put("success", false);
            result.put("message", "postIds 不能为空");
            return result;
        }
        postService.adminDeletePosts(req.getPostIds());
        result.put("success", true);
        return result;
    }

    /**
     * 帖子点赞
     * POST /post/like?postId=1&userId=1
     */
    @PostMapping("/like")
    @ResponseBody
    public Map<String, Object> like(@RequestParam("postId") Long postId,
                                    @RequestParam("userId") Long userId) {
        postService.likePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 取消帖子点赞
     * POST /post/unlike?postId=1&userId=1
     */
    @PostMapping("/unlike")
    @ResponseBody
    public Map<String, Object> unlike(@RequestParam("postId") Long postId,
                                      @RequestParam("userId") Long userId) {
        postService.unlikePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 帖子收藏
     * POST /post/favorite?postId=1&userId=1 可选 folderId（不传则默认收藏夹）
     */
    @PostMapping("/favorite")
    @ResponseBody
    public Map<String, Object> favorite(@RequestParam("postId") Long postId,
                                        @RequestParam("userId") Long userId,
                                        @RequestParam(value = "folderId", required = false) Long folderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            postService.favoritePost(postId, userId, folderId);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 将已收藏帖子移到其他收藏夹
     * POST /post/favorite/move?postId=1&userId=1&folderId=2
     */
    @PostMapping("/favorite/move")
    @ResponseBody
    public Map<String, Object> moveFavorite(@RequestParam("postId") Long postId,
                                            @RequestParam("userId") Long userId,
                                            @RequestParam("folderId") Long folderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            postService.moveFavoritePost(postId, userId, folderId);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 取消帖子收藏
     * POST /post/unfavorite?postId=1&userId=1
     */
    @PostMapping("/unfavorite")
    @ResponseBody
    public Map<String, Object> unfavorite(@RequestParam("postId") Long postId,
                                          @RequestParam("userId") Long userId) {
        postService.unfavoritePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 查询帖子点赞/收藏状态
     * GET /post/like/status?postId=1&userId=1
     * GET /post/favorite/status?postId=1&userId=1
     */
    @GetMapping("/like/status")
    @ResponseBody
    public Map<String, Object> likeStatus(@RequestParam("postId") Long postId,
                                          @RequestParam("userId") Long userId) {
        boolean liked = postService.isPostLiked(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return result;
    }

    @GetMapping("/favorite/status")
    @ResponseBody
    public Map<String, Object> favoriteStatus(@RequestParam("postId") Long postId,
                                              @RequestParam("userId") Long userId) {
        boolean favorited = postService.isPostFavorited(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        return result;
    }

    /**
     * 删除帖子（仅作者可操作，软删除）
     * POST /post/delete?postId=1&userId=1
     */
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("postId") Long postId,
                                      @RequestParam("userId") Long userId) {
        postService.deletePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    @PostMapping("/uploadImage")
    @ResponseBody
    public String uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "";
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + ext;
        if (aliyunOssClientFacade != null) {
            return aliyunOssClientFacade.uploadMultipartToKey(file, "post_img/" + fileName);
        }
        Path uploadDir = resolveUploadDir("post_img");
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(fileName);
        file.transferTo(target);
        return "/post_img/" + fileName;
    }

    /**
     * 上传视频（发帖用）
     * POST /post/uploadVideo，form-data: file
     * 返回 JSON：{ "videoUrl": "/post_video/xxx.mp4", "coverUrl": "/post_img/yyy.jpg" }；
     * coverUrl 在服务器已安装 ffmpeg 且截帧成功时存在（客户端可不自己截封面）。
     */
    @PostMapping("/uploadVideo")
    @ResponseBody
    public Map<String, Object> uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        Map<String, Object> out = new HashMap<>();
        if (file.isEmpty()) {
            out.put("videoUrl", "");
            return out;
        }

        long ratedBytes = Math.max(1L, videoRatedSizeMb) * 1024L * 1024L;
        long size = file.getSize();
        boolean shouldTranscode = size > 0 && size > ratedBytes;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String fallbackFileName = UUID.randomUUID() + (ext != null && !ext.isBlank() ? ext : ".mp4");

        if (aliyunOssClientFacade != null) {
            Path tempVideo = Files.createTempFile("post-video-upload-", ext != null && !ext.isBlank() ? ext : ".mp4");
            try {
                file.transferTo(tempVideo);
                // Zeabur 网关对长连接/长请求较敏感：同步上传 OSS 可能导致前端直接断连（ERR_CONNECTION_CLOSED）。
                // 这里先落盘到本地并快速返回，让发帖流程继续；封面/转码仍走异步逻辑。
                Path uploadDir = resolveUploadDir("post_video");
                Files.createDirectories(uploadDir);
                Path fallbackTarget = uploadDir.resolve(fallbackFileName);
                Files.move(tempVideo, fallbackTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                String videoUrl = "/post_video/" + fallbackFileName;
                out.put("videoUrl", videoUrl);

                if (shouldTranscode) {
                    videoProcessingService.enqueueTranscode(videoUrl);
                }
            } finally {
                try {
                    Files.deleteIfExists(tempVideo);
                } catch (IOException ignore) {
                }
            }
            return out;
        }

        Path uploadDir = resolveUploadDir("post_video");
        Files.createDirectories(uploadDir);
        Path fallbackTarget = uploadDir.resolve(fallbackFileName);
        file.transferTo(fallbackTarget);
        String videoUrl = "/post_video/" + fallbackFileName;
        out.put("videoUrl", videoUrl);

        if (isFfmpegAvailable()) {
            String coverRel = videoProcessingService.extractCoverToPostImgDir(fallbackTarget);
            if (coverRel != null && !coverRel.isBlank()) {
                out.put("coverUrl", coverRel);
            }
            if (shouldTranscode) {
                videoProcessingService.enqueueTranscode(videoUrl);
            }
        }
        return out;
    }
}

