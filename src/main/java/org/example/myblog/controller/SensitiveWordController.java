package org.example.myblog.controller;

import org.example.myblog.entiy.SensitiveWord;
import org.example.myblog.mapper.SensitiveWordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/sensitive-word")
public class SensitiveWordController {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    /**
     * 管理端：违禁词列表（分页）
     * GET /sensitive-word/admin/list?page=1&size=20
     * 返回：{ "list": [...], "total": 总数 }
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public Map<String, Object> listForAdmin(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 200) size = 200;
        int offset = (page - 1) * size;
        long total = sensitiveWordMapper.countAll();
        List<SensitiveWord> list = sensitiveWordMapper.listPaged(offset, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    /**
     * 管理端：新增违禁词
     * POST /sensitive-word/add
     * Body: { "word": "xxx", "level": 2, "status": 0 }
     */
    @PostMapping("/add")
    @ResponseBody
    public SensitiveWord add(@RequestBody Map<String, Object> body) {
        String word = body != null && body.get("word") != null ? body.get("word").toString().trim() : null;
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("违禁词内容不能为空");
        }
        Integer level = null;
        if (body != null && body.get("level") != null) {
            level = Integer.valueOf(body.get("level").toString());
        }
        if (level == null || level <= 0) {
            level = 2; // 默认拦截
        }
        Integer status = null;
        if (body != null && body.get("status") != null) {
            status = Integer.valueOf(body.get("status").toString());
        }
        if (status == null) {
            status = 0; // 默认启用
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        sw.setLevel(level);
        sw.setStatus(status);
        sensitiveWordMapper.insert(sw);
        return sw;
    }

    /**
     * 管理端：删除违禁词
     * DELETE /sensitive-word/delete?id=1
     */
    @DeleteMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("id") Integer id) {
        sensitiveWordMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}

