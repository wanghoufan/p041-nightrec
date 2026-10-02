# Original 完整性校验（T098）

- 日期：2026-10-02　设备：`indq5xfi6hovay4d`
- 目的：确认 Original 分片在「采集封口 → 识曲尝试」过程中 **100% 未被改动**。

## 方法

对 Session 5（18 个 Original 分片）在设备端逐一复算 SHA-256，与封口时写入的
`*.m4a.sha256` sidecar 逐条比对。

```
adb shell run-as com.nightrec.app sha256sum files/original/5/000NN.m4a
adb shell run-as com.nightrec.app cat     files/original/5/000NN.m4a.sha256
```

## 结果

| 项 | 值 |
|---|---|
| 分片数 | 18 |
| checksum 一致 | **18 / 18** |
| 不一致 | 0 |

结论：**采集→识曲全程 Original 未被写回或改动**，与设计约束（Original 只读、Clean 只写派生）一致。

## 补充（Clean/纠错路径）

- Clean 前后 Original 不变的证据由 `androidTest/OriginalImmutabilityTest`（T083）覆盖：20 真实 AB 样本
  文件级 SHA-256 前后相同、且拒绝写回 Original。见 `docs/verification/clean-quality.md`。
- 纠错（T087 TrackCorrectionRepository）只改 marker/元数据，不触碰 audio（代码审查结论，
  `docs/verification/traceability.md` FR-039–042 映射）。

## 结论

T098 通过：Original 完整性 100%。