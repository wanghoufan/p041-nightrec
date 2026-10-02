# T001 视觉基线

2026-10-02 已逐份读取 Constitution、Spec、Plan、T001–T109 与视觉规范；已查看 design 全部六张 PNG 并读取自适应前景 SVG。

- 产品 NightRec；业务对象 Night Session / 今晚记录。
- 信息架构：01 欢迎、02 权限、03 首页、04 开始设置、05 Recording、06 Away、07 结束确认、08 Processing、09 Session 完成、10 整晚播放器、11 歌曲时间轴、12 转场详情、13 歌曲详情、14 历史、15 设置。Interrupted/Recovering 补充明确的状态表达。
- Light/Dark 共用页面与交互位置；系统跟随默认；品牌紫→粉→橙渐变，禁动态色覆盖。
- 浅色启动背景 #F7F4FF；深色启动背景 #0B0B14；仅平台 SplashScreen/compat，不建 SplashActivity。
- 图标使用深靛背景与七柱波形；adaptive safe-zone 与 Android 13 monochrome 同轮廓。
- Recording：顶部 REC/计时，中部波形、当前/上一首，底部暂离和结束；Away：暂离时间和继续同 Session。
- 播放器：Original/Clean 切换、单逻辑进度、现场 Marker；不以原曲替代现场。
- 文案采用 SDD；原型 Pause、后台权限、自动删除、API key、77任务、release 等过期小字均不覆盖 SDD。
- 当前单人模式采用用户本次明确指令，不派角色链；不做未授权 commit/push。

证据：docs/verification/input-manifest.json；包内资产已复制到本仓，原开发包保持不动。
