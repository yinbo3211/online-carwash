package com.xiche.common.context;

/**
 * 登录态上下文（修复 P0-01：核心接口身份硬编码）。
 * 通过 ThreadLocal 保存当前请求身份，由 AuthInterceptor 从请求头解析并注入。
 * V1 测试环境允许默认身份兜底（TEST-ENV-V1），但身份来源唯一收敛到本类，
 * 禁止 Controller 再出现 setUserId(2L) 之类硬编码。
 */
public final class LoginContext {
    private LoginContext() {}

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<LoginUser>();

    /** 当前登录用户；未设置时返回测试身份（仅 V1 内部测试） */
    public static LoginUser current() {
        LoginUser u = HOLDER.get();
        if (u == null) {
            u = LoginUser.testDefault();
        }
        return u;
    }

    public static void set(LoginUser user) { HOLDER.set(user); }

    public static void clear() { HOLDER.remove(); }

    /** 便捷方法 */
    public static Long userId() { return current().getUserId(); }
    public static Long workerId() { return current().getWorkerId(); }
    public static Long storeId() { return current().getStoreId(); }
    public static String role() { return current().getRoleCode(); }
    public static boolean isAdmin() { return current().isAdmin(); }
}
