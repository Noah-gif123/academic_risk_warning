package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.CounselorAssignRequest;
import com.example.academic_risk_warning.entity.AdministrativeClass;
import com.example.academic_risk_warning.entity.Counselor;
import com.example.academic_risk_warning.entity.CounselorClass;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.mapper.AdministrativeClassMapper;
import com.example.academic_risk_warning.mapper.CounselorClassMapper;
import com.example.academic_risk_warning.mapper.CounselorMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.vo.CounselorClassVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 辅导员分配：按学期把行政班分配给导员（换人留痕，旧关系保留） */
@Service
@RequiredArgsConstructor
public class CounselorAssignService {

    private final CounselorClassMapper counselorClassMapper;
    private final CounselorMapper counselorMapper;
    private final AdministrativeClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final AuditService auditService;

    public List<CounselorClassVO> list(Long termId, Long counselorId) {
        List<CounselorClass> rows = counselorClassMapper.selectList(Wrappers.<CounselorClass>lambdaQuery()
                .eq(termId != null, CounselorClass::getTermId, termId)
                .eq(counselorId != null, CounselorClass::getCounselorId, counselorId)
                .orderByAsc(CounselorClass::getCounselorId));
        if (rows.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Counselor> counselors = counselorMapper.selectBatchIds(
                        rows.stream().map(CounselorClass::getCounselorId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(Counselor::getId, c -> c, (a, b) -> a));
        Map<Long, AdministrativeClass> classes = classMapper.selectBatchIds(
                        rows.stream().map(CounselorClass::getAdminClassId).distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(AdministrativeClass::getId, c -> c, (a, b) -> a));
        Map<Long, Long> counts = studentMapper.selectList(Wrappers.<Student>query()
                        .select("id", "admin_class_id")).stream()
                .filter(s -> s.getAdminClassId() != null)
                .collect(Collectors.groupingBy(Student::getAdminClassId, Collectors.counting()));
        List<CounselorClassVO> out = new ArrayList<>();
        for (CounselorClass r : rows) {
            CounselorClassVO vo = new CounselorClassVO();
            vo.setId(r.getId());
            vo.setTermId(r.getTermId());
            vo.setCounselorId(r.getCounselorId());
            Counselor c = counselors.get(r.getCounselorId());
            if (c != null) {
                vo.setCounselorNo(c.getCounselorNo());
                vo.setCounselorName(c.getCounselorName());
            }
            vo.setAdminClassId(r.getAdminClassId());
            AdministrativeClass ac = classes.get(r.getAdminClassId());
            if (ac != null) {
                vo.setClassCode(ac.getClassCode());
                vo.setClassName(ac.getClassName());
                vo.setGradeYear(ac.getGradeYear());
            }
            vo.setStudentCount(counts.getOrDefault(r.getAdminClassId(), 0L).intValue());
            vo.setIsPrimary(r.getIsPrimary());
            out.add(vo);
        }
        return out;
    }

    @Transactional
    public int assign(CounselorAssignRequest req) {
        if (counselorMapper.selectById(req.getCounselorId()) == null) {
            throw new BizException("辅导员不存在");
        }
        int created = 0;
        for (Long classId : req.getAdminClassIds()) {
            if (classMapper.selectById(classId) == null) {
                throw new BizException("行政班不存在：" + classId);
            }
            long exists = counselorClassMapper.selectCount(Wrappers.<CounselorClass>lambdaQuery()
                    .eq(CounselorClass::getTermId, req.getTermId())
                    .eq(CounselorClass::getAdminClassId, classId));
            if (exists > 0) {
                continue;
            }
            CounselorClass cc = new CounselorClass();
            cc.setCounselorId(req.getCounselorId());
            cc.setAdminClassId(classId);
            cc.setTermId(req.getTermId());
            cc.setIsPrimary(1);
            counselorClassMapper.insert(cc);
            created++;
        }
        auditService.record("COUNSELOR_CLASS", req.getCounselorId(), "ASSIGN", null, null,
                "学期 " + req.getTermId() + " 新增带班 " + created + " 个");
        return created;
    }

    @Transactional
    public void remove(Long id) {
        CounselorClass cc = counselorClassMapper.selectById(id);
        if (cc == null) {
            throw new BizException("记录不存在");
        }
        counselorClassMapper.deleteById(id);
        auditService.record("COUNSELOR_CLASS", cc.getCounselorId(), "UNASSIGN", null, null,
                "解除带班：行政班 " + cc.getAdminClassId() + "（学期 " + cc.getTermId() + "）");
    }
}
