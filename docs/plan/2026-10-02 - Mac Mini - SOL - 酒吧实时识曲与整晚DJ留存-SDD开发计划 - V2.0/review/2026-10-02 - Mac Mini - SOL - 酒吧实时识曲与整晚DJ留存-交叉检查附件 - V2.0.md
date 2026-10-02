# NightRec SDD V2.0 交叉检查附件

## 本轮发现并修复的主要问题

1. **命名冲突**：上一版 SDD/代码路径出现 `NightSetApp`，原型又出现 NightRec/NightSet。V2.0 统一产品名 NightRec，Night Session 仅作业务对象。
2. **Android SDK 基线错误**：上一版写 `targetSdk 37`。截至 2026-10-02，Android 17/API37 仍为 Beta；V2.0 改为 stable `compileSdk/targetSdk 36`，API37 仅兼容测试。
3. **Google Play 要求补齐**：2026-08-31 起新应用/更新需 target Android 16/API36 或更高，V2.0 基线符合。
4. **ShazamKit 安全冲突**：上一版“无后端 + 不硬编码凭据”未解释 Developer Token 来源。V2.0 明确：本地 debug 使用提前提供的 Developer Token；`.p8` 永不进 APK；生产刷新另开 token-broker feature。
5. **Shazam 连续流边界**：物理安全分片不应重启 StreamingSession；真正 AWAY/INTERRUPTED gap 后必须重建 recognition session。
6. **Splash 规范**：上一版只有视觉稿，没有平台落地约束。V2.0 强制 Android SplashScreen API/compat，禁止独立 SplashActivity 双启动。
7. **视觉真源缺失**：上一版 tasks 未把原型、图标、Light/Dark 当正式输入。V2.0 Constitution/Plan/Tasks 均绑定 design/ 资产并加入真机截图验证。
8. **ADB/预览缺失**：V2.0 新增每个视觉/交互 phase 的 `installDebug + relaunch + screencap` 流程。
9. **APK 预构建缺失**：V2.0 Human Gate 前强制 fresh `clean lintDebug testDebugUnitTest assembleDebug`，记录 APK SHA-256 并在连接手机安装 smoke。
10. **Human Gate 太早风险**：V2.0 采用当前 Spec Kit `analyze -> implement -> converge -> implement... -> converged` 闭环，所有 Task 完成后才进入唯一 Human Gate。
11. **Clean 过度承诺风险**：继续保留“附近讲话净化”需求，但定义为保守 Beta 能力；Original 永远真源，低置信 KEEP_ORIGINAL。
12. **本地数据库边界**：V1 明确不需要数据库 API key，Room + app-private files 足够，减少外部依赖与隐私风险。

## 当前官方规范核对

- Spec Kit current `tasks.md` 明确按 User Story 组织并带精确路径；`analyze` 是 task 生成后只读一致性检查；`converge` 是 implement 后 append-only 补遗漏任务。
- Android 14+ microphone foreground service 必须声明 microphone type 与 `FOREGROUND_SERVICE_MICROPHONE`，且 while-in-use `RECORD_AUDIO` 限制要求从可见状态启动。
- Android 12+ 使用系统 SplashScreen；官方建议 compat 库，避免专用 SplashActivity 导致双 splash。
- AGP 9.4.0 stable、Kotlin 2.4.20 stable、Compose BOM 2026.09.00 stable、Room 2.8.5、Media3 1.11.1。
