package com.game.common.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求体。
 */
@Data
public class UserDTO {

    /** 用户名（登录账号，必填，唯一） */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    private String username;

    /** 原始密码（必填，服务端使用 BCrypt 加密后再入库） */
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过 64")
    private String password;

    /** 昵称（可选，不传时默认取用户名） */
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;
}
