-- MySQL 8 / InnoDB；在已完成 V5 的库上执行一次。不修改记录或既有索引。
-- 执行前 SHOW INDEX FROM game_trading，确认下列索引尚不存在。
-- 大表请在低峰执行；LOCK=NONE 仍需短暂元数据锁，会增加磁盘和写入维护成本。
ALTER TABLE game_trading
    ADD INDEX idx_game_sale_time (is_deleted, sale_status, create_time DESC, item_id DESC),
    ADD INDEX idx_game_sale_price (is_deleted, sale_status, price ASC, item_id ASC),
    ALGORITHM=INPLACE, LOCK=NONE;
