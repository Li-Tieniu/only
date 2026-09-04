package com.game.console.service;

import com.game.module.DTO.AddGameDTO;
import com.game.module.entity.GameEntity;
import com.game.module.mapper.GameMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CommandService {

    @Autowired
    private GameMapper gameMapper;

    //新增数据
    public Long addGame(AddGameDTO dto) {

        GameEntity entity = new GameEntity();
        entity.setCoverImage(dto.getCoverImage());
        entity.setWallImages(dto.getWallImages());
        entity.setGameName(dto.getGameName());
        entity.setDetails(dto.getDetails());
        entity.setPrice(dto.getPrice());
        entity.setIsDeleted(0);
        long now = Instant.now().getEpochSecond();
        entity.setCreateTime((int) now);
        entity.setUpdateTime((int) now);

        gameMapper.insert(entity);
        return entity.getItemId();

    }

    //修改数据
    public int updateGame(AddGameDTO dto) {

        GameEntity entity = new GameEntity();
        entity.setItemId(dto.getItemId());
        entity.setCoverImage(dto.getCoverImage());
        entity.setWallImages(dto.getWallImages());
        entity.setGameName(dto.getGameName());
        entity.setPrice(dto.getPrice() != null ? dto.getPrice().toString() : null);
        entity.setDetails(dto.getDetails());
        long now = Instant.now().getEpochSecond();
        entity.setUpdateTime((int) now);

        return gameMapper.update(entity);
    }

    //删除数据(逻辑删除)
    public int softDeleteGame(AddGameDTO dto){

        GameEntity entity = new GameEntity();
        entity.setItemId(dto.getItemId());
        long now = Instant.now().getEpochSecond();
        entity.setUpdateTime((int) now);

        return gameMapper.softDelete(entity);
    }

    //强制删除数据(物理删除)
    public int hardDeleteGame(AddGameDTO dto){

        GameEntity entity = new GameEntity();
        entity.setItemId(dto.getItemId());

        return gameMapper.hardDelete(entity);

    }
}
