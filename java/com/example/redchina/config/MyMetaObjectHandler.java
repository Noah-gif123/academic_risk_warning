package com.example.redchina.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 仅@Component，不要加@Configuration（否则会被识别为配置类，导致Bean冲突）
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    // 插入时自动填充（示例）
    @Override
    public void insertFill(MetaObject metaObject) {
        // 填充创建时间、创建人等
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
    }

    // 更新时自动填充（示例）
    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}