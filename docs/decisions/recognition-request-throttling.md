# 识曲请求节流（省 AudD 额度）

起因：2026-10-02 AudD 免费试用 300 次被烧尽（服务端 `code=902 the limit was reached`）。原实现为背靠背 12s 窗口，**每 12 秒必发一次**，空场/无声环境照样空跑；89 分钟长测（开着实时识曲）约合 400+ 次当量，叠加开发期反复真机测试即触顶。

## 决定（三处，只在识曲模块，不碰录音/播放/Original）

1. **静音跳过**：窗口 mono PCM16 的 RMS 低于阈值（默认 `silenceRmsThreshold=300`，约 -40 dBFS）时判定静音，直接跳过、不发请求，且不落 unknown range。实时链（`RecognitionWindow`）与补识别链（`RecognitionBackfill`）共用同一判定。
2. **命中冷却**：一首歌命中后，其后 `hitCooldownMs=60_000`（逻辑时间）内不再发请求（同曲连续窗口无需反复上传）。实测把“已识别歌曲段”的请求从 5 次/分钟降到约 1 次/分钟。
3. **实时识曲默认关闭**：`StartSessionScreen` 开关默认 `false`，仅在用户明确需要时开启；长测一律关闭。

### 补丁：门控失效修复（2026-10-03）

T097 重跑长测（实时识曲已关）仍发现 `files/recognition/budget.txt` 从 15 增至 33，**开关形同虚设**。根因：`RecordingForegroundService.startCapture`/`resumeCapture` 无条件调用 `startRecognition(sessionId)`，从未读取开关。

修复：新增门控谓词 `shouldStartLiveRecognition(settingsEnabled, liveRecognition)`，`startRecognition` 仅在「全局设置 `recognitionEnabled` ∧ 本次开始页 `liveRecognition`」同时为真时启动；服务新增 `EXTRA_LIVE_RECOGNITION`，`RecordingUiState.liveRecognition` 跨 AWAY/resume 保持。属 B 类局部功能变化，未重开 Plan。

## 效果与口径

- 空场/间隙不消耗额度；同一首歌期间约 1 次/分钟。据此 Indie（1000 次/月）可覆盖约 6–8 小时一晚（此前无节流约 3h20m）。
- Original 采集、播放、整晚留存与识曲额度无关，始终可用。
- 本地预算文件仅个人侧载调试参考，不能代表服务端余额；不得清零冒充服务端额度（见 `recognition-budget-storage.md`）。

## 验证

- 单测 45 通过（`silentWindowIsSkippedButLoudWindowPasses`；门控 `liveRecognitionGateRequiresBothSettingsAndSessionToggle`）。
- 真机 instrumentation 5 通过（新增 `hitCooldownSkipsRequestsUntilCooldownElapses`）。
- 真机门控复验：关实时识曲录约 8 分钟，`budget.txt` 恒为 33 未增长（`docs/verification/t101-t102-device.md` §复验）。