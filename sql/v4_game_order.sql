-- V4 订单模块建表语句（MySQL 8）
-- 订单号建唯一索引保证唯一性；game_name / price 是下单时的商品快照；金额统一用 decimal

create table game_order
(
    id          bigint unsigned auto_increment comment '订单ID'
        primary key,
    order_no    varchar(32)                    not null comment '订单编号（唯一）',
    user_id     bigint unsigned                not null comment '下单用户ID',
    item_id     bigint unsigned                not null comment '商品ID',
    game_name   varchar(50)                    not null comment '下单时的商品名称快照',
    price       decimal(10, 2)                 not null comment '下单时的商品价格快照',
    status      varchar(20) default 'PENDING'  not null comment '订单状态：PENDING-待支付，PAID-已支付',
    create_time int unsigned                   not null comment '创建时间（时间戳，秒）',
    pay_time    int unsigned                   null comment '支付时间（时间戳，秒），未支付为空',
    constraint uk_game_order_order_no unique (order_no)
)
    comment '游戏交易订单表' collate = utf8mb4_unicode_ci;

create index idx_game_order_user_id
    on game_order (user_id);
