package com.xiche.auth.controller;

import com.xiche.auth.service.AuthService;
import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 登录端点（BUG-P0-05 修复）：四类终端统一从此处登录换取签名令牌。
 * 请求：{ "username": "13800000001", "password": "test1234", "subjectType": "USER" }
 * subjectType 可选；传入时与账号主体强校验，防止跨端账号串用。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Resource private AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        String username = body.get("username") == null ? null : String.valueOf(body.get("username"));
        String password = body.get("password") == null ? null : String.valueOf(body.get("password"));
        String subjectType = body.get("subjectType") == null ? null : String.valueOf(body.get("subjectType"));
        Map<String, Object> data = authService.login(username, password, subjectType);
        return Result.success(data, "登录成功（" + BusinessConstants.ENV_FLAG + "）");
    }

    /** 探活：返回环境标识，便于客户端确认 TEST-ENV-V1 */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> d = new java.util.HashMap<String, Object>();
        d.put("envFlag", BusinessConstants.ENV_FLAG);
        d.put("authMode", "TOKEN");
        return Result.success(d);
    }
}
