package org.example.myblog.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.myblog.entiy.Category;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 查询启用状态的分区，按 sort_order 升序（发帖时展示）
     */
    @Select("""
            SELECT id, name, code, sort_order AS sortOrder, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM category
            WHERE status = 1
            ORDER BY sort_order ASC, id ASC
            """)
    List<Category> listAll();

    /**
     * 管理端：查询全部分区（含禁用），按 sort_order、id 升序
     */
    @Select("""
            SELECT id, name, code, sort_order AS sortOrder, status,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM category
            ORDER BY sort_order ASC, id ASC
            """)
    List<Category> listAllForAdmin();

    /**
     * 按主键查询（可选，用于校验发帖传入的 categoryId）
     */
    @Select("SELECT id, name, code, sort_order AS sortOrder, status, created_at AS createdAt, updated_at AS updatedAt FROM category WHERE id = #{id}")
    Category selectById(@Param("id") Long id);

    @Insert("""
            INSERT INTO category (name, code, sort_order, status, created_at, updated_at)
            VALUES (#{name}, #{code}, #{sortOrder}, #{status}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Category category);

    @Delete("DELETE FROM category WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 管理端：更新分区
     */
    @Update("""
            UPDATE category SET name = #{name}, code = #{code}, sort_order = #{sortOrder}, status = #{status}
            WHERE id = #{id}
            """)
    int update(Category category);
}
