package com.swiftsprinttech.wms01.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author liujiawei
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdTime", () -> TimeUtil.format(LocalDateTime.now()), String.class);
        this.strictInsertFill(metaObject, "updatedTime", () -> TimeUtil.format(LocalDateTime.now()), String.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedTime", () -> TimeUtil.format(LocalDateTime.now()), String.class);
    }
}