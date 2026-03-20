package org.example.myblog.serverl.impl;

import org.example.myblog.entiy.SensitiveWord;
import org.example.myblog.mapper.SensitiveWordMapper;
import org.example.myblog.serverl.ContentModerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    private static final int LEVEL_WARN = 1;   // 低风险：提醒
    private static final int LEVEL_BLOCK = 2;  // 高风险：直接拦截
    private static final int LEVEL_REVIEW = 3; // 中风险：人工审核

    private static final Pattern NON_WORD_PATTERN = Pattern.compile("[\\p{Punct}\\p{IsPunctuation}\\p{S}\\s]+");
    private static final Pattern WECHAT_VARIANT_PATTERN = Pattern.compile("(加\\s*[vV]|家\\s*[vV]|微\\s*[^\\p{L}\\p{N}]*\\s*信|薇\\s*[^\\p{L}\\p{N}]*\\s*信)");

    @Autowired(required = false)
    private SensitiveWordMapper sensitiveWordMapper;

    @Override
    public ModerationResult moderateText(String content) {
        if (content == null || content.isBlank()) return ModerationResult.none();

        // 1) 组合规则优先：同时命中关键项才触发（用于降低误杀）
        String normalized = normalizeText(content);
        if (containsAll(normalized, "兼职", "日结", "高薪")) {
            return new ModerationResult(ModerationAction.BLOCK, "组合规则:兼职+日结+高薪");
        }

        // 2) 变体词规则（如：加V、加v、家V、薇信、微*信）
        if (WECHAT_VARIANT_PATTERN.matcher(content).find()) {
            return new ModerationResult(ModerationAction.BLOCK, "变体词:引流联系方式");
        }

        // 3) 词库规则：忽略大小写 + 去空格符号后匹配
        if (sensitiveWordMapper == null) return ModerationResult.none();
        List<SensitiveWord> words = sensitiveWordMapper.listActiveEntries();
        if (words == null || words.isEmpty()) return ModerationResult.none();

        ModerationAction strongest = ModerationAction.NONE;
        for (SensitiveWord entry : words) {
            if (entry == null || entry.getWord() == null || entry.getWord().isBlank()) continue;
            boolean hit = matchesWord(content, normalized, entry.getWord());
            if (!hit) continue;

            int level = entry.getLevel() == null ? LEVEL_BLOCK : entry.getLevel();
            if (level == LEVEL_BLOCK) return new ModerationResult(ModerationAction.BLOCK, entry.getWord());
            if (level == LEVEL_REVIEW) strongest = ModerationAction.REVIEW;
            if (level == LEVEL_WARN && strongest == ModerationAction.NONE) strongest = ModerationAction.WARN;
        }
        return new ModerationResult(strongest, null);
    }

    private boolean matchesWord(String rawContent, String normalizedContent, String word) {
        String w = word.trim();
        if (w.isEmpty()) return false;
        if (rawContent.toLowerCase(Locale.ROOT).contains(w.toLowerCase(Locale.ROOT))) return true;
        String normalizedWord = normalizeText(w);
        if (!normalizedWord.isEmpty() && normalizedContent.contains(normalizedWord)) return true;

        if ("微信".equals(w) || "vx".equalsIgnoreCase(w) || "v".equalsIgnoreCase(w)) {
            if (WECHAT_VARIANT_PATTERN.matcher(rawContent).find()) return true;
        }
        return false;
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(Locale.ROOT);
        return NON_WORD_PATTERN.matcher(lower).replaceAll("");
    }

    private boolean containsAll(String normalizedText, String... words) {
        if (normalizedText == null || normalizedText.isEmpty() || words == null || words.length == 0) return false;
        return Arrays.stream(words)
                .map(this::normalizeText)
                .filter(w -> !w.isEmpty())
                .allMatch(normalizedText::contains);
    }
}
