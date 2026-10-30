package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.ScoreEntryBatchRequest;
import com.example.academic_risk_warning.dto.ScoreEntryDTO;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ProjectSubmission;
import com.example.academic_risk_warning.entity.ProjectTask;
import com.example.academic_risk_warning.entity.ScoreItem;
import com.example.academic_risk_warning.entity.ScoreResult;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AttendanceRecordMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.ProjectSubmissionMapper;
import com.example.academic_risk_warning.mapper.ProjectTaskMapper;
import com.example.academic_risk_warning.mapper.ScoreItemMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.vo.PendingGradingVO;
import com.example.academic_risk_warning.vo.ScoreCellVO;
import com.example.academic_risk_warning.vo.ScoreItemColumnVO;
import com.example.academic_risk_warning.vo.ScoreMatrixVO;
import com.example.academic_risk_warning.vo.ScoreRowVO;
import com.example.academic_risk_warning.vo.ScoreSummaryVO;
import com.example.academic_risk_warning.vo.StudentScoreVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 成绩与批改：成绩矩阵（分项 → 加权 → 减扣分 → 总评）与总评汇总。
 * 口径：
 * ① 加权分 W = Σ(得分/满分×100×权重)（权重按构成的两级逐层折算）；扣分 P = 2×缺课+1×迟到+1×max(0,请假−3)；
 * ② 总评 T = W − P，不截断；及格线取开课实例的 pass_score；
 * ③ 缺课 > N_max（floor(有效课次/3)）→ 硬门槛挂科，不看分数；
 * ④ 缺考/未交按 0 分计（不是"没录入"）；未录入的分项在总评汇总前必须补齐，否则状态为 NO_SCORE；
 * ⑤ 折算分不落库（score_result 只存判定结果留档），Δ 与教师尺度只在教务端展示。
 */
@Service
@RequiredArgsConstructor
public class ScoreMatrixService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String ENGINE_VERSION = "1-10.v1";
    /** M16 折算口径版本：按已录项重算的成绩单用这个版本号，和旧的 1-10.v1 可区分 */
    private static final String M16_ENGINE_VERSION = "m16.v1";

    private final ScoreSchemeItemMapper schemeItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final TermMapper termMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final PaperMapper paperMapper;
    private final SubmissionMapper submissionMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final ScoreCalculator scoreCalculator;
    private final CourseContentService contentService;
    private final AuditService auditService;

    // ============================================================ 待批改
    public PendingGradingVO pending(Long teachingClassId, Long teacherId) {
        contentService.requireMyClass(teachingClassId, teacherId);
        PendingGradingVO vo = new PendingGradingVO();
        List<PendingGradingVO.PaperPending> papers = new ArrayList<>();
        for (Paper p : paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                .eq(Paper::getTeachingClassId, teachingClassId)
                .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED")))) {
            List<Submission> subs = submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                    .eq(Submission::getPaperId, p.getId()));
            int pending = 0;
            for (Submission s : subs) {
                if ("SYSTEM_MARKED".equals(s.getMarkedStatus())) {
                    pending++;
                }
            }
            if (pending > 0) {
                PendingGradingVO.PaperPending pp = new PendingGradingVO.PaperPending();
                pp.setPaperId(p.getId());
                pp.setPaperName(p.getPaperName());
                pp.setPaperType(p.getPaperType());
                pp.setPendingCount(pending);
                pp.setSubmittedCount(subs.size());
                papers.add(pp);
            }
        }
        List<PendingGradingVO.ProjectPending> projects = new ArrayList<>();
        for (ProjectTask t : projectTaskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                .eq(ProjectTask::getTeachingClassId, teachingClassId)
                .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED")))) {
            long submitted = projectSubmissionMapper.selectCount(Wrappers.<ProjectSubmission>lambdaQuery()
                    .eq(ProjectSubmission::getProjectId, t.getId())
                    .eq(ProjectSubmission::getSubmitStatus, "SUBMITTED"));
            if (submitted > 0) {
                PendingGradingVO.ProjectPending pp = new PendingGradingVO.ProjectPending();
                pp.setProjectId(t.getId());
                pp.setProjectName(t.getProjectName());
                pp.setSubmittedCount((int) submitted);
                if (t.getEndTime() != null) {
                    pp.setDurationDays((int) java.time.Duration.between(t.getEndTime(), LocalDateTime.now()).toDays());
                }
                projects.add(pp);
            }
        }
        vo.setPapers(papers);
        vo.setProjects(projects);
        vo.setPaperPending(papers.stream().mapToInt(PendingGradingVO.PaperPending::getPendingCount).sum());
        vo.setProjectPending(projects.stream().mapToInt(PendingGradingVO.ProjectPending::getSubmittedCount).sum());
        return vo;
    }

    // ============================================================ 成绩矩阵
    public ScoreMatrixVO matrix(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        return buildMatrix(tc);
    }

    private ScoreMatrixVO buildMatrix(TeachingClass tc) {
        // 折算统一走 ScoreCalculator：成绩单页与风险判定引擎共用一套，避免两套数
        ScoreCalculator.ClassScore sc = scoreCalculator.compute(tc);
        List<ScoreItemColumnVO> columns = sc.columns;
        List<Enrollment> ens = sc.enrollments;
        Map<Long, Student> students = sc.students;
        Map<Long, int[]> attendance = attendanceOf(tc, ens);
        int effective = sc.effectiveSessions;
        int allowedAbsent = effective / 3;
        int passScore = sc.passScore;

        Map<Long, ScoreResult> persisted = new LinkedHashMap<>();
        if (!ens.isEmpty()) {
            for (ScoreResult sr : scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                    .in(ScoreResult::getEnrollmentId, ens.stream().map(Enrollment::getId)
                            .collect(Collectors.toList()))
                    .eq(ScoreResult::getIsVoid, 0))) {
                persisted.put(sr.getEnrollmentId(), sr);
            }
        }
        List<ScoreRowVO> rows = new ArrayList<>();
        List<String> missingGlobal = new ArrayList<>();
        for (Enrollment e : ens) {
            ScoreRowVO row = new ScoreRowVO();
            row.setEnrollmentId(e.getId());
            row.setStudentId(e.getStudentId());
            Student st = students.get(e.getStudentId());
            row.setStudentNo(st == null ? null : st.getStudentNo());
            row.setStudentName(st == null ? null : st.getStudentName());
            ScoreCalculator.StudentScore ss = sc.byEnrollment.get(e.getId());
            Map<Long, ScoreCellVO> cells = ss == null ? new LinkedHashMap<>() : ss.cells;
            List<String> missing = ss == null ? new ArrayList<>() : ss.missingPaths;
            row.setCells(cells);
            BigDecimal weighted = ss == null ? BigDecimal.ZERO : ss.weightedScore;
            int[] att = attendance.getOrDefault(e.getStudentId(), new int[]{0, 0, 0});
            int absent = att[0];
            int late = att[1];
            int leave = att[2];
            BigDecimal deduction = BigDecimal.valueOf(2L * absent + late + Math.max(0, leave - 3));
            row.setWeightedScore(weighted);
            row.setDeductionPoints(deduction);
            row.setTotalScore(weighted.subtract(deduction).setScale(2, RoundingMode.HALF_UP));
            row.setAbsentCount(absent);
            row.setLateCount(late);
            row.setLeaveCount(leave);
            row.setAllowedAbsent(allowedAbsent);
            row.setEffectiveSessions(effective);
            boolean hardFail = allowedAbsent > 0 && absent > allowedAbsent;
            row.setHardFail(hardFail);
            boolean incomplete = !missing.isEmpty();
            row.setMissingItems(missing);
            if (incomplete) {
                for (String name : missing) {
                    if (!missingGlobal.contains(name)) {
                        missingGlobal.add(name);
                    }
                }
                row.setScoreStatus("NO_SCORE");
                row.setFail(false);
                row.setFailReason("NO_SCORE");
            } else {
                boolean fail = hardFail || row.getTotalScore().compareTo(BigDecimal.valueOf(passScore)) < 0;
                ScoreResult finished = persisted.get(e.getId());
                boolean committed = finished != null && "FINAL".equals(finished.getScoreStatus());
                row.setScoreStatus(committed ? "FINAL" : "PENDING");
                row.setFail(fail);
                row.setFailReason(!fail ? null : (hardFail ? "HARD_LINE" : "SCORE"));
            }
            rows.add(row);
        }
        ScoreMatrixVO vo = new ScoreMatrixVO();
        vo.setTeachingClassId(tc.getId());
        vo.setCourseName(courseName(tc));
        vo.setClassName(tc.getClassName());
        Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
        vo.setTeacherName(teacher == null ? null : teacher.getTeacherName());
        vo.setItems(columns);
        vo.setRows(rows);
        vo.setSummary(summary(rows, passScore, sc.scheme, missingGlobal));
        return vo;
    }

    private ScoreSummaryVO summary(List<ScoreRowVO> rows, int passScore, ScoreScheme scheme, List<String> missing) {
        ScoreSummaryVO s = new ScoreSummaryVO();
        s.setStudentCount(rows.size());
        long scored = rows.stream().filter(r -> !"NO_SCORE".equals(r.getScoreStatus())).count();
        s.setScoredCount((int) scored);
        s.setIncompleteCount(rows.size() - (int) scored);
        long fails = rows.stream().filter(r -> Boolean.TRUE.equals(r.getFail())).count();
        long hard = rows.stream().filter(r -> Boolean.TRUE.equals(r.getHardFail())).count();
        s.setFailCount((int) fails);
        s.setHardFailCount((int) hard);
        s.setPassScore(passScore);
        s.setMissingItemNames(missing);
        if (scored > 0) {
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal max = null;
            BigDecimal min = null;
            for (ScoreRowVO r : rows) {
                if ("NO_SCORE".equals(r.getScoreStatus())) {
                    continue;
                }
                sum = sum.add(r.getTotalScore());
                max = max == null || r.getTotalScore().compareTo(max) > 0 ? r.getTotalScore() : max;
                min = min == null || r.getTotalScore().compareTo(min) < 0 ? r.getTotalScore() : min;
            }
            s.setAvgTotal(sum.divide(BigDecimal.valueOf(scored), 2, RoundingMode.HALF_UP));
            s.setMaxTotal(max);
            s.setMinTotal(min);
            long passed = scored - fails;
            s.setPassRate(BigDecimal.valueOf(passed * 100.0 / scored).setScale(2, RoundingMode.HALF_UP));
        }
        s.setSchemeLocked(scheme != null && scheme.getLocked() != null && scheme.getLocked() == 1);
        s.setSchemeVersion(scheme == null || scheme.getVersion() == null ? null : "v" + scheme.getVersion());
        return s;
    }

    // ============================================================ 手工录入
    @Transactional
    public ScoreMatrixVO saveEntries(ScoreEntryBatchRequest req, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(req.getTeachingClassId(), teacherId);
        ScoreSchemeItem item = schemeItemMapper.selectById(req.getSchemeItemId());
        if (item == null) {
            throw new BizException("评分项不存在");
        }
        if ("AUTO".equalsIgnoreCase(item.getDataSource())) {
            throw new BizException("该项是自动汇总项（测验/作业/项目），不能手工录入");
        }
        int timesNo = req.getTimesNo() == null ? 1 : req.getTimesNo();
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, tc.getId()));
        Map<Long, Enrollment> byStudent = ens.stream()
                .collect(Collectors.toMap(Enrollment::getStudentId, e -> e, (a, b) -> a));
        BigDecimal full = item.getFullScore() == null ? HUNDRED : item.getFullScore();
        LocalDateTime now = LocalDateTime.now();
        int saved = 0;
        if (req.getItems() != null) {
            for (ScoreEntryDTO d : req.getItems()) {
                Enrollment e = byStudent.get(d.getStudentId());
                if (e == null) {
                    continue;
                }
                ScoreItem row = scoreItemMapper.selectOne(Wrappers.<ScoreItem>lambdaQuery()
                        .eq(ScoreItem::getEnrollmentId, e.getId())
                        .eq(ScoreItem::getSchemeItemId, item.getId())
                        .eq(ScoreItem::getTimesNo, timesNo)
                        .eq(ScoreItem::getIsVoid, 0).last("limit 1"));
                boolean absent = d.getIsAbsent() != null && d.getIsAbsent() == 1;
                BigDecimal score = d.getScore();
                if (absent) {
                    score = BigDecimal.ZERO;
                }
                if (score != null) {
                    if (score.signum() < 0) {
                        score = BigDecimal.ZERO;
                    }
                    if (score.compareTo(full) > 0) {
                        score = full;
                    }
                }
                if (row == null) {
                    row = new ScoreItem();
                    row.setEnrollmentId(e.getId());
                    row.setStudentId(d.getStudentId());
                    row.setSchemeItemId(item.getId());
                    row.setTermId(contentService.termIdOf(tc));
                    row.setTimesNo(timesNo);
                    row.setRawValue(d.getRawValue());
                    row.setScore(score);
                    row.setFullScore(full);
                    row.setIsAbsent(absent ? 1 : 0);
                    row.setSource("MANUAL");
                    row.setScorerId(teacherId);
                    row.setScorerRole("TEACHER");
                    row.setScoredAt(now);
                    row.setIsVoid(0);
                    row.setRemark(d.getRemark());
                    if (score == null && !absent) {
                        continue;
                    }
                    scoreItemMapper.insert(row);
                } else {
                    row.setScore(score);
                    row.setRawValue(d.getRawValue());
                    row.setFullScore(full);
                    row.setIsAbsent(absent ? 1 : 0);
                    row.setScorerId(teacherId);
                    row.setScorerRole("TEACHER");
                    row.setScoredAt(now);
                    row.setRemark(d.getRemark());
                    scoreItemMapper.updateById(row);
                }
                saved++;
            }
        }
        auditService.record("SCORE_ITEM", item.getId(), "ENTRY", null, null,
                "录入评分项「" + item.getItemName() + "」第 " + timesNo + " 次，共 " + saved + " 行");
        return buildMatrix(tc);
    }

    // ============================================================ 总评汇总
    @Transactional
    public ScoreMatrixVO commitSummary(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        ScoreMatrixVO matrix = buildMatrix(tc);
        if (!matrix.getSummary().getMissingItemNames().isEmpty()) {
            throw new BizException("还有评分项没有录入：" + String.join("、", matrix.getSummary().getMissingItemNames())
                    + "；请先补齐再汇总（缺考/缺交请标记为缺考，按 0 分计）");
        }
        CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
        int passScore = offering == null || offering.getPassScore() == null ? 60 : offering.getPassScore();
        LocalDateTime now = LocalDateTime.now();
        int written = 0;
        for (ScoreRowVO row : matrix.getRows()) {
            ScoreResult r = scoreResultMapper.selectOne(Wrappers.<ScoreResult>lambdaQuery()
                    .eq(ScoreResult::getEnrollmentId, row.getEnrollmentId()).last("limit 1"));
            if (r == null) {
                r = new ScoreResult();
                r.setEnrollmentId(row.getEnrollmentId());
                r.setStudentId(row.getStudentId());
                r.setOfferingId(tc.getOfferingId());
                r.setTermId(contentService.termIdOf(tc));
                r.setIsVoid(0);
            }
            r.setWeightedScore(row.getWeightedScore());
            r.setDeductionPoints(row.getDeductionPoints());
            r.setTotalScore(row.getTotalScore());
            r.setPassScore(passScore);
            r.setAbsentCount(row.getAbsentCount());
            r.setLateCount(row.getLateCount());
            r.setLeaveCount(row.getLeaveCount());
            r.setAllowedAbsent(row.getAllowedAbsent());
            r.setEffectiveSessions(row.getEffectiveSessions());
            boolean hardFail = Boolean.TRUE.equals(row.getHardFail());
            r.setHardFail(hardFail ? 1 : 0);
            boolean fail = row.getTotalScore().compareTo(BigDecimal.valueOf(passScore)) < 0 || hardFail;
            r.setIsFail(fail ? 1 : 0);
            r.setFailReason(!fail ? null : (hardFail ? "HARD_LINE" : "SCORE"));
            r.setScoreStatus("FINAL");
            r.setEngineVersion(ENGINE_VERSION);
            r.setCalculatedAt(now);
            if (r.getId() == null) {
                scoreResultMapper.insert(r);
            } else {
                scoreResultMapper.updateById(r);
            }
            written++;
        }
        auditService.record("SCORE_RESULT", tc.getId(), "COMMIT", null, null,
                "生成总评 " + written + " 条（及格线 " + passScore + "，引擎 " + ENGINE_VERSION + "）");
        ScoreMatrixVO after = buildMatrix(tc);
        return after;
    }

    // ============================================================ 按已录项重算（M16）
    /**
     * 按"已录项"重算成绩单 `score_result`。
     *
     * <p>为什么要它：{@link #commitSummary} 要求所有分项齐全才允许汇总，而第 9 周期末还没考，
     * 于是 `score_result` 长期停在"含期末预置分"的旧值，成绩单与预警判定的数对不上。</p>
     *
     * <p>这里的口径：只用**已录项**折算当前已得加权分（未录项按 0，不做归一），总评 = 加权分 − 考勤扣分；
     * 分项没录齐时 `score_status = PARTIAL` 且**不按分数判挂**（只保留缺课硬线），齐全时才是 `FINAL`。
     * 折算口径版本记 `m16.v1`，与旧值（`1-10.v1`）可区分。</p>
     *
     * @param termId 只重算该学期的开课实例；null 表示全部
     * @return 写入（新增 + 覆盖）的条数
     */
    @Transactional
    public int recomputeByRecorded(Long termId) {
        int written = 0;
        LocalDateTime now = LocalDateTime.now();
        for (TeachingClass tc : classMapper.selectList(Wrappers.<TeachingClass>lambdaQuery())) {
            CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
            if (offering == null || (termId != null && !termId.equals(offering.getTermId()))) {
                continue;
            }
            ScoreCalculator.ClassScore sc;
            try {
                sc = scoreCalculator.compute(tc);
            } catch (BizException e) {
                // 该课没配成绩构成，跳过（不能因为一门课没配就把整批重算打断）
                continue;
            }
            Map<Long, int[]> attendance = attendanceOf(tc, sc.enrollments);
            int passScore = sc.passScore;
            int allowed = sc.effectiveSessions / 3;
            for (Enrollment e : sc.enrollments) {
                ScoreCalculator.StudentScore ss = sc.byEnrollment.get(e.getId());
                if (ss == null) {
                    continue;
                }
                int[] att = attendance.getOrDefault(e.getStudentId(), new int[]{0, 0, 0});
                BigDecimal deduction = BigDecimal.valueOf(2L * att[0] + att[1] + Math.max(0, att[2] - 3));
                BigDecimal weighted = ss.weightedScore == null ? BigDecimal.ZERO : ss.weightedScore;
                BigDecimal total = weighted.subtract(deduction).setScale(2, RoundingMode.HALF_UP);
                boolean hardFail = allowed > 0 && att[0] > allowed;
                boolean complete = ss.missingPaths == null || ss.missingPaths.isEmpty();

                ScoreResult r = scoreResultMapper.selectOne(Wrappers.<ScoreResult>lambdaQuery()
                        .eq(ScoreResult::getEnrollmentId, e.getId()).last("limit 1"));
                if (r == null) {
                    r = new ScoreResult();
                    r.setEnrollmentId(e.getId());
                    r.setStudentId(e.getStudentId());
                    r.setOfferingId(tc.getOfferingId());
                    r.setTermId(offering.getTermId());
                    r.setIsVoid(0);
                }
                r.setWeightedScore(weighted);
                r.setDeductionPoints(deduction);
                r.setTotalScore(total);
                r.setPassScore(passScore);
                r.setAbsentCount(att[0]);
                r.setLateCount(att[1]);
                r.setLeaveCount(att[2]);
                r.setAllowedAbsent(allowed);
                r.setEffectiveSessions(sc.effectiveSessions);
                r.setHardFail(hardFail ? 1 : 0);
                boolean fail = hardFail || (complete && total.compareTo(BigDecimal.valueOf(passScore)) < 0);
                r.setIsFail(fail ? 1 : 0);
                r.setFailReason(!fail ? null : (hardFail ? "HARD_LINE" : "SCORE"));
                r.setScoreStatus(complete ? "FINAL" : "PARTIAL");
                r.setEngineVersion(M16_ENGINE_VERSION);
                r.setCalculatedAt(now);
                if (r.getId() == null) {
                    scoreResultMapper.insert(r);
                } else {
                    scoreResultMapper.updateById(r);
                }
                written++;
            }
        }
        auditService.record("SCORE_RESULT", termId, "RECOMPUTE", null, null,
                "按已录项重算成绩单：" + written + " 条（分项未齐记 PARTIAL，不计分数挂科），口径 " + M16_ENGINE_VERSION);
        return written;
    }

    // ============================================================ 导出
    public String exportCsv(Long teachingClassId, Long teacherId) {
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        ScoreMatrixVO m = buildMatrix(tc);
        StringBuilder sb = new StringBuilder();
        sb.append("学号,姓名");
        for (ScoreItemColumnVO c : m.getItems()) {
            sb.append(',').append(csv(c.getPath())).append("(百分制)");
        }
        sb.append(",加权分,考勤扣分,总评,缺课,迟到,请假,允许缺课,状态,挂科原因\n");
        for (ScoreRowVO r : m.getRows()) {
            sb.append(csv(r.getStudentNo())).append(',').append(csv(r.getStudentName()));
            for (ScoreItemColumnVO c : m.getItems()) {
                ScoreCellVO cell = r.getCells().get(c.getSchemeItemId());
                sb.append(',');
                if (cell == null || cell.getMissing() || cell.getScore() == null) {
                    sb.append("");
                } else {
                    sb.append(cell.getScore().setScale(1, RoundingMode.HALF_UP));
                }
            }
            sb.append(',').append(r.getWeightedScore())
                    .append(',').append(r.getDeductionPoints())
                    .append(',').append(r.getTotalScore())
                    .append(',').append(r.getAbsentCount())
                    .append(',').append(r.getLateCount())
                    .append(',').append(r.getLeaveCount())
                    .append(',').append(r.getAllowedAbsent())
                    .append(',').append(r.getScoreStatus())
                    .append(',').append(reasonText(r.getFailReason()))
                    .append('\n');
        }
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // ============================================================ 学生端
    public List<StudentScoreVO> studentScores(Long studentId) {
        // 我的成绩只看当前学期：历史学期与合成样本学期不再混排
        Term current = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1)
                .orderByDesc(Term::getId)
                .last("limit 1"));
        Long currentTermId = current == null ? null : current.getId();
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId)
                .eq(currentTermId != null, Enrollment::getTermId, currentTermId));
        List<StudentScoreVO> out = new ArrayList<>();
        for (Enrollment e : ens) {
            TeachingClass tc = classMapper.selectById(e.getTeachingClassId());
            if (tc == null) {
                continue;
            }
            ScoreMatrixVO m = buildMatrix(tc);
            ScoreRowVO mine = m.getRows().stream()
                    .filter(r -> e.getId().equals(r.getEnrollmentId())).findFirst().orElse(null);
            if (mine == null) {
                continue;
            }
            StudentScoreVO v = new StudentScoreVO();
            v.setTeachingClassId(tc.getId());
            v.setCourseName(courseName(tc));
            Teacher teacher = teacherMapper.selectById(tc.getTeacherId());
            v.setTeacherName(teacher == null ? null : teacher.getTeacherName());
            CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
            if (offering != null) {
                Term term = termMapper.selectById(offering.getTermId());
                v.setTermName(term == null ? null : term.getTermName());
            }
            v.setPassScore(m.getSummary().getPassScore());
            v.setWeightedScore(mine.getWeightedScore());
            v.setDeductionPoints(mine.getDeductionPoints());
            v.setTotalScore(mine.getTotalScore());
            v.setAbsentCount(mine.getAbsentCount());
            v.setLateCount(mine.getLateCount());
            v.setLeaveCount(mine.getLeaveCount());
            v.setAllowedAbsent(mine.getAllowedAbsent());
            v.setHardFail(mine.getHardFail());
            v.setFail(mine.getFail());
            v.setFailReasonText(reasonText(mine.getFailReason()));
            v.setScoreStatus(mine.getScoreStatus());
            ScoreResult persisted = scoreResultMapper.selectOne(Wrappers.<ScoreResult>lambdaQuery()
                    .eq(ScoreResult::getEnrollmentId, e.getId())
                    .eq(ScoreResult::getIsVoid, 0).last("limit 1"));
            v.setScoreStatusText(persisted != null && "FINAL".equals(persisted.getScoreStatus())
                    ? "已定稿" : ("NO_SCORE".equals(mine.getScoreStatus()) ? "成绩未齐" : "教师尚未定稿"));
            List<StudentScoreVO.Item> items = new ArrayList<>();
            for (ScoreItemColumnVO c : m.getItems()) {
                ScoreCellVO cell = mine.getCells().get(c.getSchemeItemId());
                StudentScoreVO.Item it = new StudentScoreVO.Item();
                it.setItemName(c.getItemName());
                it.setPath(c.getPath());
                it.setWeight(c.getWeight());
                it.setScore(cell == null ? null : cell.getScore());
                it.setMissing(cell != null && Boolean.TRUE.equals(cell.getMissing()));
                if (cell != null && cell.getScore() != null && c.getWeight() != null) {
                    it.setWeightPercent(cell.getScore().multiply(c.getWeight())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP));
                }
                items.add(it);
            }
            v.setItems(items);
            out.add(v);
        }
        return out;
    }

    // ============================================================ 内部
    private String reasonText(String reason) {
        if (reason == null) {
            return "";
        }
        switch (reason) {
            case "HARD_LINE":
                return "缺课超过 1/3，硬门槛挂科";
            case "SCORE":
                return "总评低于及格线";
            case "NO_SCORE":
                return "成绩未齐";
            default:
                return reason;
        }
    }

    private String courseName(TeachingClass tc) {
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        if (o == null) {
            return null;
        }
        Course c = courseMapper.selectById(o.getCourseId());
        return c == null ? null : c.getCourseName();
    }

    /** 考勤计数 [缺课, 迟到, 请假] */
    private Map<Long, int[]> attendanceOf(TeachingClass tc, List<Enrollment> ens) {
        Map<Long, int[]> out = new LinkedHashMap<>();
        if (ens.isEmpty()) {
            return out;
        }
        List<AttendanceRecord> rows = attendanceMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .in(AttendanceRecord::getEnrollmentId, ens.stream().map(Enrollment::getId).collect(Collectors.toList()))
                .eq(AttendanceRecord::getIsVoid, 0));
        for (AttendanceRecord r : rows) {
            int[] c = out.computeIfAbsent(r.getStudentId(), k -> new int[]{0, 0, 0});
            if ("ABSENT".equals(r.getStatus())) {
                c[0]++;
            } else if ("LATE".equals(r.getStatus())) {
                c[1]++;
            } else if ("LEAVE".equals(r.getStatus())) {
                c[2]++;
            }
        }
        return out;
    }
}
