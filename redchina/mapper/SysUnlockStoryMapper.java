package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.example.redchina.entity.SysUnlockStory;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户解锁剧情Mapper接口
 */
public interface SysUnlockStoryMapper extends BaseMapper<SysUnlockStory> {
    /**
     * 根据用户ID和剧情ID查询解锁记录
     * @param userId 用户ID
     * @param storyId 剧情ID
     * @return 解锁记录
     */
    SysUnlockStory selectByUserAndStory(@Param("userId") Long userId, @Param("storyId") Long storyId);

    /**
     * 根据用户ID查询已解锁的剧情列表
     * @param userId 用户ID
     * @return 解锁剧情记录列表
     */
    List<SysUnlockStory> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和完成状态查询解锁剧情
     * @param userId 用户ID
     * @param isComplete 完成状态（0-未完成，1-已完成）
     * @return 解锁剧情记录列表
     */
    List<SysUnlockStory> selectByUserIdAndCompleteStatus(@Param("userId") Long userId, @Param("isComplete") Integer isComplete);

    /**
     * 分页查询用户解锁记录（管理员统计用）
     * @param page 分页参数
     * @param userId 用户ID（可选）
     * @param storyId 剧情ID（可选）
     * @return 分页解锁记录列表
     */
    IPage<SysUnlockStory> selectUnlockStoryPage(IPage<SysUnlockStory> page, @Param("userId") Long userId, @Param("storyId") Long storyId);

    /**
     * 根据剧情ID查询解锁该剧情的用户数量
     * @param storyId 剧情ID
     * @return 用户数量
     */
    int selectUserCountByStoryId(@Param("storyId") Long storyId);
}
