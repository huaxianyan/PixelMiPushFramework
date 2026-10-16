<div align="center">
  <img src="push/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="120" alt="Pixel MiPushFramework 应用图标">

# Pixel MiPushFramework

[![Release](https://img.shields.io/github/v/release/huaxianyan/PixelMiPushFramework)](https://github.com/huaxianyan/PixelMiPushFramework/releases/latest)
[![Build](https://github.com/huaxianyan/PixelMiPushFramework/actions/workflows/test_ci.yml/badge.svg?branch=master)](https://github.com/huaxianyan/PixelMiPushFramework/actions/workflows/test_ci.yml)
[![License GPL-3.0](https://img.shields.io/badge/license-GPLv3.0-blue.svg)](LICENSE)

</div>

面向 Pixel 的小米共享推送服务，让接入小米推送的应用共用一条推送连接，并在同一处查看接入状态、推送记录和通知配置。中文显示名为「推送服务」。

本项目在上游共享推送能力上独立维护，重点是新 Android 适配、Material You 界面、连接恢复和日志隐私。

> 社区维护的非官方项目，以 Pixel 为主要验收设备。其他机型的通知外观、后台运行和接入效果需要自行验证。

## 特性

- Android 17 适配：支持 edge-to-edge、通知权限引导和预测性返回，处理服务启动与通知点击限制。
- Material You 界面：Android 12 及以上支持动态取色，层级页面采用现代动效，同级栏目保持固定外壳与内容淡入淡出。
- 服务仪表盘：集中显示连接状态、注册应用数、近期推送统计和服务运行时长。
- 按应用查看记录：先看各应用的记录概况，再查看具体事件、消息详情和配置处理结果。
- 可配置通知：通过规则调整标题、内容、渠道和通知样式，按需控制忽略、亮屏或弹出行为。
- 连接恢复与隐私：修复网络切换后连接状态卡住的问题，对日志中的推送凭据脱敏，首页聚焦服务状态与统计。
- 配套模块协作：与 [Pixel MiPush](https://github.com/huaxianyan/PixelMiPush) 分工处理设备识别适配、系统通知归属和会话样式增强。

安装包包含的功能以对应 [Release 说明](https://github.com/huaxianyan/PixelMiPushFramework/releases) 为准。源码候选与未发布改动见 [开发记录](docs/STATUS.md)。

## 快速开始

### 下载安装

框架安装包要求 Android 6.0 及以上，服务本身无需 Root 或 Xposed。系统级通知增强由配套模块提供，需要兼容的 LSPosed 环境。

**已有其他来源的同包名服务时，先阅读 [签名与迁移](docs/USAGE.md#签名与迁移)。** 不同签名无法直接覆盖，卸载会清除原有数据。

1. 从 [最新正式版](https://github.com/huaxianyan/PixelMiPushFramework/releases/latest) 下载 `normal` APK。只有接入方明确需要旧服务版本范围时，才选择 `vc105`。
2. 打开「推送服务」，按向导完成通知、电池优化等授权。
3. 按需在「设置 → 高级配置」中开启「推送服务保活」，再打开目标应用确认注册并测试一条真实消息。
4. 需要设备识别或通知增强时，另行安装 [Pixel MiPush](https://github.com/huaxianyan/PixelMiPush)，按模块文档启用并选择作用域。

### 通过 Obtainium 更新

先安装 [Obtainium](https://obtainium.imranr.dev/)，再点击下方入口导入更新源。默认跟踪正式版的 `normal` APK，`vc105` 和测试版请手动选择。

[<img src="images/obtainium-badge.png" alt="通过 Obtainium 获取 Pixel MiPushFramework" height="48">][obtainium]

## 使用与配置

仪表盘用于检查服务连接，记录页用于查看应用是否注册、何时收到消息以及消息如何处理。连接正常不等于每个应用都已接入，接入与排障步骤见 [使用说明](docs/USAGE.md)。

在设置页的「设置配置目录」中选择规则目录。可参考 [上游规则仓库](https://github.com/NihilityT/MiPushConfigurations) 按需选择配置，先查看每份规则的说明，避免同时启用互斥文件。

框架负责消息接收、记录和通知处理，Pixel MiPush 负责需要注入系统或目标应用的增强。两者分别安装和更新，具体版本的模块能力以模块文档及发布说明为准。

遇到问题时，先核对权限、应用注册和配套模块作用域，再通过「设置 → 获取日志」导出记录。提交 [Issue](https://github.com/huaxianyan/PixelMiPushFramework/issues) 前请检查日志中的设备标识、消息内容和服务器信息，参见 [反馈指南](docs/USAGE.md#日志与反馈)。

## 与上游的区别

本项目源自 [NihilityT/MiPushFramework](https://github.com/NihilityT/MiPushFramework)，保留共享推送协议、服务包名和原有事件库结构，独立维护 Android 适配、界面、修复与发布流程。

包名固定为 `com.xiaomi.xmsf`，这是接入应用寻找共享服务的约定。显示名与项目名称可以不同，原理及兼容问题见 [使用与兼容说明](docs/USAGE.md)。

本仓库使用独立版本线、长期签名和更新源。上游的旧版说明、截图及机型反馈不作为本项目当前版本的验收结论，历史来源与贡献者记录见 [致谢](docs/ACKNOWLEDGEMENTS.md)。

## 文档

- [使用与兼容说明](docs/USAGE.md)：版本选择、迁移、权限、通知规则与常见问题。
- [构建指南](BUILDING.md)：工具链、定向构建和产物校验。
- [发布约定](docs/RELEASING.md)：版本说明、签名和 Release 附件规则。
- [Android 17 适配](docs/ANDROID17-MIGRATION.md)：平台适配、页面动效和运行验证边界。
- [开发记录](docs/STATUS.md)：功能进度、验收证据与未完成事项。
- [参与贡献](CONTRIBUTION.md)：分支、代码风格和提交方式。
- [来源与致谢](docs/ACKNOWLEDGEMENTS.md)：上游项目、贡献者与第三方组件说明。

## 许可证

本项目源码沿用 [GNU General Public License v3.0](LICENSE)，保留上游版权及署名。第三方组件与资源的权属、许可不因本项目的维护而改变，具体来源见 [致谢](docs/ACKNOWLEDGEMENTS.md)。

[obtainium]: https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2522com.xiaomi.xmsf%2522%252C%2522url%2522%253A%2522https%253A%2F%2Fgithub.com%2Fhuaxianyan%2FPixelMiPushFramework%2522%252C%2522author%2522%253A%2522huaxianyan%2522%252C%2522name%2522%253A%2522Pixel%2520MiPushFramework%2522%252C%2522additionalSettings%2522%253A%2522%257B%255C%2522apkFilterRegEx%255C%2522%253A%255C%2522%255EPixelMiPushFramework-normal-universal-.%252A%255C%255C%255C%255C.apk%2524%255C%2522%252C%255C%2522includePrereleases%255C%2522%253Afalse%252C%255C%2522versionDetection%255C%2522%253Atrue%252C%255C%2522appName%255C%2522%253A%255C%2522Pixel%2520MiPushFramework%255C%2522%257D%2522%257D
