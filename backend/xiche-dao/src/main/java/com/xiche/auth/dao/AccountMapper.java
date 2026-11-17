package com.xiche.auth.dao;

import com.xiche.auth.entity.Account;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 统一登录账号 DAO（BUG-P0-05）。 */
public interface AccountMapper {
    Account selectByUsername(@Param("username") String username);

    /** 从 t_sys_user_role + t_sys_role 按主体加载角色码，避免信任请求头自报角色 */
    List<String> selectRoleCodes(@Param("subjectId") Long subjectId, @Param("subjectType") String subjectType);
}
