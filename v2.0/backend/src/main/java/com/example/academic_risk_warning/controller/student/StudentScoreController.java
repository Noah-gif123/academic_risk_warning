package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ScoreMatrixService;
import com.example.academic_risk_warning.vo.StudentScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 学生端 · 我的成绩（分项 + 总评；不含 Δ / 教师尺度等教务口径） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentScoreController {

    private final ScoreMatrixService scoreMatrixService;

    @GetMapping("/scores")
    public Result<List<StudentScoreVO>> my() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return Result.ok(scoreMatrixService.studentScores(user.getRefId()));
    }
}
