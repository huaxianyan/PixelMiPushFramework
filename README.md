# Pixel MiPushFramework

[![Release](https://github.com/huaxianyan/PixelMiPushFramework/actions/workflows/release.yml/badge.svg)](https://github.com/huaxianyan/PixelMiPushFramework/actions/workflows/release.yml)
[![License GPL-3.0](https://img.shields.io/badge/license-GPLv3.0-blue.svg)](LICENSE)
![Min Android Version](https://img.shields.io/badge/android-6.0%2B-%23860597.svg)

在非 MIUI 系统上体验小米系统级推送。

应用的显示名，英文是 **Pixel MiPushFramework**，中文仍是「推送服务」。包名固定为 `com.xiaomi.xmsf`，这一点不要被名字迷惑，原因见下面的常见问题。

## 什么是小米系统级推送，为什么会有这个项目

小米推送是小米公司提供的推送服务，许多应用都在使用（如酷安）。它非常轻量，会在 MIUI 设备上自动启用系统推送，非 MIUI 设备则要在后台保持长连接。

### 系统级推送

类似 GCM，小米推送的系统级推送在 MIUI 上由系统完成。

接入小米推送的应用在启动时，会依据当前系统是否为 MIUI ROM 决定推送方式：

- 在 MIUI ROM 上，推送全部由系统完成，应用**无需后台**，更省电
- 在非 MIUI ROM 上，每个应用都要**在后台启动**一个 `XMPushService`，各自建立一条接收推送的连接。相比由系统统一实现推送，多个应用的多条连接会明显更耗电、耗内存、耗流量

### 本项目的意义

本项目起源于一个想法：让任何非 MIUI ROM 的用户都能用上类似 MIUI 的系统级推送——应用不必常驻后台，也能把消息推给用户。

## 功能

- **基本推送能力**。与 MIUI ROM 中的推送服务（`com.xiaomi.xmsf`）基本一致，默认禁止拉起应用
- **通知改写**。通过配置文件修改消息的标题、内容与样式，控制收到消息时忽略、亮屏或自动弹出等行为
- **观测**。在应用界面里查看哪些应用接入了小米推送、它们各自推了什么

### 注意

- 请给予 `com.xiaomi.xmsf` 足够的权限，不要用黑域、绿色守护、Xposed 模块去限制它，这会导致推送不稳定
- 在 MIUI ROM 上，部分依赖推送的功能不可用（如网络短信），目前明确可用的是「查找手机」
- 服务本身不需要 Root 或 Xposed，但为了让应用主动向 `com.xiaomi.xmsf` 注册，建议配合伪装增强模块使用

## 优点

- 安装简单，装完跟着向导走完即可
- 用上之后，其他应用的 `XMPushService` 会自动禁用，行为与 MIUI 一致，同时保证推送
- 完整的事件记录，可以追踪每个应用的注册与推送
- 拦截小米推送产生的不必要唤醒，也能阻止它读取隐私
- 可定制，自己决定消息的内容与行为

## 开始使用

- 到 [Releases](https://github.com/huaxianyan/PixelMiPushFramework/releases) 下载最新的 APK 并安装
- 跟着向导完成权限设置
- 可选：开启高级配置里的「推送服务保活」

### 常见问题

**为什么包名是 `com.xiaomi.xmsf`，跟应用名对不上？**

接入方的小米推送 SDK 把包名编译进了自己的字节码：SDK 判断「系统是否具备推送能力」的方式，就是查询 `com.xiaomi.xmsf` 这个包是否存在、版本号是否不低于 105。改了包名，所有接入应用都会认为设备没有系统推送，转而走自建通道，框架也就收不到任何东西了。因此 `applicationId` 必须保持原样，能改的只有显示名、图标与 namespace。

**normal 版本与 vc105 版本有什么区别？**

- 主要差异在消息传递方式：推送服务 105 版本使用 `startService`，108 及以上版本使用 `bindService`
- 优先用 normal 版本，遇到问题再换 105 版本
- 在 MIUI ROM 上，用 normal 版本的另一个好处是系统重启后应用不会变回官方版本
- 已知 COS 系统无法通过 bind 方式传递消息，建议 COS 用户选 vc105

**配置文件都有什么作用？我应该用吗？**

配置文件可以修改消息的标题、内容与样式，控制收到消息时忽略、亮屏或自动弹出等。大部分官方配置可以无脑使用；个别配置是否该用，看它的名字和配置里的 `description` 字段。

**是否应该把推送服务安装为系统应用？**

推荐。某些应用在获取应用列表时只能拿到系统应用列表，把它装成系统应用，这些应用才能发现推送服务并完成注册。

**应用用上推送服务后，`com.xiaomi.push.service.XMPushService` 被禁用，正常吗？**

正常。有了系统推送服务之后，相关功能直接委托给系统推送服务处理。

**支持分身（999）应用吗？**

目前没有这个计划，也没有测试过，不接受相关反馈。

## 反馈问题

遇到问题请先翻一遍 [Issues](https://github.com/huaxianyan/PixelMiPushFramework/issues)，看有没有人提过（最常见的是「无法收到推送」）。

没找到答案就单独开一条，并带上这些信息：

- 你的 ROM 是什么，Android 版本是多少
- 有没有使用框架一类的工具
- 通过「设置 - 获取日志」导出的日志文件

## 日志

框架会自动记录日志并保存在私有目录，可在「设置 - 高级配置」里清理。

## 参与项目

参见 [Contribution Guideline](CONTRIBUTION.md)。构建与发布流程见 [BUILDING.md](BUILDING.md)。

## 已知问题

- 部分小众 ROM（如 360OS）下无法正常工作。这类「特殊适配」只要不妨碍推送本身就不会处理，建议更换更好的 ROM
- 努比亚 ROM 上，第三方接入应用可能不会自动禁用其 `XMPushService`，可尝试将框架设为系统应用
- 部分通知特性不可用，例如所有通知都会显示为推送框架发出，而不是目标应用

## 与上游的关系

本项目 fork 自 [NihilityT/MiPushFramework](https://github.com/NihilityT/MiPushFramework)，遵循上游的 GPL-3.0 许可。

**从 1.0.0 起独立编号，不再沿用上游的 0.3.x 版本线。** 这个仓库里没有发布过 0.3.x 的正式版本，历史版本请到上游仓库查看。

感谢上游作者与所有贡献者：

- @Rachel030219 提供文件
- Android Open Source Project, MultiType, greenDao, SetupWizardLibCompat, Condom, MaterialPreference, GreenDaoUpgradeHelper, epic, Log4a, helplib, RxJava/RxAndroid, RxActivityResult, RxPermissions, hiBeaver
- 酷安 @PzHown @lmnm011223 @苏沐晨风丶（未采纳）提供图标

## 许可

[GNU General Public License v3.0](LICENSE)，遵循上游许可。
