package org.example.myblog.controller;

import org.example.myblog.mapper.CommentMapper;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.ReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计接口（管理端）
 */
@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ReportMapper reportMapper;

    /**
     * 获取仪表盘统计数据
     * GET /dashboard/stats
     */
    @GetMapping("/stats")
    @ResponseBody
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("postCount", postMapper.countAll());
        result.put("commentCount", commentMapper.countAll());
        result.put("totalViewCount", postMapper.sumViewCount());
        result.put("todayViewCount", postMapper.sumViewCount()); // 今日访问量：暂无日度统计，先用总浏览量
        result.put("todayNewCount", postMapper.countTodayNew());
        result.put("pendingPostCount", postMapper.countPending()); // 待审核帖子（status=1 审核中）
        result.put("yesterdayPostCount", postMapper.countYesterdayNew());
        result.put("yesterdayCommentCount", commentMapper.countYesterdayNew());
        return result;
    }

    /**
     * 管理端顶部通知：汇总待审核帖子、待处理举报等信息
     * GET /dashboard/notifications
     */
    @GetMapping("/notifications")
    @ResponseBody
    public List<Map<String, Object>> notifications() {
        List<Map<String, Object>> list = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        long pendingPosts = postMapper.countPending();
        if (pendingPosts > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", "pending-posts");
            item.put("content", "有 " + pendingPosts + " 篇帖子正在等待审核。");
            item.put("type", "帖子审核");
            item.put("status", true);
            item.put("createdAt", LocalDateTime.now().format(fmt));
            list.add(item);
        }
        long pendingReports = reportMapper.countPending();
        if (pendingReports > 0) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", "pending-reports");
            item.put("content", "有 " + pendingReports + " 条举报正在等待受理。");
            item.put("type", "举报受理");
            item.put("status", true);
            item.put("createdAt", LocalDateTime.now().format(fmt));
            list.add(item);
        }
        return list;
    }

    /**
     * 近 N 天每日文章发布趋势
     * GET /dashboard/postTrend?days=7
     */
    @GetMapping("/postTrend")
    @ResponseBody
    public List<Map<String, Object>> postTrend(@RequestParam(value = "days", defaultValue = "7") int days) {
        return postMapper.listPostCountByDay(days);
    }

    /**
     * 文章分类占比（饼图）
     * GET /dashboard/categoryStats
     */
    @GetMapping("/categoryStats")
    @ResponseBody
    public List<Map<String, Object>> categoryStats() {
        return postMapper.listCategoryPostCount();
    }

    /**
     * 热门文章 Top N（按评论数）
     * GET /dashboard/topPosts?limit=5
     */
    @GetMapping("/topPosts")
    @ResponseBody
    public List<Map<String, Object>> topPosts(@RequestParam(value = "limit", defaultValue = "5") int limit) {
        return postMapper.listTopByCommentCount(limit);
    }

    /**
     * 近 N 天每日评论数
     * GET /dashboard/commentTrend?days=7
     */
    @GetMapping("/commentTrend")
    @ResponseBody
    public List<Map<String, Object>> commentTrend(@RequestParam(value = "days", defaultValue = "7") int days) {
        return commentMapper.listCountByDay(days);
    }
}
