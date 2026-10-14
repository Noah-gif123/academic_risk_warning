package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.*;
import com.example.academic_risk_warning.mapper.*;
import com.example.academic_risk_warning.vo.AdminTeacherVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员业务
 */
@Service
public class AdminService {

    private final AdminMapper adminMapper;
    private final TeacherMapper teacherMapper;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentMapper studentMapper;
    private final AlertRecordMapper alertRecordMapper;
    private final TeacherManagementLogMapper managementLogMapper;

    public AdminService(AdminMapper adminMapper,
                        TeacherMapper teacherMapper,
                        TeacherClassMapper teacherClassMapper,
                        StudentMapper studentMapper,
                        AlertRecordMapper alertRecordMapper,
                        TeacherManagementLogMapper managementLogMapper) {
        this.adminMapper = adminMapper;
        this.teacherMapper = teacherMapper;
        this.teacherClassMapper = teacherClassMapper;
        this.studentMapper = studentMapper;
        this.alertRecordMapper = alertRecordMapper;
        this.managementLogMapper = managementLogMapper;
    }

    /**
     * 管理员登录
     */
    public Admin login(String adminNo, String password) {
        if (!StringUtils.hasText(adminNo) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }
        Admin admin = adminMapper.selectOne(
                new LambdaQueryWrapper<Admin>().eq(Admin::getAdminNo, adminNo));
        if (admin == null || !password.equals(admin.getPassword())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        admin.setPassword(null);
        return admin;
    }

    /**
     * 获取所有教师列表（含风险统计和管理状态）
     */
    public List<AdminTeacherVO> listAllTeachers() {
        List<Teacher> teachers = teacherMapper.selectList(null);
        if (teachers.isEmpty()) return Collections.emptyList();

        List<Long> teacherIds = teachers.stream().map(Teacher::getId).toList();

        // 教师-班级映射
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>().in(TeacherClass::getTeacherId, teacherIds));
        Map<Long, List<TeacherClass>> tcMap = teacherClasses.stream()
                .collect(Collectors.groupingBy(TeacherClass::getTeacherId));

        // 班级名列表
        Set<String> allClassNames = teacherClasses.stream()
                .map(TeacherClass::getClassName).filter(Objects::nonNull).collect(Collectors.toSet());

        // 学生列表
        Map<String, List<Student>> studentByClass = Collections.emptyMap();
        Map<Long, Long> studentCountMap = Collections.emptyMap();
        if (!allClassNames.isEmpty()) {
            List<Student> allStudents = studentMapper.selectList(
                    new LambdaQueryWrapper<Student>().in(Student::getClassName, allClassNames));
            studentByClass = allStudents.stream()
                    .collect(Collectors.groupingBy(Student::getClassName));

            // 每个教师的学生数
            studentCountMap = new HashMap<>();
            for (Teacher teacher : teachers) {
                List<TeacherClass> tcs = tcMap.getOrDefault(teacher.getId(), List.of());
                long count = 0;
                for (TeacherClass tc : tcs) {
                    List<Student> classStudents = studentByClass.getOrDefault(tc.getClassName(), List.of());
                    if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null) {
                        count += classStudents.stream()
                                .filter(s -> s.getId() >= tc.getStudentIdStart() && s.getId() <= tc.getStudentIdEnd())
                                .count();
                    } else {
                        count += classStudents.size();
                    }
                }
                studentCountMap.put(teacher.getId(), count);
            }
        }

        // 每个教师的学生ID集合
        Map<Long, Set<Long>> teacherStudentIds = new HashMap<>();
        for (Teacher teacher : teachers) {
            Set<Long> ids = new HashSet<>();
            List<TeacherClass> tcs = tcMap.getOrDefault(teacher.getId(), List.of());
            for (TeacherClass tc : tcs) {
                List<Student> classStudents = studentByClass.getOrDefault(tc.getClassName(), List.of());
                for (Student s : classStudents) {
                    if (tc.getStudentIdStart() != null && tc.getStudentIdEnd() != null) {
                        if (s.getId() >= tc.getStudentIdStart() && s.getId() <= tc.getStudentIdEnd()) {
                            ids.add(s.getId());
                        }
                    } else {
                        ids.add(s.getId());
                    }
                }
            }
            teacherStudentIds.put(teacher.getId(), ids);
        }

        // 所有有学生的教师的学生ID汇总
        Set<Long> allStudentIds = teacherStudentIds.values().stream()
                .flatMap(Set::stream).collect(Collectors.toSet());

        // 预警数据
        Map<Long, List<AlertRecord>> alertsByTeacher = Collections.emptyMap();
        if (!allStudentIds.isEmpty()) {
            List<AlertRecord> allAlerts = alertRecordMapper.selectList(
                    new LambdaQueryWrapper<AlertRecord>()
                            .in(AlertRecord::getStudentId, allStudentIds)
                            .eq(AlertRecord::getStatus, "ACTIVE"));
            alertsByTeacher = new HashMap<>();
            for (Teacher teacher : teachers) {
                Set<Long> sids = teacherStudentIds.getOrDefault(teacher.getId(), Set.of());
                List<AlertRecord> teacherAlerts = allAlerts.stream()
                        .filter(a -> sids.contains(a.getStudentId()))
                        .collect(Collectors.toList());
                alertsByTeacher.put(teacher.getId(), teacherAlerts);
            }
        }

        // 管理状态
        Map<Long, String> managementStatusMap = new HashMap<>();
        List<TeacherManagementLog> allLogs = managementLogMapper.selectList(
                new LambdaQueryWrapper<TeacherManagementLog>()
                        .in(TeacherManagementLog::getTeacherId, teacherIds)
                        .orderByDesc(TeacherManagementLog::getCreateTime));
        Map<Long, List<TeacherManagementLog>> logsByTeacher = allLogs.stream()
                .collect(Collectors.groupingBy(TeacherManagementLog::getTeacherId));
        for (Teacher t : teachers) {
            List<TeacherManagementLog> logs = logsByTeacher.getOrDefault(t.getId(), List.of());
            managementStatusMap.put(t.getId(),
                    logs.isEmpty() ? "NORMAL" : (logs.get(0).getNewStatus() != null ? logs.get(0).getNewStatus() : "NORMAL"));
        }

        // 组装VO
        List<AdminTeacherVO> result = new ArrayList<>();
        for (Teacher t : teachers) {
            AdminTeacherVO vo = new AdminTeacherVO();
            vo.setTeacherId(t.getId());
            vo.setTeacherNo(t.getTeacherNo());
            vo.setTeacherName(t.getTeacherName());
            vo.setPhone(t.getPhone());

            List<TeacherClass> tcs = tcMap.getOrDefault(t.getId(), List.of());
            vo.setClassName(tcs.stream().map(TeacherClass::getClassName)
                    .filter(Objects::nonNull).distinct().collect(Collectors.joining("、")));

            vo.setStudentCount(studentCountMap.getOrDefault(t.getId(), 0L).intValue());

            List<AlertRecord> alerts = alertsByTeacher.getOrDefault(t.getId(), List.of());
            vo.setTotalAlertCount(alerts.size());
            vo.setRedAlertCount((int) alerts.stream().filter(a -> "RED".equals(a.getAlertLevel())).count());
            vo.setOrangeAlertCount((int) alerts.stream().filter(a -> "ORANGE".equals(a.getAlertLevel())).count());
            vo.setYellowAlertCount((int) alerts.stream().filter(a -> "YELLOW".equals(a.getAlertLevel())).count());
            vo.setHandledCount((int) alerts.stream().filter(a -> "HANDLED".equals(a.getStatus())).count());
            vo.setHandleRate(alerts.isEmpty() ? 1.0 :
                    Math.round(vo.getHandledCount() * 100.0 / alerts.size()) / 100.0);

            vo.setManagementStatus(managementStatusMap.getOrDefault(t.getId(), "NORMAL"));
            result.add(vo);
        }

        // 按红色预警数降序排列
        result.sort((a, b) -> b.getRedAlertCount().compareTo(a.getRedAlertCount()));
        return result;
    }

    /**
     * 根据ID获取管理员信息
     */
    public Admin getById(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin != null) admin.setPassword(null);
        return admin;
    }
}
