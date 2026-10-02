# Implementation Plan: NightRec — 整晚 DJ Set 连续留存

**Spec**: `specs/001-night-session/spec.md`  
**Constitution**: `.specify/memory/constitution.md`  
**Visual SSOT**: `design/` 下 V2.0 原型、Android 图标、启动画面  
**Execution Mode**: 单开发者连续执行，所有 Task 完成并 converged 后才 Human Gate

## Summary

构建 Android 原生 NightRec：前台明确启动 microphone foreground service；AudioRecord 采集一条稳定 PCM 源，分为不可阻塞的 Original 编码/安全分片支路与 ShazamKit 连续识曲支路；Room 保存 Session/Gap/Marker/Job；Media3 把多物理分片映射成单一逻辑播放器；Clean 作为独立、可失败、可回退的保守派生链。Light/Dark UI 必须依据包内 V2.0 原型实现；ADB 已连接设备是开发阶段真机预览目标；Human Gate 前必须生成并安装 debug APK。

## Current Official Baseline (verified 2026-10-02)

- Kotlin: **2.4.20**（2026-09-07 stable）
- Android Gradle Plugin: **9.4.0**（2026-09-18 stable）
- Gradle: **9.6.0**（AGP 9.4 默认/最低）
- JDK: **17**
- Compile SDK: **36**
- Target SDK: **36**
- Min SDK: **29**
- Compose BOM: **2026.09.00 stable**
- Room: **2.8.5 stable**
- Media3: **1.11.1 stable**
- Android 17 / API 37: 当前仍为 Beta；只做兼容性测试 lane，**不得作为 V1 target baseline**。
- Google Play 自 2026-08-31 要求新应用/更新 target Android 16 (API 36) 或更高；V1 直接 target 36。
- ShazamKit Android: 官方 Android AAR；StreamingSession 输入 PCM16 MONO，支持 48/44.1/32/16kHz；连续流要求 audio timestamp 且避免 gap。

## Upfront Credential Preflight

开发者在写代码前只能进行一次集中凭据预检。需要：

1. `shazamkit-android-release.aar`（用户从 Apple 官方下载后提供本地路径）；
2. `SHAZAM_DEVELOPER_TOKEN`（开发/真机识曲使用）；
3. 不接收、也不要求把 Apple `.p8` 私钥放进 Android 项目；
4. V1 不需要数据库 API key；Room 本地运行；
5. V1 Clean 默认走本地/设备端保守处理与可插拔接口，因此不强制 AI 云 API key。

若未来需要生产自动刷新 token，另开 token-broker feature；本 V1 只完成本地 debug 可运行闭环。

## Constitution Check

| Principle | Plan response | Status |
|---|---|---|
| Continuous Set First | 单 Night Session + logical timeline + Media3 multi-segment | PASS |
| Original Immutable | app-private Original + checksum + Clean 独立 | PASS |
| Capture Before Intelligence | capture pipeline 与 recognition/cleanup 异步解耦 | PASS |
| Explicit State | 单 `SessionStateMachine` | PASS |
| Conservative Cleanup | 局部 detector + low-confidence keep-original | PASS |
| Visual SSOT | design assets 作为 UI 验收真源 | PASS |
| Android/Security | API36 stable baseline；FGS；secret 不入 repo | PASS |
| Evidence/Human Gate | ADB + APK + analyze/converge + fresh verification | PASS |

## Architecture

### 1. Module / Package shape

```text
app/
  src/main/java/com/nightrec/app/
    app/
    core/
      time/
      logging/
      permissions/
      design/
    domain/
      session/
      timeline/
      recognition/
      cleanup/
    data/
      db/
      repository/
      storage/
    audio/
      capture/
      encode/
      recognition/
      playback/
      cleanup/
    service/
    worker/
    feature/
      onboarding/
      home/
      recording/
      session/
      history/
      settings/
```

### 2. Capture pipeline

```text
AudioRecord PCM
   ├─ Lossless-in-memory PCM fanout
   │    ├─ Original encoder -> 5 min AAC-LC/M4A segment -> atomic seal
   │    └─ Recognition branch -> mono PCM16 -> ShazamKit StreamingSession
   └─ Session clock / state observer
```

Rules:
- Original 优先 stereo 48kHz；设备不支持时协商稳定格式并记录能力。
- Recognition branch 独立下混 mono，不得强迫 Original mono。
- Recognition consumer backpressure 不得阻塞 Original writer。
- 正常 5 分钟分片切换不能重启 Shazam streaming；AWAY/INTERRUPTED 后恢复必须新建 streaming session，因为音频已产生真实 gap。

### 3. State machine

```text
IDLE -> STARTING -> RECORDING
RECORDING -> AWAY -> RECORDING
RECORDING -> INTERRUPTED -> RECOVERING -> RECORDING
RECORDING/AWAY/INTERRUPTED -> STOPPING -> PROCESSING -> READY
```

非法迁移由 domain 层拒绝；UI/Service 不得各自复制状态判断。

### 4. Time model

- `monotonicStartNs` / monotonic clock：计算 duration 与逻辑位置。
- `wallStartEpochMs`：现实时间展示。
- `AudioSegment(logicalStartMs, logicalDurationMs, wallStartMs)`。
- `SessionGap(wallStartMs, wallEndMs, reason)` 不进入 logical duration。
- `TimelineMapper` 负责 logical ↔ segment/offset ↔ wall time。

### 5. Recognition

`RecognitionEngine` 抽象供应商；V1 `ShazamRecognitionEngine`。

- ShazamKit AAR 放 `app/libs/`，不提交私有 token。
- `DeveloperTokenProvider` 从 debug-only local config 读取 token。
- `RecognitionObservation` 进入内部 domain，不把 Apple SDK type 写入 Room/domain。
- `TrackStabilizer` 处理重复、瞬时误判、迟到结果和同歌再次出现。
- 初始策略：连续两个可信 observation 或 10s 稳定窗口才确认新曲；具体阈值在 Spike 后固化测试。

### 6. Playback

Media3 ExoPlayer / MediaSession 维护有序 segment playlist。UI 只暴露整晚 `logicalDurationMs`。`PlaybackSourceResolver + TimelineMapper` 完成 seek、Gap 跳过与 Marker 跳转。

### 7. Speech cleanup

V1 不采用“整晚 Remove Vocals”。接口：

- `SpeechContaminationDetector`
- `SpeechCleanupEngine`
- `CleanRange`

流程：检测局部疑似近距离讲话 -> 高置信局部轻度抑制/修复 -> crossfade -> 写 `clean/v1/`。质量门不满足时保持 Beta 并 `KEEP_ORIGINAL`。Clean 绝不成为 Original 依赖。

## Visual Design Implementation

### Product naming

- App display name: `NightRec`
- Domain: `Night Session`
- 中文核心文案：`开始今晚`、`暂离`、`回来了，继续记录`、`结束今晚`、`现场原版`、`AI 净化`。
- 禁止 UI/代码模块再使用 `NightSet` 作为品牌名。

### Normative assets

`design/` 中以下文件 MUST 先读后开发：

- 深色完整产品原型图
- 浅色完整产品原型图
- NightRec Android 图标 master
- 浅色启动画面参考
- 深色启动画面参考
- 视觉资产使用规范

原型用于信息架构、层级、控件位置、状态与视觉方向；正文小字若与 Spec 不一致，以 Spec 文案为准。

### Theme

- Material 3 + 自定义 NightRec tokens；不依赖 dynamic color 改写品牌主色。
- Brand gradient：violet → magenta/pink → orange/yellow，仅用于强调、波形、关键 CTA，不大面积铺满正文。
- Light：近白/浅紫背景 + 深色文字。
- Dark：近黑靛背景 + 白/浅灰文字。
- touch target ≥48dp；支持字体放大。

### Icon

master PNG 是视觉真源；开发时重建成 adaptive icon：

- Background：深靛近黑。
- Foreground：7 条圆角竖向波形柱，紫→粉→橙渐变。
- Android 13+ monochrome：同一 7-bar 轮廓的单色 vector。
- 必须检查圆形、圆角方形、squircle 等 mask 下不裁切关键波形。

### Splash

必须用 `androidx.core:core-splashscreen` / Android 12+ SplashScreen 语义：

- 单色背景 + adaptive icon；
- 不新增专用 SplashActivity；
- 深色/浅色分别使用对应背景；
- 系统 Splash 后直接进入 onboarding/home，禁止再展示一张“第二启动页”。

包内 splash PNG 是视觉验收参考，不要求直接整图塞入系统 Splash。

## Data Model

- `NightSessionEntity(id, name, place, state, wallStartMs, wallEndMs, logicalDurationMs, createdAt, updatedAt)`
- `AudioSegmentEntity(id, sessionId, index, path, logicalStartMs, logicalDurationMs, wallStartMs, format, state, checksum)`
- `SessionGapEntity(id, sessionId, reason, wallStartMs, wallEndMs, userConfirmed)`
- `RecognitionObservationEntity(idempotencyKey, sessionId, audioTimestampMs, provider, rawSongId, confidenceLikeState, receivedAt)`
- `SongEntity(id, title, artist, album, artworkUrl, externalIds...)`
- `TrackOccurrenceEntity(id, sessionId, songId, firstTrustedLogicalMs, confirmedLogicalMs, wallTimeMs, state)`
- `UnrecognizedRangeEntity`
- `ProcessingJobEntity(type, range, status, attempt, idempotencyKey)`
- `CleanRangeEntity(sessionId, logicalStartMs, logicalEndMs, status, assetPath, confidence)`
- `FavoriteRangeEntity(type, logicalStartMs, logicalEndMs)`
- `UserSettingsEntity(themeMode, cleanEnabled, audioQuality, ... )`

## Android Permission / FGS Strategy

Manifest minimum relevant items：

- `RECORD_AUDIO`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MICROPHONE`
- `INTERNET`
- Android 13+ `POST_NOTIFICATIONS` 用于完整通知体验

`RecordingForegroundService` MUST declare `android:foregroundServiceType="microphone"` and be started from visible Activity after user action and while-use microphone permission is available.

## Storage

- Original: `filesDir/sessions/<sessionId>/original/`
- Clean: `filesDir/sessions/<sessionId>/clean/v1/`
- DB: Room app-private database
- Temp: `cacheDir/`
- 不把 Original 默认写 public Music/Downloads。

## ADB Real-device Preview Workflow

用户手机已通过 USB ADB 连接。开发者 MUST 在开发开始先执行：

```bash
adb devices -l
```

要求恰好有一个目标 device 为 `device` 状态；如果有多个，固定 `$ANDROID_SERIAL`。

每个 UI/交互 phase 完成后：

```bash
./gradlew :app:installDebug
adb shell am force-stop com.nightrec.app
adb shell am start -n com.nightrec.app/.MainActivity
adb exec-out screencap -p > docs/verification/device/<phase>.png
```

同时监看 crash/ANR：

```bash
adb logcat -c
adb logcat AndroidRuntime:E ActivityManager:E *:S
```

“实时预览”的工程定义是：视觉任务提交前自动 build/install/relaunch + 真机 screenshot；不依赖 IDE 专属 Live Edit 才能完成。

## Local APK Prebuild Gate

Human Gate 前 MUST fresh run：

```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.nightrec.app/.MainActivity
```

输出：`app/build/outputs/apk/debug/app-debug.apk`。

这个 APK 是本地 sideload 预构建，不等同 Play 可发布 release。正式 release signing / token broker 不属于 V1 Human Gate 前置条件。

## Spec Kit Execution Loop

1. 读取 Constitution / Spec / Plan / Tasks / design assets。
2. 在编码前执行 `/speckit.analyze` 等价检查，修正文档中的 CRITICAL/HIGH（本包已人工预检，开发仓库仍需重新跑）。
3. 按 `tasks.md` 依赖顺序连续实现，不逐任务问用户。
4. 所有既有 Task 完成后运行 `/speckit.converge`。
5. 如果 converge 追加任务，继续 `/speckit.implement` / 实施新增任务。
6. 重复 converge，直到不追加任务。
7. fresh tests + ADB + APK prebuild。
8. 只有此时进入 Human Gate。

## Test Strategy

- Unit：状态机、TimelineMapper、TrackStabilizer、immutability、repository。
- Integration：Room migrations、segment sealing/recovery、Media3 source mapping、Shazam adapter mock boundary。
- Instrumentation：权限/onboarding、FGS start、AWAY/resume、stop、playback seek、theme switch。
- Real device：后台/锁屏 30m、麦克风中断、ADB UI 验收、APK install。
- Soak：≥3h 真机或可重复长时测试；记录分片数、异常、耗电/存储、ANR/crash。
- Clean AB：至少 20 个“歌曲主唱 + 附近讲话”样本，质量不足不得扩大处理强度。

## Security / Privacy

- `.p8` 永不进入客户端项目。
- debug token 从 `local.properties` / env 注入，不提交 git；SafeLogger 脱敏。
- ShazamKit 官方说明匹配使用音频签名而非共享原始音频；产品隐私文案仍需说明第三方识曲通信。
- 不提供默认“分享整晚录音”。
- 提醒用户遵守所在地录音、隐私、场所与版权规则。

## Official References

- GitHub Spec Kit current templates: `github/spec-kit` main (`constitution`, `plan`, `tasks`, `analyze`, `implement`, `converge`).
- Android FGS microphone: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android Play target API: https://developer.android.com/google/play/requirements/target-sdk
- Android SplashScreen: https://developer.android.com/develop/ui/views/launch/splash-screen
- AGP 9.4: https://developer.android.com/build/releases/agp-9-4-0-release-notes
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- ShazamKit Android: https://developer.apple.com/shazamkit/android/
