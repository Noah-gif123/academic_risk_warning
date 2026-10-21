/* gen_attendance_service.js — 1-9 考勤与归档：服务层 + 学生端视图 + 控制器 */
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

// ================================================================ 学生端视图
w('vo/StudentAttendanceVO.java', `
package ${P}.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** 学生端考勤：明细 + 汇总；按口径**不显示剩余可缺次数**，只在临界时提示「再缺一次即挂」 */
@Data
public class StudentAttendanceVO {
    private Long teachingClassId;
    private String courseName;
    private String teacherName;
    private Integer totalSessions;
    private Integer calledSessions;
    private Integer presentCount;
    private Integer lateCount;
    private Integer absentCount;
    private Integer leaveCount;
    private Integer deductPoints;
    private Integer maxAbsent;
    private String thresholdHint;
    private List<Row> records;

    @Data
    public static class Row {
        private Long id;
        private Integer weekNo;
        private Integer sessionNo;
        private LocalDate sessionDate;
        private String status;
        private String leaveType;
        private String phase;
    }
}
`);

// ================================================================ 服务层
w('service/AttendanceService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.ArchiveReviewRequest;
import ${P}.dto.ArchiveSubmitRequest;
import ${P}.dto.AttendanceCallRequest;
import ${P}.dto.AttendanceItemDTO;
import ${P}.dto.AttendanceRecordUpdateRequest;
import ${P}.entity.AttendanceArchive;
import ${P}.entity.AttendanceRecord;
import ${P}.entity.AttendanceRecordLog;
import ${P}.entity.Course;
import ${P}.entity.CourseOffering;
import ${P}.entity.CourseSession;
import ${P}.entity.Enrollment;
import ${P}.entity.Student;
import ${P}.entity.Teacher;
import ${P}.entity.TeachingClass;
import ${P}.entity.Term;
import ${P}.mapper.AttendanceArchiveMapper;
import ${P}.mapper.AttendanceRecordLogMapper;
import ${P}.mapper.AttendanceRecordMapper;
import ${P}.mapper.CourseMapper;
import ${P}.mapper.CourseOfferingMapper;
import ${P}.mapper.CourseSessionMapper;
import ${P}.mapper.EnrollmentMapper;
import ${P}.mapper.StudentMapper;
import ${P}.mapper.TeacherMapper;
import ${P}.mapper.TeachingClassMapper;
import ${P}.mapper.TermMapper;
import ${P}.vo.ArchiveVO;
import ${P}.vo.AttendanceImportVO;
import ${P}.vo.AttendanceRosterRowVO;
import ${P}.vo.AttendanceSessionVO;
import ${P}.vo.AttendanceStudentStatVO;
import ${P}.vo.CoverageVO;
import ${P}.vo.StudentAttendanceVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 考勤与归档。口径（见 04_attendance.sql 附一）：
 * ① 平时 / 归档双轨：只存一行，归档就地改成 phase=ARCHIVE，不复制第二套；
 * ② 扣分不落库：P = 2×缺课 + 1×迟到 + 1×max(0, 请假−3)，无上限；
 * ③ 1/3 硬门槛：N_max = floor(有效课次/3)，缺课 ≥ N_max+1 即挂；
 * ④ 点名覆盖率 = 已点名课次 / 已过日期课次，低于 1/3 判低置信度并督办；
 * ⑤ 归档后记录冻结：教师不能再改，只能教务更正并留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final String PRESENT = "PRESENT";
    private static final String LATE = "LATE";
    private static final String ABSENT = "ABSENT";
    private static final String LEAVE = "LEAVE";

    private final AttendanceRecordMapper recordMapper;
    private final AttendanceArchiveMapper archiveMapper;
    private final AttendanceRecordLogMapper logMapper;
    private final CourseSessionMapper sessionMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final TermMapper termMapper;
    private final CourseContentService contentService;
    private final AuditService auditService;

    // ============================================================ 课次列表
    public List<AttendanceSessionVO> listSessions(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        List<CourseSession> sessions = sessionsOf(tc);
        int total = enrollmentCount(tc.getId());
        List<AttendanceSessionVO> out = new ArrayList<>();
        for (CourseSession s : sessions) {
            AttendanceSessionVO v = new AttendanceSessionVO();
            v.setSessionId(s.getId());
            v.setWeekNo(s.getWeekNo());
            v.setSessionNo(s.getSessionNo());
            v.setSessionDate(s.getSessionDate());
            v.setSessionType(s.getSessionType());
            v.setStatus(s.getStatus());
            v.setRollCallStatus(s.getRollCallStatus());
            v.setRollCallCount(s.getRollCallCount());
            v.setTotalStudents(total);
            List<AttendanceRecord> records = recordsOfSession(s.getId());
            v.setPresentCount(countStatus(records, PRESENT));
            v.setLateCount(countStatus(records, LATE));
            v.setAbsentCount(countStatus(records, ABSENT));
            v.setLeaveCount(countStatus(records, LEAVE));
            boolean archived = records.stream().anyMatch(r -> "ARCHIVE".equals(r.getPhase()));
            v.setArchived(archived);
            v.setPhase(archived ? "ARCHIVE" : "NORMAL");
            out.add(v);
        }
        return out;
    }

    // ============================================================ 点名单
    public List<AttendanceRosterRowVO> roster(Long teachingClassId, Long sessionId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        CourseSession session = requireSession(tc, sessionId);
        List<Enrollment> ens = enrollmentsOf(tc.getId());
        Map<Long, AttendanceRecord> records = recordsOfSession(session.getId()).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r, (a, b) -> a));
        Map<Long, Student> students = studentsOf(ens);
        Map<Long, Integer> absents = absentTotals(tc.getId());
        List<AttendanceRosterRowVO> out = new ArrayList<>();
        for (Enrollment e : ens) {
            AttendanceRosterRowVO v = new AttendanceRosterRowVO();
            v.setEnrollmentId(e.getId());
            v.setStudentId(e.getStudentId());
            Student st = students.get(e.getStudentId());
            v.setStudentNo(st == null ? null : st.getStudentNo());
            v.setStudentName(st == null ? null : st.getStudentName());
            AttendanceRecord r = records.get(e.getStudentId());
            if (r == null) {
                v.setStatus(PRESENT);
                v.setArchived(false);
            } else {
                v.setRecordId(r.getId());
                v.setStatus(r.getStatus());
                v.setLeaveType(r.getLeaveType());
                v.setArchived("ARCHIVE".equals(r.getPhase()));
            }
            int abs = absents.getOrDefault(e.getStudentId(), 0);
            v.setAbsentTotal(abs);
            v.setThresholdHint(hintOf(abs, maxAbsent(tc)));
            out.add(v);
        }
        return out;
    }

    // ============================================================ 提交点名
    @Transactional
    public AttendanceSessionVO call(AttendanceCallRequest req, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        CourseSession session = requireSession(tc, req.getSessionId());
        if ("CANCELLED".equals(session.getStatus())) {
            throw new BizException("该课次已取消，不能点名");
        }
        Map<Long, String> wanted = new LinkedHashMap<>();
        Map<Long, String> leaveTypes = new LinkedHashMap<>();
        if (req.getItems() != null) {
            for (AttendanceItemDTO it : req.getItems()) {
                if (it.getStudentId() == null) {
                    continue;
                }
                wanted.put(it.getStudentId(), normalizeStatus(it.getStatus()));
                leaveTypes.put(it.getStudentId(), it.getLeaveType());
            }
        }
        List<Enrollment> ens = enrollmentsOf(tc.getId());
        Map<Long, AttendanceRecord> existing = recordsOfSession(session.getId()).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r, (a, b) -> a));
        String batchKey = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        LocalDateTime now = LocalDateTime.now();
        int changed = 0;
        for (Enrollment e : ens) {
            String status = wanted.getOrDefault(e.getStudentId(), PRESENT);
            String leaveType = LEAVE.equals(status) ? leaveTypes.get(e.getStudentId()) : null;
            AttendanceRecord r = existing.get(e.getStudentId());
            if (r != null && "ARCHIVE".equals(r.getPhase())) {
                if (!status.equals(r.getStatus())) {
                    throw new BizException("本次课已归档，教师不能再改考勤（需教务更正）");
                }
                continue;
            }
            if (r == null) {
                r = new AttendanceRecord();
                r.setEnrollmentId(e.getId());
                r.setStudentId(e.getStudentId());
                r.setSessionId(session.getId());
                r.setTermId(contentService.termIdOf(tc));
                r.setStatus(status);
                r.setLeaveType(leaveType);
                r.setPhase("NORMAL");
                r.setSource("TEACHER");
                r.setRecordedBy(teacherId);
                r.setRecordedRole("TEACHER");
                r.setRecordedAt(now);
                r.setBatchKey(batchKey);
                r.setIsVoid(0);
                recordMapper.insert(r);
                writeLog(r, null, status, null, "NORMAL", "TEACHER_INPUT", req.getNote(), teacherId, "TEACHER", batchKey);
                changed++;
            } else if (!status.equals(r.getStatus())
                    || (leaveType == null ? r.getLeaveType() != null : !leaveType.equals(r.getLeaveType()))) {
                String old = r.getStatus();
                r.setStatus(status);
                r.setLeaveType(leaveType);
                r.setUpdatedBy(teacherId);
                r.setUpdatedRole("TEACHER");
                r.setUpdateNote(req.getNote());
                r.setBatchKey(batchKey);
                recordMapper.updateById(r);
                writeLog(r, old, status, "NORMAL", "NORMAL", "TEACHER_EDIT", req.getNote(), teacherId, "TEACHER", batchKey);
                changed++;
            }
        }
        session.setRollCallStatus("CALLED");
        session.setRollCallCount(countStatus(recordsOfSession(session.getId()), PRESENT)
                + countStatus(recordsOfSession(session.getId()), LATE));
        session.setRollCallBy(teacherId);
        session.setRollCallTime(now);
        sessionMapper.updateById(session);
        auditService.record("ATTENDANCE_SESSION", session.getId(), "CALL", null, null,
                "点名完成：第 " + session.getSessionNo() + " 次课，变更 " + changed + " 行");
        return listSessions(tc.getId(), teacherId).stream()
                .filter(x -> session.getId().equals(x.getSessionId())).findFirst().orElse(null);
    }

    // ============================================================ 单行修改
    @Transactional
    public AttendanceRosterRowVO updateRecord(Long recordId, AttendanceRecordUpdateRequest req, Long teacherId) {
        AttendanceRecord r = recordMapper.selectById(recordId);
        if (r == null) {
            throw new BizException("考勤记录不存在");
        }
        CourseSession session = sessionMapper.selectById(r.getSessionId());
        TeachingClass tc = contentService.requireMyClass(session.getTeachingClassId(), teacherId);
        if ("ARCHIVE".equals(r.getPhase())) {
            throw new BizException("该记录已归档并冻结：教师不能修改，需教务更正并留痕");
        }
        String status = normalizeStatus(req.getStatus());
        String leaveType = LEAVE.equals(status) ? req.getLeaveType() : null;
        String old = r.getStatus();
        r.setStatus(status);
        r.setLeaveType(leaveType);
        r.setUpdatedBy(teacherId);
        r.setUpdatedRole("TEACHER");
        r.setUpdateNote(req.getNote());
        recordMapper.updateById(r);
        writeLog(r, old, status, "NORMAL", "NORMAL", "TEACHER_EDIT", req.getNote(), teacherId, "TEACHER", null);
        AttendanceRosterRowVO v = new AttendanceRosterRowVO();
        v.setRecordId(r.getId());
        v.setEnrollmentId(r.getEnrollmentId());
        v.setStudentId(r.getStudentId());
        v.setStatus(r.getStatus());
        v.setLeaveType(r.getLeaveType());
        v.setArchived(false);
        v.setAbsentTotal(absentTotals(tc.getId()).getOrDefault(r.getStudentId(), 0));
        return v;
    }

    // ============================================================ 汇总
    public List<AttendanceStudentStatVO> stats(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        return statsOf(tc);
    }

    private List<AttendanceStudentStatVO> statsOf(TeachingClass tc) {
        List<Enrollment> ens = enrollmentsOf(tc.getId());
        Map<Long, Student> students = studentsOf(ens);
        Map<Long, List<AttendanceRecord>> byStudent = recordsOfClass(tc.getId()).stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getStudentId));
        int maxAbsent = maxAbsent(tc);
        List<AttendanceSessionVO> sessions = listSessionsInternal(tc);
        long passed = sessions.stream().filter(s -> s.getSessionDate() != null
                && !s.getSessionDate().isAfter(LocalDate.now())).count();
        long called = sessions.stream().filter(s -> s.getSessionDate() != null
                && !s.getSessionDate().isAfter(LocalDate.now())
                && ("CALLED".equals(s.getRollCallStatus()) || "PARTIAL".equals(s.getRollCallStatus()))).count();
        List<AttendanceStudentStatVO> out = new ArrayList<>();
        for (Enrollment e : ens) {
            AttendanceStudentStatVO v = new AttendanceStudentStatVO();
            v.setStudentId(e.getStudentId());
            Student st = students.get(e.getStudentId());
            v.setStudentNo(st == null ? null : st.getStudentNo());
            v.setStudentName(st == null ? null : st.getStudentName());
            List<AttendanceRecord> rs = byStudent.getOrDefault(e.getStudentId(), new ArrayList<>());
            int p = countStatus(rs, PRESENT);
            int l = countStatus(rs, LATE);
            int a = countStatus(rs, ABSENT);
            int lv = countStatus(rs, LEAVE);
            v.setPresentCount(p);
            v.setLateCount(l);
            v.setAbsentCount(a);
            v.setLeaveCount(lv);
            v.setCalledSessions((int) called);
            v.setDeductPoints(l * 1 + a * 2 + Math.max(0, lv - 3));
            v.setMaxAbsent(maxAbsent);
            v.setRemainingAbsent(maxAbsent - a);
            v.setThresholdHint(hintOf(a, maxAbsent));
            int attended = p + l;
            v.setAttendanceRate(passed == 0 ? null
                    : BigDecimal.valueOf(attended * 100.0 / passed).setScale(2, RoundingMode.HALF_UP));
            out.add(v);
        }
        return out;
    }

    private List<AttendanceSessionVO> listSessionsInternal(TeachingClass tc) {
        List<AttendanceSessionVO> out = new ArrayList<>();
        for (CourseSession s : sessionsOf(tc)) {
            AttendanceSessionVO v = new AttendanceSessionVO();
            v.setSessionId(s.getId());
            v.setWeekNo(s.getWeekNo());
            v.setSessionNo(s.getSessionNo());
            v.setSessionDate(s.getSessionDate());
            v.setStatus(s.getStatus());
            v.setRollCallStatus(s.getRollCallStatus());
            out.add(v);
        }
        return out;
    }

    // ============================================================ 批量导入
    @Transactional
    public AttendanceImportVO importFile(MultipartFile file, Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        List<List<String>> rows;
        if (name.toLowerCase().endsWith(".xlsx")) {
            rows = readXlsx(file);
        } else if (name.toLowerCase().endsWith(".csv") || name.toLowerCase().endsWith(".txt")) {
            rows = readCsv(file);
        } else {
            throw new BizException("只支持 .xlsx / .csv");
        }
        AttendanceImportVO vo = new AttendanceImportVO();
        vo.setFileName(name);
        List<String> errors = new ArrayList<>();
        int ok = 0;
        int skipped = 0;
        int total = 0;
        Map<Integer, CourseSession> byNo = sessionsOf(tc).stream()
                .collect(Collectors.toMap(CourseSession::getSessionNo, s -> s, (a, b) -> a));
        Map<String, Enrollment> byNo2 = new LinkedHashMap<>();
        Map<Long, Student> students = new LinkedHashMap<>();
        for (Enrollment e : enrollmentsOf(tc.getId())) {
            Student st = studentMapper.selectById(e.getStudentId());
            if (st != null) {
                byNo2.put(st.getStudentNo(), e);
                students.put(e.getStudentId(), st);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        String batchKey = "IMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        for (List<String> row : rows) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            String first = cell(row, 0);
            if (first.isEmpty() || first.contains("学号")) {
                continue;
            }
            total++;
            String studentNo = first;
            String sessionNoText = cell(row, 1);
            String statusText = cell(row, 2);
            String leaveText = cell(row, 3);
            Enrollment e = byNo2.get(studentNo);
            if (e == null) {
                skipped++;
                addError(errors, "第 " + (total + 1) + " 行：学号 " + studentNo + " 不在本教学班");
                continue;
            }
            CourseSession session;
            try {
                session = byNo.get(Integer.parseInt(sessionNoText.replace(".0", "")));
            } catch (RuntimeException ex) {
                skipped++;
                addError(errors, "第 " + (total + 1) + " 行：课次序号 " + sessionNoText + " 无法识别");
                continue;
            }
            if (session == null) {
                skipped++;
                addError(errors, "第 " + (total + 1) + " 行：课次 " + sessionNoText + " 不存在");
                continue;
            }
            String status = normalizeStatus(statusText);
            if (status == null) {
                skipped++;
                addError(errors, "第 " + (total + 1) + " 行：状态「" + statusText + "」无法识别（出勤/迟到/缺课/请假）");
                continue;
            }
            AttendanceRecord r = recordMapper.selectOne(Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getEnrollmentId, e.getId())
                    .eq(AttendanceRecord::getSessionId, session.getId()).last("limit 1"));
            if (r != null && "ARCHIVE".equals(r.getPhase())) {
                skipped++;
                addError(errors, "第 " + (total + 1) + " 行：该课次已归档，导入不改归档数据");
                continue;
            }
            String leaveType = LEAVE.equals(status) ? mapLeaveType(leaveText) : null;
            if (r == null) {
                r = new AttendanceRecord();
                r.setEnrollmentId(e.getId());
                r.setStudentId(e.getStudentId());
                r.setSessionId(session.getId());
                r.setTermId(contentService.termIdOf(tc));
                r.setStatus(status);
                r.setLeaveType(leaveType);
                r.setPhase("NORMAL");
                r.setSource("TEACHER");
                r.setRecordedBy(teacherId);
                r.setRecordedRole("TEACHER");
                r.setRecordedAt(now);
                r.setBatchKey(batchKey);
                r.setIsVoid(0);
                recordMapper.insert(r);
                writeLog(r, null, status, null, "NORMAL", "TEACHER_INPUT", "批量导入", teacherId, "TEACHER", batchKey);
            } else {
                String old = r.getStatus();
                r.setStatus(status);
                r.setLeaveType(leaveType);
                r.setUpdatedBy(teacherId);
                r.setUpdatedRole("TEACHER");
                r.setUpdateNote("批量导入");
                r.setBatchKey(batchKey);
                recordMapper.updateById(r);
                writeLog(r, old, status, "NORMAL", "NORMAL", "TEACHER_EDIT", "批量导入", teacherId, "TEACHER", batchKey);
            }
            if (!"CALLED".equals(session.getRollCallStatus())) {
                session.setRollCallStatus("CALLED");
                session.setRollCallBy(teacherId);
                session.setRollCallTime(now);
            }
            session.setRollCallCount(countStatus(recordsOfSession(session.getId()), PRESENT)
                    + countStatus(recordsOfSession(session.getId()), LATE));
            sessionMapper.updateById(session);
            ok++;
        }
        vo.setTotalRows(total);
        vo.setSuccessRows(ok);
        vo.setSkippedRows(skipped);
        vo.setErrors(errors);
        auditService.record("ATTENDANCE_IMPORT", tc.getId(), "IMPORT", null, null,
                "导入考勤：" + name + "，成功 " + ok + " 行，跳过 " + skipped + " 行");
        return vo;
    }

    private void addError(List<String> errors, String msg) {
        if (errors.size() < 50) {
            errors.add(msg);
        }
    }

    // ============================================================ 归档
    @Transactional
    public ArchiveVO submitArchive(ArchiveSubmitRequest req, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        String phase = req.getPhase().toUpperCase();
        if (!"MID".equals(phase) && !"FINAL".equals(phase)) {
            throw new BizException("归档阶段只能是 MID（期中）或 FINAL（期末）");
        }
        AttendanceArchive archive = archiveMapper.selectOne(Wrappers.<AttendanceArchive>lambdaQuery()
                .eq(AttendanceArchive::getTeachingClassId, tc.getId())
                .eq(AttendanceArchive::getPhase, phase)
                .eq(AttendanceArchive::getIsVoid, 0).last("limit 1"));
        if (archive != null && "REVIEWED".equals(archive.getStatus())) {
            throw new BizException("该阶段归档已由教务确认，不能再改（如需更正请联系教务）");
        }
        if (archive != null && "SUBMITTED".equals(archive.getStatus())) {
            throw new BizException("该阶段归档已提交，等待教务审核；如需修改请等教务退回");
        }
        List<CourseSession> sessions = sessionsOf(tc);
        if (sessions.isEmpty()) {
            throw new BizException("还没有课次，不能归档");
        }
        int total = sessions.size();
        int half = (int) Math.ceil(total / 2.0);
        int from;
        int to;
        if ("MID".equals(phase)) {
            from = 1;
            to = half;
        } else {
            long midReviewed = archiveMapper.selectCount(Wrappers.<AttendanceArchive>lambdaQuery()
                    .eq(AttendanceArchive::getTeachingClassId, tc.getId())
                    .eq(AttendanceArchive::getPhase, "MID")
                    .eq(AttendanceArchive::getStatus, "REVIEWED")
                    .eq(AttendanceArchive::getIsVoid, 0));
            from = midReviewed > 0 ? half + 1 : 1;
            to = total;
        }
        List<Long> sessionIds = sessions.stream()
                .filter(s -> s.getSessionNo() != null && s.getSessionNo() >= from && s.getSessionNo() <= to)
                .map(CourseSession::getId).collect(Collectors.toList());
        int recordCount = sessionIds.isEmpty() ? 0 : recordMapper.selectList(
                Wrappers.<AttendanceRecord>lambdaQuery()
                        .in(AttendanceRecord::getSessionId, sessionIds)
                        .eq(AttendanceRecord::getIsVoid, 0)).size();
        BigDecimal coverage = coverageOf(tc);
        if (archive == null) {
            archive = new AttendanceArchive();
            archive.setOfferingId(tc.getOfferingId());
            archive.setTeachingClassId(tc.getId());
            archive.setTeacherId(teacherId);
            archive.setTermId(contentService.termIdOf(tc));
            archive.setPhase(phase);
            archive.setIsVoid(0);
            archive.setRemindCount(0);
        }
        archive.setWindowOpenTime(archive.getWindowOpenTime() == null ? LocalDateTime.now() : archive.getWindowOpenTime());
        archive.setSubmitTime(LocalDateTime.now());
        archive.setSubmitBy(teacherId);
        archive.setSubmitNote(req.getNote());
        archive.setSessionFrom(from);
        archive.setSessionTo(to);
        archive.setSessionCount(sessionIds.size());
        archive.setRecordCount(recordCount);
        archive.setCoverageRate(coverage);
        archive.setStatus("SUBMITTED");
        if (archive.getId() == null) {
            archiveMapper.insert(archive);
        } else {
            archiveMapper.updateById(archive);
        }
        auditService.record("ATTENDANCE_ARCHIVE", archive.getId(), "SUBMIT", null, null,
                "提交" + ("MID".equals(phase) ? "期中" : "期末") + "归档：第 " + from + "~" + to + " 次课，"
                        + recordCount + " 条记录，覆盖率 " + coverage + "%");
        return archiveVO(archive);
    }

    public List<ArchiveVO> listTeacherArchives(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        List<AttendanceArchive> list = archiveMapper.selectList(Wrappers.<AttendanceArchive>lambdaQuery()
                .eq(AttendanceArchive::getTeachingClassId, tc.getId())
                .orderByAsc(AttendanceArchive::getPhase));
        List<ArchiveVO> out = new ArrayList<>();
        for (AttendanceArchive a : list) {
            out.add(archiveVO(a));
        }
        return out;
    }

    public List<ArchiveVO> listArchivesByTerm(Long termId, String status) {
        List<AttendanceArchive> list = archiveMapper.selectList(Wrappers.<AttendanceArchive>lambdaQuery()
                .eq(termId != null, AttendanceArchive::getTermId, termId)
                .eq(status != null && !status.isEmpty(), AttendanceArchive::getStatus, status)
                .orderByDesc(AttendanceArchive::getId));
        List<ArchiveVO> out = new ArrayList<>();
        for (AttendanceArchive a : list) {
            out.add(archiveVO(a));
        }
        return out;
    }

    /** 教务确认 / 退回 */
    @Transactional
    public ArchiveVO review(Long archiveId, boolean approve, ArchiveReviewRequest req, Long adminId) {
        AttendanceArchive a = archiveMapper.selectById(archiveId);
        if (a == null) {
            throw new BizException("归档批次不存在");
        }
        if (!"SUBMITTED".equals(a.getStatus())) {
            throw new BizException("只有已提交的批次可以审核（当前：" + a.getStatus() + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        if (approve) {
            List<CourseSession> sessions = sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                    .eq(CourseSession::getTeachingClassId, a.getTeachingClassId()));
            List<Long> sessionIds = sessions.stream()
                    .filter(s -> s.getSessionNo() != null
                            && a.getSessionFrom() != null && s.getSessionNo() >= a.getSessionFrom()
                            && a.getSessionTo() != null && s.getSessionNo() <= a.getSessionTo())
                    .map(CourseSession::getId).collect(Collectors.toList());
            if (!sessionIds.isEmpty()) {
                List<AttendanceRecord> records = recordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                        .in(AttendanceRecord::getSessionId, sessionIds)
                        .eq(AttendanceRecord::getIsVoid, 0));
                for (AttendanceRecord r : records) {
                    String oldPhase = r.getPhase();
                    r.setPhase("ARCHIVE");
                    r.setArchiveId(a.getId());
                    recordMapper.updateById(r);
                    writeLog(r, r.getStatus(), r.getStatus(), oldPhase, "ARCHIVE", "ARCHIVE_CONFIRM",
                            "教务确认归档", adminId, "ADMIN", null);
                }
            }
            a.setStatus("REVIEWED");
            a.setReviewBy(adminId);
            a.setReviewTime(now);
            a.setReviewNote(req == null ? null : req.getNote());
        } else {
            a.setStatus("RETURNED");
            a.setReturnBy(adminId);
            a.setReturnTime(now);
            a.setReturnReason(req == null ? null : req.getReason());
            a.setReviewNote(req == null ? null : req.getNote());
        }
        archiveMapper.updateById(a);
        auditService.record("ATTENDANCE_ARCHIVE", archiveId, approve ? "REVIEW" : "RETURN", null, null,
                (approve ? "教务确认归档" : "教务退回归档") + "：" + a.getPhase());
        return archiveVO(a);
    }

    @Transactional
    public ArchiveVO remind(Long archiveId, Long adminId) {
        AttendanceArchive a = archiveMapper.selectById(archiveId);
        if (a == null) {
            throw new BizException("归档批次不存在");
        }
        if ("REVIEWED".equals(a.getStatus())) {
            throw new BizException("已确认的批次不需要催办");
        }
        a.setRemindCount((a.getRemindCount() == null ? 0 : a.getRemindCount()) + 1);
        a.setLastRemindTime(LocalDateTime.now());
        archiveMapper.updateById(a);
        auditService.record("ATTENDANCE_ARCHIVE", archiveId, "REMIND", null, null,
                "第 " + a.getRemindCount() + " 次催办提交归档");
        return archiveVO(a);
    }

    /** 覆盖率督办清单 */
    public List<CoverageVO> coverage(Long termId) {
        List<TeachingClass> classes = classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery());
        List<CoverageVO> out = new ArrayList<>();
        for (TeachingClass tc : classes) {
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            if (o == null) {
                continue;
            }
            if (termId != null && !termId.equals(o.getTermId())) {
                continue;
            }
            CoverageVO v = new CoverageVO();
            v.setTeachingClassId(tc.getId());
            v.setClassCode(tc.getClassCode());
            v.setClassName(tc.getClassName());
            Course c = courseMapper.selectById(o.getCourseId());
            v.setCourseName(c == null ? null : c.getCourseName());
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(t == null ? null : t.getTeacherName());
            v.setEffectiveSessions(o.getEffectiveSessions());
            List<CourseSession> sessions = sessionsOf(tc);
            long passed = sessions.stream().filter(s -> s.getSessionDate() != null
                    && !s.getSessionDate().isAfter(LocalDate.now())).count();
            long called = sessions.stream().filter(s -> s.getSessionDate() != null
                    && !s.getSessionDate().isAfter(LocalDate.now())
                    && ("CALLED".equals(s.getRollCallStatus()) || "PARTIAL".equals(s.getRollCallStatus()))).count();
            v.setPassedSessions((int) passed);
            v.setCalledSessions((int) called);
            v.setCoverageRate(passed == 0 ? null
                    : BigDecimal.valueOf(called * 100.0 / passed).setScale(2, RoundingMode.HALF_UP));
            v.setLowCoverage(passed > 0 && called * 3 < passed);
            v.setArchivePending(archiveMapper.selectCount(Wrappers.<AttendanceArchive>lambdaQuery()
                    .eq(AttendanceArchive::getTeachingClassId, tc.getId())
                    .in(AttendanceArchive::getStatus, List.of("PENDING", "SUBMITTED"))
                    .eq(AttendanceArchive::getIsVoid, 0)).intValue());
            out.add(v);
        }
        return out;
    }

    // ============================================================ 学生端
    public List<StudentAttendanceVO> myAttendance(Long studentId) {
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
        List<StudentAttendanceVO> out = new ArrayList<>();
        for (Enrollment e : ens) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            if (tc == null) {
                continue;
            }
            StudentAttendanceVO v = new StudentAttendanceVO();
            v.setTeachingClassId(tc.getId());
            CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
            if (o != null) {
                Course c = courseMapper.selectById(o.getCourseId());
                v.setCourseName(c == null ? null : c.getCourseName());
            }
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(t == null ? null : t.getTeacherName());
            List<CourseSession> sessions = sessionsOf(tc);
            v.setTotalSessions(sessions.size());
            v.setCalledSessions((int) sessions.stream()
                    .filter(s -> "CALLED".equals(s.getRollCallStatus()) || "PARTIAL".equals(s.getRollCallStatus()))
                    .count());
            List<AttendanceRecord> rs = recordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                    .eq(AttendanceRecord::getEnrollmentId, e.getId())
                    .eq(AttendanceRecord::getIsVoid, 0));
            v.setPresentCount(countStatus(rs, PRESENT));
            v.setLateCount(countStatus(rs, LATE));
            v.setAbsentCount(countStatus(rs, ABSENT));
            v.setLeaveCount(countStatus(rs, LEAVE));
            int absent = countStatus(rs, ABSENT);
            int leave = countStatus(rs, LEAVE);
            int late = countStatus(rs, LATE);
            v.setDeductPoints(late + absent * 2 + Math.max(0, leave - 3));
            int maxAbsent = maxAbsent(tc);
            v.setMaxAbsent(maxAbsent);
            v.setThresholdHint(hintOf(absent, maxAbsent));
            Map<Long, CourseSession> sessionMap = sessions.stream()
                    .collect(Collectors.toMap(CourseSession::getId, s -> s, (a, b) -> a));
            List<StudentAttendanceVO.Row> rows = new ArrayList<>();
            for (AttendanceRecord r : rs) {
                CourseSession s = sessionMap.get(r.getSessionId());
                if (s == null) {
                    continue;
                }
                StudentAttendanceVO.Row row = new StudentAttendanceVO.Row();
                row.setId(r.getId());
                row.setWeekNo(s.getWeekNo());
                row.setSessionNo(s.getSessionNo());
                row.setSessionDate(s.getSessionDate());
                row.setStatus(r.getStatus());
                row.setLeaveType(r.getLeaveType());
                row.setPhase(r.getPhase());
                rows.add(row);
            }
            rows.sort((x, y) -> Integer.compare(y.getSessionNo() == null ? 0 : y.getSessionNo(),
                    x.getSessionNo() == null ? 0 : x.getSessionNo()));
            v.setRecords(rows);
            out.add(v);
        }
        return out;
    }

    // ============================================================ 内部
    private List<CourseSession> sessionsOf(TeachingClass tc) {
        return sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, tc.getId())
                .eq(CourseSession::getIsVoid, 0)
                .orderByAsc(CourseSession::getSessionNo));
    }

    private CourseSession requireSession(TeachingClass tc, Long sessionId) {
        CourseSession s = sessionMapper.selectById(sessionId);
        if (s == null) {
            throw new BizException("课次不存在");
        }
        if (!tc.getId().equals(s.getTeachingClassId())) {
            throw new BizException("课次不属于该教学班");
        }
        return s;
    }

    private List<Enrollment> enrollmentsOf(Long teachingClassId) {
        return enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId)
                .orderByAsc(Enrollment::getId));
    }

    private int enrollmentCount(Long teachingClassId) {
        return enrollmentMapper.selectCount(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId)).intValue();
    }

    private Map<Long, Student> studentsOf(List<Enrollment> ens) {
        Map<Long, Student> out = new LinkedHashMap<>();
        if (ens.isEmpty()) {
            return out;
        }
        for (Student st : studentMapper.selectBatchIds(ens.stream().map(Enrollment::getStudentId)
                .distinct().collect(Collectors.toList()))) {
            out.put(st.getId(), st);
        }
        return out;
    }

    private List<AttendanceRecord> recordsOfSession(Long sessionId) {
        return recordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getSessionId, sessionId)
                .eq(AttendanceRecord::getIsVoid, 0));
    }

    private List<AttendanceRecord> recordsOfClass(Long teachingClassId) {
        List<Long> ids = enrollmentsOf(teachingClassId).stream().map(Enrollment::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return recordMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .in(AttendanceRecord::getEnrollmentId, ids)
                .eq(AttendanceRecord::getIsVoid, 0));
    }

    private Map<Long, Integer> absentTotals(Long teachingClassId) {
        Map<Long, Integer> out = new LinkedHashMap<>();
        for (AttendanceRecord r : recordsOfClass(teachingClassId)) {
            if (ABSENT.equals(r.getStatus())) {
                out.merge(r.getStudentId(), 1, Integer::sum);
            }
        }
        return out;
    }

    private int countStatus(List<AttendanceRecord> records, String status) {
        return (int) records.stream().filter(r -> status.equals(r.getStatus())).count();
    }

    private int maxAbsent(TeachingClass tc) {
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        int effective = o == null || o.getEffectiveSessions() == null ? 0 : o.getEffectiveSessions();
        if (effective == 0) {
            effective = sessionsOf(tc).size();
        }
        return effective / 3;
    }

    /** 阈值提示：不显示剩余次数，只给结论（学生端口径） */
    private String hintOf(int absent, int maxAbsent) {
        if (maxAbsent <= 0) {
            return null;
        }
        if (absent >= maxAbsent + 1) {
            return "缺课已超过 1/3，按学籍规定不能参加期末考核";
        }
        if (absent >= maxAbsent) {
            return "再缺一次即挂";
        }
        return null;
    }

    private BigDecimal coverageOf(TeachingClass tc) {
        List<CourseSession> sessions = sessionsOf(tc);
        long passed = sessions.stream().filter(s -> s.getSessionDate() != null
                && !s.getSessionDate().isAfter(LocalDate.now())).count();
        long called = sessions.stream().filter(s -> s.getSessionDate() != null
                && !s.getSessionDate().isAfter(LocalDate.now())
                && ("CALLED".equals(s.getRollCallStatus()) || "PARTIAL".equals(s.getRollCallStatus()))).count();
        if (passed == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(called * 100.0 / passed).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeStatus(String text) {
        if (text == null) {
            return null;
        }
        String s = text.trim().toUpperCase();
        if (s.isEmpty()) {
            return PRESENT;
        }
        // 顺序很关键：「迟到」「未到」都含「到」，所以先判非出勤的状态
        if (s.equals(LATE) || s.contains("迟到")) {
            return LATE;
        }
        if (s.equals(ABSENT) || s.contains("缺课") || s.contains("旷课") || s.contains("未到")) {
            return ABSENT;
        }
        if (s.equals(LEAVE) || s.contains("请假")) {
            return LEAVE;
        }
        if (s.equals(PRESENT) || s.contains("出勤") || s.contains("正常") || s.contains("到课") || s.contains("已到")) {
            return PRESENT;
        }
        return null;
    }

    private String mapLeaveType(String text) {
        if (text == null) {
            return "PERSONAL";
        }
        String s = text.trim();
        if (s.contains("病")) {
            return "SICK";
        }
        if (s.contains("公")) {
            return "OFFICIAL";
        }
        return "PERSONAL";
    }

    private void writeLog(AttendanceRecord r, String oldStatus, String newStatus, String oldPhase,
                          String newPhase, String changeType, String reason, Long operatorId,
                          String role, String batchKey) {
        AttendanceRecordLog log = new AttendanceRecordLog();
        log.setRecordId(r.getId());
        log.setEnrollmentId(r.getEnrollmentId());
        log.setStudentId(r.getStudentId());
        log.setSessionId(r.getSessionId());
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOldPhase(oldPhase);
        log.setNewPhase(newPhase);
        log.setChangeType(changeType);
        log.setReason(reason);
        log.setChangedBy(operatorId);
        log.setChangedRole(role);
        log.setChangedAt(LocalDateTime.now());
        log.setBatchKey(batchKey);
        logMapper.insert(log);
    }

    private ArchiveVO archiveVO(AttendanceArchive a) {
        ArchiveVO v = new ArchiveVO();
        v.setId(a.getId());
        v.setOfferingId(a.getOfferingId());
        v.setTeachingClassId(a.getTeachingClassId());
        TeachingClass tc = a.getTeachingClassId() == null ? null : classMapper.selectById(a.getTeachingClassId());
        if (tc != null) {
            v.setClassCode(tc.getClassCode());
            v.setClassName(tc.getClassName());
            Teacher t = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(t == null ? null : t.getTeacherName());
        }
        CourseOffering o = a.getOfferingId() == null ? null : offeringMapper.selectById(a.getOfferingId());
        if (o != null) {
            Course c = courseMapper.selectById(o.getCourseId());
            v.setCourseName(c == null ? null : c.getCourseName());
        }
        Term term = a.getTermId() == null ? null : termMapper.selectById(a.getTermId());
        v.setTermName(term == null ? null : term.getTermName());
        v.setPhase(a.getPhase());
        v.setStatus(a.getStatus());
        v.setSubmitTime(a.getSubmitTime());
        v.setSubmitNote(a.getSubmitNote());
        v.setSessionFrom(a.getSessionFrom());
        v.setSessionTo(a.getSessionTo());
        v.setSessionCount(a.getSessionCount());
        v.setRecordCount(a.getRecordCount());
        v.setCoverageRate(a.getCoverageRate());
        v.setReviewTime(a.getReviewTime());
        v.setReviewNote(a.getReviewNote());
        v.setReturnTime(a.getReturnTime());
        v.setReturnReason(a.getReturnReason());
        v.setRemindCount(a.getRemindCount());
        v.setDeadline(a.getDeadline());
        return v;
    }

    // ------------------------------------------------------------ 文件解析
    private List<List<String>> readCsv(MultipartFile file) {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("[,，\\t]");
                List<String> row = new ArrayList<>();
                for (String p : parts) {
                    row.add(p.trim());
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new BizException("读取 CSV 失败：" + e.getMessage());
        }
        return rows;
    }

    private List<List<String>> readXlsx(MultipartFile file) {
        List<List<String>> rows = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (int c = 0; c < Math.max(4, row.getLastCellNum()); c++) {
                    Cell cell = row.getCell(c);
                    cells.add(cell == null ? "" : cellText(cell));
                }
                rows.add(cells);
            }
        } catch (IOException e) {
            throw new BizException("读取 Excel 失败：" + e.getMessage());
        }
        return rows;
    }

    private String cellText(Cell cell) {
        switch (cell.getCellType()) {
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return cell.getBooleanCellValue() ? "TRUE" : "FALSE";
            default:
                return "";
        }
    }

    private String cell(List<String> row, int i) {
        return i < row.size() && row.get(i) != null ? row.get(i).trim() : "";
    }
}
`);

console.log('考勤模块：服务层生成完成');
