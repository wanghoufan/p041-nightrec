# Spike — 连续识曲（T005）

## 目标
麦克风侧连续识曲（供应商无关 contract；本项目用户批准改用 **AudD**）：重复结果、timestamp、断网与 gap 后重建、共享持久化预算。

## 关键阻塞与修复
- Round 1（dir `1790927916557`）：78 次 AudD 请求，全部 `http=200 status=success`，但**零真实歌曲命中**——因为测试音乐经 `AUDIO_STREAM_MUSIC` 被路由到 `remote_submix(8000)`（虚拟设备），根本没走扬声器，麦克风收不到。
  - 教训：`HTTP 200 / status success` **不等于命中**；必须要求结果含真实 `title/artist`。
- 修复：仅 debug 测试音源改用 `USAGE_ALARM`（设备上 `AUDIO_STREAM_ALARM → speaker(2)`），并 `setPreferredDevice(TYPE_BUILTIN_SPEAKER)`、`setVolume(0.4)`。

## 已验证事实（fresh，2026-10-02 17:16–17:20）
- 第四轮 dir `1790932572665`：音乐经扬声器播放，麦克风采集，**7/7 次识别全部真实命中 `Warriors / Imagine Dragons`**（`docs/verification/spike-fresh-2026-10-02/routing-fix-events.jsonl`）。
- 窗口：每 30 秒一个 mono 16-bit WAV 窗口上传；`logicalMs` 递增（18005 / 48000 / 78016 …），时间戳与采集帧一致。
- 真实凭据：debug-only `BuildConfig.AUDD_TOKEN`（release 为空），未落日志/仓库。

## 共享预算（严格）
- 设备 `shared_prefs/audd-budget.xml` `attempted=93`；host 侧请求 2；初始 auth 重叠 1 → **合计 94 / 300，剩 206**（`docs/verification/audd-request-budget.json`）。
- 先计数再请求；耗尽只停网络识曲，Original 采集继续；禁止自动付费/无限重试。
- 待办：把 device/host 两个计数迁移合并为正式持久化总预算（`RequestBudget` 已实现进程内共享锁 + 文件锁），不得清零。

## 尚未完成 / 限制
- 断网/gap 后 session 重建路径、迟到结果、短歌漏识别的连续 30 分钟现场验证待补（用户醒后）。
- 真实夜店环境命中率未验证；受控样本只能证明管线可用。

## 结论
麦克风→扬声器→空气→AudD 真实命中链路打通（7 连击）。**T005 核心闭环**：真实识曲命中已验证；预算守恒；30 分钟连续与断网重建待补。