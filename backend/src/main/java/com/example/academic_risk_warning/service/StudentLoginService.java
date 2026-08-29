package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.vo.StudentLoginVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 学生登录业务
 */
@Service
public class StudentLoginService {

    private final StudentMapper studentMapper;
    private final TokenService tokenService;

    public StudentLoginService(StudentMapper studentMapper, TokenService tokenService) {
        this.studentMapper = studentMapper;
        this.tokenService = tokenService;
    }

    /**
     * 根据学号与密码登录，成功返回学生信息（不含密码）并生成 Token
     */
    public StudentLoginVO login(String studentNo, String password) {
        if (!StringUtils.hasText(studentNo) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("学号和密码不能为空");
        }

        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getStudentNo, studentNo)
        );
        if (student == null) {
            throw new IllegalArgumentException("学号不存在");
        }
        if (student.getPassword() == null || !student.getPassword().equals(password)) {
            throw new IllegalArgumentException("密码错误");
        }

        String token = tokenService.createToken(student.getId());

        StudentLoginVO vo = new StudentLoginVO();
        vo.setStudentId(student.getId());
        vo.setStudentNo(student.getStudentNo());
        vo.setStudentName(student.getStudentName());
        vo.setGrade(student.getGrade());
        vo.setClassName(student.getClassName());
        vo.setToken(token);
        return vo;
    }

    /**
     * 根据 studentId 获取学生完整信息（不含密码）
     */
    public Student getStudentById(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return studentMapper.selectById(studentId);
    }

    /**
     * 修改学生密码
     * @param studentId 学生ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void updatePassword(Long studentId, String oldPassword, String newPassword) {
        if (studentId == null) {
            throw new IllegalArgumentException("学生ID不能为空");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        if (!oldPassword.equals(student.getPassword())) {
            throw new IllegalArgumentException("旧密码错误");
        }

        student.setPassword(newPassword);
        studentMapper.updateById(student);
    }
}
