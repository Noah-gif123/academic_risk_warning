package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.vo.ai.AdminClassFact;
import com.example.academic_risk_warning.vo.ai.ClassFact;
import com.example.academic_risk_warning.vo.ai.GroupReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 群体报告（M8）：教学班报告与行政班报告。
 *
 * <p>与学生报告同一套哲学：**数字由规则算，模型只写结论与建议**；
 * 通道三层（Dify 群体工作流 → 直连模型 → 规则模板），任何一层失败都能出报告。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupReportService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GroupFactBuilder factBuilder;
    private final ReportComposer composer;
    private final DifyClient difyClient;
    private final DashScopeClient dashScopeClient;
    private final AiRunService runService;
    private final AiProperties props;
    private final JdbcTemplate jdbc;

    // ============================================================ 教学班报告
    public GroupReportVO generateClassReport(Long teachingClassId, Long operatorId) {
        ClassFact fact = factBuilder.buildClass(teachingClassId);
        String title = nz(fact.getClassName()) + " · 班级学情报告";
        ReportContent content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        Long runId = runService.start("GROUP_REPORT", "TEACHING_CLASS", teachingClassId, operatorId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        String userPrompt = classPrompt(fact);
        String systemPrompt = groupSystemPrompt("教学班");

        if (props.isEnabled() && props.getGroupReportAppKey() != null && !props.getGroupReportAppKey().isBlank()) {
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getGroupReportAppKey(),
                    classInputs(fact), "teacher-" + operatorId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-group-workflow", "class-report", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeGroupReport(text)) {
                    content = parseContent(text);
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        } else {
            note.append("未配置群体报告工作流；");
        }

        if (content == null && props.directReady()) {
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt);
            tokens += res.getTotalTokens();
            long t1 = System.currentTimeMillis();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "class-report", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeGroupReport(res.getText())) {
                    content = parseContent(res.getText());
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (content == null) {
            content = classTemplate(fact);
            note.append("已使用规则模板");
        }

        Long id = persist("CLASS", teachingClassId, fact.getTeachingClassId(), null,
                fact.getOfferingId(), fact.getCourseId(), fact.getTermId(), title,
                fact, content, source, runId, tokens, operatorId);
        runService.usage(runId, "GROUP_REPORT", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 行政班报告
    public GroupReportVO generateAdminClassReport(Long adminClassId, Long operatorId) {
        AdminClassFact fact = factBuilder.buildAdminClass(adminClassId);
        String title = nz(fact.getClassName()) + " · 行政班学情报告";
        ReportContent content = null;
        String source = "TEMPLATE";
        String status = "FALLBACK";
        StringBuilder note = new StringBuilder();
        Long runId = runService.start("GROUP_REPORT", "ADMIN_CLASS", adminClassId, operatorId);
        long t0 = System.currentTimeMillis();
        int tokens = 0;

        String systemPrompt = groupSystemPrompt("行政班");
        String userPrompt = adminPrompt(fact);

        if (props.isEnabled() && props.getGroupReportAppKey() != null && !props.getGroupReportAppKey().isBlank()) {
            DifyClient.WorkflowResult res = difyClient.runWorkflow(props.getGroupReportAppKey(),
                    adminInputs(fact), "counselor-" + operatorId);
            tokens += res.getTotalTokens();
            if (res.isOk()) {
                String text = pickText(res.getOutputs());
                List<String> bad = composer.invalidNumbersAgainst(text, fact);
                runService.step(runId, 1, "dify-group-workflow", "admin-report", text,
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t0);
                if (bad.isEmpty() && composer.looksLikeGroupReport(text)) {
                    content = parseContent(text);
                    source = "DIFY";
                    status = "SUCCESS";
                } else {
                    note.append("Dify 输出不可用；");
                }
            } else {
                note.append("Dify 失败：").append(res.getError()).append("；");
            }
        } else {
            note.append("未配置群体报告工作流；");
        }

        if (content == null && props.directReady()) {
            DashScopeClient.ChatResult res = dashScopeClient.chat(systemPrompt, userPrompt);
            tokens += res.getTotalTokens();
            long t1 = System.currentTimeMillis();
            if (res.isOk()) {
                List<String> bad = composer.invalidNumbersAgainst(res.getText(), fact);
                runService.step(runId, 2, "direct-chat", "admin-report", res.getText(),
                        bad.isEmpty() ? "PASS" : "WARN",
                        bad.isEmpty() ? null : "越界数字 " + bad.stream().distinct().limit(6).toList(),
                        System.currentTimeMillis() - t1);
                if (bad.isEmpty() && composer.looksLikeGroupReport(res.getText())) {
                    content = parseContent(res.getText());
                    source = "LLM";
                    status = "SUCCESS";
                } else {
                    note.append("直连输出不可用；");
                }
            } else {
                note.append("直连失败：").append(res.getError()).append("；");
            }
        }

        if (content == null) {
            content = adminTemplate(fact);
            note.append("已使用规则模板");
        }

        Long id = persist("ADMIN_CLASS", adminClassId, null, adminClassId,
                null, null, fact.getTermId(), title, fact, content, source, runId, tokens, operatorId);
        runService.usage(runId, "GROUP_REPORT", tokens, operatorId);
        runService.finish(runId, status, source, tokens, status.equals("SUCCESS") ? null : note.toString());
        return detail(id);
    }

    // ============================================================ 权限
    /** 教师只能操作自己任教的教学班 */
    public void assertTeacherOwns(Long teachingClassId, Long teacherId) {
        List<Long> owners = jdbc.queryForList(
                "select teacher_id from teaching_class where id = ?", Long.class, teachingClassId);
        if (owners.isEmpty()) {
            throw new BizException("教学班不存在");
        }
        if (teacherId != null && !teacherId.equals(owners.get(0))) {
            throw new BizException("只能查看自己任教的教学班");
        }
    }

    /** 辅导员只能操作自己带的行政班 */
    public void assertCounselorOwns(Long adminClassId, Long counselorId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from counselor_class where admin_class_id = ? and (? is null or counselor_id = ?)",
                Integer.class, adminClassId, counselorId, counselorId);
        if (n == null || n == 0) {
            throw new BizException("不在你负责的行政班范围内");
        }
    }

    // ============================================================ 查询
    public GroupReportVO latest(String scope, Long targetId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id from ai_group_report where scope = ? and target_id = ? and status = 'PUBLISHED' order by id desc limit 1",
                scope, targetId);
        if (rows.isEmpty()) {
            return null;
        }
        return detail(((Number) rows.get(0).get("id")).longValue());
    }

    public List<GroupReportVO> list(String scope, Long targetId, int limit) {
        List<GroupReportVO> out = new ArrayList<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "select id from ai_group_report where scope = ? and target_id = ? order by id desc limit ?",
                scope, targetId, limit)) {
            out.add(detail(((Number) row.get("id")).longValue()));
        }
        return out;
    }

    public GroupReportVO detail(Long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, scope, target_id, teaching_class_id, admin_class_id, title, summary, content_json, source, create_time "
                        + "from ai_group_report where id = ?", id);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("报告不存在：" + id);
        }
        Map<String, Object> r = rows.get(0);
        GroupReportVO vo = new GroupReportVO();
        vo.setId(((Number) r.get("id")).longValue());
        vo.setScope(String.valueOf(r.get("scope")));
        vo.setTargetId(r.get("target_id") == null ? null : ((Number) r.get("target_id")).longValue());
        vo.setTeachingClassId(r.get("teaching_class_id") == null ? null : ((Number) r.get("teaching_class_id")).longValue());
        vo.setAdminClassId(r.get("admin_class_id") == null ? null : ((Number) r.get("admin_class_id")).longValue());
        vo.setTitle(String.valueOf(r.get("title")));
        vo.setSummary(r.get("summary") == null ? null : String.valueOf(r.get("summary")));
        vo.setSource(String.valueOf(r.get("source")));
        vo.setCreateTime(r.get("create_time") == null ? null : String.valueOf(r.get("create_time")));
        parseInto(String.valueOf(r.get("content_json")), vo);
        return vo;
    }

    // ============================================================ 落库
    private Long persist(String scope, Long targetId, Long teachingClassId, Long adminClassId,
                         Long offeringId, Long courseId, Long termId, String title,
                         Object fact, ReportContent content, String source, Long runId, int tokens, Long operatorId) {
        String factJson = toJson(fact);
        String contentJson = toJson(content.toSections());
        BigDecimal cost = BigDecimal.valueOf(props.costOf(tokens));
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into ai_group_report(scope, target_id, teaching_class_id, admin_class_id, offering_id, course_id, term_id,
                                                title, fact_json, content_json, summary, status, source, model, app_id,
                                                prompt_version, run_id, tokens, cost, create_by, publish_time)
                    values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            ps.setString(i++, scope);
            ps.setLong(i++, targetId);
            setNullableLong(ps, i++, teachingClassId);
            setNullableLong(ps, i++, adminClassId);
            setNullableLong(ps, i++, offeringId);
            setNullableLong(ps, i++, courseId);
            setNullableLong(ps, i++, termId);
            ps.setString(i++, title);
            ps.setString(i++, factJson);
            ps.setString(i++, contentJson);
            ps.setString(i++, content.getSummary());
            ps.setString(i++, "PUBLISHED");
            ps.setString(i++, source);
            ps.setString(i++, props.getModel());
            ps.setString(i++, props.getGroupReportAppKey() == null || props.getGroupReportAppKey().isBlank()
                    ? null : maskKey(props.getGroupReportAppKey()));
            ps.setString(i++, "group-report-v1");
            setNullableLong(ps, i++, runId);
            ps.setInt(i++, tokens);
            ps.setBigDecimal(i++, cost);
            setNullableLong(ps, i++, operatorId);
            ps.setObject(i, LocalDateTime.now());
            return ps;
        }, kh);
        return kh.getKey() == null ? null : kh.getKey().longValue();
    }

    private static void setNullableLong(PreparedStatement ps, int idx, Long v) throws java.sql.SQLException {
        if (v == null) {
            ps.setNull(idx, java.sql.Types.BIGINT);
        } else {
            ps.setLong(idx, v);
        }
    }

    // ============================================================ 提示词
    private String groupSystemPrompt(String kind) {
        return """
                你是高校学业预警系统里的「群体学情研判助手」，服务对象是教师与辅导员。
                必须遵守：
                1. 只能使用用户给出的数字，禁止自己计算、换算或编造任何数字、比例、排名；
                2. 四段结构，段落标题只能是：%s；
                3. 面向教师/辅导员，语气专业、结论先行，每段 1 到 3 句；
                4. 不使用 Markdown 星号、表格、代码块；数字只挑最关键的 5 个以内；
                5. 总字数 350 到 550 字，最后一段给出 3 条可直接执行的动作；
                6. 直接输出正文，不要复述规则、不要输出思考过程。
                """.formatted("教学班".equals(kind)
                ? "【风险概览】【共性难点】【重点关注】【教学建议】"
                : "【班级画像】【集中风险课程】【多门预警学生】【干预建议】");
    }

    private String classPrompt(ClassFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(nz(f.getCourseName())).append("（").append(nz(f.getCourseType()))
                .append("，").append(nzf(f.getCredit())).append(" 学分，").append(nzf(f.getPlannedWeeks())).append(" 周）\n");
        sb.append("教学班：").append(nz(f.getClassName())).append("，任课教师：").append(nz(f.getTeacherName()))
                .append("，学期：").append(nz(f.getTermName())).append("\n");
        sb.append("人数：").append(nzf(f.getStudentCount()))
                .append("；风险分布：红灯 ").append(nzf(f.getRedCount()))
                .append("、黄灯 ").append(nzf(f.getYellowCount()))
                .append("、绿灯 ").append(nzf(f.getGreenCount())).append("\n");
        sb.append("总分：班级平均 ").append(num(f.getAvgScore()))
                .append("，最低 ").append(num(f.getMinScore()))
                .append("，最高 ").append(num(f.getMaxScore()))
                .append("，及格线 ").append(nzf(f.getPassScore())).append("\n");
        sb.append("考勤：缺勤超上限 ").append(nzf(f.getAbsentOverLimitCount()))
                .append(" 人，人均缺勤 ").append(num(f.getAvgAbsent()))
                .append(" 次，最多缺勤 ").append(nzf(f.getMaxAbsent())).append(" 次\n");
        sb.append("薄弱分组（正确率由低到高）：");
        for (ClassFact.Dim d : f.getWeakGroups()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n薄弱题型：");
        for (ClassFact.Dim d : f.getWeakTypes()) {
            sb.append(d.getName()).append("=").append(pct(d.getRate())).append("；");
        }
        sb.append("\n低分分项：");
        for (ClassFact.Item it : f.getWeakItems()) {
            sb.append(it.getName()).append("=").append(num(it.getAvgScore())).append("；");
        }
        sb.append("\n未关闭预警事件：").append(nzf(f.getActiveAlertCount())).append(" 条\n");
        sb.append("\n请据此写这个教学班的学情研判。");
        return sb.toString();
    }

    private Map<String, Object> classInputs(ClassFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("facts", classPrompt(f));
        in.put("kind", "class");
        in.put("course", nz(f.getCourseName()));
        in.put("class_name", nz(f.getClassName()));
        in.put("student_count", nzf(f.getStudentCount()));
        in.put("red", nzf(f.getRedCount()));
        in.put("yellow", nzf(f.getYellowCount()));
        in.put("green", nzf(f.getGreenCount()));
        in.put("avg_score", num(f.getAvgScore()));
        return in;
    }

    private String adminPrompt(AdminClassFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("行政班：").append(nz(f.getClassName())).append("（").append(nzf(f.getGradeYear()))
                .append(" 级，").append(nz(f.getMajorName())).append("），辅导员：").append(nz(f.getCounselorName()))
                .append("，学期：").append(nz(f.getTermName())).append("\n");
        sb.append("规模：学生 ").append(nzf(f.getStudentCount())).append(" 人，本学期待评课程 ")
                .append(nzf(f.getCourseCount())).append(" 门，选课记录 ").append(nzf(f.getEnrollmentCount())).append(" 条\n");
        sb.append("风险分布：红灯 ").append(nzf(f.getRedCount()))
                .append("、黄灯 ").append(nzf(f.getYellowCount()))
                .append("、绿灯 ").append(nzf(f.getGreenCount()))
                .append("，挂科风险率 ").append(num(f.getFailRate())).append("%\n");
        sb.append("平均总分：").append(num(f.getAvgScore())).append("；未关闭预警事件：")
                .append(nzf(f.getActiveAlertCount())).append(" 条\n");
        sb.append("集中风险课程：");
        for (AdminClassFact.CourseRisk c : f.getCourseRisks()) {
            sb.append(c.getCourseName()).append("（红 ").append(nzf(c.getRed()))
                    .append("、黄 ").append(nzf(c.getYellow())).append("，风险率 ")
                    .append(num(c.getFailRate())).append("%）；");
        }
        sb.append("\n多门预警学生（≥2 门）：");
        for (AdminClassFact.Student s : f.getMultiRiskStudents()) {
            sb.append(s.getStudentName()).append("（").append(nzf(s.getRiskCount())).append(" 门：")
                    .append(nz(s.getRiskCourses())).append("）；");
        }
        sb.append("\n请据此写这个行政班的学情研判与干预建议。");
        return sb.toString();
    }

    private Map<String, Object> adminInputs(AdminClassFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        in.put("facts", adminPrompt(f));
        in.put("kind", "admin");
        in.put("class_name", nz(f.getClassName()));
        in.put("student_count", nzf(f.getStudentCount()));
        in.put("red", nzf(f.getRedCount()));
        in.put("yellow", nzf(f.getYellowCount()));
        in.put("green", nzf(f.getGreenCount()));
        in.put("fail_rate", num(f.getFailRate()));
        return in;
    }

    // ============================================================ 模板兜底
    private ReportContent classTemplate(ClassFact f) {
        List<ReportContent.Section> sections = new ArrayList<>();
        String overview = "本班 " + nzf(f.getStudentCount()) + " 人，红灯 " + nzf(f.getRedCount())
                + " 人、黄灯 " + nzf(f.getYellowCount()) + " 人、绿灯 " + nzf(f.getGreenCount())
                + " 人；班级平均 " + num(f.getAvgScore()) + " 分，及格线 " + nzf(f.getPassScore()) + " 分。";
        sections.add(new ReportContent.Section("风险概览", overview));

        StringBuilder weak = new StringBuilder();
        if (!f.getWeakGroups().isEmpty()) {
            weak.append("正确率最低的分组：");
            int i = 1;
            for (ClassFact.Dim d : f.getWeakGroups()) {
                if (i > 3) {
                    break;
                }
                weak.append(i++).append("）「").append(d.getName()).append("」").append(pct(d.getRate())).append("；");
            }
        }
        if (!f.getWeakTypes().isEmpty()) {
            ClassFact.Dim t = f.getWeakTypes().get(0);
            weak.append("最弱题型是").append(t.getName()).append("（").append(pct(t.getRate())).append("）。");
        }
        sections.add(new ReportContent.Section("共性难点", weak.length() == 0 ? "暂无作答数据，无法判断共性难点。" : weak.toString()));

        StringBuilder watch = new StringBuilder();
        int red = nzfInt(f.getRedCount());
        int yellow = nzfInt(f.getYellowCount());
        if (red + yellow == 0) {
            watch.append("当前没有红黄灯学生，保持现有教学节奏即可。");
        } else {
            watch.append("需重点关注 ").append(red + yellow).append(" 人：");
            int i = 1;
            for (ClassFact.Student s : f.getWatchList()) {
                if (i > 5 || "GREEN".equals(s.getRiskLevel())) {
                    break;
                }
                watch.append(i++).append("）").append(s.getStudentName()).append("（")
                        .append("RED".equals(s.getRiskLevel()) ? "红灯" : "黄灯").append("，")
                        .append(num(s.getTotalScore())).append(" 分，").append(nz(s.getReason())).append("）；");
            }
        }
        sections.add(new ReportContent.Section("重点关注", watch.toString()));

        sections.add(new ReportContent.Section("教学建议",
                "一是针对正确率最低的分组安排一次课堂回顾；二是对红灯学生逐一约谈并在系统登记干预；"
                        + "三是核对未登分项，避免成绩不齐导致判定偏差。"));

        return new ReportContent(summaryOf(sections), sections);
    }

    private ReportContent adminTemplate(AdminClassFact f) {
        List<ReportContent.Section> sections = new ArrayList<>();
        sections.add(new ReportContent.Section("班级画像",
                nz(f.getClassName()) + " 共 " + nzf(f.getStudentCount()) + " 人，本学期待评课程 "
                        + nzf(f.getCourseCount()) + " 门，挂科风险率 " + num(f.getFailRate())
                        + "%，平均总分 " + num(f.getAvgScore()) + "。"));

        StringBuilder courses = new StringBuilder();
        if (f.getCourseRisks().isEmpty()) {
            courses.append("暂无集中风险课程。");
        } else {
            courses.append("风险最集中的课程：");
            int i = 1;
            for (AdminClassFact.CourseRisk c : f.getCourseRisks()) {
                courses.append(i++).append("）").append(c.getCourseName())
                        .append("（红 ").append(nzf(c.getRed())).append(" 人、黄 ").append(nzf(c.getYellow()))
                        .append(" 人）；");
            }
        }
        sections.add(new ReportContent.Section("集中风险课程", courses.toString()));

        StringBuilder multi = new StringBuilder();
        if (f.getMultiRiskStudents().isEmpty()) {
            multi.append("没有学生出现两门及以上预警。");
        } else {
            multi.append("需优先谈话 ").append(f.getMultiRiskStudents().size()).append(" 人：");
            int i = 1;
            for (AdminClassFact.Student s : f.getMultiRiskStudents()) {
                if (i > 5) {
                    break;
                }
                multi.append(i++).append("）").append(s.getStudentName()).append("（")
                        .append(nzf(s.getRiskCount())).append(" 门，").append(nz(s.getRiskCourses())).append("）；");
            }
        }
        sections.add(new ReportContent.Section("多门预警学生", multi.toString()));

        sections.add(new ReportContent.Section("干预建议",
                "一是先找多门预警学生谈话并登记干预记录；二是与集中风险课程的任课教师对齐，确认是否需要统一答疑；"
                        + "三是在系统中跟进干预后的等级变化，两周后复核一次。"));
        return new ReportContent(summaryOf(sections), sections);
    }

    private String summaryOf(List<ReportContent.Section> sections) {
        String s = sections.isEmpty() ? "" : sections.get(0).getText();
        return s.length() <= 290 ? s : s.substring(0, 289) + "…";
    }

    // ============================================================ 解析
    private ReportContent parseContent(String text) {
        List<ReportContent.Section> sections = new ArrayList<>();
        for (ReportComposer.GroupSection gs : composer.parseGroupSections(text)) {
            sections.add(new ReportContent.Section(gs.getTitle(), gs.getText()));
        }
        return new ReportContent(summaryOf(sections), sections);
    }

    private String pickText(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        for (String key : List.of("text", "result", "output", "report", "answer")) {
            Object v = outputs.get(key);
            if (v != null && !String.valueOf(v).isBlank()) {
                return String.valueOf(v);
            }
        }
        return outputs.values().stream().filter(Objects::nonNull).map(String::valueOf)
                .filter(s -> s.length() > 40).findFirst().orElse(null);
    }

    private void parseInto(String json, GroupReportVO vo) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            Map<String, Object> m = MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Object sec = m.get("sections");
            if (sec instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        GroupReportVO.Section s = new GroupReportVO.Section();
                        s.setTitle(mm.get("title") == null ? null : String.valueOf(mm.get("title")));
                        s.setText(mm.get("text") == null ? null : String.valueOf(mm.get("text")));
                        vo.getSections().add(s);
                    }
                }
            }
            Object act = m.get("actions");
            if (act instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> mm) {
                        Map<String, Object> a = new LinkedHashMap<>();
                        mm.forEach((k, v) -> a.put(String.valueOf(k), v));
                        vo.getActions().add(a);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[GroupReport] content 解析失败: {}", e.getMessage());
        }
    }

    /** 报告内容内部结构（段落 + 动作） */
    static class ReportContent {
        private String summary;
        private final List<Section> sections;
        private final List<Map<String, Object>> actions = new ArrayList<>();

        ReportContent(String summary, List<Section> sections) {
            this.summary = summary;
            this.sections = sections;
        }

        String getSummary() {
            return summary;
        }

        Map<String, Object> toSections() {
            Map<String, Object> m = new LinkedHashMap<>();
            List<Map<String, Object>> list = new ArrayList<>();
            for (Section s : sections) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", s.title);
                item.put("text", s.text);
                list.add(item);
            }
            m.put("summary", summary);
            m.put("sections", list);
            m.put("actions", actions);
            return m;
        }

        static class Section {
            private final String title;
            private final String text;

            Section(String title, String text) {
                this.title = title;
                this.text = text;
            }

            String getTitle() {
                return title;
            }

            String getText() {
                return text;
            }
        }
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "app-****";
        }
        return key.substring(0, 7) + "****" + key.substring(key.length() - 3);
    }

    private static String nz(String s) {
        return s == null ? "-" : s;
    }

    private static String nzf(Integer i) {
        return i == null ? "-" : String.valueOf(i);
    }

    private static int nzfInt(Integer i) {
        return i == null ? 0 : i;
    }

    private static String num(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        return v.setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String pct(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        return rate.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
