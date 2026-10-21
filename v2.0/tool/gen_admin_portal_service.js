/* gen_admin_portal_service.js — 1-13 教务端：服务层 + 控制器 */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('service/AdminPortalService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.entity.Admin;
import ${P}.entity.AttendanceArchive;
import ${P}.entity.AuditLog;
import ${P}.entity.Course;
import ${P}.entity.CourseJoinRequest;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Enrollment;
import ${P}.entity.ImportBatch;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.AdminMapper;
import ${P}.mapper.AttendanceArchiveMapper;
import ${P}.mapper.AuditLogMapper;
import ${P}.mapper.CourseJoinRequestMapper;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.ImportBatchMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.AdminDashboardVO;
import ${P}.vo.AuditLogPageVO;
import ${P}.vo.AuditLogVO;
import ${P}.vo.ImportBatchVO;
import ${P}.vo.JoinRequestVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教务端：工作台概览、导入批次与回滚、入课知会、审计日志、覆盖率督办。
 * 口径：教务不审批入班申请，只收「知会」；归档与督办是教务的两项日常动作。
 */
@Service
@RequiredArgsConstructor
public class AdminPortalService {

    private final TermMapper termMapper;
    private final CourseMapper courseMapper;
    private final CourseOfferingMapper offeringMapper;
    private final TeachingClassMapper classMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final CourseSessionMapper sessionMapper;
    private final AttendanceArchiveMapper archiveMapper;
    private final CourseJoinRequestMapper joinMapper;
    private final ImportBatchMapper importBatchMapper;
    private final AuditLogMapper auditLogMapper;
    private final AdminMapper adminMapper;
    private final AttendanceService attendanceService;
    private final AuditService auditService;

    // ============================================================ 工作台
    public AdminDashboardVO dashboard(Long termId) {
        AdminDashboardVO v = new AdminDashboardVO();
        Term current = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1).last("limit 1"));
        if (current == null) {
            current = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                    .orderByDesc(Term::getStartDate).last("limit 1"));
        }
        Long useTerm = termId != null ? termId : (current == null ? null : current.getId());
        Term term = useTerm == null ? null : termMapper.selectById(useTerm);
        v.setCurrentTermId(useTerm);
        v.setCurrentTermName(term == null ? null : term.getTermName());
        v.setCourseCount(courseMapper.selectCount(Wrappers.<Course>lambdaQuery()).intValue());
        v.setStudentCount(studentMapper.selectCount(Wrappers.<com.example.academic_risk_warning.entity.Student>lambdaQuery()).intValue());
        v.setTeacherCount(teacherMapper.selectCount(Wrappers.<com.example.academic_risk_warning.entity.Teacher>lambdaQuery()).intValue());
        List<CourseOffering> offerings = offeringMapper.selectList(Wrappers.<CourseOffering>lambdaQuery()
                .eq(useTerm != null, CourseOffering::getTermId, useTerm));
        v.setOfferingCount(offerings.size());
        List<Long> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());
        int classCount = 0;
        if (!offeringIds.isEmpty()) {
            classCount = classMapper.selectCount(Wrappers.<TeachingClass>lambdaQuery()
                    .in(TeachingClass::getOfferingId, offeringIds)).intValue();
        }
        v.setTeachingClassCount(classCount);
        v.setArchivePending(archiveMapper.selectCount(Wrappers.<AttendanceArchive>lambdaQuery()
                .eq(useTerm != null, AttendanceArchive::getTermId, useTerm)
                .eq(AttendanceArchive::getStatus, "SUBMITTED")
                .eq(AttendanceArchive::getIsVoid, 0)).intValue());
        v.setJoinNoticeCount(joinMapper.selectCount(Wrappers.<CourseJoinRequest>lambdaQuery()
                .eq(CourseJoinRequest::getNotifyAdmin, 1)).intValue());
        v.setImportBatchCount(importBatchMapper.selectCount(Wrappers.<ImportBatch>lambdaQuery()).intValue());
        v.setAuditTodayCount(auditLogMapper.selectCount(Wrappers.<AuditLog>lambdaQuery()
                .ge(AuditLog::getCreateTime, LocalDate.now().atStartOfDay())).intValue());
        // 低覆盖率：复用覆盖率服务，按 <1/3 判定
        long low = attendanceService.coverage(useTerm).stream()
                .filter(c -> Boolean.TRUE.equals(c.getLowCoverage())).count();
        v.setLowCoverageCount((int) low);
        return v;
    }

    // ============================================================ 导入批次
    public List<ImportBatchVO> importBatches(Long termId, String status) {
        List<ImportBatch> list = importBatchMapper.selectList(Wrappers.<ImportBatch>lambdaQuery()
                .eq(termId != null, ImportBatch::getTermId, termId)
                .eq(status != null && !status.isEmpty(), ImportBatch::getStatus, status)
                .orderByDesc(ImportBatch::getId));
        List<ImportBatchVO> out = new ArrayList<>();
        for (ImportBatch b : list) {
            out.add(batchVO(b));
        }
        return out;
    }

    @Transactional
    public ImportBatchVO rollback(Long batchId, String reason, Long adminAccountId) {
        ImportBatch b = importBatchMapper.selectById(batchId);
        if (b == null) {
            throw new BizException("导入批次不存在");
        }
        if ("ROLLED_BACK".equals(b.getStatus())) {
            throw new BizException("该批次已经回滚过");
        }
        b.setStatus("ROLLED_BACK");
        b.setRollbackBy(adminAccountId);
        b.setRollbackTime(LocalDateTime.now());
        b.setRollbackReason(reason);
        importBatchMapper.updateById(b);
        auditService.record("IMPORT_BATCH", batchId, "ROLLBACK", null, null,
                "回滚导入批次 " + b.getBatchNo() + "：" + (reason == null ? "未填写原因" : reason));
        return batchVO(b);
    }

    private ImportBatchVO batchVO(ImportBatch b) {
        ImportBatchVO v = new ImportBatchVO();
        v.setId(b.getId());
        v.setBatchNo(b.getBatchNo());
        v.setImportType(b.getImportType());
        v.setFileName(b.getFileName());
        v.setTotalRows(b.getTotalRows());
        v.setSuccessRows(b.getSuccessRows());
        v.setFailedRows(b.getFailedRows());
        v.setStatus(b.getStatus());
        v.setStatusText(switch (b.getStatus() == null ? "" : b.getStatus()) {
            case "SUCCESS" -> "成功";
            case "PARTIAL" -> "部分成功";
            case "FAILED" -> "失败";
            case "RUNNING" -> "进行中";
            case "ROLLED_BACK" -> "已回滚";
            default -> b.getStatus();
        });
        v.setOperatorRole(b.getOperatorRole());
        if (b.getOperatorId() != null) {
            com.example.academic_risk_warning.entity.Teacher t = teacherMapper.selectOne(
                    Wrappers.<com.example.academic_risk_warning.entity.Teacher>lambdaQuery()
                            .eq(com.example.academic_risk_warning.entity.Teacher::getAccountId, b.getOperatorId())
                            .last("limit 1"));
            if (t != null) {
                v.setOperatorName(t.getTeacherName());
            } else {
                Admin a = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                        .eq(Admin::getAccountId, b.getOperatorId()).last("limit 1"));
                v.setOperatorName(a == null ? null : a.getAdminName());
            }
        }
        v.setStartTime(b.getStartTime());
        v.setEndTime(b.getEndTime());
        v.setRollbackTime(b.getRollbackTime());
        v.setRollbackReason(b.getRollbackReason());
        v.setRemark(b.getRemark());
        List<String> errors = new ArrayList<>();
        String json = b.getErrorJson();
        if (json != null && !json.isBlank()) {
            for (String part : json.split("\\n")) {
                if (!part.isBlank() && errors.size() < 10) {
                    errors.add(part.trim());
                }
            }
        }
        v.setErrors(errors);
        return v;
    }

    // ============================================================ 入课知会
    public List<JoinRequestVO> joinNotices(Long termId) {
        List<JoinRequestVO> out = new ArrayList<>();
        for (CourseJoinRequest r : joinMapper.selectList(Wrappers.<CourseJoinRequest>lambdaQuery()
                .eq(CourseJoinRequest::getNotifyAdmin, 1)
                .orderByDesc(CourseJoinRequest::getNotifyTime))) {
            JoinRequestVO v = new JoinRequestVO();
            v.setId(r.getId());
            v.setTeachingClassId(r.getTeachingClassId());
            TeachingClass tc = classMapper.selectById(r.getTeachingClassId());
            if (tc != null) {
                v.setClassName(tc.getClassName());
                CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
                Course c = o == null ? null : courseMapper.selectById(o.getCourseId());
                v.setCourseName(c == null ? null : c.getCourseName());
            }
            com.example.academic_risk_warning.entity.Student st = studentMapper.selectById(r.getStudentId());
            if (st != null) {
                v.setStudentId(st.getId());
                v.setStudentNo(st.getStudentNo());
                v.setStudentName(st.getStudentName());
            }
            v.setStatus(r.getStatus());
            v.setStatusText("知会：教师已同意入班");
            v.setApplyTime(r.getApplyTime());
            v.setReviewTime(r.getReviewTime());
            v.setNotifyAdmin(true);
            out.add(v);
        }
        return out;
    }

    // ============================================================ 审计日志
    public AuditLogPageVO auditLogs(String bizType, String action, String keyword,
                                    String from, String to, int page, int size) {
        List<AuditLog> list = auditLogMapper.selectList(Wrappers.<AuditLog>lambdaQuery()
                .eq(bizType != null && !bizType.isEmpty(), AuditLog::getBizType, bizType)
                .eq(action != null && !action.isEmpty(), AuditLog::getAction, action)
                .orderByDesc(AuditLog::getId)
                .last("limit 1000"));
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (from != null && !from.isBlank()) {
            start = LocalDate.parse(from).atStartOfDay();
        }
        if (to != null && !to.isBlank()) {
            end = LocalDate.parse(to).atTime(23, 59, 59);
        }
        List<AuditLog> filtered = new ArrayList<>();
        for (AuditLog a : list) {
            if (start != null && (a.getCreateTime() == null || a.getCreateTime().isBefore(start))) {
                continue;
            }
            if (end != null && (a.getCreateTime() == null || a.getCreateTime().isAfter(end))) {
                continue;
            }
            if (keyword != null && !keyword.isBlank()) {
                String k = keyword.trim();
                boolean hit = (a.getDetail() != null && a.getDetail().contains(k))
                        || (a.getBizType() != null && a.getBizType().contains(k))
                        || (a.getAction() != null && a.getAction().contains(k));
                if (!hit) {
                    continue;
                }
            }
            filtered.add(a);
        }
        int total = filtered.size();
        int pageSize = size <= 0 ? 20 : size;
        int p = page <= 0 ? 1 : page;
        int fromIdx = Math.min((p - 1) * pageSize, total);
        int toIdx = Math.min(fromIdx + pageSize, total);
        List<AuditLogVO> items = new ArrayList<>();
        Map<Long, String> names = new LinkedHashMap<>();
        for (AuditLog a : filtered.subList(fromIdx, toIdx)) {
            AuditLogVO v = new AuditLogVO();
            v.setId(a.getId());
            v.setBizType(a.getBizType());
            v.setBizId(a.getBizId());
            v.setAction(a.getAction());
            v.setOperatorId(a.getOperatorId());
            v.setOperatorRole(a.getOperatorRole());
            v.setOperatorRoleText(switch (a.getOperatorRole() == null ? "" : a.getOperatorRole()) {
                case "ADMIN" -> "教务";
                case "TEACHER" -> "教师";
                case "STUDENT" -> "学生";
                case "COUNSELOR" -> "导员";
                default -> a.getOperatorRole();
            });
            v.setDetail(a.getDetail());
            v.setCreateTime(a.getCreateTime());
            if (a.getOperatorId() != null) {
                String name = names.get(a.getOperatorId());
                if (name == null) {
                    name = resolveOperatorName(a.getOperatorRole(), a.getOperatorId());
                    names.put(a.getOperatorId(), name == null ? "" : name);
                }
                v.setOperatorName(name);
            }
            items.add(v);
        }
        AuditLogPageVO vo = new AuditLogPageVO();
        vo.setItems(items);
        vo.setTotal(total);
        vo.setPage(p);
        vo.setSize(pageSize);
        vo.setPages(Math.max(1, (int) Math.ceil(total / (double) pageSize)));
        vo.setBizTypes(list.stream().map(AuditLog::getBizType).filter(x -> x != null).distinct()
                .sorted().collect(Collectors.toList()));
        vo.setActions(list.stream().map(AuditLog::getAction).filter(x -> x != null).distinct()
                .sorted().collect(Collectors.toList()));
        return vo;
    }

    private String resolveOperatorName(String role, Long id) {
        if ("ADMIN".equals(role)) {
            Admin a = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                    .eq(Admin::getAccountId, id).last("limit 1"));
            return a == null ? null : a.getAdminName();
        }
        if ("TEACHER".equals(role)) {
            // 审计里记的是账号ID，教师档案用 account_id 反查
            com.example.academic_risk_warning.entity.Teacher t = teacherMapper.selectOne(
                    Wrappers.<com.example.academic_risk_warning.entity.Teacher>lambdaQuery()
                            .eq(com.example.academic_risk_warning.entity.Teacher::getAccountId, id).last("limit 1"));
            return t == null ? null : t.getTeacherName();
        }
        return null;
    }

    // ============================================================ 覆盖率督办
    @Transactional
    public void urgeCoverage(Long teachingClassId, Long adminAccountId) {
        TeachingClass tc = classMapper.selectById(teachingClassId);
        if (tc == null) {
            throw new BizException("教学班不存在");
        }
        // 有未提交的归档批次就一起催办
        List<AttendanceArchive> archives = archiveMapper.selectList(Wrappers.<AttendanceArchive>lambdaQuery()
                .eq(AttendanceArchive::getTeachingClassId, teachingClassId)
                .in(AttendanceArchive::getStatus, List.of("PENDING", "RETURNED"))
                .eq(AttendanceArchive::getIsVoid, 0));
        for (AttendanceArchive a : archives) {
            a.setRemindCount((a.getRemindCount() == null ? 0 : a.getRemindCount()) + 1);
            a.setLastRemindTime(LocalDateTime.now());
            archiveMapper.updateById(a);
        }
        auditService.record("ATTENDANCE_COVERAGE", teachingClassId, "URGE", null, null,
                "督办点名覆盖率：" + tc.getClassName() + (archives.isEmpty() ? "" : "，同时催办归档 " + archives.size() + " 次"));
    }
}
`);

console.log('教务端模块：服务层生成完成');
