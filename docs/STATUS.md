# Pixel MiPushFramework 交接

## 当前主线

维护 Pixel 上的小米共享推送服务，使用独立版本线。当前基线包含 `v1.0.1` 的日志脱敏和首页调整。Android 系统行为迁移另开主题分支，保留现有推送能力。

## 接手入口

- [README](../README.md)：功能、适配范围与安装。
- [BUILDING](../BUILDING.md)：工具链、产物检查和 CI，版本参数以构建文件为准。
- [v1.0.1 发布说明](release-notes/v1.0.1.md)：隐私修复、安装与已知问题。
- `scripts/`：AAR、DEX 和 APK 的检查入口。
- 研究资料、签名恢复和设备操作边界见本地 `AGENTS.md`。

## STATUS-001：整理构建与设备验收边界

- 日期：2026-10-13。
- 仓库：`huaxianyan/PixelMiPushFramework`，分支 `master`，整理前基线 `39039ed5`。
- 当前实现：保留 `com.xiaomi.xmsf`、原 3.7.9 SDK 和 schema 17。构建及界面翻新已进入独立版本线，日志凭据脱敏与首页标识移除已纳入基线。
- 历史验证：旧记录记载 2026-09-18 至 09-20 的连接恢复、单设备推送和界面验收，以及 `v1.0.1` 装机确认。这里只补录结论，不把历史设备状态当作当前实测。
- 验证限制：AAR、DEX、APK 或签名检查各有边界，不能单独证明 ART 加载和真实推送。历史日志验收未产生真实新注册凭据样本，脱敏规则还有单元测试证据，不能说覆盖了所有真实消息形态。
- 已知问题：重连退避上限、旧消息时间戳、默认关闭的前台服务沿用发布说明。edge-to-edge 与目标 SDK 迁移尚未完成，按独立分支推进。
- 本轮完成：建立公开交接入口，整理私有约定，将 BUILDING 中构建批次的验证边界与后续历史设备验收分开。
- 验证边界：本轮未构建、运行测试、查询设备或下载 APK。没有替换签名、卸载应用、清数据或发版。
- CI 与发布：文档推送后的工作流结果未查询，本轮未推发布标签。旧记录中的 CI 成功不代表这次推送成功。
- 下一步：新功能或修复使用主题分支，按实际范围验收。装机验收使用正式 Release 附件并单独取得授权，签名不匹配时先确认数据备份和切换方案。

## STATUS-002：统一后续 Release 与 Obtainium 入口

- 日期：2026-10-13，分支 `chore/release-standard`，起点 `10b7ac9f`
- 新增版本化发布说明门禁与 APK 整理脚本，实际包信息生成构建表。normal／vc105 附件带项目、变体、架构和版本，同名附带 SHA-256 文件
- 发布标题仅标签，缺说明时停止，保持现有长期签名，禁止覆盖已有 Release 附件。显式 versionName 参数保留预发布后缀原值
- README 添加 Obtainium 图标入口，只选择新命名的 normal 正式版。历史版本、附件及发布正文保持原样
- 验证：说明门禁的 5 项回归通过，工作流 YAML 与 shell 语法、Obtainium 配置和 APK 筛选通过。整理脚本使用历史已签名模块 APK 完成一次真实签名／元数据／附件校验，临时目录已清理
- 边界：样本验证不证明当前框架 APK 构建或新模块包名运行成功。本轮未构建框架、实跑 GitHub CI、发布或操作设备
- 下一步：选定版本并填写对应说明，再确认 CI 与正式发布授权。发布流程见 `docs/RELEASING.md`

## STATUS-003：发布改造提交与推送准备

- 日期：2026-10-13，用户授权把此前 README 及相关改动全部推送
- 本轮合入 README、Obtainium 入口、实际 APK 构建信息、附件命名、版本化说明门禁及显式版本参数修正，不修改推送业务
- 框架保持 `v1.0.1` 发布基线，本轮不创建新标签或发布框架 APK。模块另行发布独立版本线首版 `v1.0.0`
- 验证沿用 STATUS-002 已通过的脚本与配置检查，不重复构建框架。改造已通过提交 `e9a8873f` 快进合入并推送 `master`，本轮使用 `[skip ci]`，未构建或发布框架
- 模块 `v1.0.0` 已通过标签 CI 并公开发布。框架仍为 `v1.0.1`，未来新附件命名从下一次框架 Release 生效，Obtainium 的框架入口目前仍无新命名附件匹配

## STATUS-004：Android 17 迁移前置与导航动效

- 日期：2026-10-13，分支 `feat/android17-framework`，起点 `f8a36279`。用户指定按 `android-modern-standards` 技能执行，不指定 PixelText 为样板
- 已改：带 intent-filter 的组件显式声明 exported，保留原有导出语义及权限保护；Manifest 接入预测性返回。主页及帮助页共用不透明分层滑动，移除未使用的旧动效参数及底栏淡出／收缩路径
- 主页各目的地拥有自己的 Scaffold，详情转场覆盖整页；共享 Motion 使用逻辑方向与公开弹簧 API，预测性返回交给现有 NavHost，不新建手势处理或返回栈
- 验证：`:push:compileNormalDebugJavaWithJavac :push:processNormalDebugManifest -PversionName=android17-stage1` 成功，用时 1 分 2 秒，Kotlin／Java 编译通过。合并清单检查发现第三方 IceBox StateReceiver 缺少 exported，补充属性后仅重跑清单任务；最终 12 个带过滤器组件均有显式声明，预测性返回标志为 true。原有弃用及 Manifest tools 警告保留
- 边界：targetSdk 仍为 30，compileSdk 仍为 36，SDK 3.7.9、schema 17、签名和推送行为保持原样。本批未生成装机 APK、安装、重启、提交、推送或发布，动效与返回手势尚未设备验收
- 后续阻塞：现有 PendingIntent 可变性、旧 SDK 定时器、通知点击 Service 中转、后台启动前台服务及可关闭前台通知的语义需在升 target 31 前核对。不得仅提升数字后沿用旧推送验收
- 迁移分级、当前问题和真实入口验收流程见 [Android 17 迁移](ANDROID17-MIGRATION.md)。下一阶段先处理 target 31 的实际服务与通知边界，阶段确认后再提升到 33／34／35／36／37

## STATUS-005：固定 PendingIntent 与 SDK 创建点修复

- 日期：2026-10-13，继续在 `feat/android17-framework` 工作，保留首批未提交改动
- 应用固定通知点击和电池优化授权通知指定 immutable，保留现有请求码、payload、更新标志与 Activity／Service 类型。移除框架及配套模块中未找到读取方的 `mipush.target` 额外 PendingIntent
- 原 3.7.9 SDK 的实际创建点已审计，新增 PendingIntentAspect，通过现有 AJC 织入补充 immutable，不替换 SDK JAR。覆盖 4 个实际创建类，该批未修改通知恢复实现
- 织入缺少旧 SDK 引用的 Apache HTTP 类型，前两次失败未产生有效 AAR。已将切点收敛到三个实际工厂方法，并用 AGP useLibrary 接入共享 Platform 的真实可选库；应用合并清单已确认 uses-library required=false，不补假类、不屏蔽错误
- 验证：Debug／Release hook AAR、normal Debug Kotlin／Java 编译成功，用时 1 分 25 秒。两个真实 AAR 检查通过：758 个 SDK 类、10 个切面初始化、4 个 PendingIntent 创建类 advice 引用，以及原 15 个处理器调用均保留。移除孤立 Intent 构造方法并补充平台库清单后，仅重跑应用编译及清单检查，通过；没有重跑已通过的 AAR 检查
- 该批保留通知恢复原实现，未把这条路径记为 target 31 已适配。关于「必须 mutable」的初判已在 STATUS-007 纠正
- 新证据：SDK enableForegroundService() 为空方法；AlarmManagerTimer 通过反射调用精确定时。精确闹钟权限、可关闭前台通知的启动语义，以及通知 Service 中转仍是 target 31 的阻塞项
- 边界：targetSdk 仍为 30，没有生成或安装应用 APK，没有提交、推送或发布；AAR 与编译成功不证明 ART、真实推送、点击或消息恢复等价

## STATUS-006：统一框架服务启动策略

- 日期：2026-10-13，继续在 `feat/android17-framework` 工作，保留前两批未提交改动
- 新增统一入口 `PushControllerUtils.startPushService()`。开启前台通知时走前台启动，关闭时走普通服务启动。控制器、桥接和 4 个 Receiver 共 6 个入口复用同一策略，保留原 action、extras、时间戳及异常处理
- `ForegroundAbility` 新增启动回调，服务已运行时再次收到启动命令也会应用当前前台通知设置。复用既有 SDK onStart 织入，不新增监听层或持久化状态
- 验证：normal Debug Kotlin／Java 定向编译通过，用时 55 秒。hook 输入未改，没有重跑已通过的 AAR 检查。前台通知开关两种状态及后台恢复尚未在设备验证
- 边界：普通服务仍受后台执行限制，前台服务仍受后台启动限制。本批只修正启动 API 与通知开关的矛盾，不以捕获异常、备用路径或临时提升前台来宣称任务已执行
- 该批尚未确定旧备份迁移方案：AOSP 身份匹配包含 flags，直接加入 mutable 会改变旧备份检索条件。后续选择及实现见 STATUS-007
- 下一步：确定旧备份的跨版本恢复方案，核对精确闹钟的真实授权边界与通知 Service 中转，准备 target 31 装机验收。安装授权和各级验收确认仍单独取得
- targetSdk 仍为 30，没有生成装机 APK，没有安装、提交、推送或发布

## STATUS-007：immutable 通知备份格式 2

- 用户明确选择直接采用新格式，接受首次迁移不恢复旧格式临时备份，不增加过渡版本或隐藏 API 兼容路径
- 纠正前述判断：EXTRA_INDEX 回填只影响日志，实际恢复序号通过 send() 的结果码传给 OnFinished。移除回填及字段后，创建和检索均使用 immutable
- 备份版本只在 Intent action 中建立格式命名空间，序号从零开始。删除旧偏移及兼容性哨兵检查，避免不同格式共享备份身份
- 保留既有筛选、payload、通知 tag／id、分组处理和恢复后的清理。注册数据、推送历史、schema 17 均未改；无 tag、非持续通知的原筛选不等于所有 MiPush 通知均会备份
- 定向编译：normal Debug Kotlin／Java 通过，用时 48 秒。没有重跑未改动的 hook AAR 检查，日志留在本地研究目录
- 验收状态：新格式的真实 PackageInstaller 更新恢复尚未执行。保留一条真实更新流程，条件及首次迁移限制见 Android 17 迁移文档
- 后续处理精确闹钟授权和通知点击中转，再准备 target 31 验收。targetSdk 仍为 30，本批未生成 APK、安装、提交、推送或发布

## STATUS-008：进入 target 31 审计点

- 唯一 target 定义由 30 提升到 31，compileSdk、minSdk、包名、SDK 3.7.9、schema 17 和签名配置未改。分支仍为 feat/android17-framework，尚未提交或合入基线
- 官方 API 文档确认省电豁免名单可免精确闹钟权限。沿用既有省电授权引导，不为 SDK 心跳新增 SCHEDULE_EXACT_ALARM 或 USE_EXACT_ALARM，也不把这项豁免当作所有后台启动均被允许
- 具体风险：Application 在省电授权前尝试启动服务，而引导完成没有显式重新启动服务。首次授权后的定时调度和恢复需实际验证，未添加未经验证的重启或定时降级路径
- 已知待修：默认通知点击仍经 Service 启动 Activity。保留原 payload、SDK 回调及直接 Activity 配置，不能只测白名单应用并宣称中转限制已解决
- 验证：normal Debug Kotlin／Java 编译及合并清单成功，用时 45 秒。合并清单 target 与根定义一致，包名 com.xiaomi.xmsf，没有新增精确闹钟权限
- 此处是审计点，不是已验收的 target 31 基线，更不是 Android 17 适配完成。没有生成 APK、安装、提交、推送或发布
- 下一步须单独授权测试版发布及同签名覆盖安装。核对实际设备、安装版本、签名和权限后，验证首次授权、服务两种前台设置、连接恢复及两种通知点击路径；按真实结果修正，验收确认前不升 target 33

## STATUS-009：预发布准备

- 用户仅授权提交、推送主题分支并发布 GitHub 预发布测试版，设备安装与验收仍未授权
- 目标标签 v1.1.0-alpha.1，保留 master 已验收基线和 v1.0.1 正式 Release。主题分支 CI 通过后再创建新标签，工作流按 prerelease 发布
- normal versionCode 在唯一构建定义中递增到 1003003001，vc105 保留 105 兼容编号。签名与 SDK 行为约束不变
- 普通构建 CI 限定分支推送和 PR，标签只运行发布工作流，避免重复未签名验证。发布仍构建 normal 和 vc105 两个签名附件并追加实际 APK 身份信息
- 发布说明明确声明 target 31 尚未设备验收、默认通知 Service 中转待修，以及首次迁移不恢复旧临时备份。发布流程允许主题分支审计预发布，正式版仍要求已验收代码
- 首次分支 CI 的 APK 构建与 AAR 检查通过，最终 APK 门禁因另存旧 normal 版本号而失败。已修正门禁，从构建文件读取版本号，保留原包名、SDK、DEX 和未签名检查；修复后 CI 通过前不推标签
- 此条记录的是准备状态，发布结果及线上附件复核完成后追加记录。发布预发布不等于进入下一 target 阶段或获得安装授权

## STATUS-010：alpha.2 原生架构发布准备

- alpha.1 发布失败，原因是发布脚本误以为当前 APK 没有原生库。分支 CI 报告确认 AndroidX graphics path 库实际覆盖四种现代 ABI。签名构建完成，但未生成公开 Release，alpha.1 标签保留
- 用户授权继续修正打包检查并改发 v1.1.0-alpha.2，安装仍未授权。保留 master 和正式 v1.0.1
- 构建文件移除没有对应二进制的旧 armeabi 声明。ABI 名单只有构建文件一份，未签名检查及发布整理共用 apk_abi.py，检查实际架构及各架构库覆盖一致
- 完整多架构包继续命名 universal，构建信息列出实际 ABI。包名、SDK、版本号和长期签名检查保持原样，没有取消架构门禁
- alpha.2 保留相同审计范围和未验收声明，normal versionCode 为 1003003001，vc105 为 105。此条是准备记录，实际发布与线上复核完成后追加结果

## STATUS-011：alpha.2 已发布并复核

- [v1.1.0-alpha.2](https://github.com/huaxianyan/PixelMiPushFramework/releases/tag/v1.1.0-alpha.2) 已公开为 prerelease，源码标签固定在 49e5b1b58759e05333809925948ad482d08c01fa。alpha.1 失败标签保留，没有覆盖任何历史 Release
- 分支 CI 38510220171 及发布 CI 38511072297 均成功。正式 Latest 仍为 v1.0.1，master 未合入未验收代码
- 线上附件已下载复核，共 normal／vc105 两个 APK 及同名 LF 校验文件。实际包名 com.xiaomi.xmsf，版本 1.1.0-alpha.2，min／target SDK 为 23／31，均为非调试包，v2 签名及长期证书匹配
- normal versionCode 1003003001，文件 13,875,143 字节，SHA-256 为 b12a9108abe3020decf9b9c56eeb82d2e414a74e4b17e22a660dff2b57dcafff
- vc105 versionCode 105，文件 13,875,146 字节，SHA-256 为 038a4fde36de1b701d6c92e8be0bd9315c9fa37d6ecf13d825cd26ba5d9dff5c
- 两个 APK 的四种实际 ABI 均完整，构建信息与实际包匹配。Release 正文 33 行，标题只用标签，签名证书与源码提交均已展示
- 新增 ABI 归档检查模块测试通过，夹具只验证归档布局，不是 ELF 加载或 ART 运行证据。真实 APK 的完整性、架构及签名由 CI 和线上下载复核分别确认
- 设备安装与验收仍未授权，当前没有安装动作。target 31 的默认 Service 通知点击、首次授权后调度、后台恢复、真实推送及导航手势继续待验收，暂不升 target 33

## STATUS-012：alpha.2 本机设备初步验收

- 用户授权本机直连设备验收。现装 1.0.1／target 30 的证书与发布附件一致，使用 GitHub Release normal alpha.2 同签名覆盖成功，保留数据，未卸载、清数据或重启
- 设备为 Android 17／API 37.1。更新后系统真实启动推送服务，服务记录 target 31，首页显示已连接。已有注册应用和历史记录仍可见；这不等于已验证新的注册交换或消息 ACK
- 从真实高级设置入口关闭前台保活，服务退出前台；再次开启后恢复前台。原设置为开启，验收后已恢复。当前设备同时具备省电豁免和悬浮窗授权，不能据此推广无授权环境的后台启动能力
- 新心跳 PendingIntent 使用 immutable，精确闹钟 window 为 0，系统许可来源为 allow-listed。包的心跳交付统计从 367 增至 371，闹钟持续重新排期。更新初期同时存在旧身份闹钟，不能把包级计数全部归因于新 PendingIntent，也没有覆盖首次省电授权流程
- 在服务器设置中确认原有空值，不更改有效服务器，通过应用真实入口发起重连后首页再次显示已连接。此项是主动重连，不是断网恢复或后台进程回收验收
- 从事件页进入 QQ 记录详情，再按返回键，回到原列表且记录行位置保持。设备当前使用三键导航，没有验证返回手势进度、取消及视觉层次，也没有读取截图
- 通用 ADB 隐式启动 Intent 未解析，随后非 root 显式启动公开 WelcomeActivity 正常跳转主页。初次检查曾使用 root 打开内部主页，不以该动作证明公开入口可用；桌面发现性仍需单独确认
- 采样窗口的 crash buffer 未发现本应用崩溃记录。设备临时 UI XML 已逐次删除，最终确认本轮固定路径不存在，没有往 sdcard 推送测试文件
- 新服务器推送及通知点击仍等待用户从另一端发送消息。默认 Service 点击、首次授权、后台恢复、真实注册及 ACK、同格式真实更新备份和预测性返回均未完成，target 31 尚未整体通过，暂不提升 target 33

## STATUS-013：真实 QQ 推送及直接点击通过

- 用户从另一端发送真实新消息，本机 alpha.2 收到并发布 QQ 通知；不是历史消息重发或模拟通知。QQ 事件计数从 227 增至 228，最新事件时间更新，通知由框架创建并归属 QQ
- 系统记录点击 PendingIntent 为 startActivity，实际点击通知后 QQ ChatActivity 成为前台页面。当前采样未出现通知中转阻止、远程动画超时或致命异常标记，随后已退出至桌面
- 这条端到端流程证明当前已有注册及直接 Activity 配置下的新消息接收、通知发布和点击打开。没有读取截图，也未操作 QQ 账号、发送聊天消息或修改数据库
- 没有独立验证新注册交换、服务器 ACK 或点击回执。当前直接路径的成功不能证明默认 Service 点击正常；后台恢复、首次授权、同格式更新备份和预测性返回仍待验收，target 31 暂不整体确认或升至 33
- 本轮临时 UI XML 逐次清除，最终确认设备固定临时路径不存在。结果补入交接文档，业务代码未改动

## STATUS-014：直接形成 target 37 候选

- 用户要求加速，后续不再逐级发布和安装中间 target。直接以 target 37 候选集中验收，平台清单保留用于故障归因；此决定替代此前逐级装机及阶段确认要求
- 根构建目标设为 37，compile 36／min 23 和现有工具链不变。本轮所用公开 API 均已在编译平台可用，没有引入假平台声明。SDK 3.7.9、schema 17、包名和签名保持原样
- 新增 POST_NOTIFICATIONS 声明与真实系统授权流程。统一复用权限行呈现，返回页面时刷新授权状态；拒绝后系统不再弹窗时提供系统通知设置入口。已完成引导的用户缺少通知授权时也进入授权页
- 前台连接服务声明 specialUse 类型、对应权限及具体用途。共享推送不局限于跨设备文本消息同步，不以 remoteMessaging 冒充用途；声明没有绕过后台启动限制
- 引导完成后通过原控制入口再次启动服务，覆盖首次授权前启动未成功的恢复点。屏幕亮起动态接收器明确为 NOT_EXPORTED，该过滤器只接收系统 SCREEN_ON，原本也属于系统广播豁免范围
- 已有主要 Compose 页面接入 edge-to-edge 和 AndroidX 导航，当前源码未发现自定义旧 onBackPressed／KEYCODE_BACK 消费路径，不额外新造返回栈
- 默认 Service 通知点击仍保留待最终设备定位，没有预先添加兼容 Activity 壳。mipush_serviceIntent 在当前框架和模块源码没有读取方，不能作为 SDK 点击回执保证；已有无调用的前台绑定辅助方法未扩展
- 默认推送服务器及 HttpService 允许的统计主机使用公网，本轮不申请本地网络权限。自定义局域网服务器在 target 37 下需要额外授权，尚未适配或验收，不能宣称保持这类连接能力
- 官方 Android 17 清单包含静态 final 写入及 MessageQueue 反射限制。javap 确认当前注入的 isMIUI、sCachedIMEI 和 pingInterval 都是非 final 字段，不需为这三处引入绕过；依赖及最终 ART 仍待设备检查。没有关闭 TLS 或用假成功绕过系统变化
- normal versionCode 递增到构建定义的新值，vc105 保持 105。只生成 1.1.0-alpha.3 最终 SDK 候选，不生成中间 target 安装包
- normal Release 本地完整构建通过，用时 2 分 26 秒。实际签名 APK 的 min／target 为 23／37、versionCode 1003003002，长期证书、v2 签名、非调试标记、四种 ABI 和 ZIP 对齐检查通过，清单含通知权限及 specialUse 类型和用途
- 本地产物 13,892,785 字节，SHA-256 为 6bf19e534b37f9c3ec7e81efa2986966f4c7cc879ec4644b862bd89dee7c7294。它尚未发布或安装，不以本地构建替代 GitHub Release 装机入口。设备仍为 target 31 alpha.2
- 默认通知点击和自定义局域网服务器仍有明确待适配／验收项，不能把这个 SDK 37 候选称为已完成全部行为迁移；本条记录时尚未发起公开发布

## STATUS-015：alpha.3 发布与最终 SDK 装机准备

- 用户授权提交、推送并发布 v1.1.0-alpha.3，随后使用 GitHub Release normal 附件同签名覆盖安装，集中验收 target 37，不生成中间 target 包
- 版本化说明已准备，明确默认 Service 点击和局域网服务器限制。正式 Latest 保留 v1.0.1，master 不合入未验收代码
- target 31 的安装、真实推送与直接点击结果一起纳入交接，和 target 37 结果分别记录，不用旧结果冒充新行为验收
- 本条为发布前记录，CI、线上附件及实际设备结果完成后追加；卸载、清数据或签名切换不在授权范围内

## STATUS-016：alpha.3 发布及 target 37 基础运行通过

- [v1.1.0-alpha.3](https://github.com/huaxianyan/PixelMiPushFramework/releases/tag/v1.1.0-alpha.3) 已发布为 prerelease，标签固定在 566edf61c5b27c0ec1cded9b23e4f52336299f6c。分支 CI 38521880008 和发布 CI 38522870065 通过，正式 Latest 仍为 v1.0.1
- 两个线上 APK 及 LF 校验文件已下载复核，源码提交、版本、大小、哈希、SDK、四种 ABI、长期证书及 v2 签名匹配。正文 37 行，未覆盖旧标签或附件
- normal versionCode 1003003002，文件 13,892,750 字节，SHA-256 为 114b997131a47800372b5cae0257e33ef5c80a8b3cc5d223fb44adaaa3bb1304
- vc105 versionCode 105，文件 13,892,737 字节，SHA-256 为 afb53fe0f1a4a5ba49d9706ab44de7a06d2ba285c35c538603c822c64e5afff5。vc105 仅作产物复核，没有装机
- 本机直连设备现装 alpha.2 签名核对一致，使用线上 normal 附件覆盖成功。实际包版本 alpha.3、min／target 23／37。公开 Welcome 入口进入主页，首页已连接，已有应用及事件记录保留
- 系统服务记录确认 target 37，前台类型为 specialUse。真实高级设置关闭保活后退出前台，再开启后恢复 specialUse，原开启状态已恢复。通知运行时权限当前已授予，没有把这个状态作为首次请求或拒绝恢复的验收
- SDK 37 下心跳精确闹钟继续排期，许可来源为 allow-listed。当前设备已有省电及悬浮窗授权，本轮结果不代表无授权环境的后台恢复能力
- 真实事件列表进入 QQ 详情，再按返回键回到事件列表，证明 API 37 下当前 AndroidX 返回分发可用；三键导航未验证手势取消或视觉进度，也未读取截图
- 当前采样没有本应用 crash buffer 记录。固定设备 UI 临时文件逐次删除并确认不存在，未卸载、清数据或重启
- 内置模拟通知使用 Activity PendingIntent，不能用它冒充默认 Service 路径证据，因此没有执行该替代测试。SDK 37 的新消息及点击仍等待用户从另一端触发，不能沿用 STATUS-013 的 target 31 结果
- 默认 Service 点击、独立注册／ACK、通知拒绝后恢复、首次省电授权、后台回收恢复、同格式更新备份、预测性返回和局域网自定义服务器仍有待验收／适配项。整体验收尚未通过，master 不合入

## STATUS-017：target 37 真实 QQ 接收与点击通过

- alpha.3／target 37 下，用户从另一端发送新的 QQ 消息，框架收到并发布归属 QQ 的通知。事件计数从 228 增至 229，系统通知时间与本轮发送相符，不是历史重发或模拟通知
- 当前通知点击 PendingIntent 为 startActivity。自动化未读到可识别的 QQ 标签，停止自动点击，由用户直接点击真实通知；用户确认如旧版一样顺利进入对应聊天
- 随后系统前台记录确认 QQ ChatActivity，采样未出现通知中转阻止、远程动画超时或致命异常标记。设备固定 UI 临时路径已确认不存在，未操作 QQ 账号、发送消息或修改数据库
- 这条 target 37 真实端到端流程已通过，不能再把核心推送与直接点击列为尚未验证；target 31 的旧结果未替代本轮新证据
- 与 STATUS-016 的启动、连接、specialUse 前台开关及返回键结果合并，当前 Android 17 主设备的已有注册、当前授权及直接 Activity 配置下核心流程可用。默认 Service 点击、独立注册／ACK 及其他边界仍按原记录保留，不据此宣布所有客户端和无授权环境均通过

## STATUS-018：普通默认通知入口迁移准备

- 后续工作先收敛普通推送默认点击。将现有 MyPushMessageHandler 从 IntentService 改为通知点击 Activity，工厂使用 Activity PendingIntent，移除旧 Service 注册，没有保留并行普通默认实现
- 入口等待实际窗口焦点后在工作线程处理，复用原应用解冻、前台检测与打开、原 payload 转发和成功后通知清理顺序。耗时处理离开主线程，结束后关闭入口。透明非浮动窗口只承接点击分发，不新增设置页或另一套返回栈
- 原直接 Activity 配置和 URL 路径保持原样。删除当前框架与模块源码均无读取方的 mipush_serviceIntent，原 payload、FROM_NOTIFICATION、通知标识和通知类别仍保留
- 删除无调用的前台绑定辅助方法及其依赖导入，SDK 目标处理器类名复用已有定义，不复制协议标识
- SDK 商业消息仍走原 PushMessageHandler。实际字节码确认 addJob 是异步调度且额外启动 Service，不把它伪装为同步回执完成；该分支与普通默认入口属于不同协议路径，尚未宣称完成迁移
- 升级前普通默认通知持有的旧 Service PendingIntent 不会自动变成 Activity，需重新发布通知再验收。没有新增兼容壳或双注册来掩盖此影响；已有 QQ 直通通知不属于该旧默认路径
- 保持 target 37，normal 版本码按构建定义递增，SDK、schema、签名和同步配置不变。定向 normal Debug Kotlin／Java 编译及清单合并通过，用时 34 秒，不为仅应用内改动重复全量构建
- 合并清单确认普通默认处理组件只注册为非导出 Activity，没有旧 Service 注册；SDK 商业处理器仍存在。静态结果不能证明窗口焦点、点击打开、第三方 SDK 回调或通知清理已通过
- 本阶段仅形成 1.1.0-alpha.4 源码候选，未生成发布 APK、提交、公开发布或安装；设备仍运行已验收核心流程的 alpha.3。随后按 STATUS-019 将系统栏与预测性返回一并集中验收

## STATUS-019：Android 现代界面能力合并验收

- 用户要求将 edge-to-edge、预测性返回与普通默认点击放入同一个 target 37 候选集中测试，不拆中间装机版本。本轮允许临时将设备从三键切为手势导航，测试结束或异常退出时必须恢复原导航设置；图片读取仍需另行授权
- 主要 Compose Activity 已调用 enableEdgeToEdge，Material You 默认开启。应用支持可调整窗口，现有内容最大宽度保持，未为单台手机虚构双栏场景，也未引入预发布 Material API
- Navigation Compose 2.9.8 已提供手势进度寻址与取消恢复，当前 NavHost 共享不透明分层滑动。按官方接入说明复用现有导航，不叠加另一套 PredictiveBackHandler 或返回状态机
- 找到真实遗漏：应用事件详情页仍使用零 TopAppBar insets，并注释称外层负责留白，但主导航改为全窗口后已无外层 Scaffold。改由目的地自己的 Scaffold 和 TopAppBar 负责系统栏边界
- 主页面、事件详情与独立近期事件页消费 Scaffold 留白后处理 IME；搜索入口 Activity 明确 adjustResize，避免键盘与系统栏重复留白。共享长页面将内容留白移入滚动范围，维持系统栏下可滚动的视口
- 合并候选的定向 normal Debug Kotlin／Java 编译和清单合并通过，用时 17 秒；alpha.4 发布说明门禁通过。本阶段仍未发布或安装，未改变设备导航模式，不用 alpha.3 冒充新布局验收
- 验收范围为系统栏与键盘、深浅色、手势进度／取消／提交、弹窗优先返回、跨 Activity 与返回桌面；与默认点击和真实推送一起记录。代码接入不替代真机视觉结果，手势测试结束恢复三键导航后再记录结论
- 官方依据：[Compose 窗口 insets](https://developer.android.com/develop/ui/compose/system/insets)、[预测性返回接入](https://developer.android.com/develop/ui/compose/system/predictive-back-setup)，本轮核对

## STATUS-020：区分底栏栏目与层级页面转场

- 用户在当前版本报告：底栏切换看起来像进入次级页面，整个页面侧滑不符合导航层级。暂停 alpha.4 发布及装机，先纠正这一真实界面问题
- 原实现将每个底栏栏目作为包含顶栏、底栏的完整目的地，并统一套用层级滑动。现在四个栏目共用一个持久 Scaffold，只切换中间内容；栏目切换不做前进／后退侧滑
- 外层导航只管理栏目容器与事件详情，保持完整窗口的分层滑动和预测性返回；内层导航只管理同级栏目及其状态恢复。两者职责分开，不用切换顶底栏或改变视口高度来模拟层级返回
- 保留原 saveState／restoreState 与单实例栏目选择、返回首页语义，预览复用新的共同外壳。系统栏及键盘留白仍由此前修复负责，没有回退为覆盖全部页面的条件 Scaffold
- 定向 normal Debug Kotlin 编译通过，用时 9 秒；alpha.4 发布说明和中文文案检查通过。未重复全量构建，也未为静态结构添加假端到端测试
- 本阶段只修改源码与说明，仍需 alpha.4 最终附件确认栏目切换、状态恢复、详情返回和手势取消／提交。当前设备未更新，未宣称视觉或运行验收通过

## STATUS-021：alpha.4 已发布，装机因设备断开暂停

- 用户开始合并候选验收。源码提交 f612bde02faa8668f83780022676de2155aa73b4，分支 CI 38536263632 和发布 CI 38537412510 通过，注解标签 v1.1.0-alpha.4 指向该提交
- [alpha.4 预发布](https://github.com/huaxianyan/PixelMiPushFramework/releases/tag/v1.1.0-alpha.4) 已发布，标题仅标签，正文 33 行。正式 Latest 保持 v1.0.1，master 未合入
- 两个 GitHub APK 附件及同名 LF 校验文件下载并核对：min 23／target 37、长期证书、v2 签名、非调试、4 ABI 和实际版本信息通过。normal 版本码 1003003003，vc105 为 105
- normal 附件 13,892,938 字节，SHA-256 f539ee9d8ffa952b54a87adc24d78123fa7c6d1e791803081b4d58f2fcc57015；vc105 附件 13,892,925 字节，SHA-256 acd0d4f91f9bd360563290198c058d3116446faeede07f0ec1d192d20e1bdaeb
- 本机 ADB 5037 起初可读设备，确认 alpha.3／target 37，取回现用 APK 并核对同签名；原导航记录为三键及其已启用 overlay。实际执行覆盖安装时设备已不在列表，安装命令返回 device not found，随后默认 ADB 列表确认为空，暂停装机
- 未执行导航切换、配置修改、数据清理或卸载。截图读取与临时 QQ 直通规则调整仍待单独确认；临时导航授权已收到，后续结束或异常退出仍须恢复原设置
- CI watcher 的代理 HTTP 连接 EOF 不代表构建失败，改为查询同一个 run 后确认成功，没有重跑 CI。附件下载改用受限 curl 后成功，没有用本地候选代替 GitHub 附件
- 下一步恢复本机设备连接后，从安装步骤继续，不重复发布或附件验证。仍需真实栏目切换、系统栏／键盘、手势进度与取消／提交、默认点击和新消息验收；没有将静态结果记为运行通过
