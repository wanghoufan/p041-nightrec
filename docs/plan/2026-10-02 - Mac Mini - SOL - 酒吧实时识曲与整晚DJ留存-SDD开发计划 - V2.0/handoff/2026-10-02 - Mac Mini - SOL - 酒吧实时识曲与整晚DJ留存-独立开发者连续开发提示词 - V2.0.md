# 独立开发者连续开发提示词（NightRec V2.0）

本次目标：读取本开发包的 `.specify/memory/constitution.md`、`specs/001-night-session/spec.md`、`plan.md`、`tasks.md`、`design/` 全部视觉资产后，由你作为**唯一开发者**连续完成 NightRec Android V1，直到全部既有 Task + 后续 `/speckit.converge` 新增 Task 都完成，才进入 Human Gate。

## 0. 唯一允许的开发前提问

开始编码前先做一次且仅一次“凭据/环境预检”。一次性检查并集中向我索取缺失项：
- ShazamKit Android AAR 本地文件/路径；
- `SHAZAM_DEVELOPER_TOKEN`；
- `adb devices -l` 能看到的目标手机（手机已 USB 有线连接）。

不要索取数据库 API key：V1 使用本地 Room。不要让我把 Apple `.p8` 私钥放进项目或 APK。完成这一次预检并取得必需项后，中途不得再因为一般技术选择、UI 小问题、命名、阈值、重构、测试失败来问我。

## 1. 连续执行规则

- 严格从 T001 开始按依赖连续推进，不等待我回复“继续”。
- 小问题自行根据优先级决策：Constitution > Spec > Plan > Tasks > design 原型正文小字。
- 无法确定但不阻塞的事项，选择最保守、最可逆方案，并写入 `docs/decisions/`。
- 不得删减需求来换取“完成”；不得用 mock 假装真实识曲完成。Spike 发现重大不可行时先修 SDD，再继续。
- 每个 Task 完成前必须有 fresh verification 证据；测试失败就修到通过再进入下一 Task。

## 2. UI 与视觉是硬约束

产品名固定 **NightRec**，业务对象叫 **Night Session**，禁止出现 NightSet。

必须使用 `design/` 中：
- 深色产品原型；
- 浅色产品原型；
- Android 图标；
- 浅/深启动画面；
- 视觉资产规范。

Light/Dark 页面结构一致。原型图文字如果有生成错字，以 SDD 文案为准。

Android launcher icon 必须实现 adaptive icon + Android 13+ monochrome themed icon。启动页必须使用 Android SplashScreen API/compat，禁止独立 SplashActivity 导致双启动页。

## 3. ADB 真机实时预览

开发开始执行 `adb devices -l`；若多设备，固定 `ANDROID_SERIAL`。

每完成一个 UI/交互 phase，自动执行：
```bash
./gradlew :app:installDebug
adb shell am force-stop com.nightrec.app
adb shell am start -n com.nightrec.app/.MainActivity
adb exec-out screencap -p > docs/verification/device/<phase>.png
```

同时检查 crash/ANR/logcat。不要等 Human Gate 才第一次上真机。

## 4. Android 本地 APK 预构建

Human Gate 前必须 fresh 执行：
```bash
./gradlew clean lintDebug testDebugUnitTest assembleDebug
./gradlew :app:installDebug
adb shell am start -n com.nightrec.app/.MainActivity
```

确认 `app/build/outputs/apk/debug/app-debug.apk` 存在、非零，计算 SHA-256，并在连接手机完成 cold start + 权限 + 开始今晚 + 暂离/继续 + 结束 + 播放 smoke test。

这个 debug APK 是本地预构建/侧载件，不等同 Play release；不要为了 release signing 阻塞本轮。

## 5. Spec Kit 闭环

编码前：执行 `/speckit.analyze` 等价检查并解决 CRITICAL/HIGH。

现有 Tasks 全完成后：
1. 运行 `/speckit.converge`；
2. 如果追加 Task，继续实现全部新增 Task；
3. 再次 converge；
4. 循环直到 converged、不再追加 Task；
5. 最后再做 analyze + fresh tests/lint/APK/ADB smoke。

## 6. Human Gate

**只有全部 Task、所有 converge Task、测试、真机、APK 预构建均完成后才停下来找我。**

Human Gate 一次性提交：
- 完成/未完成清单（正常应为全部完成）；
- final converge/analyze 结果；
- unit/instrumentation/lint 结果；
- 30min 后台与 ≥3h soak 结果；
- Original checksum 完整性；
- Light/Dark 真机截图索引；
- `app-debug.apk` 路径、大小、SHA-256；
- 已知限制，尤其 AI 净化不能承诺 100% 无损删除现场讲话。

在此之前不要中断，不要问“要不要继续”，不要逐 Task 请示。
