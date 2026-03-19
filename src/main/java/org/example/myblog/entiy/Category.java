package org.example.myblog.entiy;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分区/分类表 category，发帖时选择分区
 */
@Data
public class Category {

    private Long id;
    private String name;
    private String code;
    /** 排序，小的靠前 */
    private Integer sortOrder;
    /** 0禁用 1启用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
