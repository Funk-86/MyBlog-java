package org.example.myblog.task;

import org.example.myblog.serverl.PostHotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：将 Redis 浏览量同步到 MySQL，并重算热度分数
 */
@Component
public class HotPostSyncTask {

    @Autowired(required = false)
    private PostHotService postHotService;

    /**
     * 每 10 分钟执行：同步 Redis view_count 到 MySQL，刷新 hot_score
     */
    @Scheduled(cron = "0 */10 * * * ?")
    public void syncViewCountAndRefreshHot() {
        if (postHotService != null) {
            postHotService.syncViewCountAndRefreshHot();
        }
    }
}
