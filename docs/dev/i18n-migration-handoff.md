# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-08（P4c-1 完成，P4c-2 开卡）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序，develop @ 8784cbc）
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

## P4c-1 收尾快照（维护事件正文平移完成态）
- templates.yml 事件正文清零（24 键 P4b + 6 键 P4c-1 = 30 键）；剩 server_load/server_stop {message} 直通壳
  （记录承载，不迁）+ maintenance_motd_*/stage_cn/progress_units/world_alias/coord 场景与数据键
- **遗留（P4c-2/P4d 分界）**：
  a) maintenance_motd_*（motd_backup/optimize/manual/progress_line）仍是 templates.yml zh 正文，经
     MaintenanceModeService.renderMotdText 渲染（登录拦截/踢人/MOTD/维护命令提示）→ P4c-2 迁语言包
     maintenance.motd.*（预登录默认语言 R1，与 whitelist.kick 同语义）
  b) stage_cn（Region/Chunk/File/Done）手工映射表 → maintenance.stage.*；stageDisplayCN 内联 zh
     与 TemplateResolvers.stageAlias（TemplateOptions.stageCnMap）两处引用待收口 → P4c-2
  c) 维护进度 body 的 {stage}/{stage_name}/{stage_i18n} 数据 var（MaintenanceStage 枚举名/原始阶段名/
     本地化阶段名）语义评估 → P4c-2
  d) Templates 记录（player/geoip/whitelist/tnt/exception/maintenance 字段）已成 vestige → P4d 整体裁剪
  e) （承接 P4b 收尾）存量盘正文 P4d 迁移；孤儿 alert_* 域键与事件 var 值 zh、BlacklistCommandRegistrar 域 P5

## 进行中卡
- P4 事件/模板管线（拆子卡，串行）：
  - P4a 命令回复模板正文直通 ✓（PR #386）
  - P4b 事件通知正文 → event.* ✓（PR #388 + #389，24 键）
  - P4c-1 maintenance 六键事件正文 → event.* ✓（PR #391）
  - P4c-2 maintenance 场景/MOTD + 阶段语义化（**下一卡**）：maintenance_motd_* 四键 → 语言包
    maintenance.motd.*（renderMotdText 改磁盘优先→语言包回落；LoginAccessControl/ServerFeedback/
    WorldMaintenance/MaintenanceCommand 四处调用方梳理）；stage_cn（Region/Chunk/File/Done）→
    maintenance.stage.*（TemplateResolvers.stageAlias/TemplateOptions.stageCnMap/stageDisplayCN 收口）；
    progress_units/world_alias/coord 数据键评估（勿删）；{stage}/{stage_name}/{stage_i18n} var 语义化
  - P4d legacy 服主定制正文自动迁 custom i18n 层（config-version 升级链）+ templates.yml 瘦身
    （Templates 记录 vestige 整体裁剪）
  - P4d legacy 服主定制正文自动迁 custom i18n 层（config-version 升级链）+ templates.yml 瘦身为格式/配色/数据
    （schema 升级只补缺不改值——改默认正文需走升级链，勿直接依赖磁盘覆盖）

## 未完成清单（顺序）
1. P4c-2 maintenance 场景/MOTD + 阶段语义化（motd_* → maintenance.motd.*、stage_cn → maintenance.stage.*）
2. P4d legacy 定制迁移升级链 + templates.yml 瘦身（Templates 记录 vestige 裁剪）
3. P5 收尾：全量 rg 中文残留审计（已知残留：BlacklistCommandRegistrar 域、事件 var 值内联 zh、
   Paginator 空列表 body、孤儿 alert_* 域键清理）、YAML 布尔键排查、docs/features/README/CHANGELOG 同步、
   集成回归（维护/审核/bot 双语实机冒烟）

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → P4 各 PR 触碰语言包须串行链式合入（基于最新 develop）
- P4c-1 后 templates.yml 事件正文清零（剩 server_load/server_stop 直通壳 + maintenance_motd_*/stage_cn/
  progress_units/world_alias/coord 场景与数据键）；ConfigHealthCheck/TemplateKeysTest 已豁免 EVENT_LANG_BACKED 键缺失 body
- templates.yml 是**运行时磁盘文件**（ConfigManager 首启复制资源后以磁盘为准）；schema 升级只「补缺不改值」，
  改默认正文只对全新安装生效，存量服需靠 P4d 升级链迁移——P4a/P4b/P4c 改资源正文 + 升级链一并评估
  （各 PR 均 config-version 不 bump：不触发无意义升级）
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）；登录前踢出无 locale → 默认语言；物品/即时消息按玩家 locale
- 事件 var 值内联 zh：阶段/stage 相关 var（stage_i18n 等）在 P4c-2 与 stage_cn 迁移一并收口；
  其余数据 var 值（security_audit 描述等）勿在维护卡顺手扩大范围，统一 P5 审计
- P4a 后各 bot 命令输出已全量 {message} 直通（全新安装双语生效）；**存量服磁盘旧字面正文仍 zh**，
  冒烟验收区分：新装/重拷 templates.yml 才见双语，升级存量盘待 P4d
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P4c-2 开始：checkout 分支 feature/i18n-p4c2-maintenance-motd（基 origin/develop），
先读 docs/dev/i18n-plan.md §3.7 与 MaintenanceModeService/WorldMaintenanceService/LoginAccessControlService（buildRejectText）/ServerFeedbackService 现状；
① motd：maintenance_motd_backup/optimize/manual/progress_line 四键正文 → 语言包 maintenance.motd.*（zh 抄原文 + en；
   预登录默认语言 R1），MaintenanceModeService.renderMotdText 改磁盘优先→语言包回落（需 cfg 或解析器注入，勿破坏
   服主自定义磁盘模板）；② stage：stage_cn（Region/Chunk/File/Done + 进行中兜底）→ 语言包 maintenance.stage.*，
   TemplateResolvers.stageAlias / TemplateOptions.stageCnMap / WorldMaintenanceService.stageDisplayCN 收口单源；
   进度 body {stage}/{stage_name}/{stage_i18n} var 语义化；progress_units/world_alias/coord 数据键评估（勿删）；
验收：affected :test 绿 + `spotlessApply && ./gradlew test` + `./gradlew :compileIntegrationTestJava`；
开 PR（base develop）后 CI 绿即 squash 合入，合入后更新本文件继续 P4d。
