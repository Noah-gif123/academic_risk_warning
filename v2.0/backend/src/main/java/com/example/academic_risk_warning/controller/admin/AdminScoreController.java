package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.entity.ScoreTemplate;
import com.example.academic_risk_warning.mapper.ScoreTemplateMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ScoreSchemeService;
import com.example.academic_risk_warning.vo.ScoreSchemeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 成绩构成（模板 / 全量查看 / 裁定锁定） */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminScoreController {

    private final ScoreSchemeService schemeService;
    private final ScoreTemplateMapper templateMapper;

    @GetMapping("/score-templates")
    public Result<List<ScoreTemplate>> templates(@RequestParam(required = false) String courseType) {
        return Result.ok(templateMapper.selectList(Wrappers.<ScoreTemplate>lambdaQuery()
                .eq(courseType != null, ScoreTemplate::getCourseType, courseType)
                .eq(ScoreTemplate::getStatus, "ACTIVE")
                .orderByDesc(ScoreTemplate::getIsDefault)));
    }

    @GetMapping("/score-schemes")
    public Result<List<ScoreSchemeVO>> list(@RequestParam(required = false) Long termId) {
        return Result.ok(schemeService.listByTerm(termId));
    }

    @PostMapping("/score-schemes/{id}/lock")
    public Result<ScoreSchemeVO> lock(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.lock(id, user.getAccountId(), "ADMIN"));
    }
}
