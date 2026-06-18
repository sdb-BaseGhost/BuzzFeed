# Claude Code Skills 方法论：从 Anthropic 内部实践中学到的开发加速指南

> **来源**: [Lessons from building Claude Code: How we use skills](https://claude.com/blog/lessons-from-building-claude-code-how-we-use-skills)
> **发布时间**: 2026-06-03
> **适用场景**: 任何使用 Claude Code 进行开发的团队和个人

---

## 核心理念

Skills 不是简单的 Markdown 文件，而是**包含指令、脚本和资源的文件夹**，让 AI Agent 能够发现并高效执行特定任务。在 Anthropic 内部，已有数百个 skills 在活跃使用。

**关键洞察**：最有效的 skills 利用文件夹结构和配置选项，而不是仅仅提供文字说明。

---

## 一、Skills 的九大分类体系

将 Anthropic 内部所有 skills 进行分类后，发现了九个清晰的类别。

**好的 skill 应该清晰地属于其中一个类别；试图做太多的 skill 会让 Agent 困惑。**

### 1. 库/API 参考类 (Library and API Reference)

**用途**: 解释如何正确使用库、CLI 或 SDK，包括内部库和 Claude 容易出错的公共库。

**核心元素**: 参考代码片段文件夹 + Gotchas（易错点）列表

**示例**:
- `billing-lib` — 内部计费库的边缘情况和陷阱
- `internal-platform-cli` — 每个子命令及使用场景
- `sandbox-proxy` — 网关配置、调试技巧

### 2. 产品验证类 (Product Verification)

**用途**: 描述如何测试或验证代码是否正常工作。

**⭐ 这是对 Claude 输出质量提升最显著的类别。值得花一周时间专门打磨。**

**技术手段**: Playwright、Tmux、录制视频、每步程序化断言

**示例**:
- `signup-flow-driver` — 端到端注册流程验证
- `checkout-verifier` — Stripe 测试卡驱动结账流程
- `tmux-cli-driver` — 需要 TTY 的交互式 CLI 测试

### 3. 数据获取与分析类 (Data Fetching and Analysis)

**用途**: 连接数据和监控栈，包含带凭据的数据获取库、仪表盘 ID、常见工作流。

**示例**:
- `funnel-query` — 事件关联查询（注册 -> 激活 -> 付费）
- `cohort-compare` — 留存/转化对比，标记显著差异
- `grafana` / `datadog` — 问题到仪表盘的映射

### 4. 业务流程/团队自动化类 (Business Process Automation)

**用途**: 将重复工作流自动化为一条命令。将之前结果保存到日志文件帮助模型保持一致性。

**示例**:
- `standup-post` — 聚合工单、GitHub、Slack 生成站会报告
- `create-<ticket>-ticket` — 强制 Schema + 创建后工作流
- `weekly-recap` — 合并 PR + 关闭工单 + 部署 -> 周报

### 5. 代码脚手架/模板类 (Code Scaffolding and Templates)

**用途**: 为代码库中的特定功能生成框架代码。当脚手架有**自然语言层面的要求**时特别有用。

**示例**:
- `new-<framework>-workflow` — 脚手架新服务/工作流
- `new-migration` — 迁移文件模板 + 常见陷阱
- `create-app` — 新内部应用（含认证、日志、部署预配置）

### 6. 代码质量/审查类 (Code Quality and Review)

**用途**: 强制执行代码规范，辅助代码审查。可作为 hooks 自动运行或在 GitHub Action 中使用。

**示例**:
- `adversarial-review` — 生成子 Agent 进行对抗式审查
- `code-style` — 强制 Claude 默认不会做好的代码风格
- `testing-practices` — 测试编写指南

### 7. CI/CD 与部署类 (CI/CD and Deployment)

**用途**: 拉取、推送和部署代码。

**示例**:
- `babysit-pr` — 监控 PR -> 重试 flaky CI -> 解决冲突 -> 自动合并
- `deploy-<service>` — 构建 -> 冒烟测试 -> 渐进流量 -> 自动回滚
- `cherry-pick-prod` — 隔离工作树 -> cherry-pick -> 冲突解决 -> PR

### 8. Runbook 类 (Runbooks)

**用途**: 接收症状（Slack 线程/告警/错误签名），进行多工具调查，生成结构化报告。

**示例**:
- `<service>-debugging` — 症状 -> 工具 -> 查询模式映射
- `oncall-runner` — 获取告警 -> 检查常见原因 -> 格式化发现
- `log-correlator` — 给定 request ID，从所有系统拉取相关日志

### 9. 基础设施运维类 (Infrastructure Operations)

**用途**: 执行常规维护和运维操作，特别适合涉及破坏性操作时需要护栏的场景。

**示例**:
- `<resource>-orphans` — 查找孤立资源 -> Slack -> 缓冲期 -> 级联清理
- `dependency-management` — 依赖审批工作流
- `cost-investigation` — 存储/流量账单飙升调查

---

## 二、Skills 编写实战指南

### 1. 不要陈述显而易见的内容

Claude 已经会写代码，也能读懂代码库。**关注那些让 Claude 偏离默认思维方式的信息。**

- ❌ "请使用 React 函数式组件"
- ✅ "你的设计品味需要改进；避免 Inter 字体和紫色渐变"

### 2. 构建 Gotchas 部分 — 每个 skill 中信息密度最高的部分

基于 Claude 在使用 skill 时常遇到的失败点**持续积累**。

**高质量 Gotcha 示例**:
- "subscriptions 表是 append-only 的。你需要的是 version 最高的那行，而不是 created_at 最新的那行。"
- "这个字段在 API 网关里叫 @request_id，在计费服务里叫 trace_id。它们是同一个值。"
- "Staging 环境即使 Stripe webhook 没有实际处理也会返回 200。去 payment_events 表查真实状态。"

### 3. 使用文件系统和渐进式披露

**将整个文件系统视为上下文工程 (Context Engineering)。**

```
my-skill/
├── SKILL.md           # 入口，描述 skill 并指向其他文件
├── references/
│   ├── api.md         # 详细函数签名和用法示例
│   └── edge-cases.md  # 边缘情况处理
├── scripts/
│   ├── fetch_data.py  # 可复用的数据获取脚本
│   └── analyze.py     # 分析工具
├── assets/
│   └── template.md    # 输出模板
├── config.json        # 配置文件（可能需要用户输入）
└── memory.log         # 追加式记忆日志
```

**关键**: 在 SKILL.md 中告诉 Claude 有哪些文件可引用，它会在适当时机读取。

### 4. 避免限制 Claude 的灵活性

Claude 通常会遵循你的指令。因为 skills 可复用性很强，**给 Claude 所需的信息，但保留根据情况调整的灵活性**。

### 5. 设计启动流程（Setup Flow）

使用 `config.json` 存储配置。如果配置未设置，Agent 可以向用户询问。使用 `AskUserQuestion` 工具可以呈现结构化的多选问题。

### 6. 为模型写描述，而非为人类

**Description 不是摘要，而是"何时触发"的描述。** Claude Code 启动时会扫描所有 skill 描述来决定是否触发。

- ❌ "Standup automation skill for team meetings"
- ✅ "Use when the user wants to post a standup, generate a daily update, or aggregate work from tickets and PRs for a status report."

### 7. 帮助 Claude 记忆

通过在 skill 目录内存储数据实现记忆：

- **简单**: 追加式文本日志 (`standups.log`)
- **结构化**: JSON 文件
- **高级**: SQLite 数据库
- 使用 `${CLAUDE_PLUGIN_DATA}` 环境变量获取稳定存储目录

### 8. 提供脚本而非样板代码

**给 Claude 最强大的工具就是代码。** 让 Claude 将回合花在组合和决策上，而不是重建样板代码。

提供函数库后，Claude 可以动态生成脚本来组合这些功能回答复杂查询。

### 9. 使用按需 Hooks

Skills 可包含**只在被调用时激活、仅持续当前会话**的 hooks：

- `careful` — 阻止 `rm -rf`、`DROP TABLE`、`force-push`（仅在操作生产环境时）
- `freeze` — 只允许编辑特定目录（调试时防止意外修改）

---

## 三、Skills 分发与管理

### 两种分发方式

| 方式 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| 仓库内 `.claude/skills/` | 小团队、少量仓库 | 简单直接 | 增加上下文负担 |
| 插件市场 | 大规模团队 | 按需安装、独立管理 | 需要额外基础设施 |

### 管理 Skills 市场（去中心化模型）

1. **上传到沙箱**: 写好 skill -> 上传到 GitHub sandbox 文件夹 -> Slack 分享
2. **自然增长**: 根据使用量和反馈自然筛选
3. **正式收录**: 有足够 traction 后，提 PR 移入正式市场

### 衡量 Skill 效果

使用 `PreToolUse` hook 记录 skill 使用情况，追踪：使用频率、触发率、错误率。

---

## 四、落地执行清单

### Phase 1: 基础设施搭建（第 1 周）

- [ ] 在项目中创建 `.claude/skills/` 目录结构
- [ ] 识别团队中最重复的 3-5 个工作流
- [ ] 为每个工作流编写第一个 skill 骨架
- [ ] 创建 `config.json` 模板

### Phase 2: 核心 Skills 开发（第 2-3 周）

- [ ] **优先开发产品验证类 skills**（ROI 最高）
- [ ] 为每个 skill 添加 Gotchas 部分
- [ ] 为关键 skill 添加可复用脚本
- [ ] 实现记忆/日志功能
- [ ] 编写清晰的触发描述（为模型写，非为人写）

### Phase 3: 分发与迭代（第 4 周起）

- [ ] 建立 skill 分发机制
- [ ] 设置 PreToolUse hook 追踪使用数据
- [ ] 每周回顾：哪些 skill 被频繁使用？哪些被忽略？
- [ ] 根据 Claude 遇到的新边缘情况持续更新 Gotchas

### Phase 4: 成熟运营（持续）

- [ ] 建立 skill 提交和评审流程
- [ ] 定期清理不再使用的 skills
- [ ] 分享最佳实践和成功案例
- [ ] 探索跨 skill 组合

---

## 五、BuzzFeed 项目应用建议

基于本项目技术栈（Spring Boot + Vue.js + MinIO + Redis + Canal），建议优先创建以下 skills：

### 优先级 P0（立即创建）

| Skill 名 | 类别 | 内容 |
|-----------|------|------|
| `minio-operations` | 库/API 参考 | MinIO 客户端配置、上传/下载最佳实践、常见错误处理 |
| `redis-cache-pattern` | 库/API 参考 | Redis 缓存策略、序列化陷阱、多级缓存模式 |
| `upload-flow-verify` | 产品验证 | 文件上传 -> 存储 -> 缩略图 -> Feed 流全流程验证 |
| `security-config` | 库/API 参考 | Spring Security 配置指南、JWT 认证流程、CORS 配置 |

### 优先级 P1（第二周）

| Skill 名 | 类别 | 内容 |
|-----------|------|------|
| `feed-service-debugging` | Runbook | Feed 流异常排查、Redis 缓存命中率诊断 |
| `new-controller-scaffold` | 脚手架 | 新 Controller 模板（含异常处理、日志、分页） |
| `code-review-checklist` | 代码质量 | 代码审查清单、安全检查点、性能考虑 |
| `deploy-checklist` | CI/CD | 部署前检查清单、环境变量验证、回滚步骤 |

### 优先级 P2（持续迭代）

| Skill 名 | 类别 | 内容 |
|-----------|------|------|
| `data-analysis` | 数据分析 | 用户行为数据查询、Feed 流指标分析 |
| `weekly-recap` | 业务自动化 | 汇总本周 PR、工单、部署 -> 生成周报 |
| `cost-investigation` | 基础设施 | MinIO 存储成本调查、Redis 内存分析 |

---

## 六、核心原则速查表

| # | 原则 | 说明 |
|---|------|------|
| 1 | Skills 是文件夹不是文件 | 利用完整文件系统做上下文工程 |
| 2 | Gotchas 是最高价值内容 | 持续从失败中积累 |
| 3 | 描述为模型写，不是为人写 | 聚焦"何时触发" |
| 4 | 不要限制 Claude 的灵活性 | 给信息，不给死板流程 |
| 5 | 帮助 Claude 记忆 | 日志文件记录历史 |
| 6 | 给 Claude 脚本而非样板代码 | 让它专注组合和决策 |
| 7 | 按需启用 hooks | 精确控制行为 |
| 8 | 让 Skills 自然增长 | 从几行开始，持续迭代 |
| 9 | 衡量并优化 | 用数据驱动 skill 改进 |
| 10 | 产品验证类 skill ROI 最高 | 优先投入 |

---

*整理自 Anthropic 官方博客 | Thariq Shihipar (Anthropic 工程师) | 2026-06-03*