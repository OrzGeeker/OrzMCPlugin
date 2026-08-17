# OrzMC Folia 适配验收清单

> 本文档收录 OrzMC 插件在 **Folia 运行时**下需要完成的功能验收项。
> 目标：确认插件在 Folia（regionized 多线程）上功能与 Paper 等价、无 region 线程错误。
> 与 [test-cases.md](test-cases.md)（Paper E2E）互补——Folia 的调度/区域亲和差异只能靠真实 Folia 服务器验证。
>
> - 适配与测试策略详见 [folia-migration.md](folia-migration.md)（D3 区域亲和 / D6 测试策略 / D7 并发）
> - 结论标记：✅ 已验收（附证据）/ ⬜ 待真实环境 / ⚠️ 工具限制（非插件缺陷）/ ❌ 失败（含修复记录）
> - **失败判据**：日志出现 `IllegalThreadStateException`、`not the correct region`、死锁（卡 tick）任一即失败

---

## 一、已完成验收（无头/自动化）

以下项不需要真实玩家/Bot，已在开发机与 CI 上验证通过。

| # | 验收项 | 结果 | 证据 |
|:--|:--|:--|:--|
| FA-01 | Folia 服务端启动加载 OrzMC，无插件崩溃 | ✅ | CI `folia-smoke` job（PR #191 run 32052367629、合并后 push run 32052509225 均 SUCCESS）；PR-5 本地 `foliaSmoke` 通过（Folia 26.2 约 7s 启动） |
| FA-02 | 插件在 Folia 上干净退出（`stop` → exit 0） | ✅ | 同上；`foliaSmoke` 断言 90s 内 exit 0 |
| FA-03 | `./gradlew runFolia` 本地调试服务器可用 | ✅ | PR-5 验证：11s 启动、OrzMC 加载、`stop` 正常卸载插件并保存世界 |
| FA-04 | 调度器门面（global region / async）在 Folia 启动路径无异常 | ✅ | FA-01/02 覆盖；单测 `ServerFacadeTest`（PR-1 重写） |
| FA-05 | 区域亲和投递正确性（单元级） | ✅ | PR-2/3 新增：`RegionSchedulerProvider.run(world,cx,cz,task)` 坐标断言、`EntityScheduler` kick 投递、`teleportAsync` 验证 |
| FA-06 | 并发安全（单元级） | ✅ | PR-4：64 线程爆炸聚合 ×64 精确、50 线程入队单批精确、tick×回调并发无 `ConcurrentModificationException` |
| FA-07 | Paper 侧无回归 | ✅ | 主 `check` 流水线（spotless + test + integrationTest + jacoco + shadowJar）全绿（PR #186-#191） |

## 二、待真实环境验收（⬜）

以下项需要真实 Folia 服务器 + EasyBot 网关 + 玩家（推荐复用 test-cases.md 的 mineflayer 机器人 + 真实玩家方案，在 Folia 测试服执行）。每项执行时开启调试日志并在结束后检索服务器日志，确认无失败判据命中。

### TC-F1 白名单管理（`$w` / `$a` / `$r`）

| 项 | 内容 |
|:--|:--|
| 前置条件 | Folia 测试服 + EasyBot 接入 + 管理员群 |
| 步骤 | ① 群内 `$w` 查白名单；② `$a <玩家>` 添加；③ 非白名单玩家进服触发踢出；④ `$r <玩家>` 移除 |
| 预期 | 查询/添加正常回传；非白名单玩家被踢出并收到提示（kick 走 EntityScheduler region 线程，无错） |
| 实际 | ⬜ |
| 方式 | Bot + 真实玩家 |

### TC-F2 备份/优化（`$b` / `$o`）

| 项 | 内容 |
|:--|:--|
| 前置条件 | 同上 |
| 步骤 | ① 群内 `$b` 触发一键备份；② `$o` 触发地图优化；③ 观察进度消息到完成 |
| 预期 | 备份/优化完整执行，进度实时回传，完成无异常（调度走 async + global region） |
| 实际 | ⬜ |
| 方式 | Bot |

### TC-F3 TNT 保护与爆炸通知

| 项 | 内容 |
|:--|:--|
| 前置条件 | TNT 保护启用 + 玩家在线 |
| 步骤 | ① 白名单区域内放 TNT 并引爆；② 白名单区域外放 TNT；③ 发射器连环爆炸 |
| 预期 | 区域外被拦截；爆炸通知聚合为 ×N 单条告警（`TntEventService.pendingAlerts` 并发安全），无重复调度 |
| 实际 | ⬜ |
| 方式 | 真实玩家/机器人 |

### TC-F4 传送弓（`/tpbow`）

| 项 | 内容 |
|:--|:--|
| 前置条件 | 玩家在线 + 权限 `orzmc.tpbow.use` |
| 步骤 | ① 执行 `/tpbow` 射箭；② 远距离射击（触发 force-load 区块）；③ 落点非安全位置时自动就近找安全点 |
| 预期 | 传送至落点；`ForceLoadedChunkLease` 经 region scheduler 获取/释放，无「not the correct region」；实体策略按配置 |
| 实际 | ⬜ |
| 方式 | 真实玩家/机器人 |

### TC-F5 跨服传送门（`/portal`）

| 项 | 内容 |
|:--|:--|
| 前置条件 | 双服 Folia（或 Folia + Paper）配置传送门 |
| 步骤 | ① 管理员创建传送门；② 玩家踩踏传送门触发跨服 transfer；③ 删除传送门 |
| 预期 | 创建/删除经 region scheduler 在 anchor chunk 投递方块操作，无跨界异常；玩家 transfer 正常；`portal.yml` 运行时读写正确 |
| 实际 | ⬜ |
| 方式 | 管理员 + 真实玩家 |

### TC-F6 长稳运行（8h+ 无死锁）

| 项 | 内容 |
|:--|:--|
| 前置条件 | Folia 测试服持续运行 |
| 步骤 | 持续运行 ≥8 小时，期间混合执行 TC-F1~F5 各 ≥2 次 |
| 预期 | 无死锁（tick 持续推进）、无 region 线程异常、内存无异常增长 |
| 实际 | ⬜ |
| 方式 | 自动脚本 + 日志检索 |

## 三、验收结论

- 全部 ⬜ 项完成且无失败判据命中 → 视为 Folia 适配验收通过，可发正式版（Folia loader）。
- 任一 ❌ → 记录日志尾部、修复后走 PR 重新验证。

> 备注：Folia 下 `runFolia` 的 `run-folia/` 与 `run-folia-smoke/` 均为隔离运行目录，不影响真实配置（[`.gitignore`](../../.gitignore) 已忽略）。
