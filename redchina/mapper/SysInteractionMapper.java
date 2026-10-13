package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysInteraction;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 互动关卡Mapper接口（继承MyBatis-Plus BaseMapper，减少重复CRUD）
 */
@Repository
public interface SysInteractionMapper extends BaseMapper<SysInteraction> {

    /**
     * 根据剧情节点ID查询互动内容（核心查询：剧情节点触发互动）
     * @param storyNodeId 剧情节点ID
     * @return 互动对象（无则返回null）
     */
    SysInteraction selectByStoryNodeId(@Param("storyNodeId") Long storyNodeId);

    /**
     * 批量插入互动数据（用于初始化剧情互动）
     * @param interactionList 互动列表
     * @return 插入成功数量
     */
    int batchInsert(@Param("list") java.util.List<SysInteraction> interactionList);
}
