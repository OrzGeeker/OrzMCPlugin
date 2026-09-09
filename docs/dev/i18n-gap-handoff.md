# i18n 遗留缺口补齐（P6）交接
> 状态：现行 ｜ 最后更新：2026-09-08
> 上级规划：docs/dev/i18n-plan.md §8（一期完成）；本文为「二期遗留审查 → 补齐」工程卡。

## 任务与目标
一期（P0–P5 + D7 + 真机双语验收）完成后，全面审查发现三类代码直出中文未 i18n。
目标：A（游戏内命令提示/描述）→ B（builtin IM 未绑定引导）→ C（/config 运维树）全部语言包化；
每卡验收 = affected :test 绿 + `spotlessApply && ./gradlew test` + `:compileIntegrationTestJava` + PR(base develop) CI 绿 squash。
已知 bug（已修 PR #414）：BotModule 未注入 I18nService → $ 群帮助恒 zh fallback（4ec7d53）。

## 决议政策（定稿）
- **游戏命令 description**（Paper `Commands.register(node, desc, aliases)` 的 desc → 玩家 /help）：注册期静态 → **default_lang（R1）**，用 `CommandFeedbackService.commandDescription(key)`。
- **游戏命令运行时提示**（「仅玩家可用」等 sendMessage）：**sender locale**（player → locale，否则 default），用 `CommandFeedbackService.message(sender,key,vars)` / `playerRequiredMessage(sender)`。
- 复用现成：`CommandFeedbackService`（features/command，common.* 键 P1）+ 拦截器已走它。
- 键命名空间：`cmd.desc.<name>`、`cmd.error.*`、`cmd.<topic>_<verb>`；MessageKeys 常量同步；语言包尾部追加（串行链式合入）。

## 已完成（按时间倒序）
- PR #414 @ b25d8d3：$ 群帮助注入真实 I18nService（装配断点修复；非本卡但前置）。
- G1 @ 未合（本轮分支）：CommandFeedbackService 加 String/描述/带变量渲染；ReviewCommandRegistrar 全量替换（desc ×2、仅玩家可用 ×7、review 异常外壳 ×1）；语言包 + MessageKeys 加 cmd.desc.apply/review、cmd.review_failed。

## 卡规划（依赖序；每卡单 PR，语言包尾部追加需串行）
- [ ] **G1** ReviewCommandRegistrar（本轮，行）
- [ ] **G2** FeatureCommandRegistrar（registerSimple 描述 guide/menu/bot/gamemode… + registerSimple「仅玩家可用」+ /bot 等子命令提示 + orzdebug 运维提示）+ FeatureModule 7 处
- [ ] **G3** BlacklistCommandRegistrar（45 处：/blacklist desc + 错误/列表提示；域键 `cmd.*` 或并入 access_rule 既有域）
- [ ] **G4** Rank/Portal/Prison/Update Registrar + UpdateCommandService（/update 状态输出 4-5 条，可先独立小 PR）+ UpdateModule 10
- [ ] **G5** B 面：builtin 未绑定会话绑定引导文本语言包化（Qq/Telegram/Feishu/Discord InboundProcessor 同构；公共渲染点或 4 平台键）
- [ ] **G6** C 面 /config 树（量最大）：ConfigCommandRegistrar 26 + ImCommandRegistrar 14 + ImAdminService 24 + OrzConfigCommand 46（/config 各子树说明）+ /orzdebug；可能拆 2-3 卡
- [ ] 真机双语验证（game 命令 zh↔en、builtin 未绑定引导）→ docs 更新 i18n-plan §8 台账 + features 小节 + CHANGELOG
- 豁免面（不补，记录在案）：logger/异常/内部健康描述（ConfigPath/ConfigHealthCheck/ConfigUpgrader）、config 校验消息、数据内容（templates/guidebook/config 值）、平台适配器内部状态。

## 语言包现状
zh-CN/en-US 双语 parity（I18nCatalogConsistencyTest）；common.* P1、cmd.* P6 起始；改语言包 PR 串行。

## 下一棒开场指令
「读 docs/dev/i18n-gap-handoff.md，从卡 G2 开始：checkout 分支（基 origin/develop），处理 FeatureCommandRegistrar/FeatureModule 中文直出，
desc → feedback.commandDescription(cmd.desc.<name>)，提示 → feedback.message(sender,…)，加 MessageKeys + 双语 cmd.desc.*；
`spotlessApply && :test --tests …` 全绿 → PR develop → CI 绿 squash → 刷新本文 → 卡 G3」
