/* gen_assess_import.js — 1-7 题目导入解析（.docx / .txt → 草稿题目，供校对后入库） */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('service/PaperImportService.java', `
package ${P}.service;

import ${P}.common.BizException;
import ${P}.dto.QuestionDTO;
import ${P}.util.MiniJson;
import ${P}.vo.PaperImportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目导入：.docx / .txt 解析成草稿题目（不直接入库），教师在预览页校对后再保存。
 * 约定格式：
 *   一、单选题（每题2分）
 *   1. 题干……
 *   A. 选项
 *   B. 选项
 *   答案：B
 *   解析：……
 *   分值：2
 * 题型可由大题标题或题干里的「（单选）多选 / 判断 / 填空 / 简答」指定，也可由选项与答案形态推断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperImportService {

    private static final String CN_NUM = "一二三四五六七八九十";

    public PaperImportVO parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String lower = name.toLowerCase();
        List<String> lines;
        if (lower.endsWith(".docx")) {
            lines = readDocx(file);
        } else if (lower.endsWith(".txt")) {
            lines = readText(file);
        } else if (lower.endsWith(".pdf")) {
            throw new BizException("PDF 解析需要额外的解析库（离线环境未安装），请另存为 .docx 或 .txt 后再导入");
        } else if (lower.endsWith(".doc")) {
            throw new BizException("旧版 .doc 请用 Word 另存为 .docx 后再导入");
        } else {
            throw new BizException("仅支持 .docx / .txt");
        }
        return parseLines(name, lines);
    }

    private List<String> readDocx(MultipartFile file) {
        List<String> lines = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                lines.add(p.getText());
            }
        } catch (IOException e) {
            log.error("读取 docx 失败", e);
            throw new BizException("读取 Word 失败：" + e.getMessage());
        }
        return lines;
    }

    private List<String> readText(MultipartFile file) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new BizException("读取文本失败：" + e.getMessage());
        }
        return lines;
    }

    // ------------------------------------------------------------ 解析中间结构
    private static class Opt {
        String key;
        String text;

        Opt(String key, String text) {
            this.key = key;
            this.text = text;
        }
    }

    private static class Q {
        String groupName;
        BigDecimal defaultScore;
        String typeHint = "";
        String stem = "";
        List<Opt> options = new ArrayList<>();
        String answer;
        String analysis;
        BigDecimal score;
    }

    public PaperImportVO parseLines(String fileName, List<String> lines) {
        List<QuestionDTO> out = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String groupName = null;
        BigDecimal groupScore = null;
        String groupHint = "";
        Q cur = null;
        // 若文档里有大题标题，则只解析第一个大题之后的内容：模板里的「说明 / 格式约定」不会被当成题目
        int startAt = 0;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i) == null ? "" : lines.get(i).trim();
            if (!s.isEmpty() && isGroupStart(s)) {
                startAt = i;
                break;
            }
        }
        for (int idx = startAt; idx < lines.size(); idx++) {
            String raw = lines.get(idx);
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isGroupStart(line)) {
                if (cur != null) {
                    out.add(finish(cur, out.size() + 1, warnings));
                    cur = null;
                }
                groupName = line;
                groupScore = parseGroupScore(line);
                groupHint = line;
                continue;
            }
            if (isNumberedStart(line)) {
                if (cur != null) {
                    out.add(finish(cur, out.size() + 1, warnings));
                }
                cur = new Q();
                cur.groupName = groupName;
                cur.defaultScore = groupScore;
                cur.typeHint = groupHint;
                cur.stem = stripNumber(line);
                continue;
            }
            if (cur == null) {
                continue;
            }
            if (isOptionStart(line)) {
                cur.options.add(new Opt(String.valueOf(line.charAt(0)).toUpperCase(), stripOption(line)));
                continue;
            }
            if (isAnswerStart(line)) {
                cur.answer = afterColon(line);
                continue;
            }
            if (isAnalysisStart(line)) {
                cur.analysis = afterColon(line);
                continue;
            }
            if (isScoreStart(line)) {
                cur.score = parseNumber(afterColon(line));
                continue;
            }
            if (cur.answer == null && cur.options.isEmpty()) {
                cur.stem = cur.stem + line;
            } else if (cur.answer == null && !cur.options.isEmpty()) {
                Opt last = cur.options.get(cur.options.size() - 1);
                last.text = last.text + line;
            }
        }
        if (cur != null) {
            out.add(finish(cur, out.size() + 1, warnings));
        }
        PaperImportVO vo = new PaperImportVO();
        vo.setFileName(fileName);
        vo.setParsedCount(out.size());
        vo.setWarningCount(warnings.size());
        vo.setWarnings(warnings);
        vo.setQuestions(out);
        return vo;
    }

    private QuestionDTO finish(Q q, int no, List<String> warnings) {
        QuestionDTO d = new QuestionDTO();
        d.setQNo(no);
        d.setSortNo(no);
        d.setGroupName(q.groupName);
        d.setStem(q.stem.trim());
        d.setAnalysis(q.analysis == null ? null : q.analysis.trim());
        String hint = (q.typeHint == null ? "" : q.typeHint) + " " + d.getStem();
        String answer = q.answer == null ? "" : q.answer.trim();
        List<String> keys = letters(answer);
        boolean judgeHint = hint.contains("判断");
        boolean multiHint = hint.contains("多选") || hint.contains("多项选择");
        boolean singleHint = hint.contains("单选") || hint.contains("单项选择");
        boolean blankHint = hint.contains("填空");
        boolean shortHint = hint.contains("简答") || hint.contains("问答") || hint.contains("论述");

        String type;
        if (multiHint) {
            type = "MULTI";
        } else if (judgeHint) {
            type = "JUDGE";
        } else if (blankHint) {
            type = "BLANK";
        } else if (shortHint) {
            type = "SHORT";
        } else if (singleHint) {
            type = keys.size() > 1 ? "MULTI" : "SINGLE";
            if (keys.size() > 1) {
                warnings.add("第 " + no + " 题属于单选题大题但答案有多个字母，已按多选处理");
            }
        } else if (q.options.size() >= 2) {
            type = keys.size() > 1 ? "MULTI" : "SINGLE";
        } else if (judgeValue(answer) != null) {
            type = "JUDGE";
        } else {
            type = "SHORT";
        }
        d.setQType(type);
        d.setFullScore(q.score != null ? q.score
                : (q.defaultScore != null ? q.defaultScore : new BigDecimal("2")));

        if (!q.options.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < q.options.size(); i++) {
                Opt o = q.options.get(i);
                if (i > 0) {
                    sb.append(',');
                }
                sb.append("{\\"key\\":").append(MiniJson.quote(o.key))
                        .append(",\\"text\\":").append(MiniJson.quote(o.text)).append('}');
            }
            d.setOptionsJson(sb.append(']').toString());
        }

        if ("SINGLE".equals(type) || "MULTI".equals(type)) {
            if (keys.isEmpty()) {
                keys.add("A");
                warnings.add("第 " + no + " 题没有解析到答案，已默认 A，请在校对页修正");
            }
            d.setAnswerJson("{\\"keys\\":" + MiniJson.array(keys) + "}");
        } else if ("JUDGE".equals(type)) {
            Boolean v = judgeValue(answer);
            if (v == null) {
                v = Boolean.TRUE;
                warnings.add("第 " + no + " 题判断答案无法识别，已默认「正确」，请在校对页修正");
            }
            d.setAnswerJson("{\\"value\\":" + v + "}");
        } else if ("BLANK".equals(type)) {
            List<String> blanks = new ArrayList<>();
            for (String part : answer.split("[；;|、]")) {
                if (!part.trim().isEmpty()) {
                    blanks.add(part.trim());
                }
            }
            if (blanks.isEmpty()) {
                warnings.add("第 " + no + " 题填空题没有解析到答案，请补充");
            }
            d.setAnswerJson("{\\"answers\\":" + MiniJson.array(blanks)
                    + ",\\"ignoreCase\\":true,\\"trim\\":true}");
        } else {
            d.setAnswerJson("{\\"ref\\":" + MiniJson.quote(answer) + "}");
        }
        return d;
    }

    // ------------------------------------------------------------ 行识别（不用正则，兼容中文标点）
    private static boolean isGroupStart(String s) {
        if (s.length() < 2 || CN_NUM.indexOf(s.charAt(0)) < 0) {
            return false;
        }
        char c = s.charAt(1);
        return c == '、' || c == '.' || c == '．' || c == ' ';
    }

    private static boolean isNumberedStart(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i == 0 || i > 3) {
            return false;
        }
        if (i >= s.length()) {
            return false;
        }
        char c = s.charAt(i);
        if (c != '.' && c != '．' && c != '、' && c != ')' && c != '）') {
            return false;
        }
        return s.substring(i + 1).trim().length() > 0;
    }

    private static boolean isOptionStart(String s) {
        if (s.length() < 2) {
            return false;
        }
        char k = s.charAt(0);
        if (!((k >= 'A' && k <= 'H') || (k >= 'a' && k <= 'h'))) {
            return false;
        }
        char c = s.charAt(1);
        return c == '.' || c == '．' || c == '、' || c == ')' || c == '）';
    }

    private static boolean isAnswerStart(String s) {
        return (s.startsWith("答案") || s.startsWith("参考答案") || s.startsWith("标准答案")) && hasColon(s);
    }

    private static boolean isAnalysisStart(String s) {
        return (s.startsWith("解析") || s.startsWith("答案解析") || s.startsWith("说明")) && hasColon(s);
    }

    private static boolean isScoreStart(String s) {
        return (s.startsWith("分值") || s.startsWith("得分")) && hasColon(s);
    }

    private static boolean hasColon(String s) {
        return s.indexOf(':') >= 0 || s.indexOf('：') >= 0;
    }

    private static String afterColon(String s) {
        int i = s.indexOf(':');
        int j = s.indexOf('：');
        int p = i < 0 ? j : (j < 0 ? i : Math.min(i, j));
        return p < 0 ? "" : s.substring(p + 1).trim();
    }

    private static String stripNumber(String s) {
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
            i++;
        }
        if (i < s.length()) {
            i++;
        }
        return s.substring(i).trim();
    }

    private static String stripOption(String s) {
        return s.substring(2).trim();
    }

    private static BigDecimal parseGroupScore(String s) {
        int i = s.indexOf("每题");
        if (i < 0) {
            return null;
        }
        return parseNumber(s.substring(i + 2));
    }

    private static BigDecimal parseNumber(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        try {
            return new BigDecimal(sb.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> letters(String answer) {
        List<String> out = new ArrayList<>();
        if (answer == null) {
            return out;
        }
        for (int i = 0; i < answer.length(); i++) {
            char u = Character.toUpperCase(answer.charAt(i));
            if (u >= 'A' && u <= 'H') {
                String k = String.valueOf(u);
                if (!out.contains(k)) {
                    out.add(k);
                }
            }
        }
        return out;
    }

    private static Boolean judgeValue(String answer) {
        if (answer == null) {
            return null;
        }
        String s = answer.trim();
        if (s.isEmpty() || s.length() > 6) {
            return null;
        }
        if (s.equals("对") || s.equals("正确") || s.equals("是") || s.equals("√")
                || s.equalsIgnoreCase("T") || s.equalsIgnoreCase("TRUE")) {
            return Boolean.TRUE;
        }
        if (s.equals("错") || s.equals("错误") || s.equals("否") || s.equals("×") || s.equals("x")
                || s.equalsIgnoreCase("F") || s.equalsIgnoreCase("FALSE")) {
            return Boolean.FALSE;
        }
        return null;
    }
}
`);

console.log('题目导入解析：生成完成');
