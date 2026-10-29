package com.example.academic_risk_warning.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 课件/资料文本抽取（M11 补强）：把教师上传的课件转成知识库可入库的纯文本。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>PPTX：一页一个文本块，块首加 <code>【第N页】</code> 页码标记 —— 这样分块天然按页对齐，
 *       引用既能给「第N段」（分块序号）又能给出「第N页」（课件页码），学生可以直接翻到那一页。</li>
 *   <li>DOCX：段落与表格按原文顺序拍平（表格一行一行用 | 分隔），标题加 <code>## </code> 前缀保留层次。</li>
 *   <li>TXT/MD/CSV：UTF-8 优先，解码失败按 GBK 再试（老资料常见 GBK）。</li>
 * </ul>
 *
 * <p>不支持的格式（PDF/旧版 DOC 等）由调用方跳过并在入库结果里说明，不静默丢弃。</p>
 */
@Slf4j
public final class KbExtractor {

    /** 入库支持的扩展名（小写，不含点） */
    public static final Set<String> SUPPORTED = Set.of("txt", "md", "csv", "docx", "pptx");

    /** 页标记前缀，分块与引用都会用到 */
    public static final String PAGE_MARK_PREFIX = "【第";
    public static final String PAGE_MARK_SUFFIX = "页】";

    /** PowerPoint 母版/版式提示语与日期、页码占位符（不影响正文信息，入库前剔除） */
    private static final java.util.regex.Pattern TEMPLATE_NOISE = java.util.regex.Pattern.compile(
            "^(Click to edit Master (?:title|text) styles"
                    + "|(?:Second|Third|Fourth|Fifth|Sixth|Seventh) level"
                    + "|‹#›|\\d{1,2}\\.\\d{1,2}\\.\\d{4})$");

    private KbExtractor() {
    }

    public static boolean supports(String ext) {
        return ext != null && SUPPORTED.contains(ext.toLowerCase());
    }

    /** 抽文本。不支持的格式返回空串。 */
    public static String extract(Path file, String ext) throws IOException {
        String e = ext == null ? "" : ext.toLowerCase();
        return switch (e) {
            case "txt", "md", "csv" -> readText(file);
            case "docx" -> fromDocx(file);
            case "pptx" -> fromPptx(file);
            default -> "";
        };
    }

    // ============================================================ 纯文本
    static String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String utf8 = strictDecode(bytes, StandardCharsets.UTF_8);
        if (utf8 != null) {
            return utf8;
        }
        String gbk = strictDecode(bytes, Charset.forName("GBK"));
        return gbk == null ? new String(bytes, StandardCharsets.UTF_8) : gbk;
    }

    private static String strictDecode(byte[] bytes, Charset charset) {
        CharsetDecoder dec = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return dec.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ex) {
            return null;
        }
    }

    // ============================================================ DOCX
    static String fromDocx(Path file) throws IOException {
        StringBuilder out = new StringBuilder();
        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            for (IBodyElement el : doc.getBodyElements()) {
                if (el instanceof XWPFParagraph p) {
                    String t = p.getText();
                    if (t == null || t.isBlank()) {
                        continue;
                    }
                    String style = p.getStyle() == null ? "" : p.getStyle();
                    boolean heading = style.toLowerCase().contains("heading") || style.contains("标题")
                            || (p.getNumID() == null && isShortTitle(t) && p.getRuns().size() > 0 && isBold(p));
                    out.append(heading ? "## " : "").append(t.trim()).append('\n');
                } else if (el instanceof XWPFTable table) {
                    appendDocxTable(table, out);
                }
            }
        }
        return out.toString();
    }

    private static void appendDocxTable(XWPFTable table, StringBuilder out) {
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                String t = cell.getText() == null ? "" : cell.getText().replaceAll("\\s*\\n\\s*", " ").trim();
                cells.add(t);
            }
            String line = String.join(" | ", cells).trim();
            if (!line.isBlank() && !line.replace("|", "").isBlank()) {
                out.append(line).append('\n');
            }
        }
    }

    private static boolean isShortTitle(String t) {
        String s = t.trim();
        return s.length() <= 24 && !s.endsWith("。") && !s.endsWith("，") && !s.endsWith("；");
    }

    private static boolean isBold(XWPFParagraph p) {
        try {
            return p.getRuns().stream().anyMatch(r -> r.isBold())
                    && p.getRuns().stream().allMatch(r -> r.isBold() || (r.text() != null && r.text().isBlank()));
        } catch (Exception ex) {
            return false;
        }
    }

    // ============================================================ PPTX
    static String fromPptx(Path file) throws IOException {
        StringBuilder out = new StringBuilder();
        try (InputStream in = Files.newInputStream(file); XMLSlideShow show = new XMLSlideShow(in)) {
            List<XSLFSlide> slides = show.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);
                StringBuilder body = new StringBuilder();
                String title = safe(slide.getTitle());
                for (XSLFShape shape : slide.getShapes()) {
                    collectShape(shape, body);
                }
                String notes = slideNotes(show.getNotesSlide(slide));
                String text = body.toString().trim();
                if (text.isBlank() && notes.isBlank() && title.isBlank()) {
                    continue; // 空页（纯图片页）不入库，避免噪声分块
                }
                out.append(PAGE_MARK_PREFIX).append(i + 1).append(PAGE_MARK_SUFFIX);
                if (!title.isBlank()) {
                    out.append(' ').append(title);
                }
                out.append('\n');
                if (!text.isBlank()) {
                    out.append(text).append('\n');
                }
                if (!notes.isBlank()) {
                    out.append("（讲稿备注）").append(notes).append('\n');
                }
                out.append('\n');
            }
        }
        return out.toString();
    }

    private static void collectShape(XSLFShape shape, StringBuilder buf) {
        if (shape instanceof XSLFGroupShape group) {
            for (XSLFShape child : group.getShapes()) {
                collectShape(child, buf);
            }
            return;
        }
        if (shape instanceof XSLFTable table) {
            for (XSLFTableRow row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                for (XSLFTableCell cell : row.getCells()) {
                    cells.add(safe(cell.getText()).replaceAll("\\s*\\n\\s*", " ").trim());
                }
                String line = String.join(" | ", cells).trim();
                if (!line.replace("|", "").isBlank()) {
                    buf.append(line).append('\n');
                }
            }
            return;
        }
        if (shape instanceof XSLFTextShape textShape) {
            // 标题单独取（页首给一次），正文里不再重复；页码占位符、形如 "3" 的装饰文字也不入库
            try {
                if (textShape.isPlaceholder()) {
                    Placeholder type = textShape.getTextType();
                    if (type == Placeholder.SLIDE_NUMBER || type == Placeholder.TITLE) {
                        return;
                    }
                }
            } catch (Exception ignore) {
                // 老版本 pptx 可能取不到占位符类型，继续按文本处理
            }
            String t = safe(textShape.getText()).trim();
            if (t.isBlank() || t.matches("^\\d{1,3}$")) {
                return;
            }
            appendText(buf, t);
        }
    }

    /** 按行追加，顺手过滤 PowerPoint 母版提示语（POI/手工模板常见，属于版式噪声） */
    private static void appendText(StringBuilder buf, String text) {
        for (String line : text.replaceAll("\\n{2,}", "\n").split("\\n")) {
            String s = line.trim();
            if (s.isEmpty() || isTemplateNoise(s)) {
                continue;
            }
            buf.append(s).append('\n');
        }
    }

    private static boolean isTemplateNoise(String line) {
        return TEMPLATE_NOISE.matcher(line).matches();
    }

    private static String slideNotes(XSLFNotes notesSlide) {
        try {
            if (notesSlide == null) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (XSLFShape shape : notesSlide.getShapes()) {
                if (shape instanceof XSLFTextShape t) {
                    String s = safe(t.getText()).trim();
                    if (s.isBlank() || s.matches("^\\d{1,3}$")) {
                        continue;
                    }
                    appendText(sb, s);
                }
            }
            return sb.toString().replace('\n', ' ').trim();
        } catch (Exception e) {
            log.debug("[KB] 读取讲稿备注失败：{}", e.getMessage());
            return "";
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** 从分块内容里解析页标记（没有则返回 null） */
    public static Integer pageOf(String chunkContent) {
        if (chunkContent == null || !chunkContent.startsWith(PAGE_MARK_PREFIX)) {
            return null;
        }
        int end = chunkContent.indexOf(PAGE_MARK_SUFFIX);
        if (end < 0) {
            return null;
        }
        try {
            return Integer.parseInt(chunkContent.substring(PAGE_MARK_PREFIX.length(), end));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
