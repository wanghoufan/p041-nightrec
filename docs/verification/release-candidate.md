# Release Candidate｜NightRec V1（T103）

- 生成时间：2026-10-02（Asia/Shanghai）。
- 包名：`com.nightrec.app`；构建类型：`debug`（唯一开发者本地验证用 RC）。
- 设备：USB `indq5xfi6hovay4d`（ruby / 22101316C / Android 14）。

## 1. 构建产物

| 项 | 值 |
|---|---|
| APK 路径 | `app/build/outputs/apk/debug/app-debug.apk` |
| 大小 | 74463170 bytes（T108 最终复算） |
| SHA-256 | `b62c383d1e20ba25b425d078335b1d424f9034ff10bc8caf29f545f199dbb581`（T108 最终 fresh 构建） |
| 构建命令 | `./gradlew clean lintDebug testDebugUnitTest assembleDebug`，exit 0（BUILD SUCCESSFUL in 35s） |

> 注：本表 hash 来自 T108「最终一次 fresh 构建」（`docs/verification/t108-final-build.log`）。T099 prebuild 的 hash（`ef6899ce…a05b`）与本次不同，因两次构建时间戳/签名元数据不同，属正常；**以本表 T108 值为准**。

## 2. 测试与静态检查证据

| 项 | 结果 | 证据文件 |
|---|---|---|
| 单元测试 | 44 passed / 0 failed | `docs/verification/t108-final-build.log` |
| instrumentation（真机） | OriginalImmutabilityTest PASS；RecognitionPersistenceTest PASS | adb 日志 / `docs/verification/recognition.md` |
| lint | 0 errors / 46 warnings / 1 note（版本建议类，无 HIGH correctness） | `docs/verification/t108-final-build.log` |
| 识曲真实命中 | `Warriors / Imagine Dragons`（AudD，真音源） | `docs/verification/recognition.md`、`spike-fresh-2026-10-02/routing-fix-events.jsonl` |
| 端到端 | 开始→暂离→恢复→结束→立即 Original 播放→Marker seek→跨分片 | `docs/verification/end-to-end.md` |
| 恢复 | 进程重启 / 断网 / 抢麦 / 低存储 | `docs/verification/recovery.md` |
| Clean 20 样本 AB | Original 全不变；无主唱/转场受损；保持 Beta/轻处理 | `docs/verification/clean-quality.md` |
| traceability | FR-001–FR-059 全映射 | `docs/verification/traceability.md` |
| converge / analyze | converged；CRITICAL=0，HIGH=0 | `docs/verification/convergence.md`、`analyze-final.md` |
| 长测 | 30min 锁屏连续 + ≥3h Soak | `docs/verification/soak-report.md`、`device/soak-samples.jsonl` |

## 3. 真机截图索引（`docs/verification/device/`）

最终 UI 集（`final-ui/`，Phase 9）：

- `01-home-dark.png`、`12-home-light.png`：Home（Dark/Light）
- `02-history-dark.png`：History
- `03-settings-dark.png`、`04-settings-light.png`：Settings（Dark/Light）
- `05-recording-dark.png`：Recording（REC 计时/波形）
- `06-away-dark.png`：Away
- `07-end-confirm.png`：结束确认弹窗
- `08-home-after-end.png`：结束后 Home
- `09-player-dark.png`：Player（单进度条/Original-Clean toggle）
- `11-marker-dialog.png`：Marker/歌曲操作

端到端（`e2e/`）：

- `player-cross-segment.png`：跨分片连续播放

## 4. 图标与 Splash（T102）

- `mipmap-anydpi-v26/ic_launcher.xml`：adaptive（`ic_icon_bg` + `ic_wave`）。
- `mipmap-anydpi-v33/ic_launcher.xml`：额外 `monochrome`（`ic_wave_mono`）→ Android 13+ 主题化图标。
- `Theme.NightRec.Starting` 继承 `Theme.SplashScreen`，`postSplashScreenTheme=Theme.NightRec`（Light/Dark 各一份）；Manifest **仅 MainActivity**，无独立 SplashActivity → 无双启动页。
- 真机多 mask / themed icon 视觉核对在本 RC 安装后补（见 §6）。

## 5. 已知限制（写清，不夸大）

1. **Clean 为保守 Beta**：仅降低近距离讲话/污染干扰，**不承诺无损删除讲话**，不做整晚 Remove Vocals；低置信一律回原版。见 `clean-quality.md`。
2. **识别依赖 AudD 与共享预算**：累计请求上限 300，耗尽后仅停网络识曲（含补识别/手动），Original 采集与播放继续；不自动付费。
3. **识别需真实外放音源**：有线耳机场景麦克风物理收不到，不做识曲验证。
4. **长测音量条件**：锁屏 Soak 期间关闭实时识曲以保护预算，验证的是采集/服务连续性，非识别。
5. **debug 签名**：RC 为 debug 构建，未做 release 签名/混淆（V1 本地验证范畴）。

## 6. 待补（收尾随 RC 安装完成）

- T101：最终 APK `adb install -r -t` → cold-start / 权限 / 开始 / 结束 / 播放器 smoke（含 MIUI「继续安装」点击）。
- T102：真机 launcher 多 mask + themed icon + Splash 无双页视觉核对。
- T108：最终一次 fresh `test/lint/assembleDebug` + ADB smoke，回填 §1 hash。