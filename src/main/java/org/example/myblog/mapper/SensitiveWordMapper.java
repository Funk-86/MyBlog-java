package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.entiy.SensitiveWord;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {

    @Select("SELECT word FROM sensitive_word WHERE status = 0")
    List<String> listActiveWords();

    @Select("SELECT id, word, level, status, created_at AS createdAt, updated_at AS updatedAt FROM sensitive_word WHERE status = 0")
    List<SensitiveWord> listActiveEntries();

    @Select("SELECT id, word, level, status, created_at AS createdAt, updated_at AS updatedAt FROM sensitive_word ORDER BY id DESC")
    List<SensitiveWord> listAll();

    @Select("SELECT COUNT(*) FROM sensitive_word")
    long countAll();

    @Select("SELECT id, word, level, status, created_at AS createdAt, updated_at AS updatedAt FROM sensitive_word ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}")
    List<SensitiveWord> listPaged(@Param("offset") int offset, @Param("limit") int limit);

    @Insert("INSERT INTO sensitive_word(word, level, status, created_at, updated_at) " +
            "VALUES(#{word}, #{level}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SensitiveWord word);

    @Delete("DELETE FROM sensitive_word WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);
}

