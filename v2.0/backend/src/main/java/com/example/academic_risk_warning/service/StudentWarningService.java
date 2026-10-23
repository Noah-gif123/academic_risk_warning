package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.AlertFlowLog;
import com.example.academic_risk_warning.entity.AlertRecord;
import com.example.academic_risk_warning.entity.Course;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.RiskEngineConfig;
import com.example.academic_risk_warning.entity.RiskState;
import com.example.academic_risk_warning.entity.ScoreResult;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Teacher;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.mapper.AlertFlowLogMapper;
import com.example.academic_risk_warning.mapper.AlertRecordMapper;
import com.example.academic_risk_warning.mapper.CourseMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.RiskEngineConfigMapper;
import com.example.academic_risk_warning.mapper.RiskStateMapper;
import com.example.academic_risk_warning.mapper.ScoreResultMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.TeacherMapper;
import com.example.academic_risk_warning.mapper.TeachingClassMapper;
import com.example.academic_risk_warning.mapper.TermMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.vo.StudentWarningVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学生端 · 我的学情预警（第二阶段第 4 步）。
 *
 * <p>只读服务：把 risk_state / alert_record / score_result 汇成「一门课一条结论 + 建议 + 口径」。
 * 口径约束（与学生端一致）：<b>只给结论，不给剩余可缺次数</b>；不展示教师内部处置记录。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentWarningService {

    private static final int LEVEL_RED = 3;
    private static final int LEVEL_ORANGE = 2;
    private static final int LEVEL_YELLOW = 1;

    private final EnrollmentMapper enrollmentMapper;
    private final RiskStateMapper riskStateMapper;
    private final AlertRecordMapper alertMapper;
    private final AlertFlowLogMapper flowMapper;
    private final StudentMapper studentMapper;
    private final TeachingClassMapper classMapper;
    private final TeacherMapper teacherMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseMapper courseMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final TermMapper termMapper;
    private final RiskEngineConfigMapper configMapper;

    // ============================================================ 概览
    public StudentWarningVO overview(LoginUser user, Long termId) {
        Student st = requireStudent(user);
        Term term = resolveTerm(termId);
        RiskEngineConfig config = activeConfig();

        List<Enrollment> enrollments = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getStudentId, st.getId())
                .eq(term != null, Enrollment::getTermId, term == null ? null : term.getId())
                .orderByAsc(Enrollment::getId));
        StudentWarningVO vo = new StudentWarningVO();
        vo.setStudentNo(st.getStudentNo());
        vo.setStudentName(st.getStudentName());
        vo.setTermName(term == null ? null : term.getTermName());
        vo.setEngineVersion(config == null ? null : config.getEngineVersion());
        vo.setGuidelines(guidelines(config));
        vo.setSummary(new StudentWarningVO.Summary());

        if (enrollments.isEmpty()) {
            vo.setItems(List.of());
            vo.setAdvice(List.of("本学期暂无在修课程，等有课程与考勤数据后这里会显示学情结论。"));
            return vo;
        }

        Map<Long, RiskState> states = riskStateMapper.selectList(Wrappers.<RiskState>lambdaQuery()
                        .in(RiskState::getEnrollmentId,
                                enrollments.stream().map(Enrollment::getId).collect(Collectors.toList()))
                        .eq(RiskState::getIsVoid, 0))
                .stream().collect(Collectors.toMap(RiskState::getEnrollmentId, Function.identity(), (a, b) -> a));
        Map<Long, ScoreResult> results = scoreResultMapper.selectList(Wrappers.<ScoreResult>lambdaQuery()
                        .in(ScoreResult::getEnrollmentId,
                                enrollments.stream().map(Enrollment::getId).collect(Collectors.toList()))
                        .eq(ScoreResult::getIsVoid, 0))
                .stream().collect(Collectors.toMap(ScoreResult::getEnrollmentId, Function.identity(), (a, b) -> a));

        Map<Long, TeachingClass> classes = batchClass(enrollments);
        Map<Long, CourseOffering> offerings = batchOffering(enrollments);
        Map<Long, Course> courses = batchCourse(offerings.values());
        Map<Long, Teacher> teachers = batchTeacher(classes.values());
        Map<Long, AlertRecord> alerts = currentAlerts(enrollments);

        List<StudentWarningVO.Item> items = new ArrayList<>();
        for (Enrollment e : enrollments) {
            RiskState s = states.get(e.getId());
            AlertRecord a = alerts.get(e.getId());
            ScoreResult sr = results.get(e.getId());
            TeachingClass tc = classes.get(e.getTeachingClassId());
            CourseOffering o = offerings.get(e.getOfferingId());
            Course c = o == null ? null : courses.get(o.getCourseId());
            Teacher t = tc == null ? null : teachers.get(tc.getTeacherId());
            StudentWarningVO.Item item = itemOf(e, s, a, sr, o, tc, c, t, config);
            items.add(item);
        }
        items.sort(Comparator.comparingInt((StudentWarningVO.Item i) -> rank(i.getLevel()))
                .reversed()
                .thenComparing((StudentWarningVO.Item i) -> i.getRiskScore() == null ? BigDecimal.ZERO : i.getRiskScore(),
                        Comparator.reverseOrder()));
        vo.setItems(items);

        StudentWarningVO.Summary sum = vo.getSummary();
        sum.setTotal(items.size());
        sum.setRed((int) items.stream().filter((i) -> "RED".equals(i.getLevel())).count());
        sum.setOrange((int) items.stream().filter((i) -> "ORANGE".equals(i.getLevel())).count());
        sum.setYellow((int) items.stream().filter((i) -> "YELLOW".equals(i.getLevel())).count());
        sum.setGreen((int) items.stream().filter((i) -> i.getLevel() == null || "GREEN".equals(i.getLevel())).count());
        sum.setAppealed((int) items.stream()
                .filter((i) -> i.getStudentResponse() != null && !i.getStudentResponse().isEmpty()).count());

        StudentWarningVO.Item first = items.isEmpty() ? null : items.get(0);
        if (first != null && first.getLastEvalTime() != null) {
            vo.setLastEvalTime(first.getLastEvalTime());
        }
        vo.setAdvice(overallAdvice(sum, items));
        return vo;
    }

    // ============================================================ 单课详情
    public StudentWarningVO.Detail detail(LoginUser user, Long enrollmentId) {
        Student st = requireStudent(user);
        Enrollment e = enrollmentMapper.selectById(enrollmentId);
        if (e == null || !st.getId().equals(e.getStudentId())) {
            throw new BizException("课程不存在或不属于当前学生");
        }
        RiskEngineConfig config = activeConfig();
        RiskState s = riskStateMapper.selectOne(Wrappers.<RiskState>lambdaQuery()
                .eq(RiskState::getEnrollmentId, enrollmentId)
                .eq(RiskState::getIsVoid, 0)
                .orderByDesc(RiskState::getId)
                .last("limit 1"));
        AlertRecord a = latestAlert(enrollmentId);
        ScoreResult sr = scoreResultMapper.selectOne(Wrappers.<ScoreResult>lambdaQuery()
                .eq(ScoreResult::getEnrollmentId, enrollmentId)
                .eq(ScoreResult::getIsVoid, 0)
                .orderByDesc(ScoreResult::getId)
                .last("limit 1"));
        TeachingClass tc = e.getTeachingClassId() == null ? null : classMapper.selectById(e.getTeachingClassId());
        CourseOffering o = e.getOfferingId() == null ? null : offeringMapper.selectById(e.getOfferingId());
        Course c = o == null || o.getCourseId() == null ? null : courseMapper.selectById(o.getCourseId());
        Teacher t = tc == null || tc.getTeacherId() == null ? null : teacherMapper.selectById(tc.getTeacherId());

        StudentWarningVO.Detail d = new StudentWarningVO.Detail();
        StudentWarningVO.Item item = itemOf(e, s, a, sr, o, tc, c, t, config);
        d.setItem(item);
        d.setAdvice(item.getAdvice());
        d.setGuidelines(guidelines(config));
        d.setTimeline(timeline(a));
        return d;
    }

    // ============================================================ 内部组装
    private StudentWarningVO.Item itemOf(Enrollment e, RiskState s, AlertRecord a, ScoreResult sr,
                                        CourseOffering o, TeachingClass tc, Course c, Teacher t,
                                        RiskEngineConfig config) {
        StudentWarningVO.Item i = new StudentWarningVO.Item();
        i.setEnrollmentId(e.getId());
        i.setOfferingId(e.getOfferingId());
        i.setTeachingClassId(e.getTeachingClassId());
        i.setAlertId(a == null ? null : a.getId());
        i.setCourseName(c == null ? "未知课程" : c.getCourseName());
        i.setCourseType(c == null ? null : c.getCourseType());
        i.setClassCode(tc == null ? null : tc.getClassCode());
        i.setTeacherName(t == null ? null : t.getTeacherName());

        String level = s != null && s.getRiskLevel() != null ? s.getRiskLevel()
                : (a != null && a.getAlertLevel() != null ? a.getAlertLevel() : "GREEN");
        i.setLevel(level);
        i.setRiskScore(s != null ? s.getRiskScore() : (a == null ? null : a.getRiskScore()));
        i.setConfidence(s != null ? s.getConfidence() : (a == null ? null : a.getConfidence()));
        i.setConfidenceLevel(s != null ? s.getConfidenceLevel() : null);
        i.setOutputMode(s != null ? s.getOutputMode() : (a == null ? null : a.getOutputMode()));
        i.setActiveChannel(s != null ? s.getActiveChannel() : (a == null ? null : a.getTriggerChannel()));
        i.setTriggerRule(s == null ? (a == null ? null : a.getTriggerRule()) : triggerRule(s));
        i.setReason(a == null ? null : a.getAlertReason());

        i.setAbsentCount(sr != null ? sr.getAbsentCount() : (s == null ? null : s.getAbsentCount()));
        i.setLateCount(sr != null ? sr.getLateCount() : (s == null ? null : s.getLateCount()));
        i.setLeaveCount(sr != null ? sr.getLeaveCount() : (s == null ? null : s.getLeaveCount()));
        i.setAllowedAbsent(sr != null ? sr.getAllowedAbsent() : (s == null ? null : s.getAllowedAbsent()));
        i.setHardFail((sr != null && eq1(sr.getHardFail())) || (s != null && eq1(s.getHardFail())));
        i.setFinalUnreachable(s != null && eq1(s.getFinalUnreachable()));
        i.setFinalExamNeeded(s == null ? null : s.getFinalExamNeeded());
        i.setChannelBScore(s == null ? null : s.getChannelBScore());
        i.setChannelBRank(s == null ? null : s.getChannelBRank());
        i.setChannelBClassSize(s == null ? null : s.getChannelBClassSize());
        i.setEffectiveSessions(sr != null ? sr.getEffectiveSessions() : null);
        i.setTotalScore(sr == null ? null : sr.getTotalScore());
        i.setPassScore(o == null || o.getPassScore() == null ? null : BigDecimal.valueOf(o.getPassScore()));
        i.setLastEvalTime(s == null ? null : s.getLastEvalTime());
        i.setStatus(a == null ? "NONE" : a.getStatus());
        i.setStudentResponse(a == null ? null : a.getStudentResponse());
        i.setStudentResponseTime(a == null ? null : a.getStudentResponseTime());

        i.setConclusion(conclusionOf(i));
        i.setAdvice(adviceOf(i, config));
        return i;
    }

    /** 一句话结论（学生可读，不含"剩余可缺次数"这类额度） */
    private String conclusionOf(StudentWarningVO.Item i) {
        int absent = i.getAbsentCount() == null ? 0 : i.getAbsentCount();
        int allowed = i.getAllowedAbsent() == null ? 0 : i.getAllowedAbsent();
        BigDecimal score = i.getRiskScore();
        String scoreText = score == null ? "—" : score.stripTrailingZeros().toPlainString();
        if (Boolean.TRUE.equals(i.getHardFail())) {
            return "缺课 " + absent + " 次已超过上限 " + allowed + " 次，本课程已触及硬性不合格线";
        }
        if (Boolean.TRUE.equals(i.getFinalUnreachable())) {
            return "按当前成绩测算，即使期末满分也达不到及格线";
        }
        switch (i.getLevel() == null ? "GREEN" : i.getLevel()) {
            case "RED":
                return "综合风险较高（风险分 " + scoreText + "），需要尽快改善出勤与成绩";
            case "ORANGE":
                return "风险偏高（风险分 " + scoreText + "），出勤或成绩已明显落后";
            case "YELLOW":
                return "存在风险（风险分 " + scoreText + "），请关注出勤、作业与测验";
            default:
                return i.getTotalScore() == null
                        ? "暂无预警，等考勤与成绩录入后会自动判定"
                        : "目前正常，综合分 " + i.getTotalScore().stripTrailingZeros().toPlainString() + "，继续保持";
        }
    }

    /** 单课建议：按"最该先做什么"排序，最多 3 条 */
    private List<String> adviceOf(StudentWarningVO.Item i, RiskEngineConfig config) {
        List<String> out = new ArrayList<>();
        int absent = i.getAbsentCount() == null ? 0 : i.getAbsentCount();
        int allowed = i.getAllowedAbsent() == null ? 0 : i.getAllowedAbsent();
        if (Boolean.TRUE.equals(i.getHardFail())) {
            out.add("立即联系任课教师" + (i.getTeacherName() == null ? "" : "（" + i.getTeacherName() + "）")
                    + "说明缺课原因，并按学校规定到教务处确认本课程的学分处理方式。");
        } else if (Boolean.TRUE.equals(i.getFinalUnreachable())) {
            out.add("尽快找任课教师核对成绩构成，补齐未交的作业与测验，确认是否还有可挽回的分项。");
        } else if (i.getLevel() != null && !"GREEN".equals(i.getLevel()) && allowed > 0 && absent >= allowed - 1) {
            out.add("缺课已达 " + absent + " 次、上限 " + allowed + " 次，再缺课就会触及硬线，请保证后面的每次课都到。");
        }
        if (i.getChannelBScore() != null && i.getChannelBScore().compareTo(BigDecimal.valueOf(60)) < 0) {
            out.add("加权分偏低（约 " + i.getChannelBScore().stripTrailingZeros().toPlainString()
                    + " 分），优先补齐欠交作业与测验，把平时分提上来。");
        }
        if (i.getLateCount() != null && i.getLateCount() > 0) {
            out.add("已有 " + i.getLateCount() + " 次迟到，迟到同样会扣分，请按时到课。");
        }
        boolean lowConfidence = "BEHAVIOR_ONLY".equals(i.getOutputMode()) || "INSUFFICIENT".equals(i.getOutputMode());
        if (lowConfidence) {
            out.add("本学期考勤或成绩记录还不完整，本次结论主要依据行为数据，后续数据补齐后会重新判定。");
        }
        if (i.getAlertId() != null && !"CLOSED".equals(i.getStatus())
                && !"DISMISSED".equals(i.getStatus()) && i.getStudentResponse() == null) {
            out.add("如果结论与你的实际情况不符（如请假已获批但未录入），可以在详情里提交申诉，由任课教师核实。");
        }
        if (out.isEmpty()) {
            out.add("暂无需要特别处理的事项；保持出勤、按时交作业即可。");
        }
        return out.size() <= 3 ? out : new ArrayList<>(out.subList(0, 3));
    }

    /** 跨课程总体建议 */
    private List<String> overallAdvice(StudentWarningVO.Summary sum, List<StudentWarningVO.Item> items) {
        List<String> out = new ArrayList<>();
        int risky = sum.getRed() + sum.getOrange() + sum.getYellow();
        if (sum.getRed() > 0) {
            out.add("有 " + sum.getRed() + " 门课程处于红色预警，建议本周内当面找任课教师或辅导员沟通一次。");
        } else if (risky > 0) {
            out.add("有 " + risky + " 门课程出现预警（最高"
                    + (sum.getOrange() > 0 ? "橙色" : "黄色") + "），建议尽快找任课教师确认改进办法。");
        }
        if (risky >= 2) {
            out.add("同时有 " + risky + " 门课程出现预警，学业压力较大，可以向辅导员申请学业辅导或朋辈帮扶。");
        }
        if (risky == 0 && sum.getTotal() > 0) {
            out.add("本学期所有课程都在安全区，保持现在的出勤与作业节奏即可。");
        }
        if (sum.getAppealed() > 0) {
            out.add("已提交 " + sum.getAppealed() + " 条申诉，任课教师核实后会更新结论，请留意消息中心。");
        }
        for (StudentWarningVO.Item i : items) {
            if (Boolean.TRUE.equals(i.getHardFail())) {
                out.add(i.getCourseName() + " 已触及缺课硬线，请优先处理这门课。");
                break;
            }
        }
        return out;
    }

    /** 判定口径（学生可见版本） */
    private List<String> guidelines(RiskEngineConfig config) {
        List<String> g = new ArrayList<>();
        g.add("缺课上限：有效课次 ÷ 3 向下取整（例如 16 次课允许缺 5 次），缺课超过上限即按学校规定认定硬性不合格。");
        g.add("扣分规则：P = 2 × 缺课 + 1 × 迟到 + 1 × 超出 "
                + (config == null || config.getLeaveFreeTimes() == null ? 3 : config.getLeaveFreeTimes())
                + " 次的请假；加权分 T = 课程加权分 W − P，T 越高风险越低。");
        g.add("风险分级：风险分 ≥ " + num(config == null ? null : config.getThresholdRed(), 75)
                + " 红色、" + num(config == null ? null : config.getThresholdOrange(), 60) + "–"
                + num(config == null ? null : config.getThresholdRed(), 75) + " 橙色、"
                + num(config == null ? null : config.getThresholdYellow(), 40) + "–"
                + num(config == null ? null : config.getThresholdOrange(), 60) + " 黄色、低于 "
                + num(config == null ? null : config.getThresholdYellow(), 40) + " 绿色；缺课超限直接红色。");
        g.add("期末需考：按当前平时成绩测算的及格所需期末分数；若超过期末满分，说明按现状已不可达。");
        g.add("置信度：由考勤覆盖率、成绩完整性与样本量共同决定；数据不足时只提示行为风险，不给分数结论。");
        g.add("学生端只展示结论与建议，教师的处置记录属于内部工作记录；如对结论有疑问可提交申诉。");
        return g;
    }

    /** 学生可见的时间线：只保留结论性节点，不暴露教师的处理细节 */
    private List<StudentWarningVO.TimelineItem> timeline(AlertRecord a) {
        if (a == null) {
            return List.of();
        }
        Map<String, String> labelOf = new LinkedHashMap<>();
        labelOf.put("CREATED", "系统判定后生成预警");
        labelOf.put("LEVEL_UP", "风险等级升高");
        labelOf.put("LEVEL_DOWN", "风险等级降低");
        labelOf.put("APPEAL", "你提交了申诉");
        labelOf.put("CLOSED", "老师已处理并关闭");
        labelOf.put("DISMISSED", "老师核实后忽略该预警");
        labelOf.put("NOTIFIED", "已通知任课教师、辅导员和你");
        List<StudentWarningVO.TimelineItem> out = new ArrayList<>();
        for (AlertFlowLog f : flowMapper.selectList(Wrappers.<AlertFlowLog>lambdaQuery()
                .eq(AlertFlowLog::getAlertId, a.getId())
                .orderByAsc(AlertFlowLog::getEventTime)
                .orderByAsc(AlertFlowLog::getId))) {
            String label = labelOf.get(f.getEventType());
            if (label == null) {
                continue;
            }
            StudentWarningVO.TimelineItem t = new StudentWarningVO.TimelineItem();
            t.setEventType(f.getEventType());
            t.setLabel(label);
            t.setEventTime(f.getEventTime());
            out.add(t);
        }
        return out;
    }

    // ============================================================ 基础方法
    private Student requireStudent(LoginUser user) {
        if (user == null) {
            throw new BizException("请先登录");
        }
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        Student st = studentMapper.selectById(user.getRefId());
        if (st == null) {
            throw new BizException("学生档案不存在");
        }
        return st;
    }

    private Term resolveTerm(Long termId) {
        if (termId != null) {
            return termMapper.selectById(termId);
        }
        return termMapper.selectOne(Wrappers.<Term>lambdaQuery()
                .eq(Term::getIsCurrent, 1)
                .orderByDesc(Term::getId)
                .last("limit 1"));
    }

    private RiskEngineConfig activeConfig() {
        return configMapper.selectOne(Wrappers.<RiskEngineConfig>lambdaQuery()
                .eq(RiskEngineConfig::getIsActive, 1)
                .orderByDesc(RiskEngineConfig::getId)
                .last("limit 1"));
    }

    /** 每门课当前预警（is_current=1），没有再取最近一条已关闭的（用于展示"已处理"结论） */
    private Map<Long, AlertRecord> currentAlerts(List<Enrollment> enrollments) {
        List<Long> ids = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());
        Map<Long, AlertRecord> out = new LinkedHashMap<>();
        for (AlertRecord a : alertMapper.selectList(Wrappers.<AlertRecord>lambdaQuery()
                .in(AlertRecord::getEnrollmentId, ids)
                .eq(AlertRecord::getIsVoid, 0)
                .orderByAsc(AlertRecord::getId))) {
            out.put(a.getEnrollmentId(), a);
        }
        return out;
    }

    private AlertRecord latestAlert(Long enrollmentId) {
        return alertMapper.selectOne(Wrappers.<AlertRecord>lambdaQuery()
                .eq(AlertRecord::getEnrollmentId, enrollmentId)
                .eq(AlertRecord::getIsVoid, 0)
                .orderByDesc(AlertRecord::getId)
                .last("limit 1"));
    }

    private Map<Long, TeachingClass> batchClass(List<Enrollment> enrollments) {
        List<Long> ids = enrollments.stream().map(Enrollment::getTeachingClassId)
                .filter((x) -> x != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return classMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(TeachingClass::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, CourseOffering> batchOffering(List<Enrollment> enrollments) {
        List<Long> ids = enrollments.stream().map(Enrollment::getOfferingId)
                .filter((x) -> x != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return offeringMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(CourseOffering::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Course> batchCourse(java.util.Collection<CourseOffering> offerings) {
        List<Long> ids = offerings.stream().map(CourseOffering::getCourseId)
                .filter((x) -> x != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return courseMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Teacher> batchTeacher(java.util.Collection<TeachingClass> classes) {
        List<Long> ids = classes.stream().map(TeachingClass::getTeacherId)
                .filter((x) -> x != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return teacherMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Teacher::getId, Function.identity(), (a, b) -> a));
    }

    private String triggerRule(RiskState s) {
        if (eq1(s.getHardFail())) {
            return "A_HARD_LINE";
        }
        if (eq1(s.getFinalUnreachable())) {
            return "A_UNREACHABLE";
        }
        if (s.getRemainingAbsent() != null && s.getRemainingAbsent() <= 1) {
            return "A_NEAR_LINE";
        }
        return "B_RANK";
    }

    private static int rank(String level) {
        if (level == null) {
            return 0;
        }
        switch (level) {
            case "RED":
                return LEVEL_RED;
            case "ORANGE":
                return LEVEL_ORANGE;
            case "YELLOW":
                return LEVEL_YELLOW;
            default:
                return 0;
        }
    }

    private static boolean eq1(Integer v) {
        return v != null && v == 1;
    }

    private static String num(BigDecimal v, int fallback) {
        return v == null ? String.valueOf(fallback) : v.stripTrailingZeros().toPlainString();
    }
}
