-- V1 用户模块建表语句（MySQL 8）
-- 密码字段存放 BCrypt 密文，禁止明文；username 建唯一索引，注册时由数据库兜底防重

create table sys_user
(
    id          bigint unsigned auto_increment comment '主键ID'
        primary key,
    username    varchar(50)                not null comment '用户名（登录账号，唯一）',
    password    varchar(100)               not null comment '密码（BCrypt 加密后的密文）',
    nickname    varchar(50)                null comment '昵称',
    role        varchar(20)  default 'USER' not null comment '角色：USER-普通用户，ADMIN-管理员',
    status      tinyint unsigned default 1 not null comment '状态：1-正常，0-禁用',
    create_time int unsigned               not null comment '创建时间（时间戳，秒）',
    update_time int unsigned               not null comment '修改时间（时间戳，秒）',
    constraint uk_sys_user_username unique (username)
)
    comment '系统用户表' collate = utf8mb4_unicode_ci;
