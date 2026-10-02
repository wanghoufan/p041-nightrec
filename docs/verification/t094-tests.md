# T094｜Unit + Instrumentation 完整测试证据

- 时间：2026-10-03（Asia/Shanghai）。
- 设备：USB `indq5xfi6hovay4d`（ruby / 22101316C / Android 14）。
- APK：`app/build/outputs/apk/debug/app-debug.apk`（T108 最终构建）。

## 1. 单元测试（JVM）

- 命令：`./gradlew testDebugUnitTest --rerun-tasks`
- 结果：**44 passed / 0 failed / 0 skipped**（`app/build/test-results/testDebugUnitTest/*.xml` 汇总）。
- 证据：`docs/verification/t108-final-build.log`（T108 同一构建内含 `testDebugUnitTest`，BUILD SUCCESSFUL）。

## 2. 真机 Instrumentation（AndroidJUnitRunner）

安装：`adb install -r -t` 主 APK + androidTest APK（MIUI「继续安装」），补 `RECORD_AUDIO`/`POST_NOTIFICATIONS` grant。
运行：`adb shell am instrument -w com.nightrec.app.test/androidx.test.runner.AndroidJUnitRunner`。

### 产品 instrumentation 套件 — 全通过（9/9）

| 类 | 用例数 | 结果 |
|---|---|---|
| `OriginalImmutabilityTest` | 2 | OK |
| `RecognitionPersistenceTest` | 5 | OK |
| `RoomBaselineTest` | 2 | OK |

原始输出：`docs/verification/t094-instrumentation.log`。

### SpikeDeviceTest（Phase 0 spike 产物，T004–T008）

| 用例 | 结果 |
|---|---|
| `yamnetRunsOnDeviceAndProducesFiniteScores` | PASS |
| `classifyTwentyControlledVocalSpeechMixtures`（20 样本 AB） | PASS |
| `actualSealedSegmentsPlayAcrossBoundaryAndSeekTwentyTimes` | **边界偶发**（见 §3） |

> 说明：该用例依赖设备上真实 probe 采集分片（`files/probe/<ts>/*.m4a`，≥2 个）。本次已用 debug `ProbeActivity` 重新采集约 5.5 分钟生成真实 fixture（000/001.m4a，均带 sha256）后运行。

## 3. 已知边界：seek ≤250ms 偶发超阈

- `actualSealedSegments...SeekTwentyTimes` 断言「单次随机 seek ready ≤250ms」。
- 实测正常分布：单次 seek **46–236ms**；一完整通过轮（20/20）最大 **236ms**。
- 偶发：约每 2–3 轮出现一次 **254–256ms**（超阈值 4–6ms），导致该用例间歇性失败。
- 判定：真实设备 ExoPlayer seek 时延在 250ms 阈值的**边界抖动**，非功能回归；产品采集/播放链路不受影响。
- 证据：`docs/verification/t094-instrumentation.log`；设备 `files/media3-spike.json` 记录的逐次 `readyMs`。

## 4. 结论

- 产品回归面（单元 44 + 产品 instrumentation 9）**0 failed**。
- SpikeDeviceTest 2/3 通过，1 项为 250ms 边界时序偶发（已记录，不修改阈值、不掩盖）。