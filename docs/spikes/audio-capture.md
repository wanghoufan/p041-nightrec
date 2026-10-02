# Spike — 后台/锁屏音频采集（T004）

## 目标
目标机（22101316C / ruby，Android 14，arm64-v8a，USB `indq5xfi6hovay4d`）在锁屏/后台持续 `AudioRecord` + microphone 前台服务，稳定采样率/声道，AAC-LC/M4A 5 分钟安全分片与原子封口。

## 已验证事实（fresh）
- 采集格式协商结果：`AudioSource.UNPROCESSED`，**48000 Hz / stereo / PCM_16BIT**，双声道可用（单声道为回退）。
- 分片：5 分钟触发 `SEALED`，`.open` 临时文件在 `close()` 时 `fd.sync()` 后 `renameTo` 原子封口，并写 `.sha256`。
- 第一轮（dir `1790927916557`）：约 39 分钟，8 个封口 `.m4a`，全部与存储 `.sha256` 一致；首片 300.010667s / 7,260,532B，可解码。
- 第四轮（dir `1790932572665`，2026-10-02 17:16）：START→约 3.5 分钟→STOP 触发封口，`000.m4a` 5,567,041B；host 端校验 `sha256=80b6d27c09ba1a38dfe062896fa93fe123cbea97379fa492f40ec44553a73532` 与设备 `.sha256` 完全一致。
- FGS：`startForeground(91, ...)` 常驻通知 + `PARTIAL_WAKE_LOCK`；`dumpsys activity services` 在 STOP 后为 `(nothing)`，进程模型正确。

## 尚未完成 / 限制
- **完整 30 分钟锁屏/后台连续采集尚未取得带事件的证据**：第一轮缺可靠的前后台 VISIBILITY 事件日志，不能凭总时长宣称"明确 30min 锁屏验收完成"。第四轮为 3.5 分钟短验证。
- 待补（用户醒后，低音量或静音运行只验服务连续性）：一次带 START/VISIBILITY/HEARTBEAT/STOP 事件、跨锁屏 ≥30 分钟的采集，写 `docs/verification/capture-spike-30min-events.jsonl`。
- 第二轮 dir `1790930896892` 因未确认 STOP 重装留下 `.open` 尾片，已隔离到 `files/probe/quarantine/1790930896892-open-unsealed/`，不计成功。

## 结论
采集通道在真机可行，格式与分片/封口策略成立。**T004 部分闭环**：格式/分片/封口/校验已验证；30 分钟锁屏连续验收待补。