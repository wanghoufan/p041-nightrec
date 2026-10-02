# NightRec

> 在酒吧 / Club 里点一次「开始今晚」，之后锁屏、回消息都不影响；它会连续留存整晚的现场 DJ Set，并自动标出歌曲与转场位置。

简体中文 | [English](./README.en.md)

![录制中](./docs/verification/device/final-ui/05-recording-dark.png)

## 这是什么

NightRec 是一个 Android 应用（包名 `com.nightrec.app`），面向在连续音乐场景（酒吧、Club、演出）中想完整留住当晚声音的人。

它不产出零散的「歌曲列表」，而是先得到**一条可无缝拖动的整晚现场音轨**，再在音轨上叠加**歌曲 / 转场 Marker**。所以你回听的是昨晚真实的现场，而不是被切成一首首的平台原曲。

应用采用「先保住原版，再按需处理」的思路：现场原版（Original）一旦录下就永不修改，AI 净化只生成独立的派生版本，低置信时一律回退原版。

## 核心功能

- **一键开始、整晚连续**：进入录音后锁屏或切换到其他普通 App 都不会中断；后台有系统持续录音通知。
- **主动暂离不拆场**：点「暂离」安全封口当前分片、停麦克风；点「回来了」回到同一个 Session，并在时间轴留下真实的时间缺口。
- **连续识曲 + 稳定时间轴**：边录边识别（AudD），重复结果去重、A+B 混合时避免乱跳；未识别区间保留，可事后「重新识别这一段」。
- **结束即回听**：结束后现场原版立即可播；多段物理分片在界面里只有一条整晚进度条，点击歌曲 Marker 直接跳到昨晚的现场位置，暂离缺口默认跳过。
- **AI 净化（保守 Beta）**：可切换「现场原版 / AI 净化」，尽量降低附近讲话干扰，但保留歌曲主唱与 DJ 转场。
- **异常也尽量保住整晚**：抢麦、断网、进程重启、低存储时优先保住已录原版并支持恢复，不误结束当晚。

## 快速开始

前置：JDK 17、Android SDK（compile/target 36）、一台 minSdk 29 及以上的真机或模拟器。

```bash
# 1) 指向本机 Android SDK / JDK
#    local.properties 里写入 sdk.dir，或用环境变量 ANDROID_HOME / JAVA_HOME

# 2) 构建 debug APK
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 3) 安装到已连接的设备
./gradlew installDebug
#   或：adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

启动后授予麦克风权限，在首页点「开始今晚」即可开始记录今晚。

## 安装

### 前置要求

- JDK 17
- Android SDK：`compileSdk = 36`、`targetSdk = 36`、`minSdk = 29`
- Android Gradle Plugin 9.4.0 / Gradle 9.6（随仓库 `gradlew` 自带）

### 安装步骤

```bash
git clone https://github.com/wanghoufan/p041-nightrec.git
cd p041-nightrec
./gradlew installDebug
```

## 使用方法

1. 首次启动：确认隐私说明并授予麦克风权限（未授权无法开始录音，会给出修复入口）。
2. 首页点「开始今晚」；名称、地点、AI 净化、实时识曲均为可选，可直接开始。
3. 把手机放桌上、锁屏或用其他 App——只要没点「暂离」，Session 会继续。
4. 需要离开时点「暂离」；回来后点「回来了，继续记录」，仍属同一个今晚。
5. 真正结束：点「结束今晚」并在确认弹窗中确认；随后可立即进入播放器回听。
6. 在播放器里拖动整晚进度条、点歌曲 Marker 回现场，或用「现场原版 / AI 净化」切换音轨。

## 配置

开始使用只需真机权限，无需额外配置。**识曲功能可选**，如需真实识别：

在 `local.properties`（已被 `.gitignore` 忽略，永不入库）中填写：

| 键 | 说明 |
| --- | --- |
| `AUDD_API_TOKEN` | AudD 识曲服务的 API Token（本地调试用） |
| `AUDD_REQUEST_LIMIT` | 本地请求预算上限，默认 `300` |

> Token 只会写入 debug 构建的 `BuildConfig`，release 构建为空；请勿把真实 Token 提交到仓库。

## 详细文档

- 产品规格与任务：[specs/001-night-session/spec.md](./specs/001-night-session/spec.md)、[plan.md](./specs/001-night-session/plan.md)、[tasks.md](./specs/001-night-session/tasks.md)
- 技术决策记录：[docs/decisions/](./docs/decisions/)
- 验证与真机证据：[docs/verification/](./docs/verification/)
- Android 构建规范：[docs/sop/android.md](./docs/sop/android.md)
- 开发交接：[docs/handoff/HANDOFF.md](./docs/handoff/HANDOFF.md)

## 已知限制

- **Clean 为保守 Beta**：只降低近距离讲话干扰，**不承诺无损删除讲话**，不做整晚 Remove Vocals；低置信一律回退现场原版。
- **识曲依赖 AudD 与共享预算**：请求次数有上限，耗尽后仅暂停网络识曲（含补识别 / 手动重识别），现场采集与播放继续，且不自动付费。
- **识曲需要真实外放音源**：有线耳机场景下麦克风物理收不到声音，无法验证识曲。
- **当前为 debug 构建**：未做 release 签名与混淆，属于本地验证用途。
- **无独立 License**：仓库未附带开源许可证。

## License

未附 License（私有仓库）。