# Telegram 接入手册（builtin 内置直连）

> **状态：现行** ｜ **最后更新**：2026-09-06 ｜ 平台流程已真机验收（2026-09-05，Paper 26.2，三类会话闭环）
> **接入前先读** [`bot-builtin-common.md`](bot-builtin-common.md)。本页只列 Telegram 差异与平台侧步骤。
> 平台操作经 [@BotFather](https://t.me/BotFather) 对话完成。

---

## 0. 与 builtin 基线的主要差异

| 维度 | Telegram |
|------|----------|
| 应用形态 | **BotFather 创建 bot**（`/newbot`，即时开通免审核实名） |
| 凭据 | bot token `<bot_id>:<auth>`（`platforms.telegram.token`；长期有效，401=配置错误停用） |
| 鉴权 | 无 OAuth，Bearer token 直调 |
| 入站通道 | **长轮询 getUpdates**（免公网入站，无 WS/Webhook） |
| 会话值 | `group:<chat_id>` / `user:<chat_id>`——群 chat_id **负整数**（超级群 `-100...`），私聊 = 用户 id（正整数） |
| @提及 | 文本为纯 `@bot $cmd` 前缀（插件剥离开头连续 @token） |
| 角色判定 | getChatAdministrators（creator/administrator，60s 缓存+单飞） |
| 群普通消息 | bot 需**关闭 Privacy mode** 或设为群管理员（A3） |
| 主动私聊 | **bot 不能主动私聊从未联系它的用户**（平台限制） |
| 网络 | `api.telegram.org` 国内**不可达**，需代理（公共骨架 §4） |

## 1. 平台侧准备（一次性，约 5 分钟）

1. Telegram 内打开 @BotFather → `/newbot` → 填显示名/用户名（须以 `bot` 结尾）→ 立即返回 token（免审核）；
2. **获取 token**：BotFather 消息里 `Use this token to access the HTTP API:` 一行（→ `platforms.telegram.token`，妥善保管 R5）；
3. **关闭 Privacy mode（必须，收群普通消息）**——默认只收 @提及/命令。二选一：
   ① @BotFather → `/mybots` → 选 bot → `Bot Settings` → `Group Privacy` → `Turn off`；
   ② 把 bot 设为测试群管理员（天然可见全部消息）；
4. 目标群 → 添加成员 → 搜索 bot 用户名 → 添加；
5. **代理准备**：`api.telegram.org` 国内不可达——服务器可直连则跳过；否则备 HTTP 代理填公共骨架 §4 的 `proxy` 段（判断：启动报 getMe 自检失败/网络不可达即需代理）。

> 完成标志：能私聊 bot 得到 TG 自带反馈；bot 在测试群内。

## 2. 插件侧配置

```yaml
backend: builtin
# 国内服务器需出墙时（见公共骨架 §4）：
# proxy:
#   enabled: true
#   host: '127.0.0.1'
#   port: 7890
platforms:
  telegram:
    enabled: true
    token: '123456789:AAFxxxx你的botToken'
```

重启后控制台期望：

```
[OrzMC] [telegram] 启动成功（bot @MyServerBot），开始长轮询
```

> 完成标志：出现「启动成功」行；`/config im status` 显示 `telegram 平台: 启用`。
> 报 `getMe 自检失败（token 无效或网络不可达）`：token 抄错，或需 1.5 代理。

## 3. 会话发现与绑定（10 分钟）

群/私聊 chat_id 由 D11 自动发现：群与 bot 私聊各发一条任意消息 → 控制台出现
`[telegram] 未绑定会话消息 telegram:group:-100...…`（群为负整数、私聊为正整数用户 id，也在 status 候选）。

```
/config im bind telegram group -1001234567890 admin_group
/config im bind telegram group -1001234567890 player_group   # 可略；留空公开通知降级发管理群
/config im bind telegram user 5668266914 admin_dm            # 先私聊 bot 发一条消息取私聊 id
```

## 4. 验收差异项（基线 6 项见公共骨架，另加）

| # | 验证项 | 操作 | 期望 |
|---|--------|------|------|
| 7 | @提及命令 | 群内 @bot 发 `$l` | 返回玩家列表（@token 自动剥离） |
| 8 | 下行到私聊 | `/config im test telegram user <id> 你好` | 收到（该用户需先私聊过 bot——TG 主动私聊限制） |

## 5. 常见问题（真机踩坑）

| 现象 | 原因 / 处理 |
|------|------------|
| 启动报 `getMe 自检失败…已停用轮询` | token 抄错，或 `api.telegram.org` 不可达需代理（1.5）；修好重启 |
| 群里直接发 `$h` 没回复，但 @bot 有回复 | bot Privacy mode 开启——按 1.3 关 Privacy 或设管理员 |
| @bot 发 `$l` 无回复，普通 `$l` 正常 | 旧版本缺 @token 剥离（2026-09-05 修复）——升级 |
| 管理指令被拒 / 群主也被当非管理 | 未把 bot 设为管理员时 getChatAdministrators 不全 → fail-closed；或发送者确非群主/管理员 |
| `/config im test` 下行私聊失败 | bot 不能主动私聊未联系它的用户：先私聊 bot 建立会话再测 |
| 轮询断断续续/频繁退避 | 网络抖动/代理不稳（失败 5s 退避重试）；持续换更稳代理 |
| token 无效（401） | BotFather 检查/重新生成 token 后重启 |

## 6. 出站域名放行（R11）

| 用途 | 域名 | 方向 |
|------|------|------|
| Bot API（getUpdates/sendMessage/角色查询等全部） | `api.telegram.org` | HTTPS 出站（走代理时仅需代理可达） |

> 无 WS/公网入站——长轮询全走 `api.telegram.org` 单域名。

## 7. 能力边界

- 仅文本（D6）；发送尽力一次（D7）；
- **主动私聊限制**：bot 不能主动私聊从未联系它的用户（下行到陌生 user 会失败）；
- 群消息读取依赖 Privacy off 或 bot 为群管理员；角色判定带 60s 缓存；
- 401（token 无效）→ 停用轮询 + 健康告警（静态凭据无刷新语义）。
