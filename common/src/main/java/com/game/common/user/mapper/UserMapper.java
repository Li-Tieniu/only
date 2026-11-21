package com.game.common.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.game.common.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper。
 * 继承 BaseMapper 后即可直接使用单表 CRUD 与条件构造器，无需 XML。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
