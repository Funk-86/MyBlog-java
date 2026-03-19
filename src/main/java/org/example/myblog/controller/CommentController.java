package org.example.myblog.controller;

import org.example.myblog.entiy.Comment;
import org.example.myblog.serverl.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评论相关接口
 */
@Controller
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 发表评论（支持回复：传 parentId、rootId 即为对某条评论的回复）
     * POST /comment/create?postId=1&userId=1&content=xxx&parentId=&rootId=
     */
    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestParam("postId") Long postId,
                                      @RequestParam("userId") Long userId,
                                      @RequestParam("content") String content,
                                      @RequestParam(value = "parentId", required = false) Long parentId,
                                      @RequestParam(value = "rootId", required = false) Long rootId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Comment comment = commentService.createComment(postId, userId, content, parentId, rootId);
            result.put("success", true);
            result.put("data", comment);
        } catch (RuntimeException e) {
            if ("COMMENT_FORBIDDEN".equals(e.getMessage())) {
                result.put("success", false);
                result.put("code", "COMMENT_FORBIDDEN");
                result.put("message", "评论内容包含敏感词，已被拦截");
            } else {
                throw e;
            }
        }
        return result;
    }

    /**
     * 查询帖子评论列表（分页）
     * GET /comment/list?postId=1&page=1&size=10
     * 支持 offset：若传 offset 则忽略 page，按 offset+size 分页
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Comment> list(@RequestParam("postId") Long postId,
                              @RequestParam(value = "page", defaultValue = "1") int page,
                              @RequestParam(value = "size", defaultValue = "10") int size,
                              @RequestParam(value = "offset", required = false) Integer offset) {
        return commentService.listComments(postId, page, size, offset);
    }

    /**
     * 评论点赞
     * POST /comment/like?commentId=1&userId=1
     */
    @PostMapping("/like")
    @ResponseBody
    public Map<String, Object> like(@RequestParam("commentId") Long commentId,
                                    @RequestParam("userId") Long userId) {
        commentService.likeComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 取消评论点赞
     * POST /comment/unlike?commentId=1&userId=1
     */
    @PostMapping("/unlike")
    @ResponseBody
    public Map<String, Object> unlike(@RequestParam("commentId") Long commentId,
                                      @RequestParam("userId") Long userId) {
        commentService.unlikeComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 查询当前用户是否已赞该评论
     * GET /comment/like/status?commentId=1&userId=1
     */
    @GetMapping("/like/status")
    @ResponseBody
    public Map<String, Object> likeStatus(@RequestParam("commentId") Long commentId,
                                          @RequestParam("userId") Long userId) {
        boolean liked = commentService.isCommentLiked(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        return result;
    }

    /**
     * 获取用户在该帖子下已赞的评论 id 列表
     * GET /comment/liked?postId=1&userId=1
     */
    @GetMapping("/liked")
    @ResponseBody
    public Map<String, Object> likedIds(@RequestParam("postId") Long postId,
                                        @RequestParam("userId") Long userId) {
        List<Long> ids = commentService.getLikedCommentIds(postId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("commentIds", ids);
        return result;
    }

    /**
     * 删除评论（仅帖子作者可操作）
     * POST /comment/delete?commentId=1&userId=1
     */
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("commentId") Long commentId,
                                      @RequestParam("userId") Long userId) {
        commentService.deleteComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * 置顶评论（仅帖子作者可操作）
     * POST /comment/pin?commentId=1&userId=1
     */
    @PostMapping("/pin")
    @ResponseBody
    public Map<String, Object> pin(@RequestParam("commentId") Long commentId,
                                   @RequestParam("userId") Long userId) {
        commentService.pinComment(commentId, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}

