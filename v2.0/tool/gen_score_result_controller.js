/* gen_score_result_controller.js — 1-10 成绩与批改：教师端 / 学生端控制器 */
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

w('controller/teacher/TeacherScoreMatrixController.java', `
package ${P}.controller.teacher;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.dto.ScoreEntryBatchRequest;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ScoreMatrixService;
import ${P}.vo.PendingGradingVO;
import ${P}.vo.ScoreMatrixVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 教师端 · 成绩与批改（待批改 / 手工录入 / 成绩矩阵 / 总评汇总 / 导出） */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
@RequireRole("TEACHER")
public class TeacherScoreMatrixController {

    private final ScoreMatrixService scoreMatrixService;

    private Long me() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有教师档案");
        }
        return user.getRefId();
    }

    @GetMapping("/grading/pending")
    public Result<PendingGradingVO> pending(@RequestParam Long teachingClassId) {
        return Result.ok(scoreMatrixService.pending(teachingClassId, me()));
    }

    @GetMapping("/scores/matrix")
    public Result<ScoreMatrixVO> matrix(@RequestParam Long teachingClassId) {
        return Result.ok(scoreMatrixService.matrix(teachingClassId, me()));
    }

    @PostMapping("/scores/items")
    public Result<ScoreMatrixVO> saveEntries(@Valid @RequestBody ScoreEntryBatchRequest request) {
        return Result.ok(scoreMatrixService.saveEntries(request, me()));
    }

    @PostMapping("/scores/summary")
    public Result<ScoreMatrixVO> commit(@RequestParam Long teachingClassId) {
        return Result.ok(scoreMatrixService.commitSummary(teachingClassId, me()));
    }

    /** 导出成绩矩阵（CSV，Excel 可直接打开） */
    @GetMapping("/scores/export")
    public ResponseEntity<byte[]> export(@RequestParam Long teachingClassId) {
        String csv = scoreMatrixService.exportCsv(teachingClassId, me());
        byte[] body = ("\\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        String name = "成绩矩阵.csv";
        String encoded;
        try {
            encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = "scores.csv";
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
`);

w('controller/student/StudentScoreController.java', `
package ${P}.controller.student;

import ${P}.common.BizException;
import ${P}.common.Result;
import ${P}.security.LoginUser;
import ${P}.security.RequireRole;
import ${P}.security.UserContext;
import ${P}.service.ScoreMatrixService;
import ${P}.vo.StudentScoreVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 学生端 · 我的成绩（分项 + 总评；不含 Δ / 教师尺度等教务口径） */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@RequireRole("STUDENT")
public class StudentScoreController {

    private final ScoreMatrixService scoreMatrixService;

    @GetMapping("/scores")
    public Result<List<StudentScoreVO>> my() {
        LoginUser user = UserContext.require();
        if (user.getRefId() == null) {
            throw new BizException("当前账号没有学生档案");
        }
        return Result.ok(scoreMatrixService.studentScores(user.getRefId()));
    }
}
`);

console.log('成绩模块：控制器生成完成');
