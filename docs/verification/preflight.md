# T002 一次性凭据/环境预检

2026-10-02；状态 WAITING_CREDENTIALS，不是 Human Gate。

- 源开发包已导入，全部规范文件和视觉资产 SHA-256 与源一致。
- 已发现 JDK 17.0.20.1+1、Android SDK 36、Build Tools 36.0.0；Gradle 项目尚未创建，构建能力尚未验证。
- adb 37.0.1 沙箱内启动受限；沙箱外 adb devices -l 成功。
- USB 目标 indq5xfi6hovay4d，型号 22101316C，Android 14，device。另有无线 22041216UC，本轮不用。
- 固定 ANDROID_SERIAL=indq5xfi6hovay4d。
- 项目、Downloads 和工具链未找到 ShazamKit AAR。
- 环境无 SHAZAM_DEVELOPER_TOKEN；项目原无本地配置。已创建忽略的 local.properties，Token 字段位于最底部。
- 仅待 ShazamKit AAR 本地路径和已签发 Developer Token；不索取数据库 key 或 .p8。
- T001 已有 fresh 读取/视觉检查/文件 checksum 证据；T002 未完成，后续 Task 未宣称完成。

## 2026-10-02 用户批准替换供应商

ShazamKit AAR 与 Developer Token 需求已取消；唯一缺失项是 AUDD_API_TOKEN，配置位于 local.properties 最底部。ADB 目标沿用已验证 USB 手机。这是同一次未结束的凭据预检，不新增技术选型提问。
