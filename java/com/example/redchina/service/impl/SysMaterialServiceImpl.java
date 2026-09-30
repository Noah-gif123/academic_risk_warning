package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysMaterial;
import com.example.redchina.exception.BusinessException;
import com.example.redchina.mapper.SysMaterialMapper;
import com.example.redchina.service.SysMaterialService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 文创素材Service实现
 */
@Service
public class SysMaterialServiceImpl extends ServiceImpl<SysMaterialMapper, SysMaterial> implements SysMaterialService {

    @Resource
    private SysMaterialMapper materialMapper;

    @Override
    public List<SysMaterial> getMaterialsByTagId(Long tagId) {
        if (tagId == null) {
            throw new BusinessException("标签ID不能为空");
        }
        return materialMapper.selectByTagId(tagId);
    }

    @Override
    public List<SysMaterial> getMaterialsByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw new BusinessException("标签ID列表不能为空");
        }
        return materialMapper.selectByTagIds(tagIds);
    }

    @Override
    public List<SysMaterial> getMaterialsByLanguageType(String languageType) {
        if (languageType == null || languageType.isEmpty()) {
            throw new BusinessException("语言类型不能为空");
        }
        return materialMapper.selectByLanguageType(languageType);
    }

    @Override
    public IPage<SysMaterial> getMaterialPage(IPage<SysMaterial> page, Long tagId, String languageType) {
        return materialMapper.selectMaterialPage(page, tagId, languageType);
    }

    @Override
    public List<SysMaterial> getHotMaterials(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10; // 默认返回10条热门素材
        }
        return materialMapper.selectHotMaterials(limit);
    }

    @Override
    public boolean incrementDownloadCount(Long id) {
        if (id == null) {
            throw new BusinessException("素材ID不能为空");
        }
        // 校验素材是否存在
        SysMaterial material = getById(id);
        if (material == null) {
            throw new BusinessException("素材不存在");
        }
        return materialMapper.incrementDownloadCount(id) > 0;
    }

    @Override
    public boolean saveMaterial(SysMaterial material) {
        if (material == null || material.getMaterialName() == null || material.getTagId() == null) {
            throw new BusinessException("素材名称和标签ID不能为空");
        }
        // 默认下载次数为0
        material.setDownloadCount(0);
        return save(material);
    }

    @Override
    public boolean removeMaterialBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("素材ID列表不能为空");
        }
        return removeByIds(ids);
    }
}
