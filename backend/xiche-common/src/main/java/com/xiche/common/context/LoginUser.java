package com.xiche.common.context;

import java.util.HashSet;
import java.util.Set;

/**
 * 登录身份 value object（BUG-P0-05 修复）。
 * 权威来源为服务端签名令牌：subjectType/subjectId/roles 均由 token 载荷注入，
 * 不再信任 X-Role / X-User-Id 等客户端请求头。
 */
public class LoginUser {
    private Long userId;
    private Long workerId;
    private Long storeId;
    private String roleCode;      // 主角色码（兼容旧逻辑）
    private String subjectType;   // USER / WORKER / STORE / ADMIN
    private Set<String> roles;    // 完整角色集合（来自 token 的库加载快照）
    private String token;
    private boolean authenticated; // 是否通过 token 认证

    public LoginUser() {}

    public LoginUser(Long userId, Long workerId, Long storeId, String roleCode) {
        this.userId = userId; this.workerId = workerId; this.storeId = storeId; this.roleCode = roleCode;
        this.subjectType = roleCode;
        this.roles = new HashSet<String>();
        if (roleCode != null) {
            for (String r : roleCode.split(",")) if (!r.trim().isEmpty()) this.roles.add(r.trim().toUpperCase());
        }
    }

    /** V1 演示默认身份：仅 auth.demo-mode=true 且未带 token 时使用，固定普通车主，绝不授予 ADMIN */
    public static LoginUser testDefault() {
        return new LoginUser(2L, 1L, 1L, "USER");
    }

    /** isAdmin 只认 roles 集合中的 ADMIN，避免 roleCode 字符串包含匹配带来的伪造空间 */
    public boolean isAdmin() { return roles != null && roles.contains("ADMIN"); }

    public boolean hasRole(String code) {
        return roles != null && code != null && roles.contains(code.toUpperCase());
    }

    /** 由 token 载荷构造可信身份 */
    public static LoginUser fromToken(String subjectType, Long subjectId, String rolesCsv) {
        LoginUser u = new LoginUser();
        u.subjectType = subjectType;
        u.roles = new HashSet<String>();
        if (rolesCsv != null && !rolesCsv.trim().isEmpty()) {
            for (String r : rolesCsv.split(",")) if (!r.trim().isEmpty()) u.roles.add(r.trim().toUpperCase());
        }
        u.roleCode = rolesCsv;
        u.authenticated = true;
        if ("USER".equalsIgnoreCase(subjectType)) u.userId = subjectId;
        else if ("WORKER".equalsIgnoreCase(subjectType)) u.workerId = subjectId;
        else if ("STORE".equalsIgnoreCase(subjectType)) u.storeId = subjectId;
        else if ("ADMIN".equalsIgnoreCase(subjectType)) {
            u.userId = subjectId; u.workerId = subjectId; u.storeId = subjectId;
        }
        return u;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
}
