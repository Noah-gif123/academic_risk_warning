package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.agent.core.JsonUtils;
import com.example.academic_risk_warning.entity.StudentProfile;
import com.example.academic_risk_warning.mapper.StudentProfileMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 学生三维学情画像读取服务。
 *
 * <p>画像由 {@code ProfileAgent} 生成并写入 {@code student_profile} 表（按 学生 + 课程 维度），
 * 本服务只负责读取与结构化，不触发 LLM 调用。
 */
@Service
public class StudentProfileService {

    private final StudentProfileMapper studentProfileMapper;

    public StudentProfileService(StudentProfileMapper studentProfileMapper) {
        this.studentProfileMapper = studentProfileMapper;
    }

    /**
     * 读取指定学生（指定课程）的三维画像。
     *
     * <p>courseId 为空表示读取"跨课程综合画像"（course_id IS NULL）；
     * 指定课程没有画像时自动回退到综合画像，并置 {@code fallbackToComprehensive=true}，
     * 前端据此提示"以下为综合画像"。
     */
    public Map<String, Object> getProfile(Long studentId, Long courseId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);

        StudentProfile profile = selectByCourse(studentId, courseId);
        boolean fallback = false;
        if (profile == null && courseId != null) {
            profile = selectByCourse(studentId, null);
            fallback = profile != null;
        }

        if (profile == null) {
            result.put("exists", false);
            result.put("courseId", courseId);
            result.put("courseScoped", courseId != null);
            result.put("fallbackToComprehensive", false);
            result.put("message", courseId != null ? "该生本课程暂无三维画像" : "该生暂无三维画像");
            return result;
        }

        result.put("exists", true);
        result.put("profileId", profile.getId());
        result.put("courseId", profile.getCourseId());
        result.put("courseScoped", profile.getCourseId() != null);
        result.put("fallbackToComprehensive", fallback);
        result.put("updateTime", profile.getUpdateTime());
        result.put("createTime", profile.getCreateTime());
        result.put("knowledgeProfile", parseProfile(profile.getKnowledgeProfile()));
        result.put("habitProfile", parseProfile(profile.getHabitProfile()));
        result.put("goalProfile", parseProfile(profile.getGoalProfile()));
        return result;
    }

    private StudentProfile selectByCourse(Long studentId, Long courseId) {
        return studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>()
                .eq(StudentProfile::getStudentId, studentId)
                .eq(courseId != null, StudentProfile::getCourseId, courseId)
                .isNull(courseId == null, StudentProfile::getCourseId)
                .last("LIMIT 1"));
    }

    /**
     * 画像 JSON 文本 -> 结构化 Map。
     * 解析失败时退化为 {@code {summary: 原文}}，保证前端仍能展示内容而不是空白。
     */
    private Map<String, Object> parseProfile(String json) {
        if (json == null || json.isBlank() || "null".equals(json.trim())) {
            return null;
        }
        Map<String, Object> parsed = JsonUtils.parse(json);
        if (parsed == null || parsed.isEmpty()) {
            return null;
        }
        if (parsed.size() == 1 && parsed.containsKey("raw")) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("summary", String.valueOf(parsed.get("raw")));
            return fallback;
        }
        return parsed;
    }
}
