package org.example.myblog.serverl;

import org.example.myblog.dto.SearchDiscoverPairVO;

import java.util.List;

public interface SearchDiscoverService {

    /**
     * 按热度聚合话题 + 帖子标题，打乱后两两成对，供搜索发现展示
     *
     * @param pairCount 行数（每行左、右各一项）
     */
    List<SearchDiscoverPairVO> discover(int pairCount);
}
