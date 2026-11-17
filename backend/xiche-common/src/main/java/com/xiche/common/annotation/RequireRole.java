package com.xiche.common.annotation;

import java.lang.annotation.*;

/**
 * 功能级 RBAC 权限注解（修复 P0-02）。标注在 Controller 方法上，
 * AuthInterceptor 读取并校验当前登录身份是否具备所需角色。
 * 支持职责分离 SoD：conflictWith 指定与之互斥的角色，同一账号兼具则硬拒绝（P1-23）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
    /** 职责分离：若当前用户同时具备这些角色则拒绝（如审核+财务） */
    String[] conflictWith() default {};
}
