package com.example.academic_risk_warning.service.ai;

import com.example.academic_risk_warning.config.AiProperties;
import com.example.academic_risk_warning.service.CourseContentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 五道闸门单测（M14）：不调用模型，直接喂"模型可能产出的坏题"，验证每道闸门都能拦住。
 *
 * <p>为什么值得写这组测试：出题功能的可信度全在这五道闸门上。真实调用模型时它大多很"听话"，
 * 反而不容易看到拦截效果，所以用单测把每种坏情况钉死：结构不全、答案矛盾、依据编造、照抄原文、
 * 与历史重复，以及一道正常题必须全 PASS。</p>
 */
class QuestionGenServiceGateTest {

    private static final String PPTX = "第2讲_Excel函数与图表.pptx";

    private QuestionGenService service() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        // 历史草稿默认查不到（重复度检查只在传了历史时生效）
        when(jdbc.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
        return new QuestionGenService(jdbc, mock(CourseContentService.class), mock(DashScopeClient.class),
                mock(AiRunService.class), new AiProperties());
    }

    private List<Map<String, Object>> material() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(chunk(PPTX, 4, 4,
                "【第4页】 二、VLOOKUP：按学号查姓名、查成绩\n"
                        + "语法：=VLOOKUP(查找值, 查找区域, 返回列号, 匹配方式)\n"
                        + "返回列号从查找区域的第 1 列开始数，不是从 A 列开始数\n"
                        + "匹配方式写 0（或 FALSE）表示精确匹配，成绩查询一律用 0"));
        list.add(chunk(PPTX, 7, 7,
                "【第7页】 五、图表怎么选\n柱状图：比较不同类别的大小，例如各分数段人数\n"
                        + "折线图：看随时间变化的趋势，例如每周作业提交率"));
        return list;
    }

    private Map<String, Object> chunk(String doc, int sectionNo, int pageNo, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("docName", doc);
        m.put("sectionNo", sectionNo);
        m.put("pageNo", pageNo);
        m.put("content", content);
        return m;
    }

    private QuestionGenService.Parsed draft(String type, String stem, String answer, String citation,
                                            String... options) {
        QuestionGenService.Parsed p = new QuestionGenService.Parsed();
        p.qType = type;
        p.stem = stem;
        p.answerRaw = answer;
        p.citation = citation;
        p.analysis = "解析";
        for (int i = 0; i < options.length; i++) {
            Map<String, String> o = new LinkedHashMap<>();
            o.put("key", String.valueOf((char) ('A' + i)));
            o.put("text", options[i]);
            p.options.add(o);
        }
        return p;
    }

    private Map<String, Object> check(QuestionGenService.Parsed p, String code) {
        for (Map<String, Object> c : p.checks) {
            if (code.equals(c.get("code"))) {
                return c;
            }
        }
        fail("没有这道闸门的结果：" + code);
        return null;
    }

    @Test
    @DisplayName("正常题：五道闸门全 PASS，并解析出页码")
    void goodQuestionPassesAllGates() {
        QuestionGenService svc = service();
        QuestionGenService.Parsed p = draft("SINGLE",
                "使用 VLOOKUP 时，返回列号应当从查找区域的第几列开始数？", "B",
                PPTX + " 第4段",
                "从工作表的 A 列开始", "从查找区域的第一列开始", "从查找值所在列开始", "从公式所在列开始");
        svc.runGates(p, material(), 101L);

        assertTrue(p.passed, "正常题应当全 PASS，实际失败：" + p.failedCodes());
        assertEquals(PPTX, p.docName);
        assertEquals(4, p.sectionNo);
        assertEquals(4, p.pageNo, "页码应从分块的【第N页】标记里解析出来");
        assertEquals(2, p.fullScore.intValue());
    }

    @Test
    @DisplayName("STRUCT：选择题选项少于 3 个 → 拦下")
    void structGateBlocksTooFewOptions() {
        QuestionGenService svc = service();
        QuestionGenService.Parsed p = draft("SINGLE", "VLOOKUP 的返回列号从哪一列开始数？", "A",
                PPTX + " 第4段", "从查找区域第一列", "从 A 列");
        svc.runGates(p, material(), 101L);

        assertFalse(p.passed);
        assertFalse((Boolean) check(p, "STRUCT").get("pass"));
        assertTrue(String.valueOf(check(p, "STRUCT").get("detail")).contains("选项少于 3"));
    }

    @Test
    @DisplayName("ANSWER：单选答案不在选项里 → 拦下")
    void answerGateBlocksUnknownKey() {
        QuestionGenService svc = service();
        QuestionGenService.Parsed p = draft("SINGLE", "VLOOKUP 的返回列号从哪一列开始数？", "E",
                PPTX + " 第4段", "从查找区域第一列", "从 A 列", "从公式列", "从查找值列");
        svc.runGates(p, material(), 101L);

        assertFalse(p.passed);
        assertFalse((Boolean) check(p, "ANSWER").get("pass"));
        assertTrue(String.valueOf(check(p, "ANSWER").get("detail")).contains("不在选项里"));
    }

    @Test
    @DisplayName("SOURCE：依据是编造的（文档/段号对不上素材）→ 拦下")
    void sourceGateBlocksFakeCitation() {
        QuestionGenService svc = service();
        QuestionGenService.Parsed p = draft("SINGLE", "VLOOKUP 的返回列号从哪一列开始数？", "A",
                "大学计算机基础第9章.pdf 第12段",
                "从查找区域第一列", "从 A 列", "从公式列", "从查找值列");
        svc.runGates(p, material(), 101L);

        assertFalse(p.passed);
        assertFalse((Boolean) check(p, "SOURCE").get("pass"));
        assertNull(p.pageNo, "对不上素材时不应给出页码");
    }

    @Test
    @DisplayName("DUP：题干直接照抄原文 → 拦下")
    void dupGateBlocksCopiedStem() {
        QuestionGenService svc = service();
        // 与素材第 4 段几乎逐字相同
        QuestionGenService.Parsed p = draft("JUDGE",
                "返回列号从查找区域的第 1 列开始数，不是从 A 列开始数，匹配方式写 0 表示精确匹配", "正确",
                PPTX + " 第4段");
        svc.runGates(p, material(), 101L);

        assertFalse(p.passed);
        assertFalse((Boolean) check(p, "DUP").get("pass"));
        assertTrue(String.valueOf(check(p, "DUP").get("detail")).contains("重合"));
    }

    @Test
    @DisplayName("SCORE：难度写错 → 回退为「中」并记为需要人工确认")
    void scoreGateNormalizesDifficulty() {
        QuestionGenService svc = service();
        QuestionGenService.Parsed p = draft("JUDGE", "柱状图适合比较不同类别的大小。", "正确", PPTX + " 第7段");
        p.difficulty = "超难";
        svc.runGates(p, material(), 101L);

        assertEquals("medium", p.difficulty);
        assertFalse((Boolean) check(p, "SCORE").get("pass"), "难度被回退时应留痕，提醒教师确认");
    }

    @Test
    @DisplayName("解析：模型输出的行格式能被稳定解析（含多选、判断、填空、简答）")
    void parserHandlesAllTypes() {
        QuestionGenService svc = service();
        String raw = """
                【题目1】
                题型：单选
                难度：易
                题干：VLOOKUP 的返回列号从哪里开始数？
                选项：A. 从 A 列开始
                选项：B. 从查找区域第一列开始
                选项：C. 从查找值所在列开始
                答案：B
                解析：按查找区域第一列起算
                依据：第2讲_Excel函数与图表.pptx 第4段

                【题目2】
                题型：多选
                难度：中
                题干：下列哪些图表适合看趋势或比大小？
                选项：A. 柱状图
                选项：B. 折线图
                选项：C. 饼图
                答案：A,B
                解析：柱状图比大小、折线图看趋势
                依据：第2讲_Excel函数与图表.pptx 第7段

                【题目3】
                题型：判断
                难度：难
                题干：SUMIF 的条件可以不加英文双引号。
                答案：错误
                依据：第2讲_Excel函数与图表.pptx 第4段

                【题目4】
                题型：填空
                难度：中
                题干：VLOOKUP 的匹配方式写____表示精确匹配。
                答案：0|FALSE
                依据：第2讲_Excel函数与图表.pptx 第4段

                【题目5】
                题型：简答
                难度：中
                题干：说明柱状图与折线图各自适用的场景。
                答案：柱状图比较不同类别大小，折线图看随时间变化的趋势。
                依据：第2讲_Excel函数与图表.pptx 第7段
                """;
        List<QuestionGenService.Parsed> parsed = svc.parse(raw);
        assertEquals(5, parsed.size(), "五道题都应解析出来");
        assertEquals("SINGLE", parsed.get(0).qType);
        assertEquals("B", parsed.get(0).answerRaw);
        assertEquals(3, parsed.get(0).options.size());
        assertEquals("MULTI", parsed.get(1).qType);
        assertEquals(3, parsed.get(1).options.size());
        assertEquals("JUDGE", parsed.get(2).qType);
        assertEquals("BLANK", parsed.get(3).qType);
        assertEquals("SHORT", parsed.get(4).qType);
        assertEquals("0|FALSE", parsed.get(3).answerRaw);
    }
}
