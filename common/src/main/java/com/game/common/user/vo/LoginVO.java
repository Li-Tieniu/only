package com.game.common.user.vo;

import lombok.Data;

/**
 * 登录成功返回：JWT Token + 基础用户信息。
 * 前端拿到 token 后，后续请求放在请求头 Authorization: Bearer &lt;token&gt; 中。
 */
@Data
public class LoginVO {

    /** JWT Token */
    private String token;

    /** Token 过期时间（秒） */
    private Long expireSeconds;

    /** 基础用户信息（不含密码） */
    private UserVO user;

    /** 构建登录结果 */
    public static LoginVO of(String token, long expireSeconds, UserVO user) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpireSeconds(expireSeconds);
        vo.setUser(user);
        return vo;
    }
}
