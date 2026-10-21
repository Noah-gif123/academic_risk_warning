/* gen_project_controller.js — 1-8 项目与贡献度：教师端 / 学生端控制器 */
'use strict';
const fs = require('fs');
const path = require('path');

const BE = 'E:/JavaDEVIDEA/workspace/JavaProject/academic_risk_warning/v2.0/backend';
const J = path.join(BE, 'src/main/java/com/example/academic_risk_warning');
const P = 'com.example.academic_risk_warning';

function w(rel, content) {
  const p = path.join(J, rel);
  fs.mkdirSync(path.dirname(p), { recursive: true });
  fs.writeFileSync(p, content.replace(/^\n/, ''), 'utf8');
}

w('controller/teacher/TeacherProjectController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.ProjectSaveRequest;
import ${P}.dto.ProjectScoreRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ProjectService;
import ${P}.vo.ProjectSubmissionVO;
import ${P}.vo.ProjectVO;
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
`);

w('controller/student/StudentProjectController.java', `
package ${P}.controller.student;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.ContributionSubmitRequest;
import ${P}.dto.ProjectSubmitRequest;
import ${P}.dto.TeamSaveRequest;
import ${P}.entity.ResourceFile;
import ${P}.entity.Student;
import ${P}.entity.TeachingClass;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ProjectService;
import ${P}.service.ResourceFileService;
import ${P}.vo.ProjectSubmissionVO;
import ${P}.vo.ProjectTeamVO;
import ${P}.vo.StudentProjectVO;
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
`);

console.log('项目模块：控制器生成完成');
