package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.GuideSaveRequest;
import com.example.academic_risk_warning.dto.LessonResourceSaveRequest;
import com.example.academic_risk_warning.dto.LessonUpdateRequest;
import com.example.academic_risk_warning.dto.ObjectiveSaveRequest;
import com.example.academic_risk_warning.dto.ResourceVoidRequest;
import com.example.academic_risk_warning.entity.CourseGuide;
import com.example.academic_risk_warning.entity.Paper;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.TeachingObjective;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.CourseContentService;
import com.example.academic_risk_warning.service.ResourceFileService;
import com.example.academic_risk_warning.vo.LessonResourceVO;
import com.example.academic_risk_warning.vo.LessonVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 教师端 · 课程内容（导学 / 教学目标 / 目录 / 资料库） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherContentController {

    private final CourseContentService contentService;
    private final ResourceFileService resourceFileService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    // ---------------- 导学
    @GetMapping("/guide")
    public Result<CourseGuide> getGuide(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.getGuide(teachingClassId, me()));
    }

    @PostMapping("/guide")
    public Result<CourseGuide> saveGuide(@Valid @RequestBody GuideSaveRequest request) {
        return Result.ok(contentService.saveGuide(request, me()));
    }

    // ---------------- 教学目标
    @GetMapping("/objectives")
    public Result<List<TeachingObjective>> objectives(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listObjectives(teachingClassId, me()));
    }

    @PostMapping("/objectives")
    public Result<List<TeachingObjective>> saveObjectives(@Valid @RequestBody ObjectiveSaveRequest request) {
        return Result.ok(contentService.saveObjectives(request, me()));
    }

    // ---------------- 目录
    @GetMapping("/lessons")
    public Result<List<LessonVO>> lessons(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listLessons(teachingClassId, me()));
    }

    @PostMapping("/lessons/sync")
    public Result<List<LessonVO>> syncLessons(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.syncLessons(teachingClassId, me()));
    }

    @PostMapping("/lessons/{id}")
    public Result<LessonVO> updateLesson(@PathVariable Long id, @RequestBody LessonUpdateRequest request) {
        return Result.ok(contentService.updateLesson(id, request, me()));
    }

    // ---------------- 课时资源
    @PostMapping("/lesson-resources")
    public Result<LessonResourceVO> addResource(@Valid @RequestBody LessonResourceSaveRequest request) {
        return Result.ok(contentService.addResource(request, me()));
    }

    @PostMapping("/lesson-resources/{id}/publish")
    public Result<LessonResourceVO> publish(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean publish) {
        return Result.ok(contentService.publishResource(id, publish, me()));
    }

    @DeleteMapping("/lesson-resources/{id}")
    public Result<Void> deleteResource(@PathVariable Long id) {
        contentService.deleteResource(id, me());
        return Result.ok();
    }

    // ---------------- 资料库
    @GetMapping("/resources")
    public Result<List<ResourceFile>> resources(@RequestParam Long teachingClassId) {
        return Result.ok(contentService.listResources(teachingClassId, me()));
    }

    @PostMapping("/resources/upload")
    public Result<ResourceFile> upload(@RequestParam("file") MultipartFile file,
                                       @RequestParam Long teachingClassId) {
        return Result.ok(resourceFileService.store(file, teachingClassId, me()));
    }

    @PostMapping("/resources/{id}/void")
    public Result<ResourceFile> voidFile(@PathVariable Long id, @RequestBody(required = false) ResourceVoidRequest request) {
        return Result.ok(contentService.voidResource(id, request == null ? null : request.getReason(), me()));
    }
}
