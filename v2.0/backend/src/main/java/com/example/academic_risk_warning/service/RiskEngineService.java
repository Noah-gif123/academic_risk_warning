package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.AttendanceRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.CourseSession;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.RiskEngineConfig;
import com.example.academic_risk_warning.entity.RiskState;
import com.example.academic_risk_warning.entity.ScoreItem;
import com.example.academic_risk_warning.entity.ScoreResult;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AttendanceRecordMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.CourseSessionMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.RiskEngineConfigMapper;
import com.example.academic_risk_warning.mapper.RiskStateMapper;
import com.example.academic_risk_warning.mapper.ScoreItemMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.security.LoginUser;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 风险判定引擎（第二阶段第 1 步）：
 * 读取选课 / 考勤 / 成绩，按冻结口径算出每个学生每门课的风险，写入 risk_state（一人一课一行，覆盖刷新）。
 *
 * <p>本轮实现 A 通道（1/3 硬门槛、剩余可缺、期末需考可达性）与 B 通道（班内相对风险分）；
 * C 通道（教师尺度 Δ̂ + 课程因子 c 的跨班可比）留到第三阶段，字段置空。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final CourseSessionMapper sessionMapper;
    private final AttendanceRecordMapper attendanceMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final RiskStateMapper riskStateMapper;
    private final RiskEngineConfigMapper configMapper;
    private final TermMapper termMapper;
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
        for (Map.Entry<Long, List<Enrollment>> entry : byClass.entrySet()) {
            Long tcId = entry.getKey();
            TeachingClass tc = classMapper.selectById(tcId);
            if (tc == null) {
                continue;
            }
            CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
            List<CourseSession> sessions = sessionsOf(tcId);
            List<Long> sessionIds = sessions.stream().map(CourseSession::getId).collect(Collectors.toList());
            int effective = effectiveSessions(offering, sessions);
            Integer allowed = RiskMath.allowedAbsent(effective);
            double coverage = coverageOf(sessions);

            // 班内排名用的总评分
            List<Enrollment> classEnrollments = entry.getValue();
            Map<Long, ScoreResult> results = resultsOf(classEnrollments.stream()
                    .map(Enrollment::getId).collect(Collectors.toList()));

            for (Enrollment e : classEnrollments) {
                if (e.getStatus() != null && ("WITHDRAWN".equals(e.getStatus()) || "SUSPENDED".equals(e.getStatus()))) {
                    continue;
                }
                ScoreResult sr = results.get(e.getId());
                int[] att = attendanceCount(e.getId(), sessionIds);
                int absent = att[0];
                int late = att[1];
                int leave = att[2];

                int remaining = RiskMath.remainingAbsent(allowed, absent);
                boolean hardFail = absent > allowed;
                String aLevel = RiskMath.channelALevel(absent, allowed);

                BigDecimal W = sr == null ? null : sr.getWeightedScore();
                BigDecimal P = sr == null ? null
                        : (sr.getDeductionPoints() != null ? sr.getDeductionPoints()
                        : RiskMath.deductionPoints(absent, late, leave, nz(config.getLeaveFreeTimes())));
                BigDecimal T = sr == null ? null : sr.getTotalScore();
                BigDecimal pass = sr != null && sr.getPassScore() != null ? BigDecimal.valueOf(sr.getPassScore())
                        : (offering != null && offering.getPassScore() != null
                        ? BigDecimal.valueOf(offering.getPassScore()) : BigDecimal.valueOf(60));

                // 期末需考（仅当能定位到期末评分项时给值）
                BigDecimal[] need = finalNeed(tc.getOfferingId(), e.getId(), W, P, pass, config);
                BigDecimal finalNeeded = need == null ? null : need[0];
                boolean finalUnreachable = need != null && need[1].signum() > 0;

                // B 通道：班内相对风险分
                BigDecimal bScore = RiskMath.channelBScore(T, absent);
                int rank = rankOf(classEnrollments, results, T, e.getId());
                int classSize = classEnrollments.size();

                // 置信度：覆盖率 × 数据完备度 × 样本充足度
                double completeness = T != null ? 1.0 : (sr != null ? 0.6 : 0.3);
                double sample = effective >= 8 ? 1.0 : (effective >= 4 ? 0.7 : 0.5);
                BigDecimal confidence = RiskMath.confidence(coverage, completeness, sample);
                if (config.getConfidenceMinOk() != null
                        && confidence.compareTo(config.getConfidenceMinOk()) < 0) {
                    confidence = config.getConfidenceMinOk();
                }
                String outputMode = RiskMath.outputMode(confidence, coverage);

                // 对外风险分与等级：硬门槛直接最高档，否则用 B 通道分
                BigDecimal riskScore = hardFail ? BigDecimal.valueOf(95) : bScore;
                String level = RiskMath.riskLevel(riskScore,
                        config.getThresholdRed(), config.getThresholdOrange(), config.getThresholdYellow());

                RiskState existing = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                        .eq(RiskState::getEnrollmentId, e.getId()).last("limit 1"));
                String beforeLevel = existing == null ? null : existing.getRiskLevel();
                // 判定前快照（只看等级，用于事件层判断生成/升级/降级/关闭）
                RiskState beforeSnapshot = null;
                if (existing != null) {
                    beforeSnapshot = new RiskState();
                    beforeSnapshot.setRiskLevel(existing.getRiskLevel());
                }
                RiskState state = existing == null ? new RiskState() : existing;

                state.setEnrollmentId(e.getId());
                state.setStudentId(e.getStudentId());
                state.setOfferingId(e.getOfferingId() != null ? e.getOfferingId() : tc.getOfferingId());
                state.setTeachingClassId(tcId);
                state.setTermId(e.getTermId());
                state.setAbsentCount(absent);
                state.setLateCount(late);
                state.setLeaveCount(leave);
                state.setAllowedAbsent(allowed);
                state.setRemainingAbsent(remaining);
                state.setHardFail(hardFail ? 1 : 0);
                state.setNearFail(remaining <= 1 ? 1 : 0);
                state.setFinalExamNeeded(finalNeeded);
                state.setFinalUnreachable(finalUnreachable ? 1 : 0);
                state.setChannelALevel(aLevel);
                state.setChannelBScore(bScore);
                state.setChannelBRank(rank);
                state.setChannelBClassSize(classSize);
                state.setChannelBLevel("SAME_AS_A");
                state.setChannelCScore(null);
                state.setDeltaUsed(null);
                state.setCourseFactorUsed(null);
                state.setChannelCLevel(null);
                state.setRiskScore(riskScore);
                state.setRiskLevel(level);
                state.setActiveChannel(hardFail ? "A" : "B");
                state.setConfidence(confidence);
                state.setConfidenceLevel(RiskMath.confidenceLevel(confidence));
                state.setOutputMode(outputMode);
                state.setDataQualityJson(dataQuality(coverage, completeness, sample, effective, absent, allowed, remaining));
                state.setTriggerSource(result.getTrigger());
                state.setLastEvalTime(LocalDateTime.now());
                state.setEngineVersion(config.getEngineVersion());
                state.setIsVoid(0);
                if (existing == null) {
                    state.setCreateTime(LocalDateTime.now());
                    state.setUpdateTime(LocalDateTime.now());
                    riskStateMapper.insert(state);
                    inserted++;
                } else {
                    state.setUpdateTime(LocalDateTime.now());
                    riskStateMapper.updateById(state);
                    updated++;
                }
                // 第 2 步：判定后同步预警事件（生成 / 升级 / 降级 / 自动关闭 + 通知）
                alertEventService.sync(beforeSnapshot, state);
                if (beforeLevel != null && !beforeLevel.equals(level)) {
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

    private Map<Long, ScoreResult> resultsOf(List<Long> enrollmentIds) {
        Map<Long, ScoreResult> map = new LinkedHashMap<>();
        if (enrollmentIds.isEmpty()) {
            return map;
        }
        scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                        .in(ScoreResult::getEnrollmentId, enrollmentIds)
                        .eq(ScoreResult::getIsVoid, 0))
                .forEach((r) -> map.put(r.getEnrollmentId(), r));
        return map;
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
     * 期末需考：x = (及格线 + P − 其余项加权) / 期末权重占比。
     * 找不到期末评分项（item_code=FINAL）或没有分项得分时返回 null（页面显示“—”）。
     */
    private BigDecimal[] finalNeed(Long offeringId, Long enrollmentId, BigDecimal W, BigDecimal P,
                                   BigDecimal pass, RiskEngineConfig config) {
        ScoreScheme scheme = schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, offeringId).orderByDesc(ScoreScheme::getId).last("limit 1"));
        if (scheme == null) {
            return null;
        }
        ScoreSchemeItem finalItem = schemeItemMapper.selectOne(Wrappers.<ScoreSchemeItem>lambdaQuery()
                .eq(ScoreSchemeItem::getSchemeId, scheme.getId())
                .eq(ScoreSchemeItem::getItemCode, "FINAL").last("limit 1"));
        if (finalItem == null || finalItem.getWeight() == null || finalItem.getWeight().signum() <= 0) {
            return null;
        }
        BigDecimal ratio = finalItem.getWeight().divide(HUNDRED, 4, RoundingMode.HALF_UP);
        BigDecimal full = finalItem.getFullScore() == null ? HUNDRED : finalItem.getFullScore();
        BigDecimal finalWeighted = BigDecimal.ZERO;
        List<ScoreItem> items = scoreItemMapper.selectList(Wrappers.<ScoreItem>lambdaQuery()
                .eq(ScoreItem::getEnrollmentId, enrollmentId)
                .eq(ScoreItem::getSchemeItemId, finalItem.getId())
                .eq(ScoreItem::getIsVoid, 0));
        if (!items.isEmpty() && items.get(0).getScore() != null) {
            BigDecimal got = items.get(0).getScore();
            BigDecimal itemFull = items.get(0).getFullScore() == null ? HUNDRED : items.get(0).getFullScore();
            finalWeighted = got.divide(itemFull, 4, RoundingMode.HALF_UP).multiply(ratio).multiply(HUNDRED);
        }
        BigDecimal finalFull = RiskMath.finalExamNeeded(pass, P, W, finalWeighted, ratio, full)[0];
        boolean reachable = finalFull == null || finalFull.compareTo(full) <= 0;
        return new BigDecimal[] { finalFull, reachable ? BigDecimal.ZERO : BigDecimal.ONE };
    }

    private int rankOf(List<Enrollment> list, Map<Long, ScoreResult> results, BigDecimal myTotal, Long myEnrollmentId) {
        List<BigDecimal> totals = new ArrayList<>();
        for (Enrollment e : list) {
            ScoreResult r = results.get(e.getId());
            totals.add(r == null || r.getTotalScore() == null ? BigDecimal.valueOf(Long.MAX_VALUE) : r.getTotalScore());
        }
        totals.sort(BigDecimal::compareTo);
        int idx = totals.indexOf(myTotal == null ? BigDecimal.valueOf(Long.MAX_VALUE) : myTotal);
        return idx < 0 ? list.size() : idx + 1;
    }

    private String dataQuality(double coverage, double completeness, double sample,
                               int effective, int absent, int allowed, int remaining) {
        return "{\"coverage\":" + round3(coverage)
                + ",\"completeness\":" + round3(completeness)
                + ",\"sample\":" + round3(sample)
                + ",\"effectiveSessions\":" + effective
                + ",\"absent\":" + absent
                + ",\"allowedAbsent\":" + allowed
                + ",\"remainingAbsent\":" + remaining + "}";
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
