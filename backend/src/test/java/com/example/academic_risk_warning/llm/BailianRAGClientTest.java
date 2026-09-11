package com.example.academic_risk_warning.llm;

import com.example.academic_risk_warning.config.AgentProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * W3 引用溯源解析测试：
 * 应用返回 doc_references → 结构化引用；只返回思考过程 → 从检索命中提取来源；
 * 拒答话术要能识别；两者都没有时 grounded=false（前端会提示"无依据"）。
 */
@DisplayName("百炼 RAG 引用解析测试")
class BailianRAGClientTest {

    private BailianRAGClient client(boolean thoughtsEnabled) {
        AgentProperties properties = new AgentProperties();
        properties.setRagThoughtsEnabled(thoughtsEnabled);
        return new BailianRAGClient(properties);
    }

    @Test
    @DisplayName("返回 doc_references → 解析出引用标题/片段/分数，grounded=true")
    void shouldParseDocReferences() {
        String json = """
                {"output":{"text":"栈是后进先出，队列是先进先出。",
                  "doc_references":[
                    {"index":1,"title":"数据结构-第3章 栈与队列","text":"栈（Stack）是限定仅在表尾进行插入和删除操作的线性表……","score":0.87},
                    {"index":2,"doc_name":"数据结构-习题册","text":"队列的应用：层次遍历……","score":0.61}]},
                 "usage":{"models":[]}}
                """;

        BailianRAGClient.RagAnswer answer = client(true).parseRagAnswer(json);

        assertEquals("栈是后进先出，队列是先进先出。", answer.text());
        assertTrue(answer.grounded());
        assertFalse(answer.refusal());
        assertEquals(2, answer.citations().size());
        assertEquals("数据结构-第3章 栈与队列", answer.citations().get(0).get("title"));
        assertEquals(1, answer.citations().get(0).get("index"));
        assertEquals("DOC_REFERENCE", answer.citations().get(0).get("source"));
        assertTrue(String.valueOf(answer.citations().get(0).get("snippet")).startsWith("栈（Stack）"));
        assertEquals(0.87, answer.citations().get(0).get("score"));
        assertEquals("数据结构-习题册", answer.citations().get(1).get("title"));
    }

    @Test
    @DisplayName("应用未返回 doc_references 时，从思考过程里的 MCP 检索工具调用提取引用")
    void shouldExtractCitationsFromThoughts() {
        // 与百炼应用真实返回一致：action_type=mcp，arguments/observation 都是 JSON 字符串
        String json = """
                {"output":{"text":"操作系统有五大功能……",
                  "thoughts":[
                    {"action":"reasoning","action_type":"reasoning","response":"需要先检索"},
                    {"action":"searchFile","action_name":"searchFile","action_type":"mcp",
                     "arguments":"{\\"keyWord\\": \\"操作系统主要功能\\", \\"maxCount\\": 5}",
                     "observation":"{\\"content\\":[{\\"type\\":\\"text\\",\\"text\\":\\"第2章 操作系统基础：进程管理、内存管理、文件管理、设备管理与用户接口\\"}]}"}]}}
                """;

        BailianRAGClient.RagAnswer answer = client(true).parseRagAnswer(json);

        assertTrue(answer.grounded());
        assertFalse(answer.retrievalEmpty());
        assertEquals(1, answer.citations().size());
        Map<String, Object> cite = answer.citations().get(0);
        assertEquals("APP_RETRIEVAL", cite.get("source"));
        assertTrue(String.valueOf(cite.get("title")).contains("操作系统主要功能"), cite.toString());
        assertTrue(String.valueOf(cite.get("snippet")).contains("进程管理"), cite.toString());
    }

    @Test
    @DisplayName("检索结果为空 → 不算引用，标记 retrievalEmpty（拒答不编造）")
    void shouldMarkEmptyRetrieval() {
        String json = """
                {"output":{"text":"该问题超出当前课程范围，建议查阅教材。",
                  "thoughts":[
                    {"action":"searchFile","action_name":"searchFile","action_type":"mcp",
                     "arguments":"{\\"keyWord\\": \\"操作系统主要功能\\", \\"maxCount\\": 5}",
                     "observation":"{\\"content\\":[{\\"type\\":\\"text\\",\\"text\\":\\"查询结果为空\\"}]}"}]}}
                """;

        BailianRAGClient.RagAnswer answer = client(true).parseRagAnswer(json);

        assertFalse(answer.grounded());
        assertTrue(answer.retrievalEmpty());
        assertTrue(answer.citations().isEmpty());
        assertTrue(answer.refusal());
    }

    @Test
    @DisplayName("关闭思考过程开关 → 不再请求 thoughts，也不提取检索命中")
    void shouldNotUseThoughtsWhenDisabled() {
        String json = """
                {"output":{"text":"操作系统有五大功能……",
                  "thoughts":[{"action":"searchFile","action_type":"mcp",
                    "arguments":"{\\"keyWord\\":\\"操作系统\\"}",
                    "observation":"{\\"content\\":[{\\"text\\":\\"命中内容\\"}]}"}]}}
                """;

        BailianRAGClient.RagAnswer answer = client(false).parseRagAnswer(json);

        assertFalse(answer.grounded());
        assertFalse(answer.retrievalEmpty());
        assertTrue(answer.citations().isEmpty());
    }

    @Test
    @DisplayName("拒答话术要识别为 refusal（空检索不编造）")
    void shouldDetectRefusal() {
        BailianRAGClient client = client(true);

        BailianRAGClient.RagAnswer refused = client.parseRagAnswer(
                "{\"output\":{\"text\":\"该问题超出当前《大学计算机基础（一）》课程范围，建议咨询对应课程教师。\"}}");
        assertTrue(refused.refusal());
        assertFalse(refused.grounded());

        BailianRAGClient.RagAnswer noHit = client.parseRagAnswer(
                "{\"output\":{\"text\":\"根据知识库检索结果，未找到与\\\"栈\\\"相关的知识点。\"}}");
        assertTrue(noHit.refusal());

        BailianRAGClient.RagAnswer normal = client.parseRagAnswer(
                "{\"output\":{\"text\":\"栈是后进先出的线性表，常用于函数调用栈。\"}}");
        assertFalse(normal.refusal());
        assertFalse(normal.grounded(), "没有引用时应标记为无依据");
    }

    @Test
    @DisplayName("返回体异常（无 output）→ 退化为文本解析，不抛异常")
    void shouldFallBackOnUnexpectedPayload() {
        BailianRAGClient.RagAnswer answer = client(true).parseRagAnswer("{\"message\":\"oops\"}");

        assertFalse(answer.grounded());
        assertTrue(answer.text() != null && !answer.text().isBlank());
    }

    @Test
    @DisplayName("解析回答末尾的「依据：」声明，并把该行从正文剥离")
    void shouldSplitDeclaredEvidence() {
        BailianRAGClient.DeclaredEvidence evidence = BailianRAGClient.splitDeclaredEvidence(
                "操作系统有五大功能：进程管理、内存管理、文件管理、设备管理、用户接口。\n依据：操作系统基础知识点、第2章 操作系统\n");

        assertFalse(evidence.text().contains("依据："), evidence.text());
        assertTrue(evidence.text().contains("五大功能"), evidence.text());
        assertEquals(2, evidence.sources().size());
        assertEquals("操作系统基础知识点", evidence.sources().get(0));
        assertEquals("第2章 操作系统", evidence.sources().get(1));

        BailianRAGClient.DeclaredEvidence none =
                BailianRAGClient.splitDeclaredEvidence("这是一段没有依据声明的回答。");
        assertTrue(none.sources().isEmpty());
        assertEquals("这是一段没有依据声明的回答。", none.text());
    }

    @Test
    @DisplayName("decorateRagAnswer：助手声明的依据也会作为引用下发，但标注来源不同")
    void shouldDecorateDeclaredEvidence() {
        Map<String, Object> decorated = com.example.academic_risk_warning.service.RiskCenterService
                .decorateRagAnswer(new BailianRAGClient.RagAnswer(
                        "栈是后进先出的线性表。\n依据：数据结构-栈与队列", List.of(), false, false, false));

        assertEquals(Boolean.TRUE, decorated.get("grounded"));
        assertEquals(Boolean.FALSE, decorated.get("citationsFromPlatform"));
        assertEquals(1, decorated.get("citationCount"));
        assertEquals("MODEL_DECLARED", ((Map<?, ?>) ((List<?>) decorated.get("citations")).get(0)).get("source"));
        assertFalse(String.valueOf(decorated.get("reply")).contains("依据："));
        assertTrue(String.valueOf(decorated.get("retrievalNote")).contains("建议对照教材核对"));

        // 拒答时不应再列"助手声明的依据"，避免自相矛盾
        Map<String, Object> refused = com.example.academic_risk_warning.service.RiskCenterService
                .decorateRagAnswer(new BailianRAGClient.RagAnswer(
                        "该问题超出本课程范围。\n依据：某不相关章节", List.of(), false, true, false));
        assertEquals(0, refused.get("citationCount"));
        assertEquals(Boolean.FALSE, refused.get("grounded"));
        assertTrue(String.valueOf(refused.get("retrievalNote")).contains("拒答"), String.valueOf(refused.get("retrievalNote")));
    }

    @Test
    @DisplayName("无依据提示语区分「检索未命中」「拒答」「未返回引用」")
    void shouldBuildRetrievalNote() {
        String emptyNote = com.example.academic_risk_warning.service.RiskCenterService.retrievalNote(
                new BailianRAGClient.RagAnswer("无依据", List.of(), false, true, true));
        assertTrue(emptyNote.contains("未命中"), emptyNote);

        String refusalNote = com.example.academic_risk_warning.service.RiskCenterService.retrievalNote(
                new BailianRAGClient.RagAnswer("无依据", List.of(), false, true, false));
        assertTrue(refusalNote.contains("拒答"), refusalNote);

        String ungroundedNote = com.example.academic_risk_warning.service.RiskCenterService.retrievalNote(
                new BailianRAGClient.RagAnswer("无引用", List.of(), false, false, false));
        assertTrue(ungroundedNote.contains("未返回知识库引用依据"), ungroundedNote);

        String groundedNote = com.example.academic_risk_warning.service.RiskCenterService.retrievalNote(
                new BailianRAGClient.RagAnswer("有引用", List.of(Map.of("title", "x")), true, false, false));
        assertTrue(groundedNote.contains("1 条依据"), groundedNote);
    }
}
