# 识曲验证记录（T061 / Phase 5）

日期：2026-10-02　设备：`indq5xfi6hovay4d`（ruby/22101316C，Android 14，USB）

## 目标
验证正式采集链路内的连续识曲、稳定（TrackStabilizer）、幂等持久化与时间轴锚点，
使用真实 AudD token + 真实扬声器外放的官方测试音源（非 mock，HTTP200 不算命中）。

## 方法
1. `ProbeActivity` → “循环播放官方识曲测试音源”，经 `USAGE_ALARM` 路由到内建扬声器。
2. 正式 App：Onboarding 已完成 → Home → `开始今晚` → 确认（while-in-use 麦克风已授权）。
3. 采集前台服务（`RecordingForegroundService`，notificationId=92）启动后，
   识别消费者（`RecognitionCoordinator`）以 12s 窗口/12s 步长消费 PCM fanout。
4. 录制约 123s 后 `结束今晚` → 确认对话框 `结束并保存`。

## 结果（DB 实测，`databases/nightrec.db`）
| 事实 | 值 |
|---|---|
| Session 5 状态 | `PROCESSING` |
| 逻辑时长 | 123306 ms（wall 1790935556493 → 1790935680221） |
| 分片 | `00000.m4a`（SEALED，index 0，0 → 123306 ms）+ `.sha256` |
| 识别观测（observation） | 10 条，逻辑时间 10 → 107999 ms（12s 步长） |
| 稳定曲目（track_occurrence） | 1 条：**Warriors / Imagine Dragons**，firstTrusted=10ms，confirmed=11999ms，`CONFIRMED` |
| 歌曲表（song） | `audd` / Warriors / Imagine Dragons（upsert 去重） |
| REC 界面 | “已知歌曲 · 1 首”、当前 “Warriors / Imagine Dragons”（uiautomator dump 实测） |
| 共享预算 | 本次生产消耗 10；已按既有 94 基准对账为 104（不清零，300 上限不受突破） |

## 结论
- 窗口逻辑正确：相邻观测间隔 12s，逻辑锚点连续。
- 稳定器正确：首命中不落 occurrence，第二命中确认（≈11999ms）。
- 幂等持久化正确：同 (provider, session, 逻辑时间) 只落一行。
- 真实命中成立：扬声器外放的官方测试音源经 AudD 返回真实曲目，非模拟。

## 待补（明确未完成）
- T058 `RecognitionBackfillWorker`：断网/失败段补识别 —— 未实现。
- T059 unknown range 已落库；手动重新识别入口（UI/解码 Original）—— 未实现。
- T060 时间轴数据装载（`TrackTimeline` 纯函数已实现并单测；Repository 装载未完）。
- 30 分钟锁屏连续采集与 ≥3h Soak —— 未做。