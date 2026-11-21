package com.game.common.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统用户表实体，对应表 sys_user。
 * 表名、主键、字段映射由 MyBatis-Plus 注解声明。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class UserEntity {

    /** 角色：普通用户 */
    public static final String ROLE_USER = "USER";

    /** 角色：管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 状态：正常 */
    public static final int STATUS_ENABLED = 1;

    /** 状态：禁用 */
    public static final int STATUS_DISABLED = 0;

    /** 主键 ID，数据库自增，insert 后回填 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户名（登录账号，唯一） */
    @TableField("username")
    private String username;

    /** 密码（BCrypt 加密后的密文，禁止存明文） */
    @TableField("password")
    private String password;

    /** 昵称 */
    @TableField("nickname")
    private String nickname;

    /** 角色：USER-普通用户，ADMIN-管理员 */
    @TableField("role")
    private String role;

    /** 状态：1-正常，0-禁用 */
    @TableField("status")
    private Integer status;

    /** 创建时间（Unix 时间戳，秒） */
    @TableField("create_time")
    private Integer createTime;

    /** 修改时间（Unix 时间戳，秒） */
    @TableField("update_time")
    private Integer updateTime;
}
