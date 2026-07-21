# FEEDBACK.md — 错误与反思

记录 Claude 在本项目中犯过的错误和对应反思，避免重复犯错。

---

## 2026-07-21 — 前端代理配置遗漏

### 错误：Vite 代理只配了 `/api`，遗漏 `/post`、`/feed`、`/upload`

**事实**：后端 API 路径并不统一在 `/api` 前缀下。`/post`（发帖）、`/feed/getFeed`、`/upload/image|video` 都没有 `/api` 前缀。首次只配了 `/api` 代理，导致这些接口 404。后续逐一补上了 `/post`、`/feed`、`/upload`、`/files`。

**根因**：没有在修改代理前先完整扫描后端所有 `@RequestMapping` / `@PostMapping` 路径，只看到 `/api/auth` 就以为所有接口都在 `/api` 下。

**反思**：
- 修改代理/路由前，必须先 `grep -rn "Mapping" src/main/java` 列出所有后端路径
- 后端路径风格不统一（有的有 `/api` 前缀，有的没有），需要逐一确认

---

## 2026-07-21 — Mock 与真实接口脱节

### 错误：声称 like/comment/follow-user 后端接口「从未实现」

**事实**：前端 `post.js` 和 `user.js` 中的 `toggleLike`、`getComments`、`addComment`、`followUser`、`unfollowUser` 都有 `USE_MOCK` 分支。之前 `VITE_USE_MOCK=true` 时走 mock 数据，功能正常。关闭 mock 后这些接口 404，但 Claude 错误地声称后端「从未实现」这些功能——实际上这些接口本来就没计划在当前阶段实现，mock 就是设计意图。

**根因**：没有先检查 `USE_MOCK` 开关状态和 mock 分支逻辑，就直接断言后端缺少接口。混淆了「后端未实现」和「设计上使用 mock」的区别。

**反思**：
- 遇到 404 时，先检查 `USE_MOCK` 值和对应的 mock 分支
- 前端有 mock 分支 ≠ 后端需要实现该接口，要先确认设计意图

---

## 2026-06-26 — codemap.md 更新时的错误

### 错误1：声称技术文档不存在

**事实**：更新 codemap.md 时，声称 `docs/技术文档.md` 不存在，建议新建。实际上该文件早已存在且内容详尽（~600行，包含所有模块的请求链路、代码位置、设计决策）。

**根因**：只依赖 `find` 和 `ls` 的输出做判断，没有对每个文件做 Read 确认就下结论。看到 `ls docs/` 输出中有 `技术文档.md` 但未注意到它是中文文件名，与英文搜索预期不符。

**反思**：
- 在声称某个文件「不存在」之前，必须先 Read 确认，不能仅凭目录列表推断
- 中文文件名在 `find` / `ls` 输出中容易被忽略，要特别注意

---

### 错误2：模块 CLAUDE.md 中保留了过时的 checklist

**事实**：`feed/CLAUDE.md` 底部的「待开发」checklist 中列着「Fan-out Consumer: 待开发」，但 `FanoutConsumerService.java` 早已实现。这些 checklist 长期未更新，变成了过时信息。

**根因**：方法论要求「已完成/待开发」写在 `docs/技术文档.md` 的开发日志中，设计文档只保留当前设计。但之前的会话没有执行这个分离，导致设计文档中混入了状态追踪信息，且状态不同步。

**反思**：
- 严格遵守文档分层：CLAUDE.md 是设计文档（当前状态），技术文档.md 是开发日志（历史记录）
- 任何 checklist / TODO 如果不在技术文档.md 中维护，就会过时
