/* gen_student_portal_controller.js — 1-11 学生端控制器 + 教师端邀请码/审批 */
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

w('controller/student/StudentPortalController.java', `
package ${P}.controller.student;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.JoinRequestDTO;
import ${P}.entity.Term;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.StudentPortalService;
import ${P}.vo.CourseCardPageVO;
import ${P}.vo.JoinRequestVO;
import ${P}.vo.StudentCourseDetailVO;
import ${P}.vo.StudentProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 学生端 · 首页 / 课程学习 / 课程详情 / 加入课程 / 个人中心 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentPortalController {

    private final StudentPortalService portalService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return user.getRefId();
    }

    @GetMapping("/terms")
    public Result<List<Term>> terms() {
        return Result.ok(portalService.terms());
    }

    @GetMapping("/course-cards")
    public Result<CourseCardPageVO> cards(@RequestParam(required = false) Long termId,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String teacherName,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "12") int size) {
        return Result.ok(portalService.cards(me(), termId, keyword, status, teacherName, page, size));
    }

    @GetMapping("/course/{teachingClassId}")
    public Result<StudentCourseDetailVO> detail(@PathVariable Long teachingClassId) {
        return Result.ok(portalService.detail(teachingClassId, me()));
    }

    @GetMapping("/profile")
    public Result<StudentProfileVO> profile() {
        return Result.ok(portalService.profile(me()));
    }

    @PostMapping("/join-requests")
    public Result<JoinRequestVO> apply(@RequestBody JoinRequestDTO request) {
        return Result.ok(portalService.apply(request.getInviteCode(), me()));
    }

    @GetMapping("/join-requests")
    public Result<List<JoinRequestVO>> myRequests() {
        return Result.ok(portalService.myRequests(me()));
    }
}
`);

w('controller/teacher/TeacherJoinController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.InviteCodeDTO;
import ${P}.dto.JoinReviewDTO;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.StudentPortalService;
import ${P}.vo.JoinRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教师端 · 入班邀请码与申请审批（同意即入班，同时给教务一条知会） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherJoinController {

    private final StudentPortalService portalService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @PostMapping("/teaching-classes/{id}/invite")
    public Result<String> invite(@PathVariable Long id, @RequestBody(required = false) InviteCodeDTO request) {
        String code = portalService.setInvite(id,
                request == null ? null : request.getInviteCode(),
                request == null ? 0 : request.getJoinReviewEnabled(), me());
        return Result.ok(code);
    }

    @GetMapping("/join-requests")
    public Result<List<JoinRequestVO>> requests(@RequestParam Long teachingClassId,
                                                @RequestParam(required = false) String status) {
        return Result.ok(portalService.classRequests(teachingClassId, me(), status));
    }

    @PostMapping("/join-requests/{id}/review")
    public Result<JoinRequestVO> review(@PathVariable Long id,
                                        @RequestParam(defaultValue = "true") boolean approve,
                                        @RequestBody(required = false) JoinReviewDTO request) {
        return Result.ok(portalService.review(id, approve,
                request == null ? null : request.getNote(), me()));
    }
}
`);

console.log('学生端模块：控制器生成完成');
