package com.xiche.auth.service;

import com.xiche.auth.dao.AccountMapper;
import com.xiche.auth.entity.Account;
import com.xiche.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录认证（BUG-P0-05 修复）：手机号+密码登录，服务端从库加载身份与角色，签发签名令牌。
 * 身份不再信任 X-Role/X-User-Id 等客户端请求头。
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Resource private AccountMapper accountMapper;
    @Resource private TokenService tokenService;

    /** 登录，返回 token 与身份摘要 */
    public Map<String, Object> login(String username, String password, String subjectTypeHint) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new BizException(40000, "用户名与密码必填");
        }
        Account acc = accountMapper.selectByUsername(username.trim());
        if (acc == null) throw new BizException(40001, "账号或密码错误");
        if (acc.getStatus() != null && acc.getStatus() != 1) throw new BizException(40300, "账号已停用");
        // 若调用方声明了终端类型，必须与账号主体一致，防止跨端账号串用
        if (subjectTypeHint != null && !subjectTypeHint.trim().isEmpty()
                && !subjectTypeHint.equalsIgnoreCase(acc.getSubjectType())) {
            throw new BizException(40001, "账号或密码错误");
        }
        String expect = sha256(password, acc.getSalt());
        if (!expect.equalsIgnoreCase(acc.getPasswordHash())) throw new BizException(40001, "账号或密码错误");

        String rolesCsv = resolveRoles(acc);
        String token = tokenService.issue(acc.getSubjectType(), acc.getSubjectId(), rolesCsv);

        Map<String, Object> data = new java.util.HashMap<String, Object>();
        data.put("token", token);
        data.put("subjectType", acc.getSubjectType());
        data.put("subjectId", acc.getSubjectId());
        data.put("roles", rolesCsv);
        data.put("expireIn", TokenService.TTL_MS / 1000);
        log.info("[TEST-ENV-V1] 登录成功 subject={}:{} roles={}", acc.getSubjectType(), acc.getSubjectId(), rolesCsv);
        return data;
    }

    /** 角色集合：终端默认角色 + 按主体从 t_sys_user_role 库加载（用于运行时校验/审计） */
    public Set<String> loadRoles(String subjectType, Long subjectId) {
        Set<String> roles = new LinkedHashSet<String>();
        String def = defaultRole(subjectType);
        if (def != null) roles.add(def);
        if (subjectId != null) {
            List<String> dbRoles = accountMapper.selectRoleCodes(subjectId, subjectType);
            if (dbRoles != null) roles.addAll(dbRoles);
        }
        return roles;
    }

    private String resolveRoles(Account acc) {
        Set<String> roles = new LinkedHashSet<String>();
        String def = defaultRole(acc.getSubjectType());
        if (def != null) roles.add(def);
        if (acc.getRoles() != null && !acc.getRoles().trim().isEmpty()) {
            for (String r : acc.getRoles().split(",")) if (!r.trim().isEmpty()) roles.add(r.trim().toUpperCase());
        }
        List<String> dbRoles = accountMapper.selectRoleCodes(acc.getSubjectId(), acc.getSubjectType());
        if (dbRoles != null) for (String r : dbRoles) roles.add(r.toUpperCase());
        // ADMIN 主体默认即超管；但 SoD 仍由 @RequireRole.conflictWith 约束
        if ("ADMIN".equalsIgnoreCase(acc.getSubjectType())) roles.add("ADMIN");
        return String.join(",", roles);
    }

    private String defaultRole(String subjectType) {
        if (subjectType == null) return null;
        if ("USER".equalsIgnoreCase(subjectType)) return "USER";
        if ("WORKER".equalsIgnoreCase(subjectType)) return "WORKER";
        if ("STORE".equalsIgnoreCase(subjectType)) return "STORE";
        if ("ADMIN".equalsIgnoreCase(subjectType)) return "ADMIN";
        return null;
    }

    private static String sha256(String raw, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt == null ? "" : salt).getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
