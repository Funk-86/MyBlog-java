package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.Message;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("""
            INSERT INTO message (conversation_id, sender_id, content_type, content, status, created_at)
            VALUES (#{conversationId}, #{senderId}, #{contentType}, #{content}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Message message);

    @Select("""
            <script>
            SELECT id,
                   conversation_id AS conversationId,
                   sender_id       AS senderId,
                   content_type    AS contentType,
                   content,
                   status,
                   created_at      AS createdAt
            FROM message
            WHERE conversation_id = #{conversationId}
              <if test="beforeId != null">
                AND id &lt; #{beforeId}
              </if>
            ORDER BY id DESC
            LIMIT #{limit}
            </script>
            """)
    List<Message> listByConversation(@Param("conversationId") Long conversationId,
                                     @Param("beforeId") Long beforeId,
                                     @Param("limit") int limit);

    @Select("""
            SELECT id,
                   conversation_id AS conversationId,
                   sender_id       AS senderId,
                   content_type    AS contentType,
                   content,
                   status,
                   created_at      AS createdAt
            FROM message
            WHERE conversation_id = #{conversationId}
            ORDER BY id DESC
            LIMIT 1
            """)
    Message selectLastMessage(@Param("conversationId") Long conversationId);

    /** 只统计对方发给我、且在我上次已读之后的消息（自己发的消息不算未读） */
    @Select("""
            <script>
            SELECT COUNT(*) FROM message
            WHERE conversation_id = #{conversationId}
              AND sender_id != #{userId}
              <if test="afterId != null">
                AND id &gt; #{afterId}
              </if>
            </script>
            """)
    int countUnread(@Param("conversationId") Long conversationId,
                    @Param("userId") Long userId,
                    @Param("afterId") Long afterId);
}

