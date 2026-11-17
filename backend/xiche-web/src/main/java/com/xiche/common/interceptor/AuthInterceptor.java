package com.xiche.common.interceptor;

import com.xiche.auth.service.AuditService;
import com.xiche.auth.service.TokenService;
import com.xiche.common.annotation.RequireRole;
import com.xiche.common.context.LoginContext;
import com.xiche.common.context.LoginUser;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * 鉴权拦截器（BUG-P0-05 重写）：
 * 1) 唯一可信身份源为服务端签名令牌 X-Token；X-Role/X-User-Id 等请求头一律不再信任；
 * 2) 删除 roles.add("ADMIN")，角色来自 token 载荷（登录时从库加载并签名）；
 * 3) @RequireRole 做功能级 RBAC + 职责分离 SoD 校验，需认证身份；
 * 4) 越权/无凭证访问记录审计并告警；
 * 5) V1 演示开关 xiche.auth.demo-mode=true 时，未带 token 允许固定普通身份兜底（绝不授予 ADMIN）。
 */
public class AuthInterceptor implements HandlerInterceptor {

    /** V1 演示开关，由 spring-mvc.xml 注入；默认 false（必须登录） */
    private boolean demoMode = false;

    public void setDemoMode(boolean demoMode) { this.demoMode = demoMode; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        String path = req.getRequestURI();
        // 白名单：登录/探活/首页公开浏览，不需要 token
        if (path.contains("/api/auth/") || path.endsWith("/api/user/home")) {
            LoginContext.set(LoginUser.testDefault());
            return true;
        }

        HandlerMethod hm = (HandlerMethod) handler;
        RequireRole ann = hm.getMethodAnnotation(RequireRole.class);
        if (ann == null) ann = hm.getBeanType().getAnnotation(RequireRole.class);

        WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(req.getServletContext());
        TokenService tokenService = ctx == null ? null : ctx.getBean(TokenService.class);
        AuditService auditService = ctx == null ? null : ctx.getBean(AuditService.class);

        String token = firstNonBlank(req.getHeader("X-Token"), req.getHeader("Authorization"));
        LoginUser user = null;
        if (token != null && tokenService != null) {
            TokenService.TokenPayload p = tokenService.verify(stripBearer(token));
            if (p != null) user = LoginUser.fromToken(p.getSubjectType(), p.getSubjectId(), p.getRolesCsv());
        }

        if (user == null) {
            // 无有效 token：RBAC 端点一律拒绝；演示模式非 RBAC 端点用固定普通身份
            if (ann != null) {
                deny(resp, 40100, "需要登录后访问", req, auditService, "AUTH_NO_TOKEN");
                return false;
            }
            if (!demoMode) {
                deny(resp, 40100, "需要登录后访问", req, auditService, "AUTH_NO_TOKEN");
                return false;
            }
            user = LoginUser.testDefault(); // 固定普通身份，无 ADMIN
        }
        LoginContext.set(user);
        user.setToken(token);

        if (ann == null) return true;

        Set<String> roles = user.getRoles();
        // SoD：同一身份同时具备互斥角色对 → 硬拒绝
        for (String conflict : ann.conflictWith()) {
            if (roles.contains(conflict)) {
                deny(resp, 40300, "职责分离冲突：不可同时持有 " + String.join("/", ann.value())
                        + " 与 " + conflict, req, auditService, "AUTH_SOD_DENIED");
                return false;
            }
        }
        boolean ok = false;
        for (String need : ann.value()) if (roles.contains(need)) { ok = true; break; }
        if (!ok) {
            deny(resp, 40300, "权限不足，需要角色：" + String.join("/", ann.value()),
                    req, auditService, "AUTH_ROLE_DENIED");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp, Object handler, Exception ex) {
        LoginContext.clear();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        return b;
    }

    private String stripBearer(String t) {
        if (t != null && t.regionMatches(true, 0, "Bearer ", 0, 7)) return t.substring(7).trim();
        return t;
    }

    private void deny(HttpServletResponse resp, int code, String msg, HttpServletRequest req,
                      AuditService auditService, String op) throws IOException {
        if (auditService != null) {
            auditService.record("SYSTEM", null, op, "API", req.getRequestURI(),
                    "method=" + req.getMethod() + " ip=" + req.getRemoteAddr(), req.getRemoteAddr());
        }
        resp.setStatus(code == 40100 ? 401 : 403);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":" + code + ",\"message\":\"" + msg + "\",\"success\":false}");
    }
}
