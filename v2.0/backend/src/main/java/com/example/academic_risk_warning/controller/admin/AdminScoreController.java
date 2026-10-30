package com.example.academic_risk_warning.controller.admin;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.entity.ScoreTemplate;
import com.example.academic_risk_warning.mapper.ScoreTemplateMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ScoreMatrixService;
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
    private final ScoreMatrixService scoreMatrixService;
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

    /**
     * M16：按"已录项"重算成绩单。
     *
     * <p>期末未考时旧接口（教师端「汇总生成总评」）会拒绝执行，导致 `score_result` 停在含期末预置分的旧值；
     * 这里用已录项算当前已得加权分，未录齐的记 `PARTIAL`（不按分数判挂），让成绩单与预警判定同一套数。</p>
     */
    @PostMapping("/scores/recompute")
    public Result<java.util.Map<String, Object>> recompute(@RequestParam(required = false) Long termId) {
        int n = scoreMatrixService.recomputeByRecorded(termId);
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("written", n);
        map.put("termId", termId);
        return Result.ok(map);
    }
}
