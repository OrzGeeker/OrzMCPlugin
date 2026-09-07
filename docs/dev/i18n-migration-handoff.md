# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-16（P2h1 完成）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序）
- PR #376 P2h1：guide/menu ✓
- PR #375 P2g1：/maintenance 命令 ✓
- PR #374 P2f3：rank ✓
- PR #373 P2f2b review 业务流 ✓
- PR #372 P2f2a review 命令层 ✓
- PR #371 P2f1：样式收口+prison/gamemode ✓
- PR(本) P2e：security 域 guard/exploit/ratelimit 文案 + 样式命令反馈（待 CI/合入标记）
- PR #368 P2d2：LoginAccessControlService（IP 黑名单/玩家名规则）登录拦截 ✓ 已合 —— **features/player 域全部完成**
- PR #366 P2d1：player 聚合摘要 + geoip ✓ 已合
- PR #366 P2d1：player 聚合摘要标签 + geoip 拦截/告警（已开，待 CI）
- PR #365 docs：AGENTS 长任务连续执行/交接规范（已开，待 CI）
- PR #364 P2c portal/tnt ✓ 已合
- PR #363 P2b whitelist ✓ 已合
- PR #362 P2a teleport ✓ 已合
- PR #361 P1 common 拦截器 ✓ 已合
- PR #358 P0 基础设施 ✓ 已合
- PR #357 方案定稿 ✓ 已合

## 进行中卡
- P3 botcommands：BotCommandFeedbackService + 11 个 $cmd 帮助/反馈/列表（先 $help/$cmd ? 通用核心 → 各 handler）；
  决议：首期 bot 输出统一默认语言 R1（platform_langs 逐平台深化留待接线 platform→parse）；AccessRule/PlayerNameRule 反馈随 $b 一并迁移；
  迁移后移除 RankService 过渡静态中文 groupDisplayName（$p/$v 改用 i18n）

## 未完成清单（顺序）
1. P3 botcommands（$cmd 全套；含 $b 黑名单反馈、$p/$v 组名改用 i18n）
2. P4 事件/模板管线：templates.yml 正文 → event.* key（TemplateKeys 改 event.*、stage_cn → maintenance.stage.*、maintenance_motd_* → key、
   WorldMaintenance/ScheduledBackup 进度文案与 Region/Chunk/File/Done 阶段语义化一并迁移、MaintenanceModeService.progressMessage 文案）
   + legacy 服主定制正文自动迁 custom 层（config-version 升级链）+ templates.yml 瘦身为格式/配色/数据
3. P5 收尾：全量 rg 中文残留审计（白名单：日志/异常/注释/config 描述/guide_book 等内容数据）、
   YAML 布尔键排查（off/on 引号）、孤儿 MessageKeys/语言包 key、移除过渡静态组名遗留检查、
   docs/features.md + README + CHANGELOG 同步、集成回归（含维护/审核/bot 双语实机冒烟）

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → PR 必须串行链式合入（基于最新 develop），禁止并行攒批后统一合
- 登录前踢出（prelogin）无客户端 locale → 默认语言；物品/即时消息按玩家 locale
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P2e 开始：checkout 分支 feature/i18n-p2-security（基 origin/develop），改 features/security 相关文件，验收：affected :test 绿 + `spotlessApply && ./gradlew test`；开 PR 后 CI 绿即合（注意依赖 bump 抢 base：落后先 rebase origin/develop），合入后更新本文件继续 P2f。
