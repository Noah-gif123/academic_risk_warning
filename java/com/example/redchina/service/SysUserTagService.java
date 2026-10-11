package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysUserTag;

import java.util.List;

/**
 * 用户标签关联服务接口
 */
public interface SysUserTagService extends IService<SysUserTag> {

    /**
     * 根据用户ID查询关联的标签ID列表
     * @param userId 用户ID
     * @return 标签ID列表（无关联时返回空列表）
     */
    List<Long> getUserTagIds(Long userId);

    /**
     * 根据用户ID查询关联的标签名称列表
     * @param userId 用户ID
     * @return 标签名称列表（无关联时返回空列表）
     */
    List<String> getUserTagNames(Long userId);

    /**
     * 给用户分配标签（先删除原有标签，再添加新标签）
     * @param userId 用户ID
     * @param tagIds 新标签ID列表
     * @return 是否分配成功
     */
    boolean assignTags(Long userId, List<Long> tagIds);

    /**
     * 批量添加用户标签关联
     * @param userTagList 用户标签关联列表
     * @return 新增数量
     */
    int batchAdd(List<SysUserTag> userTagList);

    /**
     * 根据标签ID查询关联的用户ID列表
     * @param tagId 标签ID
     * @return 用户ID列表
     */
    List<Long> getUserIdByTagId(Long tagId);

    /**
     * 解除用户与所有标签的关联
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean removeByUserId(Long userId);
}
