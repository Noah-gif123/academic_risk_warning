package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 教师登录业务
 */
@Service
public class TeacherService {

    private final TeacherMapper teacherMapper;

    public TeacherService(TeacherMapper teacherMapper) {
        this.teacherMapper = teacherMapper;
    }

    /**
     * 根据工号与密码登录，成功返回教师信息（不含密码）
     */
    public Teacher login(String teacherNo, String password) {
        if (!StringUtils.hasText(teacherNo) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("工号和密码不能为空");
        }

        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>()
                        .eq(Teacher::getTeacherNo, teacherNo)
        );
        if (teacher == null || !password.equals(teacher.getPassword())) {
            throw new IllegalArgumentException("工号或密码错误");
        }

        teacher.setPassword(null);
        return teacher;
    }

    /**
     * 根据教师ID修改密码
     */
    public void updatePassword(Long teacherId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher == null) {
            throw new IllegalArgumentException("教师不存在");
        }
        if (!oldPassword.equals(teacher.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        teacher.setPassword(newPassword);
        teacherMapper.updateById(teacher);
    }

    /**
     * 根据ID获取教师信息
     */
    public Teacher getById(Long teacherId) {
        Teacher teacher = teacherMapper.selectById(teacherId);
        if (teacher != null) {
            teacher.setPassword(null);
        }
        return teacher;
    }
}
