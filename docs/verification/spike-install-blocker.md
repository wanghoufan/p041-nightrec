# 真机安装阻塞证据

- 目标 USB device: indq5xfi6hovay4d / 22101316C / Android14 / arm64-v8a。
- APK fresh 构建存在；九项 domain tests 已通过。
- 两次 installDebug 均返回 INSTALL_FAILED_USER_RESTRICTED，见 spike-install.log 与 spike-install2.log。
- ADB 查看开发者选项发现 USB安装入口。试图点击开启的操作被自动审批拒绝，要求明确授权持久性安全设置修改。
- 已通过异步问题向用户请求授权；不绕过安全限制、不切其他设备、不宣称真机验收完成。
- T004–T008 待真实 Spike；Foundation 提前搭建仅用于测试宿主。
