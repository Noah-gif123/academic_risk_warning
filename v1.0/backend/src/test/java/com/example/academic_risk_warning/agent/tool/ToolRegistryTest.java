package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.agent.core.AgentContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具注册表测试：清单文本要能让模型看懂（名字+用途+参数），
 * 同名工具后者覆盖前者，取不存在的工具返回 null 而不是抛异常。
 */
@DisplayName("工具注册表测试")
class ToolRegistryTest {

    private AgentTool tool(String name, String desc, String params) {
        return new AgentTool() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return desc;
            }

            @Override
            public String parameters() {
                return params;
            }

            @Override
            public Map<String, Object> invoke(AgentContext ctx, Map<String, Object> args) {
                return Map.of("ok", true);
            }
        };
    }

    @Test
    @DisplayName("清单文本包含每个工具的名字、用途与参数")
    void catalogShouldDescribeTools() {
        ToolRegistry registry = new ToolRegistry(List.of(
                tool("student_courses", "查询选修课程", "无参数"),
                tool("student_risk_profile", "查询风险画像", "courseId(可选)")));

        String catalog = registry.catalog();

        assertEquals(2, registry.size());
        assertTrue(catalog.contains("student_courses"));
        assertTrue(catalog.contains("查询选修课程"));
        assertTrue(catalog.contains("student_risk_profile"));
        assertTrue(catalog.contains("courseId(可选)"));
        assertEquals(List.of("student_courses", "student_risk_profile"), registry.names());
    }

    @Test
    @DisplayName("同名工具后注册的覆盖先注册的；取不存在的工具返回 null")
    void shouldHandleDuplicateAndMissingTools() {
        AgentTool first = tool("dup", "第一个", "无参数");
        AgentTool second = tool("dup", "第二个", "无参数");
        ToolRegistry registry = new ToolRegistry(List.of(first, second));

        assertEquals(1, registry.size());
        assertSame(second, registry.get("dup"));
        assertNull(registry.get("not_exists"));
        assertNull(registry.get(null));
    }

    @Test
    @DisplayName("工具名带空格也能容错命中（模型偶尔多打空格）")
    void shouldTrimToolName() {
        AgentTool t = tool("student_memory", "查询长期记忆", "courseId(可选)");
        ToolRegistry registry = new ToolRegistry(List.of(t));

        assertSame(t, registry.get(" student_memory "));
    }
}
