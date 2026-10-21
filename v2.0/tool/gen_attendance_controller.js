/* gen_attendance_controller.js — 1-9 考勤与归档：教师端 / 教务端 / 学生端控制器 */
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

w('controller/teacher/TeacherAttendanceController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.ArchiveSubmitRequest;
import ${P}.dto.AttendanceCallRequest;
import ${P}.dto.AttendanceRecordUpdateRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.AttendanceService;
import ${P}.vo.ArchiveVO;
import ${P}.vo.AttendanceImportVO;
import ${P}.vo.AttendanceRosterRowVO;
import ${P}.vo.AttendanceSessionVO;
import ${P}.vo.AttendanceStudentStatVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 教师端 · 考勤（点名单 / 行级修改 / 汇总 / 批量导入 / 归档提交） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherAttendanceController {

    private final AttendanceService attendanceService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @GetMapping("/attendance/sessions")
    public Result<List<AttendanceSessionVO>> sessions(@RequestParam Long teachingClassId) {
        return Result.ok(attendanceService.listSessions(teachingClassId, me()));
    }

    @GetMapping("/attendance/roster")
    public Result<List<AttendanceRosterRowVO>> roster(@RequestParam Long teachingClassId,
                                                      @RequestParam Long sessionId) {
        return Result.ok(attendanceService.roster(teachingClassId, sessionId, me()));
    }

    @PostMapping("/attendance/call")
    public Result<AttendanceSessionVO> call(@Valid @RequestBody AttendanceCallRequest request) {
        return Result.ok(attendanceService.call(request, me()));
    }

    @PostMapping("/attendance/records/{id}")
    public Result<AttendanceRosterRowVO> update(@PathVariable Long id,
                                                @RequestBody AttendanceRecordUpdateRequest request) {
        return Result.ok(attendanceService.updateRecord(id, request, me()));
    }

    @GetMapping("/attendance/stats")
    public Result<List<AttendanceStudentStatVO>> stats(@RequestParam Long teachingClassId) {
        return Result.ok(attendanceService.stats(teachingClassId, me()));
    }

    @PostMapping("/attendance/import")
    public Result<AttendanceImportVO> importFile(@RequestParam("file") MultipartFile file,
                                                 @RequestParam Long teachingClassId) {
        return Result.ok(attendanceService.importFile(file, teachingClassId, me()));
    }

    @GetMapping("/attendance/archives")
    public Result<List<ArchiveVO>> archives(@RequestParam Long teachingClassId) {
        return Result.ok(attendanceService.listTeacherArchives(teachingClassId, me()));
    }

    @PostMapping("/attendance/archives")
    public Result<ArchiveVO> submitArchive(@Valid @RequestBody ArchiveSubmitRequest request) {
        return Result.ok(attendanceService.submitArchive(request, me()));
    }
}
`);

w('controller/admin/AdminAttendanceController.java', `
package ${P}.controller.admin;

import ${P}.common.Result;
import ${P}.dto.ArchiveReviewRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.AttendanceService;
import ${P}.vo.ArchiveVO;
import ${P}.vo.CoverageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 教务端 · 考勤归档审批与覆盖率督办 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@RequireRole("ADMIN")
public class AdminAttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/attendance/archives")
    public Result<List<ArchiveVO>> archives(@RequestParam(required = false) Long termId,
                                            @RequestParam(required = false) String status) {
        return Result.ok(attendanceService.listArchivesByTerm(termId, status));
    }

    @PostMapping("/attendance/archives/{id}/review")
    public Result<ArchiveVO> review(@PathVariable Long id,
                                    @RequestParam(defaultValue = "true") boolean approve,
                                    @RequestBody(required = false) ArchiveReviewRequest request) {
        LoginUser user = UserContext.require();
        return Result.ok(attendanceService.review(id, approve, request, user.getAccountId()));
    }

    @PostMapping("/attendance/archives/{id}/remind")
    public Result<ArchiveVO> remind(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        return Result.ok(attendanceService.remind(id, user.getAccountId()));
    }

    @GetMapping("/attendance/coverage")
    public Result<List<CoverageVO>> coverage(@RequestParam(required = false) Long termId) {
        return Result.ok(attendanceService.coverage(termId));
    }
}
`);

w('controller/student/StudentAttendanceController.java', `
package ${P}.controller.student;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.AttendanceService;
import ${P}.vo.StudentAttendanceVO;
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
`);

console.log('考勤模块：控制器生成完成');
