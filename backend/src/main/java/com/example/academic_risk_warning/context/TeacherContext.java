package com.example.academic_risk_warning.context;

/**
 * 当前登录教师上下文（请求线程内有效）
 */
public final class TeacherContext {

    private static final ThreadLocal<Long> TEACHER_ID = new ThreadLocal<>();

    private TeacherContext() {
    }

    public static void setTeacherId(Long teacherId) {
        TEACHER_ID.set(teacherId);
    }

    public static Long getTeacherId() {
        return TEACHER_ID.get();
    }

    public static void clear() {
        TEACHER_ID.remove();
    }
}
