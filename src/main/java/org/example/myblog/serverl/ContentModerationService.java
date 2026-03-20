package org.example.myblog.serverl;

public interface ContentModerationService {

    ModerationResult moderateText(String content);

    enum ModerationAction {
        NONE, WARN, REVIEW, BLOCK
    }

    class ModerationResult {
        private final ModerationAction action;
        private final String hitWord;

        public ModerationResult(ModerationAction action, String hitWord) {
            this.action = action;
            this.hitWord = hitWord;
        }

        public static ModerationResult none() {
            return new ModerationResult(ModerationAction.NONE, null);
        }

        public ModerationAction getAction() {
            return action;
        }

        public String getHitWord() {
            return hitWord;
        }
    }
}
