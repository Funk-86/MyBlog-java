package org.example.myblog.serverl.impl;

import org.example.myblog.dto.ConversationDTO;
import org.example.myblog.dto.MessageDTO;
import org.example.myblog.dto.SendMessageRequest;
import org.example.myblog.entiy.Conversation;
import org.example.myblog.entiy.ConversationMember;
import org.example.myblog.entiy.Message;
import org.example.myblog.mapper.ConversationMapper;
import org.example.myblog.mapper.ConversationMemberMapper;
import org.example.myblog.mapper.MessageMapper;
import org.example.myblog.mapper.UserMapper;
import org.example.myblog.serverl.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private ConversationMemberMapper conversationMemberMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private static final String LAST_MSG_KEY_PREFIX = "conv:last_msg:";
    private static final String UNREAD_KEY_PREFIX = "unread:";

    @Override
    @Transactional
    public Long getOrCreateSingleConversation(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null) return null;
        if (userId1.equals(userId2)) return null;
        Long a = Math.min(userId1, userId2);
        Long b = Math.max(userId1, userId2);
        Long convId = conversationMemberMapper.findSingleConversationId(a, b);
        if (convId != null) {
            return convId;
        }
        Conversation conv = new Conversation();
        conv.setType(1);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(conv.getCreatedAt());
        conversationMapper.insert(conv);

        ConversationMember m1 = new ConversationMember();
        m1.setConversationId(conv.getId());
        m1.setUserId(a);
        m1.setRole(0);
        m1.setLastReadMessageId(null);
        m1.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(m1);

        ConversationMember m2 = new ConversationMember();
        m2.setConversationId(conv.getId());
        m2.setUserId(b);
        m2.setRole(0);
        m2.setLastReadMessageId(null);
        m2.setJoinedAt(LocalDateTime.now());
        conversationMemberMapper.insert(m2);

        return conv.getId();
    }

    @Override
    @Transactional
    public MessageDTO sendMessage(SendMessageRequest request) {
        if (request == null || request.getFromUserId() == null) {
            return null;
        }
        Long from = request.getFromUserId();
        Long to = request.getToUserId();
        Long convId = request.getConversationId();
        if (convId == null && to != null) {
            convId = getOrCreateSingleConversation(from, to);
        }
        if (convId == null) return null;

        Message msg = new Message();
        msg.setConversationId(convId);
        msg.setSenderId(from);
        // 默认文本消息，前端可传入图片等类型
        Integer type = request.getContentType();
        msg.setContentType(type == null ? 0 : type);
        msg.setContent(request.getContent());
        msg.setStatus(0);
        msg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(msg);

        conversationMapper.touch(convId, msg.getCreatedAt());

        // Redis：最近一条消息 + 未读
        if (redisTemplate != null && to != null) {
            String lastKey = LAST_MSG_KEY_PREFIX + convId;
            String json = msg.getContent();
            try {
                redisTemplate.opsForValue().set(lastKey, json);
            } catch (Exception ignored) {
            }
            String unreadKey = UNREAD_KEY_PREFIX + to + ":" + convId;
            try {
                redisTemplate.opsForValue().increment(unreadKey);
            } catch (Exception ignored) {
            }
        }

        return toMessageDTO(msg, from);
    }

    @Override
    @Transactional
    public List<MessageDTO> listMessages(Long conversationId, Long userId, Long beforeId, int size) {
        if (conversationId == null) return List.of();
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        List<Message> list = messageMapper.listByConversation(conversationId, beforeId, size);
        // 更新已读
        if (userId != null && !list.isEmpty()) {
            Long lastId = list.get(0).getId();
            conversationMemberMapper.updateLastRead(conversationId, userId, lastId);
            if (redisTemplate != null) {
                String unreadKey = UNREAD_KEY_PREFIX + userId + ":" + conversationId;
                try {
                    redisTemplate.delete(unreadKey);
                } catch (Exception ignored) {
                }
            }
        }
        List<MessageDTO> dtoList = new ArrayList<>();
        for (int i = list.size() - 1; i >= 0; i--) {
            dtoList.add(toMessageDTO(list.get(i), userId));
        }
        return dtoList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDTO> listConversations(Long userId) {
        if (userId == null) return List.of();
        List<Conversation> convs = conversationMapper.listByUser(userId);
        List<ConversationDTO> result = new ArrayList<>();
        for (Conversation c : convs) {
            ConversationDTO dto = new ConversationDTO();
            dto.setConversationId(c.getId());

            Map<String, Object> peerInfo = conversationMemberMapper.selectPeerInfo(c.getId(), userId);
            if (peerInfo != null) {
                Object pid = peerInfo.get("id");
                if (pid instanceof Number) {
                    dto.setPeerId(((Number) pid).longValue());
                }
                dto.setPeerName((String) peerInfo.get("name"));
                dto.setPeerAvatar((String) peerInfo.get("avatarUrl"));
            }

            Message last = null;
            if (redisTemplate != null) {
                // 目前 last_msg 只存 content 文本，简单起见直接从数据库查完整记录
                try {
                    last = messageMapper.selectLastMessage(c.getId());
                } catch (Exception ignored) {
                }
            } else {
                last = messageMapper.selectLastMessage(c.getId());
            }
            if (last != null) {
                dto.setLastMsgContent(last.getContent());
                dto.setLastMsgTime(last.getCreatedAt());
            }

            Long lastReadId = conversationMemberMapper.getLastReadMessageId(c.getId(), userId);
            int unread = messageMapper.countUnread(c.getId(), userId, lastReadId);
            dto.setUnreadCount(unread);

            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public void clearAllUnread(Long userId) {
        if (userId == null) return;
        List<Conversation> convs = conversationMapper.listByUser(userId);
        if (convs == null || convs.isEmpty()) return;
        for (Conversation c : convs) {
            Message last = messageMapper.selectLastMessage(c.getId());
            if (last == null) continue;
            conversationMemberMapper.updateLastRead(c.getId(), userId, last.getId());
            if (redisTemplate != null) {
                String unreadKey = UNREAD_KEY_PREFIX + userId + ":" + c.getId();
                try {
                    redisTemplate.delete(unreadKey);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private MessageDTO toMessageDTO(Message msg, Long currentUserId) {
        if (msg == null) return null;
        MessageDTO dto = new MessageDTO();
        dto.setId(msg.getId());
        dto.setConversationId(msg.getConversationId());
        dto.setSenderId(msg.getSenderId());
        dto.setContentType(msg.getContentType());
        dto.setContent(msg.getContent());
        dto.setCreatedAt(msg.getCreatedAt());
        if (currentUserId != null) {
            dto.setMine(msg.getSenderId() != null && msg.getSenderId().equals(currentUserId));
        }
        var info = userMapper.selectByIdWithProfile(msg.getSenderId());
        if (info != null) {
            String name = (String) info.get("nickname");
            if (name == null || name.isBlank()) {
                name = (String) info.get("username");
            }
            dto.setSenderName(name);
            dto.setSenderAvatar((String) info.get("avatarUrl"));
        }
        return dto;
    }
}

