# i18n 多语言迁移 交接
> 状态：现行 ｜ 最后更新：2026-09-08（P4b 完成，P4c 开卡）

## 任务与目标
仓库级 i18n 一期（中英双语 + 可扩展语言包）：游戏内 + Bot 交互 + 事件通知全部用户可见文案迁入语言包（zh 原文零回归基线）。方案：docs/dev/i18n-plan.md（§4 决策全拍板）。验收：见方案 §7。

## 已完成（倒序，develop @ 1026472）
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

## P4b 收尾快照（事件通知正文平移完成态）
- templates.yml 事件正文清零：P4b-1 10 键 + P4b-2 14 键 = 24 键全迁语言包 event.*（zh 逐字、en 初译占位符一致）
- renderEvent（磁盘正文优先→语言包回落）覆盖全部事件；TemplateKeys.EVENT_LANG_BACKED = 24 键；
  Templates 记录 player/geoip/whitelist/tnt/exception 字段成 vestige（无引用，待 P4c 后整体裁剪）
- **遗留（P4c/P5 分界）**：
  a) 存量服磁盘 templates.yml 事件正文仍 zh（config-version 升级「补缺不改值」不动现值）→ P4d 升级链
     识别「磁盘正文 == 旧内置默认」翻转为空（走语言包）或迁 custom 层，之后存量服才双语
  b) 孤儿语言包/MessageKeys：guard.alert_blocked / exploit.alert_blocked / ratelimit.alert_blocked /
     login.alert_ip_block / login.alert_name_block（P4b-2 改 renderEvent 后无引用）→ P5 收口清理
  c) 事件 var 值内联 zh：security_audit 的 online_mode/command_block/rcon/whitelist/ops/plugins 值、
     prison/guard reason 等部分 var 值仍 Java 内联 zh（正文已双语，数据值 zh 残留）→ P5 逐域评估按 var 迁移
  d) 游戏内 /blacklist 命令（BlacklistCommandRegistrar）P2 遗漏域，大量硬编码 zh → P5 审计统一迁移

## 进行中卡
- P4 事件/模板管线（拆四子卡，串行）：
  - P4a 命令回复模板正文直通 ✓（PR #386）
  - P4b 事件通知正文 → event.* ✓（PR #388 + #389，24 键）
  - P4c maintenance 进度/MOTD（**下一卡**）：maintenance_backup/optimize_* 六键事件正文 → event.*（沿用
    EVENT_LANG_BACKED + 磁盘正文优先机制，stage 进度渲染路径梳理）；maintenance_motd_* → key、
    stage_cn → maintenance.stage.*、progress_units/world_alias/coord 数据键评估、
    WorldMaintenance/ScheduledBackup 进度文案与 Region/Chunk/File/Done 阶段语义化、MaintenanceModeService.progressMessage
  - P4d legacy 服主定制正文自动迁 custom i18n 层（config-version 升级链）+ templates.yml 瘦身
    （含 Templates 记录 vestige 字段整体裁剪评估）
  - P4d legacy 服主定制正文自动迁 custom i18n 层（config-version 升级链）+ templates.yml 瘦身为格式/配色/数据
    （schema 升级只补缺不改值——改默认正文需走升级链，勿直接依赖磁盘覆盖）

## 未完成清单（顺序）
1. P4c maintenance 进度/MOTD 文案
2. P4d legacy 定制迁移升级链 + templates.yml 瘦身（Templates 记录 vestige 裁剪）
3. P5 收尾：全量 rg 中文残留审计（已知残留：BlacklistCommandRegistrar 域、事件 var 值内联 zh、
   Paginator 空列表 body、孤儿 alert_* 域键清理）、YAML 布尔键排查、docs/features/README/CHANGELOG 同步、
   集成回归（维护/审核/bot 双语实机冒烟）

## 风险/注意
- 语言包与 MessageKeys 为「文件尾追加」型文件 → P4 各 PR 触碰语言包须串行链式合入（基于最新 develop）
- P4b 后 templates.yml 事件正文清零（仅 maintenance/server 直通壳/命令回复壳）；ConfigHealthCheck/
  TemplateKeysTest 已豁免 EVENT_LANG_BACKED 键缺失 body
- templates.yml 是**运行时磁盘文件**（ConfigManager 首启复制资源后以磁盘为准）；schema 升级只「补缺不改值」，
  改默认正文只对全新安装生效，存量服需靠 P4d 升级链迁移——P4a/P4b/P4c 改资源正文 + 升级链一并评估
  （P4b 各 PR 均 config-version 不 bump：不触发无意义升级）
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）；登录前踢出无 locale → 默认语言；物品/即时消息按玩家 locale
- 事件 var 值内联 zh 不属正文平移范畴（P4b 只迁正文壳），勿在 P4c 顺手扩大范围，统一 P5 审计
- zh 主目录正文必须与迁移前逐字一致（测试断言按原文）；登录前踢出无 locale → 默认语言；物品/即时消息按玩家 locale
- P4a 后各 bot 命令输出已全量 {message} 直通（全新安装双语生效）；**存量服磁盘旧字面正文仍 zh**，
  冒烟验收区分：新装/重拷 templates.yml 才见双语，升级存量盘待 P4d
- 每域 PR：`spotlessApply && :test --tests <域>` → 全量 test + integration 编译绿 → PR；CI 绿即 squash 合入

## 下一棒开场指令
读 docs/dev/i18n-migration-handoff.md，从卡 P4c 开始：checkout 分支 feature/i18n-p4c-maintenance-templates（基 origin/develop），
先读 docs/dev/i18n-plan.md §3.7 与 docs/dev/folia-luckperms-gotchas.md §6 维护域相关；
事件正文部分：maintenance_backup_stage/done/error + maintenance_optimize_stage/done/error 六键 → 语言包
 event.*（zh 抄原文 + en），templates.yml 移除正文（沿用 EVENT_LANG_BACKED + renderEvent 磁盘正文优先机制），
 maintenance_motd_* 四键与 stage_cn/progress_units/world_alias/coord 数据键评估（勿删数据键）；
验收：affected :test 绿 + `spotlessApply && ./gradlew test` + `./gradlew :compileIntegrationTestJava`；
开 PR（base develop）后 CI 绿即 squash 合入，合入后更新本文件继续 P4d。
