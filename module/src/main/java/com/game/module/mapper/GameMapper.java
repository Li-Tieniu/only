package com.game.module.mapper;

import com.game.module.entity.GameEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.tomcat.util.http.fileupload.util.LimitedInputStream;

import java.util.List;

@Mapper
public interface GameMapper {

    //查询未删除数据
    List<GameEntity> getinfo();

    //查询所有游戏数据
    List<GameEntity> getall();

    //查询分页操作
    List<GameEntity> page(@Param("offset") int offset, @Param("size") int size);

    //查询总条数
    int count();

    //新增游戏数据
    int insert(GameEntity entity);

    //修改游戏数据
    int update(GameEntity entity);

    //逻辑删除物理数据
    int softDelete(GameEntity entity);

    //物理删除游戏数据
    int hardDelete(GameEntity entity);

}