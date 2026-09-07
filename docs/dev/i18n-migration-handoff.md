# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-08（P3d1 完成，P3d2 $v 开卡）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序，develop @ fbbebca）
- PR #381 P3d1：$e 日志溢出提示（bot.e.overflow）✓
- PR #380 P3c：$p 权限命令输出 + OrzUserCmd 内嵌中文收口 + RankService 过渡静态组名移除（bot.p.*，组名走 rank.group.*）✓
- PR #379 P3b：$w/$a/$r 白名单命令输出 + I18nServiceHolder（bot.list.* / whitelist.bot.*）✓
- PR #378 P3a：bot 帮助/用法核心（bot.help_*/desc/usage/params/examples/lbl_*）✓
- PR #377 docs：交接刷新（P2h1 完成；P3/P4/P5 范围细化）✓
- PR #376 P2h1 guide/menu ✓
- PR #375 P2g1 maintenance /maintenance 命令 ✓
- PR #374 P2f3 rank ✓
- PR #373 P2f2b review 业务流 ✓
- PR #372 P2f2a review 命令层 ✓
- PR #371 P2f1 样式收口 + prison/gamemode ✓
- PR #370 P2e security 域 ✓
- PR #368 P2d2 login 登录拦截（IP 黑名单/玩家名规则）✓ —— **features/player 域全部完成**
- PR #366 P2d1 player/geoip ✓
- PR #364 P2c portal/tnt ✓
- PR #363 P2b whitelist ✓
- PR #362 P2a teleport ✓
- PR #361 P1 common 拦截器 ✓
- PR #358 P0 基础设施（语言包加载/决议/custom 覆盖层/一致性护栏）✓
- PR #357 方案定稿 ✓
- 辅助 docs：#365（AGENTS 长任务/交接规范）、#367（交接文档首版）、#369（交接更新 P2d→P2e）
- 注：遗留本地分支 feature/i18n-p2g2-world / feature/i18n-p2h2-config 内容已全部含于 develop（旧卡片改名/重排），按收尾惯例可删。

## 进行中卡
- P3d2 $v 审核 handler（本卡）：ReviewCommandHandler 残留直发文案全部迁 `bot.v.*` 键
  （svc_unavailable / list_empty / list_header / current_group / list_item / reviewer_fallback /
  not_found / error / unknown_error / just_now / minutes_ago / hours_ago / days_ago；
  分页 meta 复用 bot.list.page_meta——需把 Paginator.paginate 改为 paginatePages 自行拼页头）；
  默认语言 R1 走 I18nServiceHolder.msg；emitMsg 辅助上移 BotCommandContext（PermissionCommandHandler 同步收口复用）；
  templates.yml command_review_list_empty 正文改 `{message}`（空列表文案才能走语言包）；
  审核决策结果保持 command_review_error/result 模板键语义（正文平移留 P4）。

## 未完成清单（顺序）
1. P3d2 $v 审核 handler（本卡）
2. P3d3 $d 黑名单 handler（BlacklistCommandHandler 残留直发文案迁 bot.d.*；AccessRule/PlayerNameRule
   规则命令反馈随 $d 一并迁移——P2e/P3 决议）
3. P4 事件/模板管线：templates.yml 正文 → event.* key（TemplateKeys 改 event.*、stage_cn → maintenance.stage.*、
   maintenance_motd_* → key、WorldMaintenance/ScheduledBackup 进度文案与 Region/Chunk/File/Done 阶段语义化一并迁移、
   MaintenanceModeService.progressMessage 文案）+ legacy 服主定制正文自动迁 custom 层（config-version 升级链）+
   templates.yml 瘦身为格式/配色/数据
4. P5 收尾：全量 rg 中文残留审计（白名单：日志/异常/注释/config 描述/guide_book 等内容数据）、
   YAML 布尔键排查（off/on 引号）、孤儿 MessageKeys/语言包 key、docs/features.md + README + CHANGELOG 同步、
   集成回归（含维护/审核/bot 双语实机冒烟）

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → P3d2/P3d3 必须串行链式合入（基于最新 develop），禁止并行攒批后统一合
- 登录前踢出（prelogin）无客户端 locale → 默认语言；物品/即时消息按玩家 locale
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）；$v 列表行/页头零回归基线见 P3c 前原样输出
- templates.yml 正文（zh 字面）在 P4 前仍优先于 i18n fallback——P3d 卡只改「{message} 直通」类文案，
  字面正文键留 P4 统一平移（本卡仅 list_empty 因无 message 直通需顺手改 {message}）
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P3d3 开始：checkout 分支 feature/i18n-p3d3-d（基 origin/develop），
改 features/botcommands/BlacklistCommandHandler（残留直发文案迁 bot.d.* + AccessRule/PlayerNameRule 规则命令反馈），
验收：affected :test 绿 + `spotlessApply && ./gradlew test` + `./gradlew :compileIntegrationTestJava`；
开 PR（base develop）后 CI 绿即 squash 合入（落后先 rebase origin/develop），合入后更新本文件继续 P4。
