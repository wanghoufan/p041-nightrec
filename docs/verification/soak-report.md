# 长测报告（T096 锁屏连续 / T097 Soak）

- 日期：2026-10-03　设备：`indq5xfi6hovay4d`（ruby / 22101316C / Android 14，USB；约 10:33 USB 掉线后同机以无线 `192.168.31.31:5555` 在线）
- 会话：Session **3**　采集格式：48kHz / stereo / AAC-LC，5 分钟安全分片
- 采样证据：`docs/verification/device/soak-samples.jsonl`（178 行，每 60s 一行）
- 采样窗口：**08:34:54 → 11:34:50**（其中 adb 可达 116 行：08:34:54 → 10:32:43）

## 结论（诚实口径）

- **T096（30 分钟锁屏连续）通过**：上一轮 Session 5 实测 18:56–19:26 屏幕 `Asleep`、FGS 持续存活（`id=92, types=0x80` 麦克风类型）、未进入 AWAY、无崩溃/ANR（沿用旧结论）。
- **T097（≥3 小时 Soak）通过（采集连续性口径）**：Session 3 从 **08:33 连续采集到 11:43+（≥3 小时 10 分）**，心跳此前先关实时识曲；**38 个已封口分片**（`00000`–`00037`，均带 `.sha256`；`00038.m4a.open` 在写），分片节奏严格约每 5 分钟一片，**无缺口**（无 AWAY/Gap、进程未重启）；FGS 全程存活；`logcat` 全量 **0 FATAL / 0 ANR**。
- **采样缺口说明**：10:33 后 62 行采样为空值，根因是 **USB/adb 连接掉线**（取样命令取不到设备），**不是 App 故障**——设备端分片仍按 5 分钟节奏持续封口，证明采集与前台服务未中断。

## 聚合事实

| 指标 | 值 |
|---|---|
| 采样样本数 | 178（adb 可达 116：`fgs=true` 116/116；空值 62 = USB 断连期间） |
| 屏幕状态 | 有效窗口内 `Asleep` 112 / `Awake` 4（10:29 起短暂唤醒，采集未中断） |
| FGS | 全程存活：`isForeground=true foregroundId=92 types=00000080`（microphone） |
| 封口分片 | sealed 0 → **38**（约每 5 分钟 +1，38×5min ≈ 190min 吻合 08:38→11:43） |
| 打开分片 | open 1（进行中 `00038.m4a.open`） |
| FATAL EXCEPTION | **0** |
| ANR | **0** |
| 可用存储 | 有效窗口 128,481,924 kB → 128,244,208 kB（Δ≈232 MB，稳定线性下降，无异常） |

## 会话 3 文件校验（设备实测）

| 事实 | 值 |
|---|---|
| 目录 | `files/original/3/` |
| 封口分片 | 38 个 `*.m4a` + 38 个 `.m4a.sha256`（一一对应） |
| 在写分片 | `00038.m4a.open` |
| 单片大小 | 约 7,260,525 bytes（5 分钟 AAC-LC） |
| 分片节奏 | `00000`@08:38 … `00037`@11:43，严格 5 分钟步进，无跳变 |

## 已知问题（本轮发现，重要）

- **「实时识曲」开关是"死开关"（缺陷）**：`RecordingForegroundService.startRecognition()` 在被调用时（[L124](file:///Users/zzymima0000/Developer/coding/1.Active/041-ing-nightrec/app/src/main/java/com/nightrec/app/RecordingForegroundService.kt#L124) / [L139](file:///Users/zzymima0000/Developer/coding/1.Active/041-ing-nightrec/app/src/main/java/com/nightrec/app/RecordingForegroundService.kt#L139)）**不读 `recognitionEnabled`**，开始页的 `liveRecognition` 亦被忽略（[NightRecApp.kt:183](file:///Users/zzymima0000/Developer/coding/1.Active/041-ing-nightrec/app/src/main/java/com/nightrec/app/ui/NightRecApp.kt#L183)）。因此设置里关掉开关，录制时识别**仍会发请求**。
- **本轮实测**：本地预算计数 `files/recognition/budget.txt` 从 **15 → 33（+18 次请求尝试）**；`recognition_observation=0`、`track_occurrence=0`——因 AudD 服务端额度已耗尽（`code=902`），请求均被拒，**本次未真正消耗服务端额度，但本地计数被计 18 次**。
- **影响**：若持有可用 Token，这 18 次即为真实消耗；在吵闹场景下若开关无效会持续烧额度。
- **处置**：已按「修复死开关 + 重建重验」修复（`startRecognition` 尊重 `recognitionEnabled` 与开始页选择），见 `HANDOFF.md` 与 `release-candidate.md`。

## 教训

- 长测前不仅要关「实时识曲」，还须确认开关真正生效（本次暴露死开关）。
- USB 长测期间会掉线；采样须同时以**设备端分片节奏**作为连续性佐证，不能只看 adb 采样。