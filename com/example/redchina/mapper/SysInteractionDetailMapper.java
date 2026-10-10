package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.redchina.entity.SysInteractionDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysInteractionDetailMapper extends BaseMapper<SysInteractionDetail> {
    // 根据互动关卡ID查询详情
    SysInteractionDetail selectByInteractionId(@Param("interactionId") Long interactionId);
}
