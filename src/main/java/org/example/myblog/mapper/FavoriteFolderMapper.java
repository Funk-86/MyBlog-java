package org.example.myblog.mapper;

import org.apache.ibatis.annotations.*;
import org.example.myblog.dto.FavoriteFolderDTO;
import org.example.myblog.entiy.FavoriteFolder;

import java.util.List;

@Mapper
public interface FavoriteFolderMapper {

    @Select("SELECT id FROM favorite_folder WHERE user_id = #{userId} AND is_default = 1 LIMIT 1")
    Long selectDefaultFolderId(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO favorite_folder (user_id, name, is_default, sort_order, created_at, updated_at)
            VALUES (#{userId}, #{name}, #{isDefault}, #{sortOrder}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FavoriteFolder folder);

    @Select("SELECT id, user_id AS userId, name, is_default AS isDefault, sort_order AS sortOrder FROM favorite_folder WHERE id = #{id}")
    FavoriteFolder selectById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*) FROM favorite_folder
            WHERE user_id = #{userId} AND LOWER(name) = LOWER(#{name})
            """)
    int countByUserAndName(@Param("userId") Long userId, @Param("name") String name);

    @Select("""
            SELECT ff.id,
                   ff.name,
                   ff.is_default AS isDefault,
                   ff.sort_order AS sortOrder,
                   COUNT(pf.id) AS itemCount
            FROM favorite_folder ff
            LEFT JOIN post_favorite pf ON pf.folder_id = ff.id
            WHERE ff.user_id = #{userId}
            GROUP BY ff.id, ff.name, ff.is_default, ff.sort_order
            ORDER BY ff.is_default DESC, ff.sort_order ASC, ff.id ASC
            """)
    List<FavoriteFolderDTO> listByUserId(@Param("userId") Long userId);

    @Delete("""
            DELETE FROM favorite_folder
            WHERE id = #{id} AND user_id = #{userId} AND is_default = 0
            """)
    int deleteNonDefault(@Param("id") Long id, @Param("userId") Long userId);

    @Update("""
            UPDATE post_favorite pf
            INNER JOIN favorite_folder fd ON fd.user_id = pf.user_id AND fd.is_default = 1
            SET pf.folder_id = fd.id
            WHERE pf.user_id = #{userId} AND pf.folder_id = #{fromFolderId}
            """)
    int moveFavoritesToDefault(@Param("userId") Long userId, @Param("fromFolderId") Long fromFolderId);
}
