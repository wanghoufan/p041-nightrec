# Ruling：Spike 需要最小 Android 构建宿主

T004–T007 在 T009 创建项目之前，无法执行真实 Android API Spike。将 T009/T010 的最小构建骨架作为 Spike 宿主提前搭建，不标记完整 Foundation 完成；业务能力必须经 Spike 后逐项验证。构建仅生成 debug 包，不 commit/push。单人模式沿用本次指令，不派 subagent。

Clean 检测缺少可证实模型和真实 AB 基线，不能凭音量/频谱冒充近距离讲话 AI；必须保留 KEEP_ORIGINAL 并把未验证能力记录为未完成。T007 先实证验证可行性，发现能力缺口就修 SDD 方案而非宣称已实现。
