# NightRec Constitution

<!--
Sync Impact Report
- Version: 2.1.0
- 2026-10-02 用户批准：识曲供应商切换 AudD，允许最小短片段上传，保护本地 Original。
- Ratified: 2026-10-02
- Last Amended: 2026-10-02
- Product name: NightRec
- Core domain object: Night Session
- Governs: specs/001-night-session/spec.md, plan.md, tasks.md and packaged design assets
- Spec Kit baseline checked against github/spec-kit main on 2026-10-02
-->

## Core Principles

### I. 连续现场体验优先（Continuous Set First）

NightRec 的核心不是“识别出若干首歌”，而是保留用户当晚真正听到的连续 DJ Set。

1. 一个 Night Session MUST 在用户层表现为一条连续、可拖动、可跳转的统一逻辑时间轴。
2. 歌曲识别结果 MUST 作为 Marker/章节存在，不得用 Apple Music、Spotify、YouTube Music 等原曲替换现场录音。
3. DJ 的叠歌、混音、转场、现场 Remix/Mashup、速度变化 MUST 尽可能保留在 Original 中。
4. 底层 MAY 使用多个安全音频分片，但用户层 MUST 不暴露物理分片。
5. `AWAY` / `INTERRUPTED` 产生 Gap；默认回放跳过 Gap，同时保留真实墙钟时间。

**Gate**：任何把整晚体验降级成“若干首独立歌曲播放列表”的实现均为 P0 违例。

### II. Original 是不可变真源（Original Is Immutable）

1. 已安全落盘的 Original 分片 MUST 不可变；任何 AI/降噪/净化/增益处理不得覆盖。
2. Clean、识曲、Marker、转场分析、收藏、修订 MUST 作为派生数据存在，可删除、重算、回滚。
3. Original/Clean 切换 MUST 保持同一逻辑播放位置。
4. V1 禁止默认自动删除 Original。
5. 存储不足时 MUST 优先保护已完成 Original，再停止/延迟非关键派生任务。

### III. 录音优先，智能能力旁路（Capture Before Intelligence）

固定优先级：

1. P0：Original 采集、安全分片、Session 状态；
2. P1：逻辑时间轴与 Gap；
3. P2：实时/补识曲；
4. P3：歌曲 Metadata；
5. P4：讲话净化；
6. P5：转场/BPM/Key 等高级分析。

断网、识曲 API 错误、Clean 失败 MUST NOT 中断 Original。异步派生任务 MUST 幂等。

### IV. Night Session 状态语义固定且唯一

单一状态机真源：

- `IDLE`
- `STARTING`
- `RECORDING`
- `AWAY`
- `INTERRUPTED`
- `RECOVERING`
- `STOPPING`
- `PROCESSING`
- `READY`

约束：

1. 锁屏、回微信/地图等普通切 App MUST NOT 自动进入 `AWAY`。
2. 用户主动点击“暂离”才进入 `AWAY`；AI 只能建议疑似离场，不能自动改写状态。
3. 电话/其他 App 抢麦/系统错误进入 `INTERRUPTED`，不是“结束今晚”。
4. 只有用户明确确认“结束今晚”才能进入 `STOPPING`。
5. V1 不提供含义模糊的通用 Pause；用户可见名称固定为“暂离”。

### V. 讲话净化必须保守、透明、可回退

“去人声”仅指尽量降低附近客人聊天、喊话、服务员说话等现场新增语音干扰；歌曲主唱不是删除目标。

1. MUST 永远保留 Original。
2. MUST 局部检测、局部处理；禁止默认整晚执行普通 Remove Vocals。
3. 低置信度 MUST 选择不处理或轻处理，不能牺牲歌曲主唱与 DJ 转场。
4. UI MUST 使用“AI 净化 / 尽量降低现场讲话干扰”等保守表述，禁止“100% 无损去人声”。
5. Clean 质量不足时 MUST 自动回退 Original；Clean 可标注 Beta。

### VI. 视觉资产与交互流程是实现真源（Visual SSOT）

1. 产品名固定为 **NightRec**；核心业务对象固定称 **Night Session / 今晚记录**。禁止再出现 `NightSet` 作为产品名。
2. `design/` 中 V2.0 深色原型、浅色原型、Android 图标、启动画面是本版本 UI 实现的规范性输入，不是“仅供参考”。
3. 页面层级、主按钮位置、状态文案、颜色关系、整晚播放器与歌曲时间轴结构 MUST 以原型图为准；若原型与 Constitution/Spec 冲突，以 Constitution/Spec 为准并记录偏差。
4. Light/Dark MUST 共用同一信息架构与组件状态；默认跟随系统主题，用户 MAY 在设置中覆盖。
5. Android 启动画面 MUST 使用平台 SplashScreen 规范实现，不得用额外 SplashActivity 造成双启动页。
6. Android Launcher Icon MUST 提供 adaptive icon；Android 13+ MUST 提供 monochrome/themed icon 资源。

### VII. Android 平台与凭据安全优先于实现便利

1. microphone foreground service MUST 从用户可见 Activity 明确启动，并保持系统可见状态。
2. 生产/发布版本禁止把 Apple `.p8` 私钥、第三方 secret、长期私钥硬编码进 APK 或仓库。
3. AudD V1 开发使用用户提供的 API Token；token 仅进入本地 debug 配置，仓库与 release logs 不得包含；不需要 Apple 会员或 ShazamKit AAR。
4. V1 不需要云数据库；Session/Marker/设置均使用本地 Room 与 app-private files。
5. 未来生产化的识曲凭据保护 MUST 新开后端代理规范；当前仅个人 debug 验证，不把长期服务凭据硬编码进发布 APK。
6. 音频默认仅保存在设备本地 app-private storage；不得默认上传整晚 Original。
7. 首次使用 MUST 明确持续录音、向 AudD 上传最小短音频片段（可能含现场讲话）、版权与所在地录音/隐私责任；未同意或关闭实时识曲时不得上传。整晚 Original 不默认上传。
8. 识曲 MUST 有本地请求计数、可见限额和硬停止；免费额度耗尽不阻断 Original，不自动购买或启用无限重试。

### VIII. 证据后置声明，连续执行到 Human Gate

1. Functional Requirement MUST 可追溯到 Plan 与至少一个 Task。
2. P0/P1 链路 MUST 有自动化测试 + ADB 真机验证证据。
3. 开发期间使用已连接 Android 手机做阶段性安装预览；视觉/交互相关 Task 完成后 MUST 真机截图核对。
4. Human Gate 之前 MUST 生成可安装 debug APK 预构建件，并在已连接设备完成安装/启动 smoke test。
5. 单开发者执行模式：完成凭据预检后，MUST 连续推进全部 tasks，中途不等待“继续”、不逐 Task 询问用户；不确定项采用 Constitution/Spec/原型优先级自行决策并记录。
6. 当前 GitHub Spec Kit 流程 MUST 包含：task 生成后的 `/speckit.analyze` 等价检查；实现后的 `/speckit.converge`；若 converge 追加 Task，则继续实现直至 converged。
7. 完成声明 MUST 基于当次 fresh verification 输出；不得仅因“代码看起来完成”进入 Human Gate。

## Product Boundaries & Quality Gates

### V1 必须包含

- Android 原生 Kotlin + Compose；
- 浅色/深色模式；
- NightRec adaptive/themed icon；
- Android 官方 SplashScreen；
- 首次隐私/权限引导；
- 一键“开始今晚”；
- 锁屏/后台持续录音；
- 安全分片与异常恢复；
- `AWAY` 暂离/继续同一 Session；
- `INTERRUPTED` 抢麦/通话恢复；
- 连续识曲、稳定去重、补识别；
- 歌曲 Marker + 真实时间；
- 一条整晚逻辑播放器；
- Gap 默认跳过；
- Original/Clean 双版本；
- 保守现场讲话净化；
- 历史、纠错、收藏转场/歌曲出现位置；
- ADB 真机预览流程；
- Human Gate 前本地 debug APK 预构建。

### V1 明确不做

- iOS / PWA 主版本；
- 公共社交/公开分发整晚录音；
- 云端整晚 Original 备份；
- Spotify/Apple Music 原曲拼接替代现场；
- 专业 DAW 波形编辑器；
- 承诺 100% 去除现场讲话；
- 在 APK 中嵌入 Apple 私钥；
- 依赖 Android 17 Beta 行为作为 V1 发布基线。

## Governance

- Constitution 是最高工程治理约束。
- `spec.md` 只定义用户需要与可验收行为，不把框架/API 当需求。
- `plan.md` 负责技术决策、视觉资产映射、凭据、ADB、构建和 Constitution Check。
- `tasks.md` 按 User Story/依赖排序并写具体文件路径与验证动作。
- 需求变更顺序：Constitution（如治理变）→ SPEC → PLAN → TASKS。
- task 生成后先做 `/speckit.analyze` 等价只读检查；实现后做 `/speckit.converge`，追加任务继续实现，直至无剩余 gap。
- Constitution 版本遵循 SemVer。

**Version**: 2.1.0 | **Ratified**: 2026-10-02 | **Last Amended**: 2026-10-02
