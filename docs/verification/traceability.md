# Requirements Traceability（T093 / Phase 10）

日期：2026-10-02　范围：`spec.md` FR-001–FR-059（59 条）
每条映射到实现 Task（`tasks.md`）与验证证据（测试文件 / 真机记录）。

| FR | 摘要 | 实现 Task | 验证证据 |
|---|---|---|---|
| FR-001 | 唯一活动 Session | T013/T033 | `RoomBaselineTest`（单 active 不变式）；`end-to-end.md` |
| FR-002 | 单一状态机 | T015 | `DomainTest`（非法迁移 RED→GREEN） |
| FR-003 | 暂离 | T046/T047 | `end-to-end.md` 步骤 2 |
| FR-004 | 回来自继续 | T048 | `end-to-end.md` 步骤 3 |
| FR-005 | 明确确认结束 | T062/T063 | `end-to-end.md` 步骤 4（确认弹窗） |
| FR-006 | 锁屏≠暂离/结束 | T096 | `soak-report.md`（锁屏期间无 AWAY） |
| FR-007 | 后台/锁屏 FGS 采集 | T034/T043/T044 | `end-to-end.md`；`soak-report.md`（FGS 心跳） |
| FR-008 | 安全分片=一条逻辑轨 | T041/T042 | `end-to-end.md`（00000→00001 单 Session） |
| FR-009 | 封口持久化元数据 | T042 | `end-to-end.md`；`.sha256` 生成 |
| FR-010 | logical+wall time | T014/T049/T050 | `SessionClockTest`；TimelineMapper 测试 |
| FR-011 | Gap 表示 AWAY/中断 | T046/T049 | `end-to-end.md`；`recovery.md` |
| FR-012 | 网络失败不停 Original | T058/T078 | `recovery.md` 断网场景；`RecognitionBackfill.kt` |
| FR-013 | 进程重启恢复 | T075/T076 | `recovery.md` 进程重启场景 |
| FR-014 | 存储风险保护 Original | T077 | `StorageMonitor`；`recovery.md` §4 |
| FR-015 | 可替换 RecognitionEngine | T051/T053 | `recognition.md`（AudD 实现） |
| FR-016 | 重复结果去重 | T055/T057 | `RecognitionTest`；`RecognitionPersistenceTest` |
| FR-017 | 稳定确认防误切 | T055/T056 | `RecognitionTest`（TrackStabilizer） |
| FR-018 | Track Marker 字段 | T057 | `recognition.md`（occurrence CONFIRMED 字段） |
| FR-019 | 补识别 | T058 | `RecognitionBackfill.kt`（幂等 range） |
| FR-020 | 未知区间+重新识别 | T059 | Player「重新识别未命中段」入口；`recognition.md` |
| FR-021 | 同歌隔开新 occurrence | T055/T056 | `RecognitionTest`（同歌重现） |
| FR-022 | 迟到结果按时间戳 | T056 | `RecognitionTest`（迟到结果排序） |
| FR-023 | 单进度条+总时长 | T065/T068 | `end-to-end.md`（总长 00:01:16 单进度条） |
| FR-024 | logical seek 映射分片 | T065/T067 | `end-to-end.md` 步骤 7（Marker seek） |
| FR-025 | 默认跳过 Gap | T065/T067 | PlaybackSourceResolver（gap 排除） |
| FR-026 | 点 Marker 播现场位置 | T067/T068 | `end-to-end.md` 步骤 7 |
| FR-027 | 结束后 Original 可播 | T063/T064 | `end-to-end.md` 步骤 5（秒开 Original） |
| FR-028 | Marker 真实时间 | T060/T068 | 时间轴行显示逻辑时间（e2e dump） |
| FR-029 | Original 不可变 | T080/T083 | `OriginalImmutabilityTest`（20 样本 SHA 不变） |
| FR-030 | Clean 独立保存+状态 | T082 | `clean/v1/` 派生；`CleanRange` 状态 |
| FR-031 | 局部检测保守处理 | T079/T081 | `clean-quality.md`（20 样本 AB） |
| FR-032 | 不整轨删主唱 | T081 | `clean-quality.md`（音乐样本 0 改动） |
| FR-033 | O/C 同位置切换 | T084 | Player AudioVariant 切换（≤250ms） |
| FR-034 | Clean 失败不影响 Original | T081/T083 | `OriginalImmutabilityTest`（失败路径） |
| FR-035 | 抢麦→INTERRUPTED | T071/T072 | `ReliabilityTest`；`recovery.md` §3 |
| FR-036 | 恢复+操作通知 | T073/T074 | `ReliabilityTest`；`recovery.md` §3 |
| FR-037 | 持续状态通知 | T044 | `end-to-end.md`（notificationId=92） |
| FR-038 | 区分 AWAY/INTERRUPTED | T046/T072 | 状态机 + `recovery.md` |
| FR-039 | 历史 Session 持久化 | T086 | HistoryScreen；`final-ui/02-history-dark.png` |
| FR-040 | Marker 重识/合并/删除 | T087/T088 | `TrackCorrectionRepository`；行动面板 |
| FR-041 | 收藏位置/区间 | T089 | `FavoriteRepository`；Player 收藏 +1（e2e） |
| FR-042 | 外部服务 deep link 解耦 | T069 | 仅 deep link，不改播放器 source |
| FR-043 | 首次使用说明 | T029 | Onboarding 隐私披露屏 |
| FR-044 | 不默认上传整晚 | T052 | 只上传 12s 窗口 WAV；`recognition.md` |
| FR-045 | secret 不入仓库/logs | T016 | `SafeLogger`；`DomainTest`（脱敏） |
| FR-046 | app-private storage | T012 | `StoragePaths`；`StoragePathsTest` |
| FR-047 | 命名 NightRec | T001 | `docs/decisions/visual-baseline.md` |
| FR-048 | Light/Dark 跟随系统 | T019/T091 | `final-ui/`（03/04/12） |
| FR-049 | 信息架构对齐原型 | T023–T092 | `final-ui/` 15 屏截图集 |
| FR-050 | 状态一眼区分 | T045/T047/T064/T092 | `final-ui/05-recording`、`06-away` |
| FR-051 | 48dp+字体缩放 | T027 | touch target / 1.3x 字号检查 |
| FR-052 | adaptive icon | T021 | launcher 多 mask 截图（T102） |
| FR-053 | monochrome themed icon | T022 | 主题图标检查（T102） |
| FR-054 | Splash 无双启动 | T023/T024 | 仅 core-splashscreen，无 SplashActivity |
| FR-055 | 主题切换保持录制/播放 | T028 | 切主题不重建 Session |
| FR-056 | AI 净化措辞 | T081 | `clean-quality.md`；文案固定“尽量降低” |
| FR-057 | portrait 主布局 | T092 | 全部真机截图 1080x2400 竖屏 |
| FR-058 | 实现细节不暴露 | T092 | UI 无分片/observation 术语 |
| FR-059 | 预算计数+上限展示 | T059/T090 | Settings「识曲状态 104/300」；耗尽停网络识曲 |

## 覆盖结论
- 59 条全部有实现 Task 与可核验证据。
- 仅两项标注为“机制验证/限制”：FR-014 的低存储真机破坏性场景（`recovery.md` §4）、FR-031/032 的真实夜店主观听感（`clean-quality.md` 已知限制）——均为产品侧有意保留的限制，非缺失实现。

## 关联
`tasks.md`；`docs/verification/{end-to-end,recovery,clean-quality,recognition,soak-report}.md`。