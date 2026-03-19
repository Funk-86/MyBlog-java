package org.example.myblog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.example.myblog.entiy.PostMedia;

@Mapper
public interface PostMediaMapper {

    @Insert("""
            INSERT INTO post_media (post_id, media_type, url, cover_url, sort_order, duration_sec)
            VALUES (#{postId}, #{mediaType}, #{url}, #{coverUrl}, #{sortOrder}, #{durationSec})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostMedia media);
}