package com.xiche.common.util;

/**
 * 字段级脱敏工具（修复 P1-24）。身份证、手机号等敏感字段对外输出前统一掩码。
 * 只有具备 sensitive:full 权限的角色可见明文，由 AuthInterceptor + 本工具配合。
 */
public final class SensitiveUtil {
    private SensitiveUtil() {}

    /** 手机号：138****5678 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证：310***********1234 */
    public static String maskIdCard(String id) {
        if (id == null || id.length() < 8) return id;
        return id.substring(0, 3) + "***********" + id.substring(id.length() - 4);
    }

    /** 依据全量权限开关决定明文或掩码 */
    public static String phone(String phone, boolean fullAccess) {
        return fullAccess ? phone : maskPhone(phone);
    }
    public static String idCard(String id, boolean fullAccess) {
        return fullAccess ? id : maskIdCard(id);
    }
}
