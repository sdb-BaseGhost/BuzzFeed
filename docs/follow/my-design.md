# 用户关系服务

## 存储设计
因为要高效支持查询用户的关注列表和粉丝列表，而用户量很多的feed应用需要分库分表，分库分表需要依赖索引键。  
所以采用冗余表设计：Following 表和 Follower 表。两个表的结构完全相同，只是索引不同。

### 一致性问题
冗余表说明两个表的数据需要相同，所以需要建立数据一致性关系。本项目采用binlog监听mysql，
Follower表通过CDC机制同步Following表数据。
监听Following表Binlog，将变更异步写入Follower表。

## 索引设计
Following表建立联合索引：
KEY idx_following_list (from_user_id, type, update_time, to_user_id)  
KEY idx_following (from_user_id, to_user_id, type)

Follower表同理  
KEY idx_follower(to_user_id, from_user_id)  
KEY idx_follower_list (to_user_id, type, update_time, from_user_id)

## 关注数和粉丝数
（后续补充Todo）

## 缓存优化高并发场景
需要缓存什么：
1. 关注列表ZSet
2. 粉丝列表ZSet，区分普通用户和大v
3. 缓存粉丝关系：Redis使用Hash对象缓存数据，Key代表用户1,
   Field代表用户2、用户3、用户4等指定用户，对应的Value表示这些用户是否分别是用
   户1的粉丝，其中值为1表示是粉丝，值为0表示不是粉丝。如果在待查询的这些用户中
   至少有一个用户不在此Hash对象中，则需要进一步为其回源查询数据库Follower表。
   {Key:follow_relation:{targetUserId}
   Field:userId
   Value:1/0}
4. Redis中的关注列表、粉丝列表和关系缓存通过CDC服务监听MySQL Binlog进行异步更新。不采用“先更新数据库，再删除缓存”的方案。

限制用户的最大关注数，如2000，所以可以把关注列表全量缓存到Redis中。  
粉丝列表则不能限制，但是大V的粉丝大部分只会查看前几页，所以粉丝列表可以缓存最近的10000个粉丝。  
对于恶意请求采取限流的方式（后续补充Todo）  

## 数据库表设计
Following

id bigint
from_user_id bigint
to_user_id bigint
type tinyint（关注关系枚举，比如：1正在关注，2取消关注）
create_time datetime
update_time datetime

说明：
from_user_id -> to_user_id

--------------------------------

Follower

id bigint
from_user_id bigint
to_user_id bigint
type tinyint
create_time datetime
update_time datetime

说明：
Follower表与Following表结构一致，仅索引方向不同。

## Redis key规范
1、Following列表

Key:
following:{userId}

Type:
ZSet

Member:
targetUserId

Score:
followTime

2、Follower列表

Key:
followers:{userId}

Type:
ZSet

Member:
followerUserId

Score:
followTime

3、关系缓存

Key:
follow_relation:{targetUserId}

Type:
Hash

Field:
userId

Value:
1/0

## 当前阶段必须完成：

1. 关注用户
2. 取消关注
3. 查询关注列表
4. 查询粉丝列表
5. 查询当前用户与粉丝列表用户的关系
6. 返回关注状态：
   FOLLOWING
   NOT_FOLLOWING
   MUTUAL_FOLLOW

## 当前阶段先不做：
引入CDC同步Follower表实现一致性。

## 具体实现阶段
Phase1

✓ Following表
✓ Follower表
✓ Redis关注列表缓存
✓ Redis粉丝列表缓存
✓ 关注
✓ 取关
✓ 粉丝列表
✓ 关注列表
✓ 查询关系状态
✓ 前端ui：关注取关选项、粉丝关注列表查询选项

----------------

Phase2

✓ 共同关注
✓ Redis Hash关系缓存
✓ 大V优化

----------------

Phase3

✓ CDC
✓ Binlog
✓ MQ
✓ 缓存异步同步