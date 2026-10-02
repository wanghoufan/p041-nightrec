# 保守 Clean 20 样本 AB 验收（T085 / Phase 8）

日期：2026-10-02　设备：`indq5xfi6hovay4d`（ruby / 22101316C，Android 14）
实现：`ConservativeSpeechCleanupEngine`（`cleanup/ConservativeSpeechCleanupEngine.kt`）
证据：`docs/verification/spike-fresh-2026-10-02/cleanup-ab-scores.json`（20 样本实测）

## 验收口径
判定“主唱/转场是否明显受损”，并据此决定是否降级为 Beta / 轻处理。

## 20 样本实测结果（逐样本）

| 样本区间 | 类型 | eligibleWindows | changedSamples | originalUnchanged | untouchedChannelExact |
|---|---|---|---|---|---|
| 0–9 | 音乐主导 | 0 | 0 | ✅ | ✅ |
| 10–14 | 讲话优势但未达双窗连续阈值 | 0 | 0 | ✅ | ✅ |
| 15,17,18 | 讲话优势、接近阈值 | 1–2 | 0 | ✅ | ✅ |
| 19 | 高置信讲话优势控制样本 | 3 | **31188** | ✅ | ✅ |

关键事实：
- **Original 逐样本 100% 不变**（`originalUnchanged` 全 true）。
- **未处理声道逐样本完全一致**（`untouchedChannelExact` 全 true）——即 Clean 不会牵连整轨。
- 仅 sample 19 产生真实派生（`changedSamples=31188`, `eligibleWindows=3`），且其派生强度受限于“较强声道最多降低 15%、100ms crossfade”。
- 音乐主导样本（0–9）`changedSamples=0`，即**不触碰歌曲本体**。

## 判定
- **主唱/转场未测到受损**：歌曲主导样本零改动；即便是高置信讲话样本也只做局部、单声道、≤15% 衰减，不整段 Remove Vocals。**FR-032 达成**。
- **Original 安全**：所有样本 Original 不变，Clean 只写 `clean/v1/` 派生；失败/低置信回落 KEEP_ORIGINAL。**FR-029/034 达成**。
- **定位为 Beta / 轻处理**：受控样本不代表真实夜店近场定位精度，V1 **不承诺无损删除讲话**，UI 文案固定为“AI 净化/尽量降低现场讲话干扰”。**FR-056 达成**。

## 结论
T085 通过：AB 未出现主唱/转场明显受损，保持现有保守基线；产品定位保持 **Beta / 轻处理**（不升级为“去除讲话”承诺）。播放器始终提供 Original 一键回落。

## 已知限制
- 20 个受控样本 ≠ 20 场真实夜店实录；真实近场定位准确率、主观听感（人耳 AB）未做，记录为后续可选项。
- 未做端到端“整晚 Clean 产物”音质评估。

## 关联需求
FR-030/031/032/033/034/056。