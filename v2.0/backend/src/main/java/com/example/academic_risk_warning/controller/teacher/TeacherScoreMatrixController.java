package com.example.academic_risk_warning.controller.teacher;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.dto.ScoreEntryBatchRequest;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ScoreMatrixService;
import com.example.academic_risk_warning.vo.PendingGradingVO;
import com.example.academic_risk_warning.vo.ScoreMatrixVO;
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
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
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
