# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-16

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序）
- PR #366 P2d1：player 聚合摘要标签 + geoip 拦截/告警（已开，待 CI）
- PR #365 docs：AGENTS 长任务连续执行/交接规范（已开，待 CI）
- PR #364 P2c portal/tnt ✓ 已合
- PR #363 P2b whitelist ✓ 已合
- PR #362 P2a teleport ✓ 已合
- PR #361 P1 common 拦截器 ✓ 已合
- PR #358 P0 基础设施 ✓ 已合
- PR #357 方案定稿 ✓ 已合

## 进行中卡
- P2d2 LoginAccessControlService：features/player/LoginAccessControlService.java 玩家名/IP 黑名单踢出 + fallback 通知文本（键 login.*/未知玩家占位；默认语言）。预计 1 小 PR。文件已勘测（见 §其代码全文），直接落码。

## 未完成清单（顺序）
1. P2d2 LoginAccessControl（上述）
2. P2e security/chat：features/security 各服务玩家提示、OrzChatEvent/CommandGuard 聊天提示、command_guard_blocked 相关代码措辞
3. P2f review/rank/prison + 残留样式文案收口（unknownLabel / coordComponent hover「点击复制坐标」等 styles 内嵌文本迁出，D9 清零）
4. P2g maintenance（进度/文案片段）
5. P2h guide/menu/feature 命令 + /orzmc config 反馈（含 ConfigPath 中文描述）
6. P3 botcommands：BotCommandFeedbackService + 11 个 $cmd 帮助/反馈/列表；入站平台 lang 透传（BotCommandContext 增 lang）
7. P4 事件通知正文平移：templates.yml 正文 → event.* key；TemplateKeys 改 event.*；stage_cn → maintenance.stage.*；legacy 定制正文自动迁 custom 层（config-version 升级链）；templates.yml 瘦身为格式+配色
8. P5 收尾：rg 全量中文残留审计（白名单：日志/注释/guide_book 等内容文件）、docs/features.md/CHANGELOG 同步、MessageKeys/语言包孤儿 key 审计

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → PR 必须串行链式合入（基于最新 develop），禁止并行攒批后统一合
- 登录前踢出（prelogin）无客户端 locale → 默认语言；物品/即时消息按玩家 locale
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P2d2 开始：checkout 分支 feature/i18n-p2-login（基 origin/develop），改 LoginAccessControlService，验收：features/player + i18n 测试绿；`spotlessApply && ./gradlew test` 全绿后开 PR，CI 绿合入后更新本文件并继续 P2e。
