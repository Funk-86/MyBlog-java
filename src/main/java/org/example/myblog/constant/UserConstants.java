package org.example.myblog.constant;

/**
 * 用户相关常量：系统账号登录名、保留名校验等。
 */
public final class UserConstants {

    private UserConstants() {
    }

    /**
     * 系统私信账号在数据库中的登录名（user.username），与 ChatServiceImpl 中按用户名解析的系统用户一致。
     * 普通用户注册、改昵称、管理端创建用户时不可使用。
     */
    public static final String SYSTEM_CHAT_USERNAME = "系统聊天";

    /**
     * 是否为系统保留登录名/昵称（当前仅「系统聊天」）。
     */
    public static boolean isReservedSystemChatName(String s) {
        if (s == null) {
            return false;
        }
        return SYSTEM_CHAT_USERNAME.equals(s.trim());
    }
}
