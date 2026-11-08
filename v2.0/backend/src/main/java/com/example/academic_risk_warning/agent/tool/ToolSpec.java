package com.example.academic_risk_warning.agent.tool;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** P2-1 ToolSpec：tools.yaml 的 Java 形态。 */
@Data
public class ToolSpec {
    private String name;
    private String description;
    private boolean readOnly;
    private int timeoutMs;
    private List<String> evidenceFields = new ArrayList<>();
    private Map<String, Param> params = new LinkedHashMap<>();

    /** 参数 schema。 */
    @Data
    public static class Param {
        /** integer / string / boolean */
        private String type;
        private boolean required;
        private Integer min;
        private Integer max;
        private Integer minLength;
        private Integer maxLength;
        private List<String> enumValues = new ArrayList<>();
    }
}
