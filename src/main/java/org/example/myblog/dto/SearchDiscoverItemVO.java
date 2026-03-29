package org.example.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 搜索发现单项：热门话题或热门帖子标题
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchDiscoverItemVO {
    /** topic | post */
    private String type;
    private String text;
    private Long topicId;
    private Long postId;
}
