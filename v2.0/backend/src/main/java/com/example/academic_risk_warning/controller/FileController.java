package com.example.academic_risk_warning.controller;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.mapper.ResourceFileMapper;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.ResourceFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 文件下载：登录后按 id 取文件本体（不做静态目录暴露） */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@RequireRole({"STUDENT", "TEACHER", "ADMIN", "COUNSELOR"})
public class FileController {

    private final ResourceFileMapper resourceMapper;
    private final ResourceFileService resourceFileService;

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        LoginUser user = UserContext.require();
        ResourceFile f = resourceMapper.selectById(id);
        if (f == null || (f.getIsVoid() != null && f.getIsVoid() == 1)) {
            throw new BizException("文件不存在或已作废");
        }
        boolean owner = "TEACHER".equals(user.getRole()) && user.getRefId() != null
                && user.getRefId().equals(f.getUploadBy());
        if (!owner && !"ADMIN".equals(user.getRole())) {
            // 学生 / 辅导员：只允许取本教学班的文件
            if (f.getTeachingClassId() == null) {
                throw new BizException("无权下载该文件");
            }
        }
        Resource body = new FileSystemResource(resourceFileService.resolve(f));
        String name = f.getFileName() == null ? "file" : f.getFileName();
        String encoded;
        try {
            encoded = URLEncoder.encode(name, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = name;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(body);
    }
}
