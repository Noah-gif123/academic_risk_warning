package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseFailRateHistory;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.RiskEngineConfig;
import com.example.academic_risk_warning.entity.RiskState;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.StudentFailHistory;
import com.example.academic_risk_warning.entity.TeacherStyle;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AttendanceRecordMapper;
import com.example.academic_risk_warning.mapper.CourseFailRateHistoryMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.RiskEngineConfigMapper;
import com.example.academic_risk_warning.mapper.RiskStateMapper;
import com.example.academic_risk_warning.mapper.StudentFailHistoryMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherStyleMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.util.JsonObjects;
import com.example.academic_risk_warning.util.RiskMath;
import com.example.academic_risk_warning.vo.RiskEvaluateResultVO;
import com.example.academic_risk_warning.vo.RiskRowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 风险判定引擎（第二阶段第 1 步 + M16 口径改造）：
 * 读取选课 / 考勤 / 成绩，按冻结口径算出每个学生每门课的风险，写入 risk_state（一人一课一行，覆盖刷新）。
 *
 * <p>M16 口径：</p>
 * <ul>
 *   <li>过程分只取**非终结性分支**分项、按**已录项**归一（成绩折算统一走 {@link ScoreCalculator}，
 *       与成绩单页同一套数，不再读 {@code score_result} 的历史总评）；</li>
 *   <li>期末/大作业只作"需考/需得"目标：`需考 = (及格线 + 扣分 − 过程分×(1−r_final)) / r_final`，
 *       `r_final` 取终结性**根**分支权重（不是子项 100%）；</li>
 *   <li>等级走绝对口径：需考 60→0 分、100→100 分，再叠 A 通道硬门槛与"再缺即挂"；</li>
 *   <li>C 通道乘子：课程类型（考试 ×1.10 / 考查 ×0.90）、该科历史挂科率、教师风格（teacher_style.Δ）、
 *       个人前科（student_fail_history），乘在风险分上；缺历史时一律 1.00，不凭空抬风险。</li>
 * </ul>
 *
 * <p>B 通道（班内相对分）降级为排序字段：{@code channel_b_rank} 只是列表顺序，不是预警结论。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ONE = BigDecimal.ONE;
    /** 过程分覆盖率闸门：低于 30% 只给行为预警，不给"需考"结论 */
    private static final BigDecimal PROCESS_COVERAGE_GATE = new BigDecimal("0.30");

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final CourseSessionMapper sessionMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final RiskStateMapper riskStateMapper;
    private final RiskEngineConfigMapper configMapper;
    private final TermMapper termMapper;
    private final TeacherStyleMapper teacherStyleMapper;
    private final CourseFailRateHistoryMapper courseFailRateMapper;
    private final StudentFailHistoryMapper failHistoryMapper;
    private final ScoreCalculator scoreCalculator;
    private final CourseContentService contentService;
    private final AuditService auditService;
    private final AlertEventService alertEventService;


    // ============================================================ 判定入口
    /**
     * 触发判定。
     *
     * <p>范围优先级：enrollmentId &gt; teachingClassId &gt; offeringId &gt; termId &gt; 当前学期。
     * 教师只能对自己带的教学班触发（传 teachingClassId），教务可任意范围。</p>
     */
    @Transactional
    public RiskEvaluateResultVO evaluate(LoginUser user, Long termId, Long offeringId,
                                         Long teachingClassId, Long enrollmentId, String trigger) {
        long start = System.currentTimeMillis();
        RiskEvaluateResultVO result = new RiskEvaluateResultVO();
        result.setTrigger(trigger == null ? "EVALUATION" : trigger);
        result.setEvaluatedAt(LocalDateTime.now());

        RiskEngineConfig config = activeConfig();
        result.setEngineVersion(config.getEngineVersion());

        // ---------------- 解析范围 ----------------
        List<Enrollment> targets;
        String scope;
        if (enrollmentId != null) {
            Enrollment e = enrollmentMapper.selectById(enrollmentId);
            if (e == null) {
                throw new BizException("选课记录不存在");
            }
            requireScope(user, e.getTeachingClassId());
            targets = List.of(e);
            scope = "选课 #" + enrollmentId;
        } else if (teachingClassId != null) {
            TeachingClass tc = requireClass(user, teachingClassId);
            targets = enrollmentsOfClass(tc.getId());
            scope = "教学班 " + tc.getClassCode();
        } else if (offeringId != null) {
            requireAdmin(user);
            CourseOffering offering = offeringMapper.selectById(offeringId);
            if (offering == null) {
                throw new BizException("开课实例不存在");
            }
            targets = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                    .eq(Enrollment::getOfferingId, offeringId)
                    .eq(Enrollment::getTermId, offering.getTermId()));
            scope = "开课实例 #" + offeringId;
        } else {
            requireAdmin(user);
            Long useTerm = resolveTerm(termId);
            targets = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                    .eq(Enrollment::getTermId, useTerm));
            Term t = useTerm == null ? null : termMapper.selectById(useTerm);
            scope = "学期 " + (t == null ? "全部" : t.getTermCode());
        }
        result.setScope(scope);

        // ---------------- 逐条判定 ----------------
        Map<Long, List<Enrollment>> byClass = targets.stream()
                .filter((e) -> e.getTeachingClassId() != null)
                .collect(Collectors.groupingBy(Enrollment::getTeachingClassId, LinkedHashMap::new, Collectors.toList()));

        int inserted = 0;
        int updated = 0;
        int levelChanged = 0;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> syntheticTerms = syntheticTermIds();
        Map<Long, Integer> termOrdinal = termOrdinals();
        for (Map.Entry<Long, List<Enrollment>> entry : byClass.entrySet()) {
            TeachingClass tc = classMapper.selectById(entry.getKey());
            if (tc == null) {
                continue;
            }
            CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
            List<CourseSession> sessions = sessionsOf(tc.getId());
            List<Long> sessionIds = sessions.stream().map(CourseSession::getId).collect(Collectors.toList());
            int effective = effectiveSessions(offering, sessions);
            Integer allowed = RiskMath.allowedAbsent(effective);
            double coverage = coverageOf(sessions);

            List<Enrollment> classEnrollments = entry.getValue();
            // M16：成绩折算走 ScoreCalculator（与成绩单页同一套数），不再读 score_result
            ScoreCalculator.ClassScore sc = scoreCalculator.compute(tc);
            Course course = offering == null ? null : courseMapper.selectById(offering.getCourseId());
            ChannelC cc = channelC(tc, offering, course, classEnrollments, syntheticTerms, termOrdinal, config);

            List<Decision> decisions = new ArrayList<>();
            for (Enrollment e : classEnrollments) {
                if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
                    continue;
                }
                decisions.add(decide(sc, cc, e, sessionIds, allowed, coverage, effective, config));
            }
            // 名次只用于列表排序（风险分从高到低），不参与定级
            decisions.sort(Comparator.comparing((Decision d) -> d.riskScore,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            for (int i = 0; i < decisions.size(); i++) {
                decisions.get(i).rank = i + 1;
                decisions.get(i).classSize = decisions.size();
            }

            for (Decision d : decisions) {
                RiskState existing = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                        .eq(RiskState::getEnrollmentId, d.enrollment.getId()).last("limit 1"));
                String beforeLevel = existing == null ? null : existing.getRiskLevel();
                // 判定前快照（只看等级，用于事件层判断生成/升级/降级/关闭）
                RiskState beforeSnapshot = null;
                if (existing != null) {
                    beforeSnapshot = new RiskState();
                    beforeSnapshot.setRiskLevel(existing.getRiskLevel());
                }
                RiskState state = existing == null ? new RiskState() : existing;
                applyDecision(state, d, cc, tc, result.getTrigger(), config, now);
                if (existing == null) {
                    state.setCreateTime(now);
                    state.setUpdateTime(now);
                    riskStateMapper.insert(state);
                    inserted++;
                } else {
                    state.setUpdateTime(now);
                    riskStateMapper.updateById(state);
                    updated++;
                }
                // 第 2 步：判定后同步预警事件（生成 / 升级 / 降级 / 自动关闭 + 通知）
                alertEventService.sync(beforeSnapshot, state);
                if (beforeLevel != null && !beforeLevel.equals(d.level)) {
                    levelChanged++;
                }
            }
        }

        result.setEvaluatedCount(inserted + updated);
        result.setInsertedCount(inserted);
        result.setUpdatedCount(updated);
        result.setLevelChangedCount(levelChanged);
        result.setCostMs(System.currentTimeMillis() - start);
        auditService.record("RISK_STATE", null, "EVALUATE", null, null,
                "触发风险判定（" + result.getTrigger() + "）：" + scope + "，判定 " + result.getEvaluatedCount()
                        + " 条，新增 " + inserted + "、更新 " + updated + "、等级变化 " + levelChanged
                        + "，引擎版本 " + config.getEngineVersion());
        return result;
    }

    // ============================================================ 查询
    /** 教师端风险名单（可按等级/关键词过滤，按风险分倒序） */
    public List<RiskRowVO> listRisks(LoginUser user, Long teachingClassId, String level, String keyword) {
        if (teachingClassId == null) {
            throw new BizException("请先选择教学班");
        }
        TeachingClass tc = requireClass(user, teachingClassId);
        List<RiskState> states = riskStateMapper.selectList(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getTeachingClassId, tc.getId())
                .eq(RiskState::getIsVoid, 0)
                .orderByDesc(RiskState::getRiskScore));
        if (states.isEmpty()) {
            return List.of();
        }
        Map<Long, Student> students = studentsOf(states.stream()
                .map(RiskState::getStudentId).distinct().collect(Collectors.toList()));
        CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
        Course course = offering == null ? null : courseMapper.selectById(offering.getCourseId());

        List<RiskRowVO> out = new ArrayList<>();
        for (RiskState s : states) {
            if (level != null && !level.isEmpty() && !level.equals(s.getRiskLevel())) {
                continue;
            }
            Student st = students.get(s.getStudentId());
            if (keyword != null && !keyword.isEmpty()) {
                String hay = (st == null ? "" : st.getStudentNo() + st.getStudentName());
                if (!hay.contains(keyword.trim())) {
                    continue;
                }
            }
            out.add(toRow(s, st, tc, course));
        }
        return out;
    }

    /** 教师端总览：按等级计数 + 待判定信息 */
    public Map<String, Object> stats(LoginUser user, Long teachingClassId) {
        TeachingClass tc = requireClass(user, teachingClassId);
        List<RiskState> states = riskStateMapper.selectList(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getTeachingClassId, tc.getId())
                .eq(RiskState::getIsVoid, 0));
        long red = states.stream().filter((s) -> "RED".equals(s.getRiskLevel())).count();
        long orange = states.stream().filter((s) -> "ORANGE".equals(s.getRiskLevel())).count();
        long yellow = states.stream().filter((s) -> "YELLOW".equals(s.getRiskLevel())).count();
        long hard = states.stream().filter((s) -> s.getHardFail() != null && s.getHardFail() == 1).count();
        LocalDateTime lastEval = states.stream().map(RiskState::getLastEvalTime)
                .filter((t) -> t != null).max(LocalDateTime::compareTo).orElse(null);
        RiskEngineConfig config = activeConfig();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("teachingClassId", tc.getId());
        map.put("classCode", tc.getClassCode());
        map.put("className", tc.getClassName());
        map.put("total", states.size());
        map.put("red", red);
        map.put("orange", orange);
        map.put("yellow", yellow);
        map.put("green", states.size() - red - orange - yellow);
        map.put("hardFail", hard);
        map.put("lastEvalTime", lastEval);
        map.put("engineVersion", config == null ? null : config.getEngineVersion());
        return map;
    }

    // ============================================================ 内部
    private RiskEngineConfig activeConfig() {
        RiskEngineConfig config = configMapper.selectOne(Wrappers.<RiskEngineConfig>lambdaQuery()
                .eq(RiskEngineConfig::getIsActive, 1).orderByDesc(RiskEngineConfig::getId).last("limit 1"));
        if (config == null) {
            throw new BizException("没有生效的风险引擎配置，请先在规则配置中启用一个版本");
        }
        return config;
    }

    private Long resolveTerm(Long termId) {
        if (termId != null) {
            return termId;
        }
        Term t = termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1).last("limit 1"));
        return t == null ? null : t.getId();
    }

    private TeachingClass requireClass(LoginUser user, Long teachingClassId) {
        if (user == null) {
            throw new BizException("请先登录");
        }
        if ("ADMIN".equals(user.getRole())) {
            TeachingClass tc = classMapper.selectById(teachingClassId);
            if (tc == null) {
                throw new BizException("教学班不存在");
            }
            return tc;
        }
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return contentService.requireMyClass(teachingClassId, user.getRefId());
    }

    private void requireScope(LoginUser user, Long teachingClassId) {
        requireClass(user, teachingClassId);
    }

    private void requireAdmin(LoginUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException("按学期/开课触发判定仅限教务处；教师请按教学班触发");
        }
    }

    private List<Enrollment> enrollmentsOfClass(Long teachingClassId) {
        return enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, teachingClassId));
    }

    private List<CourseSession> sessionsOf(Long teachingClassId) {
        return sessionMapper.selectList(Wrappers.<CourseSession>lambdaQuery()
                .eq(CourseSession::getTeachingClassId, teachingClassId)
                .eq(CourseSession::getIsVoid, 0)
                .orderByAsc(CourseSession::getSessionNo));
    }

    private Map<Long, Student> studentsOf(List<Long> studentIds) {
        Map<Long, Student> map = new LinkedHashMap<>();
        if (studentIds.isEmpty()) {
            return map;
        }
        studentMapper.selectBatchIds(studentIds).forEach((s) -> map.put(s.getId(), s));
        return map;
    }

    /** 有效课次：优先取教务确认的 effective_sessions，没有则用课次行数 */
    private int effectiveSessions(CourseOffering offering, List<CourseSession> sessions) {
        if (offering != null && offering.getEffectiveSessions() != null && offering.getEffectiveSessions() > 0) {
            return offering.getEffectiveSessions();
        }
        return sessions.size();
    }

    /** 点名覆盖率 = 已点名课次 / 应点名课次（已过日期且未取消） */
    private double coverageOf(List<CourseSession> sessions) {
        LocalDate today = LocalDate.now();
        long expected = 0;
        long called = 0;
        for (CourseSession s : sessions) {
            if ("CANCELLED".equals(s.getStatus())) {
                continue;
            }
            boolean passed = s.getSessionDate() == null || !s.getSessionDate().isAfter(today);
            boolean isCalled = "CALLED".equals(s.getRollCallStatus()) || "PARTIAL".equals(s.getRollCallStatus());
            if (passed) {
                expected++;
            }
            if (isCalled) {
                called++;
            }
        }
        if (expected <= 0) {
            return called > 0 ? 1.0 : 0.0;
        }
        return Math.min(1.0, (double) called / expected);
    }

    /** 统计某次选课的缺课/迟到/请假（同一次课取一条：归档优先，其次平时） */
    private int[] attendanceCount(Long enrollmentId, List<Long> sessionIds) {
        int[] out = new int[3];
        if (sessionIds.isEmpty()) {
            return out;
        }
        List<AttendanceRecord> records = attendanceMapper.selectList(Wrappers.<AttendanceRecord>lambdaQuery()
                .eq(AttendanceRecord::getEnrollmentId, enrollmentId)
                .eq(AttendanceRecord::getIsVoid, 0)
                .in(AttendanceRecord::getSessionId, sessionIds));
        Map<Long, AttendanceRecord> bySession = new LinkedHashMap<>();
        for (AttendanceRecord r : records) {
            AttendanceRecord old = bySession.get(r.getSessionId());
            if (old == null || "ARCHIVE".equals(r.getPhase())) {
                bySession.put(r.getSessionId(), r);
            }
        }
        for (AttendanceRecord r : bySession.values()) {
            if ("ABSENT".equals(r.getStatus())) {
                out[0]++;
            } else if ("LATE".equals(r.getStatus())) {
                out[1]++;
            } else if ("LEAVE".equals(r.getStatus())) {
                out[2]++;
            }
        }
        return out;
    }

    /**
     * 期末/大作业需考：x = (及格线 + P − W_est) / r_final，W_est = 过程分 × (1 − r_final)。
     *
     * <p>M16 起不再自己找评分项：`r_final` 由 {@link ScoreCalculator} 按**根分支**给出（子项 weight
     * 是父项内部的 100%，拿它当 r_final 会得到 1.0，把过程分整段丢掉）。</p>
     */
    private BigDecimal[] finalNeed(BigDecimal pass, BigDecimal deduction, BigDecimal earned,
                                   BigDecimal finalRatio, BigDecimal finalFull) {
        if (finalRatio == null || finalRatio.signum() <= 0 || earned == null) {
            return null;
        }
        return RiskMath.finalExamNeeded(pass, deduction, earned, BigDecimal.ZERO, finalRatio, finalFull);
    }

    // ============================================================ 一条判定的结论
    /** 一个学生在一门课上的判定结论（先算完再排名，避免边算边排） */
    private static final class Decision {
        Enrollment enrollment;
        int absent;
        int late;
        int leave;
        Integer allowed;
        Integer remaining;
        boolean hardFail;
        String aLevel;
        BigDecimal processScore;
        BigDecimal processCoverage;
        BigDecimal missingWeight;
        BigDecimal finalRatio;
        BigDecimal baseScore;
        BigDecimal need;
        boolean unreachable;
        BigDecimal factor;
        BigDecimal riskScore;
        String level;
        String activeChannel;
        boolean terminalScored;
        BigDecimal terminalScore;
        boolean factorApplied;
        BigDecimal confidence;
        String outputMode;
        String dataQuality;
        int rank;
        int classSize;
    }

    /** C 通道因子：课程类型 / 历史挂科率 / 教师风格（班级级）+ 个人前科（学生级） */
    private static final class ChannelC {
        BigDecimal courseType = ONE;
        BigDecimal failRate = ONE;
        BigDecimal teacher = ONE;
        BigDecimal delta;
        String failRateNote = "absent";
        Map<Long, BigDecimal> personal = new LinkedHashMap<>();

        BigDecimal personalOf(Long studentId) {
            BigDecimal f = personal.get(studentId);
            return f == null ? ONE : f;
        }
    }

    /** 课程历史挂科率因子与取数说明 */
    private record FailRateFactor(BigDecimal factor, String note) {
    }

    /** 一个学生的判定（不含名次；名次由调用方统一给） */
    private Decision decide(ScoreCalculator.ClassScore sc, ChannelC cc, Enrollment e, List<Long> sessionIds,
                            Integer allowed, double coverage, int effective, RiskEngineConfig config) {
        Decision d = new Decision();
        d.enrollment = e;
        d.allowed = allowed;
        ScoreCalculator.StudentScore ss = sc.byEnrollment.get(e.getId());
        int[] att = attendanceCount(e.getId(), sessionIds);
        d.absent = att[0];
        d.late = att[1];
        d.leave = att[2];
        d.remaining = RiskMath.remainingAbsent(allowed, d.absent);
        d.hardFail = d.absent > allowed;
        d.aLevel = RiskMath.channelALevel(d.absent, allowed);
        d.processScore = ss == null ? null : ss.processScore;
        d.processCoverage = ss == null || ss.processCoverage == null ? BigDecimal.ZERO : ss.processCoverage;
        d.missingWeight = ss == null || ss.missingWeight == null ? BigDecimal.ZERO : ss.missingWeight;
        d.finalRatio = sc.finalRatio;

        BigDecimal pass = BigDecimal.valueOf(sc.passScore);
        BigDecimal finalFull = config.getFinalFullScore() == null ? HUNDRED : config.getFinalFullScore();
        BigDecimal deduction = RiskMath.deductionPoints(d.absent, d.late, d.leave, nz(config.getLeaveFreeTimes()));
        boolean processReady = d.processCoverage.compareTo(PROCESS_COVERAGE_GATE) >= 0;
        BigDecimal earned = processReady ? RiskMath.estimatedEarned(d.processScore, d.finalRatio) : null;
        BigDecimal[] need = finalNeed(pass, deduction, earned, d.finalRatio, finalFull);
        BigDecimal required = need == null ? null : need[0];
        BigDecimal terminalActual = ss == null ? null : ss.terminalScore;
        d.terminalScored = terminalActual != null;
        d.terminalScore = terminalActual;
        d.unreachable = need != null && need[1].signum() > 0;

        // 分数型风险（不含门槛地板）：后面先乘 C 通道乘子，再和确定性门槛取最大
        BigDecimal base;
        if (!processReady || required == null) {
            d.need = null;
            base = null;
        } else if (d.unreachable) {
            d.need = required;
            base = null;                       // 门槛地板 100 兜住
        } else if (d.terminalScored) {
            // 终结分支已录分（如考查课的大作业批完了）：看"还差多少分"，而不是"还需要考多少分"
            BigDecimal gap = required.subtract(terminalActual).max(BigDecimal.ZERO);
            d.need = gap;
            base = RiskMath.clamp100(gap.multiply(HUNDRED).divide(finalFull, 2, RoundingMode.HALF_UP));
        } else {
            d.need = required;
            base = RiskMath.absoluteRiskScore(required, pass, finalFull);
        }
        d.baseScore = base == null ? BigDecimal.ZERO : base;
        d.factor = cc.courseType.multiply(cc.failRate).multiply(cc.teacher)
                .multiply(cc.personalOf(e.getStudentId())).setScale(4, RoundingMode.HALF_UP);
        // C 通道乘子只校准"分数型"风险；确定性门槛（缺课硬线、需考不可达、再缺即挂）取最大、不连乘，
        // 否则 55×1.15 会把"再缺一次即挂"从黄抬到橙，档位就不可解释了。
        BigDecimal scorePart = base == null ? BigDecimal.ZERO : base;
        if (base != null) {
            scorePart = RiskMath.clamp100(scorePart.multiply(d.factor).setScale(2, RoundingMode.HALF_UP));
            d.factorApplied = d.factor.compareTo(ONE) != 0;
        }
        BigDecimal floor = RiskMath.guardFloor(d.hardFail, d.unreachable, d.remaining);
        d.riskScore = RiskMath.clamp100(scorePart.max(floor));
        d.level = RiskMath.riskLevel(d.riskScore,
                config.getThresholdRed(), config.getThresholdOrange(), config.getThresholdYellow());
        d.activeChannel = d.hardFail ? "A" : (d.factorApplied ? "C" : "B");

        // 置信度：点名覆盖率 × 完备度 × 样本 × 过程分覆盖率；floor 只抬"点名覆盖率不足"那一路
        double completeness = d.processScore != null ? 1.0 : (processReady ? 0.6 : 0.3);
        double sample = effective >= 8 ? 1.0 : (effective >= 4 ? 0.7 : 0.5);
        BigDecimal confidence = RiskMath.confidenceWithScoreCoverage(coverage, completeness, sample, d.processCoverage);
        if (config.getCoverageThreshold() != null && coverage < config.getCoverageThreshold().doubleValue()
                && config.getConfidenceMinOk() != null
                && confidence.compareTo(config.getConfidenceMinOk()) < 0) {
            confidence = config.getConfidenceMinOk();
        }
        d.confidence = confidence;
        d.outputMode = processReady ? RiskMath.outputMode(confidence, coverage) : "BEHAVIOR_ONLY";
        d.dataQuality = dataQuality(d, cc, coverage, completeness, sample, effective);
        return d;
    }

    /** 把判定结论写进 risk_state（一人一课一行，覆盖刷新） */
    private void applyDecision(RiskState state, Decision d, ChannelC cc, TeachingClass tc,
                              String trigger, RiskEngineConfig config, LocalDateTime now) {
        state.setEnrollmentId(d.enrollment.getId());
        state.setStudentId(d.enrollment.getStudentId());
        state.setOfferingId(d.enrollment.getOfferingId() != null ? d.enrollment.getOfferingId() : tc.getOfferingId());
        state.setTeachingClassId(tc.getId());
        state.setTermId(d.enrollment.getTermId());
        state.setAbsentCount(d.absent);
        state.setLateCount(d.late);
        state.setLeaveCount(d.leave);
        state.setAllowedAbsent(d.allowed);
        state.setRemainingAbsent(d.remaining);
        state.setHardFail(d.hardFail ? 1 : 0);
        state.setNearFail(d.remaining <= 1 ? 1 : 0);
        state.setFinalExamNeeded(d.need);
        state.setFinalUnreachable(d.unreachable ? 1 : 0);
        state.setChannelALevel(d.aLevel);
        // B 通道只剩排序：绝对口径基础分 + 班内名次
        state.setChannelBScore(d.baseScore);
        state.setChannelBRank(d.rank);
        state.setChannelBClassSize(d.classSize);
        state.setChannelBLevel("SORT_ONLY");
        state.setChannelCScore(d.riskScore);
        state.setDeltaUsed(cc.delta);
        state.setCourseFactorUsed(cc.courseType.multiply(cc.failRate).setScale(3, RoundingMode.HALF_UP));
        state.setChannelCLevel(d.level);
        state.setRiskScore(d.riskScore);
        state.setRiskLevel(d.level);
        state.setActiveChannel(d.activeChannel);
        state.setConfidence(d.confidence);
        state.setConfidenceLevel(RiskMath.confidenceLevel(d.confidence));
        state.setOutputMode(d.outputMode);
        state.setDataQualityJson(d.dataQuality);
        state.setTriggerSource(trigger);
        state.setLastEvalTime(now);
        state.setEngineVersion(config.getEngineVersion());
        state.setIsVoid(0);
    }

    // ============================================================ C 通道
    /** C 通道因子（按开关回退：关了就是 1.00） */
    private ChannelC channelC(TeachingClass tc, CourseOffering offering, Course course,
                              List<Enrollment> classEnrollments, Set<Long> syntheticTerms,
                              Map<Long, Integer> termOrdinal, RiskEngineConfig config) {
        ChannelC cc = new ChannelC();
        boolean cOn = config.getChannelCEnabled() != null && config.getChannelCEnabled() == 1;
        if (!cOn) {
            return cc;
        }
        boolean courseFactorOn = config.getCourseFactorEnabled() != null && config.getCourseFactorEnabled() == 1;
        if (courseFactorOn && course != null) {
            cc.courseType = courseTypeFactor(course.getCourseType(), config);
        }
        if (courseFactorOn && course != null && offering != null) {
            FailRateFactor fr = failRateFactor(course.getId(), offering.getTermId(), syntheticTerms, config);
            cc.failRate = fr.factor();
            cc.failRateNote = fr.note();
        }
        boolean deltaOn = config.getDeltaEnabled() != null && config.getDeltaEnabled() == 1;
        if (deltaOn) {
            TeacherStyle ts = teacherStyleOf(tc);
            if (ts != null) {
                cc.delta = ts.getDelta();
                cc.teacher = teacherFactor(ts.getDelta(), ts.getSampleSize(), config);
            }
        }
        cc.personal = personalFactors(classEnrollments, course == null ? null : course.getId(),
                offering == null ? null : offering.getTermId(), termOrdinal, syntheticTerms, config);
        return cc;
    }

    /** 课程类型：考试课更难（默认 ×1.10）、考查课更松（默认 ×0.90） */
    private BigDecimal courseTypeFactor(String courseType, RiskEngineConfig config) {
        BigDecimal exam = config.getCourseFactorExam() == null ? new BigDecimal("1.10") : config.getCourseFactorExam();
        BigDecimal assess = config.getCourseFactorAssess() == null ? new BigDecimal("0.90") : config.getCourseFactorAssess();
        if ("EXAM".equalsIgnoreCase(courseType)) {
            return exam;
        }
        if ("ASSESS".equalsIgnoreCase(courseType)) {
            return assess;
        }
        return ONE;
    }

    /**
     * 该科历史挂科率：优先用 `course_fail_rate_history` 的**已结束学期**；没有就用
     * `student_fail_history` 按课程聚合反推（本学期与合成学期一律排除）。
     *
     * <p>不能用 `teaching_class_fail_rate` / `admin_class_fail_rate`：那是本期数据、且是旧口径算出来的，
     * 拿它当"历史"等于用旧结论喂新口径。</p>
     */
    private FailRateFactor failRateFactor(Long courseId, Long currentTermId, Set<Long> syntheticTerms,
                                          RiskEngineConfig config) {
        BigDecimal sumRate = BigDecimal.ZERO;
        int rateRows = 0;
        int examinees = 0;
        for (CourseFailRateHistory r : courseFailRateMapper.selectList(
                Wrappers.<CourseFailRateHistory>lambdaQuery().eq(CourseFailRateHistory::getCourseId, courseId))) {
            if (r.getTermId() == null || r.getTermId().equals(currentTermId)
                    || syntheticTerms.contains(r.getTermId()) || r.getFailRate() == null) {
                continue;
            }
            sumRate = sumRate.add(r.getFailRate());
            rateRows++;
            examinees += r.getExamineeCount() == null ? 0 : r.getExamineeCount();
        }
        String source = "course_fail_rate_history";
        if (rateRows == 0) {
            // 兜底：从归档的挂科历史反推课程级挂科率（term 4 起才有真历史）
            int n = 0;
            int fails = 0;
            for (StudentFailHistory h : failHistoryMapper.selectList(
                    Wrappers.<StudentFailHistory>lambdaQuery().eq(StudentFailHistory::getCourseId, courseId))) {
                if (h.getTermId() == null || h.getTermId().equals(currentTermId)
                        || syntheticTerms.contains(h.getTermId())) {
                    continue;
                }
                n++;
                if (h.getIsFail() != null && h.getIsFail() == 1) {
                    fails++;
                }
            }
            if (n == 0) {
                return new FailRateFactor(ONE, "absent（没有已结束学期的课程级挂科记录，按 1.00）");
            }
            sumRate = BigDecimal.valueOf(fails * 100.0 / n).setScale(2, RoundingMode.HALF_UP);
            rateRows = 1;
            examinees = n;
            source = "student_fail_history";
        }
        BigDecimal avg = sumRate.divide(BigDecimal.valueOf(rateRows), 2, RoundingMode.HALF_UP);
        BigDecimal factor = RiskMath.failRateFactor(avg, examinees, config.getFailRateBase(),
                config.getFailRateStep(), config.getFailRateCap());
        return new FailRateFactor(factor, source + " avg=" + avg.toPlainString() + "% examinees=" + examinees);
    }

    /**
     * 教师风格：连续映射（偏严侧 strictStep^(−Δ)、偏松侧 looseStep^Δ），见
     * {@link RiskMath#teacherStyleFactor}。样本 &lt; 30 折半收缩，|Δ| 超过熔断阈值直接不用。
     */
    private BigDecimal teacherFactor(BigDecimal delta, Integer sampleSize, RiskEngineConfig config) {
        return RiskMath.teacherStyleFactor(delta, sampleSize, config.getDeltaFuseThreshold(),
                config.getTeacherStrictFactor(), config.getTeacherLooseFactor());
    }

    private TeacherStyle teacherStyleOf(TeachingClass tc) {
        return teacherStyleMapper.selectOne(Wrappers.<TeacherStyle>lambdaQuery()
                .eq(TeacherStyle::getTeacherId, tc.getTeacherId())
                .eq(TeacherStyle::getOfferingId, tc.getOfferingId())
                .eq(TeacherStyle::getStatus, "ACTIVE")
                .last("limit 1"));
    }

    /**
     * 个人前科：把"挂过的课"按权重累计 —— 同一门课（本课程）重修 ×2，并按时间衰减
     * （相隔 n 个学期 ×1/(1+0.5n)），再按权重给 prior1 / prior2。大一或从没挂过 → 1.00。
     */
    private Map<Long, BigDecimal> personalFactors(List<Enrollment> ens, Long courseId, Long currentTermId,
                                                  Map<Long, Integer> termOrdinal, Set<Long> syntheticTerms,
                                                  RiskEngineConfig config) {
        Map<Long, BigDecimal> out = new LinkedHashMap<>();
        List<Long> ids = ens.stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return out;
        }
        int currentOrdinal = termOrdinal.getOrDefault(currentTermId, 0);
        Map<Long, BigDecimal> weighted = new LinkedHashMap<>();
        for (StudentFailHistory h : failHistoryMapper.selectList(Wrappers.<StudentFailHistory>lambdaQuery()
                .in(StudentFailHistory::getStudentId, ids)
                .eq(StudentFailHistory::getIsFail, 1))) {
            if (h.getTermId() == null || h.getTermId().equals(currentTermId)
                    || syntheticTerms.contains(h.getTermId())) {
                continue;
            }
            int ago = Math.max(0, currentOrdinal - termOrdinal.getOrDefault(h.getTermId(), currentOrdinal));
            BigDecimal w = RiskMath.priorWeight(courseId != null && courseId.equals(h.getCourseId()), ago);
            weighted.merge(h.getStudentId(), w, BigDecimal::add);
        }
        for (Long sid : ids) {
            out.put(sid, RiskMath.personalPriorFactor(weighted.get(sid),
                    config.getPersonalPrior1(), config.getPersonalPrior2()));
        }
        return out;
    }

    /** 合成学期（term_code 以 SYN 开头）——库内备注"合成数据·勿混用"，历史因子一律排除 */
    private Set<Long> syntheticTermIds() {
        return termMapper.selectList(Wrappers.<Term>lambdaQuery()).stream()
                .filter((t) -> t.getTermCode() != null
                        && t.getTermCode().toUpperCase().startsWith("SYN"))
                .map(Term::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 学期先后序号（按开始日期），用于个人前科的"相隔几个学期"衰减 */
    private Map<Long, Integer> termOrdinals() {
        List<Term> terms = termMapper.selectList(Wrappers.<Term>lambdaQuery());
        terms.sort(Comparator.comparing(Term::getStartDate, Comparator.nullsFirst(Comparator.naturalOrder())));
        Map<Long, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < terms.size(); i++) {
            out.put(terms.get(i).getId(), i);
        }
        return out;
    }

    /** data_quality_json：过程分口径 + 四个 C 通道因子都留痕，回归和排查只认这一处 */
    private String dataQuality(Decision d, ChannelC cc, double coverage, double completeness, double sample,
                               int effective) {
        return "{\"coverage\":" + round3(coverage)
                + ",\"completeness\":" + round3(completeness)
                + ",\"sample\":" + round3(sample)
                + ",\"processScore\":" + plain(d.processScore)
                + ",\"processCoverage\":" + plain(d.processCoverage)
                + ",\"missingWeight\":" + plain(d.missingWeight)
                + ",\"finalRatio\":" + plain(d.finalRatio)
                + ",\"terminalScored\":" + d.terminalScored
                + ",\"terminalScore\":" + plain(d.terminalScore)
                + ",\"courseTypeFactor\":" + plain(cc.courseType)
                + ",\"failRateFactor\":" + plain(cc.failRate)
                + ",\"failRateHistory\":\"" + cc.failRateNote + "\""
                + ",\"teacherFactor\":" + plain(cc.teacher)
                + ",\"delta\":" + plain(cc.delta)
                + ",\"personalFactor\":" + plain(cc.personalOf(d.enrollment.getStudentId()))
                + ",\"factor\":" + plain(d.factor)
                + ",\"factorApplied\":" + d.factorApplied
                + ",\"effectiveSessions\":" + effective
                + ",\"absent\":" + d.absent
                + ",\"allowedAbsent\":" + d.allowed
                + ",\"remainingAbsent\":" + d.remaining + "}";
    }

    /** JSON 里的数值：null 写字面量 null，其余不带多余标度 */
    private static String plain(BigDecimal v) {
        return v == null ? "null" : v.toPlainString();
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private RiskRowVO toRow(RiskState s, Student st, TeachingClass tc, Course course) {
        RiskRowVO v = new RiskRowVO();
        v.setRiskStateId(s.getId());
        v.setEnrollmentId(s.getEnrollmentId());
        v.setStudentId(s.getStudentId());
        v.setStudentNo(st == null ? null : st.getStudentNo());
        v.setStudentName(st == null ? null : st.getStudentName());
        v.setOfferingId(s.getOfferingId());
        v.setCourseName(course == null ? null : course.getCourseName());
        v.setTeachingClassId(s.getTeachingClassId());
        v.setClassCode(tc.getClassCode());
        v.setClassName(tc.getClassName());
        v.setTermId(s.getTermId());
        v.setAbsentCount(s.getAbsentCount());
        v.setLateCount(s.getLateCount());
        v.setLeaveCount(s.getLeaveCount());
        v.setAllowedAbsent(s.getAllowedAbsent());
        v.setRemainingAbsent(s.getRemainingAbsent());
        v.setHardFail(s.getHardFail());
        v.setNearFail(s.getNearFail());
        v.setFinalExamNeeded(s.getFinalExamNeeded());
        v.setFinalUnreachable(s.getFinalUnreachable());
        v.setChannelALevel(s.getChannelALevel());
        // 过程分与覆盖率存在 data_quality_json 里（risk_state 没有这两列）
        v.setProcessScore(JsonObjects.number(s.getDataQualityJson(), "processScore"));
        v.setProcessCoverage(JsonObjects.number(s.getDataQualityJson(), "processCoverage"));
        v.setChannelBScore(s.getChannelBScore());
        v.setChannelBRank(s.getChannelBRank());
        v.setChannelBClassSize(s.getChannelBClassSize());
        v.setRiskScore(s.getRiskScore());
        v.setRiskLevel(s.getRiskLevel());
        v.setActiveChannel(s.getActiveChannel());
        v.setConfidence(s.getConfidence());
        v.setConfidenceLevel(s.getConfidenceLevel());
        v.setOutputMode(s.getOutputMode());
        v.setEngineVersion(s.getEngineVersion());
        v.setLastEvalTime(s.getLastEvalTime());
        return v;
    }

    private static Integer nz(Integer v) {
        return v == null ? 0 : v;
    }
}
