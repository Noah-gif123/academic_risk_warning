package com.example.academic_risk_warning.service.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 服务端课程答疑会话。 */
@Service
@RequiredArgsConstructor
public class QaSessionService {

    private static final tools.jackson.databind.ObjectMapper MAPPER = new tools.jackson.databind.ObjectMapper();
    private final JdbcTemplate jdbc;

    public Map<String, Object> create(Long studentId, Long courseId, Long teachingClassId, String title) {
        jdbc.update("insert into ai_qa_session(student_id,course_id,teaching_class_id,title) values(?,?,?,?)",
                studentId, courseId, teachingClassId, title == null || title.isBlank() ? "课程答疑" : title);
        Long id = jdbc.queryForObject("select last_insert_id()", Long.class);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("courseId", courseId);
        out.put("teachingClassId", teachingClassId);
        out.put("title", title);
        return out;
    }

    public Map<String, Object> list(Long studentId, Long courseId, int page, int size) {
        int p = Math.max(1, page);
        int n = Math.max(1, Math.min(size, 50));
        int offset = (p - 1) * n;
        List<Map<String, Object>> items = jdbc.queryForList("""
                select id,course_id courseId,teaching_class_id teachingClassId,title,update_time updateTime
                from ai_qa_session where student_id=? and (? is null or course_id=?)
                order by update_time desc limit ? offset ?
                """, studentId, courseId, courseId, n, offset);
        Integer total = jdbc.queryForObject("""
                select count(*) from ai_qa_session where student_id=? and (? is null or course_id=?)
                """, Integer.class, studentId, courseId, courseId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", total == null ? 0 : total);
        out.put("page", p);
        out.put("size", n);
        return out;
    }

    public int rename(Long studentId, Long sessionId, String title) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("标题不能为空");
        return jdbc.update("update ai_qa_session set title=? where id=? and student_id=?",
                title.substring(0, Math.min(120, title.length())), sessionId, studentId);
    }

    public List<Map<String, Object>> messages(Long studentId, Long sessionId) {
        Integer n = jdbc.queryForObject(
                "select count(*) from ai_qa_session where id=? and student_id=?", Integer.class, sessionId, studentId);
        if (n == null || n == 0) throw new IllegalArgumentException("会话不存在");
        return jdbc.queryForList("""
                select id,role,content,citations_json citationsJson,run_id runId,create_time createTime
                from ai_qa_message where session_id=? order by id
                """, sessionId);
    }

    public Long saveExchange(Long studentId, Long sessionId, Long courseId, Long teachingClassId,
                             String question, String answer, Object citations, Long runId) {
        if (courseId == null && teachingClassId != null) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "select o.course_id from teaching_class tc join course_offering o on o.id=tc.offering_id where tc.id=?",
                    teachingClassId);
            if (!rows.isEmpty() && rows.get(0).get("course_id") != null) {
                courseId = ((Number) rows.get(0).get("course_id")).longValue();
            }
        }
        if (courseId == null) {
            throw new IllegalArgumentException("缺少课程上下文，无法创建答疑会话");
        }
        if (sessionId == null) {
            Map<String, Object> s = create(studentId, courseId, teachingClassId, question);
            sessionId = ((Number) s.get("id")).longValue();
        }
        jdbc.update("insert into ai_qa_message(session_id,role,content) values(?,'user',?)", sessionId, question);
        String cj = "[]";
        try { cj = MAPPER.writeValueAsString(citations == null ? List.of() : citations); } catch (Exception ignored) { }
        jdbc.update("insert into ai_qa_message(session_id,role,content,citations_json,run_id) values(?,'assistant',?,?,?)",
                sessionId, answer, cj, runId);
        jdbc.update("update ai_qa_session set update_time=now() where id=?", sessionId);
        return sessionId;
    }

    public int delete(Long studentId, Long sessionId) {
        jdbc.update("delete from ai_qa_message where session_id=?", sessionId);
        return jdbc.update("delete from ai_qa_session where id=? and student_id=?", sessionId, studentId);
    }
}


