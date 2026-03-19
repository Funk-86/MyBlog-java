package org.example.myblog.controller;

import org.example.myblog.entiy.Post;
import org.example.myblog.entiy.Topic;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.TopicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/topic")
public class TopicController {

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private PostMapper postMapper;

    /**
     * 热门话题列表：按帖子数量倒序
     * GET /topic/hot?limit=20
     */
    @GetMapping("/hot")
    @ResponseBody
    public List<Topic> hot(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        if (limit <= 0) limit = 20;
        if (limit > 50) limit = 50;
        return topicMapper.listHotTopics(limit);
    }

    /**
     * 某个话题下的帖子列表
     * GET /topic/posts?name=steam游戏&page=1&size=10
     */
    @GetMapping("/posts")
    @ResponseBody
    public List<Post> posts(@RequestParam("name") String name,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size) {
        if (size <= 0) size = 10;
        if (size > 50) size = 50;
        int offset = (page <= 0 ? 0 : page - 1) * size;
        return postMapper.listByTopic(name, offset, size);
    }

    /**
     * 管理端：话题列表（简单全部返回，前端本地分页）
     * GET /topic/admin/list
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public List<Topic> listForAdmin() {
        return topicMapper.listAllForAdmin();
    }

    /**
     * 管理端：新增话题
     * POST /topic/add  Body: { "name": "xxx", "slug": "xxx" }
     */
    @PostMapping("/add")
    @ResponseBody
    public Topic add(@RequestBody Map<String, Object> body) {
        String name = body != null && body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("话题名称不能为空");
        }
        String slug = body != null && body.get("slug") != null ? body.get("slug").toString().trim() : null;
        Topic topic = new Topic();
        topic.setName(name);
        topic.setSlug(slug);
        topicMapper.insert(topic);
        return topic;
    }

    /**
     * 管理端：更新话题
     * PUT /topic/update  Body: { "id": 1, "name": "xxx", "slug": "xx" }
     */
    @PutMapping("/update")
    @ResponseBody
    public Topic update(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Long id = Long.parseLong(idObj.toString());
        String name = body != null && body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("话题名称不能为空");
        }
        String slug = body != null && body.get("slug") != null ? body.get("slug").toString().trim() : null;
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName(name);
        topic.setSlug(slug);
        topicMapper.update(topic);
        return topicMapper.selectByName(name);
    }

    /**
     * 管理端：删除话题
     * DELETE /topic/delete?id=1
     */
    @DeleteMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("id") Long id) {
        topicMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}

