package org.example.myblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 热度计算配置
 */
@Component
@ConfigurationProperties(prefix = "hot")
public class HotProperties {

    private Weights weights = new Weights();
    /** 原始热度中的时间衰减系数 */
    private double timeDecay = 1.5;
    /** Wilson 下界与热度线性组合的权重，0 则退化为原公式 */
    private double wilsonWeight = 0.5;
    /** Wilson 置信区间 z 值（1.96 ≈ 95%） */
    private double wilsonZ = 1.96;
    /** 热度与新鲜度混合排序中的 α（0=只看新鲜度，1=只看热度） */
    private double mixAlpha = 0.7;
    private String zsetKey = "zset:post:hot";
    private String viewKeyPrefix = "post:view:";

    public static class Weights {
        private int view = 1;
        private int like = 5;
        private int comment = 3;
        private int favorite = 4;
        /** 阅读到 50% 的人数（去重） */
        private int read50 = 0;
        /** 阅读到 90% 的人数（近似完读，去重） */
        private int read90 = 0;

        public int getView() { return view; }
        public void setView(int view) { this.view = view; }
        public int getLike() { return like; }
        public void setLike(int like) { this.like = like; }
        public int getComment() { return comment; }
        public void setComment(int comment) { this.comment = comment; }
        public int getFavorite() { return favorite; }
        public void setFavorite(int favorite) { this.favorite = favorite; }
        public int getRead50() { return read50; }
        public void setRead50(int read50) { this.read50 = read50; }
        public int getRead90() { return read90; }
        public void setRead90(int read90) { this.read90 = read90; }
    }

    public Weights getWeights() { return weights; }
    public void setWeights(Weights weights) { this.weights = weights; }

    public double getTimeDecay() { return timeDecay; }
    public void setTimeDecay(double timeDecay) { this.timeDecay = timeDecay; }

    public double getWilsonWeight() { return wilsonWeight; }
    public void setWilsonWeight(double wilsonWeight) { this.wilsonWeight = wilsonWeight; }

    public double getWilsonZ() { return wilsonZ; }
    public void setWilsonZ(double wilsonZ) { this.wilsonZ = wilsonZ; }

    public double getMixAlpha() { return mixAlpha; }
    public void setMixAlpha(double mixAlpha) { this.mixAlpha = mixAlpha; }

    public String getZsetKey() { return zsetKey; }
    public void setZsetKey(String zsetKey) { this.zsetKey = zsetKey; }

    public String getViewKeyPrefix() { return viewKeyPrefix; }
    public void setViewKeyPrefix(String viewKeyPrefix) { this.viewKeyPrefix = viewKeyPrefix; }
}

