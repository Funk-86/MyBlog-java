package org.example.myblog.controller;

import org.example.myblog.dto.CreatePostRequest;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.serverl.PostHotService;
import org.example.myblog.serverl.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
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

    @Autowired
    private PostMapper postMapper;

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

    @Value("${video.upload.ffmpeg-bin:ffmpeg}")
    private String ffmpegBin;

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
     * 管理端：待审核帖子列表（status=1）
     * GET /post/admin/pending?page=1&size=20
     */
    @GetMapping("/admin/pending")
    @ResponseBody
    public List<Map<String, Object>> adminPendingPosts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        return postMapper.listPendingPosts(offset, size);
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
    public Post detail(@RequestParam("id") Long id) {
        Post post = postService.getPostDetail(id);
        if (post != null && postHotService != null) {
            postHotService.incrementView(id);
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
                               @RequestParam(value = "categoryId", required = false) Long categoryId) {
        return postService.listHotPostsByCategory(categoryId, page, size);
    }

    /**
     * 管理端：帖子列表（带总数，用于表格分页）
     * GET /post/admin/list?page=1&size=10[&categoryId=2]
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public Map<String, Object> adminPostList(@RequestParam(value = "page", defaultValue = "1") int page,
                                             @RequestParam(value = "size", defaultValue = "10") int size,
                                             @RequestParam(value = "categoryId", required = false) Long categoryId) {
        int offset = (page - 1) * size;
        List<Post> list;
        long total;
        if (categoryId != null) {
            // 分区 + 热度排序，统计同一筛选条件下的总数
            list = postMapper.listByHotScoreWithCategory(categoryId, offset, size);
            total = postMapper.countByCategory(categoryId);
        } else {
            // 全站热门列表，统计可见且正常的总数
            list = postMapper.listByHotScore(offset, size);
            total = postMapper.countVisible();
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
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
                             @RequestParam(value = "size", defaultValue = "20") int size) {
        return postService.searchPosts(keyword, page, size);
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
                                @RequestParam(value = "size", defaultValue = "20") int size) {
        return postService.listByUserId(userId, page, size);
    }

    /**
     * 用户收藏的帖子列表 GET /post/favorites?userId=1&page=1&size=20
     */
    @GetMapping("/favorites")
    @ResponseBody
    public List<Post> favoritePosts(@RequestParam("userId") Long userId,
                                    @RequestParam(value = "page", defaultValue = "1") int page,
                                    @RequestParam(value = "size", defaultValue = "20") int size) {
        return postService.listFavoritePosts(userId, page, size);
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
                                    @RequestParam(value = "size", defaultValue = "10") int size) {
        if (size <= 0) size = 10;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByHotScoreWithCategory(categoryId, offset, size);
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
            return postService.createPostWithImages(
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
     * POST /post/favorite?postId=1&userId=1
     */
    @PostMapping("/favorite")
    @ResponseBody
    public Map<String, Object> favorite(@RequestParam("postId") Long postId,
                                        @RequestParam("userId") Long userId) {
        postService.favoritePost(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
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
        Path uploadDir = Paths.get("post_img").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(fileName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/post_img/" + fileName;
    }

    /**
     * 上传视频（发帖用）
     * POST /post/uploadVideo，form-data: file
     * 返回：/post_video/xxx.mp4
     */
    @PostMapping("/uploadVideo")
    @ResponseBody
    public String uploadVideo(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return "";
        }
        Path uploadDir = Paths.get("post_video").toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        long ratedBytes = Math.max(1L, videoRatedSizeMb) * 1024L * 1024L;
        long size = file.getSize(); // 可能返回 -1（未知），未知则不转码
        boolean shouldTranscode = size > 0 && size > ratedBytes;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf("."));
        }

        // fallback：永远能返回一个可用视频文件
        String fallbackFileName = UUID.randomUUID() + (ext != null && !ext.isBlank() ? ext : ".mp4");
        Path fallbackTarget = uploadDir.resolve(fallbackFileName);

        // 未超过额定大小：直接保存原视频（不做转码）
        if (!shouldTranscode) {
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/post_video/" + fallbackFileName;
        }

        // 超过额定大小：先检查 ffmpeg 是否可用
        if (!isFfmpegAvailable()) {
            System.err.println("[uploadVideo] ffmpeg not available, save original only: " + fallbackFileName);
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/post_video/" + fallbackFileName;
        }

        // 超过额定大小且 ffmpeg 可用：只落盘一次到临时文件，再转码/回退
        Path tmpInput = null;
        Path transcodedTarget = null;
        try {
            tmpInput = Files.createTempFile("video-upload-", ext != null && ext.startsWith(".") ? ext : ".mp4");
            file.transferTo(tmpInput);

            transcodedTarget = uploadDir.resolve(UUID.randomUUID() + ".mp4");

            String vf = "scale='if(gt(iw,ih)," + videoTranscodeMaxSide + ",-2)':'if(gt(iw,ih),-2," + videoTranscodeMaxSide + ")':force_original_aspect_ratio=decrease";
            String crf = String.valueOf(videoTranscodeCrf);
            String ab = String.valueOf(videoTranscodeAudioBitrateK) + "k";

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegBin,
                    "-y",
                    "-i",
                    tmpInput.toAbsolutePath().toString(),
                    "-vf",
                    vf,
                    "-c:v",
                    "libx264",
                    "-crf",
                    crf,
                    "-preset",
                    "veryfast",
                    "-c:a",
                    "aac",
                    "-b:a",
                    ab,
                    "-movflags",
                    "+faststart",
                    transcodedTarget.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            Process p = pb.start();
            boolean finished = p.waitFor(5, TimeUnit.MINUTES);
            int exitCode = finished ? p.exitValue() : -1;
            if (exitCode != 0) {
                // 转码失败：回退保存临时文件内容到 fallbackTarget
                Files.copy(tmpInput, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
                return "/post_video/" + fallbackFileName;
            }

            if (!Files.exists(transcodedTarget) || Files.size(transcodedTarget) <= 0) {
                Files.copy(tmpInput, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
                return "/post_video/" + fallbackFileName;
            }

            return "/post_video/" + transcodedTarget.getFileName().toString();
        } catch (Throwable t) {
            // 转码报错：回退
            try {
                if (tmpInput != null && Files.exists(tmpInput)) {
                    Files.copy(tmpInput, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    try (InputStream is = file.getInputStream()) {
                        Files.copy(is, fallbackTarget, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception ignore) {}
            return "/post_video/" + fallbackFileName;
        } finally {
            try {
                if (tmpInput != null && Files.exists(tmpInput)) Files.deleteIfExists(tmpInput);
            } catch (Exception ignore) {}
        }
    }
}

