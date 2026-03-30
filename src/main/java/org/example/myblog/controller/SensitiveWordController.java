package org.example.myblog.controller;

import org.example.myblog.entiy.SensitiveWord;
import org.example.myblog.mapper.SensitiveWordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
     * 管理端：更新违禁词
     * PUT /sensitive-word/update
     * Body: { "id": 1, "word": "xxx", "level": 2, "status": 0 }
     */
    @PutMapping("/update")
    @ResponseBody
    public SensitiveWord update(@RequestBody Map<String, Object> body) {
        if (body == null || body.get("id") == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        int id = Integer.parseInt(body.get("id").toString());
        SensitiveWord existing = sensitiveWordMapper.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("记录不存在");
        }
        String word = body.get("word") != null ? body.get("word").toString().trim() : existing.getWord();
        if (word == null || word.isEmpty()) {
            throw new IllegalArgumentException("违禁词内容不能为空");
        }
        if (word.length() > 100) {
            throw new IllegalArgumentException("违禁词最多 100 字");
        }
        if (sensitiveWordMapper.countByWordExcludingId(word, id) > 0) {
            throw new IllegalArgumentException("该违禁词已存在");
        }
        int level = existing.getLevel() != null ? existing.getLevel() : 2;
        if (body.get("level") != null) {
            level = Integer.parseInt(body.get("level").toString());
        }
        if (level < 1 || level > 3) {
            level = 2;
        }
        int status = existing.getStatus() != null ? existing.getStatus() : 0;
        if (body.get("status") != null) {
            status = Integer.parseInt(body.get("status").toString());
        }
        if (status != 0 && status != 1) {
            status = 0;
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setId(id);
        sw.setWord(word);
        sw.setLevel(level);
        sw.setStatus(status);
        sensitiveWordMapper.updateById(sw);
        return sensitiveWordMapper.findById(id);
    }

    /**
     * 管理端：从 txt 批量导入违禁词（UTF-8，每行一个或一行内逗号/分号分隔）
     * POST /sensitive-word/import-txt  multipart: file, level(可选), status(可选)
     * 返回：{ success, imported, duplicates, invalid }
     */
    @PostMapping("/import-txt")
    @ResponseBody
    public Map<String, Object> importFromTxt(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "level", defaultValue = "2") int level,
            @RequestParam(value = "status", defaultValue = "0") int status) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 txt 文件");
        }
        if (level < 1 || level > 3) {
            level = 2;
        }
        if (status != 0 && status != 1) {
            status = 0;
        }
        final int maxTokens = 10000;
        int imported = 0;
        int duplicates = 0;
        int invalid = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (imported + duplicates + invalid >= maxTokens) {
                    break;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    continue;
                }
                String[] parts = trimmed.split("[,，;；]");
                for (String part : parts) {
                    if (imported + duplicates + invalid >= maxTokens) {
                        break;
                    }
                    String w = part.trim();
                    if (w.isEmpty() || w.startsWith("#")) {
                        continue;
                    }
                    if (w.length() > 100) {
                        invalid++;
                        continue;
                    }
                    if (sensitiveWordMapper.countByWord(w) > 0) {
                        duplicates++;
                        continue;
                    }
                    SensitiveWord sw = new SensitiveWord();
                    sw.setWord(w);
                    sw.setLevel(level);
                    sw.setStatus(status);
                    sensitiveWordMapper.insert(sw);
                    imported++;
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("imported", imported);
        result.put("duplicates", duplicates);
        result.put("invalid", invalid);
        return result;
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

