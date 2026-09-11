package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.StudentGoal;
import com.example.academic_risk_warning.mapper.StudentGoalMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学生学习目标服务
 */
@Service
public class StudentGoalService {

    private final StudentGoalMapper goalMapper;

    public StudentGoalService(StudentGoalMapper goalMapper) {
        this.goalMapper = goalMapper;
    }

    /**
     * 获取学生某课程的学习目标
     */
    public StudentGoal getByStudentAndCourse(Long studentId, Long courseId) {
        return goalMapper.selectOne(new LambdaQueryWrapper<StudentGoal>()
                .eq(StudentGoal::getStudentId, studentId)
                .eq(StudentGoal::getCourseId, courseId));
    }

    /**
     * 获取学生的全部课程学习目标（未指定课程时供画像生成参考）
     */
    public List<StudentGoal> listByStudent(Long studentId) {
        return goalMapper.selectList(new LambdaQueryWrapper<StudentGoal>()
                .eq(StudentGoal::getStudentId, studentId)
                .orderByDesc(StudentGoal::getUpdateTime));
    }

    /**
     * 保存或更新学习目标（一个学生一门课只保留一条）
     */
    public StudentGoal saveOrUpdate(Long studentId, Long courseId,
                                     String goalLevel, Integer targetScore,
                                     String milestones, String note,
                                     LocalDateTime deadline) {
        if (!StringUtils.hasText(goalLevel)) {
            throw new IllegalArgumentException("目标等级不能为空");
        }
        if (targetScore != null && (targetScore < 0 || targetScore > 100)) {
            throw new IllegalArgumentException("目标分数需在 0-100 之间");
        }

        StudentGoal existing = getByStudentAndCourse(studentId, courseId);
        if (existing != null) {
            existing.setGoalLevel(goalLevel);
            existing.setTargetScore(targetScore);
            existing.setMilestones(milestones);
            existing.setNote(note);
            existing.setDeadline(deadline);
            existing.setUpdateTime(LocalDateTime.now());
            goalMapper.updateById(existing);
            return existing;
        } else {
            StudentGoal goal = new StudentGoal();
            goal.setStudentId(studentId);
            goal.setCourseId(courseId);
            goal.setGoalLevel(goalLevel);
            goal.setTargetScore(targetScore);
            goal.setMilestones(milestones);
            goal.setNote(note);
            goal.setDeadline(deadline);
            goal.setCreateTime(LocalDateTime.now());
            goal.setUpdateTime(LocalDateTime.now());
            goalMapper.insert(goal);
            return goal;
        }
    }
}
