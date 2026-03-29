package org.example.myblog.dto;

import lombok.Data;

@Data
public class FavoriteFolderDTO {
    private Long id;
    private String name;
    /** 1=系统默认收藏夹，不可删除 */
    private Integer isDefault;
    private Integer itemCount;
    private Integer sortOrder;
}
