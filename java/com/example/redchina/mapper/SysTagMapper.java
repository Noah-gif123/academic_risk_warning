package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.example.redchina.entity.SysTag;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文化标签Mapper接口
 */
public interface SysTagMapper extends BaseMapper<SysTag> {
    /**
     * 分页查询标签（支持类型筛选、关键词搜索）
     * @param page 分页参数
     * @param tagType 标签类型（IP_TYPE/INTANGIBLE_CULTURE/RED_CULTURE，可选）
     * @param keyword 搜索关键词（模糊匹配标签名称/描述，可选）
     * @return 分页标签列表
     */
    IPage<SysTag> selectTagPage(IPage<SysTag> page,
                                @Param("tagType") String tagType,
                                @Param("keyword") String keyword);

    /**
     * 根据标签类型查询标签列表
     * @param tagType 标签类型（IP_TYPE/INTANGIBLE_CULTURE/RED_CULTURE）
     * @return 同类型标签列表
     */
    List<SysTag> selectByTagType(@Param("tagType") String tagType);

    /**
     * 根据标签名称查询标签（精确匹配）
     * @param tagName 标签名称
     * @return 匹配的标签
     */
    SysTag selectByTagName(@Param("tagName") String tagName);

    /**
     * 批量查询标签（根据标签ID列表）
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    List<SysTag> selectByIds(@Param("tagIds") List<Long> tagIds);

    /**
     * 查询标签使用数量（关联剧情数+关联术语数）
     * @param tagId 标签ID
     * @return 该标签的关联数据总数
     */
    int selectTagUseCount(@Param("tagId") Long tagId);

    /**
     * 查询热门标签（按关联数据量降序，取前N条）
     * @param limit 最大返回数量
     * @return 热门标签列表
     */
    List<SysTag> selectHotTags(@Param("limit") Integer limit);
}
