package org.example.myblog.serverl;

import org.example.myblog.config.AliyunGreenProperties;
import org.example.myblog.entiy.Post;
import org.example.myblog.mapper.PostMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.example.myblog.dto.SendMessageRequest;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.green.model.v20180509.ImageSyncScanRequest;
import com.aliyuncs.http.MethodType;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationPlusRequest;
import com.aliyun.green20220302.models.TextModerationPlusResponse;
import com.aliyun.green20220302.models.TextModerationPlusResponseBody;
import com.aliyun.teaopenapi.models.Config;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;

/**
 * 阿里云内容安全统一服务：文字 / 图片 / 视频审核
 * 这里只定义对外接口和基础结构，具体 SDK / HTTP 调用可根据阿里云官方文档补充。
 */
@Service
public class AliyunGreenService {

    /** 用于系统通知的管理员账号 ID（请确保数据库中存在该用户），与 PostServiceImpl 保持一致 */
    private static final long SYSTEM_ADMIN_ID = 6L;

    private final AliyunGreenProperties props;
    private final PostMapper postMapper;
    // 旧版 Green 1.0 客户端（用于图片/视频）
    private volatile IAcsClient greenClient;
    // 文本审核增强版 PLUS 客户端（green20220302）
    private volatile Client plusClient;

    /** 站内信服务，用于给用户发送“AI审核通过/拦截”的系统通知 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ChatService chatService;

    private IAcsClient getClient() {
        if (greenClient != null) return greenClient;
        synchronized (this) {
            if (greenClient != null) return greenClient;
            String regionId = props.getRegionIdOrDefault();
            String ak = props.getAccessKeyId();
            String masked = (ak == null || ak.isBlank()) ? "(empty)" : ak.substring(0, Math.min(4, ak.length())) + "***";
            System.out.println("[AliyunGreen] init image client region=" + regionId + " ak=" + masked
                    + " connectTimeoutMs=" + props.getConnectTimeoutMs() + " readTimeoutMs=" + props.getReadTimeoutMs());
            DefaultProfile profile = DefaultProfile.getProfile(
                    regionId,
                    props.getAccessKeyId(),
                    props.getAccessKeySecret()
            );
            greenClient = new DefaultAcsClient(profile);
            return greenClient;
        }
    }

    private Client getPlusClient() throws Exception {
        if (plusClient != null) return plusClient;
        synchronized (this) {
            if (plusClient != null) return plusClient;
            int cto = props.getConnectTimeoutMs();
            int rto = props.getReadTimeoutMs();
            String regionId = props.getRegionIdOrDefault();
            String ep = props.resolveGreenCipEndpointHost();
            String ak = props.getAccessKeyId();
            String masked = (ak == null || ak.isBlank()) ? "(empty)" : ak.substring(0, Math.min(4, ak.length())) + "***";
            System.out.println("[AliyunGreen] init text Plus client region=" + regionId + " endpoint=" + ep + " ak=" + masked
                    + " connectTimeoutMs=" + cto + " readTimeoutMs=" + rto);
            Config config = new Config()
                    .setAccessKeyId(props.getAccessKeyId())
                    .setAccessKeySecret(props.getAccessKeySecret())
                    .setRegionId(regionId)
                    .setEndpoint(ep)
                    .setReadTimeout(rto)
                    .setConnectTimeout(cto);
            plusClient = new Client(config);
            return plusClient;
        }
    }

    /** 连接超时、DNS、TLS 等可重试；业务错误不重试由 checkTextOnce 内部 return */
    private static boolean isLikelyTransientNetworkError(Throwable t) {
        while (t != null) {
            if (t instanceof SocketTimeoutException || t instanceof ConnectException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("timed out") || msg.contains("Timed out") || msg.contains("Connection reset"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }


    /**
     * 文本审核
     *
     * @param content 待审核文本
     * @return 建议结果：pass / review / block
     */
    public String checkText(String content) {
        if (content == null || content.isBlank()) {
            return "pass";
        }
        int max = props.getTextMaxAttempts();
        Exception last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                return checkTextOnce(content);
            } catch (Exception e) {
                last = e;
                if (attempt < max && isLikelyTransientNetworkError(e)) {
                    System.err.println("[AliyunGreen] text moderation network error, attempt " + attempt + "/" + max + ": " + e.getMessage());
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    e.printStackTrace();
                    return "review";
                }
            }
        }
        if (last != null) {
            last.printStackTrace();
        }
        return "review";
    }

    /**
     * 单次调用文本审核；仅抛出网络/SDK 异常，业务层 code!=200 仍返回 review 字符串。
     */
    private String checkTextOnce(String content) throws Exception {
        Client client = getPlusClient();

        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("content", content);

        TextModerationPlusRequest request = new TextModerationPlusRequest();
        request.setService("comment_detection_pro");
        request.setServiceParameters(serviceParameters.toJSONString());

        TextModerationPlusResponse response = client.textModerationPlus(request);
        if (response.getStatusCode() != 200) {
            System.out.println("TextModerationPlus response not success. status=" + response.getStatusCode());
            return "review";
        }

        TextModerationPlusResponseBody body = response.getBody();
        System.out.println("TextModerationPlus body = " + JSON.toJSONString(body));

        Integer code = body.getCode();
        if (code == null || code != 200) {
            System.out.println("text moderation not success. code=" + code);
            return "review";
        }

        TextModerationPlusResponseBody.TextModerationPlusResponseBodyData data = body.getData();
        System.out.println("TextModerationPlus data = " + JSON.toJSONString(data));
        if (data == null) {
            return "pass";
        }

        String overallRiskLevel = data.getRiskLevel();
        System.out.println("TextModerationPlus overall riskLevel = " + overallRiskLevel);
        if (overallRiskLevel != null && "none".equalsIgnoreCase(overallRiskLevel)) {
            return "pass";
        }

        java.util.List<TextModerationPlusResponseBody.TextModerationPlusResponseBodyDataResult> results =
                data.getResult();
        if (results == null || results.isEmpty()) {
            return "pass";
        }

        boolean hasResult = false;
        for (TextModerationPlusResponseBody.TextModerationPlusResponseBodyDataResult r : results) {
            String json = JSON.toJSONString(r);
            System.out.println("TextModerationPlus item = " + json);
            hasResult = true;
            try {
                JSONObject obj = JSON.parseObject(json);
                String riskLevel = obj.getString("riskLevel");
                if (riskLevel == null) {
                    riskLevel = obj.getString("risk_level");
                }
                if (riskLevel != null) {
                    System.out.println("TextModerationPlus riskLevel = " + riskLevel);
                    if ("high".equalsIgnoreCase(riskLevel)
                            || "HIGH_RISK".equalsIgnoreCase(riskLevel)) {
                        return "block";
                    }
                }
            } catch (Exception ignore) {
                // 解析失败则退化为后面的 review 处理
            }
        }
        return hasResult ? "review" : "pass";
    }

    public AliyunGreenService(AliyunGreenProperties props, PostMapper postMapper) {
        this.props = props;
        this.postMapper = postMapper;
    }

    /**
     * 图片审核
     *
     * @param imageUrls 待审核图片的公网 URL 列表
     * @return 建议结果：pass / review / block
     */
    public String checkImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "pass";
        }
        try {
            IAcsClient client = getClient();

            ImageSyncScanRequest request = new ImageSyncScanRequest();
            // 旧版 SDK 使用 setMethod，而不是 setSysMethod
            request.setMethod(MethodType.POST);
            request.setConnectTimeout(props.getConnectTimeoutMs());
            request.setReadTimeout(props.getReadTimeoutMs());

            // scenes：按你控制台开通的场景来，这里举例涉黄+暴恐
            JSONArray scenes = new JSONArray();
            scenes.add("porn");
            scenes.add("terrorism");

            JSONArray tasks = new JSONArray();
            for (String url : imageUrls) {
                if (url == null || url.isBlank()) continue;
                JSONObject task = new JSONObject();
                task.put("dataId", java.util.UUID.randomUUID().toString());
                task.put("url", url);
                // 可选 extra 字段
                tasks.add(task);
            }
            if (tasks.isEmpty()) {
                return "pass";
            }

            JSONObject body = new JSONObject();
            body.put("scenes", scenes);
            body.put("tasks", tasks);

            request.setHttpContent(
                    body.toJSONString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "UTF-8",
                    com.aliyuncs.http.FormatType.JSON
            );

            com.aliyuncs.http.HttpResponse response = client.doAction(request);
            if (!response.isSuccess()) {
                return "review";
            }

            JSONObject resp = JSON.parseObject(new String(response.getHttpContent(), java.nio.charset.StandardCharsets.UTF_8));
            if (resp.getIntValue("code") != 200) {
                return "review";
            }

            JSONArray data = resp.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return "pass";
            }

            boolean hasReview = false;
            for (int i = 0; i < data.size(); i++) {
                JSONObject taskResult = data.getJSONObject(i);
                if (taskResult.getIntValue("code") != 200) continue;
                JSONArray results = taskResult.getJSONArray("results");
                if (results == null) continue;
                for (int j = 0; j < results.size(); j++) {
                    JSONObject sceneResult = results.getJSONObject(j);
                    String suggestion = sceneResult.getString("suggestion");
                    if ("block".equalsIgnoreCase(suggestion)) {
                        return "block";
                    }
                    if ("review".equalsIgnoreCase(suggestion)) {
                        hasReview = true;
                    }
                }
            }
            return hasReview ? "review" : "pass";
        } catch (Exception e) {
            return "review";
        }
    }

    /**
     * 视频审核
     *
     * @param videoUrl 待审核视频的公网 URL
     * @return 建议结果：pass / review / block
     */
    public String checkVideo(String videoUrl) {
        // 这里 videoUrl 建议传封面图 URL，如果没有封面就直接 pass 或者走 review
        if (videoUrl == null || videoUrl.isBlank()) {
            return "pass";
        }
        // 简单版：当作一张图片来审
        return checkImages(java.util.Collections.singletonList(videoUrl));
    }

    /**
     * 异步审核整条帖子：文本 + 图片 + 视频
     * 说明：这里用 Spring @Async 在后台线程调用同步接口，实现业务层面的“异步审核”。
     * 审核结果会自动回写到 post.status：0=通过，1=人工审核中，2=审核拒绝/删除，3=AI 拦截。
     */
    @Async
    public void asyncCheckPostContent(Post post, List<String> imageUrls, String videoUrl) {
        if (post == null || post.getId() == null) {
            return;
        }
        System.out.println(">>> asyncCheckPostContent called, postId = " + post.getId());

        // 文本：标题 + 正文
        StringBuilder sb = new StringBuilder();
        if (post.getTitle() != null) sb.append(post.getTitle()).append('\n');
        if (post.getContent() != null) sb.append(post.getContent());
        String textSuggestion = checkText(sb.toString());

        // 图片：直接使用存储在数据库中的 URL（需保证是公网可访问）
        String imageSuggestion = checkImages(imageUrls);

        // 视频：这里传的是封面图 URL，当作图片审核
        String videoSuggestion = checkVideo(videoUrl);

        System.out.println(">>> textSuggestion=" + textSuggestion
                + ", imageSuggestion=" + imageSuggestion
                + ", videoSuggestion=" + videoSuggestion);

        String finalSuggestion = mergeSuggestion(textSuggestion, imageSuggestion, videoSuggestion);
        if ("block".equalsIgnoreCase(finalSuggestion)) {
            // AI 明确判定违规
            postMapper.updateStatus(post.getId(), 3);
            System.out.println(">>> asyncCheckPostContent updated status=3 (block) for postId = " + post.getId());
            sendSystemNotify(post.getUserId(), buildAiRejectedText(post.getTitle()));
        } else if ("review".equalsIgnoreCase(finalSuggestion)) {
            // AI 建议人工审核：进入后台审核列表（status=1）
            postMapper.updateStatus(post.getId(), 1);
            System.out.println(">>> asyncCheckPostContent updated status=1 (review->manual) for postId = " + post.getId());
            sendSystemNotify(post.getUserId(), buildAiReviewText(post.getTitle()));
        } else {
            postMapper.updateStatus(post.getId(), 0);
            System.out.println(">>> asyncCheckPostContent updated status=0 (pass) for postId = " + post.getId());
            sendSystemNotify(post.getUserId(), buildAiApprovedText(post.getTitle()));
        }
    }

    private void sendSystemNotify(Long toUserId, String content) {
        if (chatService == null || toUserId == null || content == null || content.isBlank()) return;
        try {
            SendMessageRequest req = new SendMessageRequest();
            req.setFromUserId(SYSTEM_ADMIN_ID);
            req.setToUserId(toUserId);
            req.setContent(content);
            req.setContentType(0);
            chatService.sendMessage(req);
        } catch (Exception ignored) {
        }
    }

    private String buildAiApprovedText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "已通过审核，现已发布。";
    }

    private String buildAiRejectedText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "未通过审核，已被系统拦截。";
    }

    private String buildAiReviewText(String title) {
        String t = (title == null || title.isBlank()) ? "您发布的帖子" : "您发布的《" + title + "》";
        return t + "审核中，已进入人工复核流程。";
    }

    private String mergeSuggestion(String... suggestions) {
        boolean hasReview = false;
        if (suggestions == null) return "pass";
        for (String s : suggestions) {
            if (s == null) continue;
            String v = s.toLowerCase();
            if ("block".equals(v)) return "block";
            if ("review".equals(v)) hasReview = true;
        }
        return hasReview ? "review" : "pass";
    }
}

