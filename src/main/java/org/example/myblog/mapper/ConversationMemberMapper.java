package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.ConversationMember;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ConversationMemberMapper {

    @Insert("""
            INSERT INTO conversation_member (conversation_id, user_id, role, last_read_message_id, joined_at)
            VALUES (#{conversationId}, #{userId}, #{role}, #{lastReadMessageId}, #{joinedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ConversationMember member);

    @Select("""
            SELECT id,
                   conversation_id AS conversationId,
                   user_id         AS userId,
                   role,
                   last_read_message_id AS lastReadMessageId,
                   joined_at       AS joinedAt
            FROM conversation_member
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            """)
    ConversationMember selectOne(@Param("conversationId") Long conversationId,
                                 @Param("userId") Long userId);

    @Update("""
            UPDATE conversation_member
            SET last_read_message_id = #{lastReadMessageId}
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            """)
    int updateLastRead(@Param("conversationId") Long conversationId,
                       @Param("userId") Long userId,
                       @Param("lastReadMessageId") Long lastReadMessageId);

    @Select("""
            SELECT last_read_message_id
            FROM conversation_member
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            """)
    Long getLastReadMessageId(@Param("conversationId") Long conversationId,
                              @Param("userId") Long userId);

    /**
     * 单聊场景：获取对方用户的基础信息（昵称 + 头像）
     */
    @Select("""
            SELECT u.id,
                   COALESCE(up.nickname, u.username) AS name,
                   up.avatar_url                     AS avatarUrl
            FROM conversation_member m
                     JOIN `user` u ON u.id = m.user_id
                     LEFT JOIN user_profile up ON up.user_id = u.id
            WHERE m.conversation_id = #{conversationId}
              AND m.user_id <> #{selfUserId}
            LIMIT 1
            """)
    Map<String, Object> selectPeerInfo(@Param("conversationId") Long conversationId,
                                       @Param("selfUserId") Long selfUserId);

    /**
     * 查询两个人是否已经有单聊会话（通过成员表判断）
     */
    @Select("""
            SELECT cm1.conversation_id
            FROM conversation_member cm1
                     JOIN conversation_member cm2
                          ON cm1.conversation_id = cm2.conversation_id
            JOIN conversation c ON c.id = cm1.conversation_id AND c.type = 1
            WHERE cm1.user_id = #{userId1}
              AND cm2.user_id = #{userId2}
            LIMIT 1
            """)
    Long findSingleConversationId(@Param("userId1") Long userId1,
                                  @Param("userId2") Long userId2);

    @Select("""
            SELECT joined_at
            FROM conversation_member
            WHERE conversation_id = #{conversationId}
              AND user_id = #{userId}
            """)
    LocalDateTime getJoinedAt(@Param("conversationId") Long conversationId,
                              @Param("userId") Long userId);

    @Select("""
            SELECT user_id AS userId
            FROM conversation_member
            WHERE conversation_id = #{conversationId}
            """)
    List<Long> listUserIdsByConversation(@Param("conversationId") Long conversationId);
}

