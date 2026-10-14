# Android 17 迁移与导航动效

## 目标与边界

框架目标系统为 Android 17（API 37）。用户已将后续节奏改为直接形成 target 37 候选、集中真机验收，不再逐级发布和安装中间版本。target 31 的已有设备结果保留作为诊断基线，不重复测试。当前目标已设为 37，alpha.3 已发布并使用线上附件覆盖安装。启动、连接、specialUse 前台开关和返回键的基础运行检查通过，结果见 STATUS-016。真实新消息、默认点击及其他边界未闭环，不把基础运行视为整体适配完成。

保留包名 `com.xiaomi.xmsf`、3.7.9 SDK、schema 17、注册与 ACK 行为。SDK 更换、数据库迁移、签名切换和界面组件翻新不混入本轮。版本取值以根构建文件为准。

安装、清数据、重启与发布分别授权。应用是接入方寻找的共享服务，不改包名建立并行推送服务。编译和静态检查不能代替 ART、服务运行与真实推送验收。

## 导航动效

- 主页及帮助页共用 `Tokens.kt` 中的 `Motion`，移除没有调用方的旧时长和 easing 参数。
- 前进时，新页从逻辑末端完整滑入，旧页向逻辑起始端移动容器宽度的三分之一。返回使用相反的分层位移，方向由 Compose 处理 RTL。
- 页面保持不透明，空间移动使用 Compose 的公开弹簧实现，不给页面转场叠加淡入淡出。没有自定义 effects 参数时，沿用组件自身反馈。
- 每个目的地拥有自己的 Scaffold。主页详情切换时，顶部栏及底栏随所属页面移动，导航容器保持全屏尺寸，避免原底栏收缩改变内容区域。
- Manifest 接入预测性返回，手势进度、取消及提交由现有 navigation-compose 的 NavHost 处理，不另建返回栈或自行消费手势。
- 此次统一 Compose 页面转场，跨 Activity 转场保留系统实现。PixelText 是技能中的示例，本轮没有把它当作指定样板或复制其依赖。

## 分级迁移

| 阶段 | 需要核对的边界 | 验收点 |
| --- | --- | --- |
| target 31／32 | 组件 exported、PendingIntent 可变性、通知点击中转、后台前台服务启动、旧 SDK 定时器 | 应用启动、服务持续运行、重连、通知点击及恢复 |
| target 33 | 通知运行时权限，拒绝和再次授权后的服务状态 | 首次授权、通知发布、服务状态通知 |
| target 34 | 前台服务类型与权限、动态 Receiver 导出标志、隐式 Intent 与 PendingIntent | 前台服务启动、系统及接入方广播、注册入口 |
| target 35 | edge-to-edge、系统栏与 IME 边距、后台启动及服务限制 | 页面与输入框布局、后台恢复、通知打开目标应用 |
| target 36 | 预测性返回、大屏可调整性、退出边到边开关失效 | 返回提交与取消、Activity 返回、大屏布局 |
| target 37 | 本地网络权限、TLS 默认行为、反射写入 final 字段及 MessageQueue、后台音频与 BAL | 实际服务连接、ART、通知与点击，不以模块宿主验收代替框架验收 |

上表现在用作行为归因清单，不再对应逐级装机门禁。仅对最终 target 37 候选集中验收，按对应平台边界记录失败原因。target 31 的结果不能替代 37 下的实际运行证据。

## 已发现的 target 31 前置问题

带 intent-filter 的组件已显式标记 exported，保留原 target 30 下的导出语义及已有权限保护，尚未改变服务调用协议。合并清单检查发现 IceBox SDK 的 StateReceiver 也缺少该声明，已在应用清单补充导出属性；最终合并清单的 12 个带过滤器组件均有显式声明。

target 31 尚未闭合的边界如下，当前保留这些问题进入真实行为验收：

- 固定通知点击及通知备份均已指定 immutable。备份升级到格式 2，用户接受首次迁移不恢复旧格式临时备份。新格式的真实更新恢复仍待设备验收。
- 原 3.7.9 SDK 的 PendingIntent 调用已审计并织入 immutable，原 JAR 未替换。精确闹钟使用已有省电豁免，不增加闹钟权限；实际授权、定时调度和后台启动仍需设备验收。
- 框架的 6 个显式服务启动入口已统一按前台通知配置选择启动 API，服务也在每次启动时重新应用该配置。后台启动权限、用户撤销电池优化豁免后的状态及重连仍待设备验收，不能把普通服务启动当作系统限制的豁免。
- 默认通知点击仍可进入 Service，由它再启动目标 Activity。必须核对 target 31 的通知中转限制，保留消息 payload、ACK 及现有直接 Activity 配置的语义。

## 第二批：固定 PendingIntent 与 SDK 织入

应用内的通知点击 Intent 都在创建时写入完整 payload，保留请求码、更新标志及 Activity／Service 类型，仅增加 immutable。未改变直接打开 Activity 的配置或 ACK 流程。

原 SDK 中有 4 个实际创建 PendingIntent 的类：AlarmManagerTimer、XMPushService、MIPushNotificationHelper 和 NotifyAdsManagerNew。通过现有织入入口统一补充 immutable，保留原 API、Intent 和定时参数。原 SDK 的通知点击走 `send()`，没有发现这些调用向 PendingIntent 补充 Intent 数据的路径；应用通知恢复不在这一织入范围内。

新增 advice 需要解析旧 SDK 引用的 Apache HTTP 类型。编译配置及应用清单已声明共享 Android Platform 的真实 `org.apache.http.legacy` 可选库，required 为 false；没有添加假类型、屏蔽织入错误或打包另一份 HTTP 实现。原有宽泛匹配曾因缺少 `NameValuePair` 类型失败，已改成实际使用的三个工厂方法，并补齐平台类路径。

`mipush.target` 是只写入通知 extras 的额外 PendingIntent，在框架与配套模块中均未找到读取方，已移除该额外创建路径及因此失去调用方的 Intent 构造方法。主通知的 contentIntent 保留。

Debug／Release hook AAR 和 normal Debug Kotlin／Java 编译均已通过。两个 AAR 的 758 个 SDK 类保留，10 个切面完成初始化，4 个 SDK PendingIntent 创建类均包含实际 advice 引用，原 15 个关键处理器调用检查仍通过。验证只覆盖结构，不证明设备上的 PendingIntent、ART、AlarmManager 或消息恢复已通过。

SDK 的 `enableForegroundService()` 经字节码检查为空方法。真正的框架前台提升仍依赖 `ForegroundHelper`，而配置允许关闭该通知。后台启动策略与这一配置必须一起核对，不把 SDK 空方法当作保活实现。

## 第三批：服务启动与前台通知策略

`PushControllerUtils.startPushService()` 是框架显式启动 SDK 服务的统一入口。开启前台通知时使用 `ContextCompat.startForegroundService()`，关闭时使用普通 `startService()`。控制器、桥接服务和网络、卸载、心跳、保活 Receiver 共 6 个入口调用同一策略，原 Intent、action、extras、时间戳和异常处理保持原样。

原 `ForegroundAbility` 只在服务创建时应用配置。现在通过已织入的 SDK `onStart()` 回调，每次启动都重新应用前台通知配置，覆盖服务已经运行时的再次启动。保留设置界面既有的前台通知开关，没有新增失败后的备用启动路径。注册、ACK 与定时参数未改。

普通 `startService()` 同样受后台执行限制。此次修复的是「请求前台启动但关闭前台通知」的矛盾，不证明后台启动均被允许，也不保证普通服务长期存活。电池优化授权、自启、广播恢复及重连仍需在提升 target 后验收。已核对 Android 12 官方限制，没有把前台启动异常转成任务执行成功。

第三批保留旧通知备份实现。AOSP 的 `PendingIntentRecord.Key` 将 flags 纳入身份匹配，增加可变性标志会改变旧备份的检索条件。后续用户选择新的 immutable 格式，迁移取舍及实现见第四批。

normal Debug Kotlin／Java 定向编译通过，用时 55 秒。没有改动 hook AAR，也没有重跑第二批已通过的 AAR 检查。前台通知开关两种状态、已有服务再次启动和关闭通知后的后台恢复尚未做设备验证。

## 第四批：immutable 通知备份

用户选择直接采用新格式，接受首次迁移不恢复旧格式临时备份。没有引入过渡版本、旧格式读取分支或隐藏 API。注册数据、推送历史和数据库 schema 未改。

纠正此前「通知恢复必须 mutable」的判断：回填的 `EXTRA_INDEX` 只用于日志，恢复序号由 `send()` 的结果码传入 `OnFinished`。现已移除这项回填及字段，直接发送原备份 payload。Receiver 使用中性的交付日志，避免把恢复发送误记为过期。

备份版本为 2，版本只在 Intent action 中定义命名空间。创建与检索使用同一组件、action、从零开始的序号和 immutable 标志。移除旧的序号偏移及兼容性哨兵检查。恢复后保留原通知 tag／id，并沿用既有分组处理及 AlarmManager／PendingIntent 清理。

保存条件仍为既有的无 tag、非持续通知筛选，普通带 MiPush tag 的通知不纳入此备份机制。本轮没有扩大保存范围。正式版定时参数仍为 30 秒，Debug 为 5 分钟；AlarmManager 实际交付可能延后，不能把这两个参数当作严格的存活上限。

normal Debug Kotlin／Java 编译通过，用时 48 秒。没有生成 APK、安装或运行设备测试，不能把编译结果记为更新恢复已验收。

本能力保留一条真实更新验收流程，取得安装授权后执行：

1. 安装格式 2 版本并保持服务运行。在受控环境准备一条符合原筛选条件的提醒。
2. 通过 PackageInstaller 更新到另一份同签名、同备份格式的 APK。
3. 确认标题、正文及通知标识恢复。

普通 ADB 安装不自动等价于该流程。首次从格式 1 迁移的临时备份不属于恢复承诺。

## 第五批：target 31 审计点

根构建文件的唯一 target 定义已提升到 31，保留原包名、compileSdk、minSdk、SDK 和 schema。不继续停在旧 target 做推测性修补，也不预先增加打开应用的中转页面。该批当时停留在 target 31；后续节奏已由用户调整为直接形成 target 37 候选，见 STATUS-014。

官方 `AlarmManager.canScheduleExactAlarms()` 文档明确：target 31 以上持有精确闹钟权限，或位于设备省电豁免名单时，可以调度精确闹钟。现有引导要求省电豁免，因此不为 SDK 心跳额外申请 `SCHEDULE_EXACT_ALARM` 或 `USE_EXACT_ALARM`。这不等于后台服务启动自动获准，也不证明当前设备已经取得豁免。

应用在申请省电豁免前就会尝试启动服务，引导完成没有显式重新启动服务。首次授权后的定时调度及恢复因此是具体待验收点，不能只测已经授权的热启动。暂不以假设增加重启、备用定时器或错误吞掉后的继续执行。

已知未解决项是默认通知点击仍使用 Service，再由它启动目标 Activity。它不符合 target 31 的通知中转迁移要求。现有直接 Activity 配置保留，不能拿白名单应用成功证明默认路径通过。审计版本不能按正式可用版本交付。

normal Debug Kotlin／Java 编译及合并清单通过，用时 45 秒。合并清单的 target 与根定义一致，包名仍为 `com.xiaomi.xmsf`，没有新增精确闹钟权限。未生成 APK，也未安装或发布。

已从主题分支发布 [v1.1.0-alpha.2](https://github.com/huaxianyan/PixelMiPushFramework/releases/tag/v1.1.0-alpha.2) 审计测试版，线上附件已复核。alpha.1 因原生库命名门禁失败，其标签保留。正式 Latest 仍为 v1.0.1。随后用户授权本机直连验收，已核对现装签名、版本和权限并同签名覆盖 normal alpha.2，初步设备结果见 STATUS-012，后续真实 QQ 新消息接收及直接 Activity 点击通过，证据边界见 STATUS-013。默认 Service 点击仍未完成验收。签名不符时停止，不卸载或清数据。至少覆盖首次省电授权后的调度、前台开关两种状态、连接恢复，以及直接 Activity 和默认 Service 两条通知点击路径。备份和导航按前述真实入口验收。

## 本批验证

前五批通过 normal Debug Kotlin／Java 编译及 Manifest 合并。随后主题分支及发布 CI 通过，已生成并公开 alpha.2 签名附件。线上包的版本、签名、ABI、大小及哈希复核完成，结果见 STATUS-011。后续授权覆盖安装成功，连接、前台开关、主动重连和返回键状态完成初步检查，未完成项及证据边界见 STATUS-012；真实 QQ 新消息和直接点击结果见 STATUS-013。这些结果均来自 target 31，最终 target 37 候选尚需集中验收。

导航动效及预测性返回尚未在设备上验收。装机授权后，先用一条真实入口流程验证：从主页事件页打开某应用的消息详情，执行一次返回取消，再提交返回。观察详情、顶部栏和底栏的层次，确认取消后仍停留在详情，提交后回到原列表位置。帮助文章进退和四个主标签切换随后作人工对照。

不读取截图；如需截图分析，单独取得图片授权。

## 官方参考

- [Android 12 目标版本行为变更](https://developer.android.com/about/versions/12/behavior-changes-12)
- [Android 8 后台执行限制](https://developer.android.com/about/versions/oreo/background)
- [AlarmManager.canScheduleExactAlarms](https://developer.android.com/reference/android/app/AlarmManager#canScheduleExactAlarms())
- [精确闹钟适用范围](https://developer.android.com/develop/background-work/services/alarms/schedule)
- [AOSP PendingIntent 身份匹配](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-16.0.0_r1/services/core/java/com/android/server/am/PendingIntentRecord.java)
- [Android 13 目标版本行为变更](https://developer.android.com/about/versions/13/behavior-changes-13)
- [Android 14 目标版本行为变更](https://developer.android.com/about/versions/14/behavior-changes-14)
- [Android 15 目标版本行为变更](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Android 16 目标版本行为变更](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 全部应用行为变更](https://developer.android.com/about/versions/17/behavior-changes-all)
- [Android 17 目标版本行为变更](https://developer.android.com/about/versions/17/behavior-changes-17)
- [预测性返回](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
