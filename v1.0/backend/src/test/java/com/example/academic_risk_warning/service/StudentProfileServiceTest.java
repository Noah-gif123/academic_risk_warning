package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.entity.StudentProfile;
import com.example.academic_risk_warning.mapper.StudentProfileMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 三维学情画像读取服务测试：课程级命中、回退综合画像、无画像、JSON 解析兜底。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("三维学情画像读取服务测试")
class StudentProfileServiceTest {

    private static final Long STUDENT_ID = 9L;
    private static final Long COURSE_ID = 2L;

    @Mock private StudentProfileMapper studentProfileMapper;

    private StudentProfileService service() {
        return new StudentProfileService(studentProfileMapper);
    }

    @Test
    @DisplayName("课程级画像命中：不回退，返回三个维度")
    void shouldReturnCourseScopedProfile() {
        when(studentProfileMapper.selectOne(any())).thenReturn(profile(COURSE_ID,
                "{\"level\":\"良好\",\"summary\":\"知识点掌握较好\",\"masteryPercent\":78}",
                "{\"level\":\"规律型\",\"summary\":\"作息规律\",\"traits\":[\"按时完成\"]}",
                "{\"level\":\"稳定\",\"summary\":\"目标合理\",\"targetScore\":85,\"feasibility\":\"高\"}"));

        Map<String, Object> result = service().getProfile(STUDENT_ID, COURSE_ID);

        assertTrue((Boolean) result.get("exists"));
        assertEquals(COURSE_ID, result.get("courseId"));
        assertEquals(Boolean.TRUE, result.get("courseScoped"));
        assertEquals(Boolean.FALSE, result.get("fallbackToComprehensive"));
        assertNotNull(result.get("updateTime"));

        @SuppressWarnings("unchecked")
        Map<String, Object> knowledge = (Map<String, Object>) result.get("knowledgeProfile");
        assertEquals("良好", knowledge.get("level"));
        assertEquals(78, knowledge.get("masteryPercent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> habit = (Map<String, Object>) result.get("habitProfile");
        assertEquals("规律型", habit.get("level"));
        assertEquals(List.of("按时完成"), habit.get("traits"));

        @SuppressWarnings("unchecked")
        Map<String, Object> goal = (Map<String, Object>) result.get("goalProfile");
        assertEquals(85, goal.get("targetScore"));
    }

    @Test
    @DisplayName("本课程无画像时回退到综合画像并标记 fallback")
    void shouldFallbackToComprehensiveProfile() {
        when(studentProfileMapper.selectOne(any()))
                .thenReturn(null)
                .thenReturn(profile(null, "{\"level\":\"一般\"}", null, null));

        Map<String, Object> result = service().getProfile(STUDENT_ID, COURSE_ID);

        assertTrue((Boolean) result.get("exists"));
        assertEquals(Boolean.TRUE, result.get("fallbackToComprehensive"));
        assertEquals(Boolean.FALSE, result.get("courseScoped"));
        assertNull(result.get("courseId"));
        assertNotNull(result.get("knowledgeProfile"));
        assertNull(result.get("habitProfile"));
    }

    @Test
    @DisplayName("完全没有画像时 exists=false 且给出提示")
    void shouldReportMissingProfile() {
        when(studentProfileMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service().getProfile(STUDENT_ID, COURSE_ID);

        assertFalse((Boolean) result.get("exists"));
        assertEquals(Boolean.TRUE, result.get("courseScoped"));
        assertEquals("该生本课程暂无三维画像", result.get("message"));
    }

    @Test
    @DisplayName("courseId 为空时只查综合画像")
    void shouldQueryComprehensiveProfileWhenCourseIsNull() {
        when(studentProfileMapper.selectOne(any())).thenReturn(profile(null, "{\"level\":\"优秀\"}", null, null));

        Map<String, Object> result = service().getProfile(STUDENT_ID, null);

        assertTrue((Boolean) result.get("exists"));
        assertNull(result.get("message"));
        assertEquals(Boolean.FALSE, result.get("courseScoped"));
    }

    @Test
    @DisplayName("画像 JSON 损坏时退化为 summary 文本，不返回空白")
    void shouldDegradeWhenJsonBroken() {
        when(studentProfileMapper.selectOne(any())).thenReturn(profile(COURSE_ID, "这不是合法JSON", null, null));

        Map<String, Object> result = service().getProfile(STUDENT_ID, COURSE_ID);

        @SuppressWarnings("unchecked")
        Map<String, Object> knowledge = (Map<String, Object>) result.get("knowledgeProfile");
        assertEquals("这不是合法JSON", knowledge.get("summary"));
    }

    private StudentProfile profile(Long courseId, String knowledge, String habit, String goal) {
        StudentProfile profile = new StudentProfile();
        profile.setId(1L);
        profile.setStudentId(STUDENT_ID);
        profile.setCourseId(courseId);
        profile.setKnowledgeProfile(knowledge);
        profile.setHabitProfile(habit);
        profile.setGoalProfile(goal);
        profile.setUpdateTime(LocalDateTime.now());
        profile.setCreateTime(LocalDateTime.now());
        return profile;
    }
}
