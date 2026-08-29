package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.service.DataImportService;
import com.example.academic_risk_warning.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 批量数据导入：Excel模板上传 + 模板下载
 */
@RestController
@RequestMapping("/api/import")
public class DataImportController {

    private final DataImportService dataImportService;
    private final TokenService tokenService;

    public DataImportController(DataImportService dataImportService, TokenService tokenService) {
        this.dataImportService = dataImportService;
        this.tokenService = tokenService;
    }

    /**
     * 上传 Excel 批量导入数据
     * POST /api/import/upload
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 鉴权
        Long userId = resolveUserId(request);
        if (userId == null) {
            result.put("success", false);
            result.put("message", "未登录");
            return result;
        }

        if (file == null || file.isEmpty()) {
            result.put("success", false);
            result.put("message", "请选择要上传的Excel文件");
            return result;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            result.put("success", false);
            result.put("message", "仅支持 .xlsx 或 .xls 格式的Excel文件");
            return result;
        }

        try {
            DataImportService.ImportResult ir = dataImportService.importExcel(file);
            result.put("success", true);
            result.put("message", String.format("导入成功！成绩%d条，作业%d条，出勤%d条，共%d条",
                    ir.scoreCount, ir.homeworkCount, ir.attendanceCount, ir.total()));
            result.put("data", ir);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 下载空白导入模板
     * GET /api/import/template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        try {
            byte[] bytes = dataImportService.generateTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "数据导入模板.xlsx");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || auth.isBlank()) return null;
        if (auth.startsWith("Bearer ")) auth = auth.substring(7).trim();
        return tokenService.validateAndGetTeacherId(auth);
    }
}
