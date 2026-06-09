ALTER TABLE inbox MODIFY COLUMN publish_time datetime(3) NOT NULL COMMENT '内容发布时间';

-- ============================================================
-- 为 testC 用户插入 10 条测试内容 + 对应 inbox 记录
-- 使用方式：直接在 MySQL 客户端执行，或 mysql -u root -p1234 buzzfeed < this.sql
-- ============================================================

-- 1. 查出 testC 的 user_id（假设 username = 'testC'）
--    如果你的用户名不同，请替换下面的 'testC'
SET @creator_id = (SELECT user_id FROM user WHERE username = 'testC' LIMIT 1);

-- 2. 查出当前最大 item_id，从它 +1 开始
SET @start_id = (SELECT IFNULL(MAX(item_id), 0) + 1 FROM item_info);

-- 3. 插入 10 条内容到 item_info（纯文本，状态=1正常，间隔 2 分钟）
INSERT INTO item_info (item_id, creator_id, item_type, title, summary, visibility, status, create_time, publish_time, update_time)
VALUES
(@start_id,     @creator_id, 0, '第1条：今天天气真好',       '阳光明媚，适合出门散步晒太阳。', 3, 1, NOW(3) - INTERVAL 18 MINUTE, NOW(3) - INTERVAL 18 MINUTE, NOW(3) - INTERVAL 18 MINUTE),
(@start_id + 1, @creator_id, 0, '第2条：学 Vue 3 心得',      'Composition API 写起来真爽，ref 和 reactive 各有场景。', 3, 1, NOW(3) - INTERVAL 16 MINUTE, NOW(3) - INTERVAL 16 MINUTE, NOW(3) - INTERVAL 16 MINUTE),
(@start_id + 2, @creator_id, 0, '第3条：Spring Boot 小技巧',  '用 @Transactional 时注意异常类型，RuntimeException 才会回滚。', 3, 1, NOW(3) - INTERVAL 14 MINUTE, NOW(3) - INTERVAL 14 MINUTE, NOW(3) - INTERVAL 14 MINUTE),
(@start_id + 3, @creator_id, 0, '第4条：MySQL 索引优化',      '联合索引遵循最左前缀原则，查询条件顺序要和索引一致。', 3, 1, NOW(3) - INTERVAL 12 MINUTE, NOW(3) - INTERVAL 12 MINUTE, NOW(3) - INTERVAL 12 MINUTE),
(@start_id + 4, @creator_id, 0, '第5条：Redis 缓存穿透',     '空值缓存 + 布隆过滤器，双管齐下解决缓存穿透问题。', 3, 1, NOW(3) - INTERVAL 10 MINUTE, NOW(3) - INTERVAL 10 MINUTE, NOW(3) - INTERVAL 10 MINUTE),
(@start_id + 5, @creator_id, 0, '第6条：Kafka 入门笔记',      'Producer -> Broker -> Consumer，理解分区和消费者组是关键。', 3, 1, NOW(3) - INTERVAL 8 MINUTE, NOW(3) - INTERVAL 8 MINUTE, NOW(3) - INTERVAL 8 MINUTE),
(@start_id + 6, @creator_id, 0, '第7条：TailwindCSS 真香',    'utility-first 的开发体验太棒了，再也不用写一堆 CSS 文件。', 3, 1, NOW(3) - INTERVAL 6 MINUTE, NOW(3) - INTERVAL 6 MINUTE, NOW(3) - INTERVAL 6 MINUTE),
(@start_id + 7, @creator_id, 0, '第8条：Docker 部署实战',     'docker-compose up -d 一键拉起所有服务，开发环境再也不用配半天。', 3, 1, NOW(3) - INTERVAL 4 MINUTE, NOW(3) - INTERVAL 4 MINUTE, NOW(3) - INTERVAL 4 MINUTE),
(@start_id + 8, @creator_id, 0, '第9条：Git 分支管理',        'feature 分支开发，develop 合并，main 只放稳定版本。', 3, 1, NOW(3) - INTERVAL 2 MINUTE, NOW(3) - INTERVAL 2 MINUTE, NOW(3) - INTERVAL 2 MINUTE),
(@start_id + 9, @creator_id, 0, '第10条：周末去哪玩？',       '有没有人推荐北京周边的好去处？想找个安静的地方放松一下。', 3, 1, NOW(3), NOW(3), NOW(3));

-- 4. 为所有关注 testC 的用户推送 inbox（模拟 fan-out）
--    这样这些用户在关注 Tab 就能看到 testC 的新内容
INSERT INTO inbox (user_id, content_id, publish_time)
SELECT
    uf.user_id,
    i.item_id,
    i.publish_time
FROM user_follow uf
CROSS JOIN item_info i
WHERE uf.follow_user_id = @creator_id
  AND i.item_id BETWEEN @start_id AND @start_id + 9;

-- 5. 也为 testC 自己推一份 inbox（自己发的内容自己能看到）
INSERT INTO inbox (user_id, content_id, publish_time)
SELECT
    @creator_id,
    i.item_id,
    i.publish_time
FROM item_info i
WHERE i.item_id BETWEEN @start_id AND @start_id + 9;