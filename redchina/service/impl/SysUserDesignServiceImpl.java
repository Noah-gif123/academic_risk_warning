package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.redchina.entity.SysUserDesign;
import com.example.redchina.mapper.SysUserDesignMapper;
import com.example.redchina.service.SysUserDesignService;
import com.example.redchina.common.ResultVo;
import com.example.redchina.util.UploadUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;

@Service
public class SysUserDesignServiceImpl extends ServiceImpl<SysUserDesignMapper, SysUserDesign> implements SysUserDesignService {

    @Resource
    private SysUserDesignMapper userDesignMapper;

    @Resource
    private UploadUtil uploadUtil;

    @Override
    public List<SysUserDesign> getUserDesigns(Long userId) {
        return userDesignMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVo<SysUserDesign> uploadDesign(MultipartFile file, Long userId, String designName, String tags, String languageType) {
        try {
            // 1. 上传文件，获取存储路径
            String filePath = uploadUtil.uploadFile(file, userId);

            // 2. 构建实体类
            SysUserDesign design = new SysUserDesign();
            design.setUserId(userId);
            design.setDesignName(designName);
            design.setFilePath(filePath);
            design.setTags(tags);
            design.setLanguageType(languageType);

            // 3. 保存到数据库
            save(design);

            return ResultVo.success(design);
        } catch (IOException e) {
            log.error("文件上传失败：", e);
            return ResultVo.error("文件上传失败，请重试");
        } catch (IllegalArgumentException e) {
            return ResultVo.error(e.getMessage());
        }
    }

    @Override
    public List<SysUserDesign> recommendByTag(String tag) {
        return userDesignMapper.selectByTags(tag);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResultVo<?> deleteDesign(Long id, Long userId) {
        // 1. 校验设计归属
        SysUserDesign design = getById(id);
        if (design == null) {
            return ResultVo.error("设计不存在");
        }
        if (!design.getUserId().equals(userId)) {
            return ResultVo.error("无权限删除该设计");
        }

        // 2. 删除数据库记录（文件可保留或异步删除）
        removeById(id);

        return ResultVo.success();
    }
}