package com.game.common.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.game.common.exception.BusinessException;
import com.game.common.security.JwtUtil;
import com.game.common.user.convert.UserConverter;
import com.game.common.user.dto.UserDTO;
import com.game.common.user.dto.UserLoginDTO;
import com.game.common.user.entity.UserEntity;
import com.game.common.user.mapper.UserMapper;
import com.game.common.user.vo.LoginVO;
import com.game.common.user.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务：注册、登录、查询用户信息（app / console 共用）。
 * 登录成功后由 JwtUtil 生成 Token 返回给前端，后续请求靠该 Token 认证。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册用户：用户名唯一校验 + BCrypt 加密存储。
     *
     * @param dto 注册请求（用户名、原始密码、昵称）
     * @return 新用户主键 ID
     */
    public Long register(UserDTO dto) {
        String username = trimToNull(dto.getUsername());
        String password = trimToNull(dto.getPassword());
        if (username == null) {
            throw new BusinessException("用户名不能为空");
        }
        if (password == null) {
            throw new BusinessException("密码不能为空");
        }
        if (existsByUsername(username)) {
            log.warn("注册失败，用户名已存在, username={}", username);
            throw new BusinessException("用户名已存在");
        }

        // nickname 不传时默认取用户名
        String nickname = trimToNull(dto.getNickname());
        UserDTO normalized = new UserDTO();
        normalized.setUsername(username);
        normalized.setNickname(nickname == null ? username : nickname);

        UserEntity entity = UserConverter.toNewEntity(normalized, passwordEncoder.encode(password));
        try {
            userMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发注册同一用户名时由唯一索引兜底
            log.warn("注册失败，用户名已存在（唯一索引兜底）, username={}", username);
            throw new BusinessException("用户名已存在");
        }
        log.info("用户注册成功, userId={}, username={}", entity.getId(), username);
        return entity.getId();
    }

    /**
     * 登录：校验用户名与密码，成功后签发 JWT Token 并返回基础用户信息。
     *
     * @param dto 登录请求
     * @return Token + 用户基础信息（不含密码）
     */
    public LoginVO login(UserLoginDTO dto) {
        String username = trimToNull(dto.getUsername());
        String password = dto.getPassword();
        if (username == null || password == null || password.isBlank()) {
            log.warn("登录失败，用户名或密码为空");
            throw new BusinessException(401, "用户名或密码错误");
        }

        UserEntity user = findByUsername(username);
        // 账号不存在与密码错误返回同样的提示，避免暴露账号是否注册过
        if (user == null) {
            log.warn("登录失败，用户不存在, username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("登录失败，密码错误, username={}", username);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!Integer.valueOf(UserEntity.STATUS_ENABLED).equals(user.getStatus())) {
            log.warn("登录失败，账号已被禁用, username={}", username);
            throw new BusinessException(403, "账号已被禁用");
        }

        // 认证通过：把 id、username、role 写进 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        log.info("用户登录成功, userId={}, username={}, role={}", user.getId(), username, user.getRole());
        return LoginVO.of(token, jwtUtil.getExpireSeconds(), UserConverter.toVO(user));
    }

    /**
     * 查询用户基础信息（当前登录用户由 Controller 从 SecurityContext 中取，再传 userId 进来）。
     *
     * @param userId 用户主键 ID
     */
    public UserVO getInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException("userId 不能为空");
        }
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserConverter.toVO(user);
    }

    /** 按用户名查询用户（用户名唯一，最多一条） */
    private UserEntity findByUsername(String username) {
        return userMapper.selectOne(
                Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username));
    }

    /** 用户名是否已存在 */
    private boolean existsByUsername(String username) {
        return userMapper.exists(
                Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getUsername, username));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
