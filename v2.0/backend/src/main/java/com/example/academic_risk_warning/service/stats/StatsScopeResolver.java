package com.example.academic_risk_warning.service.stats;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.vo.stats.StatsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统计范围解析（P1-4）：
 * 管理员看全校；辅导员只看 counselor_class 中本人名下行政班（当前学期，缺失时回退到最近配置）。
 * 教师端按 PLAN 不开放（v3.1）。
 */
@Component
@RequiredArgsConstructor
public class StatsScopeResolver {

    private final JdbcTemplate jdbc;

    /** 解析后的统计范围；classIds == null 表示全校（管理员）。 */
    public record Scope(Long termId, String termName, LocalDate termStart, LocalDate termEnd,
                        List<Long> classIds, String scopeText) {
        public boolean allScope() {
            return classIds == null;
        }
    }

    public Scope resolve(LoginUser user, StatsQuery query) {
        Map<String, Object> term = term(query.getTermId());
        Long termId = ((Number) term.get("id")).longValue();
        String termName = String.valueOf(term.get("term_name"));
        LocalDate start = toLocalDate(term.get("start_date"));
        LocalDate end = toLocalDate(term.get("end_date"));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return new Scope(termId, termName, start, end, null, "全校");
        }
        if (!"COUNSELOR".equalsIgnoreCase(user.getRole())) {
            throw new BizException(403, "该统计接口仅对辅导员与教务开放");
        }
        if (user.getRefId() == null) {
            throw new BizException(403, "当前账号没有辅导员档案");
        }
        String counselorName = jdbc.queryForObject(
                "select counselor_name from counselor where id = ?", String.class, user.getRefId());
        List<Long> classIds = jdbc.queryForList(
                "select admin_class_id from counselor_class where counselor_id = ? and term_id = ?",
                Long.class, user.getRefId(), termId);
        if (classIds.isEmpty()) {
            classIds = jdbc.queryForList(
                    "select admin_class_id from counselor_class where counselor_id = ? order by term_id desc",
                    Long.class, user.getRefId());
            classIds = new ArrayList<>(new java.util.LinkedHashSet<>(classIds));
        }
        return new Scope(termId, termName, start, end, classIds,
                counselorName == null ? "本人行政班" : counselorName + " 名下行政班");
    }

    private Map<String, Object> term(Long termId) {
        List<Map<String, Object>> rows = termId == null
                ? jdbc.queryForList("select id, term_name, start_date, end_date from term where is_current = 1 order by id desc limit 1")
                : jdbc.queryForList("select id, term_name, start_date, end_date from term where id = ?", termId);
        if (rows.isEmpty()) {
            throw new BizException(400, termId == null ? "没有当前学期" : "学期不存在：" + termId);
        }
        return rows.get(0);
    }

    /** MySQL DATE 可能映射为 java.sql.Date 或 LocalDate，统一转 LocalDate。 */
    private static LocalDate toLocalDate(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof LocalDate) {
            return (LocalDate) v;
        }
        if (v instanceof java.sql.Date) {
            return ((java.sql.Date) v).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(v).substring(0, 10));
    }
}
