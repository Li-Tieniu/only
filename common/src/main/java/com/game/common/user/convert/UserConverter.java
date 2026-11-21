package com.game.common.user.convert;

import com.game.common.user.dto.UserDTO;
import com.game.common.user.entity.UserEntity;
import com.game.common.user.vo.UserVO;

import java.time.Instant;

/**
 * 用户对象转换：DTO / Entity / VO
 */
public final class UserConverter {

    private UserConverter() {
    }

    /**
     * 注册请求转实体。
     *
     * @param dto             注册请求
     * @param encodedPassword BCrypt 加密后的密码密文
     */
    public static UserEntity toNewEntity(UserDTO dto, String encodedPassword) {
        int now = currentEpochSecond();
        UserEntity entity = new UserEntity();
        entity.setUsername(dto.getUsername());
        entity.setPassword(encodedPassword);
        entity.setNickname(dto.getNickname());
        entity.setRole(UserEntity.ROLE_USER);
        entity.setStatus(UserEntity.STATUS_ENABLED);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        return entity;
    }

    /** 实体转用户基础信息 VO（不携带密码） */
    public static UserVO toVO(UserEntity entity) {
        UserVO vo = new UserVO();
        vo.setId(entity.getId());
        vo.setUsername(entity.getUsername());
        vo.setNickname(entity.getNickname());
        vo.setRole(entity.getRole());
        vo.setStatus(entity.getStatus());
        return vo;
    }

    private static int currentEpochSecond() {
        return (int) Instant.now().getEpochSecond();
    }
}
