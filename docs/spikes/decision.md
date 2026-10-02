# Spike 汇总与决策（T008）

三条主干可行性结论（真机 fresh 证据，2026-10-02）：

| 主干 | 结论 | 关键证据 | 待补 |
|---|---|---|---|
| Capture | **可行** | 48kHz stereo、5min 分片、STOP 封口、checksum 一致（`audio-capture.md`） | 30min 锁屏连续（带事件） |
| Recognition | **可行（真实命中）** | 麦克风 7/7 命中 Warriors/Imagine Dragons；预算 94/300（`audd-recognition.md`） | 30min 连续、断网/gap 重建 |
| Playback | **可行且达标** | 20 随机 seek 全过，readyMs 60–177ms；跨片段自动续播（`media3.md`） | 整晚长时缓冲 |
| Cleanup | **保守可行** | Original/未处理声道逐样本不变；高优势样本真实派生（`cleanup.md`） | 现场质量、主观 AB |

## 决策
- 供应商由 ShazamKit 改为 **AudD**（用户已批准，非待决策项）；Constitution/Spec/Plan/Tasks 已同步。不再索取 Apple 会员 / AAR / `.p8` / DB key。
- 采样格式固定 48000Hz/stereo/UNPROCESSED，AAC-LC 192kbps，5min 安全分片 + 原子封口。
- 测试音源路由 workaround（USAGE_ALARM + speaker + volume 0.4）**仅 debug 测试用**；正式播放器继续用 `USAGE_MEDIA`，不被污染。
- Clean 维持保守基线；现场质量不足只允许 Beta/轻处理/KEEP_ORIGINAL，**不承诺无损删除讲话**。
- **无需回写 SDD**：三条主干均可行，无 CRITICAL 不可行项。

## 阻塞与教训
- Round 1 的 `HTTP 200 ≠ 命中`：识曲验收必须要求真实 `title`。
- Round 2 未确认 STOP 即重装留下 `.open` 尾片：已隔离，后续一律"新目录 + START/STOP 事件"双确认后才允许重装（`probe-control.py` 已加固）。
- 有线耳机不可用于识曲验证：音频入耳机后麦克风物理收不到，等同 remote_submix 问题。

## 结论
T004/T005 核心闭环（30min 现场连续待用户醒后补）；**T006/T007/T008 闭环**。可以进入 Phase 1 Foundation。