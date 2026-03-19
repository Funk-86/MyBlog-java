package org.example.myblog.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreatePostRequest {
    private Long userId;
    private String title;
    private String content;
    private List<String> images; // 图片 URL 列表
    private Long categoryId1; // 主分区，最多选 2 个
    private Long categoryId2;
    /** 话题名称列表（不出现在正文里，由前端显式传入） */
    private List<String> topics;
    private String videoUrl;       // 视频地址（与 images 二选一，有则 type=2）
    private String videoCoverUrl;  // 视频封面图地址（前端截帧上传后填入）
    private Integer videoDurationSeconds; // 视频时长（秒），用于列表展示
    /** 0=所有人看 1=仅个人查看 */
    private Integer visibility;
}