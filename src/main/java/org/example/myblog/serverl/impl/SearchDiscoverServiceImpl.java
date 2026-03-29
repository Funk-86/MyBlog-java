package org.example.myblog.serverl.impl;

import org.example.myblog.dto.SearchDiscoverItemVO;
import org.example.myblog.dto.SearchDiscoverPairVO;
import org.example.myblog.entiy.Post;
import org.example.myblog.entiy.Topic;
import org.example.myblog.mapper.PostMapper;
import org.example.myblog.mapper.TopicMapper;
import org.example.myblog.serverl.SearchDiscoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
public class SearchDiscoverServiceImpl implements SearchDiscoverService {

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private PostMapper postMapper;

    @Override
    public List<SearchDiscoverPairVO> discover(int pairCount) {
        if (pairCount < 4) pairCount = 4;
        if (pairCount > 15) pairCount = 15;
        int needItems = pairCount * 2;

        int fetch = Math.max(needItems, 24);
        List<Topic> topics = topicMapper.listHotTopics(Math.min(fetch, 50));
        List<Post> posts = postMapper.listByHotScore(0, Math.min(fetch, 50));

        Set<String> usedTexts = new HashSet<>();
        List<SearchDiscoverItemVO> items = new ArrayList<>();

        if (topics != null) {
            for (Topic t : topics) {
                if (t == null || t.getName() == null) continue;
                String name = t.getName().trim();
                if (name.isEmpty()) continue;
                String key = name.toLowerCase();
                if (usedTexts.contains(key)) continue;
                usedTexts.add(key);
                items.add(SearchDiscoverItemVO.builder()
                        .type("topic")
                        .text(name)
                        .topicId(t.getId())
                        .postId(null)
                        .build());
            }
        }

        if (posts != null) {
            for (Post p : posts) {
                if (p == null || p.getTitle() == null) continue;
                String title = p.getTitle().trim();
                if (title.isEmpty()) continue;
                String key = title.toLowerCase();
                if (usedTexts.contains(key)) continue;
                usedTexts.add(key);
                items.add(SearchDiscoverItemVO.builder()
                        .type("post")
                        .text(title)
                        .topicId(null)
                        .postId(p.getId())
                        .build());
            }
        }

        Collections.shuffle(items, new Random());

        int take = Math.min(needItems, items.size());
        if (take == 0) {
            return List.of();
        }
        items = new ArrayList<>(items.subList(0, take));

        List<SearchDiscoverPairVO> pairs = new ArrayList<>();
        for (int i = 0; i < items.size(); i += 2) {
            SearchDiscoverItemVO left = items.get(i);
            SearchDiscoverItemVO right = i + 1 < items.size() ? items.get(i + 1) : null;
            pairs.add(new SearchDiscoverPairVO(left, right));
        }
        return pairs;
    }
}
