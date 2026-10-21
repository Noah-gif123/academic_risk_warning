package com.example.academic_risk_warning.controller.student;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AttendanceService;
import com.example.academic_risk_warning.vo.StudentAttendanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 学生端 · 我的考勤（只读；临界时提示「再缺一次即挂」，不给剩余次数数值） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/attendance")
    public Result<List<StudentAttendanceVO>> my() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return Result.ok(attendanceService.myAttendance(user.getRefId()));
    }
}
