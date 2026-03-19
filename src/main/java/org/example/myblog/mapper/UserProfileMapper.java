package org.example.myblog.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserProfileMapper {

    /**
     * 插入或更新用户头像
     */
    @Insert("""
            INSERT INTO user_profile (user_id, avatar_url, created_at, updated_at)
            VALUES (#{userId}, #{avatarUrl}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE avatar_url = VALUES(avatar_url), updated_at = NOW()
            """)
    int upsertAvatar(@Param("userId") Long userId,
                     @Param("avatarUrl") String avatarUrl);

    /**
     * 更新资料：昵称、简介、性别（需先有 user_profile 记录，如由 upsertAvatar 创建）
     */
    @Update("""
            UPDATE user_profile
            SET nickname = #{nickname}, bio = #{bio}, gender = #{gender}, updated_at = NOW()
            WHERE user_id = #{userId}
            """)
    int updateProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio,
                      @Param("gender") Integer gender);

    /**
     * 插入或更新完整资料（含 nickname、bio、gender）
     */
    @Insert("""
            INSERT INTO user_profile (user_id, nickname, bio, gender, created_at, updated_at)
            VALUES (#{userId}, #{nickname}, #{bio}, #{gender}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              nickname = #{nickname}, bio = #{bio}, gender = #{gender}, updated_at = NOW()
            """)
    int upsertProfile(@Param("userId") Long userId,
                      @Param("nickname") String nickname,
                      @Param("bio") String bio,
                      @Param("gender") Integer gender);
}

