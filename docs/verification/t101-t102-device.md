# T101 / T102｜最终 APK 真机 smoke 与图标/Splash 核对

- 时间：2026-10-03（Asia/Shanghai）。
- 设备：USB `indq5xfi6hovay4d`（ruby / 22101316C / Android 14）。
- APK：`app/build/outputs/apk/debug/app-debug.apk`（74463170 bytes，SHA-256 `b62c383d1e20ba25b425d078335b1d424f9034ff10bc8caf29f545f199dbb581`，T108 最终构建）。

## T101 冒烟（全部通过）

| 步骤 | 命令/操作 | 结果 |
|---|---|---|
| 安装 | `adb install -r -t` 最终 APK（MIUI「继续安装」） | Success |
| 权限 | `pm grant RECORD_AUDIO` + `POST_NOTIFICATIONS` | granted=true |
| cold-start | `am start -W -n .MainActivity` | LaunchState=**COLD**，TotalTime=1244ms，无 crash/ANR |
| 首页 | 截图 `01-coldstart-home.png` | Dark 主题、渐变「开始今晚」、最近记录空态、底部导航 |
| 开始页 | 截图 `02-start-session.png` | 名称「今晚·10/03 00:13」、地点、**实时识曲默认关**、AI 净化开 |
| 开始录制 | 点「开始今晚」 | `RecordingForegroundService` foregroundId=92、type=microphone、通知在 |
| 录制页 | 截图 `03-recording.png` | REC 徽标、计时、波形、暂离/结束今晚 |
| 结束确认 | 点「结束今晚」 | 弹窗「结束今晚？已记录 00:00:35…」，截图 `04-end-confirm.png` |
| 停止事务 | 点「结束并保存」 | 服务已停；首页新 Session 状态 **READY**，截图 `05-after-end.png` |
| 播放器 | 点 session 行 | Original/收藏、总长 00:00:45、播放/暂停、切到 Clean，截图 `06-player.png` |
| 播放 | 点「播放」 | MediaSession `state=PLAYING(3)`，UI 位置推进（0:04→），截图 `07-player-playing.png` |

全程 `logcat` 无 `FATAL EXCEPTION` / `ANR in com.nightrec`。

## T102 图标与 Splash 核对

- **Splash 无双启动页（通过）**：`dumpsys activity activities` 冷启动后仅 `Hist #0 com.nightrec.app/.MainActivity`、单任务单 Activity；代码侧 `installSplashScreen()` + 应用主题 `Theme.NightRec.Starting`（`postSplashScreenTheme=Theme.NightRec`），Manifest 仅 MainActivity，**无 SplashActivity**。冷启动主界面到达极快，未捕获到独立第二启动页。
- **Adaptive 图标渲染（通过）**：应用信息页截图 `09-app-info-icon.png`，圆角方块 mask 下渐变 7 波形正常。
  - `mipmap-anydpi-v26/ic_launcher.xml`：`ic_icon_bg` + `ic_wave`（adaptive）。
  - `mipmap-anydpi-v33/ic_launcher.xml`：额外 `<monochrome android:drawable="@drawable/ic_wave_mono"/>`（Android 13+ themed icon）。
- **限制**：本机 launcher 无法强制切换 circle/squircle/rounded-square mask，也无法强制开启系统「主题化图标」；多 mask 与 themed icon 依赖 launcher/系统设置，adaptive-icon 结构已保证 mask 兼容。此项据代码配置 + 实际渲染判定通过，已在本文档记录该限制。

## 证据文件

`docs/verification/device/t101-t102/`：`01-coldstart-home.png`、`02-start-session.png`、`03-recording.png`、`04-end-confirm.png`、`05-after-end.png`、`06-player.png`、`07-player-playing.png`、`09-app-info-icon.png`。