package com.xiche.common.context;

/**
 * 登录身份 value object。V1 测试环境通过请求头 X-User-Id / X-Worker-Id / X-Store-Id / X-Role 传入。
 */
public class LoginUser {
    private Long userId;
    private Long workerId;
    private Long storeId;
    private String roleCode;
    private String token;

    public LoginUser() {}

    public LoginUser(Long userId, Long workerId, Long storeId, String roleCode) {
        this.userId = userId; this.workerId = workerId; this.storeId = storeId; this.roleCode = roleCode;
    }

    /** V1 测试默认身份（owners: user=2, worker=1, store=1） */
    public static LoginUser testDefault() {
        return new LoginUser(2L, 1L, 1L, "USER");
    }

    public boolean isAdmin() { return roleCode != null && roleCode.toUpperCase().contains("ADMIN"); }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getWorkerId() { return workerId; }
    public void setWorkerId(Long workerId) { this.workerId = workerId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
