package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.dto.ScoreEntryBatchRequest;
import com.example.academic_risk_warning.dto.ScoreEntryDTO;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseGroup;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ProjectContribution;
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
import com.example.academic_risk_warning.mapper.CourseGroupMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.ProjectContributionMapper;
import com.example.academic_risk_warning.mapper.ProjectSubmissionMapper;
import com.example.academic_risk_warning.mapper.ProjectTaskMapper;
import com.example.academic_risk_warning.mapper.ScoreItemMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.util.JsonObjects;
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

    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseGroupMapper groupMapper;
    private final CourseMapper courseMapper;
    private final TeacherMapper teacherMapper;
    private final TermMapper termMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final PaperMapper paperMapper;
    private final SubmissionMapper submissionMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final ProjectContributionMapper projectContributionMapper;
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
        List<ScoreSchemeItem> all = schemeItems(tc);
        ScoreScheme scheme = schemeOf(tc);
        List<ScoreSchemeItem> roots = all.stream().filter(i -> i.getParentId() == null).collect(Collectors.toList());
        List<ScoreItemColumnVO> columns = new ArrayList<>();
        Map<Long, String> rootNames = new LinkedHashMap<>();
        for (ScoreSchemeItem r : roots) {
            rootNames.put(r.getId(), r.getItemName());
        }
        for (ScoreSchemeItem it : all) {
            boolean leaf = all.stream().noneMatch(x -> it.getId().equals(x.getParentId()));
            if (!leaf) {
                continue;
            }
            ScoreItemColumnVO c = new ScoreItemColumnVO();
            c.setSchemeItemId(it.getId());
            c.setItemName(it.getItemName());
            c.setPath(it.getParentId() == null ? it.getItemName()
                    : rootNames.getOrDefault(it.getParentId(), "") + " / " + it.getItemName());
            c.setCategory(it.getCategory());
            c.setWeight(it.getWeight());
            c.setDataSource(it.getDataSource());
            c.setScoreType(it.getScoreType());
            c.setFullScore(it.getFullScore() == null ? HUNDRED : it.getFullScore());
            c.setExpectedTimes(it.getExpectedTimes());
            c.setParentId(it.getParentId());
            columns.add(c);
        }
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, tc.getId())
                .orderByAsc(Enrollment::getId));
        List<Long> studentIds = ens.stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Student> students = new LinkedHashMap<>();
        if (!studentIds.isEmpty()) {
            for (Student s : studentMapper.selectBatchIds(studentIds)) {
                students.put(s.getId(), s);
            }
        }
        Map<Long, Map<Long, BigDecimal>> manual = manualScores(tc, ens);
        Map<Long, Map<Long, ScoreCellVO>> auto = autoScores(tc, columns, studentIds);
        Map<Long, int[]> attendance = attendanceOf(tc, ens);
        CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
        int effective = offering == null || offering.getEffectiveSessions() == null ? 0 : offering.getEffectiveSessions();
        int allowedAbsent = effective / 3;
        int passScore = offering == null || offering.getPassScore() == null ? 60 : offering.getPassScore();

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
            Map<Long, ScoreCellVO> cells = new LinkedHashMap<>();
            List<String> missing = new ArrayList<>();
            BigDecimal weighted = BigDecimal.ZERO;
            for (ScoreItemColumnVO c : columns) {
                ScoreCellVO cell = null;
                Map<Long, ScoreCellVO> autoRow = auto.get(e.getStudentId());
                if (autoRow != null && autoRow.containsKey(c.getSchemeItemId())) {
                    cell = autoRow.get(c.getSchemeItemId());
                } else {
                    Map<Long, BigDecimal> m = manual.get(e.getStudentId());
                    BigDecimal v = m == null ? null : m.get(c.getSchemeItemId());
                    if (v != null) {
                        cell = new ScoreCellVO();
                        cell.setScore(v);
                        cell.setManual(true);
                        cell.setSource("MANUAL");
                        cell.setMissing(false);
                    }
                }
                if (cell == null) {
                    cell = new ScoreCellVO();
                    cell.setMissing(true);
                    cell.setManual(true);
                    missing.add(c.getPath());
                }
                cells.put(c.getSchemeItemId(), cell);
            }
            row.setCells(cells);
            // 两级折算
            for (ScoreSchemeItem r : roots) {
                boolean isLeaf = all.stream().noneMatch(x -> r.getId().equals(x.getParentId()));
                BigDecimal rootPct;
                if (isLeaf) {
                    ScoreCellVO cell = cells.get(r.getId());
                    rootPct = cell == null || cell.getScore() == null ? BigDecimal.ZERO : cell.getScore();
                } else {
                    BigDecimal acc = BigDecimal.ZERO;
                    for (ScoreSchemeItem child : all) {
                        if (!r.getId().equals(child.getParentId())) {
                            continue;
                        }
                        ScoreCellVO cell = cells.get(child.getId());
                        BigDecimal score = cell == null || cell.getScore() == null ? BigDecimal.ZERO : cell.getScore();
                        BigDecimal w = child.getWeight() == null ? BigDecimal.ZERO : child.getWeight();
                        acc = acc.add(score.multiply(w).divide(HUNDRED, 4, RoundingMode.HALF_UP));
                    }
                    rootPct = acc;
                }
                BigDecimal rootWeight = r.getWeight() == null ? BigDecimal.ZERO : r.getWeight();
                weighted = weighted.add(rootPct.multiply(rootWeight).divide(HUNDRED, 4, RoundingMode.HALF_UP));
            }
            weighted = weighted.setScale(2, RoundingMode.HALF_UP);
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
        vo.setSummary(summary(rows, passScore, scheme, missingGlobal));
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
        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, studentId));
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

    private ScoreScheme schemeOf(TeachingClass tc) {
        CourseOffering o = offeringMapper.selectById(tc.getOfferingId());
        if (o == null) {
            return null;
        }
        CourseGroup group = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                .eq(CourseGroup::getCourseId, o.getCourseId())
                .eq(CourseGroup::getTermId, o.getTermId()).last("limit 1"));
        if (group == null) {
            return null;
        }
        return schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, group.getId())
                .orderByDesc(ScoreScheme::getVersion).last("limit 1"));
    }

    private List<ScoreSchemeItem> schemeItems(TeachingClass tc) {
        ScoreScheme scheme = schemeOf(tc);
        if (scheme == null) {
            throw new BizException("该课程的老师还没有配置并锁定成绩构成，不能算分");
        }
        return schemeItemMapper.selectList(Wrappers.<ScoreSchemeItem>lambdaQuery()
                .eq(ScoreSchemeItem::getSchemeId, scheme.getId())
                .eq(ScoreSchemeItem::getStatus, "ACTIVE")
                .orderByAsc(ScoreSchemeItem::getSortNo));
    }

    /** 手工分项：按 (studentId, schemeItemId) 取多批次平均，换算成百分制 */
    private Map<Long, Map<Long, BigDecimal>> manualScores(TeachingClass tc, List<Enrollment> ens) {
        Map<Long, Map<Long, BigDecimal>> out = new LinkedHashMap<>();
        if (ens.isEmpty()) {
            return out;
        }
        List<ScoreItem> rows = scoreItemMapper.selectList(Wrappers.<ScoreItem>lambdaQuery()
                .in(ScoreItem::getEnrollmentId, ens.stream().map(Enrollment::getId).collect(Collectors.toList()))
                .eq(ScoreItem::getIsVoid, 0));
        Map<Long, Map<Long, List<ScoreItem>>> grouped = new LinkedHashMap<>();
        for (ScoreItem r : rows) {
            grouped.computeIfAbsent(r.getStudentId(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(r.getSchemeItemId(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<Long, Map<Long, List<ScoreItem>>> byStudent : grouped.entrySet()) {
            for (Map.Entry<Long, List<ScoreItem>> e : byStudent.getValue().entrySet()) {
                BigDecimal sum = BigDecimal.ZERO;
                int n = 0;
                for (ScoreItem r : e.getValue()) {
                    BigDecimal full = r.getFullScore() == null || r.getFullScore().signum() == 0
                            ? HUNDRED : r.getFullScore();
                    BigDecimal score = r.getScore() == null ? BigDecimal.ZERO : r.getScore();
                    sum = sum.add(score.multiply(HUNDRED).divide(full, 4, RoundingMode.HALF_UP));
                    n++;
                }
                if (n > 0) {
                    out.computeIfAbsent(byStudent.getKey(), k -> new LinkedHashMap<>())
                            .put(e.getKey(), sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
                }
            }
        }
        return out;
    }

    /** 自动分项：测验/作业按试卷均分、项目按个人得分（团队按贡献折算）换算百分制 */
    private Map<Long, Map<Long, ScoreCellVO>> autoScores(TeachingClass tc, List<ScoreItemColumnVO> columns,
                                                         List<Long> studentIds) {
        Map<Long, Map<Long, ScoreCellVO>> out = new LinkedHashMap<>();
        for (ScoreItemColumnVO c : columns) {
            if (!"AUTO".equalsIgnoreCase(c.getDataSource())) {
                continue;
            }
            Map<Long, List<BigDecimal>> acc = new LinkedHashMap<>();
            Map<Long, Integer> absentFlag = new LinkedHashMap<>();
            // 试卷
            List<Paper> papers = paperMapper.selectList(Wrappers.<Paper>lambdaQuery()
                    .eq(Paper::getTeachingClassId, tc.getId())
                    .eq(Paper::getSchemeItemId, c.getSchemeItemId())
                    .in(Paper::getStatus, List.of("PUBLISHED", "CLOSED")));
            for (Paper p : papers) {
                List<Submission> subs = submissionMapper.selectList(Wrappers.<Submission>lambdaQuery()
                        .eq(Submission::getPaperId, p.getId()));
                Map<Long, Submission> byStudent = subs.stream()
                        .collect(Collectors.toMap(Submission::getStudentId, s -> s, (a, b) -> a));
                for (Long sid : studentIds) {
                    Submission s = byStudent.get(sid);
                    BigDecimal pct;
                    if (s == null || (s.getIsAbsent() != null && s.getIsAbsent() == 1)) {
                        pct = BigDecimal.ZERO;
                        absentFlag.put(sid, 1);
                    } else {
                        BigDecimal full = p.getFullScore() == null || p.getFullScore().signum() == 0
                                ? HUNDRED : p.getFullScore();
                        BigDecimal got = s.getTotalScore() != null ? s.getTotalScore()
                                : (s.getObjectiveScore() == null ? BigDecimal.ZERO : s.getObjectiveScore());
                        pct = got.multiply(HUNDRED).divide(full, 2, RoundingMode.HALF_UP);
                    }
                    acc.computeIfAbsent(sid, k -> new ArrayList<>()).add(pct);
                }
            }
            // 项目
            List<ProjectTask> tasks = projectTaskMapper.selectList(Wrappers.<ProjectTask>lambdaQuery()
                    .eq(ProjectTask::getTeachingClassId, tc.getId())
                    .eq(ProjectTask::getSchemeItemId, c.getSchemeItemId())
                    .in(ProjectTask::getStatus, List.of("PUBLISHED", "CLOSED")));
            for (ProjectTask t : tasks) {
                List<ProjectSubmission> subs = projectSubmissionMapper.selectList(
                        Wrappers.<ProjectSubmission>lambdaQuery()
                                .eq(ProjectSubmission::getProjectId, t.getId()));
                for (Long sid : studentIds) {
                    ProjectSubmission mine = null;
                    BigDecimal ratio = BigDecimal.ONE;
                    for (ProjectSubmission s : subs) {
                        if (s.getTeamId() == null && sid.equals(s.getStudentId())) {
                            mine = s;
                            break;
                        }
                        if (s.getTeamId() != null) {
                            ProjectContribution pc = projectContributionMapper.selectOne(
                                    Wrappers.<ProjectContribution>lambdaQuery()
                                            .eq(ProjectContribution::getProjectId, t.getId())
                                            .eq(ProjectContribution::getTeamId, s.getTeamId()).last("limit 1"));
                            if (pc == null) {
                                continue;
                            }
                            for (Map<String, String> mem : JsonObjects.arrayObjects(pc.getContributionJson())) {
                                if (String.valueOf(sid).equals(mem.get("studentId"))) {
                                    mine = s;
                                    try {
                                        ratio = new BigDecimal(mem.get("ratio"));
                                    } catch (RuntimeException ignored) {
                                        ratio = BigDecimal.ONE;
                                    }
                                    break;
                                }
                            }
                        }
                        if (mine != null) {
                            break;
                        }
                    }
                    BigDecimal pct;
                    if (mine == null || mine.getProjectScore() == null) {
                        pct = BigDecimal.ZERO;
                        absentFlag.put(sid, 1);
                    } else {
                        BigDecimal full = t.getFullScore() == null || t.getFullScore().signum() == 0
                                ? HUNDRED : t.getFullScore();
                        pct = mine.getProjectScore().multiply(ratio).multiply(HUNDRED)
                                .divide(full, 2, RoundingMode.HALF_UP);
                    }
                    acc.computeIfAbsent(sid, k -> new ArrayList<>()).add(pct);
                }
            }
            if (acc.isEmpty()) {
                continue;
            }
            for (Map.Entry<Long, List<BigDecimal>> e : acc.entrySet()) {
                BigDecimal sum = e.getValue().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal avg = sum.divide(BigDecimal.valueOf(e.getValue().size()), 2, RoundingMode.HALF_UP);
                ScoreCellVO cell = new ScoreCellVO();
                cell.setScore(avg);
                cell.setSource("AUTO");
                cell.setManual(false);
                cell.setMissing(false);
                cell.setTimes(e.getValue().size());
                cell.setIsAbsent(absentFlag.getOrDefault(e.getKey(), 0));
                cell.setNote("按 " + e.getValue().size() + " 次自动汇总");
                out.computeIfAbsent(e.getKey(), k -> new LinkedHashMap<>())
                        .put(c.getSchemeItemId(), cell);
            }
        }
        return out;
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
