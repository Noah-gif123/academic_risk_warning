package com.example.academic_risk_warning.agent.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * P2-1 ToolSpec 加载与校验。
 *
 * <p>启动时读取 classpath:ai/tools.yaml，校验必填字段/类型/枚举/范围/唯一性；
 * 通过后可列出工具、按名查找、生成 OpenAPI 3 文档。</p>
 */
@Component
public class ToolSpecLoader {

    private final Map<String, ToolSpec> specs = new LinkedHashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void load() {
        try (InputStream in = new ClassPathResource("ai/tools.yaml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null || root.get("version") == null || !(root.get("tools") instanceof List)) {
                throw new IllegalStateException("tools.yaml 缺少 version/tools");
            }
            List<Map<String, Object>> tools = (List<Map<String, Object>>) root.get("tools");
            for (Map<String, Object> raw : tools) {
                ToolSpec spec = parse(raw);
                if (specs.containsKey(spec.getName())) {
                    throw new IllegalStateException("工具名重复：" + spec.getName());
                }
                specs.put(spec.getName(), spec);
            }
            if (specs.size() != 9) {
                throw new IllegalStateException("工具数量应为 9，实际 " + specs.size());
            }
        } catch (Exception e) {
            throw new IllegalStateException("加载 ai/tools.yaml 失败：" + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private ToolSpec parse(Map<String, Object> raw) {
        ToolSpec spec = new ToolSpec();
        spec.setName(required(raw, "name").toString());
        spec.setDescription(required(raw, "description").toString());
        spec.setReadOnly(Boolean.TRUE.equals(raw.get("read_only")));
        spec.setTimeoutMs(((Number) required(raw, "timeout_ms")).intValue());
        if (spec.getTimeoutMs() <= 0) {
            throw new IllegalStateException(spec.getName() + " timeout_ms 必须 >0");
        }
        Object ev = raw.get("evidence_fields");
        if (!(ev instanceof List) || ((List<Object>) ev).isEmpty()) {
            throw new IllegalStateException(spec.getName() + " 缺少 evidence_fields");
        }
        ev = ev instanceof List ? ev : List.of();
        ((List<Object>) ev).forEach((x) -> spec.getEvidenceFields().add(String.valueOf(x)));
        Map<String, Object> params = (Map<String, Object>) raw.getOrDefault("params", Map.of());
        for (Map.Entry<String, Object> e : params.entrySet()) {
            Map<String, Object> p = (Map<String, Object>) e.getValue();
            ToolSpec.Param param = new ToolSpec.Param();
            param.setType(String.valueOf(required(p, "type")));
            if (!List.of("integer", "string", "boolean").contains(param.getType())) {
                throw new IllegalStateException(spec.getName() + "." + e.getKey() + " type 非法：" + param.getType());
            }
            param.setRequired(Boolean.TRUE.equals(p.get("required")));
            param.setMin(num(p.get("min")));
            param.setMax(num(p.get("max")));
            param.setMinLength(num(p.get("minLength")));
            param.setMaxLength(num(p.get("maxLength")));
            Object en = p.get("enum");
            if (en instanceof List) {
                ((List<Object>) en).forEach((x) -> param.getEnumValues().add(String.valueOf(x)));
            }
            spec.getParams().put(e.getKey(), param);
        }
        return spec;
    }

    private Integer num(Object v) {
        return v == null ? null : ((Number) v).intValue();
    }

    private Object required(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            throw new IllegalStateException("tools.yaml 缺少字段：" + key);
        }
        return v;
    }

    public List<ToolSpec> list() {
        return new ArrayList<>(specs.values());
    }

    public ToolSpec get(String name) {
        return specs.get(name);
    }

    public int count() {
        return specs.size();
    }

    /** 生成 OpenAPI 3.0.3（P2-1 验收：可生成 OpenAPI）。 */
    public Map<String, Object> openApi() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.0.3");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", "v3.0 Agent Tools");
        info.put("version", "1.0.0");
        root.put("info", info);
        Map<String, Object> paths = new LinkedHashMap<>();
        for (ToolSpec spec : specs.values()) {
            Map<String, Object> op = new LinkedHashMap<>();
            op.put("operationId", spec.getName());
            op.put("summary", spec.getDescription());
            Map<String, Object> props = new LinkedHashMap<>();
            List<String> required = new ArrayList<>();
            for (Map.Entry<String, ToolSpec.Param> e : spec.getParams().entrySet()) {
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", e.getValue().getType());
                if (e.getValue().getMin() != null) schema.put("minimum", e.getValue().getMin());
                if (e.getValue().getMax() != null) schema.put("maximum", e.getValue().getMax());
                if (!e.getValue().getEnumValues().isEmpty()) schema.put("enum", e.getValue().getEnumValues());
                props.put(e.getKey(), schema);
                if (e.getValue().isRequired()) required.add(e.getKey());
            }
            Map<String, Object> bodySchema = new LinkedHashMap<>();
            bodySchema.put("type", "object");
            bodySchema.put("properties", props);
            if (!required.isEmpty()) bodySchema.put("required", required);
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("required", true);
            requestBody.put("content", Map.of("application/json", Map.of("schema", bodySchema)));
            op.put("requestBody", requestBody);
            op.put("responses", Map.of("200", Map.of("description", "ToolResult")));
            paths.put("/api/agent/tools/" + spec.getName(), Map.of("post", op));
        }
        root.put("paths", paths);
        return root;
    }
}
