# E2E 测试发现 Bug 记录

> 本文件由 e2e 测试套件执行时发现的问题自动/手动登记。每条记录含复现方式与验证状态。
> 优先级：P0=功能不可用（阻塞） / P1=功能异常（需修复） / P2=健壮性/体验 / P3=优化项

## BUG-E2E-002：大世界（17GB/316万chunk）$b 备份极慢+失败误报—— ✅ 已修复（2026-08-19，OrzMCBackup PR #46）

- **现象**：Paper 测试服（317 万 chunk）执行 `$b` 备份数小时未完成，`OptimizeError: Pattern matching failed: unknown compression: 31` 反复触发「地图备份 失败」通知
- **根因（全量扫描 3,163,860 chunks + 局部复现确认）**：
  1. 99.998% chunk 是标准 ZLIB(2)，另有 ~56 个**损坏 chunk 条目**（compression byte 垃圾值 + 部分**长度字段荒谬**如 0x789cd12e≈20亿字节）——**不是 MC 26.2 新压缩格式**
  2. 长度荒谬+compression 合法的损坏 chunk：旧版 `dataBytes` 尝试分配+读取 20 亿字节 → 单 chunk 卡死数分钟 → 56 个损坏 chunk 拖垮整次备份（数小时级）→ **备份实际未完成**（非失败）
  3. `errorHandler` 对每个 Pattern 错误触发「地图备份 失败」→ 误报风暴
- **修复（OrzMCBackup fix/corrupted-chunk-keep → PR #46，2 commits）**：
  1. `McaEntry`：未知 compression → `UNKNOWN`（不抛，长度字段可读）；`dataBytes`/`serializedBytes` 对荒谬长度（>8MB）短路返回空——**秒级跳过不卡死**
  2. `McaWriter.writeEntry` 空数据明确报错；`DimensionProcessor` pattern 异常 → 安全保留原始 chunk（compression 非法但长度正常的透传保留），错误仅记录不中断
  3. `CorruptedChunkKeepTest` 3 测试：损坏保留 / strict 不中断 / 荒谬长度快速跳过
- **插件侧配套（WorldMaintenanceService）**：errorHandler 聚合——Pattern 错误（损坏 chunk）计数不报失败；致命错误限频 1 次；Done 时汇总「含 N 个损坏区块已安全保留」
- **验证**：ktlint+detekt+全量测试通过；真实损坏样本（r.7.2.mca / r.-1.0.mca）CLI 验证：**1.5-1.8s 完成 + zip 生成 + 错误记录**（旧版同样本卡死/极慢）
- **待办**：发布 backup-core 新版（tag）→ 插件升级依赖 → Paper 服 `$b` 端到端验证

## BUG-E2E-003：CommandGuard 审计日志洪泛（P2，待修）

- **现象**：Paper 测试服命令方块循环每 tick 触发 ~20 条「危险命令放行」WARN → 53MB 日志/20 分钟，挤爆日志轮转窗口（E2E waitLog 200 行窗口被挤出）
- **根因**：测试服世界含巨型命令方块系统（CB_SAVE_TEST）；CommandGuard 对「放行」命令每条记 WARN
- **建议**：放行类降级 DEBUG 或限频（如 5s 窗口内同类只记 1 条）；拦截类保持 WARN
- **连带**：E2E waitLog 默认 tail 200→3000 已缓解

## BUG-E2E-001：`$w` 白名单分页在 Folia 上抛异常（第一页即炸）—— ✅ 已修复并双核心验证（2026-08-19）

- **环境**：Folia 26.2-4 测试服（~/folia-test/）+ OrzMC 1.0.18-dev.jar
- **现象**：`orzdebug $w` 只回「debug 已受理」，收不到白名单列表；日志抛
  `java.lang.IllegalArgumentException: Delay ticks may not be <= 0`
- **根因**：`Paginator.paginatePages`/`paginate` 循环 `i * delayTicks`，**i=0 时 delay=0**。
  Paper BukkitScheduler 允许 0，Folia `FoliaGlobalRegionScheduler.runDelayed` 要求 ≥1。
  `delayTicks <= 0 ? 5L : delayTicks` 只保护配置值，不保护 i=0 首页。
- **修复（已提交代码 + 单测护栏）**：
  1. `Paginator.java` 两处 delay 计算 → `Math.max(1L, (long) i * (delayTicks <= 0 ? 5L : delayTicks))`
  2. `ServerFacade.runLater` 防御性钳位 `Math.max(1, delayTicks)`（覆盖所有调用点）
  3. `PaginatorTest` 新增 3 个回归护栏（首页 delay ≥ 1 / 负 delay 兜底 / 单页）
- **验证**：Folia 26.2-4 ✅（`$w` 输出 123 人白名单列表）+ Paper 26.2-112 ✅（01 用例 8/8 全过）
- **连带修复（E2E 套件健壮性）**：
  - `spawnBot` 成功判定去掉「Welcome! just joined」误匹配（首登广播≠登录成功），只认「登录成功/注册成功」——否则 bot 过早 resolve 命令被 SimpleLogin 拦截
  - `waitLog` tail 200→3000（高刷屏环境窗口被挤出）

## BUG-E2E-002：大世界 `$b` 备份失败 —— backup-core 无法解析未知 MCA 压缩格式（P1，未修复）

- **发现**：2026-08-19 Paper 测试服（~317 万 chunk 大世界）E2E
- **现象**：`$b` 触发后 ChunkProgress 跑到 37%，压缩阶段失败：
  `OptimizeError(path=.../region/r.60.19.mca, kind=Pattern, message=Pattern matching failed: unknown compression: 31)`
  → `地图备份 失败 用时:5分53秒` → 无 .zip 产出（仅 tempDir 残留）
- **影响**：包含特殊压缩格式 MCA 的世界（MC 26.2 或历史版本世界）备份失败；Folia 小世界正常（无此类文件）
- **根因**：`io.github.wangzhizhou:backup-core:0.1.6` 的 MCA 解析器不认识 compression id 31/-3（
  Paper 26.2 世界可能用了新压缩格式或损坏文件）
- **建议**：① 升级 backup-core 到支持 26.2 的版本；② 或备份前扫描跳过无法解析的 MCA（并告警）；
  ③ 或回退用 `plugins/OrzMC/backup` 之外的普通 zip 备份方案兜底
- **E2E 断言缺陷（连带）**：04 用例原「文件名差异」把 `tempDir` 中间产物误判为成功 → 已修为
  **只认 .zip 完成文件**（waitForZipFile），备份失败时用例显式 FAIL

## BUG-E2E-003：CommandGuard 审计日志洪泛 —— 高频命令放行每条 WARN（P2，未修复）

- **发现**：2026-08-19 Paper 测试服（世界含巨型命令方块循环系统，CB_SAVE_TEST 链）
- **现象**：命令方块每 tick 执行 ~20 条 effect/execute 命令 → CommandGuard 每条输出
  `危险命令放行（未限定目标选择器）` WARN → **53MB 日志 / ~20 分钟**，日志轮转压力大、
  干扰 E2E waitLog 读取（200 行窗口被刷屏填满）
- **影响**：生产环境若有循环命令方块（Paper 端）日志爆炸；Folia 端命令方块禁用无此问题
- **建议**：放行类审计默认降级为 DEBUG 或按命令哈希限频（如 1 条/秒），仅拦截/异常保持 WARN；
  或审计日志走独立文件 + 限流
- **测试环境连带**：E2E waitLog 的 tail 窗口被刷屏挤出 → 建议 waitLog 支持按时间戳搜索而非纯 tail

## 观察项（非插件缺陷，环境事实）

| # | 观察 | 说明 |
|:--|:--|:--|
| O1 | Paper 测试服世界有巨型命令方块循环 | CB_SAVE_TEST 链每 tick 执行 ~20 条命令，触发 BUG-E2E-003 放大 |
| O2 | Paper 测试服线程名 `Folia Async Scheduler Thread` | Paper 26.2 实现了 Folia 调度 API，OrzMC 统一调度器代码路径，线程名相同属正常 |
| O3 | Paper 测试服备份目录 retention=1 | 旧 zip 被清理，E2E 断言须用文件名差异而非数量 |
| O4 | shadowJar 12 个编译警告 | Predicate 泛型 raw type（P3，CI 会标 ##[warning]，建议清零） |
