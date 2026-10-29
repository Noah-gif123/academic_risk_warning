package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;

/**
 * 课程知识库（M11）：语料入库 + 检索。
 *
 * <p>首版 RAG 的"R"用 <b>MySQL ngram 全文检索</b>实现：分块表自带 FULLTEXT 索引，
 * 中文按 ngram 切分，相关度由 MySQL 打分。分块与引用（文档名 + 第几段）与向量库方案一致，
 * 以后接向量检索只需替换 {@link #retrieve} 的内部实现。</p>
 *
 * <p>语料来源：课程导学（HTML→纯文本）+ 课程资料（txt）。题目暂无解析，暂不入库。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbService {

    private final JdbcTemplate jdbc;

    /** 上传资料落盘目录（与 app.file.store-dir 一致） */
    @Value("${app.file.store-dir:./file-store}")
    private String storeDir;

    private static final int CHUNK_SIZE = 420;
    private static final int CHUNK_OVERLAP = 60;

    // ============================================================ 入库
    @Transactional
    public Map<String, Object> syncCourse(Long courseId) {
        Map<String, Object> out = new LinkedHashMap<>();
        int docs = 0;
        int chunks = 0;
        List<String> errors = new ArrayList<>();

        // 1) 导学：每个教学班一条（同一门课多个班会得到多份导学，便于按班检索）
        for (Map<String, Object> g : jdbc.queryForList("""
                select g.id, g.offering_id, g.teaching_class_id, g.content, tc.class_code
                from course_guide g
                         join course_offering o on o.id = g.offering_id
                         left join teaching_class tc on tc.id = g.teaching_class_id
                where o.course_id = ? and g.is_published = 1 and g.content is not null
                """, courseId)) {
            try {
                String text = htmlToText(String.valueOf(g.get("content")));
                String docName = "导学 · " + (g.get("class_code") == null ? "课程" : g.get("class_code"));
                int n = upsertDoc(courseId, lng(g.get("offering_id")), "GUIDE", lng(g.get("id")), docName,
                        text, lng(g.get("teaching_class_id")));
                if (n > 0) {
                    docs++;
                    chunks += n;
                }
            } catch (Exception e) {
                errors.add("导学 " + g.get("id") + " 入库失败：" + e.getMessage());
            }
        }

        // 2) 课程资料：本课程本学期的 txt 资料（PPTX 需先抽文本，见规划）
        for (Map<String, Object> r : jdbc.queryForList("""
                select r.id, r.offering_id, r.teaching_class_id, r.file_name, r.file_path, r.file_ext
                from resource_file r join course_offering o on o.id = r.offering_id
                where o.course_id = ? and r.is_void = 0
                """, courseId)) {
            try {
                String ext = r.get("file_ext") == null ? "" : String.valueOf(r.get("file_ext")).toLowerCase();
                if (!List.of("txt", "md", "csv").contains(ext)) {
                    continue;
                }
                Path p = Paths.get(storeDir, String.valueOf(r.get("file_path")));
                if (!Files.exists(p)) {
                    errors.add("资料 " + r.get("file_name") + " 文件不存在：" + p);
                    continue;
                }
                String text = Files.readString(p, StandardCharsets.UTF_8);
                int n = upsertDoc(courseId, lng(r.get("offering_id")), "RESOURCE", lng(r.get("id")),
                        String.valueOf(r.get("file_name")), text, lng(r.get("teaching_class_id")));
                if (n > 0) {
                    docs++;
                    chunks += n;
                }
            } catch (Exception e) {
                errors.add("资料 " + r.get("file_name") + " 入库失败：" + e.getMessage());
            }
        }

        out.put("courseId", courseId);
        out.put("docs", docs);
        out.put("chunks", chunks);
        out.put("errors", errors);
        out.put("message", docs == 0 ? "没有新的可入库内容（已是最新）" : "入库完成");
        return out;
    }

    /** 写入/更新一个来源；返回分块数（内容未变返回 0） */
    private int upsertDoc(Long courseId, Long offeringId, String type, Long sourceId,
                          String docName, String rawText, Long teachingClassId) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return 0;
        }
        String hash = sha256(text);
        List<Map<String, Object>> exist = jdbc.queryForList(
                "select id, content_hash from ai_kb_doc where course_id = ? and source_type = ? and source_id = ?",
                courseId, type, sourceId);
        if (!exist.isEmpty() && hash.equals(String.valueOf(exist.get(0).get("content_hash")))) {
            return 0;
        }
        List<String> parts = chunk(text);
        Long docId;
        if (exist.isEmpty()) {
            jdbc.update("""
                    insert into ai_kb_doc(course_id, offering_id, source_type, source_id, doc_name,
                                          content_hash, char_count, chunk_count, status)
                    values (?,?,?,?,?,?,?,?,'READY')
                    """, courseId, offeringId, type, sourceId, docName, hash, text.length(), parts.size());
            docId = jdbc.queryForObject(
                    "select id from ai_kb_doc where course_id = ? and source_type = ? and source_id = ?",
                    Long.class, courseId, type, sourceId);
        } else {
            docId = lng(exist.get(0).get("id"));
            jdbc.update("""
                    update ai_kb_doc set doc_name = ?, content_hash = ?, char_count = ?, chunk_count = ?,
                                         status = 'READY', error_message = null, offering_id = ?
                    where id = ?
                    """, docName, hash, text.length(), parts.size(), offeringId, docId);
        }
        jdbc.update("delete from ai_kb_chunk where course_id = ? and source_type = ? and source_id = ?",
                courseId, type, sourceId);
        int no = 1;
        for (String part : parts) {
            jdbc.update("""
                    insert into ai_kb_chunk(course_id, offering_id, teaching_class_id, source_type, source_id,
                                            doc_name, section_no, content, char_len)
                    values (?,?,?,?,?,?,?,?,?)
                    """, courseId, offeringId, teachingClassId, type, sourceId, docName, no++, part, part.length());
        }
        return parts.size();
    }

    // ============================================================ 检索
    public List<Map<String, Object>> retrieve(Long courseId, String query, int topK) {
        String q = normalize(query);
        List<Map<String, Object>> rows = new ArrayList<>();
        if (q.isBlank()) {
            return rows;
        }
        int limit = Math.max(1, Math.min(topK, 10));
        try {
            rows = jdbc.queryForList("""
                    select id, doc_name, section_no, source_type, content,
                           match(content) against (? in natural language mode) as score
                    from ai_kb_chunk
                    where course_id = ? and match(content) against (? in natural language mode)
                    order by score desc
                    limit ?
                    """, q, courseId, q, limit);
        } catch (Exception e) {
            log.warn("[KB] 全文检索失败，回退 LIKE：{}", e.getMessage());
        }
        if (rows.isEmpty()) {
            // 回退：按关键词 LIKE 粗筛（ngram 对超短查询可能召回为 0）
            List<String> terms = terms(q);
            for (String t : terms) {
                rows = jdbc.queryForList("""
                        select id, doc_name, section_no, source_type, content, 0.05 as score
                        from ai_kb_chunk where course_id = ? and content like ? limit ?
                        """, courseId, "%" + t + "%", limit);
                if (!rows.isEmpty()) {
                    break;
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("chunkId", r.get("id"));
            m.put("docName", r.get("doc_name"));
            m.put("sectionNo", r.get("section_no"));
            m.put("sourceType", r.get("source_type"));
            m.put("score", r.get("score"));
            String content = String.valueOf(r.get("content"));
            m.put("snippet", content.length() <= 220 ? content : content.substring(0, 219) + "…");
            m.put("content", content);
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> status(Long courseId) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Map<String, Object>> docs = jdbc.queryForList("""
                select id, doc_name, source_type, chunk_count, char_count, status, update_time
                from ai_kb_doc where course_id = ? order by id
                """, courseId);
        Integer chunks = jdbc.queryForObject(
                "select count(*) from ai_kb_chunk where course_id = ?", Integer.class, courseId);
        m.put("courseId", courseId);
        m.put("docCount", docs.size());
        m.put("chunkCount", chunks == null ? 0 : chunks);
        m.put("docs", docs);
        return m;
    }

    // ============================================================ 工具
    private static List<String> chunk(String text) {
        List<String> out = new ArrayList<>();
        String[] paragraphs = text.split("\\n+");
        StringBuilder buf = new StringBuilder();
        for (String p : paragraphs) {
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (buf.length() > 0 && buf.length() + s.length() > CHUNK_SIZE) {
                out.add(buf.toString());
                int keep = Math.min(CHUNK_OVERLAP, buf.length());
                String tail = buf.substring(buf.length() - keep);
                buf.setLength(0);
                buf.append(tail);
            }
            if (buf.length() > 0) {
                buf.append('\n');
            }
            buf.append(s);
        }
        if (buf.length() > 0) {
            out.add(buf.toString());
        }
        return out;
    }

    private static String htmlToText(String html) {
        if (html == null) {
            return "";
        }
        String s = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        s = s.replaceAll("(?i)</(p|div|tr|h[1-6]|li|table)>", "\n");
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</t[dh]>", " ");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
        return normalize(s);
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\r', '\n').replaceAll("[ \\t\\u00a0]+", " ").replaceAll("\\n{3,}", "\n\n");
        return t.trim();
    }

    private static List<String> terms(String q) {
        List<String> out = new ArrayList<>();
        for (String t : q.split("[\\s,，。；;、:：/\\\\()（）\\[\\]\"'“”‘’]+")) {
            if (t.length() >= 2 && !out.contains(t)) {
                out.add(t);
            }
        }
        if (out.isEmpty() && q.length() >= 2) {
            out.add(q);
        }
        return out.size() > 4 ? out.subList(0, 4) : out;
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
            return Integer.toHexString(s.hashCode());
        }
    }

    private static Long lng(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (Exception e) {
            return null;
        }
    }
}
