# Discord 接入手册（builtin 内置直连）

> **状态：现行** ｜ **最后更新**：2026-09-06 ｜ 平台流程已真机验收（2026-09-06，Paper 26.2，三类会话闭环）
> **接入前先读** [`bot-builtin-common.md`](bot-builtin-common.md)。本页只列 Discord 差异与平台侧步骤。
> 界面名称以[开发者门户](https://discord.com/developers/applications)为准。

---

## 0. 与 builtin 基线的主要差异

| 维度 | Discord |
|------|---------|
| 应用形态 | 开发者门户 **Application + Bot**（即时创建免审核实名） |
| 凭据 | **Bot Token**（开发者门户 → Bot → Reset Token；`platforms.discord.token`） |
| 入站通道 | **Gateway WS v10**（identify/resume + 心跳，出站长连接免公网） |
| 会话粒度 | 服务器内**每文本频道一个 `group` 会话**（channel_id snowflake 全局唯一） |
| 会话值 | `group:<channel_id>`（频道）/ `user:<user_id>`（DM 用户） |
| @提及 | **snowflake 标记** `<@bot_id>` 内嵌 content（插件剥离开头连续提及；`<@!id>`/`<@&id>` 同剥） |
| 角色判定 | **guild owner 或成员角色含 ADMINISTRATOR/MANAGE_GUILD 权限位**（REST + 60s 缓存） |
| 群普通消息 | 需开发者门户开 **MESSAGE CONTENT INTENT**（特权，否则 content 空） |
| 主动私聊 | bot 只能私聊**与之共享服务器**的用户 |
| 网络 | `discord.com`/`gateway.discord.gg` 国内不可达，需代理（公共骨架 §4） |

## 1. 平台侧准备（一次性，约 10 分钟）

1. 开发者门户 → **New Application** → 左侧 **Bot** → **Add Bot**（免审核）；
2. Bot 页 → **Reset Token** → 复制（→ `platforms.discord.token`；只显示一次，泄露立即 Reset，R5）；
3. **MESSAGE CONTENT INTENT（必须）**：Bot 页 → Privileged Gateway Intents → 打开——不开则事件收得到但 `content` 空，`$l`/`$h` 全部静默；
4. **把 bot 拉进服务器**：服务器设置→成员→邀请（或 OAuth2：`https://discord.com/api/oauth2/authorize?client_id=<应用ID>&permissions=0&scope=bot`）；
   ⚠️ 建议授予 bot 一个含 **Administrator** 的角色——群主判定不依赖权限，但「成员角色权限位」判定需 bot 能读服务器角色（GET roles），读不到按 fail-closed 仅群主可管理；
5. 建文本频道（如「开发测试」）作测试会话。

> 完成标志：bot 在测试服务器内、能读频道消息。

## 2. 插件侧配置

```yaml
backend: builtin
# 国内服务器需出墙时（见公共骨架 §4）：
# proxy:
#   enabled: true
#   host: '127.0.0.1'
#   port: 7890
platforms:
  discord:
    enabled: true
    token: '你的BotToken'
```

重启后控制台期望：

```
[OrzMC] IM backend=builtin：启用内置直连（可用平台：discord）。
[OrzMC] [discord] 网关连接已建立
[OrzMC] [discord] 发送 identify（intents=37376）
[OrzMC] [discord] 网关 READY（会话已建立，bot @MyBot）
```

> 完成标志：`[discord] 网关 READY`。若反复退避：token 无效（4004 后停用）或网络/代理不可达。

## 3. 会话发现与绑定（10 分钟）

频道/用户 snowflake id 由 D11 自动发现：目标频道与 bot 私聊各发一条消息 → 控制台出现
`[discord] 未绑定会话消息 discord:group:<channel_id>…`（snowflake，也在 status 候选）。
**服务器内每频道独立会话**——只绑定想响应的频道。

```
/config im bind discord group <频道id> admin_group
/config im bind discord group <频道id> player_group   # 可略；留空公开通知降级发管理频道
/config im bind discord user <用户id> admin_dm        # 先私聊 bot 发一条消息取候选
```

## 4. 验收差异项（基线 6 项见公共骨架，另加）

| # | 验证项 | 操作 | 期望 |
|---|--------|------|------|
| 7 | @提及命令 | 频道 @bot 发 `$l` | 返回玩家列表（`<@id>` 自动剥离） |
| 8 | DM 上行 | bot 私聊发 `$h` | 回复（admin_dm 会话） |
| 9 | 下行 DM | `/config im test discord user <用户id> 你好` | 私聊收到（需与 bot 共享服务器） |

## 5. 常见问题（真机踩坑）

| 现象 | 原因 / 处理 |
|------|------------|
| 一直见不到 `[discord] READY`，日志反复 `网关地址不可用` | token 无效（403/4004 停用）或网络/代理不可达 `discord.com` |
| 网关 4004 关闭后不再重连 | token 无效（Authentication failed）——静态凭据无刷新：检查/Reset token 后重启 |
| 频道 `$l` 有回复，@bot `$l` 无反应 | Discord @提及是 `<@bot_id>` snowflake 标记（非纯文本 @bot）——旧版本缺剥离（2026-09-06 修复），升级 |
| `$h`/`$l` 全部静默（含不 @） | 1.3 MESSAGE CONTENT INTENT 未开（content 空）；开启后重启 |
| 管理指令被拒 / 群主也被当非管理 | 1.4 未给 bot Administrator 角色（读不到角色权限表 → 仅群主可管理） |
| `/config im test` 下行 DM 失败 | bot 只能私聊共享服务器的用户；且对方未屏蔽私信 |
| gateway 反复断连/心跳超时 | 网络/代理不稳；Discord 自动 resume（op7/op9 内置）；持续查代理与出口 |
| 与 EasyBot 同连一 bot | Gateway 会话抢占式：同 token 两处 identify 互踢（R3）——切前停用其一 |

## 6. 出站域名放行（R11）

| 用途 | 域名 | 方向 |
|------|------|------|
| REST API（引导/发消息/DM/角色查询） | `discord.com` | HTTPS 出站（走代理时仅需代理可达） |
| Gateway WS | `gateway.discord.gg` | WSS 出站（同代理） |

## 7. 能力边界

- 仅文本（D6）；发送尽力一次（D7）；无公网入站需求（出站 WS）；
- **会话粒度 = 频道**：一频道一 `group` 会话（未绑定频道消息仅 D11 提示不回复）；
- **主动私聊限制**：bot 只能私聊共享服务器用户；DM 出站经 `/users/@me/channels` 建通道（每用户缓存）；
- 角色判定：owner 或成员角色含 ADMINISTRATOR/MANAGE_GUILD（REST+60s 缓存+单飞；查询失败 fail-closed；DM 恒非管理）；
- @提及 snowflake 标记剥离开头连续（中间保留）；消息读取依赖 MESSAGE CONTENT INTENT（特权）；intents=37376。
