# 端到端验证记录（T070 / Phase 6）

日期：2026-10-02　设备：`indq5xfi6hovay4d`（ruby / 22101316C，Android 14，USB）
被测：debug APK `app/build/outputs/apk/debug/app-debug.apk`（SHA-256 见 release-candidate.md）
驱动：`scripts/nightrec/ui.py`（uiautomator dump + `input tap`，按真实控件文本定位）

## 场景（一条完整链路）

| 步骤 | 动作 | 观察到的真实结果（uiautomator / dumpsys / 文件系统） |
|---|---|---|
| 1 | Home → `开始今晚` → 确认页 → `开始今晚` | 进入 REC 屏；`dumpsys activity services` 显示 `RecordingForegroundService isForeground=true foregroundId=92 types=00000080`（microphone）；计时 `00:00:06` |
| 2 | REC → `暂离` | Away 屏“已暂离 / 录音已暂停，识别已暂停”；`files/original/2/00000.m4a` 已封口并生成 `.sha256` |
| 3 | Away → `回来了，继续记录` | 回 REC 屏；同一 Session 新建分片 `files/original/2/00001.m4a.open`；逻辑计时连续 |
| 4 | REC → `结束今晚` → 确认弹窗“已记录 00:01:00” → `结束并保存` | 返回 Home；`00001.m4a.open` → `00001.m4a` + `.sha256`（原子封口）；FGS 停止 |
| 5 | Home → 点击该 Session 行（今晚 · 10/02 18:44） | 立即进入 Player；`Original` 变体、`总长 00:01:16`（wall/logical 一致）；无需等待任何 AI 处理 |
| 6 | Player → `播放` | `dumpsys media_session`：`state=PLAYING(3), position=…` 持续增长；`active item id=1`（已跨到第 2 个 MediaItem，即第二分片） |
| 7 | 点击时间轴 Marker 行（`00:00:54` 对应行，逻辑 54s） | `dumpsys media_session`：`position=24000`，位置发生跳转 → Marker seek 生效 |

## 关键判定
- **单 Session 跨分片**：Away/Resume 只增分片（00000→00001），不新建 Session（Home 仅新增一条“今晚 · 10/02 18:44”）。
- **立即 Original 播放**：结束即 READY，Player 秒开，不依赖 Backfill/Clean。
- **跨 segment 连续播放**：播放器以有序 MediaItem 列表承载分片，`active item id` 随播放从前一分片推进到后一分片，无业务层静音。
- **Marker seek**：点击时间轴行使 `position` 跳转，映射到正确分片/offset。

## 证据
- 截图：`docs/verification/device/e2e/player-cross-segment.png`
- 控件文本实测（`ui.py dump`）逐屏已在上表复述。
- 无 `FATAL EXCEPTION` / `ANR in com.nightrec.app`（`logcat -d` 全量扫描）。

## 关联需求
FR-001/003/004/005/008/009/023/024/026/027/028/038。