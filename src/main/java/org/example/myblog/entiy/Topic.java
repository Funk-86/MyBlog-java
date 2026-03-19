package org.example.myblog.entiy;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 话题 topic
 */
@Data
public class Topic {

    private Long id;

    private String name;

    private String slug;

    private Integer postCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

