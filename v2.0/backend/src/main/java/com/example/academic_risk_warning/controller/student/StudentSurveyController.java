package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.QuestionnaireAnswerRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.StudentSurveyService;
import com.example.academic_risk_warning.vo.QuestionnaireVO;
import com.example.academic_risk_warning.vo.StudentSurveyVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 学生端 · 问卷调查（只有「教师风格判定」一种，匿名） */
@RestController
@RequestMapping("/api/student/questionnaires")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentSurveyController {

    private final StudentSurveyService surveyService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    @GetMapping
    public Result<List<QuestionnaireVO>> list() {
        return Result.ok(surveyService.myQuestionnaires(me()));
    }

    @GetMapping("/{id}")
    public Result<StudentSurveyVO> detail(@PathVariable Long id) {
        return Result.ok(surveyService.detail(id, me()));
    }

    @PostMapping("/{id}/answer")
    public Result<Void> answer(@PathVariable Long id, @RequestBody QuestionnaireAnswerRequest request) {
        surveyService.submit(id, request, me());
        return Result.ok();
    }
}
