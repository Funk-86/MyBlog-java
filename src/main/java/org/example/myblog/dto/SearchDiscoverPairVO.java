package org.example.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchDiscoverPairVO {
    private SearchDiscoverItemVO left;
    private SearchDiscoverItemVO right;
}
