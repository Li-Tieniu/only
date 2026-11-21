-- 验收只读脚本：先选定目标库。可在业务库执行，不改变任何数据或结构。
SELECT DATABASE() AS current_database, VERSION() AS mysql_version;
SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('sys_user','game_trading','game_order');
SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('sys_user','game_trading','game_order')
ORDER BY TABLE_NAME,ORDINAL_POSITION;
SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME,COLLATION
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('sys_user','game_trading','game_order')
ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;

SELECT item_id,COUNT(*) AS active_count FROM game_order WHERE status IN ('PENDING','PAID')
GROUP BY item_id HAVING COUNT(*)>1;
SELECT o.id,o.item_id,o.status FROM game_order o LEFT JOIN game_trading g ON o.item_id=g.item_id
WHERE o.status IN ('PENDING','PAID') AND (g.item_id IS NULL OR g.is_deleted<>0);
SELECT id,status,pay_time FROM game_order
WHERE status NOT IN ('PENDING','PAID','CANCELLED')
   OR (status='PAID' AND pay_time IS NULL)
   OR (status IN ('PENDING','CANCELLED') AND pay_time IS NOT NULL);

-- 以下语句仅在上面的列检查确认 sale_status 已存在后执行；缺列表示 V5 尚未完成。
-- CANCELLED 不计入有效占用：取消后的商品可能已被另一个订单占用或售出。
SELECT g.item_id,g.sale_status,COALESCE(o.pending_count,0) AS pending_count,
       COALESCE(o.paid_count,0) AS paid_count
FROM game_trading g LEFT JOIN (
    SELECT item_id,SUM(status='PENDING') AS pending_count,SUM(status='PAID') AS paid_count
    FROM game_order GROUP BY item_id
) o ON o.item_id=g.item_id
WHERE (g.sale_status='ON_SALE' AND COALESCE(o.pending_count,0)+COALESCE(o.paid_count,0)<>0)
   OR (g.sale_status='LOCKED' AND (COALESCE(o.pending_count,0)<>1 OR COALESCE(o.paid_count,0)<>0))
   OR (g.sale_status='SOLD' AND (COALESCE(o.paid_count,0)<>1 OR COALESCE(o.pending_count,0)<>0))
   OR g.sale_status NOT IN ('ON_SALE','LOCKED','SOLD');
