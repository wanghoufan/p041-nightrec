# 保守讲话净化 Spike 方案

使用 TensorFlow 官方 YAMNet 音频事件分类模型作为候选，输入为 15600 个 float32 的 16kHz mono 样本，输出 1×521；模型结构已从真实 TFLite 文件读取。官方模型 SHA-256：e193da5676a2fdb7a3702c10fba62aa27d76f64e4534467443811c1a2ff1667d。

模型只能判断讲话/唱歌事件，不等于判断“附近讲话”，更不能分离主唱。因此须结合近场线索与主唱/说唱否决，并在低置信度保持 Original；处理限定轻度、局部和 crossfade，结果写派生文件。不能仅凭类别为 Speech 就去人声。正式采用需真机推理、20样本AB与音乐主唱保护验证，不因下载成功视作 T007/T079 完成。

模型来源：https://storage.googleapis.com/audioset/yamnet.tflite
代码/类别与许可证来源：https://github.com/tensorflow/models/tree/master/research/audioset/yamnet
