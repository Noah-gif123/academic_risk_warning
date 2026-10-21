package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ArchiveSubmitRequest;
import com.example.academic_risk_warning.dto.AttendanceCallRequest;
import com.example.academic_risk_warning.dto.AttendanceRecordUpdateRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.AttendanceService;
import com.example.academic_risk_warning.vo.ArchiveVO;
import com.example.academic_risk_warning.vo.AttendanceImportVO;
import com.example.academic_risk_warning.vo.AttendanceRosterRowVO;
import com.example.academic_risk_warning.vo.AttendanceSessionVO;
import com.example.academic_risk_warning.vo.AttendanceStudentStatVO;
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
