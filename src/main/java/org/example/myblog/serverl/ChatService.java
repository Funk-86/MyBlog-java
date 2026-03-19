package org.example.myblog.serverl;

import org.example.myblog.dto.ConversationDTO;
import org.example.myblog.dto.MessageDTO;
import org.example.myblog.dto.SendMessageRequest;

import java.util.List;

public interface ChatService {

    /**
     * 获取或创建单聊会话
     */
    Long getOrCreateSingleConversation(Long userId1, Long userId2);

    /**
     * 发送消息（单聊 / 群聊）
     */
    MessageDTO sendMessage(SendMessageRequest request);

    /**
     * 拉取会话消息列表
     */
    List<MessageDTO> listMessages(Long conversationId, Long userId, Long beforeId, int size);

    /**
     * 当前用户的会话列表
     */
    List<ConversationDTO> listConversations(Long userId);

    /**
     * 清除当前用户所有会话未读数
     */
    void clearAllUnread(Long userId);
}

