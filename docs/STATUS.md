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
