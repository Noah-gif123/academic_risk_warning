package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.vo.ai.ReportFact;
import tools.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报告写作器：**先规则、后语言**。
 *
 * <ul>
 *   <li>{@link #composeByTemplate(ReportFact)}：纯规则模板，模型不可用时的兜底，保证页面永远有内容；</li>
 *   <li>{@link #composeByDify(ReportFact, String)}：把模型输出按段落切开；</li>
 *   <li>{@link #numbersValid(String, ReportFact)}：数字白名单回验，出现事实包里没有的数字即判失败。</li>
 * </ul>
 */
@Slf4j
@Component
public class ReportComposer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    /** 结构性/常识性数字，允许出现但不算事实 */
    private static final Set<String> SAFE = Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "60", "75", "40", "100", "2024", "2025", "2026", "2027", "12", "16", "20", "30", "50", "70", "80", "90");

    @Data
    public static class Section {
        private String title;
        private String text;

        public Section() {
        }

        public Section(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    @Data
    public static class Action {
        private String type;
        private String label;
        private Map<String, Object> payload = new LinkedHashMap<>();

        public Action() {
        }

        public Action(String type, String label, Map<String, Object> payload) {
            this.type = type;
            this.label = label;
            this.payload = payload == null ? new LinkedHashMap<>() : payload;
        }
    }

    @Data
    public static class Content {
        private String summary;
        private List<Section> sections = new ArrayList<>();
        private List<Action> actions = new ArrayList<>();
    }

    // ================================================================ 模板兜底
    public Content composeByTemplate(ReportFact f) {
        Content c = new Content();
        String level = levelText(f.getRiskLevel());
        String summary = "《" + nz(f.getCourseName()) + "》当前总分 " + num(f.getTotalScore()) + " 分"
                + (f.getClassAvg() == null ? "" : "，班级均分 " + num(f.getClassAvg()) + " 分")
                + (f.getClassRank() == null ? "" : "，班内第 " + f.getClassRank() + " / " + nzInt(f.getClassSize()))
                + "，当前" + level + "。";
        c.setSummary(trim(summary, 290));

        List<Section> sections = new ArrayList<>();
        sections.add(new Section("结论", summary + "\n" + conclusionLine(f)));

        sections.add(new Section("你的强项", strongText(f)));
        sections.add(new Section("需要补的地方", weakText(f)));
        sections.add(new Section("学习投入", investText(f)));
        sections.add(new Section("下一步做什么", "按下面的动作清单做，两周后再看一次这份报告。"));
        c.setSections(sections);
        c.setActions(actions(f));
        return c;
    }

    private String conclusionLine(ReportFact f) {
        if (Boolean.TRUE.equals(f.getHardFail())) {
            return "当前已达到" + absentText(f) + "，按学籍管理规定会被取消考试资格，请尽快联系任课老师。";
        }
        if ("RED".equals(f.getRiskLevel())) {
            return "主要风险来自" + riskReason(f) + "；现在还来得及补救，但窗口不大。";
        }
        if ("YELLOW".equals(f.getRiskLevel())) {
            return "目前处在可补救区间，重点把下面的失分项补起来。";
        }
        return "目前状态正常，保持节奏即可。";
    }

    private String strongText(ReportFact f) {
        Optional<ReportFact.Item> best = f.getItems() == null ? Optional.empty() : f.getItems().stream()
                .filter(i -> i.getScore() != null)
                .max(Comparator.comparing(ReportFact.Item::getScore));
        StringBuilder sb = new StringBuilder();
        best.ifPresent(i -> sb.append("「").append(i.getName()).append("」是你的强项，得分 ")
                .append(num(i.getScore())).append(" / 100。"));
        f.getGroups().stream()
                .filter(g -> g.getMyRate() != null && g.getClassRate() != null && g.getMyRate().compareTo(g.getClassRate()) > 0)
                .max(Comparator.comparing(ReportFact.Dim::getGap))
                .ifPresent(g -> sb.append("「").append(g.getName()).append("」这组题你对了 ")
                        .append(pct(g.getMyRate())).append("，高于班级平均 ").append(pct(g.getClassRate())).append("。"));
        if (sb.length() == 0) {
            sb.append("暂时没有明显高于班级平均的板块，先从下面的失分项里挑一个补起来。");
        }
        return sb.toString();
    }

    private String weakText(ReportFact f) {
        StringBuilder sb = new StringBuilder();
        List<ReportFact.Dim> weak = f.getGroups().stream()
                .filter(g -> g.getMyRate() != null && g.getClassRate() != null && g.getMyRate().compareTo(g.getClassRate()) < 0)
                .sorted(Comparator.comparing(ReportFact.Dim::getGap))
                .limit(3).toList();
        if (!weak.isEmpty()) {
            sb.append("按分组看，最需要补的是：");
            int idx = 1;
            for (ReportFact.Dim g : weak) {
                sb.append(idx++).append("）「").append(g.getName()).append("」共 ")
                        .append(nzInt(g.getQuestionCount())).append(" 题，你对了 ")
                        .append(pct(g.getMyRate())).append("，班级平均 ").append(pct(g.getClassRate())).append("；");
            }
            sb.append("这三块先补，性价比最高。");
        } else {
            sb.append("所有分组的正确率都不低于班级平均，维持即可。");
        }
        List<ReportFact.Item> missing = f.getItems() == null ? List.of() : f.getItems().stream()
                .filter(i -> Boolean.TRUE.equals(i.getMissing())).toList();
        if (!missing.isEmpty()) {
            sb.append("另外有 ").append(missing.size()).append(" 个评分项老师还没登分（")
                    .append(String.join("、", missing.stream().limit(3).map(ReportFact.Item::getName).toList()))
                    .append("），这块缺口的结论要等登分后才准。");
        }
        return sb.toString();
    }

    private String investText(ReportFact f) {
        StringBuilder sb = new StringBuilder();
        sb.append("缺勤 ").append(nzInt(f.getAbsentCount())).append(" 次、迟到 ").append(nzInt(f.getLateCount()))
                .append(" 次、请假 ").append(nzInt(f.getLeaveCount())).append(" 次");
        if (f.getAllowedAbsent() != null) {
            sb.append("；本课程允许缺勤 ").append(f.getAllowedAbsent()).append(" 次");
            if (f.getRemainingAbsent() != null) {
                sb.append("，剩余额度 ").append(f.getRemainingAbsent()).append(" 次");
            }
        }
        sb.append("。");
        if (f.getDeductionPoints() != null && f.getDeductionPoints().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("考勤扣分 ").append(num(f.getDeductionPoints())).append(" 分。");
        }
        List<ReportFact.Project> ps = f.getProjects() == null ? List.of() : f.getProjects();
        if (!ps.isEmpty()) {
            sb.append("项目参与 ").append(ps.size()).append(" 次");
            ps.stream().filter(p -> p.getRatio() != null).findFirst().ifPresent(p ->
                    sb.append("，最近一次贡献比 ").append(p.getRatio()));
            sb.append("。");
        }
        return sb.toString();
    }

    // ================================================================ 模型输出处理
    /**
     * 去掉推理模型的"思考过程"。
     *
     * <p>DeepSeek 的推理模型会输出 &lt;think&gt;…&lt;/think&gt;（Dify 还会插入
     * {@code <!--dify-deepseek-reasoning-->} 标记），这些内容既不该给学生看，也会把
     * 数字回验带偏，所以在解析与校验之前统一剥掉。</p>
     */
    public static String stripReasoning(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String out = text;
        // 1) 成对的思考块
        out = out.replaceAll("(?is)<think\\b[^>]*>.*?</think\\s*>", "\n");
        out = out.replaceAll("(?is)<reasoning\\b[^>]*>.*?</reasoning\\s*>", "\n");
        out = out.replace("<!--dify-deepseek-reasoning-->", "\n");
        // 2) 只有开标签（推理模型没闭合）时：从第一个段落标题处截断，标题之前的全扔掉
        String lower = out.toLowerCase();
        int open = lower.indexOf("<think");
        if (open >= 0) {
            int head = firstHeadingIndex(out);
            if (head > open) {
                out = out.substring(head);
            } else {
                out = out.replaceAll("(?is)</?think\\b[^>]*>", " ");
            }
        }
        out = out.replaceAll("(?is)</?\\s*think\\b[^>]*>", " ");
        return out.replaceAll("[ \\t]+\\n", "\n").trim();
    }

    /** 第一个段落标题/章节号的下标，找不到返回 -1 */
    private static int firstHeadingIndex(String text) {
        int best = -1;
        for (String mark : new String[]{"【", "### ", "## "}) {
            int i = text.indexOf(mark);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    /** 像不像一份四段式报告（至少 2 个段落标题，且正文不空） */
    public boolean looksLikeReport(String raw) {
        String text = stripReasoning(raw);
        if (text == null || text.isBlank()) {
            return false;
        }
        int hits = 0;
        for (String mark : new String[]{"【结论】", "【你的强项】", "【需要补的地方】", "【下一步做什么】"}) {
            if (text.contains(mark)) {
                hits++;
            }
        }
        if (hits >= 2) {
            return true;
        }
        return text.contains("【") && text.length() > 120;
    }
    // ================================================================ 群体报告（M8）支持
    /** 群体报告段落（教学班 / 行政班） */
    public static class GroupSection {
        private final String title;
        private final String text;

        public GroupSection(String title, String text) {
            this.title = title;
            this.text = text;
        }

        public String getTitle() {
            return title;
        }

        public String getText() {
            return text;
        }
    }

    private static final Pattern GROUP_HEAD = Pattern.compile(
            "【(风险概览|共性难点|重点关注|教学建议|班级画像|集中风险课程|多门预警学生|干预建议)】");

    /** 切分群体报告的四段 */
    public java.util.List<GroupSection> parseGroupSections(String raw) {
        String text = stripReasoning(raw);
        java.util.List<GroupSection> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return out;
        }
        Matcher m = GROUP_HEAD.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (m.find()) {
            spans.add(new int[]{m.start(), m.end()});
            titles.add(m.group(1));
        }
        for (int i = 0; i < spans.size(); i++) {
            int from = spans.get(i)[1];
            int to = (i + 1 < spans.size()) ? spans.get(i + 1)[0] : text.length();
            if (to <= from) {
                continue;
            }
            String body = text.substring(from, to).trim().replaceAll("^[\\s:：,，.。]+", "");
            if (!body.isEmpty()) {
                out.add(new GroupSection(titles.get(i), body));
            }
        }
        return out;
    }

    /** 像不像一份群体报告（至少 2 个群体标题） */
    public boolean looksLikeGroupReport(String raw) {
        String text = stripReasoning(raw);
        if (text == null || text.isBlank()) {
            return false;
        }
        int hits = 0;
        Matcher m = GROUP_HEAD.matcher(text);
        while (m.find()) {
            hits++;
        }
        return hits >= 2 && text.length() > 80;
    }

    /** 通用数字回验：白名单从任意事实对象的 JSON 里抽取 */
    public List<String> invalidNumbersAgainst(String rawText, Object fact) {
        String text = stripReasoning(rawText);
        List<String> bad = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return bad;
        }
        List<BigDecimal> allowed = allowedFromObject(fact);
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String raw = m.group();
            BigDecimal v;
            try {
                v = new BigDecimal(raw);
            } catch (Exception e) {
                continue;
            }
            boolean ok = false;
            for (BigDecimal a : allowed) {
                BigDecimal tol = BigDecimal.valueOf(v.abs().doubleValue() > 1000 ? 0.0001 : 0.6);
                if (v.subtract(a).abs().compareTo(tol) <= 0) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                bad.add(raw);
            }
        }
        return bad;
    }

    /** 从事实对象 JSON 抽白名单（含百分比换算） */
    private List<BigDecimal> allowedFromObject(Object fact) {
        List<BigDecimal> out = new ArrayList<>();
        for (String s : SAFE) {
            try {
                out.add(new BigDecimal(s));
            } catch (Exception ignore) {
                // 忽略
            }
        }
        String json;
        try {
            json = MAPPER.writeValueAsString(fact);
        } catch (Exception e) {
            return out;
        }
        Matcher m = NUMBER.matcher(json);
        Set<String> seen = new LinkedHashSet<>();
        while (m.find()) {
            if (!seen.add(m.group())) {
                continue;
            }
            try {
                BigDecimal v = new BigDecimal(m.group());
                out.add(v);
                out.add(v.multiply(BigDecimal.valueOf(100)));
            } catch (Exception ignore) {
                // 忽略
            }
        }
        return out;
    }
    /** 把 Dify 返回的正文按段落切开（支持 【标题】 或 一、标题 或 ### 标题） */
    public Content composeByDify(ReportFact f, String rawText) {
        Content fallback = composeByTemplate(f);
        String text = stripReasoning(rawText);
        if (text == null || text.isBlank()) {
            return fallback;
        }
        List<Section> sections = parseSections(text);
        if (sections.size() < 2) {
            return fallback;
        }
        // 模型偶尔漏写"下一步做什么"：用规则动作补一段，保证四段结构与可执行性
        boolean hasNext = sections.stream()
                .anyMatch(s -> s.getTitle() != null && s.getTitle().contains("下一步"));
        if (!hasNext) {
            List<Action> acts = actions(f);
            StringBuilder sb = new StringBuilder();
            int idx = 1;
            for (Action a : acts) {
                sb.append(idx++).append("）").append(a.getLabel()).append("；");
            }
            sb.append("两周后再看一次这份报告。");
            sections.add(new Section("下一步做什么", sb.toString()));
        }
        Content c = new Content();
        c.setSections(sections);
        c.setActions(actions(f));
        c.setSummary(trim(sections.get(0).getText().replace("\n", " "), 290));
        return c;
    }

    /** 段落标题正则：兼容"内联"（同一行连续多个标题）与"独占一行"两种排版 */
    private static final Pattern SECTION_HEAD = Pattern.compile(
            "【(结论|你的强项|需要补的地方|下一步做什么|学习投入)】|(#{2,3})\\s*(结论|你的强项|需要补的地方|下一步做什么|学习投入)");

    private List<Section> parseSections(String text) {
        List<Section> out = new ArrayList<>();
        Matcher m = SECTION_HEAD.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        while (m.find()) {
            String title = m.group(1) != null ? m.group(1) : m.group(3);
            spans.add(new int[]{m.start(), m.end()});
            titles.add(title);
        }
        for (int i = 0; i < spans.size(); i++) {
            int from = spans.get(i)[1];
            int to = (i + 1 < spans.size()) ? spans.get(i + 1)[0] : text.length();
            if (to <= from) {
                continue;
            }
            String body = text.substring(from, to).trim();
            body = body.replaceAll("^[\\s:：,，.。]+", "").trim();
            if (body.isEmpty()) {
                continue;
            }
            out.add(new Section(titles.get(i), body));
        }
        return out;
    }
    private String headingOf(String line) {
        if (line.startsWith("【") && line.contains("】")) {
            String t = line.substring(1, line.indexOf('】'));
            return t.isBlank() ? null : t;
        }
        if (line.startsWith("###")) {
            String t = line.replace("#", "").trim();
            return t.isBlank() ? null : t;
        }
        if (line.matches("^[一二三四五六七八九十]+[、.．].{0,20}$")) {
            return line.replaceFirst("^[一二三四五六七八九十]+[、.．]", "").trim();
        }
        return null;
    }

    // ================================================================ 数字白名单回验
    /** 允许出现的数字（含容差）：事实包全部数字 + 比例换算的百分数 + 结构性常量 */
    public List<BigDecimal> allowedNumbers(ReportFact f) {
        List<BigDecimal> out = new ArrayList<>();
        for (String s : SAFE) {
            out.add(new BigDecimal(s));
        }
        for (String s : f.keyNumbers()) {
            try {
                out.add(new BigDecimal(s));
            } catch (Exception ignore) {
                // 忽略非数字
            }
        }
        List<ReportFact.Dim> dims = new ArrayList<>(f.getGroups());
        dims.addAll(f.getTypes());
        for (ReportFact.Dim d : dims) {
            addRate(out, d.getMyRate());
            addRate(out, d.getClassRate());
        }
        for (ReportFact.Trend t : f.getWeeks()) {
            addRate(out, t.getMyRate());
            addRate(out, t.getClassRate());
            if (t.getWeek() != null) {
                out.add(BigDecimal.valueOf(t.getWeek()));
            }
        }
        if (f.getItems() != null) {
            for (ReportFact.Item i : f.getItems()) {
                if (i.getScore() != null) {
                    out.add(i.getScore());
                }
                if (i.getWeight() != null) {
                    out.add(i.getWeight());
                }
            }
        }
        return out;
    }

    private void addRate(List<BigDecimal> out, BigDecimal rate) {
        if (rate == null) {
            return;
        }
        BigDecimal pct = rate.multiply(BigDecimal.valueOf(100));
        out.add(pct);
        out.add(pct.setScale(0, RoundingMode.HALF_UP));
        out.add(rate);
    }

    /**
     * 越界数字清单。
     *
     * <p>判定规则：与白名单任一数字相差 ≤ 0.6 视为"模型做了四舍五入"，允许；
     * 超过 1000 的数字（年份等）必须精确命中，避免编造学年。</p>
     */
    public List<String> invalidNumbers(String text, ReportFact f) {
        List<String> bad = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return bad;
        }
        List<BigDecimal> allowed = allowedNumbers(f);
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String raw = m.group();
            BigDecimal v;
            try {
                v = new BigDecimal(raw);
            } catch (Exception e) {
                continue;
            }
            boolean ok = false;
            for (BigDecimal a : allowed) {
                BigDecimal diff = v.subtract(a).abs();
                if (diff.compareTo(BigDecimal.valueOf(v.abs().doubleValue() > 1000 ? 0.0001 : 0.6)) <= 0) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                bad.add(raw);
            }
        }
        return bad;
    }

    /**
     * 报告口径的数字校验：**前三段必须严格命中事实**，最后一段"下一步做什么"是行动建议，
     * 允许出现"每天 20 分钟""再刷 3 道题"这类由模型给出的计划量，不做白名单校验。
     */
    public List<String> invalidNumbersForReport(String rawText, ReportFact f) {
        String text = stripReasoning(rawText);
        if (text == null) {
            return new ArrayList<>();
        }
        int cut = -1;
        for (String mark : new String[]{"【下一步做什么】", "【下一步】", "### 下一步", "四、下一步", "4. 下一步"}) {
            int idx = text.indexOf(mark);
            if (idx > 0) {
                cut = idx;
                break;
            }
        }
        String checked = cut > 0 ? text.substring(0, cut) : text;
        return invalidNumbers(checked, f);
    }

    public boolean numbersValid(String text, ReportFact f) {
        List<String> bad = invalidNumbers(text, f);
        if (!bad.isEmpty()) {
            log.info("[Report] 数字回验未通过，越界数字={}", bad.stream().distinct().limit(8).toList());
            return false;
        }
        return text != null && !text.isBlank();
    }
    // ================================================================ 喂给 Dify 的输入
    /** 把事实包压成"必讲的 6 条事实"，减小 token 与跑题概率 */
    public Map<String, Object> promptInputs(ReportFact f) {
        Map<String, Object> in = new LinkedHashMap<>();
        StringBuilder facts = new StringBuilder();
        facts.append("课程：").append(nz(f.getCourseName())).append("（").append(nz(f.getCourseType()))
                .append("，").append(nzInt(f.getCredit())).append(" 学分，")
                .append(nzInt(f.getPlannedWeeks())).append(" 周）\n");
        facts.append("总分：").append(num(f.getTotalScore()))
                .append("；班级均分：").append(num(f.getClassAvg()))
                .append("；班内排名：").append(nzInt(f.getClassRank())).append(" / ").append(nzInt(f.getClassSize())).append("\n");
        facts.append("考勤：缺勤 ").append(nzInt(f.getAbsentCount())).append("，迟到 ").append(nzInt(f.getLateCount()))
                .append("，请假 ").append(nzInt(f.getLeaveCount()))
                .append("，允许缺勤 ").append(nzInt(f.getAllowedAbsent())).append("\n");
        facts.append("分项：");
        if (f.getItems() != null) {
            for (ReportFact.Item i : f.getItems()) {
                facts.append(i.getName()).append("=").append(num(i.getScore())).append("；");
            }
        }
        facts.append("\n分组正确率（我 / 班级）：");
        for (ReportFact.Dim g : f.getGroups()) {
            facts.append(g.getName()).append("=").append(pct(g.getMyRate())).append("/").append(pct(g.getClassRate())).append("；");
        }
        facts.append("\n题型正确率（我 / 班级）：");
        for (ReportFact.Dim t : f.getTypes()) {
            facts.append(t.getName()).append("=").append(pct(t.getMyRate())).append("/").append(pct(t.getClassRate())).append("；");
        }
        facts.append("\n预警：等级=").append(levelText(f.getRiskLevel()))
                .append("，置信度=").append(confidenceText(f.getConfidenceLevel()))
                .append("，期末需考=").append(num(f.getFinalExamNeeded()));
        in.put("facts", facts.toString());
        in.put("course", nz(f.getCourseName()));
        in.put("student", nz(f.getStudentName()));
        in.put("risk_level", levelText(f.getRiskLevel()));
        in.put("total_score", num(f.getTotalScore()));
        in.put("class_avg", num(f.getClassAvg()));
        in.put("absents", nzInt(f.getAbsentCount()));
        in.put("allowed_absents", nzInt(f.getAllowedAbsent()));
        return in;
    }

    // ================================================================ 动作与工具
    private List<Action> actions(ReportFact f) {
        List<Action> list = new ArrayList<>();
        List<ReportFact.Dim> weak = f.getGroups().stream()
                .filter(g -> g.getMyRate() != null && g.getClassRate() != null && g.getMyRate().compareTo(g.getClassRate()) < 0)
                .sorted(Comparator.comparing(ReportFact.Dim::getGap)).limit(2).toList();
        for (ReportFact.Dim g : weak) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("group", g.getName());
            p.put("offeringId", f.getOfferingId());
            p.put("teachingClassId", f.getTeachingClassId());
            list.add(new Action("REDO", "重做「" + g.getName() + "」的题", p));
        }
        Map<String, Object> guide = new LinkedHashMap<>();
        guide.put("offeringId", f.getOfferingId());
        guide.put("teachingClassId", f.getTeachingClassId());
        guide.put("week", f.getWeeks().isEmpty() ? null : f.getWeeks().get(0).getWeek());
        list.add(new Action("GUIDE", "看本课程导学", guide));
        Map<String, Object> ask = new LinkedHashMap<>();
        ask.put("offeringId", f.getOfferingId());
        ask.put("teacherName", f.getTeacherName());
        list.add(new Action("ASK_TEACHER", "找任课老师答疑", ask));
        return list;
    }

    /** 置信度中文 */
    private String confidenceText(String level) {
        if (level == null) {
            return "-";
        }
        return switch (level) {
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> level;
        };
    }

    private String levelText(String level) {
        if ("RED".equals(level)) {
            return "红灯预警";
        }
        if ("YELLOW".equals(level)) {
            return "黄灯预警";
        }
        if ("GREEN".equals(level)) {
            return "绿灯（正常）";
        }
        return "暂无判定";
    }

    private String riskReason(ReportFact f) {
        if (Boolean.TRUE.equals(f.getHardFail())) {
            return absentText(f);
        }
        if (f.getFinalExamNeeded() != null && f.getPassScore() != null
                && f.getFinalExamNeeded().compareTo(BigDecimal.valueOf(f.getPassScore())) > 0) {
            return "期末需要考到 " + num(f.getFinalExamNeeded()) + " 分（及格线 " + f.getPassScore() + " 分）";
        }
        return "平时分与考勤的综合表现";
    }

    private String absentText(ReportFact f) {
        return "缺勤 " + nzInt(f.getAbsentCount()) + " 次（允许 " + nzInt(f.getAllowedAbsent()) + " 次）";
    }

    private static String num(BigDecimal v) {
        if (v == null) {
            return "-";
        }
        return v.setScale(v.scale() > 1 ? 1 : v.scale(), RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String pct(BigDecimal rate) {
        if (rate == null) {
            return "-";
        }
        return rate.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static String nz(String s) {
        return s == null ? "-" : s;
    }

    private static String nzInt(Integer i) {
        return i == null ? "-" : String.valueOf(i);
    }

    private static String trim(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** 写作规则（Dify 工作流与直连通道共用同一份 System 提示） */
    public String promptSystem() {
        return """
                你是高校学业预警系统里的「学习评价撰写助手」，服务对象是学生本人。
                必须遵守：
                1. 只能使用用户给出的数字，禁止自己计算、换算或编造任何数字、比例、排名；
                2. 严格输出四段，段落标题只能是【结论】【你的强项】【需要补的地方】【下一步做什么】，每段 1-3 句；
                3. 用第二人称，语气平实，不出现"挂科""必挂""肯定过不了"等判定词，风险结论只能引用给定的预警等级；
                4. 不使用 Markdown 星号、表格、代码块；前两段只挑最关键的 3 到 5 个数字，不要罗列；
                5. 总字数 350 到 600 字，最后一段给出可以直接执行的动作；
                """;
    }

    /** 直连通道的用户提示：事实包 + 写作要求 */
    public String promptUser(ReportFact f) {
        return "以下是这名学生的真实数据（由系统计算，不得改动）：\n"
                + promptInputs(f).get("facts") + "\n\n"
                + "请据此写一份学习评价报告：先给结论（结合预警等级），再说强项与最需要补的 2 到 3 块，"
                + "最后给出 3 条可以直接执行的动作。";
    }

    /** 供服务层序列化内容 */
    public String toJson(Content c) {
        try {
            return MAPPER.writeValueAsString(c);
        } catch (Exception e) {
            return "{}";
        }
    }
}
