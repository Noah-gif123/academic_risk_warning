package com.example.academic_risk_warning.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 角色校验注解：标在 Controller 类或方法上，允许访问的角色列表 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /** 允许的角色：STUDENT / TEACHER / COUNSELOR / ADMIN */
    String[] value();
}
