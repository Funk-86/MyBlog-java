package org.example.myblog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReportMapper {

    /**
     * 管理端：举报列表，按创建时间倒序
     * target_type: 1=用户, 2=帖子, 3=评论
     */
    @Select("""
            SELECT r.id,
                   r.reporter_id AS reporterId,
                   COALESCE(up.nickname, u.username) AS reporterName,
                   r.target_type AS targetType,
                   r.target_id AS targetId,
                   r.reason_code AS reasonCode,
                   r.description,
                   r.status,
                   r.handler_id AS handlerId,
                   r.result,
                   r.created_at AS createdAt,
                   r.updated_at AS updatedAt
            FROM report r
                     LEFT JOIN `user` u ON u.id = r.reporter_id
                     LEFT JOIN user_profile up ON up.user_id = r.reporter_id
            ORDER BY r.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Map<String, Object>> listForAdmin(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 创建举报记录
     */
    @Insert("""
            INSERT INTO report (reporter_id, target_type, target_id, reason_code, description, status, created_at, updated_at)
            VALUES (#{reporterId}, #{targetType}, #{targetId}, #{reasonCode}, #{description}, 0, NOW(), NOW())
            """)
    int insertReport(@Param("reporterId") Long reporterId,
                     @Param("targetType") Integer targetType,
                     @Param("targetId") Long targetId,
                     @Param("reasonCode") Integer reasonCode,
                     @Param("description") String description);

    /**
     * 根据 ID 查询举报详情
     */
    @Select("SELECT * FROM report WHERE id = #{id}")
    Map<String, Object> selectById(@Param("id") Long id);

    /**
     * 更新举报状态和处理结果
     * status: 0待处理,1已处理(成立),2已处理(不成立)
     */
    @Update("""
            UPDATE report
            SET status = #{status},
                handler_id = #{handlerId},
                result = #{result},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("handlerId") Long handlerId,
                     @Param("result") String result);

    /**
     * 批量更新同一 target 的举报状态（例如同一评论被多次举报）
     */
    @Update("""
            UPDATE report
            SET status = #{status},
                handler_id = #{handlerId},
                result = #{result},
                updated_at = NOW()
            WHERE target_type = #{targetType}
              AND target_id = #{targetId}
              AND status = 0
            """)
    int updateStatusByTarget(@Param("targetType") Integer targetType,
                             @Param("targetId") Long targetId,
                             @Param("status") Integer status,
                             @Param("handlerId") Long handlerId,
                             @Param("result") String result);

    /**
     * 待处理举报数量（status=0）
     */
    @Select("SELECT COUNT(*) FROM report WHERE status = 0")
    long countPending();
}
