package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.GradeRequest;
import com.example.academic_risk_warning.dto.PaperSaveRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.PaperImportService;
import com.example.academic_risk_warning.service.PaperService;
import com.example.academic_risk_warning.vo.PaperImportVO;
import com.example.academic_risk_warning.vo.PaperVO;
import com.example.academic_risk_warning.vo.SubmissionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 教师端 · 测验与作业（导入校对 / 发布 / 批改 / 缺考） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherPaperController {

    private final PaperService paperService;
    private final PaperImportService importService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @GetMapping("/papers")
    public Result<List<PaperVO>> list(@RequestParam Long teachingClassId) {
        return Result.ok(paperService.listPapers(teachingClassId, me()));
    }

    @GetMapping("/papers/{id}")
    public Result<PaperVO> get(@PathVariable Long id) {
        return Result.ok(paperService.getPaper(id, me()));
    }

    @PostMapping("/papers")
    public Result<PaperVO> save(@Valid @RequestBody PaperSaveRequest request) {
        return Result.ok(paperService.save(request, me()));
    }

    @PostMapping("/papers/{id}/publish")
    public Result<PaperVO> publish(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean publish) {
        return Result.ok(paperService.setPublish(id, publish, me()));
    }

    @PostMapping("/papers/{id}/close")
    public Result<PaperVO> close(@PathVariable Long id) {
        return Result.ok(paperService.closePaper(id, me()));
    }

    /** 导入题目：只解析，不入库（教师校对后再保存） */
    @PostMapping("/papers/import")
    public Result<PaperImportVO> parse(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.parse(file));
    }

    @GetMapping("/papers/{id}/submissions")
    public Result<List<SubmissionVO>> submissions(@PathVariable Long id) {
        return Result.ok(paperService.listSubmissions(id, me()));
    }

    @GetMapping("/submissions/{id}")
    public Result<SubmissionVO> submission(@PathVariable Long id) {
        return Result.ok(paperService.getSubmission(id, me()));
    }

    @PostMapping("/submissions/{id}/grade")
    public Result<SubmissionVO> grade(@PathVariable Long id, @RequestBody GradeRequest request) {
        return Result.ok(paperService.grade(id, request, me()));
    }

    @PostMapping("/papers/{id}/absent")
    public Result<SubmissionVO> absent(@PathVariable Long id, @RequestParam Long studentId) {
        return Result.ok(paperService.markAbsent(id, studentId, me()));
    }
}
