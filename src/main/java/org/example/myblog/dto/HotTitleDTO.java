package org.example.myblog.dto;

import lombok.Data;

/** 用于搜索发现：高热帖子标题与热度 */
@Data
public class HotTitleDTO {
    private String title;
    private Double hotScore;
}
