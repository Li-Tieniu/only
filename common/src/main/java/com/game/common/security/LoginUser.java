package com.game.common.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户（从 JWT 中解析出来，作为 Spring Security 的 principal）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    /** 用户主键 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 角色：USER-普通用户，ADMIN-管理员 */
    private String role;
}
