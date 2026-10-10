package com.example.redchina.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysTag;


import java.util.List;

public interface SysTagService extends IService<SysTag> {
    // 分页查询标签（支持类型筛选）
    IPage<SysTag> selectTagPage(IPage<SysTag> page, String tagType, String keyword);

    // 根据标签类型查询标签列表
    List<SysTag> selectByTagType(String tagType);

    // 校验标签名称唯一性
    boolean checkTagNameUnique(String tagName, Long id);

    // 管理员删除标签（需先校验是否有关联数据）
    boolean removeTagById(Long id);
}
