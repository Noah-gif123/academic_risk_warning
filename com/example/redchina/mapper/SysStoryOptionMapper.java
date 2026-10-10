package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysStoryOption;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 剧情选项Mapper接口
 */
public interface SysStoryOptionMapper extends BaseMapper<SysStoryOption> {
    /**
     * 根据节点ID查询选项（按ID升序排列）
     * @param nodeId 节点ID
     * @return 选项列表
     */
    List<SysStoryOption> selectByNodeId(@Param("nodeId") Long nodeId);

    /**
     * 根据节点ID和下一个节点ID查询选项
     * @param nodeId 当前节点ID
     * @param nextNodeId 下一个节点ID
     * @return 匹配的选项
     */
    SysStoryOption selectByNodeIdAndNextNodeId(@Param("nodeId") Long nodeId, @Param("nextNodeId") Long nextNodeId);

    /**
     * 批量删除节点对应的选项
     * @param nodeIds 节点ID列表
     * @return 删除数量
     */
    int deleteBatchByNodeIds(@Param("nodeIds") List<Long> nodeIds);

    // 新增：根据剧情ID查询所有选项
    List<SysStoryOption> selectByStoryId(@Param("storyId") Long storyId);

    // 新增：批量插入选项
    int insertBatch(@Param("list") List<SysStoryOption> optionList);

    /**
     * 批量删除剧情对应的选项
     * @param storyIds 剧情ID列表
     * @return 删除数量
     */
    int deleteBatchByStoryIds(@Param("storyIds") List<Long> storyIds);
}