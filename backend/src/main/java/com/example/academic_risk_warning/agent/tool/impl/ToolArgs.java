package com.example.academic_risk_warning.agent.tool.impl;

import java.util.Map;

/**
 * 工具参数解析小工具：模型给的参数可能是数字、字符串，也可能没给，
 * 这里统一做容错解析，避免工具因为一个脏参数直接失败。
 */
public final class ToolArgs {

    private ToolArgs() {}

    public static Long longOf(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && !s.isBlank()) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static int intOf(Map<String, Object> args, String key, int defaultValue) {
        Long v = longOf(args, key);
        return v == null ? defaultValue : v.intValue();
    }

    public static String strOf(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    /** 课程ID：模型没传就用上下文里的课程，仍为空则返回 null（工具按"全部课程"处理） */
    public static Long courseId(Map<String, Object> args, Long contextCourseId) {
        Long id = longOf(args, "courseId");
        return id != null ? id : contextCourseId;
    }
}
