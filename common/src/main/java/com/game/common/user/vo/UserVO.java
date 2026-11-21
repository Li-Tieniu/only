package com.game.common.user.vo;

import lombok.Data;

/**
 * 用户基础信息展示对象（注册 / 登录 / 用户信息接口返回，绝不包含密码）。
 */
@Data
public class UserVO {

    /** 用户主键 ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 角色：USER-普通用户，ADMIN-管理员 */
    private String role;

    /** 状态：1-正常，0-禁用 */
    private Integer status;
}
