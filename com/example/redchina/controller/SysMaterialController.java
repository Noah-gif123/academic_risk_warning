package com.example.redchina.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.redchina.common.ResultVo;
import com.example.redchina.entity.SysMaterial;
import com.example.redchina.service.SysMaterialService;

import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 文创素材Controller（用户端-素材推荐/下载；管理员端-素材管理）
 */
@RestController
@RequestMapping("/material")
public class SysMaterialController {

    @Resource
    private SysMaterialService materialService;

    // 素材上传路径（与WebMvcConfig配置一致）
    private static final String UPLOAD_PATH = System.getProperty("user.dir") + "/src/main/resources/static/material/";

    // 按标签ID查询素材（用户端-个性化推荐）
    @GetMapping("/byTag/{tagId}")
    public ResultVo<List<SysMaterial>> getByTagId(@PathVariable Long tagId) {
        return ResultVo.success(materialService.getMaterialsByTagId(tagId));
    }

    // 按多个标签ID查询素材（用户端-个性化推荐核心）
    @GetMapping("/byTagIds")
    public ResultVo<List<SysMaterial>> getByTagIds(@RequestParam List<Long> tagIds) {
        return ResultVo.success(materialService.getMaterialsByTagIds(tagIds));
    }

    // 按语言类型查询素材（多语言传播站）
    @GetMapping("/byLanguage/{languageType}")
    public ResultVo<List<SysMaterial>> getByLanguageType(@PathVariable String languageType) {
        return ResultVo.success(materialService.getMaterialsByLanguageType(languageType));
    }

    // 查询热门素材（用户端-首页推荐）
    @GetMapping("/hot")
    public ResultVo<List<SysMaterial>> getHotMaterials(@RequestParam(defaultValue = "10") Integer limit) {
        return ResultVo.success(materialService.getHotMaterials(limit));
    }

    // 素材下载次数+1（用户端-下载素材）
    @PostMapping("/download/{id}")
    public ResultVo<Boolean> downloadMaterial(@PathVariable Long id) {
        return ResultVo.success(materialService.incrementDownloadCount(id));
    }

    // 分页查询素材（管理员端-素材管理）
    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<IPage<SysMaterial>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String languageType) {
        IPage<SysMaterial> page = new Page<>(pageNum, pageSize);
        return ResultVo.success(materialService.getMaterialPage(page, tagId, languageType));
    }

    // 上传素材图片（管理员端）
    @PostMapping("/uploadImage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResultVo.error("请选择上传文件");
        }
        // 创建上传目录（不存在则创建）
        File uploadDir = new File(UPLOAD_PATH);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        // 生成唯一文件名（避免覆盖）
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID() + suffix;
        // 保存文件
        try {
            file.transferTo(new File(UPLOAD_PATH + fileName));
            // 返回文件访问路径（对应WebMvcConfig配置的/material/**）
            return ResultVo.success("/material/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return ResultVo.error("文件上传失败");
        }
    }

    // 新增素材（管理员端）
    @PostMapping("/save")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> saveMaterial(@RequestBody SysMaterial material) {
        return ResultVo.success(materialService.saveMaterial(material));
    }

    // 更新素材（管理员端）
    @PutMapping("/update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> updateMaterial(@RequestBody SysMaterial material) {
        if (material.getId() == null) {
            return ResultVo.error("素材ID不能为空");
        }
        return ResultVo.success(materialService.updateById(material));
    }

    // 删除素材（管理员端）
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> deleteMaterial(@PathVariable Long id) {
        return ResultVo.success(materialService.removeById(id));
    }

    // 批量删除素材（管理员端）
    @DeleteMapping("/deleteBatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResultVo<Boolean> deleteBatch(@RequestParam List<Long> ids) {
        return ResultVo.success(materialService.removeMaterialBatch(ids));
    }
}
