package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysMaterial;
import java.util.List;

/**
 * 文创素材Service
 */
public interface SysMaterialService extends IService<SysMaterial> {

    // 按标签ID查询素材
    List<SysMaterial> getMaterialsByTagId(Long tagId);

    // 按多个标签ID查询素材（个性化推荐核心）
    List<SysMaterial> getMaterialsByTagIds(List<Long> tagIds);

    // 按语言类型查询素材
    List<SysMaterial> getMaterialsByLanguageType(String languageType);

    // 分页查询素材
    IPage<SysMaterial> getMaterialPage(IPage<SysMaterial> page, Long tagId, String languageType);

    // 查询热门素材
    List<SysMaterial> getHotMaterials(Integer limit);

    // 素材下载次数+1
    boolean incrementDownloadCount(Long id);

    // 新增素材（管理员功能）
    boolean saveMaterial(SysMaterial material);

    // 批量删除素材（管理员功能）
    boolean removeMaterialBatch(List<Long> ids);
}
