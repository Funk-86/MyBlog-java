package org.example.myblog.controller;

import org.example.myblog.entiy.Category;
import org.example.myblog.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分区/分类接口，发帖时从数据库读取分区列表
 */
@Controller
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 获取启用状态的分区列表（发帖时用）
     * GET /category/list
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Category> list() {
        return categoryMapper.listAll();
    }

    /**
     * 管理端：获取全部分区（含禁用）
     * GET /category/admin/list
     */
    @GetMapping("/admin/list")
    @ResponseBody
    public List<Category> listForAdmin() {
        return categoryMapper.listAllForAdmin();
    }

    /**
     * 管理端：新增分区
     * POST /category/add  Body: { "name": "xxx", "sortOrder": 0 }
     */
    @PostMapping("/add")
    @ResponseBody
    public Category add(@RequestBody Map<String, Object> body) {
        String name = body != null && body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("分区名称不能为空");
        }
        Category category = new Category();
        category.setName(name);
        category.setCode((String) (body != null ? body.get("code") : null));
        Object so = body != null ? body.get("sortOrder") : null;
        category.setSortOrder(so != null ? Integer.parseInt(so.toString()) : 0);
        Object st = body != null ? body.get("status") : null;
        category.setStatus(st != null ? Integer.parseInt(st.toString()) : 1);
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 管理端：更新分区
     * PUT /category/update  Body: { "id": 1, "name": "xxx", "code": "xx", "sortOrder": 0, "status": 1 }
     */
    @PutMapping("/update")
    @ResponseBody
    public Category update(@RequestBody Map<String, Object> body) {
        Object idObj = body != null ? body.get("id") : null;
        if (idObj == null) {
            throw new IllegalArgumentException("id 不能为空");
        }
        Long id = Long.parseLong(idObj.toString());
        String name = body != null && body.get("name") != null ? body.get("name").toString().trim() : null;
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("分区名称不能为空");
        }
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setCode((String) (body != null ? body.get("code") : null));
        Object so = body != null ? body.get("sortOrder") : null;
        category.setSortOrder(so != null ? Integer.parseInt(so.toString()) : 0);
        Object st = body != null ? body.get("status") : null;
        category.setStatus(st != null ? Integer.parseInt(st.toString()) : 1);
        categoryMapper.update(category);
        return categoryMapper.selectById(id);
    }

    /**
     * 管理端：删除分区
     * DELETE /category/delete?id=1
     */
    @DeleteMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("id") Long id) {
        categoryMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }
}
