# 权限组节点配置表（Rank & Review）

> 基于本地测试服（与线上插件一致：EssentialsX 2.22.0/GetMeHome 3.0.0-4/GriefPrevention/WorldEdit 7.4.4/WorldGuard 7.0.18/DeathChest/EzShops 2.5.9/BackOnDeath/LoginSecurity 3.3.2/OrzMC 等 16 插件）**逐权限名核对 plugin.yml 与 jar 字节码后**设计。
> 配置命令：`lp group <组> permission set <节点> true`（LP 继承链 admin→builder→member→default，各组只配增量）。
> **线上同步时逐条执行本表命令即可**（同步前先 `lp export` 备份）。
> **⚠️ 继承链（parent）必须一并设置**：`lp group member parent set default`、`lp group builder parent set member`、`lp group admin parent set builder`——LuckPermsBootstrap 已自动校正（启动时校验/修正，不动权限节点）。

## 设计原则

1. **最小化**：能用父权限/通配符合并的绝不分列（getmehome.user 含全部家命令、worldedit.* 系列通配）
2. **只配插件真实检查的权限名**：已逐项核对 plugin.yml/字节码，无效节点（essentials.reply/craft/teleport 等）已剔除
3. **不含管理侧**：任何组的通配符都避开管理分支（worldedit.reload、worldguard.region.bypass、essentials.gamemode.others 等）
4. **权限组内其它细节由线上自管**：本表只保证「定位功能可用」，不覆盖线上自定义

## L0 default（访客）— 生存基础体验（14 项）

| 权限节点 | 验证指令 | 预期 |
|:--|:--|:--|
| `ls.bypass` | `lp group default permission check ls.bypass` | true |
| `essentials.afk` | `/afk` | 「你暂时离开了」 |
| `essentials.back` | `/back`（死亡后） | 传送回死亡点 |
| `essentials.msg` | `/msg <玩家> hi` | 对方收到 |
| `essentials.balance` | `/balance` | 显示余额 |
| `essentials.balancetop` | `/baltop` | 显示排行 |
| `essentials.pay` | `/pay <玩家> 1` | 转账成功 |
| `essentials.spawn` | `lp group default permission check essentials.spawn` | true（命令由服务器 26.2 兼容问题另行解决） |
| `bod.back` | 死亡后重生 | 死亡箱/回档提示 |
| `ezshops.shop` | `/shop` | 打开商店 GUI |
| `ezshops.shop.buy` | 商店点购买 | 购买成功 |
| `ezshops.shop.sell` | 商店点出售 | 出售成功 |
| `ezshops.playershop.browse` | `/playershops` | 浏览玩家商店 |

> 剔除项：`getmehome.user`（家功能属 member，default 不给）、`deathchest.command.report`（管理命令，位于 DeathChest admin 包）、`essentials.reply`（无此权限，/reply 随 /msg）。

## L1 member（成员）— 完整玩家功能（12 项）

| 权限节点 | 验证指令 | 预期 |
|:--|:--|:--|
| `getmehome.user` | `/sethome`、`/home`、`/delhome`、`/listhomes` | 全部可用（父权限含 5 个家命令） |
| `essentials.tpa` | `/tpa <玩家>` | 传送请求发出 |
| `essentials.tpahere` | `/tpahere <玩家>` | 邀请对方 |
| `essentials.warp` | `/warp test` | 传送到传送点 |
| `essentials.warp.list` | `/warp` | 列出传送点（2026-08-08 验收补：/warp 无参需 list） |
| `essentials.kit` | `/kit` | 显示可用补给包 |
| `essentials.mail` | `/mail` | 邮件基础 |
| `essentials.mail.send` | `/mail send <玩家> hi` | 发送成功（2026-08-08 验收补：send 为独立子权限） |
| `griefprevention.createclaims` | 木铲圈地 | 成功圈地 |
| `griefprevention.trapped` | `/trapped` | 触发卡死传送 |
| `ezshops.playershop.create` | 牌子创建商店 | 创建成功 |
| `ezshops.playershop.buy` | 玩家商店购买 | 购买成功 |
| `ezshops.playershop.sell` | 玩家商店出售 | 出售成功 |

> 剔除项：`essentials.spawn`（继承自 default，不重复列）；5 个 `getmehome.command.*` 分列节点 → 合并为 `getmehome.user`（省 5 项）。

## L2 builder（建造者）— WE/WG 裁剪子集 + 建造便利（26 项）

| 权限节点 | 验证指令 | 预期 |
|:--|:--|:--|
| `worldedit.wand` | `//wand` | 获得木斧 |
| `worldedit.selection.*` | `//pos1`、`//expand 10` | 选区成功 |
| `worldedit.region.*` | `//set stone` | 填充成功 |
| `worldedit.clipboard.*` | `//copy`、`//paste` | 复制粘贴成功 |
| `worldedit.history.*` | `//undo` | 撤销成功 |
| `worldedit.brush.*` | `//brush sphere stone` | 笔刷设置成功 |
| `worldedit.tool.*` | `//tool` | 工具列表 |
| `worldedit.utility.*` | `//fill`、`//drain` | 工具命令可用 |
| `worldedit.help` | `//help` | 显示帮助 |
| `worldedit.schematic.*` | `//schem save test` | 保存成功 |
| `worldedit.navigation.*` | `//unstuck` | 脱出卡墙 |
| `worldedit.analysis.*` | `//count stone` | 显示统计 |
| `worldguard.region.claim.*` | `//claim`、`/rg claim` | 圈地成功（含 claim.own） |
| `worldguard.region.define` | `/rg define test` | 创建区域 |
| `worldguard.region.remove` | `/rg remove test` | 删除区域 |
| `worldguard.region.addmember` | `/rg addmember test <玩家>` | 添加成员 |
| `worldguard.region.removemember` | `/rg removemember test <玩家>` | 移除成员 |
| `worldguard.region.setparent` | `/rg setparent test parent` | 设置父区域 |
| `worldguard.region.flag.*` | `/rg flag test pvp deny` | 设置旗标 |
| `worldguard.region.list` | `/rg list` | 显示区域 |
| `worldguard.region.info` | `/rg info test` | 显示区域详情 |
| `worldguard.region.teleport` | `/rg tp test` | 传送到区域 |
| `essentials.gamemode.creative` | `/gamemode creative` | 切换创造（**无 .others**） |
| `essentials.gamemode.survival` | `/gamemode survival` | 切回生存 |
| `essentials.fly` | `/fly` | 飞行开启 |
| `essentials.heal` | `/heal` | 恢复满血（无 heal.others） |
| `essentials.workbench` | `/workbench` | 打开随身工作台（含 /craft 别名） |
| `essentials.top` | `/top` | 传送到地表 |

> 合并说明：`worldguard.region.claim` + `claim.own` → `claim.*`（省 1）；`essentials.craft` 无此权限（/craft 是 /workbench 别名，已剔除）；**fly/gamemode 归属 builder（增量原则）——admin 经继承自动获得**。

## L3 admin（管理员）— 管理命令（32 项，**无 `*`、无 luckperms.\*、无 op**）

| 权限节点 | 验证指令 | 预期 |
|:--|:--|:--|
| `orzmc.admin` | `/bot status` | 显示机器人状态 |
| `minecraft.command.kick` | `/kick <玩家>` | 踢出成功 |
| `minecraft.command.ban` | `/ban <玩家>` | 封禁成功 |
| `minecraft.command.pardon` | `/pardon <玩家>` | 解封成功 |
| `minecraft.command.whitelist` | `/whitelist list` | 显示白名单 |
| `minecraft.command.gamemode` | `/gamemode creative <玩家>` | 改他人模式 |
| `minecraft.command.effect` | `/effect <玩家> clear` | 清除效果 |
| `minecraft.command.tp` | `/tp <玩家>` | 传送他人 |
| `minecraft.command.give` | `/give <玩家> stone 1` | 发放物品 |
| `minecraft.command.save-all` | `/save-all` | 存档成功 |
| `bukkit.command.gamemode` | 同 minecraft.command.gamemode | Bukkit 别名 |
| `bukkit.command.kick` | 同 minecraft.command.kick | Bukkit 别名 |
| `bukkit.command.ban` | 同 minecraft.command.ban | Bukkit 别名 |
| `bukkit.command.whitelist` | 同 minecraft.command.whitelist | Bukkit 别名 |
| `essentials.kick` | `/kick <玩家>` | Essentials 踢人 |
| `essentials.ban` | `/ban <玩家>` | Essentials 封禁 |
| `essentials.unban` | `/unban <玩家>` | Essentials 解封 |
| `essentials.gamemode` | `/gamemode creative <玩家>` | 改他人模式 |
| `essentials.heal` | `/heal` | 自我治疗（继承 builder，2026-08-08 验收清理冗余；无 heal.others） |
| `essentials.give` | `/give <玩家> stone 1` | 发放物品 |
| `essentials.tp` | `/tp <玩家>` | 传送他人 |
| `essentials.time` | `/time` | 时间基础 |
| `essentials.time.set` | `/time set day` | 设置时间（2026-08-08 验收补：set 为独立子权限） |
| `essentials.weather` | `/weather clear` | 设置天气 |
| `griefprevention.admin.*` | `/gpadmin` 相关 | 领地管理 |
| `griefprevention.restorenature` | `/restorenature` | 自然恢复 |
| `worldguard.region.bypass` | 进他人区域 | 不被限制 |
| `worldguard.region.override` | `/rg` 管理操作 | 覆盖区域限制 |
| `vault.admin` | `lp check` | true |
| `ezshops.shop.admin` | `lp check` | true |
| `ezshops.playershop.admin` | 管理他人商店 | 成功 |
| `deathchest.admin` | `/deathchest` 管理命令 | 成功 |
| `bod.bypass` | 死亡回档豁免 | 成功 |

> 剔除项：`essentials.teleport`（无此权限，/tp 已含）、`essentials.unbanip` 等未列（无功能需求）。

**高危节点（明确不授予任何组）**：`*`、`luckperms.*`、`minecraft.command.op`、`bukkit.command.op`、`essentials.stop`、`essentials.reload`。

## 验证方法总纲

1. **LP 层**：`lp group <组> permission check <节点>` → true（确认配置生效）
2. **命令层**：用对应组账号实测命令（上表「验证指令」列）——确认插件真实检查并放行
3. **越权层**：`lp group <组> permission check <管理节点>` → false（确认无越权，如 admin 组 check `minecraft.command.op`）
4. **继承层**：`lp group admin permission check <builder 节点>` → true（确认继承链完整，如 `essentials.fly`）

## bot 全量验收（2026-08-08）

### 验收方法

1. **账号对应组**：HermesBot（member，含 default 继承）/ TestNewbie（builder）/ TestMember（admin）——用对应组账号实测
2. **逐个实测命令**：对配置表每个权限项执行对应命令（见「验证指令」列），目标玩家用不存在的账号（如 NoSuchPlayer）验证**权限放行**（避免副作用）
3. **判定标准**：命令存在（非 Unknown）+ 权限放行（非「没有权限」拒绝）= ✅ 通过；Unknown = 命令未注册（非权限问题，单独标注）；权限拒绝 = 配置缺失（修复后复测）
4. **LP check 交叉验证**：命令实测后 `lp group <组> permission check <节点>` 复核 LP 层状态

### 验收结果明细

| 等级 | 权限项 | 实测命令 | 结果 |
|:--|:--|:--|:--|
| L0 | `essentials.afk` | `/afk` | ✅「你暂时离开了」 |
| L0 | `essentials.balance` | `/balance` | ✅「余额：$0」 |
| L0 | `essentials.balancetop` | `/baltop` | ✅ 排行榜输出 |
| L0 | `essentials.pay` | `/pay TestNewbie 1` | ✅ 放行（目标离线提示） |
| L0 | `essentials.msg` | `/msg TestNewbie hi` | ⚠️ 反垃圾拦截（「移动后才能聊天」——非权限问题） |
| L0 | `essentials.spawn` | `/spawn` | ❌ Unknown（26.2 兼容已知，权限节点已配） |
| L0 | `ls.bypass`/`bod.back`/`ezshops.*` | LP check + GUI 命令 | ✅ true |
| L1 | `getmehome.user` | `/sethome` `/home` `/listhomes` `/delhome` | ✅ 全部成功（父权限展开） |
| L1 | `essentials.tpa`/`tpahere` | `/tpa TestNewbie` | ✅ 放行（目标离线） |
| L1 | `essentials.warp.list` | `/warp` | ✅ 复测放行（初测缺 list 被拒→修复） |
| L1 | `essentials.mail.send` | `/mail send TestNewbie hi` | ✅ 复测放行（初测缺 send 被拒→修复） |
| L1 | `essentials.kit` | `/kit` | ✅「没有可获得的物品包」 |
| L1 | `griefprevention.createclaims` | LP check | ✅ true（木铲交互需手测） |
| L1 | `griefprevention.trapped` | `/trapped` | ✅ 命令执行 |
| L2 | `worldedit.wand` | `//wand` | ✅ 木斧说明输出 |
| L2 | `worldedit.selection.*` | `//pos1` `//expand 10` | ✅ 选区成功/放行 |
| L2 | `worldedit.region.*` | `//set stone` | ✅ 放行（需选区提示） |
| L2 | `worldedit.clipboard.*` | `//copy` `//paste` | ✅ 放行（剪贴板提示） |
| L2 | `worldedit.history.*` | `//undo` | ✅「Nothing left to undo」 |
| L2 | `worldedit.brush.*` | `//brush sphere stone` | ✅ 放行（用法提示） |
| L2 | `worldedit.tool.*` | `//tool` | ⚠️ Unknown（无参需 `//tool <类型>`，命令存在） |
| L2 | `worldedit.utility.*` | LP check | ✅ true |
| L2 | `worldedit.help` | `//help` | ✅ |
| L2 | `worldedit.schematic.*` | `//schem save test` | ✅ 放行 |
| L2 | `worldedit.navigation.*` | `//unstuck` | ⚠️ Unknown（WE 7.4.4 命令注册待查） |
| L2 | `worldedit.analysis.*` | `//count stone` | ✅ 放行 |
| L2 | `worldguard.region.claim.*` | `/rg claim testregion` | ✅ 放行（需选区提示） |
| L2 | `worldguard.region.define/remove/addmember/removemember/setparent/flag/info/teleport` | `/rg define testrg` 等 | ✅ 全部放行（「No region found」） |
| L2 | `worldguard.region.list` | `/rg list` | ✅「No results found」 |
| L2 | `essentials.gamemode.creative/.survival` | `/gamemode creative/survival` | ✅ 切换成功 |
| L2 | `essentials.fly` | `/fly` | ✅「飞行模式开启」 |
| L2 | `essentials.heal` | `/heal` | ✅「Healed!」 |
| L2 | `essentials.workbench` | `/workbench` | ✅ 执行（GUI） |
| L2 | `essentials.top` | `/top` | ✅「正在传送到顶部」 |
| L3 | `orzmc.admin` | `/bot status` | ✅ 放行（参数提示） |
| L3 | `minecraft.command.kick` | `/kick NoSuchPlayer` | ✅「找不到玩家」 |
| L3 | `minecraft.command.ban` | `/ban NoSuchPlayer` | ✅「无法封禁已离线」 |
| L3 | `minecraft.command.pardon` | `/pardon NoSuchPlayer` | ✅ 执行（静默） |
| L3 | `minecraft.command.whitelist` | `/whitelist list` | ✅ 白名单列表 |
| L3 | `minecraft.command.gamemode` | `/gamemode creative NoSuchPlayer` | ✅ 放行 |
| L3 | `minecraft.command.effect` | `/effect NoSuchPlayer clear` | ✅ 放行（参数提示） |
| L3 | `minecraft.command.tp` | `/tp NoSuchPlayer` | ✅「找不到玩家」 |
| L3 | `minecraft.command.give` | `/give NoSuchPlayer stone 1` | ✅「找不到玩家」 |
| L3 | `minecraft.command.save-all` | `/save-all` | ✅「Saved the game」 |
| L3 | `essentials.unban` | `/unban NoSuchPlayer` | ✅ 执行（静默） |
| L3 | `essentials.time.set` | `/time set day` | ✅ 复测放行（初测缺 set 被拒→修复） |
| L3 | `essentials.weather` | `/weather clear` | ✅「天气设为晴天」 |
| L3 | `griefprevention.restorenature` | `/restorenature` | ✅「Ready to restore」 |
| L3 | `worldguard.region.bypass/override`、`vault.admin`、`ezshops.*.admin`、`deathchest.admin`、`bod.bypass` | LP check | ✅ true（隐式/无直接命令） |

### 验收发现并修复

| 项 | 问题（初测） | 修复 |
|:--|:--|:--|
| member `essentials.mail.send` | `/mail send` 拒绝「没有 mail.send 权限」 | 补充配置（mail.send 是独立子权限） |
| member `essentials.warp.list` | `/warp` 拒绝「没有列出传送点权限」 | 补充配置（warp.list 是独立子权限） |
| admin `essentials.time.set` | `/time set day` 拒绝「无权设置时间」 | 补充配置（time.set 是独立子权限） |
| admin `essentials.heal` | 冗余（admin 继承 builder 的 heal） | 移除配置项（继承保持） |

### 遗留标注（非权限配置问题）

- `//tool`：Unknown 因无参（需 `//tool <类型>`）——命令存在，权限项有效
- `//unstuck`：Unknown——WE 7.4.4 命令注册异常（与 /spawn 同类 26.2 兼容问题），`worldedit.navigation.*` 权限本身有效（LP check true）
- `/spawn`：Unknown——Essentials 在 Paper 26.2 未注册 spawn 命令（兼容问题，权限节点已配好，命令恢复后即生效）
- `/msg`：被反垃圾插件拦截（需移动后聊天）——非权限问题

## 本地测试服验证结果（2026-08-08 实测）

| 等级 | 验证账号 | 权限检查 | 结果 |
|:--|:--|:--|:--|
| L0 | default 组 | `essentials.balancetop` | ✅ true |
| L0 | default 组 | `getmehome.user` | ✅ false（已移至 member） |
| L1 | joker | `getmehome.user` / `essentials.tpa` | ✅ true |
| L2 | TestNewbie | `worldedit.wand` / `worldedit.selection.pos` | ✅ true |
| L2 | TestNewbie | `essentials.gamemode.creative` / `essentials.fly` | ✅ true（/gamemode creative 实测切换成功） |
| L2 | TestNewbie | `minecraft.command.kick` | ✅ false（无越权） |
| L3 | TestMember | `orzmc.admin` / `minecraft.command.kick` | ✅ true |
| L3 | TestMember | `minecraft.command.op` / `luckperms.user` | ✅ **false（不可自封 op/改权限）** |

## 注意事项

- **joker 残留脏数据**（本地测试历史）：world 上下文快照的 builder/member 组无法用普通 LP 命令移除（完整上下文匹配），已确认**不影响 global 判定**，线上部署前用 `lp user <X> parent info` 逐个检查存量玩家，带上下文的组按 `lp user <X> parent remove <组> --context ...` 清理
- 配置以命令清单形式同步：`perm_commands.txt`（本地测试服执行记录）可作为线上同步脚本蓝本，**线上执行前先 `lp export` 备份**
- **Essentials spawn 命令缺失**：Paper 26.2（实验版）不被 EssentialsX 2.22.0 支持，/spawn 命令未注册（权限节点 essentials.spawn 已配好，命令恢复后即生效）
