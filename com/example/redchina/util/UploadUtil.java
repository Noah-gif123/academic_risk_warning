package com.example.redchina.util;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传工具（适配用户文创设计上传）
 */
@Component
public class UploadUtil {

    // 外部存储目录（与ResourceConfig配置一致）
    @Value("${upload.base-path:D:/culture-images/upload/}")
    private String basePath;

    /**
     * 上传文件（返回相对路径，存入数据库）
     */
    public String uploadFile(MultipartFile file, Long userId) throws IOException {
        // 1. 校验文件不为空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 2. 构建目录（按用户ID+日期分目录）
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String userDir = basePath + "design/" + userId + "/" + dateDir + "/";
        File dir = new File(userDir);
        if (!dir.exists()) {
            dir.mkdirs(); // 递归创建目录
        }

        // 3. 生成唯一文件名（避免覆盖）
        String originalFilename = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalFilename); // 文件后缀
        String fileName = UUID.randomUUID().toString() + "." + extension;

        // 4. 保存文件
        File destFile = new File(userDir + fileName);
        file.transferTo(destFile);

        // 5. 返回相对路径（数据库存储格式：/upload/design/{userId}/{dateDir}/{fileName}）
        return "/upload/design/" + userId + "/" + dateDir + "/" + fileName;
    }
}
