package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.StudentGoal;
import com.example.academic_risk_warning.mapper.StudentGoalMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * StudentGoalService 单元测试
 * <p>
 * 使用 Mockito 模拟 StudentGoalMapper，验证学习目标的查询、新增、更新逻辑及参数校验。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudentGoalService 单元测试")
class StudentGoalServiceTest {

    @Mock
    private StudentGoalMapper goalMapper;

    private StudentGoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new StudentGoalService(goalMapper);
    }

    // ==================== getByStudentAndCourse ====================

    @Nested
    @DisplayName("getByStudentAndCourse — 按学生+课程查询")
    class GetByStudentAndCourse {

        @Test
        @DisplayName("存在记录时 → 返回 StudentGoal")
        void shouldReturnGoalWhenExists() {
            StudentGoal existing = new StudentGoal();
            existing.setId(1L);
            existing.setStudentId(10L);
            existing.setCourseId(100L);
            existing.setGoalLevel("冲刺");

            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            StudentGoal result = goalService.getByStudentAndCourse(10L, 100L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("冲刺", result.getGoalLevel());
        }

        @Test
        @DisplayName("不存在记录时 → 返回 null")
        void shouldReturnNullWhenNotExists() {
            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            StudentGoal result = goalService.getByStudentAndCourse(10L, 100L);
            assertNull(result);
        }
    }

    // ==================== saveOrUpdate — 新增 ====================

    @Nested
    @DisplayName("saveOrUpdate — 新增目标")
    class SaveOrUpdateCreate {

        @Test
        @DisplayName("无已有记录 → 执行 insert")
        void shouldInsertWhenNoExistingRecord() {
            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            StudentGoal result = goalService.saveOrUpdate(
                    10L, 100L, "稳定", 85, "[]", "努力学习", null);

            verify(goalMapper).insert(any(StudentGoal.class));
            assertNotNull(result);
            assertEquals("稳定", result.getGoalLevel());
            assertEquals(85, result.getTargetScore());
            assertNotNull(result.getCreateTime());
        }

        @Test
        @DisplayName("参数校验：goalLevel 为空 → 抛 IllegalArgumentException")
        void shouldThrowWhenGoalLevelIsEmpty() {
            assertThrows(IllegalArgumentException.class, () ->
                    goalService.saveOrUpdate(10L, 100L, "", 80, null, null, null));
            assertThrows(IllegalArgumentException.class, () ->
                    goalService.saveOrUpdate(10L, 100L, null, 80, null, null, null));
        }

        @Test
        @DisplayName("参数校验：targetScore 负数 → 抛 IllegalArgumentException")
        void shouldThrowWhenScoreIsNegative() {
            assertThrows(IllegalArgumentException.class, () ->
                    goalService.saveOrUpdate(10L, 100L, "稳定", -1, null, null, null));
        }

        @Test
        @DisplayName("参数校验：targetScore > 100 → 抛 IllegalArgumentException")
        void shouldThrowWhenScoreExceeds100() {
            assertThrows(IllegalArgumentException.class, () ->
                    goalService.saveOrUpdate(10L, 100L, "稳定", 101, null, null, null));
        }

        @Test
        @DisplayName("参数校验：targetScore 为 null → 合法(允许不设分数)")
        void shouldAllowNullTargetScore() {
            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertDoesNotThrow(() ->
                    goalService.saveOrUpdate(10L, 100L, "稳定", null, null, null, null));
            verify(goalMapper).insert(any(StudentGoal.class));
        }

        @Test
        @DisplayName("带 deadline 的新增 → deadline 正确设置")
        void shouldSetDeadline() {
            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
            LocalDateTime deadline = LocalDateTime.of(2026, 12, 31, 23, 59);

            StudentGoal result = goalService.saveOrUpdate(10L, 100L, "追赶", 70, null, null, deadline);

            assertEquals(deadline, result.getDeadline());
        }
    }

    // ==================== saveOrUpdate — 更新 ====================

    @Nested
    @DisplayName("saveOrUpdate — 更新已有目标")
    class SaveOrUpdateUpdate {

        @Test
        @DisplayName("已有记录 → 执行 updateById")
        void shouldUpdateWhenRecordExists() {
            StudentGoal existing = new StudentGoal();
            existing.setId(5L);
            existing.setStudentId(10L);
            existing.setCourseId(100L);
            existing.setGoalLevel("保底");
            existing.setTargetScore(60);

            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            StudentGoal result = goalService.saveOrUpdate(10L, 100L, "冲刺", 90, null, "加油", null);

            verify(goalMapper).updateById(any(StudentGoal.class));
            verify(goalMapper, never()).insert(any(StudentGoal.class));
            assertEquals("冲刺", result.getGoalLevel());
            assertEquals(90, result.getTargetScore());
            assertEquals("加油", result.getNote());
            assertNotNull(result.getUpdateTime());
        }

        @Test
        @DisplayName("更新保留原有 id")
        void shouldKeepOriginalIdOnUpdate() {
            StudentGoal existing = new StudentGoal();
            existing.setId(5L);

            when(goalMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

            StudentGoal result = goalService.saveOrUpdate(10L, 100L, "稳定", 80, null, null, null);
            assertEquals(5L, result.getId());
        }
    }
}
