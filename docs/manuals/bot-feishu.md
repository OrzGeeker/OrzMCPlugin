# 飞书接入手册（builtin 内置直连）

> **状态：现行** ｜ **最后更新**：2026-09-06 ｜ 平台流程已真机验收（2026-09-05，Paper 26.2，三类会话闭环）
> **接入前先读** [`bot-builtin-common.md`](bot-builtin-common.md)。本页只列飞书差异与平台侧步骤。
> 界面名称以[飞书开放平台](https://open.feishu.cn/)为准。

---

## 0. 与 builtin 基线的主要差异

| 维度 | 飞书 |
|------|------|
| 应用形态 | **企业自建应用**（需飞书企业/团队租户管理员） |
| 凭据 | App ID（`cli_` 前缀）+ App Secret（`platforms.feishu.app_id` / `app_secret`） |
| 鉴权 | tenant_access_token（2h 预刷新，同一机制） |
| 入站通道 | 事件订阅**长连接 WS**（二进制帧，无需公网；集群单活——同凭据只一个连接收事件） |
| 会话值 | `group:<chat_id>` / `user:<chat_id>`（chat_id 形如 `oc_...`，群/单聊均为 chat_id；D11 发现） |
| @提及 | 文本含 `@_user_N` 占位符前缀（插件自动剥离） |
| 角色判定 | 事件无角色；插件查群信息 API（owner_id + 管理员列表，30s 缓存） |
| 网络 | 国内可达；海外部署可配 proxy 回国 |

## 1. 平台侧准备（一次性，约 15 分钟）

1. 飞书开放平台 → **创建企业自建应用**（需企业租户管理员）→ 填名称/描述/图标；
2. **凭证与基础信息**：复制 App ID（`cli_...` → `app_id`）；App Secret 点查看复制（→ `app_secret`，妥善保管 R5）；
3. **权限管理**（必须）：`im:message`、`im:message.group_msg`（敏感，群内全部消息）、
   `im:message.p2p_msg:readonly`（单聊）、`im:chat`（群信息——**角色判定必需**）；
   敏感权限需管理员审核；**发布新版本并审核通过**后生效；
4. **事件与回调**：添加 `im.message.receive_v1`；**接收方式必须选「长连接」**（不要 Webhook，除非有公网 URL）；
5. 目标群 → 群设置 → 群机器人/应用 → 添加该应用。

> 完成标志：应用在测试群内；能 @ 应用 得到飞书的应用消息反馈。

## 2. 插件侧配置

```yaml
backend: builtin
platforms:
  feishu:
    enabled: true
    app_id: 'cli_xxxxxxxxxxxxxxxx'
    app_secret: '你的AppSecret'
```

重启后控制台期望：

```
[OrzMC] [feishu] 网关连接已建立
```

> 完成标志：`[feishu] 网关连接已建立`；`/config im status` 显示 `feishu 平台: 启用` + `connection: 已连接`。
> 若未出现，最常见是 1.3/1.4 权限与事件订阅未审核生效或凭据抄错。

## 3. 会话发现与绑定（10 分钟）

群/单聊 chat_id 同为 `oc_` 前缀、各不相同，由插件 D11 自动发现：测试群/单聊发任意消息 → 控制台出现
`[feishu] 忽略未绑定会话消息 target=feishu:group:oc_xxx…`（末尾 chat_id 即会话值，也在 status 候选）。

```
/config im bind feishu group <chat_id> admin_group
/config im bind feishu group <chat_id> player_group   # 可略；留空公开通知降级发管理群
/config im bind feishu user <chat_id> admin_dm        # 先私聊 bot 发一条消息，取单聊 chat_id
```

## 4. 验收差异项（基线 6 项见公共骨架，另加）

| # | 验证项 | 操作 | 期望 |
|---|--------|------|------|
| 7 | @提及命令 | 群内 @机器人 发 `$l` | 返回玩家列表（@_user_N 自动剥离） |

## 5. 常见问题（真机踩坑）

| 现象 | 原因 / 处理 |
|------|------------|
| 一直不见 `[feishu] 网关连接已建立` | 1.3 权限/1.4 事件订阅未审核生效；凭据抄错；应用未发布新版本 |
| 能发消息但收不到群事件 | 1.4 接收方式选了 Webhook 且无公网（应选长连接）；或事件未添加/新版本未审核 |
| 只收到 @ 消息、普通群消息收不到 | 1.3 `im:message.group_msg` 未开或未审核 |
| @机器人 命令不回复，普通 `$h` 正常 | 旧版本缺 @_user_N 剥离（2026-09-05 修复）——升级版本 |
| 管理指令被拒 / 群主也被当非管理 | 1.3 `im:chat` 未开（角色查询失败按非管理 fail-closed）；或发送者确实非群主/管理员 |
| 应用收不到单聊消息 | 1.3 `im:message.p2p_msg:readonly` 未开；先私聊 bot 一条触发发现 |

## 6. 出站域名放行（R11）

| 用途 | 域名 | 方向 |
|------|------|------|
| 开放 API（鉴权/消息/群信息/长连接引导） | `open.feishu.cn` | HTTPS 出站 |
| 长连接 WS | 端点引导下发（`wss://…`） | WSS 出站 |

## 7. 能力边界

- 仅文本（D6）；发送尽力一次（D7）；无 QQ 式被动回复语义（均 chat_id 直发）；
- **同应用长连接集群单活**：多实例/与 EasyBot 并存会互抢事件——一个凭据只允许一个连接（R3）；
- 群角色查询带 30s 缓存；应用需在群内才能收发该群消息。
