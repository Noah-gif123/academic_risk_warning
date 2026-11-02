import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 课程资料 → ai_kb_chunk 入库工具（R3）。
 *
 * <p>支持 docx / pptx / pdf / txt / md；按目录名映射课程，切块后写入 {@code ai_kb_chunk} 与 {@code ai_kb_doc}。
 * 幂等：同一门课、同名文档（source_id = 0，即"由资料原件入库"）会先删后插，重复执行不会翻倍。</p>
 *
 * <p>用法：{@code java -cp ".;<libs>" KbIngest <kb-source 根目录> [--dry-run]}</p>
 */
public class KbIngest {

    private static final String DB = "jdbc:mysql://127.0.0.1:3306/study_warning_system_v2"
            + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false";

    /** 目录名 → course_id（与库里 course 表对齐） */
    private static final Map<String, Long> COURSE_OF_DIR = new LinkedHashMap<>();

    static {
        COURSE_OF_DIR.put("大学计算机基础", 1L);
        COURSE_OF_DIR.put("高等数学", 4L);
        COURSE_OF_DIR.put("数据结构", 7L);
        COURSE_OF_DIR.put("计算机网络", 8L);
        COURSE_OF_DIR.put("四史", 10L);
    }

    /** 切块参数：目标 350 字、重叠 70 字（约 20%） */
    private static final int TARGET = 300;
    private static final int OVERLAP = 60;
    private static final int MIN_CHUNK = 40;

    /**
     * 入库前的口径修正（以「课程导学」为准）。
     *
     * <p>原始资料里的课程大纲是教务处模板，学分与导学不一致（大纲 3 学分 / 导学 2.0 学分）。
     * 不改用户原件，而是在入库时统一到导学口径，并在这里留痕，任何一次重建都会得到一致的语料。</p>
     */
    private static final List<Correction> CORRECTIONS = List.of(
            // 大学计算机基础：大纲写 3 学分，导学写 2.0 学分 → 以导学为准
            new Correction("课程大纲_大学计算机基础", "学　　分：3　　总学时", "学　　分：2　　总学时"),
            // 大纲正文里的"3 学分"提法（若有）
            new Correction("课程大纲_大学计算机基础", "3 学分", "2 学分")
    );

    /** 一条入库修正：命中某一文档时，把 before 替换成 after */
    private record Correction(String docNameContains, String before, String after) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("用法: java -cp ... KbIngest <kb-source 根目录> [--dry-run]");
            return;
        }
        Path root = Path.of(args[0]);
        boolean dryRun = args.length > 1 && "--dry-run".equals(args[1]);
        if (!Files.isDirectory(root)) {
            System.out.println("目录不存在: " + root);
            return;
        }

        List<Doc> docs = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            List<Path> list = files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().toLowerCase(Locale.ROOT).equals("readme.md"))
                    // Word/PPT 打开时会留 ~$ 锁文件，不是真文档
                    .filter(p -> !p.getFileName().toString().startsWith("~$"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path p : list) {
                String rel = root.relativize(p).toString();
                Long courseId = courseOf(rel);
                if (courseId == null) {
                    System.out.println("SKIP（不认识的课程目录）: " + rel);
                    continue;
                }
                String ext = extOf(p);
                String text;
                try {
                    text = extract(p, ext);
                } catch (Exception e) {
                    System.out.println("FAIL 解析 " + rel + " : " + e);
                    continue;
                }
                if (text == null || text.isBlank()) {
                    System.out.println("WARN 无文本（可能是扫描件/纯图片）: " + rel);
                    continue;
                }
                String docName = stripExt(p.getFileName().toString());
                text = applyCorrections(docName, text);
                List<String> chunks = chunk(text);
                docs.add(new Doc(courseId, docName, rel, sha256(text), text.length(), chunks));
                System.out.printf("READ %-58s %5d 字 → %2d 块%n", rel, text.length(), chunks.size());
            }
        }

        int totalChunks = docs.stream().mapToInt(d -> d.chunks().size()).sum();
        System.out.println("\n=== 汇总：文档 " + docs.size() + " 份，切块 " + totalChunks + " 个 ===");
        for (Doc d : docs) {
            System.out.printf("  course=%d  %-46s %2d 块%n", d.courseId(), d.docName(), d.chunks().size());
        }
        if (dryRun) {
            System.out.println("\n--dry-run：不落库");
            return;
        }
        if (docs.isEmpty()) {
            System.out.println("没有可入库的文档");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB, "root", "root")) {
            conn.setAutoCommit(false);
            int insertedChunks = 0;
            int insertedDocs = 0;
            for (Doc d : docs) {
                // ai_kb_doc 有唯一键 uk_source(course_id, source_type, source_id)，
                // 资料原件没有真实资源记录，用「文档名派生的负数 id」当身份，既保证唯一又避开真实 id
                long srcId = syntheticSourceId(d.docName());
                // 幂等：先清掉同名文档此前由本工具入库的分块与文档记录
                try (PreparedStatement ps = conn.prepareStatement(
                        "delete from ai_kb_chunk where course_id=? and doc_name=? and source_type='RESOURCE' and source_id=?")) {
                    ps.setLong(1, d.courseId());
                    ps.setString(2, d.docName());
                    ps.setLong(3, srcId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "delete from ai_kb_doc where course_id=? and doc_name=? and source_id=?")) {
                    ps.setLong(1, d.courseId());
                    ps.setString(2, d.docName());
                    ps.setLong(3, srcId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement("""
                        insert into ai_kb_chunk(course_id, offering_id, teaching_class_id, source_type, source_id,
                                                doc_name, section_no, content, char_len)
                        values (?, null, null, 'RESOURCE', ?, ?, ?, ?, ?)
                        """)) {
                    int no = 0;
                    for (String c : d.chunks()) {
                        no++;
                        ps.setLong(1, d.courseId());
                        ps.setLong(2, srcId);
                        ps.setString(3, d.docName());
                        ps.setInt(4, no);
                        ps.setString(5, c);
                        ps.setInt(6, c.length());
                        ps.addBatch();
                    }
                    insertedChunks += ps.executeBatch().length;
                }
                try (PreparedStatement ps = conn.prepareStatement("""
                        insert into ai_kb_doc(course_id, offering_id, source_type, source_id, doc_name,
                                              content_hash, char_count, chunk_count, status)
                        values (?, null, 'RESOURCE', ?, ?, ?, ?, ?, 'READY')
                        """)) {
                    ps.setLong(1, d.courseId());
                    ps.setLong(2, srcId);
                    ps.setString(3, d.docName());
                    ps.setString(4, d.contentHash());
                    ps.setInt(5, d.charCount());
                    ps.setInt(6, d.chunks().size());
                    insertedDocs += ps.executeUpdate();
                }
            }
            conn.commit();
            System.out.println("\n落库完成：文档 " + insertedDocs + " 条，分块 " + insertedChunks + " 条");
        }
    }

    // ================================================================ 解析
    private static String extract(Path p, String ext) throws Exception {
        return switch (ext) {
            case "docx" -> docx(p);
            case "pptx" -> pptx(p);
            case "pdf" -> pdf(p);
            case "txt", "md" -> Files.readString(p, StandardCharsets.UTF_8);
            default -> "";
        };
    }

    private static String docx(Path p) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = Files.newInputStream(p); XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String t = clean(para.getText());
                if (!t.isEmpty()) {
                    sb.append(t).append('\n');
                }
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder line = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String t = clean(cell.getText());
                        if (!t.isEmpty()) {
                            line.append(t).append(" | ");
                        }
                    }
                    String s = line.toString().trim();
                    if (s.endsWith("|")) {
                        s = s.substring(0, s.length() - 1).trim();
                    }
                    if (!s.isEmpty()) {
                        sb.append(s).append('\n');
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String pptx(Path p) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = Files.newInputStream(p); XMLSlideShow show = new XMLSlideShow(in)) {
            int page = 0;
            for (XSLFSlide slide : show.getSlides()) {
                page++;
                StringBuilder line = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape text) {
                        String t = clean(text.getText());
                        if (!t.isEmpty()) {
                            line.append(t).append('\n');
                        }
                    }
                }
                if (line.length() > 0) {
                    sb.append("【第").append(page).append("页】\n").append(line);
                }
            }
        }
        return sb.toString();
    }

    private static String pdf(Path p) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (PDDocument doc = Loader.loadPDF(p.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String t = clean(stripper.getText(doc));
                if (!t.isEmpty()) {
                    sb.append("【第").append(page).append("页】\n").append(t).append('\n');
                }
            }
        }
        return sb.toString();
    }

    // ================================================================ 切块
    /** 按行累积到 350 字成块，块间保留 70 字重叠，避免答案被切断 */
    private static List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder cur = new StringBuilder();
        String carry = "";
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (cur.length() > 0 && cur.length() + line.length() > TARGET) {
                String block = cur.toString().trim();
                if (block.length() >= MIN_CHUNK) {
                    out.add(block);
                }
                carry = block.length() > OVERLAP ? block.substring(block.length() - OVERLAP) : block;
                cur.setLength(0);
                cur.append(carry).append('\n');
            }
            // 单行超长（如整段课文）按句号硬切
            while (line.length() > TARGET) {
                int cut = line.lastIndexOf('。', TARGET);
                if (cut < TARGET / 2) {
                    cut = TARGET;
                }
                String head = line.substring(0, Math.min(cut + 1, line.length()));
                cur.append(head).append('\n');
                String block = cur.toString().trim();
                if (block.length() >= MIN_CHUNK) {
                    out.add(block);
                }
                carry = block.length() > OVERLAP ? block.substring(block.length() - OVERLAP) : block;
                cur.setLength(0);
                cur.append(carry).append('\n');
                line = line.substring(Math.min(cut + 1, line.length()));
            }
            cur.append(line).append('\n');
        }
        String last = cur.toString().trim();
        if (last.length() >= MIN_CHUNK) {
            out.add(last);
        }
        return out;
    }

    // ================================================================ 小工具

    /** 应用 {@link #CORRECTIONS} 里登记的口径修正，命中就打印，便于核对 */
    private static String applyCorrections(String docName, String text) {
        String out = text;
        for (Correction c : CORRECTIONS) {
            if (docName.contains(c.docNameContains()) && out.contains(c.before())) {
                out = out.replace(c.before(), c.after());
                System.out.println("  ↳ 口径修正：" + c.before() + " → " + c.after());
            }
        }
        return out;
    }
    private static Long courseOf(String relPath) {
        String dir = relPath.contains("\\") ? relPath.substring(0, relPath.indexOf('\\'))
                : relPath.contains("/") ? relPath.substring(0, relPath.indexOf('/')) : "";
        for (Map.Entry<String, Long> e : COURSE_OF_DIR.entrySet()) {
            if (dir.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    private static String extOf(Path p) {
        String n = p.getFileName().toString();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 资料原件的合成 source_id：由文档名派生一个稳定的负数。
     *
     * <p>真实资源（resource_file / course_guide）用的是正数自增 id，这里取负值既不冲突，
     * 又能在 {@code ai_kb_doc.uk_source(course_id, source_type, source_id)} 上保证唯一。</p>
     */
    private static long syntheticSourceId(String docName) {
        return -1L - (Math.abs(docName.hashCode()) % 1_000_000_000L);
    }

    private static String stripExt(String name) {
        int i = name.lastIndexOf('.');
        return i < 0 ? name : name.substring(0, i);
    }

    private static String clean(String s) {
        if (s == null) {
            return "";
        }
        return s.replace('\u00a0', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\n").trim();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private record Doc(long courseId, String docName, String relPath, String contentHash,
                       int charCount, List<String> chunks) {
    }
}
