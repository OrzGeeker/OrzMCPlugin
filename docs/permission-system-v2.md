# 权限系统二期 · 通用审核框架方案（v8 定稿）

> 状态：已确认（2026-08-07）｜分支：`feat/rank-promotion`｜关联：[features.md](features.md)、[test-cases.md](test-cases.md)

## 一、背景与目标

**现状**：Rank 权限系统一期已闭环（时长读服务器原生 stats、default→member 自动晋升、member→builder 申请审核、LP 软依赖降级），但存在 3 个问题：

| # | 问题 | 现状 |
|:--|:--|:--|
| 1 | 审核流程**写死在 rank 模块** | `pending_application` 专用于 builder 晋升，无法复用 |
| 2 | 群内审核**依赖 `$e` 裸命令** | 无校验、无列表、易出错 |
| 3 | 晋升阈值**硬编码 10h** | 改阈值要改代码 |

**目标**：

1. **通用审核框架**——「申请→审核→处理→通知」全流程，本次落地晋升，未来扩展零框架改动
2. **1 条群指令 `$v`** 承载审核，维护负担最低
3. 权限模块**单一独立配置文件**（`permission.yml`），阈值可调，不混 `config.yml`
4. 审核请求**携带结构化内容**（data），「审核什么」明确表达
5. **全链路通知**：提交/撤回/通过/拒绝 4 环节全部同步群，结果必达玩家
6. 玩家**自助查询**：当前权限组、申请状态、撤回申请
7. **代码边界按「未来独立沉淀为通用审核插件」设计**，可整体拆分

---

## 二、总体架构

```
┌─── 通用审核框架（可整体搬走，零宿主依赖）───┐
│ features/review/                           │
│  ReviewType  ReviewHandler  ReviewRequest  │
│  ReviewService  ReviewStore                │
└────────────────────────────────────────────┘
   ▲ 依赖注入（构造器）            ▲ 端口实现（留在宿主）
   │  ReviewStore(持久化)         │  PermissionStore（permission.yml）
   │  NotifierSink(通知)          │  现有 Notifier 适配
   │  PlayerLookup(玩家名↔UUID)    │  ServerAccess 适配
   │  Scheduler(异步)             │  SafeScheduler 适配
   └──────────────────────────────┴────────────────────
   features/rank/（消费者）
     ├ 注册 ReviewType 枚举项（BUILDER_PROMOTION 元数据）
     └ 注入 handler：LuckPermsPromoter（LP 授权）
```

**分层原则**：

- `review` 包内**只出现端口接口**（ReviewStore / NotifierSink / PlayerLookup / Scheduler），不 import `OrzServices`、`BotMessageService`、`ConfigService` 等宿主类
- 审核类型**元数据定义在框架侧，handler 由 rank 模块注入注册**——review 包零 LP 依赖
- 未来拆插件 = 搬 `features/review/` + `permission.yml` 的 reviews 节 → 新工程补 4 个端口适配器 → 注册自己的审核类型。**核心代码零改动**

---

## 三、详细设计

### 3.1 核心类（`features/review/`）

**ReviewRequest（值对象）**：

```java
public record ReviewRequest(
    String id,               // 唯一标识
    String typeId,           // 审核类型 id
    UUID applicantId,        // 申请人
    Map<String,String> data, // 请求内容（键值对）
    Status status,           // PENDING/APPROVED/REJECTED/CANCELLED
    long createdAt, long reviewedAt, String reviewerName) { ... }
```

**ReviewType（注册表）**——每项：id、展示名、命令键 + 参数解析 + 资格预检、列表摘要；handler 由 rank 模块注入：

```java
// 框架侧：定义元数据
BUILDER_PROMOTION(
    "builder-promotion", "晋升建造者",
    "builder",                              // /apply builder [理由]
    args -> Map.of("target-group", "builder",
                   "reason", args.getOrDefault("reason", "")),
    p -> RankService.currentGroup(p).equals("member"),   // 预检
    data -> "申请晋升builder" + ...)        // 列表摘要

// rank 模块侧：注入通过后处理
reviewRegistry.register(BUILDER_PROMOTION, id -> promoter.promoteToBuilder(id));
```

**ReviewService（核心）**——通知逻辑收在 service 层，任何入口触发都自动通知：

```java
String submit(ReviewType type, UUID applicantId, Map<String,String> data);
    // 资格预检（不满足直接拒）→ 防重复 → PENDING → 持久化
    // → 游戏内「已提交」 → 群 review_submitted
boolean cancel(String requestId, UUID applicantId);
    // 仅 PENDING 可撤回 → CANCELLED → 游戏内「已撤回」 → 群 review_cancelled
boolean review(String requestId, boolean approved, String reviewerName);
    // PENDING → 置状态 → approved 时调 handler()
    // → 群 review_approved|rejected → 游戏内通知申请人（在线即发）
List<ReviewRequest> listPending();
boolean hasPending(ReviewType type, UUID applicantId);
Optional<ReviewRequest> pendingFor(ReviewType type, String playerName);
```

**ReviewStore**——持久化端口接口（实现 = PermissionStore 的 reviews 节）。
**ReviewHandler**——函数式接口 `void onApproved(UUID applicantId)`。
**ReviewCommandService**——游戏内 `/review approve|reject <name>` 薄封装。

### 3.2 配置（单一独立文件，不混 config.yml）

```
permission.yml
├── config:      member-threshold-hours: 10      # 静态配置节
├── ranks:       players.<uuid>.promoted: true   # 晋升状态节（运行时）
└── reviews:     requests.<id>: {type, applicant,# 审核记录节（运行时）
                 data, status, ...}
```

- 一个文件、一个统一 `PermissionStore` 类管理（load/save 整个文件），config 节静态，ranks/reviews 节 markAlwaysSave
- 命名 `permission.yml`（功能主体是权限，审核是其中流程）；将来拆审核插件时 reviews 节连同 review 框架整体切出
- **迁移**：启动时 `ranks.yml` 遗留 `pending_application=true` → 写入 reviews 节（BUILDER_PROMOTION）→ 清旧字段；`promoted` 标记搬入 ranks 节

### 3.3 群指令（新增 1 条 `$v`）

| 群指令 | 权限 | 功能 | 返回 |
|:--|:--|:--|:--|
| `$v l` | admin | 待审列表（多页 `$v l 2`，复用 Paginator） | `[晋升建造者] TestMember（当前组：member）：申请晋升builder（10分钟前）` |
| `$v y <玩家>` | admin | 通过 | `已通过 TestMember 的晋升建造者申请` |
| `$v n <玩家>` | admin | 拒绝 | `已拒绝 TestMember 的申请` |

- 复用 `OrzUserCmd` 枚举 + `BotCommandService` handler + `needAdminPermission=true`
- 列表带申请人**当前组**；按玩家名定位唯一待审，`$v y <id>` 预留精确操作

### 3.4 游戏内命令（注册表驱动）

| 命令 | 功能 |
|:--|:--|
| `/apply` | 列出可申请类型（自动生成帮助） |
| `/apply builder [理由]` | 提交晋升建造者申请 |
| `/apply whitelist [理由]` | 提交白名单申请（未来） |
| `/apply status` | 查看自己的申请及状态 |
| `/apply cancel <type>` | 撤回自己的待审申请 |
| `/review approve\|reject <name>` | （admin）替代 /rank approve/reject |
| `/rank` | 查自己：当前组 + 时长/进度 + 下一步可申请项 |
| `/rank <玩家>` | （admin）查指定玩家，审核前核对 |

`/rank` 返回示例：

```
你的当前权限组：member
已在线时长：12.5h / 晋升阈值 10h（✅ 已达标）
下一步可申请：builder（/apply builder [理由]）
```

- 「下一步可申请」由 ReviewType 注册表**反向生成**（资格预检通过的项）——与审核类型天然同步
- `/rank approve/reject` 移除，迁移至 `/review`

### 3.5 通知矩阵（4 环节全覆盖）

| 环节 | 触发方 | 群通知（模板键） | 玩家侧 |
|:--|:--|:--|:--|
| 提交申请 | 玩家 `/apply` | 📋 `review_submitted` | 游戏内「已提交，等待审核」 |
| 撤回申请 | 玩家 `/apply cancel` | ↩️ `review_cancelled` | 游戏内「已撤回」 |
| 通过申请 | 管理员 `$v y` / `/review approve` | ✅ `review_approved` | 游戏内「已通过」+ 群兜底 |
| 拒绝申请 | 管理员 `$v n` / `/review reject` | ❌ `review_rejected` | 游戏内「被拒（原因）」+ 群兜底 |

| 模板键 | 内容示例 |
|:--|:--|
| `review_submitted` | `📋 [新申请] TestMember 申请晋升builder：理由（$v l 查看）` |
| `review_cancelled` | `↩️ TestMember 撤回了晋升builder申请` |
| `review_approved` | `✅ TestMember 的晋升builder申请已通过（审核人：admin）` |
| `review_rejected` | `❌ TestMember 的晋升builder申请被拒（审核人：admin）` |
| `rank_status` | `/rank` 返回文案 |

- 机制复用现有 `TypedConfigProvider.renderEvent(key, vars)` + `Notifier.event(key, env)`（与 whitelist_block 同款）
- 模板键注册进 `TemplateKeys.ALL` + `templates.yml`，文案可配不写死
- **玩家结果三层兜底**：游戏内消息（在线即发）→ 群通知（离线可见）→ `/apply status`（随时自查）

### 3.6 数据迁移（启动时）

```
ranks.yml 遗留 players.<uuid>.pending_application=true
    → 写入 permission.yml 的 reviews 节（BUILDER_PROMOTION）→ 清旧字段
ranks.yml 遗留 players.<uuid>.promoted=true
    → 写入 permission.yml 的 ranks 节 → 清旧字段（或保留兼容读）
```

---

## 四、全流程时序

```
玩家 /apply builder [理由]
  → 预检 → permission.yml (PENDING)
  → 游戏内「已提交」 → 群 📋 review_submitted
        ↓
玩家 /apply cancel → CANCELLED → 游戏内「已撤回」 → 群 ↩️ review_cancelled
        ↓
管理员 $v l 查看（含当前组） / $v y|n 或 /review approve|reject
  → 状态变更 → handler 执行（LP 授权）
  → 群 ✅/❌ → 游戏内通知申请人（在线即发）
        ↓
玩家 /apply status 随时查结果 | /rank 查权限组/进度
```

**玩家侧三件套**：`/rank`（我是谁）· `/apply status`（我申请了什么）· `/apply cancel`（我能撤回）

---

## 五、未来扩展成本

| 新审核项 | 改动 | 涉及 |
|:--|:--|:--|
| 白名单申请 | 枚举加 1 项 + 注入 handler | 框架、`$v`、`/apply`、`/review`、通知**全部零改动** |
| 领地申请 | 枚举加 1 项（data 带坐标） | 同上 |
| 独立审核插件 | 搬 review 包 + reviews 节 + 补 4 适配器 | 核心零改动 |

---

## 六、范围清单

| 项 | 状态 |
|:--|:--|
| 通用审核框架（review 包，端口注入） | ✅ 本次 |
| 单一配置 permission.yml（三段式 + PermissionStore） | ✅ 本次 |
| 群指令 `$v`（l/y/n + Paginator） | ✅ 本次 |
| 游戏内 `/apply` 通用化 + `/review` + `/rank` 增强 | ✅ 本次 |
| 4 环节群通知 + 玩家结果三层兜底 | ✅ 本次 |
| 数据迁移（遗留 pending → reviews 节） | ✅ 本次 |
| builder→admin 申请、领地/白名单审核项 | ⏸ 暂缓（框架已预留） |

---

## 七、开发清单（执行跟踪）

| # | 任务 | 涉及 | 状态 |
|:--|:--|:--|:--|
| 1 | review 包：ReviewRequest / ReviewType / ReviewHandler / ReviewStore / ReviewService | features/review/ | ⬜ |
| 2 | PermissionStore（permission.yml 三段：config/ranks/reviews，markAlwaysSave） | infra/config/ + 存储 | ⬜ |
| 3 | rank 模块：阈值读取 + 完整视图查询（组+进度+可申请）+ handler 注入注册 | features/rank/ | ⬜ |
| 4 | `/apply` 四子命令 + `/review` + `/rank` 增强（Brigadier 注册） | 命令注册 + ReviewCommandService | ⬜ |
| 5 | `$v` 群指令（OrzUserCmd + handler + needAdminPermission） | features/botcommands/ | ⬜ |
| 6 | 5 个模板键（review_* ×4 + rank_status）+ templates.yml | TemplateKeys + 模板文件 | ⬜ |
| 7 | 数据迁移（启动时，遗留 pending → reviews 节） | OrzServices 装配 | ⬜ |
| 8 | 单元测试 + MockBukkit 集成测试（含通知捕获 CapturingSink） | 各模块 test | ⬜ |
| 9 | `./gradlew check` 全绿 + 本地服冒烟 | — | ⬜ |
