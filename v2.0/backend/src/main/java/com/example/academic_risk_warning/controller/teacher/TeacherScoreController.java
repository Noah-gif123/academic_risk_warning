package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ScoreSchemeSaveRequest;
import com.example.academic_risk_warning.entity.ScoreTemplate;
import com.example.academic_risk_warning.mapper.ScoreTemplateMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ScoreSchemeService;
import com.example.academic_risk_warning.vo.ScoreSchemeVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教师端 · 成绩构成（课程组共同确定 → 全体确认 → 锁定） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherScoreController {

    private final ScoreSchemeService schemeService;
    private final ScoreTemplateMapper templateMapper;

    /** 成绩构成模板（按课程类型筛选，教师套用） */
    @GetMapping("/score-templates")
    public Result<List<ScoreTemplate>> templates(@RequestParam(required = false) String courseType) {
        return Result.ok(templateMapper.selectList(Wrappers.<ScoreTemplate>lambdaQuery()
                .eq(courseType != null && !courseType.isEmpty(), ScoreTemplate::getCourseType, courseType)
                .eq(ScoreTemplate::getStatus, "ACTIVE")
                .orderByDesc(ScoreTemplate::getIsDefault)));
    }

    @GetMapping("/score-scheme")
    public Result<ScoreSchemeVO> get(@RequestParam Long offeringId) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.getByOffering(offeringId, user.getRefId()));
    }

    @PostMapping("/score-scheme")
    public Result<ScoreSchemeVO> save(@Valid @RequestBody ScoreSchemeSaveRequest request) {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return Result.ok(schemeService.save(request, user.getRefId()));
    }

    @PostMapping("/score-scheme/{id}/confirm")
    public Result<ScoreSchemeVO> confirm(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.confirm(id, user.getRefId()));
    }

    @PostMapping("/score-scheme/{id}/lock")
    public Result<ScoreSchemeVO> lock(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(schemeService.lock(id, user.getRefId(), "TEACHER"));
    }
}
