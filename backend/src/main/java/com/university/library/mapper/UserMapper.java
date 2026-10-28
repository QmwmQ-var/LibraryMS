package com.university.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.university.library.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper。
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 登录时锁定账号行，串行化同一账号的并发失败计数，避免绕过 3 次锁定规则。
     */
    @Select("SELECT * FROM `user` WHERE username = #{username} FOR UPDATE")
    User selectByUsernameForUpdate(@Param("username") String username);
}
