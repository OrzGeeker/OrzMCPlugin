# IM 网关内建方案（in-house IM gateway）与 EasyBot 双通道

> **状态：方案定稿**（待按 P1 起拆实施 PR）｜**最后更新**：2026-09-03
> **决策日期**：2026-09-03（owner 逐条拍板，见 §0）
> **前置调研**：EasyBot 源码（EasyIndie/EasyBot@main）协议取证 + 本插件 `infra/bot` 装配取证 + 四平台公共 API 可行性分析
> **配套**：[features.md §2.5 EasyBot 网关配置指南](../features.md)（现状文档，含失真项待修）、[EasyBot 接入文档 PR（EasyIndie/EasyBot#114）](https://github.com/EasyIndie/EasyBot/pull/114)

## 0. 决策记录（owner 拍板，勿回退）

| # | 项 | 决策 |
|:--|:--|:--|
| D1 | 切换粒度 | **全局** `backend: easybot \| builtin`（v1 不做平台级混合） |
| D2 | 配置归属 | **独立 `im.yml`**（backend + builtin 凭据/会话；easybot.yml 保留为 EasyBotDriver 连接配置，见 §4） |
| D3 | 失败回退 | builtin 通道启动失败 → **停群功能 + 日志/`/bot` 告警**，等管理员处理；**不做自动 fallback**（掩盖故障难排查） |
| D4 | 切换生效 | 首版仅**重启 或 `/orzmc config reload` 后生效**（复用现有回调链，不做运行时无感热切） |
| D5 | 平台落地顺序 | **QQ → 飞书 → Discord → Telegram**（每平台一个独立 PR，成熟一个挂一个） |

> D5 与「先 TG 试点」的早期草案不同——owner 以现网主平台 QQ 为优先。各平台仍建议首个落地平台同时铺「通用骨架 + 该平台 adapter」，后续平台只是往骨架里填 adapter。

## 1. 背景与目标

插件群消息/管理指令目前**依赖外部 EasyBot 网关进程**（OrzMC 只连它的 REST + WebSocket，见
`infra/bot/`）。外部服务单点与运维负担促使评估「把跨平台双向文本通信内建进插件」。调研结论：

- **四平台（QQ/飞书/Discord/Telegram）官方协议全部可手写实现（HTTP+WS+JSON），运行时依赖增量为 0**——现有
  JDK `HttpClient`（AsyncHttp）+ `Java-WebSocket`（已打进 jar 137KB）+ Gson 即覆盖全部需要；
- **官方 SDK 路线否决**：JDA/telegrambots/飞书 SDK 体积 MB 级，叠加会撞 Hangar 10MB 上限（当前 shadowJar 3.1MB）；
- **EasyBot 已稳定服役**，内建开发需要时间 → **双通道并存 + 可切换**是过渡与兜底策略（本方案）。

目标：
1. `backend=easybot`（默认）= 现状零风险，始终可回退；
2. `backend=builtin` 落地后，内建各平台 adapter 逐个替代；
3. 业务层（BotCommandService/Notifier/审核等）**零改动**——两通道对外语义一致。

非目标（v1）：平台级混合路由、运行时无感热切、媒体/富文本/交互消息（仅文本）、QQ 个人号（无公共 API）、微信（不可行，已移出支持列表）。

## 2. 现状接缝（代码事实）

```
BotModule(组合根, assembly/BotModule.java)
  └─ BotMessageServiceProvider.create(...) ──► OrzEasyBot（BotMessageService 唯一实现）
        BotMessageService 接口：setup / send(MessageEnvelope) / tryReconnectIfDisconnected /
                                reloadConfig / tearDown
  运行时重载：/orzmc config reload [easybot] ─► easyBotConfigReload 回调
              ─► OrzEasyBot.reloadConfig() ─► WebSocketLifecycle.reconcile()（fingerprint 比对重建）
```

- `BotMessageService` 即现成 driver 抽象；`BotMessageServiceProvider` 即 driver 选择点——**架构无需改动**，
  双模式 = Provider 按 `im.yml` 的 backend 选实现 + 外层协调切换。
- 健康聚合：`HealthRegistry`（当前 key=`easybot`）→ `/bot` 命令展示；builtin 沿用同构（key 细化到平台，如 `builtin.qq`）。
- 业务侧依赖仅 orzmc-api 的 `MessageEnvelope{PUBLIC|PRIVATE, text, format}` 与 `BotInboundHandler`——不感知 backend。

## 3. 目标架构

```
              BotMessageService（接口）          ← 业务层无感知
                        ▲
        ┌───────────────┴────────────────┐
   ImGatewayService（Facade，新增）         ← 持当前 driver，转发；reload 时若 backend 变化
        │                                    tearDown 旧 driver → setup 新 driver（D4：仅 reload/重启生效）
        │
        ├─ EasyBotDriver（≈ 现 OrzEasyBot，默认兜底）
        │     ├─ WebSocketLifecycle / HttpSender / InboundEventParser（现状原样）
        │     └─ 会话路由（抽共享层，见下）
        └─ BuiltinDriver（内建；按 D5 顺序逐平台挂 adapter）
              ├─ QqAdapter → FeishuAdapter → DiscordAdapter → TelegramAdapter
              │    各含：InboundSource（WS 网关/长轮询） + Sender（REST 出站） + 角色判定
              └─ 会话路由（复用共享层）
```

**共享路由层**（从 OrzEasyBot 抽出的通用部分，双 driver 复用，行为不变）：
MessageEnvelope 目标解析（PUBLIC→player_group 降级 admin_group；PRIVATE→admin_dm）、
入站会话门槛（fail-closed）、限频、格式化分段。抽出后 OrzEasyBot 退化为「EasyBot transport」。
抽取本身应是**零行为变更的重构**（现测试全绿为验收门槛）。

## 4. 配置设计（D2）

### v1（backend 开关；P1 交付）

```yaml
# im.yml（新增，schema 文件，纳入 ConfigSchema 版本治理）
config-version: 1
backend: easybot            # easybot | builtin；builtin 不可用时按 D3 处理
# v1 只放 backend；builtin 凭据/会话在首个 builtin 平台（QQ）PR 时按需扩展，
# 届时把 easybot.yml 的 platforms 段收敛为 im.yml 单一事实源（见「演进」）。
```

- `easybot.yml` **完全不动**（EasyBotDriver 继续读它：api_server/ws_server/api_key/platforms/超时重试）；
- `im.yml` 注册进 ConfigService（schema 文件：`im`），与 easybot 并列可 `/orzmc config reload im`；
- Provider.create 读取 `im.yml.backend`：`easybot` → 现 OrzEasyBot；`builtin` → Facade/BuiltinDriver。

### 演进（builtin 首平台落地时，单独 PR）

会话路由成为单一事实源：`easybot.yml.platforms` 迁移入 `im.yml`，easybot.yml 退化为纯连接参数
（api_server/ws_server/api_key/超时重试/ws 日志）；提供一次性迁移（沿用 ConfigUpgrader 惯例），
并做「双文件平台段不一致」启动告警。QQ 会话标识为平台原生 OpenID 三段式
（`qq:group:{OpenID}` 等，与 EasyBot target 语义一致——两模式共用同一会话值，切换 backend 无需改配置）。

## 5. 两模式语义一致性（双通道可行前提）

| 语义 | EasyBotDriver（网关归一） | BuiltinDriver（平台 API 直判） |
|:--|:--|:--|
| `sender.role` 取值 | `Owner/Admin/Member/Bot/Anonymous` | 各 adapter 归一为同枚举 |
| QQ 群主/管理 | 事件自带 `author.member_role`（owner/admin） | 同左（事件自带，零 API） |
| Telegram | `getChatAdministrators` creator/administrator | 同左（per-chat 缓存 + chat_member 事件失效） |
| Discord | 群主 `GET /guilds/{id}`；管理 `member.permissions` ADMINISTRATOR 位 | 同左 |
| 飞书 | `GET /im/v1/chats/{id}` owner_id + user_manager_id_list | 同左（role cache TTL + 事件失效） |
| 私聊 | 无角色 → 管理指令仅群内 | 同左（admin_dm 仅下行告警 + 入站门槛） |

> 判定来源在两通道都是**平台官方数据，不配置 ID 白名单**（owner 决策）。业务层
> `guardAdminCommand(isAdmin)` 无需区分 backend——这是双实现能并存的最重要前提。

## 6. 平台实现要点（builtin 落地依据，均已对照 EasyBot 源码/官方协议取证）

| | QQ（官方开放平台） | 飞书 | Discord | Telegram |
|:--|:--|:--|:--|:--|
| 鉴权 | `app_id+client_secret`→`bots.qq.com` 换 access_token（2h 缓存刷新） | `app_id+app_secret`→`/open-apis/auth/v3/tenant_access_token/internal` | `Authorization: Bot <token>` | token 在 URL 路径 |
| 上行 | 出站 WS Gateway（identify/heartbeat/resume/session，同 Discord 构型） | 事件订阅长连接（集群单活） | Gateway WS `gateway.discord.gg`（opcode/intents/resume） | `getUpdates` 长轮询（免公网） |
| 下行 | `POST /v2/groups/{openid}/messages`（msg_type 1）；C2C `/v2/users/{openid}/messages` | `POST /im/v1/messages`（chat_id, msg_type text） | `POST /channels/{id}/messages`（2000 上限） | `sendMessage`（4096 上限） |
| 身份 | openid（非 QQ 号） | openid + chat_id | snowflake + roles | 数字 id |
| 入站限 | 事件 `author.member_role` 自带角色 | 事件无角色→ chats API 查询（缓存） | 事件 `member.permissions` 自带 | 无角色→ admins API（缓存） |
| 政策/门槛 | 开放平台注册+审核+进群方式（**需 owner 后台核验**） | 需企业组织；事件端点细节核验 | 开放；>100 guilds 需申请 intent | 完全开放（BotFather 即建即用） |
| 预估 adapter 量 | ~600 行 | ~500 行 | ~600 行 | ~300 行 |

体积：四平台 adapter 全落地预计 shadowJar 增加 <100KB（仅业务代码，无新依赖）——10MB 上限余量充足。

## 7. 实施路线

| 步 | 内容 | 验收（对应测试） |
|:--|:--|:--|
| **P1** | `im.yml` schema（backend）+ Provider 按 backend 选 driver（builtin 未实现时 backend=builtin 报「不可用」并停群功能+告警，D3）+ Facade 骨架 | 新单测：backend 解析/选择/不可用路径；现有 EasyBot 测试全绿；`backend=easybot` 行为与现状字节级一致 |
| **P2** | 共享路由层抽取（OrzEasyBot 拆 transport/路由，零行为变更） | 现有 26 个 EasyBot 相关测试 + e2e（Paper/Folia）全绿，diff 仅移动 |
| **P3a** | **QQ adapter**（D5 首个）：WS Gateway + member_role 角色 + `/v2/...` 下行 + openid 会话 | QQ adapter 单测（MockWebServer + mock gateway）；真实平台冒烟脚本 |
| **P3b-e** | 飞书 → Discord → Telegram adapter 逐个挂载 | 同 P3a 模式 |
| **P4** | backend=builtin 端到端（`/bot` 健康/投递、群指令一问一答）+ 文档（features.md §2.5 重写为双通道） | e2e 双核心全绿 |

每步独立 PR（AGENTS.md：单 PR <500 行），逐步合并、逐步可回退（backend 一行切回 easybot）。

## 8. 测试策略

- **模式选择/切换**：`im.yml` backend 解析、Provider 选择、builtin 不可用路径 → JUnit（MockBukkit 不需要，纯装配级）；
- **builtin 各 adapter**：协议层单测用 OkHttp MockWebServer（已有 testImplementation）模拟平台 REST；QQ/Discord/飞书
  WS 网关用本地 WebSocket mock（复用测试基建思路），角色判定矩阵化断言；
- **回归门槛**：P2 重构必须现测试全绿（EasyBot 相关 26 文件 + 全量 1600+）再合并；
- **真实平台冒烟**：QQ 优先（D5），按 `docs/dev/folia-luckperms-gotchas.md` §6 测试服方法论，事件/通知/审核改动真机验证后再合。

## 9. 风险与待核验清单

1. **QQ 政策门槛**（最高风险，先于 P3a 排期核验）：开放平台注册（个人/企业）、机器人进群方式、沙箱→正式流程——owner 后台确认；
2. **飞书企业组织**：无企业资源则该平台实际不可用（需从 builtin 列表剔除或仅文档支持）；
3. **字段演进**：EasyBot 0.0.33 事件 `sender.nickname/user_id` 与 main `sender.name/id` 不一致——若升级网关需适配（已在 EasyBot#114 文档落账）；
4. **会话值兼容**：QQ 填 openid 而非群号（现有生产 easybot.yml 若填错则两模式都发不出，需核对现网值）。
