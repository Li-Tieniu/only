# Isolated MySQL query benchmark

Run: 2026-11-18T09:03:41.056069200Z

```text
DATABASE()	VERSION()	@@innodb_buffer_pool_size	@@transaction_isolation
only_perf_test_v6	8.0.46	134217728	REPEATABLE-READ
```

JDBC wall time includes result retrieval; 2 warmups + 7 measured runs, single client, no concurrent writers.

## 10000 / baseline

### time_first_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC LIMIT 20;
```
ms: median=10.518 min=10.012 max=11.091 samples=[10.9453, 10.5176, 11.0914, 10.9673, 10.4712, 10.1733, 10.0119]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=8.89..8.89 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=8.89..8.89 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0337..6.85 rows=6500 loops=1)

```

### time_first_stable
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=10.138 min=9.820 max=10.511 samples=[10.3549, 9.9662, 10.1377, 10.0292, 10.511, 9.8199, 10.3374]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=9.24..9.24 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=9.24..9.24 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0344..7 rows=6500 loops=1)

```

### price_asc_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id DESC LIMIT 20;
```
ms: median=10.498 min=10.055 max=13.270 samples=[10.4928, 10.6898, 10.4981, 10.9937, 10.0554, 10.3301, 13.2695]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=10.1..10.1 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=10.1..10.1 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0361..7.37 rows=6500 loops=1)

```

### price_asc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=10.339 min=10.228 max=11.394 samples=[10.4061, 11.3936, 10.2282, 10.3251, 10.3392, 10.2424, 10.3678]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=9.33..9.34 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=9.33..9.34 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0254..6.72 rows=6500 loops=1)

```

### price_desc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price DESC,item_id DESC LIMIT 20;
```
ms: median=10.390 min=10.003 max=10.779 samples=[10.5914, 10.7785, 10.6637, 10.39, 10.2268, 10.1646, 10.0027]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=9.27..9.27 rows=20 loops=1)
    -> Sort: game_trading.price DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=9.26..9.27 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0252..6.68 rows=6500 loops=1)

```

### price_range
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=7.910 min=7.763 max=8.482 samples=[7.9103, 7.7967, 7.7632, 7.8419, 8.1883, 8.0353, 8.4817]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=241 rows=20) (actual time=6.98..6.98 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=241 rows=6500) (actual time=6.98..6.98 rows=20 loops=1)
        -> Filter: (game_trading.price between 100 and 200)  (cost=241 rows=6500) (actual time=0.028..6.79 rows=648 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0258..6.28 rows=6500 loops=1)

```

### keyword
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=7.963 min=7.215 max=8.689 samples=[7.9631, 7.3668, 7.2149, 7.3799, 8.4479, 8.3452, 8.6892]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=241 rows=20) (actual time=7.47..7.48 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=241 rows=6500) (actual time=7.47..7.47 rows=20 loops=1)
        -> Filter: (game_trading.game_name like '%006%')  (cost=241 rows=6500) (actual time=0.0269..7.42 rows=100 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.024..6.83 rows=6500 loops=1)

```

### keyword_absent
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%absent%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=7.506 min=7.125 max=8.025 samples=[7.1254, 7.4371, 7.3686, 7.5059, 7.7826, 7.6562, 8.0252]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=241 rows=20) (actual time=7.08..7.08 rows=0 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=241 rows=6500) (actual time=7.08..7.08 rows=0 loops=1)
        -> Filter: (game_trading.game_name like '%absent%')  (cost=241 rows=6500) (actual time=7.07..7.07 rows=0 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0248..6.55 rows=6500 loops=1)

```

### count_range
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200;
```
ms: median=6.325 min=5.814 max=6.646 samples=[6.3254, 5.8142, 6.6459, 5.8998, 6.6015, 6.4328, 6.2684]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=313 rows=1) (actual time=6.41..6.41 rows=1 loops=1)
    -> Filter: (game_trading.price between 100 and 200)  (cost=241 rows=722) (actual time=0.0336..6.37 rows=648 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0307..5.83 rows=6500 loops=1)

```

### count_live
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE';
```
ms: median=1.769 min=1.452 max=2.217 samples=[1.5417, 2.2169, 1.769, 2.0214, 1.4829, 1.8637, 1.4517]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=1304 rows=1) (actual time=1.61..1.61 rows=1 loops=1)
    -> Covering index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=654 rows=6500) (actual time=0.0147..1.4 rows=6500 loops=1)

```

### count_keyword
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%';
```
ms: median=6.318 min=5.880 max=6.647 samples=[6.0459, 5.8803, 6.3179, 6.0894, 6.3393, 6.5347, 6.6474]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=313 rows=1) (actual time=5.95..5.95 rows=1 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=241 rows=722) (actual time=0.0259..5.94 rows=100 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0223..5.37 rows=6500 loops=1)

```

### admin_time
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=13.748 min=13.489 max=14.041 samples=[13.7478, 13.4894, 13.8011, 13.6343, 14.035, 13.6735, 14.0405]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	1	const	4924	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=661 rows=20) (actual time=13.4..13.4 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=661 rows=4924) (actual time=13.4..13.4 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=661 rows=4924) (actual time=0.0246..10.5 rows=9500 loops=1)

```

### admin_price
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=13.706 min=12.850 max=14.430 samples=[14.4299, 12.8503, 13.8153, 14.2296, 13.1057, 13.6793, 13.706]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	1	const	4924	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=661 rows=20) (actual time=13.2..13.2 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=661 rows=4924) (actual time=13.2..13.2 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=661 rows=4924) (actual time=0.0215..9.89 rows=9500 loops=1)

```

### time_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20 OFFSET 5000;
```
ms: median=12.792 min=12.045 max=13.739 samples=[12.7924, 12.4331, 12.5341, 12.0452, 13.7389, 12.8946, 12.9293]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit/Offset: 20/5000 row(s)  (cost=819 rows=20) (actual time=11.6..11.6 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 5020 row(s) per chunk  (cost=819 rows=6500) (actual time=10.9..11.4 rows=5020 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0245..6.79 rows=6500 loops=1)

```

### id_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY item_id DESC LIMIT 20 OFFSET 5000;
```
ms: median=6.200 min=5.689 max=7.051 samples=[6.1999, 6.5391, 5.8856, 5.6887, 7.0513, 6.421, 5.7866]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	6500	100.0	Backward index scan
EXPLAIN
-> Limit/Offset: 20/5000 row(s)  (cost=819 rows=20) (actual time=5.64..5.66 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=819 rows=6500) (actual time=0.0188..5.53 rows=5020 loops=1)

```

### id_cursor_same_page
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<2306 ORDER BY item_id DESC LIMIT 20;
```
ms: median=0.394 min=0.303 max=0.549 samples=[0.5488, 0.4295, 0.3943, 0.3744, 0.3033, 0.504, 0.3293]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status	PRIMARY	8	null	4536	16.67	Using where; Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=909 rows=20) (actual time=0.0143..0.0346 rows=20 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 2306))  (cost=909 rows=756) (actual time=0.014..0.0323 rows=20 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 2306) (reverse)  (cost=909 rows=4536) (actual time=0.0124..0.0269 rows=31 loops=1)

```

### id_cursor_fetch_extra
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<2306 ORDER BY item_id DESC LIMIT 21;
```
ms: median=0.385 min=0.332 max=0.462 samples=[0.409, 0.4617, 0.4099, 0.3851, 0.357, 0.3831, 0.3318]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status	PRIMARY	8	null	4536	16.67	Using where; Backward index scan
EXPLAIN
-> Limit: 21 row(s)  (cost=909 rows=21) (actual time=0.0281..0.0603 rows=21 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 2306))  (cost=909 rows=756) (actual time=0.0277..0.0584 rows=21 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 2306) (reverse)  (cost=909 rows=4536) (actual time=0.0251..0.0501 rows=32 loops=1)

```

### user_orders
```sql
SELECT * FROM game_order WHERE user_id=7 ORDER BY id DESC LIMIT 20;
```
ms: median=0.637 min=0.534 max=4.014 samples=[0.7758, 0.6367, 0.5343, 0.5533, 0.5344, 4.0139, 0.9199]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	2000	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=273 rows=20) (actual time=0.251..0.257 rows=20 loops=1)
    -> Index lookup on game_order using idx_game_order_user_id (user_id=7) (reverse)  (cost=273 rows=2000) (actual time=0.25..0.255 rows=20 loops=1)

```

### count_user_orders
```sql
SELECT COUNT(*) FROM game_order WHERE user_id=7;
```
ms: median=0.650 min=0.516 max=0.795 samples=[0.6504, 0.6827, 0.7952, 0.5436, 0.5157, 0.6776, 0.6335]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	2000	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=401 rows=1) (actual time=0.506..0.506 rows=1 loops=1)
    -> Covering index lookup on game_order using idx_game_order_user_id (user_id=7)  (cost=201 rows=2000) (actual time=0.0338..0.431 rows=2000 loops=1)

```

### order_detail
```sql
SELECT * FROM game_order WHERE order_no='PERF0000000100';
```
ms: median=0.224 min=0.184 max=0.425 samples=[0.2238, 0.279, 0.2533, 0.196, 0.188, 0.1836, 0.4253]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	const	uk_game_order_order_no	uk_game_order_order_no	130	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=100e-6..100e-6 rows=1 loops=1)

```

### product_detail
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND item_id=9991;
```
ms: median=0.254 min=0.213 max=0.308 samples=[0.3083, 0.2737, 0.2276, 0.2673, 0.2419, 0.2126, 0.2539]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	const	PRIMARY,idx_game_sale_status	PRIMARY	8	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=100e-6..100e-6 rows=1 loops=1)

```

### name_suggestions
```sql
SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 AND game_name LIKE '%007%' ORDER BY game_name LIMIT 20;
```
ms: median=8.250 min=8.020 max=9.254 samples=[9.2538, 8.0196, 8.387, 8.1139, 8.2495, 8.6715, 8.0995]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_name,idx_game_sale_status	idx_game_sale_status	1	const	4924	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=716 rows=20) (actual time=8.77..8.77 rows=1 loops=1)
    -> Group (no aggregates)  (cost=716 rows=100) (actual time=8.77..8.77 rows=1 loops=1)
        -> Sort: game_trading.game_name  (cost=223 rows=4924) (actual time=8.76..8.76 rows=100 loops=1)
            -> Filter: (game_trading.game_name like '%007%')  (cost=223 rows=4924) (actual time=6.04..8.68 rows=100 loops=1)
                -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=223 rows=4924) (actual time=0.0305..7.89 rows=9500 loops=1)

```

## 10000 / candidate

### time_first_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC LIMIT 20;
```
ms: median=0.429 min=0.369 max=0.554 samples=[0.3689, 0.4802, 0.5023, 0.5541, 0.4288, 0.3771, 0.3756]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	6500	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=0.0209..0.0434 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0204..0.042 rows=20 loops=1)

```

### time_first_stable
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=0.369 min=0.338 max=0.530 samples=[0.5301, 0.3694, 0.436, 0.3621, 0.3377, 0.402, 0.3659]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	6500	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=0.0203..0.0421 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.02..0.0407 rows=20 loops=1)

```

### price_asc_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id DESC LIMIT 20;
```
ms: median=10.433 min=9.857 max=10.889 samples=[10.4332, 10.5752, 10.8885, 10.5508, 9.8573, 9.9278, 10.3768]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	6500	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=9.49..9.49 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=819 rows=6500) (actual time=9.49..9.49 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0265..7.01 rows=6500 loops=1)

```

### price_asc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=0.339 min=0.298 max=0.386 samples=[0.2983, 0.3394, 0.3224, 0.3856, 0.3442, 0.3515, 0.3097]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	2	const,const	6500	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=0.0177..0.0389 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_price (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.0175..0.0382 rows=20 loops=1)

```

### price_desc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price DESC,item_id DESC LIMIT 20;
```
ms: median=0.381 min=0.284 max=0.440 samples=[0.3979, 0.284, 0.3813, 0.3487, 0.3422, 0.44, 0.4151]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	2	const,const	6500	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=819 rows=20) (actual time=0.0276..0.0618 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_price (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=819 rows=6500) (actual time=0.0271..0.0601 rows=20 loops=1)

```

### price_range
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=0.326 min=0.290 max=0.365 samples=[0.3344, 0.3263, 0.3034, 0.2897, 0.3654, 0.3444, 0.3124]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	7	null	648	100.0	Using index condition
EXPLAIN
-> Limit: 20 row(s)  (cost=292 rows=20) (actual time=0.0456..0.0795 rows=20 loops=1)
    -> Index range scan on game_trading using idx_game_sale_price over (is_deleted = 0 AND sale_status = 'ON_SALE' AND 100.00 <= price <= 200.00), with index condition: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.price between 100 and 200))  (cost=292 rows=648) (actual time=0.0446..0.0773 rows=20 loops=1)

```

### keyword
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=2.006 min=1.806 max=2.708 samples=[2.3055, 2.0201, 1.8056, 1.8097, 1.9037, 2.0061, 2.708]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	6500	11.11	Using where
EXPLAIN
-> Limit: 20 row(s)  (cost=241 rows=20) (actual time=0.138..1.69 rows=20 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=241 rows=722) (actual time=0.137..1.69 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0349..1.57 rows=1295 loops=1)

```

### keyword_absent
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%absent%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=7.980 min=7.104 max=8.531 samples=[8.3943, 7.9804, 8.1312, 8.5313, 7.6579, 7.5135, 7.1038]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	6500	11.11	Using where
EXPLAIN
-> Limit: 20 row(s)  (cost=241 rows=20) (actual time=7.16..7.16 rows=0 loops=1)
    -> Filter: (game_trading.game_name like '%absent%')  (cost=241 rows=722) (actual time=7.16..7.16 rows=0 loops=1)
        -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0169..6.65 rows=6500 loops=1)

```

### count_range
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200;
```
ms: median=0.555 min=0.472 max=0.721 samples=[0.5456, 0.7207, 0.6358, 0.6692, 0.4812, 0.5551, 0.4715]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	7	null	648	100.0	Using where; Using index
EXPLAIN
-> Aggregate: count(0)  (cost=197 rows=1) (actual time=0.472..0.472 rows=1 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.price between 100 and 200))  (cost=132 rows=648) (actual time=0.019..0.443 rows=648 loops=1)
        -> Covering index range scan on game_trading using idx_game_sale_price over (is_deleted = 0 AND sale_status = 'ON_SALE' AND 100.00 <= price <= 200.00)  (cost=132 rows=648) (actual time=0.0161..0.268 rows=648 loops=1)

```

### count_live
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE';
```
ms: median=1.716 min=1.426 max=2.232 samples=[1.4472, 2.2322, 1.7163, 1.8633, 1.4256, 1.5168, 2.0123]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	6500	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=1304 rows=1) (actual time=1.44..1.44 rows=1 loops=1)
    -> Covering index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=654 rows=6500) (actual time=0.0126..1.26 rows=6500 loops=1)

```

### count_keyword
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%';
```
ms: median=5.941 min=5.566 max=6.212 samples=[5.9974, 6.0363, 5.9412, 5.7907, 5.8248, 6.2116, 5.566]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	6500	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=313 rows=1) (actual time=5.69..5.69 rows=1 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=241 rows=722) (actual time=0.028..5.69 rows=100 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=241 rows=6500) (actual time=0.0242..5.14 rows=6500 loops=1)

```

### admin_time
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=12.505 min=12.067 max=13.157 samples=[12.0669, 12.5364, 12.2239, 13.1568, 12.8964, 12.2445, 12.5047]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	4924	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=661 rows=20) (actual time=12.5..12.5 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=661 rows=4924) (actual time=12.5..12.5 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=661 rows=4924) (actual time=0.0246..9.76 rows=9500 loops=1)

```

### admin_price
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=13.157 min=12.695 max=13.706 samples=[13.5803, 12.9974, 12.695, 13.7059, 13.1576, 13.1571, 12.7163]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	4924	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=661 rows=20) (actual time=12.9..12.9 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=661 rows=4924) (actual time=12.9..12.9 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=661 rows=4924) (actual time=0.0244..9.78 rows=9500 loops=1)

```

### time_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20 OFFSET 5000;
```
ms: median=5.663 min=5.268 max=6.093 samples=[6.0931, 6.0044, 5.6627, 5.9856, 5.5041, 5.5356, 5.2679]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	6500	100.0	null
EXPLAIN
-> Limit/Offset: 20/5000 row(s)  (cost=819 rows=20) (actual time=5.6..5.62 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=819 rows=6500) (actual time=0.025..5.51 rows=5020 loops=1)

```

### id_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY item_id DESC LIMIT 20 OFFSET 5000;
```
ms: median=5.569 min=5.091 max=5.994 samples=[5.9935, 5.0907, 5.5685, 5.5101, 5.5835, 5.8928, 5.5504]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	6500	100.0	Backward index scan
EXPLAIN
-> Limit/Offset: 20/5000 row(s)  (cost=819 rows=20) (actual time=5.15..5.17 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=819 rows=6500) (actual time=0.0273..5.04 rows=5020 loops=1)

```

### id_cursor_same_page
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<2306 ORDER BY item_id DESC LIMIT 20;
```
ms: median=0.335 min=0.284 max=0.417 samples=[0.4134, 0.4163, 0.4174, 0.3351, 0.3165, 0.2857, 0.2841]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	null	4536	66.0	Using where; Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=468 rows=20) (actual time=0.0156..0.0347 rows=20 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 2306))  (cost=468 rows=2994) (actual time=0.0154..0.0336 rows=20 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 2306) (reverse)  (cost=468 rows=4536) (actual time=0.0138..0.028 rows=31 loops=1)

```

### id_cursor_fetch_extra
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<2306 ORDER BY item_id DESC LIMIT 21;
```
ms: median=0.255 min=0.246 max=0.291 samples=[0.2855, 0.2906, 0.2608, 0.2523, 0.2552, 0.2463, 0.2468]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	null	4536	66.0	Using where; Backward index scan
EXPLAIN
-> Limit: 21 row(s)  (cost=468 rows=21) (actual time=0.0157..0.0357 rows=21 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 2306))  (cost=468 rows=2994) (actual time=0.0155..0.035 rows=21 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 2306) (reverse)  (cost=468 rows=4536) (actual time=0.0142..0.0299 rows=32 loops=1)

```

### user_orders
```sql
SELECT * FROM game_order WHERE user_id=7 ORDER BY id DESC LIMIT 20;
```
ms: median=0.432 min=0.369 max=0.474 samples=[0.4114, 0.4315, 0.3702, 0.3693, 0.4444, 0.4744, 0.4714]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	2000	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=273 rows=20) (actual time=0.156..0.16 rows=20 loops=1)
    -> Index lookup on game_order using idx_game_order_user_id (user_id=7) (reverse)  (cost=273 rows=2000) (actual time=0.156..0.158 rows=20 loops=1)

```

### count_user_orders
```sql
SELECT COUNT(*) FROM game_order WHERE user_id=7;
```
ms: median=0.406 min=0.400 max=0.429 samples=[0.4294, 0.3999, 0.4078, 0.4114, 0.4004, 0.4014, 0.4059]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	2000	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=401 rows=1) (actual time=0.332..0.332 rows=1 loops=1)
    -> Covering index lookup on game_order using idx_game_order_user_id (user_id=7)  (cost=201 rows=2000) (actual time=0.0205..0.281 rows=2000 loops=1)

```

### order_detail
```sql
SELECT * FROM game_order WHERE order_no='PERF0000000100';
```
ms: median=0.141 min=0.138 max=0.234 samples=[0.2335, 0.1682, 0.1415, 0.1394, 0.1412, 0.1403, 0.1384]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	const	uk_game_order_order_no	uk_game_order_order_no	130	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=100e-6..100e-6 rows=1 loops=1)

```

### product_detail
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND item_id=9991;
```
ms: median=0.184 min=0.156 max=0.642 samples=[0.1843, 0.6424, 0.1836, 0.2168, 0.1636, 0.1575, 0.1562]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	const	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=200e-6..200e-6 rows=1 loops=1)

```

### name_suggestions
```sql
SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 AND game_name LIKE '%007%' ORDER BY game_name LIMIT 20;
```
ms: median=8.624 min=7.935 max=9.084 samples=[9.0839, 8.6237, 8.3806, 8.6813, 8.9006, 8.0813, 7.935]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_name,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	4924	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=716 rows=20) (actual time=8.16..8.16 rows=1 loops=1)
    -> Group (no aggregates)  (cost=716 rows=100) (actual time=8.16..8.16 rows=1 loops=1)
        -> Sort: game_trading.game_name  (cost=223 rows=4924) (actual time=8.15..8.15 rows=100 loops=1)
            -> Filter: (game_trading.game_name like '%007%')  (cost=223 rows=4924) (actual time=5.57..8.08 rows=100 loops=1)
                -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=223 rows=4924) (actual time=0.0163..7.32 rows=9500 loops=1)

```

## 100000 / baseline

### time_first_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC LIMIT 20;
```
ms: median=83.733 min=81.036 max=85.210 samples=[83.9941, 81.0358, 81.7665, 84.4797, 85.2101, 83.7331, 81.3204]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=85.7..85.7 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=85.7..85.7 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.027..74.2 rows=65000 loops=1)

```

### time_first_stable
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=83.576 min=81.094 max=85.234 samples=[85.1876, 85.2343, 84.6685, 81.7122, 83.3119, 81.0938, 83.5755]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=87.1..87.1 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=87.1..87.1 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0369..74.1 rows=65000 loops=1)

```

### price_asc_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id DESC LIMIT 20;
```
ms: median=86.928 min=85.797 max=93.893 samples=[93.8925, 88.5655, 86.3439, 86.738, 88.1312, 85.7966, 86.9275]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=87.6..87.6 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=87.6..87.6 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0254..72.1 rows=65000 loops=1)

```

### price_asc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=86.276 min=84.314 max=87.073 samples=[84.3141, 86.4591, 86.2661, 87.0725, 86.6332, 86.2762, 85.5404]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=86.8..86.8 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=86.8..86.8 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0259..71.8 rows=65000 loops=1)

```

### price_desc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price DESC,item_id DESC LIMIT 20;
```
ms: median=86.845 min=84.164 max=89.308 samples=[88.6029, 85.4756, 84.164, 87.7121, 89.3081, 85.361, 86.8447]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=89.7..89.7 rows=20 loops=1)
    -> Sort: game_trading.price DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=89.7..89.7 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0328..74.2 rows=65000 loops=1)

```

### price_range
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=79.285 min=77.775 max=83.136 samples=[79.6444, 79.2845, 77.8627, 77.7751, 78.6709, 80.899, 83.136]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=2060 rows=20) (actual time=78..78 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=2060 rows=49024) (actual time=78..78 rows=20 loops=1)
        -> Filter: (game_trading.price between 100 and 200)  (cost=2060 rows=49024) (actual time=0.0266..74.9 rows=6500 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0246..69.7 rows=65000 loops=1)

```

### keyword
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=76.572 min=75.745 max=78.800 samples=[75.8124, 75.745, 78.4449, 78.7995, 76.368, 76.5724, 77.2302]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=2060 rows=20) (actual time=76.3..76.3 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=2060 rows=49024) (actual time=76.3..76.3 rows=20 loops=1)
        -> Filter: (game_trading.game_name like '%006%')  (cost=2060 rows=49024) (actual time=0.0306..74.8 rows=1000 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0269..69 rows=65000 loops=1)

```

### keyword_absent
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%absent%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=74.987 min=72.627 max=77.303 samples=[74.4111, 77.3029, 74.2413, 74.9872, 75.4742, 75.3227, 72.6267]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=2060 rows=20) (actual time=73.7..73.7 rows=0 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=2060 rows=49024) (actual time=73.7..73.7 rows=0 loops=1)
        -> Filter: (game_trading.game_name like '%absent%')  (cost=2060 rows=49024) (actual time=73.7..73.7 rows=0 loops=1)
            -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0227..68.5 rows=65000 loops=1)

```

### count_range
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200;
```
ms: median=59.763 min=59.044 max=63.934 samples=[59.1563, 60.6784, 59.0439, 63.9344, 59.5887, 59.7629, 60.9579]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=2604 rows=1) (actual time=65.9..65.9 rows=1 loops=1)
    -> Filter: (game_trading.price between 100 and 200)  (cost=2060 rows=5447) (actual time=0.0305..65.6 rows=6500 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0281..60.4 rows=65000 loops=1)

```

### count_live
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE';
```
ms: median=12.522 min=12.413 max=13.073 samples=[13.0554, 12.6698, 12.5215, 13.0727, 12.4954, 12.4133, 12.4719]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=9832 rows=1) (actual time=14.5..14.5 rows=1 loops=1)
    -> Covering index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=4930 rows=49024) (actual time=0.0119..12.8 rows=65000 loops=1)

```

### count_keyword
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%';
```
ms: median=60.979 min=60.443 max=69.515 samples=[61.2273, 60.4425, 60.7678, 60.4581, 62.2594, 60.979, 69.5151]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=2604 rows=1) (actual time=65.4..65.4 rows=1 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=2060 rows=5447) (actual time=0.0207..65.4 rows=1000 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0181..59.7 rows=65000 loops=1)

```

### admin_time
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=120.005 min=118.670 max=120.784 samples=[120.0045, 120.0326, 119.696, 118.6699, 119.7431, 120.1105, 120.7844]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	1	const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=125..125 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=125..125 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=6417 rows=49024) (actual time=0.0341..107 rows=95000 loops=1)

```

### admin_price
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=124.925 min=121.179 max=128.299 samples=[124.557, 121.1792, 123.299, 124.9247, 127.4867, 128.299, 125.415]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	1	const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=132..132 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=132..132 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=6417 rows=49024) (actual time=0.0354..111 rows=95000 loops=1)

```

### time_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20 OFFSET 50000;
```
ms: median=120.086 min=116.402 max=133.382 samples=[133.3816, 118.4912, 120.0855, 121.4419, 119.7304, 116.4016, 126.8031]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit/Offset: 20/50000 row(s)  (cost=6417 rows=0) (actual time=122..122 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 50020 row(s) per chunk  (cost=6417 rows=49024) (actual time=115..121 rows=50020 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0303..72.1 rows=65000 loops=1)

```

### id_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY item_id DESC LIMIT 20 OFFSET 50000;
```
ms: median=58.129 min=57.713 max=59.194 samples=[58.0099, 57.7134, 58.0698, 59.1944, 58.1829, 58.1293, 58.8479]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status	idx_game_sale_status	2	const,const	49024	100.0	Backward index scan
EXPLAIN
-> Limit/Offset: 20/50000 row(s)  (cost=6417 rows=0) (actual time=60.1..60.2 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=6417 rows=49024) (actual time=0.024..59 rows=50020 loops=1)

```

### id_cursor_same_page
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<23075 ORDER BY item_id DESC LIMIT 20;
```
ms: median=0.228 min=0.217 max=0.254 samples=[0.2537, 0.2501, 0.228, 0.2172, 0.2213, 0.2445, 0.2264]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status	PRIMARY	8	null	48116	33.33	Using where; Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=9642 rows=20) (actual time=0.0156..0.0323 rows=20 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 23075))  (cost=9642 rows=16039) (actual time=0.0151..0.0304 rows=20 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 23075) (reverse)  (cost=9642 rows=48116) (actual time=0.0136..0.0255 rows=30 loops=1)

```

### id_cursor_fetch_extra
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<23075 ORDER BY item_id DESC LIMIT 21;
```
ms: median=0.246 min=0.219 max=0.400 samples=[0.2187, 0.231, 0.2307, 0.2463, 0.3997, 0.3439, 0.2668]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status	PRIMARY	8	null	48116	33.33	Using where; Backward index scan
EXPLAIN
-> Limit: 21 row(s)  (cost=9642 rows=21) (actual time=0.0151..0.0335 rows=21 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 23075))  (cost=9642 rows=16039) (actual time=0.015..0.0325 rows=21 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 23075) (reverse)  (cost=9642 rows=48116) (actual time=0.0134..0.0269 rows=31 loops=1)

```

### user_orders
```sql
SELECT * FROM game_order WHERE user_id=7 ORDER BY id DESC LIMIT 20;
```
ms: median=0.424 min=0.401 max=0.615 samples=[0.424, 0.4154, 0.5424, 0.4521, 0.6146, 0.4028, 0.4007]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	39674	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=4376 rows=20) (actual time=0.241..0.247 rows=20 loops=1)
    -> Index lookup on game_order using idx_game_order_user_id (user_id=7) (reverse)  (cost=4376 rows=39674) (actual time=0.241..0.246 rows=20 loops=1)

```

### count_user_orders
```sql
SELECT COUNT(*) FROM game_order WHERE user_id=7;
```
ms: median=2.839 min=2.743 max=3.174 samples=[2.7426, 2.817, 2.8387, 2.8159, 2.8581, 3.1739, 2.8834]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	39674	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=7954 rows=1) (actual time=4.11..4.11 rows=1 loops=1)
    -> Covering index lookup on game_order using idx_game_order_user_id (user_id=7)  (cost=3987 rows=39674) (actual time=0.0672..3.53 rows=20000 loops=1)

```

### order_detail
```sql
SELECT * FROM game_order WHERE order_no='PERF0000000100';
```
ms: median=0.203 min=0.156 max=0.352 samples=[0.226, 0.3522, 0.2057, 0.1884, 0.1563, 0.1769, 0.2031]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	const	uk_game_order_order_no	uk_game_order_order_no	130	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=0..0 rows=1 loops=1)

```

### product_detail
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND item_id=9991;
```
ms: median=0.149 min=0.144 max=0.155 samples=[0.1551, 0.1436, 0.1488, 0.1511, 0.1495, 0.1482, 0.1462]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	const	PRIMARY,idx_game_sale_status	PRIMARY	8	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=100e-6..100e-6 rows=1 loops=1)

```

### name_suggestions
```sql
SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 AND game_name LIKE '%007%' ORDER BY game_name LIMIT 20;
```
ms: median=92.973 min=91.868 max=94.957 samples=[92.3389, 91.8679, 93.2497, 93.0583, 92.7553, 94.9571, 92.9733]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_name,idx_game_sale_status	idx_game_sale_status	1	const	49024	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6962 rows=20) (actual time=95..95 rows=1 loops=1)
    -> Group (no aggregates)  (cost=6962 rows=101) (actual time=95..95 rows=1 loops=1)
        -> Sort: game_trading.game_name  (cost=2060 rows=49024) (actual time=94.9..94.9 rows=1000 loops=1)
            -> Filter: (game_trading.game_name like '%007%')  (cost=2060 rows=49024) (actual time=62.2..92.3 rows=1000 loops=1)
                -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=2060 rows=49024) (actual time=0.0317..84.7 rows=95000 loops=1)

```

## 100000 / candidate

### time_first_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC LIMIT 20;
```
ms: median=0.296 min=0.274 max=0.342 samples=[0.2964, 0.2857, 0.3018, 0.2738, 0.3419, 0.3095, 0.2784]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	49024	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=0.0263..0.0491 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0257..0.0468 rows=20 loops=1)

```

### time_first_stable
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=0.245 min=0.229 max=0.346 samples=[0.2775, 0.2566, 0.2449, 0.3455, 0.239, 0.2294, 0.2359]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	49024	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=0.0178..0.0391 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0176..0.0377 rows=20 loops=1)

```

### price_asc_original
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id DESC LIMIT 20;
```
ms: median=88.962 min=86.536 max=91.417 samples=[87.4506, 86.5359, 87.4606, 88.9618, 90.4356, 91.4171, 89.8329]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=94.6..94.6 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=94.6..94.6 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0281..77.9 rows=65000 loops=1)

```

### price_asc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=0.272 min=0.265 max=0.291 samples=[0.2654, 0.2675, 0.2707, 0.2721, 0.2749, 0.2908, 0.2803]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	2	const,const	49024	100.0	null
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=0.0173..0.0402 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_price (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0171..0.039 rows=20 loops=1)

```

### price_desc
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY price DESC,item_id DESC LIMIT 20;
```
ms: median=0.278 min=0.265 max=0.393 samples=[0.2775, 0.2656, 0.2955, 0.2648, 0.276, 0.2936, 0.3933]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	2	const,const	49024	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=0.0195..0.0441 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_price (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=6417 rows=49024) (actual time=0.0192..0.0428 rows=20 loops=1)

```

### price_range
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=0.330 min=0.272 max=0.487 samples=[0.4867, 0.3303, 0.3476, 0.3278, 0.3151, 0.2717, 0.3962]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	7	null	6500	100.0	Using index condition
EXPLAIN
-> Limit: 20 row(s)  (cost=2925 rows=20) (actual time=0.0278..0.0622 rows=20 loops=1)
    -> Index range scan on game_trading using idx_game_sale_price over (is_deleted = 0 AND sale_status = 'ON_SALE' AND 100.00 <= price <= 200.00), with index condition: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.price between 100 and 200))  (cost=2925 rows=6500) (actual time=0.0272..0.0603 rows=20 loops=1)

```

### keyword
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=1.848 min=1.632 max=2.351 samples=[2.3512, 2.1085, 2.167, 1.848, 1.6654, 1.6501, 1.6323]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	49024	11.11	Using where
EXPLAIN
-> Limit: 20 row(s)  (cost=2060 rows=20) (actual time=0.125..2.14 rows=20 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=2060 rows=5447) (actual time=0.124..2.14 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0258..1.98 rows=1295 loops=1)

```

### keyword_absent
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%absent%' ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=84.921 min=79.478 max=87.010 samples=[84.9208, 87.01, 86.6973, 86.2635, 80.5732, 79.9781, 79.4784]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	49024	11.11	Using where
EXPLAIN
-> Limit: 20 row(s)  (cost=2060 rows=20) (actual time=80.3..80.3 rows=0 loops=1)
    -> Filter: (game_trading.game_name like '%absent%')  (cost=2060 rows=5447) (actual time=80.3..80.3 rows=0 loops=1)
        -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0307..75.1 rows=65000 loops=1)

```

### count_range
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND price BETWEEN 100 AND 200;
```
ms: median=2.802 min=2.742 max=3.182 samples=[2.7839, 3.1817, 2.8019, 3.1626, 2.7416, 2.7467, 2.8097]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_price	7	null	6500	100.0	Using where; Using index
EXPLAIN
-> Aggregate: count(0)  (cost=1969 rows=1) (actual time=3.04..3.04 rows=1 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.price between 100 and 200))  (cost=1319 rows=6500) (actual time=0.0163..2.85 rows=6500 loops=1)
        -> Covering index range scan on game_trading using idx_game_sale_price over (is_deleted = 0 AND sale_status = 'ON_SALE' AND 100.00 <= price <= 200.00)  (cost=1319 rows=6500) (actual time=0.0144..1.71 rows=6500 loops=1)

```

### count_live
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE';
```
ms: median=12.644 min=12.446 max=12.801 samples=[12.701, 12.6444, 12.4464, 12.529, 12.8011, 12.676, 12.5529]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	49024	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=9832 rows=1) (actual time=14.5..14.5 rows=1 loops=1)
    -> Covering index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=4930 rows=49024) (actual time=0.0114..12.8 rows=65000 loops=1)

```

### count_keyword
```sql
SELECT COUNT(*) FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND game_name LIKE '%006%';
```
ms: median=65.928 min=63.923 max=68.016 samples=[63.9231, 65.4703, 66.9928, 66.6895, 68.0162, 65.928, 65.0977]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	49024	11.11	Using where
EXPLAIN
-> Aggregate: count(0)  (cost=2604 rows=1) (actual time=69.4..69.4 rows=1 loops=1)
    -> Filter: (game_trading.game_name like '%006%')  (cost=2060 rows=5447) (actual time=0.0254..69.4 rows=1000 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE')  (cost=2060 rows=49024) (actual time=0.0227..63.6 rows=65000 loops=1)

```

### admin_time
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY create_time DESC,item_id DESC LIMIT 20;
```
ms: median=122.133 min=118.616 max=131.499 samples=[125.773, 131.4987, 126.9821, 118.6163, 119.7029, 122.1334, 119.1135]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=124..124 rows=20 loops=1)
    -> Sort: game_trading.create_time DESC, game_trading.item_id DESC, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=124..124 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=6417 rows=49024) (actual time=0.025..106 rows=95000 loops=1)

```

### admin_price
```sql
SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY price ASC,item_id ASC LIMIT 20;
```
ms: median=126.607 min=123.995 max=133.770 samples=[126.3499, 124.0759, 128.4689, 128.168, 126.6073, 133.7704, 123.9949]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	49024	100.0	Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6417 rows=20) (actual time=128..128 rows=20 loops=1)
    -> Sort: game_trading.price, game_trading.item_id, limit input to 20 row(s) per chunk  (cost=6417 rows=49024) (actual time=128..128 rows=20 loops=1)
        -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=6417 rows=49024) (actual time=0.0233..106 rows=95000 loops=1)

```

### time_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY create_time DESC,item_id DESC LIMIT 20 OFFSET 50000;
```
ms: median=56.518 min=55.068 max=59.260 samples=[55.1321, 55.3241, 55.0682, 57.1278, 56.5175, 57.4443, 59.2603]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_time	2	const,const	49024	100.0	null
EXPLAIN
-> Limit/Offset: 20/50000 row(s)  (cost=6417 rows=0) (actual time=58..58.1 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_time (is_deleted=0, sale_status='ON_SALE')  (cost=6417 rows=49024) (actual time=0.0355..56.9 rows=50020 loops=1)

```

### id_offset_deep
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' ORDER BY item_id DESC LIMIT 20 OFFSET 50000;
```
ms: median=58.577 min=57.054 max=59.613 samples=[59.0712, 58.5768, 57.2206, 58.2195, 57.054, 59.6131, 58.6716]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	2	const,const	49024	100.0	Backward index scan
EXPLAIN
-> Limit/Offset: 20/50000 row(s)  (cost=6417 rows=0) (actual time=60.3..60.3 rows=20 loops=1)
    -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0, sale_status='ON_SALE') (reverse)  (cost=6417 rows=49024) (actual time=0.0255..59.1 rows=50020 loops=1)

```

### id_cursor_same_page
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<23075 ORDER BY item_id DESC LIMIT 20;
```
ms: median=0.240 min=0.234 max=0.251 samples=[0.246, 0.237, 0.2403, 0.2338, 0.2447, 0.2514, 0.238]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	null	48116	50.0	Using where; Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=3921 rows=20) (actual time=0.0237..0.0495 rows=20 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 23075))  (cost=3921 rows=24058) (actual time=0.0232..0.0478 rows=20 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 23075) (reverse)  (cost=3921 rows=48116) (actual time=0.0211..0.0396 rows=30 loops=1)

```

### id_cursor_fetch_extra
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE' AND item_id<23075 ORDER BY item_id DESC LIMIT 21;
```
ms: median=0.232 min=0.231 max=0.276 samples=[0.2599, 0.2757, 0.234, 0.2319, 0.2306, 0.2314, 0.2322]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	range	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	null	48116	50.0	Using where; Backward index scan
EXPLAIN
-> Limit: 21 row(s)  (cost=3921 rows=21) (actual time=0.0157..0.0338 rows=21 loops=1)
    -> Filter: ((game_trading.is_deleted = 0) and (game_trading.sale_status = 'ON_SALE') and (game_trading.item_id < 23075))  (cost=3921 rows=24058) (actual time=0.0154..0.0322 rows=21 loops=1)
        -> Index range scan on game_trading using PRIMARY over (item_id < 23075) (reverse)  (cost=3921 rows=48116) (actual time=0.0142..0.0272 rows=31 loops=1)

```

### user_orders
```sql
SELECT * FROM game_order WHERE user_id=7 ORDER BY id DESC LIMIT 20;
```
ms: median=0.396 min=0.387 max=0.415 samples=[0.3871, 0.3884, 0.4148, 0.3992, 0.3955, 0.3972, 0.3937]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	39674	100.0	Backward index scan
EXPLAIN
-> Limit: 20 row(s)  (cost=4376 rows=20) (actual time=0.226..0.229 rows=20 loops=1)
    -> Index lookup on game_order using idx_game_order_user_id (user_id=7) (reverse)  (cost=4376 rows=39674) (actual time=0.225..0.228 rows=20 loops=1)

```

### count_user_orders
```sql
SELECT COUNT(*) FROM game_order WHERE user_id=7;
```
ms: median=2.907 min=2.722 max=3.841 samples=[2.7839, 3.841, 3.2733, 2.9072, 3.1179, 2.7351, 2.7221]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	ref	idx_game_order_user_id	idx_game_order_user_id	8	const	39674	100.0	Using index
EXPLAIN
-> Aggregate: count(0)  (cost=7954 rows=1) (actual time=3.22..3.22 rows=1 loops=1)
    -> Covering index lookup on game_order using idx_game_order_user_id (user_id=7)  (cost=3987 rows=39674) (actual time=0.0652..2.71 rows=20000 loops=1)

```

### order_detail
```sql
SELECT * FROM game_order WHERE order_no='PERF0000000100';
```
ms: median=0.146 min=0.135 max=0.154 samples=[0.154, 0.1471, 0.1458, 0.1468, 0.1458, 0.139, 0.1351]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_order	null	const	uk_game_order_order_no	uk_game_order_order_no	130	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=200e-6..200e-6 rows=1 loops=1)

```

### product_detail
```sql
SELECT * FROM game_trading WHERE is_deleted=0 AND item_id=9991;
```
ms: median=0.174 min=0.138 max=0.246 samples=[0.1905, 0.2458, 0.1858, 0.1377, 0.1569, 0.1742, 0.1508]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	const	PRIMARY,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	PRIMARY	8	const	1	100.0	null
EXPLAIN
-> Rows fetched before execution  (cost=0..0 rows=1) (actual time=0..0 rows=1 loops=1)

```

### name_suggestions
```sql
SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 AND game_name LIKE '%007%' ORDER BY game_name LIMIT 20;
```
ms: median=95.928 min=93.005 max=97.224 samples=[96.2378, 94.9325, 97.2235, 94.6363, 93.0049, 95.9279, 96.7444]
```text
id	select_type	table	partitions	type	possible_keys	key	key_len	ref	rows	filtered	Extra
1	SIMPLE	game_trading	null	ref	idx_game_name,idx_game_sale_status,idx_game_sale_time,idx_game_sale_price	idx_game_sale_status	1	const	49024	11.11	Using where; Using filesort
EXPLAIN
-> Limit: 20 row(s)  (cost=6962 rows=20) (actual time=95.7..95.7 rows=1 loops=1)
    -> Group (no aggregates)  (cost=6962 rows=101) (actual time=95.7..95.7 rows=1 loops=1)
        -> Sort: game_trading.game_name  (cost=2060 rows=49024) (actual time=95.6..95.7 rows=1000 loops=1)
            -> Filter: (game_trading.game_name like '%007%')  (cost=2060 rows=49024) (actual time=62.8..92.9 rows=1000 loops=1)
                -> Index lookup on game_trading using idx_game_sale_status (is_deleted=0)  (cost=2060 rows=49024) (actual time=0.0237..85.3 rows=95000 loops=1)

```
