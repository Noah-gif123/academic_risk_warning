package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysInteraction;
import java.util.List;

/**
 * 互动关卡服务接口（继承MyBatis-Plus IService，提供基础CRUD）
 */
public interface SysInteractionService extends IService<SysInteraction> {

    /**
     * 根据剧情节点ID获取互动内容
     * @param storyNodeId 剧情节点ID
     * @return 互动对象（无则返回null）
     */
    SysInteraction getByStoryNodeId(Long storyNodeId);

    /**
     * 新增互动关卡（含参数校验）
     * @param interaction 互动对象
     * @return 是否新增成功
     */
    boolean addInteraction(SysInteraction interaction);

    /**
     * 编辑互动关卡
     * @param interaction 互动对象（需包含ID）
     * @return 是否编辑成功
     */
    boolean updateInteraction(SysInteraction interaction);

    /**
     * 批量新增互动关卡（用于初始化）
     * @param interactionList 互动列表
     * @return 新增成功数量
     */
    int batchAddInteraction(List<SysInteraction> interactionList);

    /**
     * 根据剧情ID删除关联互动（剧情删除时联动）
     * @param storyId 剧情ID
     * @return 删除成功数量
     */
    int deleteByStoryId(Long storyId);
}
