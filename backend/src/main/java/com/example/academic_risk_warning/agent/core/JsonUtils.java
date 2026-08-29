package com.example.academic_risk_warning.agent.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;

/**
 * LLM JSON 解析工具类 — 六大智能体共享，消除 350+ 行重复代码
 *
 * 替代各 Agent 中手写的 parseSimpleJSON / extractBalanced / tryParse 方法，
 * 使用 Jackson 安全解析，正确处理嵌套对象和转义字符。
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    /**
     * 解析 LLM 输出的 JSON 文本为 Map
     *
     * @param text LLM 原始输出（可能包含非 JSON 的前后缀）
     * @return 解析成功的 Map；解析失败返回 {"raw": text}
     */
    public static Map<String, Object> parse(String text) {
        if (text == null || text.isBlank()) return Collections.emptyMap();
        try {
            String trimmed = text.trim();
            int s = trimmed.indexOf('{');
            int e = trimmed.lastIndexOf('}');
            if (s < 0 || e < 0) {
                return Map.of("raw", text);
            }
            String json = trimmed.substring(s, e + 1);
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Map.of("raw", text);
        }
    }

    /**
     * 将对象序列化为 JSON 字符串（用于 DB 持久化，替代原来的 toString()）
     */
    public static String toJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
