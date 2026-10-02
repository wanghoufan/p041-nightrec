# Feature Specification: NightRec — 整晚 DJ Set 连续留存

**Feature Branch**: `001-night-session`  
**Created**: 2026-10-02  
**Status**: Ready for Planning  
**Product Name**: NightRec  
**Core Object**: Night Session（今晚记录）

## Product Intent

用户在酒吧/Club 等连续音乐场景中点击一次“开始今晚”，随后正常锁屏、使用其他 App。NightRec 持续保存现场 DJ Set，并自动识别歌曲。最终不是独立歌曲列表，而是**一条可无缝拖动的整晚现场音轨 + 歌曲/转场时间 Marker**。用户主动暂离时，同一 Night Session 保留一个真实时间 Gap；附近客人讲话尽量降低，但歌曲主唱与 DJ 原始转场优先保留。

## User Scenarios & Testing

### User Story 1 - 首次进入并一键开始今晚（P1）

首次打开时，用户看见 NightRec 品牌、持续录音用途、隐私说明和最少必要权限；完成后进入首页，一键“开始今晚”。

**Acceptance**
1. 首次启动展示品牌启动体验后进入权限/隐私引导，不重复出现双 Splash。
2. 用户未授予麦克风权限时不能开始录音，并给出明确修复入口。
3. 首页核心动作只有一个高优先级“开始今晚”；最近 Session 可直接进入。
4. 开始前名称/地点/AI 净化等属于可选设置，不应强迫填写。

### User Story 2 - 锁屏/用手机时仍连续记录（P1）

用户开始后把手机放桌上，也可能回微信、地图、拍照或锁屏；只要没有主动“暂离”，Session 应继续。

**Acceptance**
1. 进入 `RECORDING` 后显示计时、当前稳定歌曲、已识别数量和显著 REC 状态。
2. 锁屏/切其他普通 App 不进入 `AWAY`。
3. 后台存在系统可见的持续录音通知。
4. 识曲断网不停止 Original。
5. 底层安全分片无用户感知，用户始终看到一条 Session。

### User Story 3 - 暂离后继续同一个晚上（P1）

用户出去抽烟、转场或离开半小时，点击“暂离”；回来点击“回来了，继续记录”。

**Acceptance**
1. `RECORDING -> AWAY` 时安全封口当前分片并停止麦克风与实时识曲。
2. `AWAY -> RECORDING` 时 Session ID 不变。
3. 时间轴显示真实暂离开始/结束/时长。
4. 回放默认跳过 Gap，不播放半小时静音。
5. 普通切 App 不得生成 Gap。

### User Story 4 - 连续识曲与稳定歌曲时间轴（P1）

系统持续识别，不要求用户逐首操作；重复回调去重，DJ A+B 混合时避免一次误识别就乱跳。

**Acceptance**
1. 同一稳定区间重复识别只形成一个主要 TrackOccurrence。
2. 新歌需满足稳定确认规则才切换。
3. 迟到结果按音频时间戳落到正确位置。
4. 网络恢复/Session 结束后可补识别。
5. 未识别区间保留并可手动“重新识别这一段”。

### User Story 5 - 结束今晚并无缝回听现场（P1）

真正离开时点击“结束今晚”并确认。Original 立即可播放；AI 后处理可以继续。

**Acceptance**
1. 只有用户确认“结束今晚”才结束 Session。
2. 多物理分片在 UI 中只有一条整晚进度条。
3. 逻辑 seek 能准确映射到现场音频。
4. 点击歌曲 Marker 跳到昨晚现场，而不是外部平台原曲。
5. Gap 默认跳过，Gap 前后无业务层插入静音。
6. Original 在 PROCESSING 阶段也能播放。

### User Story 6 - AI 净化附近讲话但保留歌曲主唱（P2）

用户可切换“现场原版 / AI 净化”。AI 只尽量降低附近讲话干扰，不承诺完全删除。

**Acceptance**
1. Original 永不修改。
2. Clean 独立生成，失败自动回退 Original。
3. 低置信区间不强行处理。
4. Original/Clean 切换保持同一逻辑位置。
5. UI 能看到 Clean 状态与处理区间。

### User Story 7 - 电话/抢麦/崩溃/低存储仍尽量保住整晚（P1）

**Acceptance**
1. 麦克风失去进入 `INTERRUPTED`，不误结束。
2. 可自动恢复时经 `RECOVERING` 回到 `RECORDING` 并记录 Gap。
3. 不能自动恢复时通知用户恢复同一 Session。
4. 进程重启后恢复已封口 Original 与 Session 元数据。
5. 存储风险优先暂停 Clean/Backfill，必要时安全停止新采集并保留已有 Original。

### User Story 8 - 第二天查找、纠错、收藏与学习（P3）

**Acceptance**
1. 历史按日期显示 Session、时长、歌曲数、处理状态。
2. 错误 Marker 可重新识别、合并上一首、删除。
3. 可收藏歌曲出现位置和转场区间并跳回现场。
4. 歌曲详情可提供外部音乐服务入口，但不得改变 NightRec 播放源。

### User Story 9 - 一致的浅色/深色品牌体验（P1）

用户看到的 App 应与 V2.0 原型保持一致：NightRec 品牌、紫/粉/橙波形视觉、浅色与深色模式、整晚播放器和时间轴层级固定。

**Acceptance**
1. 产品名始终为 NightRec；不出现 NightSet。
2. Light/Dark 只改变主题，不改变信息架构和交互位置。
3. Android launcher icon 与启动画面使用包内规范资产。
4. Recording/Away/Interrupted/Processing/Ready 各状态有明显且不混淆的视觉表达。
5. 原型中的 15 个核心页面/状态都能映射到真实实现页面或状态。

## Edge Cases

- 开始后立刻锁屏。
- 用户拒绝通知权限但 microphone FGS 仍需满足平台要求。
- 录制中来电、微信语音或其他 App 抢麦。
- 用户忘记暂离，录到街道/出租车；系统只能提出“疑似离场”建议。
- 同一首歌 20 分钟后又出现。
- DJ 只播放 20–30 秒片段。
- 识曲服务在 A+B 转场阶段交替返回 A/B。
- AudD Token 无效/额度耗尽/网络超时。
- 某物理分片尾部损坏。
- Clean 模型误把歌曲主唱当讲话。
- 设备只支持 mono 或非 48kHz 录音组合。
- 空间不足、低电、进程被系统杀死。
- Light/Dark 切换发生在正在录音或播放时。

## Functional Requirements

### Session & State
- **FR-001**：MUST 通过“开始今晚”创建且仅创建一个活动 Night Session。
- **FR-002**：MUST 使用 `IDLE/STARTING/RECORDING/AWAY/INTERRUPTED/RECOVERING/STOPPING/PROCESSING/READY` 单一状态机。
- **FR-003**：MUST 提供“暂离”，停止采集/实时识曲但不结束 Session。
- **FR-004**：MUST 提供“回来了，继续记录”，恢复同一 Session。
- **FR-005**：MUST 仅在明确确认“结束今晚”后停止。
- **FR-006**：锁屏/普通切 App MUST NOT 等同暂离或结束。

### Capture & Reliability
- **FR-007**：MUST 在 Android 支持的后台/锁屏场景用可见前台服务持续麦克风采集。
- **FR-008**：MUST 使用可恢复安全分片，并在用户层表现为一条逻辑音轨。
- **FR-009**：MUST 在分片封口后持久化恢复元数据。
- **FR-010**：MUST 同时维护 logical time 与 wall time。
- **FR-011**：MUST 用 Gap 表示 AWAY/INTERRUPTED，不伪造静音音频。
- **FR-012**：网络/识曲失败 MUST NOT 停止 Original。
- **FR-013**：进程重启 MUST 能发现未正常结束 Session 并恢复已落盘数据。
- **FR-014**：存储风险 MUST 优先保护 Original。

### Recognition & Timeline
- **FR-015**：MUST 通过可替换 RecognitionEngine 持续识曲并接收音频时间戳结果。
- **FR-016**：MUST 对重复结果去重。
- **FR-017**：MUST 使用稳定确认规则防止瞬时误切。
- **FR-018**：MUST 创建 Track Marker，记录歌曲身份、首次可信时间、确认时间、来源、状态。
- **FR-019**：MUST 支持补识别。
- **FR-020**：MUST 显示未知区间并支持重新识别。
- **FR-021**：同歌隔开后再次出现 MUST 可形成新 occurrence。
- **FR-022**：迟到结果 MUST 按音频时间戳排序。

### Playback
- **FR-023**：MUST 为 Night Session 提供单一逻辑进度条和总逻辑时长。
- **FR-024**：MUST 将 logical seek 映射到正确分片/offset。
- **FR-025**：MUST 默认跳过 Gap。
- **FR-026**：点击 Marker MUST 播放现场对应位置。
- **FR-027**：结束后即使 AI 未完成，Original MUST 可播放。
- **FR-028**：MUST 显示 Marker 对应真实时间。

### Original / Clean
- **FR-029**：Original MUST 不可变。
- **FR-030**：Clean MUST 独立保存并有状态。
- **FR-031**：MUST 局部检测附近讲话，并只对高置信区间保守处理。
- **FR-032**：MUST 避免把歌曲主唱按 vocals 全量删除。
- **FR-033**：Original/Clean 切换 MUST 保持同一 logical position。
- **FR-034**：Clean 失败 MUST 不影响 Original。

### Interruption & Notification
- **FR-035**：抢麦/通话/系统异常 MUST 进入 `INTERRUPTED`。
- **FR-036**：MUST 尝试恢复；失败时提供恢复同一 Session 的操作通知。
- **FR-037**：活动 Session MUST 有持续状态通知。
- **FR-038**：MUST 区分 AWAY 与 INTERRUPTED。

### History & Learning
- **FR-039**：MUST 持久化历史 Session。
- **FR-040**：MUST 支持 Marker 重新识别/合并/删除。
- **FR-041**：MUST 支持收藏歌曲出现位置或转场区间。
- **FR-042**：MAY 提供外部音乐服务入口，但 MUST 与现场播放器解耦。

### Privacy & Security
- **FR-043**：首次使用 MUST 说明持续录音、本地保存、向 AudD 上传短音频片段（可能含现场讲话）、识曲计费/限额与合法使用责任；关闭识曲或未同意则不上传。
- **FR-044**：MUST 不默认上传整晚 Original。
- **FR-045**：MUST 不把 secret/token/private key 写入仓库或 release logs。
- **FR-046**：MUST 将 Original 与数据库存放在 app-private storage，除非用户主动导出。

### Visual & Interaction
- **FR-047**：产品名 MUST 固定 NightRec；业务对象固定 Night Session。
- **FR-048**：MUST 支持 Light/Dark，两者页面层级一致并默认跟随系统。
- **FR-049**：首页、开始设置、Recording、Away、结束确认、Processing、Session 完成、播放器、时间轴、转场详情、歌曲详情、历史、设置 MUST 与 V2.0 原型的信息架构对应。
- **FR-050**：Recording/Away/Interrupted/Processing 状态 MUST 一眼可区分。
- **FR-051**：主要触控目标 MUST 至少 48dp；关键文本支持系统字体缩放且不遮挡核心操作。
- **FR-052**：MUST 使用包内 NightRec Android icon 作为视觉真源并实现 adaptive icon。
- **FR-053**：Android 13+ MUST 提供 monochrome/themed icon。
- **FR-054**：MUST 使用包内启动画面作为视觉参考，并避免系统 Splash + 自定义 SplashActivity 双启动。
- **FR-055**：MUST 在主题切换时保持正在录制/播放状态，不重建 Session。
- **FR-056**：MUST 使用“AI 净化/尽量降低现场讲话干扰”而非绝对去除承诺。
- **FR-057**：V1 手机端 MUST 以 portrait 为主要布局；横屏不作为核心验收目标。
- **FR-058**：底层分片、识曲原始 observation 等实现细节 MUST 不暴露给普通用户。

- **FR-059**：MUST 显示本地识曲请求计数和请求上限；达到上限暂停实时/补识别/手动请求，继续 Original，不自动购买额度；网络重试也计数。

## Key Entities

- `NightSession`
- `AudioSegment`
- `SessionGap`
- `RecognitionObservation`
- `Song`
- `TrackOccurrence`
- `UnrecognizedRange`
- `ProcessingJob`
- `CleanRange`
- `FavoriteRange`
- `UserSettings`

## Success Criteria

- **SC-001**：用户从首页到开始录音不超过 2 个必要操作（已授权场景）。
- **SC-002**：锁屏/切其他普通 App 30 分钟，不因 App 退后台产生逻辑断裂。
- **SC-003**：3 小时以上设备 Soak 中，已封口分片 100% 可恢复；不得因尾片异常丢失整个 Session。
- **SC-004**：AWAY Gap 不计入 logical duration，wall time 保留。
- **SC-005**：随机 logical seek 映射误差目标 ≤250ms。
- **SC-006**：跨分片播放不得由业务层人为插入静音。
- **SC-007**：重复/迟到识曲回调不产生重复主 Marker。
- **SC-008**：20 分钟断网期间 Original 继续；恢复后可补识别。
- **SC-009**：Original checksum 在识曲、Clean、纠错前后保持不变。
- **SC-010**：Original/Clean 同位置切换偏差目标 ≤250ms。
- **SC-011**：关键流程在 Light/Dark 下均完成且信息架构一致。
- **SC-012**：NightRec、Night Session 命名在 UI/代码文档中无产品级漂移。
- **SC-013**：Human Gate 前，已连接手机可成功安装并启动当前 debug APK。
- **SC-014**：Human Gate 前，生成本地 `app-debug.apk`，fresh build exit code 0。
- **SC-015**：最终 Spec Kit consistency/convergence 检查无 CRITICAL/HIGH 未解决 gap。

## Assumptions

- V1 Android-only，minSdk 29。
- 用户主要在个人学习/回顾场景使用；公开分发不在 V1。
- 用户于 2026-10-02 批准实时识曲改为 AudD；开发只需要 AudD API Token，不需要 Apple 会员、AAR 或私钥。
- V1 通过周期性短片段实现自动识曲；间隔可能漏过短歌，需 Spike 验证并诚实保留未知区间，不宣称逐采样连续匹配。
- V1 不需要云数据库/API key；Room 是本地真源。
- “现场讲话净化”是保守增强，不能保证完全去除。
- 酒吧地点手填/可选；V1 不依赖精确定位。
