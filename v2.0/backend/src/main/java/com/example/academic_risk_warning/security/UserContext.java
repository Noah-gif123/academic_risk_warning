package com.example.academic_risk_warning.security;

/** 当前请求的登录用户（线程内可见） */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static LoginUser require() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("未登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
