package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.CourseGroup;
import com.example.academic_risk_warning.entity.CourseOffering;
import com.example.academic_risk_warning.entity.Enrollment;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ProjectContribution;
import com.example.academic_risk_warning.entity.ProjectSubmission;
import com.example.academic_risk_warning.entity.ProjectTask;
import com.example.academic_risk_warning.entity.ScoreItem;
import com.example.academic_risk_warning.entity.ScoreScheme;
import com.example.academic_risk_warning.entity.ScoreSchemeItem;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.Submission;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.CourseGroupMapper;
import com.example.academic_risk_warning.mapper.CourseOfferingMapper;
import com.example.academic_risk_warning.mapper.EnrollmentMapper;
import com.example.academic_risk_warning.mapper.PaperMapper;
import com.example.academic_risk_warning.mapper.ProjectContributionMapper;
import com.example.academic_risk_warning.mapper.ProjectSubmissionMapper;
import com.example.academic_risk_warning.mapper.ProjectTaskMapper;
import com.example.academic_risk_warning.mapper.ScoreItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeItemMapper;
import com.example.academic_risk_warning.mapper.ScoreSchemeMapper;
import com.example.academic_risk_warning.mapper.StudentMapper;
import com.example.academic_risk_warning.mapper.SubmissionMapper;
import com.example.academic_risk_warning.util.JsonObjects;
import com.example.academic_risk_warning.util.RiskMath;
import com.example.academic_risk_warning.vo.ScoreCellVO;
import com.example.academic_risk_warning.vo.ScoreItemColumnVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成绩折算：列矩阵 → 两级加权 → 过程分 / 覆盖率。
 *
 * <p>从 {@link ScoreMatrixService} 抽出，作为"成绩单页"与"风险判定引擎"的**唯一**折算入口：
 * 同一份成绩只允许有一套数，否则预警结论会和成绩单互相打脸（M16 明确要求）。</p>
 *
 * <p>口径：</p>
 * <ul>
 *   <li>构成是两级的：根项（如「平时成绩 40」）带子项，子项权重是父项内部百分比 →
 *       绝对权重 = 父.weight × 子.weight / 100；根项自己就是叶子时用自身 weight；</li>
 *   <li>加权分 W = Σ 根项(根项百分制分 × 根权重 / 100)；子项先按父项内部权重折成根项百分制；</li>
 *   <li>过程分 = **非终结性分支**中有数据来源的分项的加权平均（0–100，按已录项归一）；</li>
 *   <li>覆盖率 = 有数据来源的分项绝对权重 / 非终结分支绝对权重（按课程算，不按人算）；</li>
 *   <li>缺考/未交按 0 分计入，但"未录入"仍标 {@code missing}，由调用方决定是否拦（成绩单页拦、预警页按覆盖率降级）。</li>
 * </ul>
 *
 * <p>这里不注入任何业务判断（扣分、及格线、风险等级）——那些留在 RiskMath / 各自的 service 里。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MAX_PARENT_DEPTH = 8;

    private final ScoreSchemeMapper schemeMapper;
    private final ScoreSchemeItemMapper schemeItemMapper;
    private final ScoreItemMapper scoreItemMapper;
    private final EnrollmentMapper enrollmentMapper;
    private final StudentMapper studentMapper;
    private final CourseOfferingMapper offeringMapper;
    private final CourseGroupMapper groupMapper;
    private final PaperMapper paperMapper;
    private final SubmissionMapper submissionMapper;
    private final ProjectTaskMapper projectTaskMapper;
    private final ProjectSubmissionMapper projectSubmissionMapper;
    private final ProjectContributionMapper projectContributionMapper;

    // ============================================================ 对外入口
    /** 一次算出一个教学班的全部折算结果（列、单元格、加权分、过程分、覆盖率、绝对权重）。 */
    public ClassScore compute(TeachingClass tc) {
        CourseOffering offering = offeringMapper.selectById(tc.getOfferingId());
        ScoreScheme scheme = schemeOf(offering);
        if (scheme == null) {
            throw new BizException("该课程的老师还没有配置并锁定成绩构成，不能算分");
        }
        List<ScoreSchemeItem> all = schemeItems(scheme);
        Map<Long, ScoreSchemeItem> byId = new LinkedHashMap<>();
        for (ScoreSchemeItem it : all) {
            byId.put(it.getId(), it);
        }

        ClassScore out = new ClassScore();
        out.offering = offering;
        out.scheme = scheme;
        out.items = all;
        out.roots = all.stream().filter(i -> i.getParentId() == null).collect(Collectors.toList());
        out.columns = columns(all, out.roots);
        out.effectiveSessions = effectiveSessions(offering);
        out.passScore = passScore(offering);

        List<Enrollment> ens = enrollmentMapper.selectList(Wrappers.<Enrollment>lambdaQuery()
                .eq(Enrollment::getTeachingClassId, tc.getId())
                .orderByAsc(Enrollment::getId));
        out.enrollments = ens;
        out.students = students(ens);

        Map<Long, Map<Long, BigDecimal>> manual = manualScores(ens);
        List<Long> studentIds = ens.stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());
        Map<Long, Map<Long, ScoreCellVO>> auto = autoScores(tc, out.columns, studentIds);

        out.weights = weights(out.columns, byId, manual, auto);
        out.finalRatio = RiskMath.finalRatio(out.weights);

        Map<Long, String> pathOf = new LinkedHashMap<>();
        for (ScoreItemColumnVO c : out.columns) {
            pathOf.put(c.getSchemeItemId(), c.getPath());
        }

        for (Enrollment e : ens) {
            StudentScore ss = new StudentScore();
            ss.enrollmentId = e.getId();
            ss.studentId = e.getStudentId();
            ss.cells = cells(e.getStudentId(), out.columns, manual, auto);
            ss.missingPaths = new ArrayList<>();
            Map<Long, BigDecimal> pct = new LinkedHashMap<>();
            for (ScoreItemColumnVO c : out.columns) {
                ScoreCellVO cell = ss.cells.get(c.getSchemeItemId());
                boolean missing = cell == null || Boolean.TRUE.equals(cell.getMissing());
                if (missing) {
                    ss.missingPaths.add(pathOf.get(c.getSchemeItemId()));
                } else {
                    pct.put(c.getSchemeItemId(), cell.getScore());
                }
            }
            ss.weightedScore = weightedScore(out.roots, out.items, ss.cells);
            RiskMath.ProcessScore ps = RiskMath.processScore(out.weights, pct);
            ss.processScore = ps.score;
            ss.processCoverage = ps.coverage;
            RiskMath.ProcessScore ts = RiskMath.terminalScore(out.weights, pct);
            ss.terminalScore = ts.score;
            ss.terminalCoverage = ts.coverage;
            ss.missingWeight = missingWeight(out.weights, ss.cells);
            out.byEnrollment.put(e.getId(), ss);
            out.byStudent.put(e.getStudentId(), ss);
        }
        return out;
    }

    // ============================================================ 结果载体
    /** 一个教学班的折算结果 */
    public static class ClassScore {
        public CourseOffering offering;
        public ScoreScheme scheme;
        public List<ScoreSchemeItem> items;
        public List<ScoreSchemeItem> roots;
        public List<ScoreItemColumnVO> columns;
        public List<Enrollment> enrollments;
        public Map<Long, Student> students;
        public List<RiskMath.SchemeWeight> weights;
        public Map<Long, StudentScore> byEnrollment;
        public Map<Long, StudentScore> byStudent;
        public BigDecimal finalRatio;
        public int effectiveSessions;
        public int passScore;

        public ClassScore() {
            this.byEnrollment = new LinkedHashMap<>();
            this.byStudent = new LinkedHashMap<>();
            this.weights = new ArrayList<>();
        }
    }

    /** 一个学生的折算结果 */
    public static class StudentScore {
        public Long enrollmentId;
        public Long studentId;
        /** 叶子评分项的单元格（schemeItemId → cell），未录入的 cell 标 missing */
        public Map<Long, ScoreCellVO> cells;
        /** 未录入的分项路径（成绩单页据此判 NO_SCORE） */
        public List<String> missingPaths;
        /** 含终结性分支的加权分（成绩单口径，未变） */
        public BigDecimal weightedScore;
        /** 非终结性分支的加权平均 0–100（M16 预警口径），无可用分项时为 null */
        public BigDecimal processScore;
        /** 过程分覆盖率 0–1（按课程算，同班每人相同） */
        public BigDecimal processCoverage;
        /** 终结性分支（期末/大作业）已录分时的加权平均 0–100，无数据为 null */
        public BigDecimal terminalScore;
        /** 终结性分支的覆盖率 0–1 */
        public BigDecimal terminalCoverage;
        /** 该生缺的"有数据来源"非终结分项绝对权重（预警闸门用） */
        public BigDecimal missingWeight;
    }

    // ============================================================ 内部：构成与列
    /** 方案：开课 → 课程组 → 最新版本（不能拿 offeringId 直接当 groupId，两者只是恰好撞号） */
    private ScoreScheme schemeOf(CourseOffering offering) {
        if (offering == null) {
            return null;
        }
        CourseGroup group = groupMapper.selectOne(Wrappers.<CourseGroup>lambdaQuery()
                .eq(CourseGroup::getCourseId, offering.getCourseId())
                .eq(CourseGroup::getTermId, offering.getTermId()).last("limit 1"));
        if (group == null) {
            return null;
        }
        return schemeMapper.selectOne(Wrappers.<ScoreScheme>lambdaQuery()
                .eq(ScoreScheme::getGroupId, group.getId())
                .orderByDesc(ScoreScheme::getVersion).last("limit 1"));
    }

    private List<ScoreSchemeItem> schemeItems(ScoreScheme scheme) {
        return schemeItemMapper.selectList(Wrappers.<ScoreSchemeItem>lambdaQuery()
                .eq(ScoreSchemeItem::getSchemeId, scheme.getId())
                .eq(ScoreSchemeItem::getStatus, "ACTIVE")
                .orderByAsc(ScoreSchemeItem::getSortNo));
    }

    /** 矩阵的列 = 叶子评分项 */
    private List<ScoreItemColumnVO> columns(List<ScoreSchemeItem> all, List<ScoreSchemeItem> roots) {
        Map<Long, String> rootNames = new LinkedHashMap<>();
        for (ScoreSchemeItem r : roots) {
            rootNames.put(r.getId(), r.getItemName());
        }
        List<ScoreItemColumnVO> columns = new ArrayList<>();
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
        return columns;
    }

    private int effectiveSessions(CourseOffering offering) {
        return offering == null || offering.getEffectiveSessions() == null ? 0 : offering.getEffectiveSessions();
    }

    private int passScore(CourseOffering offering) {
        return offering == null || offering.getPassScore() == null ? 60 : offering.getPassScore();
    }

    private Map<Long, Student> students(List<Enrollment> ens) {
        Map<Long, Student> out = new LinkedHashMap<>();
        List<Long> ids = ens.stream().map(Enrollment::getStudentId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return out;
        }
        for (Student s : studentMapper.selectBatchIds(ids)) {
            out.put(s.getId(), s);
        }
        return out;
    }

    // ============================================================ 内部：手工分 / 自动分
    /** 手工分项：按 (studentId, schemeItemId) 取多批次平均，换算成百分制 */
    private Map<Long, Map<Long, BigDecimal>> manualScores(List<Enrollment> ens) {
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
                    BigDecimal teamSize = BigDecimal.ONE;
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
                            List<Map<String, String>> mems = JsonObjects.arrayObjects(pc.getContributionJson());
                            for (Map<String, String> mem : mems) {
                                if (String.valueOf(sid).equals(mem.get("studentId"))) {
                                    mine = s;
                                    try {
                                        ratio = new BigDecimal(mem.get("ratio"));
                                    } catch (RuntimeException ignored) {
                                        ratio = BigDecimal.ONE;
                                    }
                                    // 个人得分 = 团队得分 × 个人占比 × 组员数：等分时等于团队分，超出均值按比例上浮（上限 100）
                                    teamSize = BigDecimal.valueOf(Math.max(1, mems.size()));
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
                        pct = mine.getProjectScore().multiply(ratio).multiply(teamSize).multiply(HUNDRED)
                                .divide(full, 2, RoundingMode.HALF_UP);
                        if (pct.compareTo(HUNDRED) > 0) {
                            pct = HUNDRED;
                        }
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

    // ============================================================ 内部：权重与折算
    /** 绝对权重 + 终结性标记 + 本班是否有数据来源 */
    private List<RiskMath.SchemeWeight> weights(List<ScoreItemColumnVO> columns, Map<Long, ScoreSchemeItem> byId,
                                                Map<Long, Map<Long, BigDecimal>> manual,
                                                Map<Long, Map<Long, ScoreCellVO>> auto) {
        Set<Long> manualItems = new LinkedHashSet<>();
        for (Map<Long, BigDecimal> m : manual.values()) {
            manualItems.addAll(m.keySet());
        }
        Set<Long> autoItems = new LinkedHashSet<>();
        for (Map<Long, ScoreCellVO> m : auto.values()) {
            autoItems.addAll(m.keySet());
        }
        List<RiskMath.SchemeWeight> out = new ArrayList<>();
        for (ScoreItemColumnVO c : columns) {
            ScoreSchemeItem leaf = byId.get(c.getSchemeItemId());
            BigDecimal absW = absWeight(leaf, byId);
            boolean terminal = isTerminal(leaf, byId);
            boolean available = "AUTO".equalsIgnoreCase(c.getDataSource())
                    ? autoItems.contains(c.getSchemeItemId())
                    : manualItems.contains(c.getSchemeItemId());
            out.add(new RiskMath.SchemeWeight(c.getSchemeItemId(), absW, terminal, available));
        }
        return out;
    }

    /** 绝对权重：根项取自身 weight；子项 = 父.weight × 子.weight / 100（递归以兼容多层） */
    private BigDecimal absWeight(ScoreSchemeItem leaf, Map<Long, ScoreSchemeItem> byId) {
        if (leaf == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal w = leaf.getWeight() == null ? BigDecimal.ZERO : leaf.getWeight();
        Long parentId = leaf.getParentId();
        int depth = 0;
        while (parentId != null && depth++ < MAX_PARENT_DEPTH) {
            ScoreSchemeItem p = byId.get(parentId);
            if (p == null) {
                break;
            }
            BigDecimal pw = p.getWeight() == null ? BigDecimal.ZERO : p.getWeight();
            w = w.multiply(pw).divide(HUNDRED, 6, RoundingMode.HALF_UP);
            parentId = p.getParentId();
        }
        return w.setScale(4, RoundingMode.HALF_UP);
    }

    /** 是否挂在终结性分支（根项 category=FINAL）下 */
    private boolean isTerminal(ScoreSchemeItem leaf, Map<Long, ScoreSchemeItem> byId) {
        ScoreSchemeItem cur = leaf;
        int depth = 0;
        while (cur != null && depth++ < MAX_PARENT_DEPTH) {
            if (cur.getParentId() == null) {
                return "FINAL".equalsIgnoreCase(cur.getCategory());
            }
            cur = byId.get(cur.getParentId());
        }
        return false;
    }

    /** 一个学生的单元格：自动优先，其次手工；缺失留空 cell 并标 missing */
    private Map<Long, ScoreCellVO> cells(Long studentId, List<ScoreItemColumnVO> columns,
                                         Map<Long, Map<Long, BigDecimal>> manual,
                                         Map<Long, Map<Long, ScoreCellVO>> auto) {
        Map<Long, ScoreCellVO> cells = new LinkedHashMap<>();
        Map<Long, ScoreCellVO> autoRow = auto.get(studentId);
        Map<Long, BigDecimal> manualRow = manual.get(studentId);
        for (ScoreItemColumnVO c : columns) {
            ScoreCellVO cell = null;
            if (autoRow != null && autoRow.containsKey(c.getSchemeItemId())) {
                cell = autoRow.get(c.getSchemeItemId());
            } else if (manualRow != null && manualRow.get(c.getSchemeItemId()) != null) {
                cell = new ScoreCellVO();
                cell.setScore(manualRow.get(c.getSchemeItemId()));
                cell.setManual(true);
                cell.setSource("MANUAL");
                cell.setMissing(false);
            }
            if (cell == null) {
                cell = new ScoreCellVO();
                cell.setMissing(true);
                cell.setManual(true);
            }
            cells.put(c.getSchemeItemId(), cell);
        }
        return cells;
    }

    /** 含终结分支的加权分（与改造前完全一致，保证成绩单页数字不变） */
    private BigDecimal weightedScore(List<ScoreSchemeItem> roots, List<ScoreSchemeItem> all,
                                     Map<Long, ScoreCellVO> cells) {
        BigDecimal weighted = BigDecimal.ZERO;
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
        return weighted.setScale(2, RoundingMode.HALF_UP);
    }

    /** 该生缺的"有数据来源"非终结分项绝对权重之和（预警闸门用） */
    private BigDecimal missingWeight(List<RiskMath.SchemeWeight> weights, Map<Long, ScoreCellVO> cells) {
        BigDecimal sum = BigDecimal.ZERO;
        for (RiskMath.SchemeWeight w : weights) {
            if (w.terminal || !w.available) {
                continue;
            }
            ScoreCellVO cell = cells.get(w.itemId);
            if (cell == null || Boolean.TRUE.equals(cell.getMissing()) || cell.getScore() == null) {
                sum = sum.add(w.absWeight);
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }
}
