package org.example.myblog.controller;

import org.example.myblog.dto.SearchDiscoverPairVO;
import org.example.myblog.serverl.SearchDiscoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 搜索相关：发现页聚合等
 */
@Controller
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private SearchDiscoverService searchDiscoverService;

    /**
     * 搜索发现：热门话题 + 按 hot_score 排序的公开帖子标题，随机配对展示
     * GET /search/discover?pairs=8
     */
    @GetMapping("/discover")
    @ResponseBody
    public List<SearchDiscoverPairVO> discover(@RequestParam(value = "pairs", defaultValue = "8") int pairs) {
        return searchDiscoverService.discover(pairs);
    }
}
