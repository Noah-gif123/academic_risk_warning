package com.example.academic_risk_warning.agent.biz;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.service.ai.GroupReportService;
import com.example.academic_risk_warning.vo.ai.GroupReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * P2-8 Reporter：三类报告（教学班 / 课程 / 行政班）契约。
 *
 * <p>复用 v2.0 GroupReportService 的报告生成与逐字段对账（checksPassed/Total）；
 * 输出 {reportType,targetId,title,sections,checksPassed,checksTotal,stale,latencyMs}。</p>
 */
@Component
@RequiredArgsConstructor
public class ReporterAgent {

    private final GroupReportService reports;

    public Map<String, Object> generate(String type, Long targetId, LoginUser user) {
        long t0 = System.currentTimeMillis();
        GroupReportVO vo;
        switch (type) {
            case "class":
                requireTeacher(user);
                vo = reports.generateClassReport(targetId, user.getRefId());
                break;
            case "offering":
                requireTeacher(user);
                vo = reports.generateOfferingReport(targetId, user.getRefId());
                break;
            case "adminClass":
                if (!"COUNSELOR".equalsIgnoreCase(user.getRole())) {
                    throw new BizException(403, "行政班报告仅辅导员可生成");
                }
                vo = reports.generateAdminClassReport(targetId, user.getRefId());
                break;
            default:
                throw new BizException(400, "type 仅支持 class/offering/adminClass");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("agent", "reporter");
        out.put("reportType", type);
        out.put("targetId", targetId);
        out.put("id", vo.getId());
        out.put("title", vo.getTitle());
        out.put("summary", vo.getSummary());
        out.put("sections", vo.getSections());
        out.put("actions", vo.getActions());
        out.put("checksPassed", vo.getChecksPassed());
        out.put("checksTotal", vo.getChecksTotal());
        out.put("checks", vo.getChecks());
        out.put("stale", vo.getStale());
        out.put("source", vo.getSource());
        out.put("latencyMs", System.currentTimeMillis() - t0);
        return out;
    }

    private void requireTeacher(LoginUser user) {
        if (!"TEACHER".equalsIgnoreCase(user.getRole())) {
            throw new BizException(403, "教学班/课程报告仅教师可生成");
        }
        if (user.getRefId() == null) {
            throw new BizException(403, "当前账号没有教师档案");
        }
    }
}
