package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.AlertExplainService;
import com.example.academic_risk_warning.vo.ai.AlertExplainVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端 · 预警解释卡（M9）。
 *
 * <p>一张卡回答三件事：为什么触发、差多少、三条例行动作；数字全部来自判定结果与成绩数据。</p>
 */
@RestController
@RequestMapping("/api/teacher/alerts")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherAlertExplainController {

    private final AlertExplainService alertExplainService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    /** 生成（或重新生成）一张解释卡 */
    @PostMapping("/{alertId}/explain")
    public Result<AlertExplainVO> generate(@PathVariable Long alertId) {
        return Result.ok(alertExplainService.generate(alertId, me()));
    }

    /** 取最新一张（没有则返回 null） */
    @GetMapping("/{alertId}/explain")
    public Result<AlertExplainVO> latest(@PathVariable Long alertId) {
        return Result.ok(alertExplainService.latest(alertId));
    }
}
