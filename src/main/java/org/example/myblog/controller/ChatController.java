package org.example.myblog.controller;

import org.example.myblog.dto.ConversationDTO;
import org.example.myblog.dto.MessageDTO;
import org.example.myblog.dto.SendMessageRequest;
import org.example.myblog.serverl.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * 获取或创建单聊会话
     * GET /chat/conversation/single?userId1=1&userId2=2
     */
    @GetMapping("/conversation/single")
    @ResponseBody
    public Long getOrCreateSingle(@RequestParam("userId1") Long userId1,
                                  @RequestParam("userId2") Long userId2) {
        return chatService.getOrCreateSingleConversation(userId1, userId2);
    }

    /**
     * 发送消息
     * POST /chat/send  body: { fromUserId, toUserId, conversationId?, content }
     */
    @PostMapping("/send")
    @ResponseBody
    public MessageDTO send(@RequestBody SendMessageRequest request) {
        return chatService.sendMessage(request);
    }

    /**
     * 会话消息列表
     * GET /chat/messages?conversationId=1&userId=1&beforeId&size=20
     */
    @GetMapping("/messages")
    @ResponseBody
    public List<MessageDTO> messages(@RequestParam("conversationId") Long conversationId,
                                     @RequestParam("userId") Long userId,
                                     @RequestParam(value = "beforeId", required = false) Long beforeId,
                                     @RequestParam(value = "size", defaultValue = "20") int size) {
        return chatService.listMessages(conversationId, userId, beforeId, size);
    }

    /**
     * 会话列表
     * GET /chat/conversations?userId=1
     */
    @GetMapping("/conversations")
    @ResponseBody
    public List<ConversationDTO> conversations(@RequestParam("userId") Long userId) {
        return chatService.listConversations(userId);
    }

    /**
     * 清除当前用户所有会话未读数
     * POST /chat/clearUnread?userId=1
     */
    @PostMapping("/clearUnread")
    @ResponseBody
    public void clearUnread(@RequestParam("userId") Long userId) {
        chatService.clearAllUnread(userId);
    }
}

