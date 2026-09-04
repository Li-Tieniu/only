package com.game.module.mapper;

import com.game.module.entity.GameEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GameMapper {

    //查询未删除数据
    List<GameEntity> getinfo();

    //查询所有游戏数据
    List<GameEntity> getall();

    //新增游戏数据
    int insert(GameEntity entity);

    //修改游戏数据
    int update(GameEntity entity);

    //逻辑删除物理数据
    int softDelete(GameEntity entity);

    //物理删除游戏数据
    int hardDelete(GameEntity entity);

}