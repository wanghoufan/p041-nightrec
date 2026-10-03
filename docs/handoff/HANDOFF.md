# HANDOFF｜NightRec 连续开发进度

- Updated at：2026-10-03（Asia/Shanghai）「收尾 4」，图标按方案 A 重设计 + 重建复算，仅剩 T109。
- PROJECT_PHASE：DEVELOP；执行方式：**唯一开发者、连续推进、无 subagent**。
- PLAN_VERSION / DEV_BASELINE：NightRec SDD 开发包 V2.0 + 用户批准的 AudD 增量；Constitution 2.1.0。
- CHANGE_REQUEST：NONE（识曲节流属 B 类局部功能变化，已就地实现并记录，未重开 Plan）。
- Human Gate（T109）**尚未到达**：全部 Task + converge + 长测 + 最终 APK 完成后才一次性提交。
- 仓库：`https://github.com/wanghoufan/p041-nightrec.git`（main）。本次已 commit + push（见文末）。

---

## A. 当前工作进展

**总览**：T001–T110 共 111 项，已完成 **110 项**；剩 **1 项**（T109 Human Gate）。收尾只剩一步。

### 本轮收尾进展（2026-10-03）
- **T094**：单测 44/0；产品 instrumentation 9/9（OriginalImmutabilityTest 2 / RecognitionPersistenceTest 5 / RoomBaselineTest 2）；SpikeDeviceTest 之 seek≤250ms 用例为边界偶发（正常 46–236ms，偶发 254–256ms），已记录不掩盖。证据 `docs/verification/t094-tests.md`。
- **T097**：3h 长测**重做完成**（session id=3，08:33→11:43+，锁屏、实时识曲关）。以「采集连续性」口径**通过**：3h10m、38 个封口分片（`00000`–`00037`，各约 7.26MB，带 `.sha256`）+ `00038.m4a.open`、FGS 存活、logcat 0 FATAL/0 ANR。末段采样出现 62 行空值（`fgs=false/free_kb=0`），实测为 **USB 与设备约 10:33 断连**（手机改以无线在线），App 录制未中断（分片严格 5 分钟步进），已在报告如实写明缺口与佐证。报告 `docs/verification/soak-report.md`、采样 `docs/verification/device/soak-samples.jsonl`。
- **死开关修复（T097 暴露）**：长测发现「实时识曲」关闭开关不生效——`RecordingForegroundService.startRecognition` 被无条件调用，导致关着开关仍发请求（`budget.txt` 15→33）。修复：新增门控 `shouldStartLiveRecognition(settingsEnabled, liveRecognition)`，`startRecognition` 仅在「全局设置 ∧ 本次勾选」同时真时启动（改 `RecognitionPolicy.kt`/`RecordingForegroundService.kt`/`RecordingStateStore.kt`/`NightRecApp.kt`，服务读 `liveRecognition` extra 并跨 resume 保持），补单测 `liveRecognitionGateRequiresBothSettingsAndSessionToggle`。属 **B 类局部功能变化**，未重开 Plan。
- **T108**：修复后重跑 `./gradlew clean test assembleDebug` exit 0；单测 **45/0**；APK **74463158 bytes**，SHA-256 `6ceb89afde8509547b799a96520d5c584d7814bd03df6e4aaad0fee0af55626b`（**图标方案 A 重设计 + 死开关修复后**，两次 fresh 构建同 hash，确定性）。lint 沿用 T108 结论（0 error / 46 warning，无 HIGH correctness，本轮为资源改动未重跑）。证据 `docs/verification/t108-final-build.log`。
- **T101**：最终 APK 真机 smoke 全通过并**复验**——cold-start（复验 1251ms、无 crash/ANR）、权限 grant、开始（FGS id=92 microphone）、结束（session READY）、播放器（AudioPlayback `state:started`）；门控验证（关实时识曲）录制约 8 分钟 `budget.txt` 恒为 33 未增长。证据 `docs/verification/t101-t102-device.md` + `device/t101-t102/`。
- **T102**：adaptive（v26）+ monochrome（v33）图标；**按方案 A「均衡脉冲」重设计**——108 视口 7 条脉冲、条宽 6、中心 x 精确 54（居中）、中央条高 56（y 26..82），全部收进 72dp 安全区（21–87），不再越界被 mask 裁切；`ic_wave.xml`（彩色）与 `ic_wave_mono.xml`（主题化）同几何。Manifest 仅 MainActivity、单任务单 Activity → Splash 无双启动页。已重建并装真机，launcher 渲染待用户解锁后目视确认。
- **T103**：`release-candidate.md` 回填最新 APK 大小/hash（`6ceb89…526b`，74463158 bytes）。

### 已实现并真机验证的功能（代码事实）
- **工程/基础**：Kotlin 2.4.20 / AGP 9.4.0 / Gradle 9.6 / JDK17，compile·target 36、min 29；`com.nightrec.app`。Room schema v1（11 实体）已导出；`ClockProvider`；`SessionStateMachine`；`SafeLogger`（token 不落 release 日志）。
- **视觉**：Compose Material3 自定义 tokens（品牌渐变 `#6D28FF→#FF35B5→#FFB11B`），Light/Dark，禁用 dynamic color；共享组件（PrimaryGradientButton/StateBadge/WaveformCard/SessionListItem/TrackMarkerRow/BottomNav）；adaptive + Android13 monochrome 图标；官方 core-splashscreen，无独立 SplashActivity。
- **采集链**：可见 Activity → microphone FGS（`RecordingForegroundService`，id=92）→ 48kHz/stereo AAC-LC → 5 分钟安全分片 + 原子封口 + sha256；AWAY 封口建 Gap、RESUME 同 Session 逻辑连续。
- **识曲（AudD，用户批准替代 ShazamKit）**：`recognition/` 全链路——PCM 下混 → 有界 Channel（不阻塞录音）→ AudD 识别 → `TrackStabilizer` 稳定 → 幂等持久化（Observation / TrackOccurrence）；补识别 `RecognitionBackfillWorker`；`TrackTimeline`。**实测真实命中 `Warriors / Imagine Dragons`**。
- **停止/处理/回放**：结束确认弹窗 → 停止事务 → PROCESSING → Original 可立即播放；`PlaybackService`（MediaSessionService）+ ExoPlayer 单逻辑播放器；跨分片连续播放、Marker seek、Gap skip；Original/Clean 同位置切换。
- **恢复/存储安全**：CaptureFailure 标准化、RECORDING→INTERRUPTED→RECOVERING→RECORDING、恢复卡片、`SegmentRecoveryPlanner` 隔离损坏尾片、`StorageMonitor`。
- **保守 Clean（Beta）**：只读 Original → `clean/v1/` 派生；`OriginalImmutabilityTest` 证明 Original 前后 sha256 100% 不变；20 样本 AB 判定无主唱/转场受损。
- **历史/纠错/收藏/设置**：History、TrackCorrection、FavoriteRange、Settings（主题、识曲状态 104/300、存储、Original 自动删除默认关）。
- **本次新增：识曲请求节流（防额度空跑）**
  - 静音跳过（窗口 RMS < 300 不发请求，实时链 + 补识别链同判定）。
  - 命中冷却（命中后 60s 逻辑时间内不重复发请求）。
  - 「实时识曲」开关默认关闭。
  - **门控修复（2026-10-03）**：`startRecognition` 曾无条件调用致开关失效；现仅当「全局设置 ∧ 本次勾选」同真才启动（`shouldStartLiveRecognition`）。
  - 决策文 `docs/decisions/recognition-request-throttling.md`；验证：单测 45 通过、真机 instrumentation 5 通过。

### 验证证据（均在 `docs/verification/`）
- `traceability.md`（FR-001–FR-059 全映射）、`convergence.md`、`analyze-final.md`（CRITICAL=0/HIGH=0）。
- `end-to-end.md`、`recovery.md`（真机 ADB 场景）、`clean-quality.md`（20 样本 AB）、`checksum-verification.md`（Session 5 18/18 分片一致）。
- `soak-report.md`：T096 通过（30min 锁屏 Asleep、FGS 持续、0 crash/ANR）；**T097 重做完成**（session 3，3h10m、38 封口分片、0 crash/ANR，采集连续性通过；末段 USB 断连缺口已如实记录）。
- `release-candidate.md`（T103，APK hash 已回填 `e35194…3401`）；`t108-final-build.log`、`t094-t095-host.log`、`t099-prebuild.log`。
- 真机截图：`docs/verification/device/final-ui/`（01–12）、`device/e2e/`。

### 已知关键问题
- **AudD 服务端额度已耗尽**（铁证：`code=902 "the limit was reached."`）。实时/补识别/手动重识别均无法命中；**Original 采集、播放、整晚留存不受影响**。详见 §C。
- 最新 debug APK 已装 22101316C（ruby）；2026-10-03 起该机以无线在线 `192.168.31.31:5555`（USB `indq5xfi6hovay4d` 已断）；重装会清 DB 与 RECORD_AUDIO 权限（需补 grant）。

---

## B. 下一步任务

> 剩 1 项。

1. **T109**（唯一 Human Gate）：向用户一次性提交——功能清单 / 已知限制 / 真机截图索引 / APK 路径+hash / 验证证据。此为最终一步，需用户签收（首次发布）。
2. 收尾后更新 `tasks.md` 勾选与 `HANDOFF.md`。
3. **识曲重测（待用户付费）**：用户购买 AudD Indie（$5/月，1000 次）并把 token 填入 `local.properties` 后，重装并实机重测一次真实命中；节流已就位，1000 次预计可覆盖 6–8 小时一晚。

**当前未勾选项**：T109。

---

## C. 注意事项与规矩

### 红线（不可违反）
- **Original 不可变**，Clean 只写派生；低置信一律回原版；不承诺无损删除讲话。
- **真实识曲必须真 Token + 真音源**；HTTP 200 或 mock ≠ 命中。
- 识曲消费者共享预算：**先计数再发请求**；耗尽只停网络识曲，Original 继续；**不自动付费**。
- Light/Dark 原型、adaptive+monochrome 图标、官方 SplashScreen；**不得新增 SplashActivity**。
- **不碰 secrets**（不输出 token、Key、`.p8`、数据库 Key）；不擅自 commit/push（本次经用户明确指令）；保留用户既有改动。
- 不用有线耳机做识曲验证（音频入耳机后麦克风物理收不到）。

### 构建 / 设备（务必照做）
- 构建前：`export JAVA_HOME="$HOME/android-toolchain/jdk-17.0.20.1+1/Contents/Home"; export ANDROID_HOME="$HOME/android-toolchain/sdk"`。
- 设备固定 USB：`ANDROID_SERIAL=indq5xfi6hovay4d`（ruby / 22101316C / Android 14）。
- **MIUI 安装坑**：`adb install -r -t <apk>` 后 ~3s 内必须点「继续安装」`input tap 389 2114`，否则自动拒绝（`INSTALL_FAILED_USER_RESTRICTED`）。
- **重装清权限**：重装后需 `pm grant com.nightrec.app android.permission.RECORD_AUDIO`（+`POST_NOTIFICATIONS`）。
- `connectedDebugAndroidTest` 常被 MIUI 拦截安装测试 APK：改为手动 `adb install` 两个 APK 后 `adb shell am instrument -w -e class <Class> com.nightrec.app.test/androidx.test.runner.AndroidJUnitRunner` 直跑。
- 构建/设备命令需在沙箱外执行（`dangerouslyDisableSandbox`）。

### 识曲额度（重要，历史上被踩过）
- AudD 免费试用 **300 次已在服务端耗尽**（`code=902 the limit was reached`），**本地 `budget.txt` 清零无效**。
- 1 次 request = 上传一小段音频（本项目 12s 窗口）识别一首歌，**按次数计费**。旧行为 12s/次 ≈ 5 次/分，300 次仅约 1 小时。
- **教训**：长测在无声环境开着实时识曲空跑，把额度烧光（Session 5 的 163 条 observation 全 UNKNOWN）。**长测前必关实时识曲**。
- 本地计数仅个人侧载调试参考，**不代表服务端额度**，禁止清零冒充。
- 节流已实现（§1c），务必保留。

### 文档与仓库
- `docs/verification/*.log` 为文档引用的证据，已在 `.gitignore` 显式放行（`!docs/verification/*.log`），会随仓交接。
- **测试 fixture 不入库**：`app/src/debug/assets/*.mp3` 与 `app/src/androidTest/assets/cleanup-ab/` 被忽略（个人音频，版权）；**本机存在，换机需自备**，否则 20 样本 AB / 部分 instrumentation 无法重跑。
- 会话残留已清理（2026-10-03）：`README 2.md`（误入的「ORCA 新项目模板包」README）已 `git rm`；`AGENTS.md.旧版-2026-09-29`（与 git 历史 `e17616a:AGENTS.md` 逐字节一致，可从中恢复）已删除；根目录及子目录 5 个 `.DS_Store` 已删（本就被 `.gitignore` 忽略）。正式项目说明为根目录 `README.md` / `README.en.md`。
- SDD 包归档：`docs/plan/` 的解压目录为现役 SDD 源（spec/plan/tasks 所在）；原始 `.zip` 归档已于 2026-10-02 按用户指令删除（内容与解压目录一致）。

### 交接纪律
- 本仓治理规则见根 `AGENTS.md`；模型分工见根 `USER_MODEL_OVERRIDE.md`。
- 不 push 到 main 以外的分支除非用户指定；破坏性 git 操作需用户明确授权。

---

## D. 本次「大交接 2」动作
- neat-freak 文档对齐：修正 `.gitignore`（放行验证证据日志）；标记会话残留待裁决。
- 重写本 HANDOFF（进展/下一步/注意事项）。
- commit + push（main）。