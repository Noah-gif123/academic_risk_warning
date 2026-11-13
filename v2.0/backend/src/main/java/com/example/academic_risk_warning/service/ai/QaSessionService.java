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

    public List<Map<String, Object>> list(Long studentId, Long courseId) {
        return jdbc.queryForList("""
                select id,course_id courseId,teaching_class_id teachingClassId,title,update_time updateTime
                from ai_qa_session where student_id=? and (? is null or course_id=?)
                order by update_time desc limit 30
                """, studentId, courseId, courseId);
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

