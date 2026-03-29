package org.example.myblog.dto;

import lombok.Data;

import java.util.List;

@Data
public class AdminBatchPostIdsRequest {
    private List<Long> postIds;
}
