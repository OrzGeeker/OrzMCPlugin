# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-08（P4d-1 升级链完成，P4d-2 收编开卡）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序，develop @ 39f6126）
- PR #395 P4d-1：templates.yml 存量盘旧正文升级链（config-version 13→14）——
  TemplatesBodyMigration：磁盘正文==旧默认（事件 30/motd 4/stage_cn/command_*4）→ 删键走语言包或翻 {message}；
  定制/变体保留零回归；旧默认比对事件/维护/阶段取 bundled zh 主目录（单一事实源）✓
- PR #393 P4c-2：maintenance 场景/MOTD 与阶段名迁语言包（maintenance.motd.* 4 键 +
  maintenance.stage.* 5 键）；新 MaintenanceTexts（磁盘正文优先→语言包回落）；renderMotdText 改走 texts、
  MaintenanceProgress 删 zh 字面 progressMessage（status 实时经语言包进度行渲染）；阶段显示名单源收口
  （TemplateResolvers.stageAlias/TemplateOptions.stageCnMap/stageDisplayCN 删除）；Templates 记录 motd 字段裁剪 ✓
- PR #391 P4c-1：maintenance 六键事件正文（backup/optimize_{stage,done,error}）迁语言包 event.*
  （沿用磁盘正文优先→语言包回落；EVENT_LANG_BACKED 扩至 30 键；templateForEvent 维护分支移除）✓
- PR #389 P4b-2：安全/审核/权限/坐牢事件正文迁入语言包 event.*（command_guard_blocked/security_audit/
  login_rate_limit_alert/exploit_blocked/ip_blacklist_block/player_name_block/review_*/rank_*/prison_* 14 键；
  ReviewNotifierAdapter.groupEvent 与安全域 5 服务改走 configs.renderEvent，删内联 zh fallback；
  guard/exploit/ratelimit/login.alert_* 域键成孤儿留 P5）✓
- PR #388 P4b-1：renderEvent 域事件正文迁语言包 event.*（player_join/quit/kick/digest、whitelist_block/toggle、
  geoip_block/unverifiable、tnt_alert、exception_alert 10 键；TemplateService.renderEvent 磁盘正文优先→
  语言包回落；EVENT_LANG_BACKED/ConfigHealthCheck 豁免；config-version 不 bump）✓ —— **P4b 全部完成**
- PR #386 P4a：$l/$w 命令回复模板正文直通（command_players/command_whitelist_header/command_whitelist_page/
  command_whitelist_cleanup 四键正文改 {message}，BotCommandListFeedbackService 按 bot.list.* 组装完整文案；
  占位符并集保历史磁盘/服主自定义零回归；config-version 不 bump，存量服正文留 P4d）✓
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

## P4c 收尾快照（templates.yml 文案迁移收口——可译文案全清零）
- **templates.yml 现仅剩：** 命令回复 {message} 直通壳（command_*）、server_load/server_stop {message} 直通壳、
  templates.format 格式表、styles.colors 配色、progress_units/world_alias/coord 数据键——全部非可译 UI 文案
- 可译文案去向：命令回复 → bot.*（P4a）；事件正文 30 键 → event.*（P4b + P4c-1）；维护场景/MOTD/进度行 →
  maintenance.motd.*；阶段名 → maintenance.stage.*（P4c-2，MaintenanceTexts 承载，磁盘正文优先→语言包回落）
- **遗留（P4d/P5 分界）**：
  a) 存量服磁盘 templates.yml 仍含旧正文（事件 30 键/motd/stage_cn）→ P4d 升级链识别「磁盘正文 == 旧内置默认」
     翻转（删正文走语言包）或迁 custom 层；服主真实定制保留——之后存量服才随 default_lang 双语
  b) Templates 记录（player/geoip/whitelist/tnt/exception/serverLoad/serverStop 字段）与 templateForEvent/
     ConfigHealthCheck 豁免逻辑：P4d 评估整体裁剪/收编（事件正文已全走 renderEvent 磁盘→语言包）
  c) 孤儿语言包/MessageKeys：guard/exploit/ratelimit/login.alert_*（P4b-2 改 renderEvent 后无引用）→ P5
  d) 事件 var 值内联 zh：security_audit 描述等 → P5；/blacklist 游戏命令域（BlacklistCommandRegistrar，P2 遗漏，
     需 sender locale 决议）→ P5；Paginator 空列表 body zh → P5
  e) WorldMaintenanceService 内联 zh 仍多（callback 提示/日志/“损坏区块”汇总等，控制台日志域按 D1 不做）

## 进行中卡
- P4 事件/模板管线全部完成；P4d 拆两棒：P4d-1 升级链 ✓（PR #395，config-version 14），**P4d-2（下一卡）**：
  Templates 记录 vestige 收编（见「下一棒开场指令」）

## 未完成清单（顺序）
1. P4d-2 Templates 记录 vestige 收编：Templates record 事件字段（player/geoip/whitelist/tnt/exception/
   maintenance，P4 后零消费）删除；serverLoad/serverStop {message} 直通壳评估（并入语言包 or 保留直通壳）；
   templateForEvent/DefaultTypedConfigProvider.templates()/ConfigHealthCheck 豁免逻辑同步收编；连带清理
   TemplatesBodyMigration 事件比对在 renderEvent 侧的兜底一致性（升级后存量服事件键磁盘缺失 → 语言包，已闭环）
2. P5 收尾：全量 rg 中文残留审计（已知残留：BlacklistCommandRegistrar 游戏命令域、事件 var 值内联 zh
   security_audit 描述、server_load 组装片段 zh、Paginator 空列表 body、孤儿 guard/exploit/ratelimit/login.alert_*
   域键清理）、YAML 布尔键排查、docs/features/README/CHANGELOG/plan §7 同步、
   集成回归（维护/审核/bot 双语实机冒烟：新装 vs 13→14 升级存量盘对照）

## 风险/注意
- **升级链已闭环（P4d-1）**：config-version 现为 14；存量盘 13 → 首次启动自动迁移正文（备份 .bak）；
  磁盘已 14 或全新安装（bundled 14）零动作。TemplatesBodyMigration 幂等，重复调用零操作
- Templates 记录 18 个事件字段目前仍由 DefaultTypedConfigProvider.templates() 构建（vestige，仅
  templateForEvent 用 serverLoad/serverStop）→ P4d-2 裁剪前先 rg 确证零消费
- 语言包与 MessageKeys 为「文件尾追加」型文件 → 触碰语言包的 PR 串行链式合入
- zh 主目录正文必须与迁移前逐字一致；登录前踢出/MOTD 无 locale → 默认语言 R1（D6 不变）
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入；
  **禁止直接 commit/push develop**（#395 曾误落 develop，已分支纠正——先落卡到 feature 分支再 PR）

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P4d-2 开始：checkout 分支 feature/i18n-p4d2-templates-trim（基 origin/develop），
先 rg 确证 Templates record 各事件字段零消费（playerJoin/playerQuit/playerKick/playerDigest/exceptionAlert/
geoipBlock/geoipUnverifiable/tntAlert/whitelistBlock/whitelistToggleAlert/commandGuardBlocked 等——
templateForEvent 现仅用 serverLoad/serverStop）；改动：
① Templates record 收窄为仅 serverLoad/serverStop（或并入语言包直通壳评估）；
② DefaultTypedConfigProvider.templates()/renderEvent 签名相应瘦身（renderEvent 的 Templates 参数如不再用可移除）；
③ ConfigHealthCheck/任何残留引用清理；④ 连带：确认 TemplatesBodyMigration 与 renderEvent 磁盘优先机制在
   升级后闭环（存量盘事件键已删 → 语言包；有定制 → 磁盘仍在渲染）；
验收：affected :test 绿 + `spotlessApply && ./gradlew test` + `./gradlew :compileIntegrationTestJava`；
开 PR（base develop）后 CI 绿即 squash 合入，合入后更新本文件继续 P5。
