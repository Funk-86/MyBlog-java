package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.Conversation;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper {

    @Insert("""
            INSERT INTO conversation (type, created_at, updated_at)
            VALUES (#{type}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Conversation conversation);

    @Select("""
            SELECT id, type, created_at AS createdAt, updated_at AS updatedAt
            FROM conversation
            WHERE id = #{id}
            """)
    Conversation selectById(@Param("id") Long id);

    /**
     * 当前用户参与的会话列表，按最近更新时间倒序
     */
    @Select("""
            SELECT c.id, c.type, c.created_at AS createdAt, c.updated_at AS updatedAt
            FROM conversation c
                     JOIN conversation_member m ON m.conversation_id = c.id
            WHERE m.user_id = #{userId}
            ORDER BY c.updated_at DESC
            """)
    List<Conversation> listByUser(@Param("userId") Long userId);

    /**
     * 更新会话的更新时间
     */
    @Update("""
            UPDATE conversation
            SET updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int touch(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);
}

