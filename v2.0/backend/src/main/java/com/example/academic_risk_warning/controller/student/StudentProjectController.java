package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ContributionSubmitRequest;
import com.example.academic_risk_warning.dto.ProjectSubmitRequest;
import com.example.academic_risk_warning.dto.TeamSaveRequest;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.Student;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ProjectService;
import com.example.academic_risk_warning.service.ResourceFileService;
import com.example.academic_risk_warning.vo.ProjectSubmissionVO;
import com.example.academic_risk_warning.vo.ProjectTeamVO;
import com.example.academic_risk_warning.vo.StudentProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 学生端 · 项目与贡献度（组队 / 提交文件 / 组长提交贡献表） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentProjectController {

    private final ProjectService projectService;
    private final ResourceFileService resourceFileService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    @GetMapping("/projects")
    public Result<List<StudentProjectVO>> list() {
        return Result.ok(projectService.listForStudent(me()));
    }

    @GetMapping("/projects/{id}")
    public Result<StudentProjectVO> get(@PathVariable Long id) {
        return Result.ok(projectService.getForStudent(id, me()));
    }

    /** 同班同学（组队用） */
    @GetMapping("/projects/teammates")
    public Result<List<Student>> teammates(@RequestParam Long teachingClassId) {
        return Result.ok(projectService.listClassmates(teachingClassId, me()));
    }

    /** 组队：本人为组长 */
    @PostMapping("/projects/{id}/team")
    public Result<ProjectTeamVO> createTeam(@PathVariable Long id, @RequestBody TeamSaveRequest request) {
        return Result.ok(projectService.createTeam(id, request, me()));
    }

    /** 提交项目文件（先调下面的 upload 拿 fileId，再提交） */
    @PostMapping("/projects/{id}/submit")
    public Result<ProjectSubmissionVO> submit(@PathVariable Long id, @RequestBody ProjectSubmitRequest request) {
        return Result.ok(projectService.submit(id, request, me()));
    }

    /** 组长提交贡献表（提交即锁定） */
    @PostMapping("/projects/{id}/contribution")
    public Result<ProjectTeamVO> contribution(@PathVariable Long id, @RequestBody ContributionSubmitRequest request) {
        return Result.ok(projectService.submitContribution(id, request, me()));
    }

    /** 上传项目文件（Word / PDF / 代码 / PPT / 压缩包） */
    @PostMapping("/projects/upload")
    public Result<ResourceFile> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam Long teachingClassId) {
        Long studentId = me();
        TeachingClass tc = projectService.requireEnrolledClass(teachingClassId, studentId);
        return Result.ok(resourceFileService.storeRaw(file, tc, studentId, "STUDENT"));
    }
}
