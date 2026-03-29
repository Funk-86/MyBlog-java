package org.example.myblog.controller;

import org.example.myblog.dto.FavoriteFolderDTO;
import org.example.myblog.serverl.FavoriteFolderService;
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
 * 收藏夹：列表、新建、删除
 */
@Controller
@RequestMapping("/favorite-folder")
public class FavoriteFolderController {

    @Autowired
    private FavoriteFolderService favoriteFolderService;

    /**
     * GET /favorite-folder/list?userId=1
     */
    @GetMapping("/list")
    @ResponseBody
    public List<FavoriteFolderDTO> list(@RequestParam("userId") Long userId) {
        return favoriteFolderService.listFolders(userId);
    }

    /**
     * POST /favorite-folder/create?userId=1&name=技术
     */
    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> create(@RequestParam("userId") Long userId,
                                        @RequestParam("name") String name) {
        Map<String, Object> result = new HashMap<>();
        try {
            long id = favoriteFolderService.createFolder(userId, name);
            result.put("success", true);
            result.put("id", id);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * POST /favorite-folder/delete?userId=1&folderId=2
     */
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestParam("userId") Long userId,
                                       @RequestParam("folderId") Long folderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            favoriteFolderService.deleteFolder(userId, folderId);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
