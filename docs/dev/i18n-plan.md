# 多语言（i18n）方案：中英双语起步、可扩展语言包

> **状态：方案草案（待 owner 评估）**｜**最后更新**：2026-09-16
> **范围**：一期英文 + 中文；架构上预留第三语言扩展（法语/日语/…）只需新增语言包文件。
> **配套**：本方案为纯计划文档，不含代码改动；拍板后按 §5 拆实施 PR。

## 0. 现状盘点（证据）

| 渠道 | 载体 | 现状 |
|:--|:--|:--|
| 游戏内玩家消息（命令反馈/事件提示/踢出/弓 lore/菜单/引导） | Adventure `Component`，代码内联中文 + `OrzTextStyles` 上色 + hover/click | **~绝大多数中文硬编码在 Java 里** |
| 群/私聊 Bot 消息（交互回复 + 事件通知） | 纯文本 `MessageEnvelope` | 交互回复中文硬编码在代码；**事件通知正文已外置 templates.yml**（`{var}` 占位 + 每事件格式，见 §0.1） |
| 控制台/日志 | Logger | 中文（管理员受众，默认不迁，见 D1） |
| 内容数据文件 | guide_book.yml / 白名单踢出 UP 名单 / world_alias / stage_cn / progress_units | 用户自维护内容或**非语言数据**，默认不迁（见 §1 非目标） |

规模量化：主代码 328 个 Java 文件，**276 个含中文**；引号内中文串字面量约 **1119 处**（含日志/异常消息，剔除后用户可见文案估计 600–800 key）。抽样式本：`TeleportBowService` 21 处、`WhitelistEventService` 9 处、botcommands 每 handler 3–15 处。

### 0.1 已有的「半成品 i18n」——templates.yml

事件通知正文已外置模板文件（约 35 个事件键，如 `review_approved: "✅ [申请通过] {player}\n..."`），由 `TemplateService`/`TemplateRenderer` 用 `{var}` 渲染成 `MessageEnvelope`；另有 `stage_cn: {Region: 区域…}` 这类**手工映射表**（`stage_i18n` 占位符已在为英文铺垫）与 `styles.colors` 配色。这说明仓库已隐含「文案外置 + 占位符」心智，但**只有 bot 事件通知走这条路**，且只支持一套中文。

### 0.2 对方案有利的既有机制

- `ConfigManager.registerConfig(name, fileName)`：数据目录缺文件时从 resources 拷贝默认 → **语言包可直接复用这套「内置默认 + 管理员可改 + reload」机制**；
- `ConfigService.reloadAll()` / `reloadConfig(name)`：热重载回调链已存在；
- `ConfigUpgrader` + `config-version`：语言包未来增键可版本化升级；
- `ConfigHealthCheck`：启动健康检查 issues 列表 → 语言包一致性校验挂这里；
- `NotifierSink`/`CapturingSink`：通知测试替身，改文案不影响测试结构。

## 1. 目标与非目标

**目标（一期）**
1. 全部**用户可见文案**（游戏内 + Bot 交互回复 + Bot 事件通知）可中英切换；默认 zh-CN，**老服务器零改动、现网中文文案一字不变**（语言包 zh 正文 = 现代码原文，向后兼容）。
2. 架构上语言 = 插件级可插拔资源：**加第三语言 = 新增一个 yml + 一行注册**，不动业务代码。
3. 语言决议分层可配：游戏内玩家（客户端 locale）→ 群（平台级配置）→ 服务器默认 → 内置兜底。
4. 一致性护栏：key 集/占位符集跨语言强校验（启动健康检查 + 单测），杜绝英文包漏译导致的空消息/坏消息。

**非目标（明确不做，防范围膨胀）**
- 控制台/日志本地化（管理员受众，跟随默认语言写死中文，二期可议）。
- 用户自维护内容（guide_book 正文、白名单踢出 UP 名单、world_alias 地图别名、进度单位缩写）——**内容/数据不是文案**，翻译职责在服主。
- 命令、权限节点、配置键本身的多语言（终端用户不直接见）。
- Bot 命令解析词（`$w`/`$v` 等）的多语言别名（见 §3.6 备注，一期不做）。

## 2. 总体设计

**翻译单元 = 一条完整用户消息或完整句子，带 `{var}` 占位符；代码侧只保留「组装与样式」职责，文案全部外置。**

```
业务代码 ──msg(Lang, key, vars)──▶ I18nService ──▶ messages_<lang>.yml（语言包）
   │  ▲                                   │
   │  │ Lang 决议                          ├─ zh-CN（主目录，作者=开发者，迁自代码原文）
   │  │                                    ├─ en-US（翻译，作者=owner/校对）
   │  └── 游戏内: Player.locale()          └─ <未来第三语言>：加文件即扩展
   │      群聊  : i18n.platform_langs[平台]
   │      兜底  : i18n.default_lang → zh-CN
   ▼
样式层（OrzTextStyles / 组件组装 / MessageEnvelope 格式）保持现状不动
```

### 2.1 文本渲染模型：A1（推荐）——抽取文案、保留代码样式组装

| | A1 文案抽取 | A2 全量 MiniMessage 模板化 |
|:--|:--|:--|
| 做法 | key → 纯文本 + `{var}`；颜色/样式仍由 `styles.warn(...)` 等代码包 | 文案内联 MiniMessage 标签（`<red>…`），样式并入语言包 |
| 依赖 | 无新增（复用 TemplateRenderer 占位符引擎思路） | 新增 adventure-minimessage |
| 改造面 | 每处「中文串 → `i18n.msg(lang, key, vars)`」，代码结构不动 | 全部 Component 组装重写为模板串 |
| 样式责任 | 留在代码/`templates.yml styles`（现设计） | 移给翻译者，易乱、难审 |
| 语序自由度 | 受限：个别「多色分段长句」按语义拆 key，段落顺序由代码定 | 翻译者可整体重排 |
| 适用 | **一期推荐** | 二期选项（第三语言出现、翻译者专业分工后再议） |

> A1 的已知短板（跨语言句子重排）只影响**极少数**多色富文本消息（如白名单踢出「QQ 群 + Discord + UP 名单」拼装）。处理规则：此类消息**按语义角色拆成多个 key、由代码决定拼装顺序与样式**（现状本就是代码拼装），文案层面不做整句重排。普通整句消息（绝大多数）单 key 单句式，翻译无碍。

### 2.2 语言包文件与加载

- **文件名/注册**：`messages_zh-CN.yml`、`messages_en-US.yml` 放 `src/main/resources/`，启动时 `registerConfig("messages_zh-CN", "messages_zh-CN.yml")` 等；数据目录缺文件自动拷贝内置默认。`I18nService` 启动时枚举 `messages_*.yml` 得到**已安装语言集**（加语言 = 加资源 + 一行注册）。
- **文件结构**（YAML 嵌套按域分组、代码按完整 dot key 取值，与现 config 读取风格一致）：
```yaml
config-version: 1
messages:
  common.admin_required: "仅管理员可执行该命令"
  common.cooldown: "命令冷却中，请 {secs} 秒后再试"
  teleport.bow.name: "传送弓"
  teleport.bow.give: "你获得了 {name}"
  whitelist.kick.join_hint: "不在服务器白名单中，请先加入QQ群：{group}，联系管理员添加白名单"
  bot.event.review_approved: "✅ [申请通过] {player}\n{summary}\n审核人：{reviewer}"
```
- **key 规范**：`域.子域.名词`；跨渠道复用的通用反馈（权限/冷却/用法/无效参数…）归 `common.*`，其余归各自域（`teleport.*`/`whitelist.*`/`review.*`…），**渠道不进 key**（同一条「无权限」游戏内和群里共用）。
- **占位符**：沿用 templates.yml 的 `{name}` 语法（含 `\n` 转义），渲染引擎与 TemplateRenderer 同一实现思路。
- **热重载**：`I18nService` 缓存「语言 → 不可变 Map」；`reloadAll()` 时原子换新 Map（读线程无感，天然线程安全，适配 Folia 多线程）。

### 2.3 语言决议（Lang 解析优先级）

| 场景 | 决议链（先命中先得） | 说明 |
|:--|:--|:--|
| 游戏内命令/事件反馈 | `Player.locale()` → `i18n.default_lang` → `zh-CN` | Paper 提供客户端 locale；玩家不可改服务端语言 |
| Bot 交互回复 | 会话来源平台 → `i18n.platform_langs[平台id]` → `i18n.default_lang` → `zh-CN` | 平台 id 沿用 im.yml 的 `qq/discord/telegram/feishu/wechat` |
| Bot 事件通知广播 | `i18n.default_lang`（R1 简单路线，见 §3.5） | 通知在调用侧渲染一次后广播，一期不按目标会话分语言 |
| 兜底 | 请求语言不在已安装集 → 归一化后仍无 → `default_lang` → `zh-CN` | 保证永不为空 |

- **语言归一化**：大小写/`_`→`-` 归一（`zh_CN`→`zh-CN`）；基础子码唯一命中时可用（`en`→`en-US`、`zh`→`zh-CN`）；`zh-TW`/`de-DE` 等未装语言一律落 `default_lang`（可在 `i18n.aliases` 手配指向，如 `zh-TW: zh-CN`）。
- **新增配置段**（config.yml，`i18n:` 域，带 schema 版本升级）：
```yaml
i18n:
  default_lang: zh-CN      # 服务器默认语言（事件通知 / 游戏内非玩家 / bot 兜底）
  platform_langs:          # 群语言覆盖（bot 交互回复）
    qq: zh-CN
    discord: en-US
  # aliases: {zh-TW: zh-CN}   # 可选：未装语言指向
```

### 2.4 I18nService API 草案（装配点：OrzServices，注入各 Feature）

```java
public final class I18nService {
    Set<String> installedLangs();                       // ["zh-CN","en-US",...]
    String normalize(String raw);                       // "zh_CN"→"zh-CN"，未装→null
    String resolve(Player p);                           // 游戏内：locale→default→zh-CN
    String resolve(String platformId);                  // bot：platform_langs→default→zh-CN
    boolean has(String lang, String key);
    String msg(String lang, String key);                // 无参
    String msg(String lang, String key, Map<String,String> vars);
}
```
- 调用面约定：**在渠道边界解析一次 `lang`**，向下传给消息组装（不层层现查 locale）；字符串拼接处直接 `i18n.msg(lang, key, vars)`。
- zh 主目录由开发者维护（迁代码原文）；en-US 由翻译者维护（来源见 §5 决策 D7）。

### 2.5 一致性护栏（防漏译/坏文案）

| 机制 | 内容 | 触发时机 |
|:--|:--|:--|
| key 集校验 | 每语言包与 zh 主目录 key 集完全一致（缺/多都报） | 启动 `ConfigHealthCheck` + 单测 |
| 占位符校验 | 同 key 跨语言 `{...}` 集合一致（防英文版漏 `{player}` 产生 `{player}` 字面量） | 同上 |
| 运行时兜底 | 命中缺失 key → 返回 key 本身（可见、易定位）+ 按 key 去重 warn 一次（防刷屏） | 运行时 |
| 单测 | `I18nCatalogConsistencyTest`：资源内置两包 key/占位符一致；归一化与决议链用例 | `./gradlew test` |

> zh 主目录 = 现网文案原文，绝大多数现有断言中文串的测试**期望值不变**，迁移对测试的影响仅限「常量改走 i18n 后构造方式」的机械替换。

### 2.6 templates.yml 事件通知正文的去向

事件通知键（`review_approved` 等约 35 个，正文含真实文案者）**正文迁入语言包**（key 落 `bot.event.*`），templates.yml 只保留两类**非文案**内容：`templates.format` 格式表（CODE_BLOCK/PLAIN）与 `styles.colors` 配色。`{message}` 直通壳（`command_output` 等）原地不动。迁移后事件渲染路径改为 `I18nService` 取正文 → 原 TemplateRenderer 变量填充 → `MessageEnvelope`。

### 2.7 与现有事件通知的关系（待 D8 拍板）

- R1（推荐，一期）：事件通知在调用侧按 `default_lang` 渲染一次——跨平台事件（QQ 群 + Discord 频道同收一条）共享服务器语言。交互回复不受影响（§2.3 已按平台分语言）。
- R2（二期选项）：`MessageEnvelope` 改携带 key+vars，**发送边界**按目标会话语言延迟渲染——多语言社区按会话分语言的终极形态，需动通知管线与绑定表，一期不做。

## 3. 决策点（owner 拍板，勿留模糊）

| # | 项 | 推荐 | 备选 |
|:--|:--|:--|:--|
| D1 | 一期范围 | 游戏内玩家消息 + Bot 交互回复 + Bot 事件通知正文；**不含**控制台日志、内容数据文件 | 缩到只做游戏内+交互回复，事件通知正文二期 |
| D2 | 默认语言 | **zh-CN**（老服零改动、兼容现网） | en-US（新服英文向，不推荐：迁移期中文包是主目录） |
| D3 | 渲染模型 | A1 文案抽取（无 MiniMessage 依赖） | A2 全模板化（二期再议） |
| D4 | 语言包位置 | resources + 数据目录同名 yml，复用 ConfigManager/热重载/版本升级 | 纯代码内嵌（不可改，放弃管理员微调） |
| D5 | 兜底语言 | 请求语言未装 → default_lang → **zh-CN**（zh 是 key 完整性基线） | en-US 兜底 |
| D6 | 游戏内语言决议 | 跟随客户端 locale（Paper `Player.locale()`） | 仅服务器默认语言（不做 per-player） |
| D7 | **英文文案来源** | owner 提供/校对；AI 初译 + owner 审校 | 纯 AI 直出不审（不推荐：群通知面向玩家社区） |
| D8 | 事件通知多语言形态 | R1（默认语言渲染一次） | R2 延迟渲染（二期） |

## 4. 分阶段实施 PR 计划（遵守「单 PR < ~500 行」成本红线）

| 阶段 | 内容 | 规模估算 | 验收 |
|:--|:--|:--|:--|
| P0 基础设施 | `I18nService` + Lang 归一化 + `i18n:` 配置段 + zh 空主目录 + en 骨架 + 健康检查/一致性单测 + 装配注入 | 1 PR，~400–600 行 | 决议链/兜底/一致性护栏全绿；全量文案未迁时不缺 key |
| P1 高复用通用文案 | `common.*` + 拦截器提示（cooldown/admin/usage）+ `OrzConfigCommand` 等命令反馈；**连带迁移对应调用点** | 1–2 PR | 中英两包 key 一致；游戏内切换语言可见变化 |
| P2 分域迁移（每域 1–3 PR，域内测试同步改） | 建议顺序：teleport → whitelist → portal/tnt → player 事件聚合 → security/chat → review/rank/prison → maintenance → guide/menu | ~8–12 PR | 每域：中文输出与迁移前逐条一致（diff 比对）；en 包补齐 |
| P3 botcommands 大块 | 11 个 `$cmd` 帮助/反馈/列表（含 `CommandOutputAssembler` 分页、分隔线） | 2–4 PR | bot 交互回复双语验证（含 $cmd ? 查询） |
| P4 事件通知正文迁移 | templates.yml 正文 → `bot.event.*` key，templates.yml 瘦身为格式+配色；templates 记录类字段裁剪 | 1–2 PR（含回填 `stage_i18n` 处） | 通知格式/路由不变；双语样例实机验证 |
| P5 收尾 | 全量 key 审计（无孤儿 key/无残留中文字面量）、README/docs/features 同步、CHANGELOG | 1 PR | `rg` 中文残留清单清零（白名单除外项） |

> 估算总计 **15–22 个小 PR**，全部走 develop；每 PR 独立可合。zh 包随迁移同步从代码原文抄入（保证中文输出零回归）；en 包可在各域 PR 中分批补齐（D7 依赖 owner 校对节奏，可滞后合入不影响 zh 先行）。

## 5. 风险与边界（迁移期红线）

1. **测试回归**：多数测试断言现网中文原文 → zh 包抄原文后**期望值不变**；风险集中在「常量折叠的字符串被 i18n 调用取代后测试里硬编码的期望与 key 文案漂移」，靠 §2.5 占位符校验 + 每域 PR 自测兜住。
2. **漏网中文**：`rg '"…中文…"'` 逐 PR 清零；迁移清单以引号内中文串为准（日志/异常消息白名单除外，D1）。
3. **频控红线不回归**：通知仍走 `ThrottledNotifier`/聚合，i18n 只换文案来源，不动节流路径。
4. **Folia 线程**：I18nService 只读不可变 Map + reload 原子换引用，无同步等待；调用均在既有线程模型内，不新增调度。
5. **翻译质量**：英文包面向玩家社区，D7 需要 owner 校对门槛；语法/占位符错漏由护栏拦截结构性错误，语义由人审。
6. **成本控制**：P2 分域小 PR 单审；迁移是机械替换（中文串→key+抄入 zh 包），AI 可批量但**每域独立提交**，禁止一次大 PR 推全量。
7. **与 im-gateway/其他在途分支的冲突面**：本方案不触碰 `im.yml` 绑定表、不改消息路由，冲突面小；P4 裁剪 Templates 记录类字段时注意与在途改动同步。

## 6. 验收标准（一期完成定义）

- [ ] 内置 `messages_zh-CN.yml` / `messages_en-US.yml`，加第三语言仅需新增 yml + 一行注册（文档示例验证）。
- [ ] 游戏内：中文客户端见中文、英文客户端见英文（未装语言客户端落默认）；命令反馈/事件提示全覆盖无残留中文。
- [ ] Bot：交互回复按 `i18n.platform_langs` 分语言；事件通知按默认语言；`$cmd ?` 帮助全量双语。
- [ ] 一致性护栏全绿：启动健康检查无 key/占位符差异；`I18nCatalogConsistencyTest` 随 `./gradlew test` 通过。
- [ ] 管理员改数据目录语言包 + reload 即时生效；缺 key 时输出 key 本体可定位，不产生空消息。
- [ ] 中文输出与迁移前逐条一致（回归 diff）；README / docs/features.md / CHANGELOG 已同步。
