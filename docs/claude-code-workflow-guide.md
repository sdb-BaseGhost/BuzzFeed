# Claude Code 需求开发工作流指南（BuzzFeed 项目实战版）

> 本文档将 Anthropic 官方 Skills 最佳实践落地为可操作的工作流，覆盖小功能、大功能、Skills 沉淀三条线。

---

## 〇、你目前的工具链

```
你的武器库:
├── CLAUDE.md              ✅ 项目全局规则（测试规范、代码风格、架构约束）
├── brainstorming skill    ✅ 设计先行（强制提问 → 方案 → 用户批准）
├── writing-plans skill    ✅ 拆解计划（拆成 bite-sized tasks）
├── TDD skill              ✅ 测试先行
├── systematic-debugging   ✅ 根因调试
├── verification skill     ✅ 完成前必须验证
├── request/receive review ✅ 代码审查
├── finishing-branch skill ✅ 合并收尾
├── hooks/session-start    ✅ 自动触发 skill 检测
└── design-first rule      ✅ 设计文档 → 用户批准 → 才能写代码
```

**你目前缺少的：项目特定知识 Skills**（MinIO、Redis、Feed 流等的 gotchas 和验证脚本）

---

## 一、小功能工作流（30 分钟 ~ 2 小时）

**典型场景**：修复登录 UserContext 刷新消失、改个样式、加个字段

### 示例：修复 UserContext 刷新后消失

#### Step 1: 直接描述问题（不走 brainstorming）

小功能不需要完整设计流程，直接告诉 Claude Code：

```bash
# 启动 Claude Code
claude

# 直接说问题
> 刷新页面后 UserContext 里的用户信息消失了。当前 token 存在 
> localStorage，页面刷新时会调 fetchCurrentUser 恢复，但看起来
> 有竞态问题。帮我修复。
```

**关键技巧**：把问题现象 + 你已经知道的线索都告诉它，避免它从零排查。

#### Step 2: Claude 执行 → 你看它怎么改

Claude Code 会：
1. 读 `stores/auth.js`、`interceptor.js`、`JwtInterceptor.java`
2. 分析竞态条件
3. 提出修复方案
4. 修改代码

**你要做的**：看它的分析是否正确，不要直接让它改。如果方向错了及时纠正。

#### Step 3: 验证（必须）

```bash
# 告诉 Claude Code 验证
> 启动前后端，帮我验证：1) 登录后刷新页面，用户信息是否保留 
> 2) token 过期后刷新是否正确跳转登录页
```

或者更高效——**用 Playwright 自动验证**（后面会讲）。

#### Step 4: 提交

```bash
# 用你的 commit command
> /commit-message
```

---

### 小功能的节奏

```
描述问题 → Claude 分析 → 你确认方向 → Claude 修改 → 你验证 → 提交
   ↑                                                    ↓
   └──────── 不对就打断，重新描述 ←──────────────────────┘
```

**耗时估算**：真正花时间的是 Step 1（描述清楚问题）和 Step 3（验证），Claude 执行改代码很快。

---

## 二、大功能工作流（2 天 ~ 1 周）

**典型场景**：计数系统、新的 Feed 流特性、多模块联动功能

### 示例：实现计数系统（阅读量/点赞数/评论数/播放次数）

#### Phase 0: 先想清楚再开口（你自己的准备工作，30 分钟）

在跟 Claude Code 对话之前，先想清楚这几件事：

```
我要做什么？
  - 阅读量：每次打开 PostDetail +1
  - 点赞数：点赞/取消时 +1/-1
  - 评论数：发评论/删评论时 +1/-1  
  - 播放次数：视频播放 +1

数据放哪里？
  - 方案 A：MySQL 字段（简单但高频更新压力大）
  - 方案 B：Redis 计数 + 异步持久化到 MySQL（推荐）
  - 方案 C：Canal 监听 Binlog 自动同步（你已经有 Canal 了）

影响范围？
  - 后端：新增 CounterService、修改 PostService、修改 KafkaConsumer
  - 前端：PostDetail.vue、Feed 卡片、Profile 页面
  - 基础设施：Redis 缓存策略
```

#### Phase 1: 设计（brainstorming skill，1-2 小时）

```bash
> 我想做一个计数系统，统计每个内容的阅读量、点赞数、评论数、
> 播放次数。技术约束：
> 1. 点赞数和评论数要实时准确
> 2. 阅读量允许延迟几秒（最终一致）
> 3. 播放次数允许延迟
> 4. 当前用 MySQL + Redis + Canal，你看看怎么设计
> 5. 参考现有的 feeds 设计里的推拉结合模式
```

Claude Code 会（通过 brainstorming skill）：
1. 读现有代码（FeedServiceImpl、PostServiceImpl 等）
2. 分析技术约束
3. 提出 2-3 种方案
4. 你选一个，它出设计文档
5. **你批准后才能动手**

#### Phase 2: 写计划（writing-plans skill，30 分钟）

```bash
> 基于批准的设计，帮我写实施计划
```

Claude 会生成类似这样的计划：

```markdown
docs/superpowers/plans/2026-06-18-counting-system.md

## Task 1: 数据库 Schema 变更
- 新增 content_counter 表
- 编写 Flyway 迁移脚本
- 验证：运行 migrate，确认表结构正确

## Task 2: Redis 计数器核心
- 新增 CounterService（incr/decr/get）
- Redis Key 设计：counter:{contentId}:{type}
- 编写单元测试
- 验证：运行测试通过

## Task 3: 后端 API 接入
- 修改 PostService（阅读量 incr）
- 修改 LikeService（点赞数 incr/decr）
- 修改 CommentService（评论数 incr/decr）
- 验证：每个修改有对应测试

## Task 4: MySQL 持久化
- 定时任务从 Redis 同步到 MySQL
- 或利用 Canal 监听
- 验证：写入 Redis 后查询 MySQL 确认同步

## Task 5: 前端展示
- Feed 卡片显示阅读量/评论数
- PostDetail 显示完整计数
- 验证：Playwright 验证页面展示
```

#### Phase 3: 执行（subagent-driven-development，1-3 天）

```bash
> 按照计划执行，每个 Task 完成后跑测试
```

Claude Code 会：
- 逐个 Task 派发子 Agent 执行
- 每个 Task 完成后自动跑测试
- 双阶段 Review（spec 合规 + 代码质量）

**你要做的**：
- 不要打断它执行（"Should I continue?" 之类的问话是在浪费你的时间）
- 只在它 BLOCKED 或有歧义时介入
- 每个 Task 完成后快速看一眼改动是否合理

#### Phase 4: 验证（verification-before-completion skill）

```bash
> 所有 Task 完成了，帮我做端到端验证：
> 1. 启动后端，写一个帖子
> 2. 用另一个用户访问，阅读量应该 +1
> 3. 点赞后点赞数 +1，取消后 -1
> 4. 发评论后评论数 +1
> 5. 检查 Redis 里有计数，MySQL 里也同步了
```

#### Phase 5: 收尾

```bash
# 代码审查
> /request-code-review

# 合并
> 完成开发分支的合并流程
```

---

### 大功能的时间分配

```
Phase 0  想清楚需求      ████░░░░░░  10% 时间
Phase 1  设计（brainstorming）██████░░░░  20% 时间
Phase 2  写计划           ███░░░░░░░  10% 时间
Phase 3  执行（Claude主力）██████████  40% 时间（Claude 执行，你审查）
Phase 4  验证             ████░░░░░░  15% 时间
Phase 5  收尾             ██░░░░░░░░   5% 时间
```

**关键原则**：你的时间花在 Phase 0（想清楚）和 Phase 4（验证），而不是 Phase 3（写代码）。

---

## 三、Playwright 自动验证（最高 ROI 的 Skill 投资）

### 为什么要做

Anthropic 原文说：**"产品验证类 skill 是对 Claude 输出质量提升最显著的类别，值得花一周时间专门打磨。"**

手动验证 = 每次改完代码都要人工打开浏览器点一遍。
Playwright 验证 = Claude 改完代码后自动跑一遍，10 秒出结果。

### BuzzFeed 项目的第一批验证脚本

#### 1. 登录流程验证（对应你的 UserContext 问题）

```bash
# tests/e2e/auth-flow.spec.js
```

```javascript
// 这个文件让 Claude Code 来写，你只需要描述场景
const { test, expect } = require('@playwright/test');

test.describe('登录流程', () => {
  
  test('登录后刷新页面，用户信息不丢失', async ({ page }) => {
    // 1. 打开登录页
    await page.goto('http://localhost:5173/login');
    
    // 2. 填写表单并登录
    await page.fill('input[placeholder*="用户名"]', 'testuser');
    await page.fill('input[placeholder*="密码"]', '123456');
    await page.click('button[type="submit"]');
    
    // 3. 等待跳转到首页
    await page.waitForURL('**/home');
    
    // 4. 验证用户信息显示
    await expect(page.locator('.user-avatar')).toBeVisible();
    
    // 5. 刷新页面
    await page.reload();
    
    // 6. 关键验证：刷新后用户信息仍在
    await expect(page.locator('.user-avatar')).toBeVisible();
    await expect(page.locator('.user-name')).toContainText('testuser');
  });

  test('Token 过期后刷新跳转登录页', async ({ page }) => {
    // 清除 localStorage 模拟 token 过期
    await page.goto('http://localhost:5173/home');
    await page.evaluate(() => localStorage.clear());
    await page.reload();
    
    // 应该跳转到登录页
    await page.waitForURL('**/login');
  });
});
```

#### 2. Feed 流验证

```javascript
test.describe('Feed 流', () => {
  
  test('首页加载 Feed 并显示内容卡片', async ({ page }) => {
    await page.goto('http://localhost:5173/home');
    
    // 等待内容加载
    await page.waitForSelector('.feed-card', { timeout: 5000 });
    
    // 验证卡片数量
    const cards = await page.locator('.feed-card').count();
    expect(cards).toBeGreaterThan(0);
    
    // 验证卡片包含必要元素
    const firstCard = page.locator('.feed-card').first();
    await expect(firstCard.locator('.post-title')).toBeVisible();
    await expect(firstCard.locator('.post-images')).toBeVisible();
  });

  test('下拉加载更多', async ({ page }) => {
    await page.goto('http://localhost:5173/home');
    await page.waitForSelector('.feed-card');
    
    const initialCount = await page.locator('.feed-card').count();
    
    // 滚动到底部
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    
    // 等待新内容加载
    await page.waitForTimeout(1000);
    const newCount = await page.locator('.feed-card').count();
    
    expect(newCount).toBeGreaterThan(initialCount);
  });
});
```

#### 3. 发帖流程验证

```javascript
test.describe('发帖', () => {

  test('发布图文帖子', async ({ page }) => {
    // 先登录
    await loginAs(page, 'testuser');
    
    // 进入发帖页
    await page.goto('http://localhost:5173/compose');
    
    // 填写内容
    await page.fill('textarea', '这是一条测试帖子');
    
    // 上传图片
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('tests/fixtures/test-image.jpg');
    
    // 等待上传完成
    await page.waitForSelector('.upload-success', { timeout: 10000 });
    
    // 发布
    await page.click('button:has-text("发布")');
    
    // 验证跳转到帖子详情或首页
    await page.waitForURL(/(home|post\/\d+)/);
  });
});
```

### 如何让 Claude Code 帮你写 Playwright

```bash
# 在 Claude Code 里说：
> 帮我写 Playwright E2E 测试，覆盖以下场景：
> 1. 登录 → 刷新 → 用户信息不丢失
> 2. Token 过期 → 刷新 → 跳转登录页
> 3. Feed 流加载 → 下拉加载更多
> 4. 发帖 → 图片上传 → 发布成功
> 测试文件放在 tests/e2e/ 目录
> 登录函数抽取为 utils/auth-helper.js 复用
```

Claude Code 会读你的前端代码（Login.vue、Home.vue、Compose.vue），生成匹配你实际 DOM 结构的测试。

### 持续使用

```bash
# 每次改完代码后
> 跑一遍 E2E 测试看看有没有回归

# 或者在 CLAUDE.md 里加一条规则
> 完成任何修改后，必须运行 `npx playwright test` 确认 E2E 通过
```

---

## 四、Skills 沉淀（把你踩过的坑变成 Claude 的长期记忆）

### 原则：从 Gotchas 开始

**不要从零写一个"完美 skill"。** 先写 3 行，遇到坑了再加。

### 第一个要沉淀的 Skill：`minio-operations`

```markdown
---
name: minio-operations
description: "Use when working with file upload, download, MinIO client 
  configuration, thumbnail generation, or storage-related debugging. 
  Triggers: upload, file, minio, storage, thumbnail, bucket."
---

# MinIO 操作指南

## Gotchas

- MinIO 连接超时默认 10 秒，大文件上传需要调大到 60 秒
- `MinioConfig.java` 里的 endpoint 不能用 localhost（容器内不通），
  用 `minio:9000`（Docker 网络）
- 缩略图生成失败不要直接报错，记录日志继续上传原图
- 分片上传的 uploadId 需要在异常时清理，否则会残留碎片

## 常用操作

### 上传文件
参考 `MinioService.java`，使用 `uploadFile()` 方法。
注意：文件名用 UUID + 原始后缀，不要用原始文件名（避免重名覆盖）。

### 生成预签名 URL
用于前端展示，有效期不要超过 7 天。
```

### 第二个要沉淀的 Skill：`redis-cache-pattern`

```markdown
---
name: redis-cache-pattern
description: "Use when working with Redis caching, cache invalidation, 
  multi-level cache, or debugging cache-related issues. 
  Triggers: cache, redis, invalidate, TTL, 缓存."
---

# Redis 缓存模式

## Gotchas

- Redis 里的对象必须设置过期时间，否则内存泄漏
- Feed 流用 ZSet，Key 设计：feed:recommend:{userId}，Score 用时间戳
- 推模式的收件箱用 List：inbox:{userId}
- 缓存击穿防护：singleflight 模式（同一个 Key 只放一个请求去查 DB）

## 多级缓存策略

1. 本地缓存（Caffeine，1 分钟 TTL）→ 热点数据
2. Redis 缓存（10 分钟 TTL）→ 分布式共享
3. MySQL → 最终数据源

## 序列化

Java 对象用 JSON 序列化（Jackson），不要用 JDK 序列化。
前端需要的字段用 VO/DTO 转换，不要直接返回 Entity。
```

### 第三个要沉淀的 Skill：`feed-flow-debugging`

```markdown
---
name: feed-flow-debugging
description: "Use when debugging Feed流 issues: missing posts, wrong order, 
  slow loading, Redis cache inconsistency. 
  Triggers: feed, 流, 推送, 收件箱, 发件箱, feed流."
---

# Feed 流调试 Runbook

## 症状 → 工具 → 查询

### 发帖后粉丝收不到
1. 检查 Kafka 消息：`kafka-console-consumer --topic content-events`
2. 检查 Fan-out 服务日志：是否成功推送到 inbox
3. 检查 Redis：`ZCARD inbox:{粉丝ID}` 看数量
4. 检查关注关系：`SELECT * FROM user_follow WHERE user_id = ?`

### Feed 流顺序错乱
1. Redis ZSet 的 score 用的是 content.create_time
2. 检查数据库时间是否 UTC
3. 检查前端 lastTime 参数是否正确传递

### 加载慢
1. 检查 Redis 命中率：`INFO stats | grep keyspace_hits`
2. 检查是否有大 Key：`redis-cli --bigkeys`
3. Feed 流用拉模式兜底时查数据库：EXPLAIN 看索引
```

### Skill 沉淀的工作流

```
你在开发中遇到一个坑
    ↓
告诉 Claude Code："把这个坑记录到 skill 里"
    ↓
Claude 写入 gotchas 到对应的 SKILL.md
    ↓
下次 Claude 遇到类似问题，会自动读这个 skill 避免重复踩坑
```

---

## 五、日常开发节奏总结

### 每日节奏

```
早上:
  claude  →  看看昨天的 TODO，今天做什么
    ↓
上午:
  处理小功能（直接描述问题 → Claude 改 → 验证 → 提交）
    ↓
下午:
  推进大功能（按 Phase 3 执行计划）
    ↓
遇到坑:
  立刻沉淀到 skill（1 分钟的事，以后省 10 分钟）
    ↓
下班前:
  > /commit-message  →  提交所有改动
  > 检查 git status，确认没有遗漏
```

### 需求到来时的决策树

```
需求来了
  ↓
是 bug 修复 / 样式调整 / 加字段？
  ├── 是 → 小功能流程（30 分钟 ~ 2 小时）
  └── 否 → 是跨模块 / 涉及数据库 / 新 API？
       ├── 是 → 大功能流程（2 天 ~ 1 周）
       └── 不确定 → 先花 10 分钟写几句话描述清楚
                    → 让 Claude 帮你评估复杂度
```

### 效率最高的三个习惯

1. **描述问题时带上你已经知道的线索**（省掉 Claude 排查的时间）
2. **改完代码必须验证**（哪怕只是 `npx playwright test --grep "登录"`）
3. **遇到坑立刻写进 skill**（5 分钟写 gotcha = 以后省 10 分钟排查）

---

*最后更新: 2026-06-18*