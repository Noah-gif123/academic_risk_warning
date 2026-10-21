package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.InviteCodeDTO;
import com.example.academic_risk_warning.dto.JoinReviewDTO;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.StudentPortalService;
import com.example.academic_risk_warning.vo.JoinRequestVO;
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
