package com.xmreader.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSessionEntity> {

    @Select("SELECT * FROM user_sessions WHERE refresh_token_hash = #{hash} LIMIT 1")
    UserSessionEntity findByRefreshTokenHash(@Param("hash") byte[] hash);

    @Update("""
            UPDATE user_sessions
            SET refresh_token_hash = #{newHash}, last_used_at = #{now}
            WHERE id = #{id}
              AND refresh_token_hash = #{oldHash}
              AND revoked_at IS NULL
              AND expires_at > #{now}
            """)
    int rotate(
            @Param("id") long id,
            @Param("oldHash") byte[] oldHash,
            @Param("newHash") byte[] newHash,
            @Param("now") LocalDateTime now);

    @Update("""
            UPDATE user_sessions
            SET revoked_at = #{now}
            WHERE refresh_token_hash = #{hash} AND revoked_at IS NULL
            """)
    int revokeByRefreshTokenHash(@Param("hash") byte[] hash, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE user_sessions
            SET revoked_at = #{now}
            WHERE user_id = #{userId} AND revoked_at IS NULL
            """)
    int revokeAllForUser(@Param("userId") long userId, @Param("now") LocalDateTime now);
}
