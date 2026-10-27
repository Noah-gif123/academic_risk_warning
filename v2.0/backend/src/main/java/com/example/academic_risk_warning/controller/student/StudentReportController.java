package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.StudentReportService;
import com.example.academic_risk_warning.vo.ai.StudentReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 · 学习评价报告（个人学情报告）。
 *
 * <p>数字全部来自规则计算（成绩构成项 / 试卷周次 / 分组 / 题型 / 考勤 / 预警），
 * 模型只负责把事实写成人话；模型不可用时自动退回模板，接口永远可用。</p>
 */
@RestController
@RequestMapping("/api/student/reports")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentReportController {

    private final StudentReportService reportService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    /** 报告列表（按时间倒序，最多 50 条） */
    @GetMapping
    public Result<List<StudentReportVO>> list() {
        return Result.ok(reportService.list(me()));
    }

    /** 报告详情（首次查看会记录已读） */
    @GetMapping("/{id}")
    public Result<StudentReportVO> detail(@PathVariable Long id) {
        return Result.ok(reportService.detail(me(), id));
    }

    /** 生成一份报告（同一门课可以反复生成，旧报告保留，便于对比与答辩演示） */
    @PostMapping("/generate")
    public Result<StudentReportVO> generate(@RequestBody Map<String, Object> body) {
        String type = body.get("reportType") == null ? "STAGE" : String.valueOf(body.get("reportType"));
        Object en = body.get("enrollmentId");
        if (en != null) {
            return Result.ok(reportService.generate(me(), Long.parseLong(String.valueOf(en)), type));
        }
        Object tc = body.get("teachingClassId");
        if (tc != null) {
            return Result.ok(reportService.generateByClass(me(), Long.parseLong(String.valueOf(tc)), type));
        }
        throw new BizException("缺少 enrollmentId 或 teachingClassId");
    }

    /** 有用 / 没用反馈 */
    @PostMapping("/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean useful = body.get("useful") == null || Boolean.parseBoolean(String.valueOf(body.get("useful")));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        reportService.feedback(me(), id, useful, reason);
        return Result.ok();
    }
}
