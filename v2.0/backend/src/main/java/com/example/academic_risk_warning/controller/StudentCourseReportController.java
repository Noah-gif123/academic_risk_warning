package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ai.StudentReportService;
import com.example.academic_risk_warning.service.ai.ReportPdfService;
import com.example.academic_risk_warning.vo.ai.StudentReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师/辅导员查看与生成“学生 × 课程”报告。
 *
 * <p>报告快照落在 ai_report，按 teachingClassId 做权限校验：
 * 教师只能看自己任教课程；辅导员只能看名下行政班学生。</p>
 */
@RestController
@RequestMapping("/api/reports/student")
@RequiredArgsConstructor
@RequireRole({"TEACHER", "COUNSELOR"})
public class StudentCourseReportController {

    private final StudentReportService reportService;
    private final ReportPdfService pdfService;
    private final JdbcTemplate jdbc;

    @GetMapping("/{studentId}")
    public Result<List<StudentReportVO>> list(@PathVariable Long studentId) {
        checkStudentList(studentId);
        List<StudentReportVO> all = reportService.list(studentId);
        LoginUser user = UserContext.require();
        if ("TEACHER".equals(user.getRole())) {
            all = all.stream().filter(r -> ownsTeachingClass(user.getRefId(), r.getTeachingClassId())).toList();
        } else {
            all = all.stream().filter(r -> ownsStudent(user.getRefId(), studentId)).toList();
        }
        return Result.ok(all);
    }

    @GetMapping("/{studentId}/{reportId}")
    public Result<StudentReportVO> detail(@PathVariable Long studentId, @PathVariable Long reportId) {
        StudentReportVO vo = reportService.detail(studentId, reportId);
        LoginUser user = UserContext.require();
        if ("TEACHER".equals(user.getRole()) && !ownsTeachingClass(user.getRefId(), vo.getTeachingClassId())) {
            throw new BizException(403, "只能查看自己任教课程的学生报告");
        }
        checkStudent(studentId, vo.getTeachingClassId());
        return Result.ok(vo);
    }

    @PostMapping("/{studentId}/generate")
    public Result<StudentReportVO> generate(@PathVariable Long studentId,
                                            @RequestParam Long teachingClassId) {
        checkStudent(studentId, teachingClassId);
        return Result.ok(reportService.generateByClass(studentId, teachingClassId, "COURSE"));
    }

    @GetMapping("/{studentId}/{reportId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long studentId, @PathVariable Long reportId) {
        StudentReportVO vo = reportService.detail(studentId, reportId);
        LoginUser user = UserContext.require();
        if ("TEACHER".equals(user.getRole()) && !ownsTeachingClass(user.getRefId(), vo.getTeachingClassId())) {
            throw new BizException(403, "只能导出自己任教课程的学生报告");
        }
        checkStudent(studentId, vo.getTeachingClassId());
        byte[] bytes = pdfService.render(vo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + reportId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /** 列表只要求：教师名下任一课程有该生，或辅导员名下行政班有该生 */
    private void checkStudentList(Long studentId) {
        LoginUser user = UserContext.require();
        if ("TEACHER".equals(user.getRole())) {
            Integer n = jdbc.queryForObject("""
                    select count(*) from enrollment e join teaching_class tc on tc.id = e.teaching_class_id
                    where e.student_id = ? and tc.teacher_id = ? and e.status in ('ACTIVE','ENROLLED')
                    """, Integer.class, studentId, user.getRefId());
            if (n == null || n == 0) throw new BizException(403, "只能查看自己任教课程的学生报告");
        } else if (!ownsStudent(user.getRefId(), studentId)) {
            throw new BizException(403, "只能查看名下行政班学生的报告");
        }
    }

    /** 教师校验教学班归属；辅导员校验学生行政班归属 */
    private void checkStudent(Long studentId, Long teachingClassId) {
        LoginUser user = UserContext.require();
        if ("TEACHER".equals(user.getRole())) {
            if (teachingClassId == null) throw new BizException(400, "缺少 teachingClassId");
            if (!ownsTeachingClass(user.getRefId(), teachingClassId)) {
                throw new BizException(403, "只能查看自己任教课程的学生报告");
            }
            Integer n = jdbc.queryForObject(
                    "select count(*) from enrollment where student_id = ? and teaching_class_id = ? and status in ('ACTIVE','ENROLLED')",
                    Integer.class, studentId, teachingClassId);
            if (n == null || n == 0) throw new BizException(404, "该学生不在这个教学班");
        } else {
            if (!ownsStudent(user.getRefId(), studentId)) {
                throw new BizException(403, "只能查看名下行政班学生的报告");
            }
        }
    }

    private boolean ownsTeachingClass(Long teacherId, Long teachingClassId) {
        if (teacherId == null || teachingClassId == null) return false;
        Integer n = jdbc.queryForObject(
                "select count(*) from teaching_class where id = ? and teacher_id = ?",
                Integer.class, teachingClassId, teacherId);
        return n != null && n > 0;
    }

    private boolean ownsStudent(Long counselorId, Long studentId) {
        Integer n = jdbc.queryForObject("""
                select count(*) from student s join counselor_class cc on cc.admin_class_id = s.admin_class_id
                where s.id = ? and cc.counselor_id = ?
                """, Integer.class, studentId, counselorId);
        return n != null && n > 0;
    }
}
