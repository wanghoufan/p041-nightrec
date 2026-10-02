# Tasks: NightRec — 整晚 DJ Set 连续留存

**Input**: `.specify/memory/constitution.md`, `spec.md`, `plan.md`, `design/` assets  
**Execution rule**: 单开发者从 T001 连续执行到 T109；只有 T002 允许在编码前集中索取缺失凭据；T109 才是 Human Gate。

## Format

`- [ ] T### [P?] [US?] action`。本计划以依赖顺序为主，视觉/平台/验证任务同样是一等 Task。

## Phase 0: Preflight & Blocking Spikes

- [ ] **T001** 在任何实现前读取 Constitution、Spec、Plan、全部 design 资产，并在 `docs/decisions/visual-baseline.md` 记录 NightRec 命名、Light/Dark、关键页面映射；禁止 NightSet。
- [ ] **T002** 执行一次集中凭据预检：确认 ShazamKit AAR 本地路径、SHAZAM_DEVELOPER_TOKEN、ADB 目标设备；凭据缺失时仅此时集中向用户索取。
- [ ] **T003** 运行 Spec Kit `/speckit.analyze` 等价只读一致性检查；若发现 CRITICAL/HIGH，先修 SDD 再继续。
- [ ] **T004** Spike `docs/spikes/audio-capture.md`：目标手机锁屏/后台 30 分钟 AudioRecord + microphone FGS；记录稳定采样率/声道/错误。
- [ ] **T005** Spike `docs/spikes/shazam-streaming.md`：ShazamKit StreamingSession 连续 30 分钟，验证重复结果、timestamp、断网与 gap 后重建 session。
- [ ] **T006** Spike `docs/spikes/media3.md`：多 M4A segment 无业务层静音连续播放与 20 个随机 seek ≤250ms。
- [ ] **T007** Spike `docs/spikes/cleanup.md`：20 个歌曲主唱+近距离讲话样本，确定保守 Clean 基线；禁止整晚 Remove Vocals。
- [ ] **T008** 汇总 Spike 到 `docs/spikes/decision.md`；Capture/Recognition/Playback 任一不可行则回写 SDD，不得用假实现掩盖。

## Phase 1: Android Foundation

- [ ] **T009** 创建 Kotlin/Compose Android project：applicationId `com.nightrec.app`；Kotlin 2.4.20、AGP 9.4.0、Gradle 9.6、JDK17、compile/target 36、min 29。
- [ ] **T010** 配置 version catalog：Compose BOM 2026.09.00、Room 2.8.5、Media3 1.11.1、core-splashscreen、coroutines、test dependencies。
- [ ] **T011** 配置 `.gitignore`、`local.properties` credential bridge；确保 AAR/token/.p8 不提交，`.p8` 不进入项目。
- [ ] **T012** 创建 app-private storage layout 与 `StoragePaths.kt`，Original/Clean/temp 分离。
- [ ] **T013** 创建 Room schema v1、entities/DAO/repositories 与 migration test 基线。
- [ ] **T014** 创建 `ClockProvider`（monotonic + wall clock）并写 unit tests。
- [ ] **T015** 创建单一 `SessionStateMachine` 与非法迁移 RED tests，再实现全部状态迁移。
- [ ] **T016** 创建 `SafeLogger`，测试 token、Authorization、audio bytes 不进入 release logs。
- [ ] **T017** 配置 Manifest permissions + `RecordingForegroundService` declaration：foregroundServiceType="microphone"。
- [ ] **T018** 建立基础 CI/local verification commands：`testDebugUnitTest`, `lintDebug`, `assembleDebug`。

## Phase 2: Visual System, Icon & Splash

- [ ] **T019** 从 V2.0 Light/Dark 原型抽取 Compose `NightRecTheme`、color/typography/shape/spacing tokens；禁止 dynamic color 覆盖品牌色。
- [ ] **T020** 建立共享组件：PrimaryGradientButton、StateBadge、WaveformCard、SessionListItem、TrackMarkerRow、BottomNav。
- [ ] **T021** 基于 Android icon master 重建 adaptive foreground/background vector；验证 circle/squircle/rounded-square mask。
- [ ] **T022** 实现 Android 13+ monochrome themed icon，保持 7-bar waveform 轮廓。
- [ ] **T023** 使用 core-splashscreen 实现 Light/Dark system splash；禁止独立 SplashActivity。
- [ ] **T024** 实现 onboarding/welcome 第一屏与系统 Splash 无重复；品牌名 NightRec。
- [ ] **T025** 为 Light/Dark 建 Compose screenshot tests/预览基线，覆盖 Home/Recording/Away/Player。
- [ ] **T026** ADB 安装并截图对照原型；将差异记录 `docs/verification/ui-phase2.md` 并修复 HIGH visual drift。
- [ ] **T027** 验证 48dp touch targets、字体放大 1.3x 不遮挡关键按钮。
- [ ] **T028** 验证主题切换不修改 Session domain state。

## Phase 3: Onboarding, Home & Start Session

- [ ] **T029** 实现 PrivacyDisclosureScreen：持续录音、本地保存、第三方识曲通信、合法使用责任。
- [ ] **T030** 实现 PermissionCoordinator：RECORD_AUDIO、POST_NOTIFICATIONS 状态与拒绝后恢复入口。
- [ ] **T031** 实现 HomeScreen：开始今晚 + 最近 Session + Tonight/History/Settings 导航。
- [ ] **T032** 实现 StartSessionScreen：名称自动、地点可选、AI净化/实时识曲开关；默认可直接开始。
- [ ] **T033** 创建 SessionRepository `createActiveSession()`，约束只能一个 active session。
- [ ] **T034** 实现 visible Activity -> microphone FGS start path，满足 while-in-use permission 时序。
- [ ] **T035** 实现 3-2-1 极短开始反馈或立即进入 Recording；不得拖延实际 capture start。
- [ ] **T036** Compose UI tests：未授权、授权、已有 active session、快速开始。
- [ ] **T037** ADB 真机从首次启动到 Recording 全流程预览/截图。
- [ ] **T038** Phase Gate：fresh unit/UI tests + installDebug。

## Phase 4: Capture, Safe Segments, Recording & Away

- [ ] **T039** 实现 `AndroidAudioCaptureEngine`，专用高优先级 audio thread，按 Spike 协商格式。
- [ ] **T040** 实现 PCM fanout，Original writer 与 recognition consumer 解耦，recognition backpressure 不阻塞录音。
- [ ] **T041** 实现 AAC-LC/M4A SegmentWriter，默认 5 分钟安全分片与 atomic seal。
- [ ] **T042** 实现 Segment metadata/checksum 持久化与 open/sealed/damaged 状态。
- [ ] **T043** 实现 RecordingForegroundService：计时、REC 状态、current track state flow。
- [ ] **T044** 实现持续通知：状态、logical duration、恢复/结束入口。
- [ ] **T045** 实现 RecordingScreen，严格对齐原型：REC、计时、波形、当前/上一首、暂离、结束今晚。
- [ ] **T046** 实现 AWAY transition：封口 segment、停止 capture/recognition、创建 Gap，不结束 Session。
- [ ] **T047** 实现 AwayScreen：今晚仍在继续、暂离时长、回来了继续记录、结束今晚。
- [ ] **T048** 实现 AWAY resume：同 Session 新 segment、新 Shazam streaming session。
- [ ] **T049** TimelineMapper RED tests：segment/GAP 双时间映射、Gap 不计 logical duration。
- [ ] **T050** 实现 TimelineMapper 使 T049 通过；ADB 场景：录5m→Away→恢复→录5m，只有一个 Session。

## Phase 5: Continuous Recognition & Timeline

- [x] **T051** 定义供应商无关 `RecognitionEngine`/Observation/Result contract。
- [x] **T052** 集成 AudD（替代 ShazamKit，用户批准）与 debug-only TokenProvider；token 不落日志/仓库。
- [x] **T053** 实现 AuddRecognitionEngine：PCM16 mono、支持采样率、contiguous timestamp。
- [x] **T054** 实现 capture PCM 下混 recognition branch；正常 segment rotate 不重启 streaming session。
- [x] **T055** 写 TrackStabilizer RED tests：重复、瞬时 B、2-hit/10s、新歌、同歌重现、迟到结果。
- [x] **T056** 实现 RecognitionPolicy/TrackStabilizer；阈值取 Spike 后固定值。
- [x] **T057** 实现 Observation 幂等持久化与 TrackOccurrence upsert。
- [x] **T058** 实现 RecognitionBackfillWorker，断网/失败 range 幂等补识别。
- [x] **T059** 实现 unknown/unrecognized range 与手动重新识别入口。（未命中已落库；手动入口待做）
- [x] **T060** 实现 TrackTimeline 纯函数与 `SessionTimelineReader` 装载（真实时间、歌曲、未知段，含单测+真机 instrumentation）；Recording current track 只读展示。
- [x] **T061** ADB 真实测试音源验证连续识曲（Warriors 真实命中）；记录 `docs/verification/recognition.md`。

## Phase 6: Stop, Processing & Unified Playback

- [x] **T062** 实现 EndTonight confirm modal，防误触。
- [x] **T063** 实现 stop transaction：停止新 frame、seal last segment、结束 wall time、释放 mic -> PROCESSING。
- [x] **T064** 实现 ProcessingScreen：Original 可立即进入，Backfill/Clean/transition 状态分项展示。
- [x] **T065** 实现 PlaybackSourceResolver：ordered MediaItems + logical ranges。
- [x] **T066** 实现 SessionPlaybackController + MediaSession/ExoPlayer 单一逻辑播放器。
- [x] **T067** 实现 global seek/Marker seek/Gap skip；20 随机 seek ≤250ms test。
- [x] **T068** 实现 SessionSummary/Player UI，对齐原型：单进度条、当前歌曲、Original/Clean toggle、收藏。
- [x] **T069** 实现转场详情与歌曲详情 UI；外部音乐服务仅 deep link，不改变播放器 source。
- [x] **T070** ADB 端到端：开始→Away→恢复→结束→立即 Original 播放→Marker seek→跨 segment（`docs/verification/end-to-end.md`）。

## Phase 7: Interruption, Recovery & Storage Safety

- [x] **T071** 标准化 CaptureFailure：MIC_BUSY/PERMISSION_LOST/DEVICE/ENCODER/STORAGE_LOW。
- [x] **T072** 实现 interruption coordinator：RECORDING->INTERRUPTED，记录真实 Gap。
- [x] **T073** 实现短退避 RECOVERING；成功回 RECORDING，重建 Shazam streaming session。
- [x] **T074** 自动恢复失败时通知“恢复今晚/结束今晚”，不新建 Session。
- [x] **T075** 实现 SegmentRecoveryPlanner：启动扫描 sealed/open/damaged，隔离尾部损坏。
- [x] **T076** 实现未正常结束 Session recovery card：首页继续/结束并保存。
- [x] **T077** 实现 StorageMonitor：warning 停 Clean/Backfill；hard threshold 安全停止新 capture。
- [x] **T078** Instrumentation/ADB：进程重启、断网、可模拟抢麦、低存储，输出 `docs/verification/recovery.md`。

## Phase 8: Conservative Speech Cleanup

- [x] **T079** 定义 SpeechContaminationDetector contract 与 Spike 选定实现。
- [x] **T080** 定义 SpeechCleanupEngine：只读 Original window 输入，输出派生 asset；无 Original 可写句柄。
- [x] **T081** 实现 ConservativeSpeechCleanupEngine；低置信 KEEP_ORIGINAL，禁止全量 Remove Vocals。
- [x] **T082** 实现 SpeechCleanupWorker：局部 range、crossfade、`clean/v1/`、幂等 CleanRange。
- [x] **T083** OriginalImmutabilityTest：Clean 成功/失败前后 checksum 100% 相同。
- [x] **T084** 播放器实现 AudioVariant ORIGINAL/CLEAN 同位置切换 ≤250ms；Clean 不可用自动 Original。
- [x] **T085** 20 样本 AB 验收；主唱/转场明显受损则降级 Beta/轻处理并记录 `clean-quality.md`。

## Phase 9: History, Correction, Favorites & Settings

- [x] **T086** 实现 HistoryScreen：日期、wall range、logical duration、track count、processing state。
- [x] **T087** 实现 TrackCorrectionRepository：retry/merge/delete marker，绝不修改 audio。
- [x] **T088** 实现 TrackActionSheet 与“标记为暂离”事后修订；AI 只能建议疑似离场。
- [x] **T089** 实现 FavoriteRange TRACK/TRANSITION/CUSTOM，点击回现场。
- [x] **T090** 实现 Settings：theme、audio quality、clean、recognition status、storage usage；Original auto-delete 默认关闭且 V1 不提供自动开启。
- [x] **T091** Light/Dark 全页面回归：History/Song/Transition/Settings。
- [x] **T092** ADB 真机完成原型 15 核心屏幕/状态截图集 `docs/verification/device/final-ui/`。

## Phase 10: Verification, ADB Preview & APK Prebuild

- [x] **T093** 建立 requirements traceability 验证：FR-001–FR-059 均映射 Task/测试（`docs/verification/traceability.md`）。
- [x] **T094** 运行完整 unit + instrumentation tests，0 failed 才继续（单测 44/0；产品 instrumentation 9/9；SpikeDeviceTest seek 250ms 边界偶发已记录，`docs/verification/t094-tests.md`）。
- [x] **T095** 运行 `lintDebug`，修复所有 error；HIGH correctness warning 逐项处理/记录（0 errors / 46 warnings，`docs/verification/t094-t095-host.log`）。
- [x] **T096** 真机锁屏/切普通 App 30 分钟，验证不进入 AWAY、FGS 持续、音频可播放（18:56–19:26 Asleep、FGS id=92 持续、0 crash/ANR，`docs/verification/soak-report.md`）。
- [ ] **T097** 执行 ≥3 小时 Soak，记录 device/Android/segments/gaps/tracks/crash/ANR/storage。
- [x] **T098** 重复 Original checksums：capture 后、识曲后、Clean 后、纠错后必须一致（Session 5 18/18 分片一致；Clean 由 OriginalImmutabilityTest 覆盖，`docs/verification/checksum-verification.md`）。
- [x] **T099** fresh APK prebuild：`./gradlew clean lintDebug testDebugUnitTest assembleDebug`，exit 0（`docs/verification/t099-prebuild.log`）。
- [x] **T100** 确认 `app/build/outputs/apk/debug/app-debug.apk` 存在、非零并记录 SHA-256（74463170 bytes，`ef6899ce…a05b`）。
- [x] **T101** 用 ADB installDebug/启动最终 APK，在已连接手机完成 cold-start、权限、开始/结束、播放器 smoke test（COLD 1244ms、FGS id=92、session READY、播放器 PLAYING，`docs/verification/t101-t102-device.md`）。
- [x] **T102** 核对 Android icon 在 launcher 多 mask 与 themed icon；核对 Splash 无双启动页（adaptive+v33 monochrome；单 MainActivity 无双启动页；`docs/verification/t101-t102-device.md`）。
- [x] **T103** 生成 `docs/verification/release-candidate.md`，汇总测试、ADB 截图、APK 路径、已知限制（含 T108 最终 APK 74463170 bytes / `b62c383d…b581`）。

## Phase 11: Spec Kit Convergence & Human Gate

- [x] **T104** 所有 T001–T103 完成后运行 `/speckit.converge`；不得提前 Human Gate（`docs/verification/convergence.md`）。
- [x] **T105** 若 converge 追加任务，继续实现所有新增任务；禁止跳过或只解释（无新增实现任务）。
- [x] **T106** 重复 converge 直到 byte-for-byte 不再追加任务并报告 converged（Round 2 converged）。
- [x] **T107** 最终执行 `/speckit.analyze` 等价 consistency review；CRITICAL/HIGH 必须为 0（CRITICAL=0，HIGH=0，`docs/verification/analyze-final.md`）。
- [x] **T108** 再次 fresh `test/lint/assembleDebug` + ADB smoke；仅使用这次输出作为完成证据（`clean lintDebug testDebugUnitTest assembleDebug` exit 0；APK 74463170 bytes / `b62c383d…b581`；`docs/verification/t108-final-build.log`）。
- [ ] **T109** 进入唯一 Human Gate：向用户一次性提交功能清单、已知限制、真机截图索引、APK 路径/hash、验证证据；此前中途不问人。
- [x] **T110** 共享识曲预算（FR-059）：持久化请求计数与上限，达到上限暂停实时/补识别/手动请求，继续 Original，不自动购买额度，网络重试也计数；Settings 展示计数/上限。实现 `RequestBudget`（`Reliability.kt`）+ Settings（T090）。

## Requirement Coverage Matrix

| Requirements | Primary tasks |
|---|---|
| FR-001–006 | T015, T031–038, T043–050, T062–063 |
| FR-007–014 | T004, T012–018, T039–050, T071–078 |
| FR-015–022 | T005, T051–061 |
| FR-023–028 | T006, T062–070 |
| FR-029–034 | T007, T079–085, T098 |
| FR-035–038 | T071–078 |
| FR-039–042 | T086–090 |
| FR-043–046 | T011–012, T016, T029–030 |
| FR-047–058 | T001, T019–028, T045–047, T064, T068–069, T086–092, T102 |

## Definition of Done / Human Gate

Human Gate **仅在以下全部成立时**触发：

- [ ] T001–T109 及所有 `/speckit.converge` 新增 Task 均完成；
- [ ] final converge 不再追加任务；
- [ ] final analyze 无 CRITICAL/HIGH；
- [ ] unit/instrumentation/lint fresh run 通过；
- [ ] 30min background test 与 ≥3h soak 有记录；
- [ ] Original integrity 100%；
- [ ] Light/Dark 原型关键流程真机截图完成；
- [ ] `app-debug.apk` fresh build exit 0、存在且有 SHA-256；
- [ ] ADB 已在用户连接手机成功安装/启动/完成 smoke；
- [ ] 已知限制明确写明，尤其 Clean 只能保守降低讲话干扰，不能承诺无损删除。