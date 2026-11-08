package com.example.academic_risk_warning.agent.tool;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** 工具统一返回：data + evidence（供 P2-6 台账入账）+ 摘要/耗时。 */
@Data
public class ToolResult {
    private boolean ok;
    private Map<String, Object> data = new LinkedHashMap<>();
    private Map<String, Object> evidence = new LinkedHashMap<>();
    private String errorCode;
    private String message;
    private long elapsedMs;
    private String digest;
    private int bytes;

    public static ToolResult success(Map<String, Object> data, Map<String, Object> evidence) {
        ToolResult r = new ToolResult();
        r.ok = true;
        r.data = data == null ? new LinkedHashMap<>() : data;
        r.evidence = evidence == null ? new LinkedHashMap<>() : evidence;
        return r;
    }

    public static ToolResult fail(String code, String message) {
        ToolResult r = new ToolResult();
        r.ok = false;
        r.errorCode = code;
        r.message = message;
        return r;
    }
}
