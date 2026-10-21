package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ProjectSaveRequest;
import com.example.academic_risk_warning.dto.ProjectScoreRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ProjectService;
import com.example.academic_risk_warning.vo.ProjectSubmissionVO;
import com.example.academic_risk_warning.vo.ProjectVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师端 · 项目与贡献度。
 * 注意：这里**没有**修改贡献表的接口——贡献表由组长提交后即锁定，教师也只能查看。
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherProjectController {

    private final ProjectService projectService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @GetMapping("/projects")
    public Result<List<ProjectVO>> list(@RequestParam Long teachingClassId) {
        return Result.ok(projectService.listProjects(teachingClassId, me()));
    }

    @GetMapping("/projects/{id}")
    public Result<ProjectVO> get(@PathVariable Long id) {
        return Result.ok(projectService.getProject(id, me()));
    }

    @PostMapping("/projects")
    public Result<ProjectVO> save(@Valid @RequestBody ProjectSaveRequest request) {
        return Result.ok(projectService.save(request, me()));
    }

    @PostMapping("/projects/{id}/publish")
    public Result<ProjectVO> publish(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean publish) {
        return Result.ok(projectService.setPublish(id, publish, me()));
    }

    @PostMapping("/projects/{id}/close")
    public Result<ProjectVO> close(@PathVariable Long id) {
        return Result.ok(projectService.close(id, me()));
    }

    /** 给项目总分（团队项目为整组分，个人得分按贡献占比折算） */
    @PostMapping("/project-submissions/{id}/grade")
    public Result<ProjectSubmissionVO> grade(@PathVariable Long id, @RequestBody ProjectScoreRequest request) {
        return Result.ok(projectService.grade(id, request, me()));
    }
}
