package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.example.redchina.entity.SysStoryKnowledge;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 剧情知识点Mapper接口（多语言支持）
 */
public interface SysStoryKnowledgeMapper extends BaseMapper<SysStoryKnowledge> {
    /**
     * 根据节点ID和语言类型查询知识点（核心场景：剧情节点展示知识点）
     * @param storyNodeId 剧情节点ID
     * @param languageType 语言类型（zh_CN/en_US/ja_JP/ko_KR）
     * @return 匹配的知识点（单个节点单个语言仅存一条）
     */
    SysStoryKnowledge selectByNodeIdAndLanguage(@Param("storyNodeId") Long storyNodeId,
                                                @Param("languageType") String languageType);

    /**
     * 根据剧情ID查询所有知识点（多语言）
     * @param storyId 剧情ID
     * @return 该剧情下所有节点的知识点列表
     */
    List<SysStoryKnowledge> selectByStoryId(@Param("storyId") Long storyId);

    /**
     * 根据节点ID查询所有语言的知识点
     * @param storyNodeId 剧情节点ID
     * @return 该节点的多语言知识点列表
     */
    List<SysStoryKnowledge> selectByNodeId(@Param("storyNodeId") Long storyNodeId);

    /**
     * 分页查询知识点（管理员管理用，支持剧情ID、节点ID筛选）
     * @param page 分页参数
     * @param storyId 剧情ID（可选）
     * @param storyNodeId 节点ID（可选）
     * @param languageType 语言类型（可选）
     * @return 分页知识点列表
     */
    IPage<SysStoryKnowledge> selectKnowledgePage(IPage<SysStoryKnowledge> page,
                                                 @Param("storyId") Long storyId,
                                                 @Param("storyNodeId") Long storyNodeId,
                                                 @Param("languageType") String languageType);

    /**
     * 批量删除节点对应的知识点
     * @param storyNodeIds 节点ID列表
     * @return 删除数量
     */
    int deleteBatchByNodeIds(@Param("storyNodeIds") List<Long> storyNodeIds);

    /**
     * 批量删除剧情对应的知识点
     * @param storyIds 剧情ID列表
     * @return 删除数量
     */
    int deleteBatchByStoryIds(@Param("storyIds") List<Long> storyIds);

    /**
     * 校验节点+语言的知识点唯一性（新增/编辑时使用）
     * @param storyNodeId 节点ID
     * @param languageType 语言类型
     * @param id 知识点ID（编辑时传入，排除自身）
     * @return 存在数量（0=唯一，1=已存在）
     */
    int checkNodeLanguageUnique(@Param("storyNodeId") Long storyNodeId,
                                @Param("languageType") String languageType,
                                @Param("id") Long id);
}
