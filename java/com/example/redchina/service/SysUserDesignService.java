package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUserDesign;
import com.example.redchina.common.ResultVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SysUserDesignService extends IService<SysUserDesign> {
    // 根据用户ID查询设计列表
    List<SysUserDesign> getUserDesigns(Long userId);

    // 上传文创设计
    ResultVo<SysUserDesign> uploadDesign(MultipartFile file, Long userId, String designName, String tags, String languageType);

    // 根据标签推荐设计
    List<SysUserDesign> recommendByTag(String tag);

    // 删除文创设计
    ResultVo<?> deleteDesign(Long id, Long userId);
}
