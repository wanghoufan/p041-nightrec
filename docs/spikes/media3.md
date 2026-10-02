# Spike — 统一连续回放（T006）

## 目标
多个 M4A segment 在业务层无静音连续播放；20 个随机 seek ≤250ms；跨片段边界自动续播。

## 实现
`ExoPlayer` + `setMediaItems(List<MediaItem>)`，同一 `ConcatenatingMediaSource` 语义；对真实封口采集片段直接播放（不重编码）。

## 已验证事实（fresh，真机，2026-10-02）
- 输入：第一轮 dir `1790927916557` 的真实封口 `.m4a`（≥2 片，48kHz stereo AAC）。
- 20 个跨片段随机 seek：**全部 passed**，`readyMs` 60–177ms（≤250ms 目标留有余量）。
- 跨片段边界的 `MEDIA_ITEM_TRANSITION_REASON_AUTO` 自动续播：通过。
- 播放前后逐片 `sha256` 不变（播放不改写 Original）。
- 证据：`docs/verification/spike-fresh-2026-10-02/media3-spike.json`。

## 尚未完成 / 限制
- 长时（整晚多小时）连续播放的内存/缓冲行为未做长测。
- 全局 logical timeline 的 Gap 跳过、Marker seek 需要 `TimelineMapper`（Domain.kt 已有）在播放器层接线（Phase 6 实现）。

## 结论
Media3 连续回放 + 快速 seek 在真机可行且达标。**T006 闭环。**