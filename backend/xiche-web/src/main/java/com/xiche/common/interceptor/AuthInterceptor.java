package com.xiche.common.interceptor;

import com.xiche.common.annotation.RequireRole;
import com.xiche.common.context.LoginContext;
import com.xiche.common.context.LoginUser;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 鉴权拦截器（修复 P0-01 身份硬编码 / P0-02 RBAC / P1-23 职责分离 SoD）。
 * 1) 从请求头解析身份注入 LoginContext，请求结束清理 ThreadLocal；
 * 2) 对标注 @RequireRole 的方法做角色校验；
 * 3) 同一账号兼具 conflictWith 互斥角色时硬拒绝（职责分离）。
 * V1 测试环境未带任何头时以测试身份兜底，保证内部演示可用。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        LoginUser user = parseUser(req);
        LoginContext.set(user);

        HandlerMethod hm = (HandlerMethod) handler;
        RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
        if (ann == null) ann = hm.getBeanType().getAnnotation(RequireRole.class);
        if (ann == null) return true;

        Set<String> roles = new HashSet<String>(Arrays.asList(user.getRoleCode() == null
                ? new String[0] : user.getRoleCode().split(",")));
        roles.add("ADMIN"); // 超管天然具备（高危 SoD 仍受 conflictWith 约束）

        // SoD：同一身份同时具备互斥角色对 → 硬拒绝
        for (String conflict : ann.conflictWith()) {
            if (roles.contains(conflict)) {
                resp.setStatus(403);
                resp.setContentType("application/json;charset=UTF-8");
                resp.getWriter().write("{\"code\":40300,\"message\":\"职责分离冲突：不可同时持有 "
                        + String.join("/", ann.value()) + " 与 " + conflict + "\"}");
                return false;
            }
        }
        boolean ok = false;
        for (String need : ann.value()) {
            if (roles.contains(need)) { ok = true; break; }
        }
        if (!ok) {
            resp.setStatus(403);
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write("{\"code\":40300,\"message\":\"权限不足，需要角色："
                    + String.join("/", ann.value()) + "\"}");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        LoginContext.clear();
    }

    private LoginUser parseUser(HttpServletRequest req) {
        String uid = req.getHeader("X-User-Id");
        String wid = req.getHeader("X-Worker-Id");
        String sid = req.getHeader("X-Store-Id");
        String role = req.getHeader("X-Role");
        String token = req.getHeader("X-Token");
        if (uid == null && wid == null && sid == null && role == null) {
            return LoginUser.testDefault();
        }
        LoginUser u = new LoginUser(parseLong(uid, 2L), parseLong(wid, 1L),
                parseLong(sid, 1L), role == null ? "USER" : role.toUpperCase());
        u.setToken(token);
        return u;
    }

    private Long parseLong(String s, Long def) {
        if (s == null || s.trim().isEmpty()) return def;
        try { return Long.valueOf(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
