package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.Topic;

import java.util.List;

@Mapper
public interface TopicMapper {

    @Select("""
            SELECT id,
                   name,
                   slug,
                   post_count AS postCount,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM topic
            WHERE name = #{name}
            """)
    Topic selectByName(@Param("name") String name);

    @Insert("""
            INSERT INTO topic (name, slug, post_count)
            VALUES (#{name}, #{slug}, 0)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Topic topic);

    @Insert("""
            INSERT IGNORE INTO post_topic (post_id, topic_id, created_at)
            VALUES (#{postId}, #{topicId}, NOW())
            """)
    int insertPostTopic(@Param("postId") Long postId, @Param("topicId") Long topicId);

    @Update("""
            UPDATE topic
            SET post_count = post_count + 1
            WHERE id = #{id}
            """)
    int incrPostCount(@Param("id") Long id);

    @Select("""
            SELECT id,
                   name,
                   slug,
                   post_count AS postCount,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM topic
            ORDER BY post_count DESC, created_at DESC
            LIMIT #{limit}
            """)
    List<Topic> listHotTopics(@Param("limit") int limit);

    @Select("""
            SELECT id,
                   name,
                   slug,
                   post_count AS postCount,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM topic
            ORDER BY id DESC
            """)
    List<Topic> listAllForAdmin();

    @Update("""
            UPDATE topic
            SET name = #{name},
                slug = #{slug}
            WHERE id = #{id}
            """)
    int update(Topic topic);

    @Delete("""
            DELETE FROM topic
            WHERE id = #{id}
            """)
    int deleteById(@Param("id") Long id);
}

