package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.example.redchina.entity.SysUserTag;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户-标签关联Mapper接口
 */
public interface SysUserTagMapper extends BaseMapper<SysUserTag> {
    /**
     * 根据用户ID查询关联的标签ID
     * @param userId 用户ID
     * @return 标签ID列表
     */
    List<Long> selectTagIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据标签ID查询关联的用户ID
     * @param tagId 标签ID
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByTagId(@Param("tagId") Long tagId);

    /**
     * 批量插入用户-标签关联
     * @param userTagList 用户-标签关联列表
     * @return 插入数量
     */
    int insertBatch(@Param("list") List<SysUserTag> userTagList);

    /**
     * 根据用户ID删除所有关联标签
     * @param userId 用户ID
     * @return 删除数量
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据标签ID删除所有关联用户
     * @param tagId 标签ID
     * @return 删除数量
     */
    int deleteByTagId(@Param("tagId") Long tagId);

    /**
     * 根据用户ID查询关联的标签名称列表
     * @param userId 用户ID
     * @return 标签名称列表
     */
    List<String> selectTagNamesByUserId(@Param("userId") Long userId);
}
