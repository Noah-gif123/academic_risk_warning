/* gen_teacher_portal_controller.js — 1-12 教师端控制器（首页 / 教学统计 / 导出 / 结课） */
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

w('controller/teacher/TeacherPortalController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.TeacherPortalService;
import ${P}.vo.CourseStatsVO;
import ${P}.vo.TeacherHomeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 教师端 · 首页汇总、教学统计、导出学生数据、结课 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherPortalController {

    private final TeacherPortalService portalService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @GetMapping("/home")
    public Result<TeacherHomeVO> home(@RequestParam(required = false) Long termId) {
        return Result.ok(portalService.home(me(), termId));
    }

    @GetMapping("/course-stats")
    public Result<CourseStatsVO> stats(@RequestParam Long teachingClassId) {
        return Result.ok(portalService.stats(teachingClassId, me()));
    }

    @GetMapping("/course-export")
    public ResponseEntity<byte[]> export(@RequestParam Long teachingClassId) {
        String csv = portalService.exportStudents(teachingClassId, me());
        byte[] body = ("\\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        String name = "学生数据.csv";
        String encoded;
        try {
            encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "students.csv";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }

    /** 结课：结课后该课程在学生端不再显示（数据保留） */
    @PostMapping("/teaching-classes/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        portalService.closeClass(id, me());
        return Result.ok();
    }
}
`);

console.log('教师端模块：控制器生成完成');
