# AudD 供应商变更

2026-10-02 用户拒绝 Apple 会员后，明确同意使用 AudD 免费额度验证；已更新 Constitution 2.1.0 → Spec → Plan → Tasks，源开发包不修改。

- 用 Standard REST 周期性提交约 12 秒短片段，初始间隔 30 秒；录音、Room、单逻辑播放、Original 不变。
- 最大 300 次本地持久化请求；服务端剩余额度以后台为准，本地计数不可承诺阻止供应商计费，Token 免费余额不足时须停用。
- 不使用每月广播流方案，不默认上传整晚，不自动开通付费或提升上限。
- 真机 30 分钟识曲 Spike 验证后决定可逆窗口策略；短歌与混音不保证识别成功，不以 mock 替代。
- 文案明确 AudD 接收音频片段，可能包含现场讲话；关闭识曲不上传。
- 增加 T110：上传同意与总预算控制，依赖顺序纳入接入阶段，最终 converge 之前验收。
- 当前 T002 仍等待 AUDD_API_TOKEN；不启动后续依赖 Task，不宣称 analyze/converge 完成。

来源：https://docs.audd.io/ 、https://audd.io/ 、https://dashboard.audd.io/
