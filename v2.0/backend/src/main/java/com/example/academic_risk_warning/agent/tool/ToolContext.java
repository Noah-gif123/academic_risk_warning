package com.example.academic_risk_warning.agent.tool;

import com.example.academic_risk_warning.security.LoginUser;

/** 工具执行上下文：当前用户（决定数据范围）+ 学期。 */
public record ToolContext(LoginUser user, Long termId) {
}
