package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.redchina.entity.SysStoryNode;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 剧情节点Mapper接口
 */
public interface SysStoryNodeMapper extends BaseMapper<SysStoryNode> {
    /**
     * 根据剧情ID查询所有节点（按ID升序排列）
     * @param storyId 剧情ID
     * @return 节点列表
     */
    List<SysStoryNode> selectByStoryId(@Param("storyId") Long storyId);

    /**
     * 根据父节点ID查询子节点
     * @param parentNodeId 父节点ID
     * @return 子节点列表
     */
    List<SysStoryNode> selectByParentNodeId(@Param("parentNodeId") Long parentNodeId);

    /**
     * 根据节点ID查询节点详情（含关联知识点ID）
     * @param id 节点ID
     * @return 节点详情
     */
    SysStoryNode selectNodeDetail(@Param("id") Long id);

    /**
     * 查询剧情的所有结局节点
     * @param storyId 剧情ID
     * @return 结局节点列表
     */
    List<SysStoryNode> selectEndNodesByStoryId(@Param("storyId") Long storyId);

    /**
     * 批量删除剧情对应的节点
     * @param storyIds 剧情ID列表
     * @return 删除数量
     */
    int deleteBatchByStoryIds(@Param("storyIds") List<Long> storyIds);

    /**
     * 查询剧情的初始节点（parent_node_id为0或null）
     * @param storyId 剧情ID
     * @return 初始节点
     */
    SysStoryNode selectInitialNode(@Param("storyId") Long storyId);
}

