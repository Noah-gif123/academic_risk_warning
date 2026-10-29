package com.example.academic_risk_warning.service;

import com.example.academic_risk_warning.common.BizException;
import com.example.academic_risk_warning.entity.ResourceFile;
import com.example.academic_risk_warning.entity.TeachingClass;
import com.example.academic_risk_warning.mapper.ResourceFileMapper;
import com.example.academic_risk_warning.service.ai.KbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 资源文件：文件本体落磁盘目录（storage_type=LOCAL），库里只存元信息。
 * 目录约定：{store-dir}/{teachingClassId}/{uuid}.{ext}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceFileService {

    private final ResourceFileMapper resourceMapper;
    private final CourseContentService contentService;
    private final KbService kbService;

    @Value("${app.file.store-dir:./file-store}")
    private String storeDir;

    public ResourceFile store(MultipartFile file, Long teachingClassId, Long teacherId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        TeachingClass tc = contentService.requireMyClass(teachingClassId, teacherId);
        return storeRaw(file, tc, teacherId, "TEACHER");
    }

    /** 通用上传：权限由调用方校验（教师资料库 / 学生项目提交都用它） */
    public ResourceFile storeRaw(MultipartFile file, TeachingClass tc, Long uploaderId, String uploadRole) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        Long teachingClassId = tc.getId();
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        String key = teachingClassId + "/" + UUID.randomUUID().toString().replace("-", "")
                + (ext.isEmpty() ? "" : "." + ext);
        Path target = root().resolve(key);
        try {
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new BizException("文件写入失败：" + e.getMessage());
        }
        ResourceFile f = new ResourceFile();
        f.setTeachingClassId(tc.getId());
        f.setOfferingId(tc.getOfferingId());
        f.setTermId(contentService.termIdOf(tc));
        f.setFileName(original);
        f.setFilePath(key);
        f.setFileExt(ext);
        f.setFileSize(file.getSize());
        f.setMimeType(file.getContentType());
        f.setStorageType("LOCAL");
        f.setUploadBy(uploaderId);
        f.setUploadRole(uploadRole);
        f.setCreateTime(LocalDateTime.now());
        f.setIsVoid(0);
        resourceMapper.insert(f);
        // 上传即入库：课件（pptx/docx/txt）立刻进课程知识库，学生答疑马上能检索到；
        // 入库失败不影响上传结果（教师可在资料库重新触发一次入库）
        if ("TEACHER".equals(uploadRole)) {
            try {
                kbService.ingestUploaded(f.getId());
            } catch (Exception e) {
                log.warn("[KB] 上传后自动入库失败 resourceId={}：{}", f.getId(), e.getMessage());
            }
        }
        return f;
    }

    /** 上传根目录：转成绝对路径（Tomcat 会把相对路径解析到临时 work 目录，务必先绝对化） */
    private Path root() {
        Path dir = Paths.get(storeDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BizException("无法创建文件目录：" + dir);
        }
        return dir;
    }

    /** 读取文件本体（下载 / 预览） */
    public Path resolve(ResourceFile f) {
        Path p = root().resolve(f.getFilePath());
        if (!Files.exists(p)) {
            throw new BizException("文件不存在或已被清理");
        }
        return p;
    }
}
