-- MySQL 8 增量迁移。先停止 app/console 写入并备份，再在目标库执行一次。
-- 不删除或修改历史订单。存在重复有效订单、孤儿订单或已删除商品的有效订单时中止；
-- 必须人工核对业务数据后重新执行，禁止直接批量取消/删除订单。
DELIMITER $$
CREATE PROCEDURE migrate_game_sale_status()
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()
        AND table_name IN ('game_trading', 'game_order') AND engine = 'InnoDB') <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Both trading and order tables must use InnoDB';
    END IF;
    IF EXISTS (SELECT item_id FROM game_order WHERE status IN ('PENDING', 'PAID')
               GROUP BY item_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Duplicate active orders: reconcile before migration';
    END IF;
    IF EXISTS (SELECT 1 FROM game_order o LEFT JOIN game_trading g ON g.item_id = o.item_id
               WHERE o.status IN ('PENDING', 'PAID') AND (g.item_id IS NULL OR g.is_deleted <> 0)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Active orders reference missing/deleted products';
    END IF;
    -- ALTER 是隐式提交，维护窗口期间禁止旧版本服务写入。
    ALTER TABLE game_trading ADD COLUMN sale_status
        ENUM('ON_SALE', 'LOCKED', 'SOLD') NOT NULL DEFAULT 'ON_SALE' COMMENT '销售状态';
    UPDATE game_trading g JOIN game_order o ON o.item_id = g.item_id
        SET g.sale_status = CASE WHEN o.status = 'PAID' THEN 'SOLD' ELSE 'LOCKED' END
        WHERE o.status IN ('PENDING', 'PAID');
    CREATE INDEX idx_game_sale_status ON game_trading (is_deleted, sale_status, item_id);
END$$
DELIMITER ;
CALL migrate_game_sale_status();
DROP PROCEDURE migrate_game_sale_status;
