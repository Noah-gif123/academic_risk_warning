package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.GroupReportService;
import com.example.academic_risk_warning.vo.ai.GroupReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端 · 授课班学情报告（M8）。
 *
 * <p>与「学生个人报告」同一套哲学：数字由规则算，模型只写结论与建议；
 * 通道三层（Dify 群体工作流 → 直连 → 模板），任何一层失败都能出报告。</p>
 */
@RestController
@RequestMapping("/api/teacher/reports")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherGroupReportController {

    private final GroupReportService groupReportService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    /** 生成一份班级报告（可反复生成，历史保留） */
    @PostMapping("/class/{teachingClassId}")
    public Result<GroupReportVO> generate(@PathVariable Long teachingClassId) {
        Long teacherId = me();
        groupReportService.assertTeacherOwns(teachingClassId, teacherId);
        return Result.ok(groupReportService.generateClassReport(teachingClassId, teacherId));
    }

    /** 最近一份（没有则返回 null，前端据此决定是否提示“生成”） */
    @GetMapping("/class/{teachingClassId}/latest")
    public Result<GroupReportVO> latest(@PathVariable Long teachingClassId) {
        groupReportService.assertTeacherOwns(teachingClassId, me());
        return Result.ok(groupReportService.latest("CLASS", teachingClassId));
    }

    /** 历史列表 */
    @GetMapping("/class/{teachingClassId}/history")
    public Result<List<GroupReportVO>> history(@PathVariable Long teachingClassId,
                                               @RequestParam(defaultValue = "10") int limit) {
        groupReportService.assertTeacherOwns(teachingClassId, me());
        return Result.ok(groupReportService.list("CLASS", teachingClassId, Math.min(limit, 50)));
    }
}
