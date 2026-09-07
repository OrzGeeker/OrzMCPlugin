# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-08（P3 botcommands 全部完成，P4 开卡）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序，develop @ 6f8aa18）
- PR #384 P3d3：$d 黑名单命令残留文案 + PlayerNameRuleFeedback 反馈（bot.d.* + access_rule.*）✓ —— **P3 botcommands 全部完成**
- PR #383 P3d2：$v 审核命令残留文案（bot.v.*；列表分页改 paginatePages + 复用 bot.list.page_meta；
  templates.yml command_review_list_empty 改 {message}）✓
- PR #382 docs：交接刷新（P3d1 完成 P3d2 开卡）✓
- PR #381 P3d1：$e 日志溢出提示（bot.e.overflow）✓
- PR #380 P3c：$p 权限命令 + OrzUserCmd 内嵌中文收口 + RankService 过渡静态组名移除（bot.p.*）✓
- PR #379 P3b：$w/$a/$r 白名单输出 + I18nServiceHolder（bot.list.* / whitelist.bot.*）✓
- PR #378 P3a：bot 帮助/用法核心（bot.help_*/desc/usage/params/examples/lbl_*）✓
- PR #377 docs：交接刷新（P2h1 完成；P3/P4/P5 范围细化）✓
- PR #376 P2h1 guide/menu ✓
- PR #375 P2g1 maintenance /maintenance 命令 ✓
- PR #374 P2f3 rank ✓ ｜ PR #373 P2f2b review 业务流 ✓ ｜ PR #372 P2f2a review 命令层 ✓ ｜ PR #371 P2f1 样式收口+prison/gamemode ✓
- PR #370 P2e security 域 ✓ ｜ PR #368 P2d2 login 登录拦截 ✓（features/player 域全部完成）｜ PR #366 P2d1 player/geoip ✓
- PR #364 P2c portal/tnt ✓ ｜ PR #363 P2b whitelist ✓ ｜ PR #362 P2a teleport ✓ ｜ PR #361 P1 common ✓ ｜ PR #358 P0 基础设施 ✓ ｜ PR #357 方案定稿 ✓

## P3 收尾快照（botcommands 完成态）
- 11 个 $cmd 处理器用户可见文案全部 i18n（残留仅日志/注释）；组名走 rank.group.*；review 决策结果走 review.*
- **遗留（重要，P4/P5 分界）**：
  a) `templates.yml` 命令回复模板字面正文仍优先于 i18n fallback——$l/$w 实际运行时仍显示 zh：
     command_players / command_whitelist_header / command_whitelist_page / command_whitelist_cleanup
     四个键仍是中文正文（其余 command_* 已是 {message}/{patterns} 直通）→ P4a 收口
  b) 游戏内 /blacklist 命令（BlacklistCommandRegistrar）为 P2 遗漏域，大量硬编码 zh；
     PlayerNameRuleFeedback 已走 access_rule.*（R1 holder），该命令其余文案留 P5 审计统一迁移（建议按 sender locale）

## 进行中卡
- P4 事件/模板管线（拆四子卡，串行）：
  - P4a 命令回复模板正文：command_players / command_whitelist_header / command_whitelist_page /
    command_whitelist_cleanup 正文改 {message}/{header+body} 直通（bot.list.* 已备 fallback，$l/$w 双语即时生效；
    custom 覆盖语义保留）
  - P4b 事件通知模板正文 → event.* key：player_join/quit/kick/digest、tnt_alert、exception_alert、security_audit、
    whitelist_block/whitelist_toggle_alert、geoip_block/unverifiable、review_submitted/approved/…、rank_promoted/demoted、
    command_guard_blocked 等（TemplateKeys 常量改 event.*、renderEvent/renderTemplate 路径梳理）
  - P4c maintenance 进度/MOTD：maintenance_motd_* → key、stage_cn → maintenance.stage.*、progress_units/world_alias/coord
    数据键评估、WorldMaintenance/ScheduledBackup 进度文案与 Region/Chunk/File/Done 阶段语义化、MaintenanceModeService.progressMessage
  - P4d legacy 服主定制正文自动迁 custom i18n 层（config-version 升级链）+ templates.yml 瘦身为格式/配色/数据
    （schema 升级只补缺不改值——改默认正文需走升级链，勿直接依赖磁盘覆盖）

## 未完成清单（顺序）
1. P4a 命令回复模板正文直通（上述四键）——小 PR，先做（$l/$w 双语立即可见）
2. P4b 事件通知模板正文 → event.* key（最大子卡，按事件域可再拆 1–2 PR）
3. P4c maintenance 进度/MOTD 文案
4. P4d legacy 定制迁移升级链 + templates.yml 瘦身
5. P5 收尾：全量 rg 中文残留审计（白名单：日志/异常/注释/config 描述/guide_book 等内容数据；
   已知残留：BlacklistCommandRegistrar 游戏命令域、templates.yml 待瘦身键）、YAML 布尔键排查（off/on 引号）、
   孤儿 MessageKeys/语言包 key、docs/features.md + README + CHANGELOG 同步、集成回归（含维护/审核/bot 双语实机冒烟）

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → P3d/P4 各 PR 触碰语言包须串行链式合入（基于最新 develop）
- templates.yml 是**运行时磁盘文件**（ConfigManager 首启复制资源后以磁盘为准）；schema 升级只「补缺不改值」，
  改默认正文只对全新安装生效，存量服需靠 P4d 升级链迁移——P4a/P4b 改资源正文 + 升级链一并评估
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）；登录前踢出无 locale → 默认语言；物品/即时消息按玩家 locale
- P4 前各 bot 命令输出：{message}/{patterns} 直通类已双语生效；字面正文类（$l/$w 头）仍 zh——冒烟验收注意区分
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P4a 开始：checkout 分支 feature/i18n-p4a-cmd-templates（基 origin/develop），
改 templates.yml 四个命令回复正文为直通 + BotCommandListFeedbackService 组合 {message}（bot.list.* 已备），
验收：affected :test 绿 + `spotlessApply && ./gradlew test` + `./gradlew :compileIntegrationTestJava`；
开 PR（base develop）后 CI 绿即 squash 合入，合入后更新本文件继续 P4b。
