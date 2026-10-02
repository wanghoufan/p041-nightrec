# 恢复与中断验证记录（T078 / Phase 7）

日期：2026-10-02　设备：`indq5xfi6hovay4d`（ruby / 22101316C，Android 14，USB）
驱动：`scripts/nightrec/ui.py`

覆盖 T078 要求的四类场景：进程重启、断网、抢麦、低存储。

## 1. 进程重启（PASS，真机实测）
1. 正常开始录制（Session 3，`files/original/3/00000.m4a.open` 存在）。
2. `adb shell am force-stop com.nightrec.app` 模拟进程被杀（未封口尾片 `00000.m4a.open` 保留）。
3. 重新 `am start`：
   - Home 顶部出现恢复卡片：**“未正常结束 / 今晚还没结束 / 今晚 · 10/02 18:49 · RECORDING / 已采集逻辑时长 00:00:00 / 继续今晚 / 结束并保存”**，并提示“继续会沿用同一次 Night Session，不会新建。”
   - `SegmentRecoveryPlanner.plan()` 把 OPEN/缺失分片标记 `DAMAGED`，不参与播放；进入 Player 显示 `总长 00:00:00`，**未把未封口尾片当可播放内容**。
4. 点 `结束并保存` → 该 Session 收尾为 READY，未新建 Session。
- 判定：进程重启能发现未正常结束 Session、隔离尾部损坏、保留已封口数据。**FR-013 达成。**

## 2. 断网（PASS，真机实测）
1. 开始录制 → `svc wifi disable` + `svc data disable`。
2. 持续 25s：REC 屏计时正常推进（`00:00:30`），**无崩溃**；识别分支在网络不可达时静默失败，不影响 Original 采集。
3. 恢复 `svc wifi enable` / `svc data enable`，会话继续；随后正常结束。
- 判定：网络/识曲失败不停止 Original。**FR-012 达成。**（补识别 `RecognitionBackfillWorker` 逻辑见 `RecognitionBackfill.kt`，对失败 range 幂等重试。）

## 3. 抢麦（PASS — Android 14 语义下未中断，且无崩溃）
1. 录制中打开系统录音机（`am start -a android.provider.MediaStore.RECORD_SOUND` → 同意 → 录音）。
2. 观察：NightRec 的 FGS（type=microphone，启动于可见期）在 Android 14 while-in-use 语义下**保住了麦克风并持续采集**；录音机未取得独占。
3. 返回 NightRec：仍为 REC（`00:01:43`），无 `CaptureFailure`、无崩溃；随后正常结束。
- 判定：本设备/MIUI 未触发强制抢麦；`CaptureFailure.MIC_BUSY / PERMISSION_LOST → INTERRUPTED → 短退避 RECOVERING → 回 RECORDING / 超限通知恢复` 的协调器路径由 `ReliabilityTest` 单测覆盖（注入式 AudioRecord 失败）。**FR-035/036 机制达成。**

## 4. 低存储（机制验证；设备未做破坏性实测）
- 不在设备上人为制造低存储（避免破坏用户数据）。
- `StorageMonitor`（T077）逻辑：warning 阈值停 Clean/Backfill、hard 阈值安全停止新 capture，只保护 Original。由单元测试覆盖阈值分支。
- 判定：机制达成 **FR-014**；真机破坏性低存储场景**未执行**（记录为已知限制）。

## 证据与结果汇总
| 场景 | 结果 | 证据 |
|---|---|---|
| 进程重启 | PASS | 恢复卡片文本 dump；DAMAGED 分片不进播放 |
| 断网 | PASS | REC 计时持续；无 FATAL/ANR |
| 抢麦 | PASS（未中断，无崩溃） | 录音机占用后 NightRec 仍 REC；协调器单测 |
| 低存储 | 机制 PASS / 真机未测 | `StorageMonitor` 单测；记录为限制 |

无 `FATAL EXCEPTION` / `ANR in com.nightrec.app`（`logcat -d` 全量扫描）。

## 关联需求
FR-012/013/014/035/036/037/038。