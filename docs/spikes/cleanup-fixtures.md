# Clean AB 受控样本说明

20 份 stereo PCM WAV 来自 AudD 官方文档给出的 Imagine Dragons《Warriors》公开歌曲试听与本机合成讲话。音乐和讲话先各校准至相同 RMS 基准，再改变讲话/音乐 -12 至 +20dB 的强度和重叠位置；讲话放左声道，音乐居中。6 个独立 0.975s 模型窗口/样本。试听与派生混音仅用于本地 QA，不提交或对外分发。

源地址见 AudD 文档 https://docs.audd.io/ 的 Apple Music previews 示例。原始 AudD example.mp3 是低电平含环境噪声片段，其上传可以识曲，但不能承担主唱保护验收，已替换此 AB 来源。

这些样本用于主唱保护的受控测试，不代表 20 个不同场所的实地录音，也不能证明任意夜店中的近场定位准确率。未取得模型和处理输出证据前维持 OPEN；质量不足只允许 Beta、轻处理或 KEEP_ORIGINAL。
