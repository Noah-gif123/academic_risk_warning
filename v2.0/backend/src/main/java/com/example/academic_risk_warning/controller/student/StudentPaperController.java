package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.PaperSubmitRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.StudentPaperService;
import com.example.academic_risk_warning.vo.StudentPaperVO;
import com.example.academic_risk_warning.vo.SubmissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 学生端 · 测验与作业（我的考试 / 在线作答 / 结果） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentPaperController {

    private final StudentPaperService studentPaperService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    @GetMapping("/papers")
    public Result<List<StudentPaperVO>> list() {
        return Result.ok(studentPaperService.list(me()));
    }

    @GetMapping("/papers/{id}")
    public Result<StudentPaperVO> get(@PathVariable Long id) {
        return Result.ok(studentPaperService.get(id, me()));
    }

    @PostMapping("/papers/{id}/submit")
    public Result<SubmissionVO> submit(@PathVariable Long id, @RequestBody PaperSubmitRequest request) {
        return Result.ok(studentPaperService.submit(id, request, me()));
    }

    @GetMapping("/papers/{id}/result")
    public Result<SubmissionVO> result(@PathVariable Long id) {
        return Result.ok(studentPaperService.result(id, me()));
    }
}
