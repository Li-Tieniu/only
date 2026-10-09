package com.game.common.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求体。
 */
@Data
public class UserLoginDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 原始密码（服务端用 BCrypt 与库中密文比对） */
    @NotBlank(message = "密码不能为空")
    private String password;
}
