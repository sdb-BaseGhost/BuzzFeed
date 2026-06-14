# Timeline Feed服务

## 推模式
发布时直接写入用户的收件箱，当大v发布时会有写扩散问题和存储压力

## 拉模式
发布时只写入发件箱，用户获取时访问发件箱。大v内容读取的会有读压力。

## 推拉结合模式
思路：推拉模式结合的关键点在于一条内容的发布者有多少粉丝：
- 当非大v用户发布内容时，直接推送到用户的收件箱即可。
- 大v用户发布时区分活跃用户，活跃用户采用推模式，非活跃用户采用拉模式。
- 当用户获取Timeline Feed时，先检查关注列表有没有大v，有大v仅

## 技术细节
### 1、内容与用户收件箱的交互
将内容发布服务与 Timeline Feed服务解耦，当审核通过时发送消息给Timeline消费者，他负责分发子任务给推送消费者，将1000个用户作为推送单位。

分发器流程如下：
1. 分发器订阅主题为event_content_meta_change的事件。
2. 获取该内容的粉丝列表。
3. 假设是3000粉丝，id为1-3000，以1000为单位拆分为3个子任务，消息内容包括内容id、发布时间、任务编号、目标粉丝的用户id。

消费者流程如下：  
1. 订阅事件
2. 检查是否发布成功
3. 获取内容id和这一批的目标粉丝
4. 进行推拉模式的判断，符合条件的用户加入待推送列表。
5. 进行收件箱的插入，失败抛异常。

### 2、收件箱存什么数据
不存内容本身，存内容id和发布时间即可。内容ID用于唯一标识一条内容，发布时间用于Timeline排序

### 3、获取Timeline Feed流的请求
有两种获取方式：下拉和上滑。

1. 操作类型：是下拉操作还是上滑操作
2. 时间戳：目前Feed流最后一条内容的发布时间，服务上滑操作
3. 内容数量：一次feed流返回多少条数据
4. 最后内容ID：上滑操作需要已经展示在Feed流中的最后一条内容的内容ID。

### 4、使用数据库实现收件箱
DDL如下：
CREATE TABLE `inbox` (
`id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增主键，无特殊含义',
`user_id` bigint NOT NULL COMMENT '用户id',
`content_id` bigint NOT NULL COMMENT '内容id',
`publish_time` date NOT NULL COMMENT '内容发布时间',
PRIMARY KEY (`id`),
KEY `idx_user_publish_content` (`user_id`,`publish_time`,`content_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1740019 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

下拉sql示例：
SELECT content_id, publish_time FROM inbox WHERE user_id = 111 ORDER BY
publish_time DESC LIMIT N

上滑sql示例：
SELECT content_id, publish_time FROM inbox WHERE user_id = 111 AND
(publish_time < ts OR (publish_time = ts AND content_id < last_content_id))
ORDER BY publish__time DESC LIMIT N

### 5、使用Redis ZSET实现收发件箱
key为outbox/inbox_{userId}，member为contentID， score为发布时间戳

参考数据库的联合索引方式，Redis上滑获取，当score相同时，按照member值从小到大排序。
但是member值是按字典序，所以需要往member值前面补0，规定member长度为20，不足往前面补零，这样就能达到和数据库联合索引的效果了。


### 6、用户读取侧如何体现推拉结合模式
工作流程：
1. 拉取用户关注列表
2. 从收件箱拉取N条，并从中获取内容发布者的ID
3. 计算在用户的关注列表中，但并不属于收件箱中内容发布者的用户，拉取内容
4. 从M个关注者中分别拉取N条
5. 对这（M+1）*N 个内容进行合并操作，保证：
    内容按照发布时间从近到远排序
    发布时间相同的按照内容ID从大到小排序
6. 合并排序这些内容，得到前N条展示Feed，思路同力扣合并k个有序链表

### 7、收尾工作
根据此次刷新Feed流应该展示的内容ID列表，需要：
◎从内容发布服务中获取这些内容的原文，包括文本、图片、视频等；
◎从计数服务中获取这些内容的评论数、点赞数、转发数、收藏数等计数信息；（扩展功能，先不做）
◎从用户服务中获取这些内容的发布者的头像、昵称等用户相关信息；