package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysUserInteraction;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户-互动关卡结果Mapper
 */
public interface SysUserInteractionMapper extends BaseMapper<SysUserInteraction> {

    // 按用户ID查询互动记录
    List<SysUserInteraction> selectByUserId(@Param("userId") Long userId);

    // 按用户ID+互动关卡ID查询（判断是否已参与）
    SysUserInteraction selectByUserAndInteraction(@Param("userId") Long userId, @Param("interactionId") Long interactionId);

    // 按互动关卡ID查询所有用户结果
    List<SysUserInteraction> selectByInteractionId(@Param("interactionId") Long interactionId);

    // 按用户ID+成功状态查询
    List<SysUserInteraction> selectByUserIdAndSuccess(@Param("userId") Long userId, @Param("isSuccess") Integer isSuccess);
}