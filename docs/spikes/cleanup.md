# Spike — 保守讲话轻处理（T007）

## 目标
20 个"歌曲主唱 + 近距离讲话"受控样本，确定保守 Clean 基线；禁止整晚 Remove Vocals。

## 实现（候选基线）
`ConservativeCleanup`（Cleanup.kt）：只读 Original window，输出派生；需同侧两个连续高置信窗口；阈值 `Speech≥0.98`、唱歌/说唱/哼唱 `≤0.05`、`Music<0.4`、声道功率比 `≥4`；只把较强声道最多降低 **15%**，100ms crossfade，**另一声道逐样本不变**。**不是语音分离，不承诺讲话消失。**

## 已验证事实（fresh，真机，2026-10-02）
- 真机 YAMNet（`assets/yamnet.tflite`，SHA `e193da56…1667d`，官方模型）单次推理 **latencyMs=8**，输入 15600 float32，输出 1×521，全 finite。
- 20 样本 AB（`app/src/androidTest` 实际调用 `ConservativeCleanup.apply`）：
  - `originalUnchanged` 全 true（Original 逐样本不变）。
  - `untouchedChannelExact` 全 true（未处理声道逐样本不变）。
  - 处理强度边界：`|clean| ≥ |original|*0.849`。
  - 高讲话优势控制样本 sample 19：`changedSamples=31188`，`eligibleWindows=3`，`maxSpeech=0.991`——**确有真实派生输出**。
  - 音乐主导样本（0–9）`changedSamples=0`（正确保持原样）。
- 证据：`docs/verification/spike-fresh-2026-10-02/cleanup-ab-scores.json`；样本来源见 `docs/spikes/cleanup-fixtures.md`。

## 尚未完成 / 限制
- 受控样本不代表 20 个不同夜店实录；无法证明任意现场近场定位准确率。
- 真实场所主唱保护质量未验证 → 质量不足时只允许 **Beta / 轻处理 / KEEP_ORIGINAL**，不得承诺无损删讲话。
- 20 样本 AB 主观听感验收（T085）待做。

## 结论
保守轻处理内核在真机成立且严格保护 Original 与未处理声道。**T007 核心闭环**（机制与安全边界已验证；现场质量与主观 AB 属后续 Task）。