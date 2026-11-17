package com.xiche.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 服务端签名令牌（BUG-P0-05 修复）：零第三方依赖，JDK 自带 HMAC-SHA256。
 * Payload 紧凑格式：v1|subjectType|subjectId|rolesCsv|expMillis
 * 角色码仅含字母与逗号，不含分隔符，安全。令牌格式：base64url(payload).base64url(sig)
 * 签名密钥来自配置，生产必须替换；校验恒定时间比较，失败即拒绝。
 */
@Service
public class TokenService {
    /** 令牌有效期：12 小时（毫秒） */
    public static final long TTL_MS = 12L * 60 * 60 * 1000;

    @Value("${xiche.auth.secret:xiche-test-secret-change-me-2026}")
    private String secret;

    private static final String SEP = "|";

    /** 签发令牌 */
    public String issue(String subjectType, Long subjectId, String rolesCsv) {
        long exp = System.currentTimeMillis() + TTL_MS;
        String safeRoles = rolesCsv == null || rolesCsv.isEmpty() ? "-" : rolesCsv;
        String payload = "v1" + SEP + subjectType + SEP + subjectId + SEP + safeRoles + SEP + exp;
        String body = b64url(payload.getBytes(StandardCharsets.UTF_8));
        String sig = b64url(hmac(payload));
        return body + "." + sig;
    }

    /** 解析并校验令牌；无效/过期返回 null */
    public TokenPayload verify(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return null;
        String body = token.substring(0, dot);
        String sig = token.substring(dot + 1);
        byte[] payloadBytes;
        byte[] sigBytes;
        try {
            payloadBytes = Base64.getUrlDecoder().decode(body);
            sigBytes = Base64.getUrlDecoder().decode(sig);
        } catch (Exception e) {
            return null;
        }
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        byte[] expect = hmac(payload);
        if (!constantEquals(expect, sigBytes)) return null; // 签名不符
        String[] parts = payload.split("\\" + SEP, -1);
        if (parts.length != 5 || !"v1".equals(parts[0])) return null;
        try {
            long exp = Long.parseLong(parts[4]);
            if (System.currentTimeMillis() > exp) return null; // 过期
            Long sid = Long.valueOf(parts[2]);
            String roles = "-".equals(parts[3]) ? "" : parts[3];
            return new TokenPayload(parts[1], sid, roles, exp);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 初始化失败", e);
        }
    }

    private static String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 恒定时间比较，防时序攻击 */
    private static boolean constantEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int r = 0;
        for (int i = 0; i < a.length; i++) r |= a[i] ^ b[i];
        return r == 0;
    }

    /** 令牌载荷 */
    public static final class TokenPayload {
        private final String subjectType;
        private final Long subjectId;
        private final String rolesCsv;
        private final long expireAt;

        public TokenPayload(String subjectType, Long subjectId, String rolesCsv, long expireAt) {
            this.subjectType = subjectType;
            this.subjectId = subjectId;
            this.rolesCsv = rolesCsv;
            this.expireAt = expireAt;
        }
        public String getSubjectType() { return subjectType; }
        public Long getSubjectId() { return subjectId; }
        public String getRolesCsv() { return rolesCsv; }
        public long getExpireAt() { return expireAt; }
    }
}
