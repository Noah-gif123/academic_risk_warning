package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.JoinRequestDTO;
import com.example.academic_risk_warning.entity.Term;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.StudentPortalService;
import com.example.academic_risk_warning.vo.CourseCardPageVO;
import com.example.academic_risk_warning.vo.JoinRequestVO;
import com.example.academic_risk_warning.vo.StudentCourseDetailVO;
import com.example.academic_risk_warning.vo.StudentProfileVO;
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
