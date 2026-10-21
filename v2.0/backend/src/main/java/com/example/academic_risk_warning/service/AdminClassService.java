package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.AdminClassSaveRequest;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.entity.Major;
import com.example.academic_risk_warning.mapper.AdministrativeClassMapper;
import com.example.academic_risk_warning.mapper.MajorMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.entity.Student;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 行政班维护（导员按行政班管理学生） */
@Service
@RequiredArgsConstructor
public class AdminClassService {

    private final AdministrativeClassMapper classMapper;
    private final MajorMapper majorMapper;
    private final StudentMapper studentMapper;
    private final AuditService auditService;

    /** 班级列表，附带专业名与学生人数 */
    public List<Map<String, Object>> list(Long majorId, Integer gradeYear) {
        List<AdministrativeClass> classes = classMapper.selectList(Wrappers.<AdministrativeClass>lambdaQuery()
                .eq(majorId != null, AdministrativeClass::getMajorId, majorId)
                .eq(gradeYear != null, AdministrativeClass::getGradeYear, gradeYear)
                .orderByAsc(AdministrativeClass::getGradeYear)
                .orderByAsc(AdministrativeClass::getClassCode));
        Map<Long, String> majorNames = majorMapper.selectList(null).stream()
                .collect(Collectors.toMap(Major::getId, Major::getMajorName, (a, b) -> a));
        Map<Long, Long> counts = studentMapper.selectList(Wrappers.<Student>query()
                        .select("id", "admin_class_id")).stream()
                .filter(s -> s.getAdminClassId() != null)
                .collect(Collectors.groupingBy(Student::getAdminClassId, Collectors.counting()));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AdministrativeClass c : classes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("classCode", c.getClassCode());
            m.put("className", c.getClassName());
            m.put("majorId", c.getMajorId());
            m.put("majorName", majorNames.getOrDefault(c.getMajorId(), ""));
            m.put("gradeYear", c.getGradeYear());
            m.put("status", c.getStatus());
            m.put("studentCount", counts.getOrDefault(c.getId(), 0L));
            out.add(m);
        }
        return out;
    }

    @Transactional
    public AdministrativeClass save(AdminClassSaveRequest req) {
        if (majorMapper.selectById(req.getMajorId()) == null) {
            throw new BizException("所选专业不存在");
        }
        AdministrativeClass c;
        boolean create = req.getId() == null;
        if (create) {
            if (classMapper.selectCount(Wrappers.<AdministrativeClass>lambdaQuery()
                    .eq(AdministrativeClass::getClassCode, req.getClassCode())) > 0) {
                throw new BizException("班级编码已存在：" + req.getClassCode());
            }
            c = new AdministrativeClass();
        } else {
            c = classMapper.selectById(req.getId());
            if (c == null) {
                throw new BizException("班级不存在");
            }
        }
        c.setClassCode(req.getClassCode());
        c.setClassName(req.getClassName());
        c.setMajorId(req.getMajorId());
        c.setGradeYear(req.getGradeYear());
        if (create) {
            c.setStatus("ACTIVE");
            classMapper.insert(c);
        } else {
            classMapper.updateById(c);
        }
        auditService.record("ADMIN_CLASS", c.getId(), create ? "CREATE" : "UPDATE", null, null,
                (create ? "新增行政班 " : "修改行政班 ") + c.getClassName());
        return c;
    }
}
