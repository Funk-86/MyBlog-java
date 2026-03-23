package org.example.myblog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.myblog.entiy.PostMedia;

@Mapper
public interface PostMediaMapper {

    @Insert("""
            INSERT INTO post_media (post_id, media_type, url, cover_url, sort_order, duration_sec)
            VALUES (#{postId}, #{mediaType}, #{url}, #{coverUrl}, #{sortOrder}, #{durationSec})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PostMedia media);

    @Update("""
            UPDATE post_media
            SET cover_url = #{coverUrl}
            WHERE post_id = #{postId}
              AND media_type = 2
              AND (cover_url IS NULL OR cover_url = '')
            LIMIT 1
            """)
    int updateVideoCoverIfMissing(@Param("postId") Long postId, @Param("coverUrl") String coverUrl);
}