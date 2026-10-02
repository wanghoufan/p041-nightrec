# NightRec 视觉资产使用规范

本目录中的图片是 V2.0 开发输入，不是营销素材。

## 固定命名

- 产品品牌：NightRec
- 核心对象：Night Session / 今晚记录
- 禁止：NightSet

## 资产用途

1. 深色产品原型：Dark Theme 的页面层级、录音/暂离/播放/时间轴/历史/设置视觉真源。
2. 浅色产品原型：Light Theme 同一流程的视觉真源。
3. Android 图标：品牌视觉 master；开发时重建 adaptive foreground/background 和 monochrome vector。
4. 启动画面：Android system Splash 的视觉参考。实际实现必须使用 Android SplashScreen API/compat，而不是把整张 PNG 当 Activity。

## 冲突优先级

Constitution > spec.md > plan.md > tasks.md > 原型图中的正文小字。

原型图中的结构、组件位置和视觉关系是强约束；若图片中文字因生成原因存在错字，以 SDD 文案为准。

## 启动画面

- Light background: `#F7F4FF`
- Dark background: `#0B0B14`
- 中央使用 NightRec adaptive icon。
- 不允许系统 Splash 之后再出现第二个专用 SplashActivity。

## Android icon

- 深靛背景 + 7 条圆角 waveform bar。
- 紫/蓝 -> 品红/粉 -> 橙/黄渐变。
- adaptive safe zone 内不得裁切最高中央 bar。
- Android 13+ monochrome 使用同一 waveform silhouette。
