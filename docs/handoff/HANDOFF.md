# HANDOFF｜NightRec 连续开发进度

- Updated at：2026-10-02（Asia/Shanghai）「大交接 2」，开发暂时收尾。
- PROJECT_PHASE：DEVELOP；执行方式：**唯一开发者、连续推进、无 subagent**。
- PLAN_VERSION / DEV_BASELINE：NightRec SDD 开发包 V2.0 + 用户批准的 AudD 增量；Constitution 2.1.0。
- CHANGE_REQUEST：NONE（识曲节流属 B 类局部功能变化，已就地实现并记录，未重开 Plan）。
- Human Gate（T109）**尚未到达**：全部 Task + converge + 长测 + 最终 APK 完成后才一次性提交。
- 仓库：`https://github.com/wanghoufan/p041-nightrec.git`（main）。本次已 commit + push（见文末）。

---

## A. 当前工作进展

**总览**：T001–T110 共 111 项，已完成 **109 项**；剩 **2 项**（T097 长测进行中、T109 Human Gate）。收尾已到最后一段。

### 本轮收尾进展（2026-10-03）
- **T094**：单测 44/0；产品 instrumentation 9/9（OriginalImmutabilityTest 2 / RecognitionPersistenceTest 5 / RoomBaselineTest 2）；SpikeDeviceTest 之 seek≤250ms 用例为边界偶发（正常 46–236ms，偶发 254–256ms），已记录不掩盖。证据 `docs/verification/t094-tests.md`。
- **T108**：`./gradlew clean lintDebug testDebugUnitTest assembleDebug` exit 0；lint 0 error / 46 warning / 1 note；APK **74463170 bytes**，SHA-256 `b62c383d1e20ba25b425d078335b1d424f9034ff10bc8caf29f545f199dbb581`。证据 `docs/verification/t108-final-build.log`。
- **T101**：最终 APK 真机 smoke 全通过——cold-start（COLD 1244ms、无 crash/ANR）、权限 grant、开始（FGS id=92 microphone）、结束（session READY）、播放器（MediaSession PLAYING）。证据 `docs/verification/t101-t102-device.md` + `device/t101-t102/`。
- **T102**：adaptive（v26）+ monochrome（v33）图标；Manifest 仅 MainActivity、单任务单 Activity → Splash 无双启动页；真机图标渲染正常。
- **T103**：`release-candidate.md` 回填 T108 最终 APK 大小/hash。
- **T097**：3h 长测已启动（session id=2、锁屏 Asleep、实时识曲默认关），采样器 `scripts/nightrec/soak-sampler.py 2`，报告待完成后写 `soak-report.md`。

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
  - 决策文 `docs/decisions/recognition-request-throttling.md`；验证：单测 44 通过、真机 instrumentation 5 通过。

### 验证证据（均在 `docs/verification/`）
- `traceability.md`（FR-001–FR-059 全映射）、`convergence.md`、`analyze-final.md`（CRITICAL=0/HIGH=0）。
- `end-to-end.md`、`recovery.md`（真机 ADB 场景）、`clean-quality.md`（20 样本 AB）、`checksum-verification.md`（Session 5 18/18 分片一致）。
- `soak-report.md`：T096 通过（30min 锁屏 Asleep、FGS 持续、0 crash/ANR）；**T097 未跑满 3h（实际约 89min）**。
- `release-candidate.md`（T103 草稿，APK hash 待 T108 回填）；`t094-t095-host.log`、`t099-prebuild.log`。
- 真机截图：`docs/verification/device/final-ui/`（01–12）、`device/e2e/`。

### 已知关键问题
- **AudD 服务端额度已耗尽**（铁证：`code=902 "the limit was reached."`）。实时/补识别/手动重识别均无法命中；**Original 采集、播放、整晚留存不受影响**。详见 §C。
- 最新 debug APK 已装 `indq5xfi6hovay4d`；重装会清 DB 与 RECORD_AUDIO 权限（需补 grant）。

---

## B. 下一步任务

> 剩 2 项。

1. **T097**（进行中）：3h 长测跑满后，用采样结果写 `docs/verification/soak-report.md`（device/Android/segments/gaps/tracks/crash/ANR/storage）。
2. **T109**（唯一 Human Gate）：向用户一次性提交——功能清单 / 已知限制 / 真机截图索引 / APK 路径+hash / 验证证据。
3. 每步更新 `tasks.md` 勾选与 `HANDOFF.md`。
4. **识曲重测（待用户付费）**：用户购买 AudD Indie（$5/月，1000 次）并把 token 填入 `local.properties` 后，重装并实机重测一次真实命中；节流已就位，1000 次预计可覆盖 6–8 小时一晚。

**当前未勾选项**：T097、T109。

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
- 会话残留（**待用户裁决，未删**）：根目录 `README.md` 已被删除、`README 2.md` 未入库。
- SDD 包归档：`docs/plan/` 的解压目录为现役 SDD 源（spec/plan/tasks 所在）；原始 `.zip` 归档已于 2026-10-02 按用户指令删除（内容与解压目录一致）。

### 交接纪律
- 本仓治理规则见根 `AGENTS.md`；模型分工见根 `USER_MODEL_OVERRIDE.md`。
- 不 push 到 main 以外的分支除非用户指定；破坏性 git 操作需用户明确授权。

---

## D. 本次「大交接 2」动作
- neat-freak 文档对齐：修正 `.gitignore`（放行验证证据日志）；标记会话残留待裁决。
- 重写本 HANDOFF（进展/下一步/注意事项）。
- commit + push（main）。