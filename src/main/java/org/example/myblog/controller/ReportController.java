package org.example.myblog.controller;

import org.example.myblog.dto.SendMessageRequest;
import org.example.myblog.entiy.Comment;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.CommentMapper;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.ReportMapper;
import org.example.myblog.serverl.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 举报接口（管理端 + 客户端）
 */
@Controller
@RequestMapping("/report")
public class ReportController {

    /** 管理员用户 ID（用于接收举报审核提醒 / 作为系统通知账号） */
    private static final long ADMIN_USER_ID = 6L;

    @Autowired
    private ReportMapper reportMapper;

    @Autowired
    private PostMapper postMapper;

    @Autowired(required = false)
    private CommentMapper commentMapper;

    @Autowired(required = false)
    private ChatService chatService;

    /**
     * 管理端：举报列表
     * GET /report/admin/list?page=1&size=20
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public List<Map<String, Object>> listForAdmin(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        int offset = (page - 1) * size;
        return reportMapper.listForAdmin(offset, size);
    }

    /**
     * 客户端：创建举报
     * POST /report/create
     * Body: { reporterId, targetType, targetId, reasonCode, description }
     */
    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        Object targetTypeObj = body.get("targetType");
        Object targetIdObj = body.get("targetId");
        Object reasonObj = body.get("reasonCode");
        if (targetTypeObj == null || targetIdObj == null || reasonObj == null) {
            throw new IllegalArgumentException("targetType、targetId、reasonCode 不能为空");
        }
        int targetType = ((Number) targetTypeObj).intValue();
        long targetId = ((Number) targetIdObj).longValue();
        String reasonStr = reasonObj.toString();
        String description = body.get("description") != null ? body.get("description").toString() : null;

        Long reporterId = null;
        Object reporterObj = body.get("reporterId");
        if (reporterObj instanceof Number) {
            reporterId = ((Number) reporterObj).longValue();
        }

        int reasonCode = mapReasonCode(reasonStr);
        reportMapper.insertReport(reporterId, targetType, targetId, reasonCode, description);

        // 给举报人发送系统通知：已收到举报
        sendUserReportCreatedNotify(reporterId, targetType, targetId);
        // 给管理员发送提示：有新的举报需要处理
        sendAdminReportPendingNotify(reporterId, targetType, targetId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    /**
     * 管理端：举报处理通过（举报成立）
     * POST /report/admin/approve?id=1&handlerId=100
     */
    @PostMapping("/admin/approve")
    @ResponseBody
    public Map<String, Object> approve(@RequestParam("id") Long id,
                                       @RequestParam(value = "handlerId", required = false) Long handlerId) {
        handleReport(id, handlerId, true);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    /**
     * 管理端：举报处理不成立
     * POST /report/admin/reject?id=1&handlerId=100
     */
    @PostMapping("/admin/reject")
    @ResponseBody
    public Map<String, Object> reject(@RequestParam("id") Long id,
                                      @RequestParam(value = "handlerId", required = false) Long handlerId) {
        handleReport(id, handlerId, false);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        return res;
    }

    /**
     * 管理端：下架（删除帖子 + 通知举报人 + 通知被举报人）
     * POST /report/admin/takeDown?id=1&handlerId=100
     * 对帖子/评论类型举报生效
     */
    @PostMapping("/admin/takeDown")
    @ResponseBody
    public Map<String, Object> takeDown(@RequestParam("id") Long id,
                                        @RequestParam(value = "handlerId", required = false) Long handlerId) {
        if (id == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "参数错误");
            return err;
        }
        Map<String, Object> r = reportMapper.selectById(id);
        if (r == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "举报记录不存在");
            return err;
        }
        Integer targetType = r.get("target_type") instanceof Number
                ? ((Number) r.get("target_type")).intValue()
                : null;
        Long targetId = r.get("target_id") instanceof Number
                ? ((Number) r.get("target_id")).longValue()
                : null;
        Long reporterId = r.get("reporter_id") instanceof Number
                ? ((Number) r.get("reporter_id")).longValue()
                : null;
        if (targetType == null || targetId == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "举报记录参数错误");
            return err;
        }
        // 2=帖子 3=评论
        if (targetType == 2) {
            Post post = postMapper.selectById(targetId);
            if (post == null) {
                reportMapper.updateStatus(id, 1, handlerId, "已下架（原帖已不存在）");
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                return res;
            }
            Long postAuthorId = post.getUserId();
            postMapper.updateStatus(targetId, 2);
            reportMapper.updateStatus(id, 1, handlerId, "已下架");
            if (chatService != null) {
                if (reporterId != null) {
                    try {
                        SendMessageRequest req = new SendMessageRequest();
                        req.setFromUserId(ADMIN_USER_ID);
                        req.setToUserId(reporterId);
                        req.setContent("举报成功");
                        req.setContentType(0);
                        chatService.sendMessage(req);
                    } catch (Exception ignored) {}
                }
                if (postAuthorId != null && !postAuthorId.equals(reporterId)) {
                    try {
                        SendMessageRequest req = new SendMessageRequest();
                        req.setFromUserId(ADMIN_USER_ID);
                        req.setToUserId(postAuthorId);
                        req.setContent("你的帖子被人举报，经审核后，违反社区公告，现已下架你的帖子");
                        req.setContentType(0);
                        chatService.sendMessage(req);
                    } catch (Exception ignored) {}
                }
            }
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            return res;
        }

        if (targetType == 3) {
            if (commentMapper == null) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "CommentMapper 未注入");
                return err;
            }
            Comment c = commentMapper.selectById(targetId);
            if (c == null) {
                // 评论已经不存在，标记所有同一评论的举报为已处理
                reportMapper.updateStatusByTarget(3, targetId, 1, handlerId, "已删除（原评论已不存在）");
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                return res;
            }
            // 软删除评论：1=删除
            commentMapper.updateStatus(targetId, 1);
            // 评论数 -1
            if (c.getPostId() != null) {
                postMapper.decrementCommentCount(c.getPostId());
            }
            // 当前这条 + 所有同一评论且仍为待处理的举报，一并标记为已处理
            reportMapper.updateStatusByTarget(3, targetId, 1, handlerId, "已删除评论");
            if (chatService != null) {
                if (reporterId != null) {
                    try {
                        SendMessageRequest req = new SendMessageRequest();
                        req.setFromUserId(ADMIN_USER_ID);
                        req.setToUserId(reporterId);
                        req.setContent("举报成功");
                        req.setContentType(0);
                        chatService.sendMessage(req);
                    } catch (Exception ignored) {}
                }
                Long commentAuthorId = c.getUserId();
                if (commentAuthorId != null && !commentAuthorId.equals(reporterId)) {
                    try {
                        SendMessageRequest req = new SendMessageRequest();
                        req.setFromUserId(ADMIN_USER_ID);
                        req.setToUserId(commentAuthorId);
                        req.setContent("你的评论被人举报，经审核后，违反社区公告，现已删除该评论");
                        req.setContentType(0);
                        chatService.sendMessage(req);
                    } catch (Exception ignored) {}
                }
            }
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            return res;
        }

        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("message", "仅支持下架帖子/评论类型的举报");
        return err;
    }

    /**
     * 文本原因 -> 整数编码，便于存到 int 列
     */
    private int mapReasonCode(String reason) {
        if (reason == null) return 0;
        return switch (reason) {
            case "发布社区无关内容" -> 1;
            case "发布违法违规内容" -> 2;
            case "发布色情低俗内容" -> 3;
            case "骚扰、引战、人身攻击" -> 4;
            case "侵权/隐私泄露" -> 5;
            case "诈骗或虚假信息" -> 6;
            case "未成年人不当内容" -> 7;
            case "其他问题" -> 8;
            default -> 0;
        };
    }

    private void handleReport(Long id, Long handlerId, boolean success) {
        if (id == null) return;
        Map<String, Object> r = reportMapper.selectById(id);
        if (r == null) return;
        Long reporterId = r.get("reporter_id") instanceof Number
                ? ((Number) r.get("reporter_id")).longValue()
                : null;
        Integer targetType = r.get("target_type") instanceof Number
                ? ((Number) r.get("target_type")).intValue()
                : null;
        Long targetId = r.get("target_id") instanceof Number
                ? ((Number) r.get("target_id")).longValue()
                : null;
        String resultText = success
                ? "您的举报已受理成功，我们已根据平台规则对相关内容进行了处理。"
                : "本次举报未达到处理标准，我们会加强对该用户/内容的关注，感谢你的反馈。";
        reportMapper.updateStatus(id, success ? 1 : 2, handlerId, resultText);
        // 给举报人发送处理结果
        sendUserReportResultNotify(reporterId, targetType, targetId, success);
    }

    private void sendUserReportCreatedNotify(Long reporterId, int targetType, long targetId) {
        if (chatService == null || reporterId == null) return;
        String typeStr = switch (targetType) {
            case 1 -> "用户";
            case 2 -> "帖子";
            case 3 -> "评论";
            default -> "内容";
        };
        String content = "您已成功举报" + typeStr + "（ID=" + targetId + "），我们已收到举报并会尽快处理。";
        try {
            SendMessageRequest req = new SendMessageRequest();
            req.setFromUserId(ADMIN_USER_ID);
            req.setToUserId(reporterId);
            req.setContent(content);
            req.setContentType(0);
            chatService.sendMessage(req);
        } catch (Exception ignored) {
        }
    }

    private void sendAdminReportPendingNotify(Long reporterId, int targetType, long targetId) {
        if (chatService == null) return;
        String typeStr = switch (targetType) {
            case 1 -> "用户";
            case 2 -> "帖子";
            case 3 -> "评论";
            default -> "内容";
        };
        String content = "有新的举报待处理：举报" + typeStr + "（ID=" + targetId + "）。";
        try {
            SendMessageRequest req = new SendMessageRequest();
            req.setFromUserId(reporterId != null ? reporterId : ADMIN_USER_ID);
            req.setToUserId(ADMIN_USER_ID);
            req.setContent(content);
            req.setContentType(0);
            chatService.sendMessage(req);
        } catch (Exception ignored) {
        }
    }

    private void sendUserReportResultNotify(Long reporterId, Integer targetType, Long targetId, boolean success) {
        if (chatService == null || reporterId == null) return;
        String typeStr = switch (targetType != null ? targetType : 0) {
            case 1 -> "用户";
            case 2 -> "帖子";
            case 3 -> "评论";
            default -> "内容";
        };
        String content;
        if (success) {
            content = "您对" + typeStr + "（ID=" + targetId + "）的举报已受理成功，我们已对相关内容进行处理。";
        } else {
            content = "您对" + typeStr + "（ID=" + targetId + "）的举报未被判定为违规，我们会加强对该用户/内容的关注，感谢你的反馈。";
        }
        try {
            SendMessageRequest req = new SendMessageRequest();
            req.setFromUserId(ADMIN_USER_ID);
            req.setToUserId(reporterId);
            req.setContent(content);
            req.setContentType(0);
            chatService.sendMessage(req);
        } catch (Exception ignored) {
        }
    }
}
