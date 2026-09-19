# 构建现代化基线

## 当前范围

构建链路与界面改造均已完成。本仓库自 1.0.0 起启用独立版本线，不再沿用上游的 0.3.x 编号；`targetSdk` 仍留在 30，用于隔离系统行为变化。

| 项目 | 当前配置 |
|---|---|
| JDK | 17 |
| Gradle | 8.11.1，使用仓库 Wrapper |
| Android Gradle Plugin | 8.9.1 |
| compileSdk / targetSdk / minSdk | 36 / 30 / 23 |
| Kotlin / Compose 编译插件 | 2.1.20 / 2.1.20 |
| AspectJ 编译器与运行时 | 1.9.7 |
| GreenDAO 插件 / 运行时 / schema | 3.3.1 / 3.3.0 / 17 |
| 小米 SDK | 仓库原有 `miuipushsdkshared_3_7_9.jar` |
| Compose UI / Material 3 | 1.11.4 / 1.4.0 |
| navigation-compose / activity-compose | 2.9.8 / 1.13.0 |
| lifecycle-runtime-ktx | 2.10.0 |

界面依赖已随本批升级，选版约束与原因见「界面依赖版本」一节。

安装共享 Android SDK 的 Platform 36，设置 `JAVA_HOME` 和 `ANDROID_HOME` 后使用仓库 Wrapper。Windows 使用 `gradlew.bat`。

## 定向验证

应用源码编译，包括 Kotlin、Java 和生成的 DAO：

```sh
./gradlew :push:compileNormalDebugJavaWithJavac -PversionName=build-check
```

库产物：

```sh
./gradlew :mipush_hook:assemble :common:assembleDebug :condom:assembleDebug
```

使用 Python 3 和构建 JDK 的 `javap` 检查真实 AAR：

```sh
python scripts/verify_hook_artifact.py \
  mipush_hook/build/outputs/aar/mipush_hook-debug.aar \
  mipush_hook/build/outputs/aar/mipush_hook-release.aar
```

检查包括 SDK 类完整性、重复打包、现有关键边界的类字节变化、9 个真实切面的 `aspectOf()` 方法，以及 SDK 类内的 15 个关键处理器调用指令。它不执行 Android 代码，不能证明切面顺序、ART 校验或实际推送行为。

## APK 验证

生成 normal Release APK：

```sh
./gradlew :push:assembleNormalRelease -PversionName=build-check
```

本机恢复过正式密钥时产出已签名 APK（`xmsf-vbuild-check-normal-release.apk`），没有可用密钥时退回未签名产物（`xmsf-vbuild-check-normal-release-unsigned.apk`）。两条路径都保留是有意的：CI 不带密钥，产物必须停在未签名状态。

`scripts/verify_apk_artifact.py` 只接受未签名产物，遇到 JAR 签名或 APK 签名块会直接报错。安装 Android Build Tools 36.0.0 后：

```sh
python scripts/verify_apk_artifact.py \
  push/build/outputs/apk/normal/release/xmsf-vbuild-check-normal-release-unsigned.apk \
  --build-tools "$ANDROID_HOME/build-tools/36.0.0"
```

脚本检查包名和版本码、minSdk／targetSdk、Release 调试标记、ZIP 对齐、未签名状态、全部 DEX 的校验和与重复类、SDK 类完整性、清单组件和关键方法代码。DEX 读取逻辑统一在 `scripts/dex_inventory.py`。

签名产物另行用 apksigner 核对：

```sh
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs \
  push/build/outputs/apk/normal/release/xmsf-vbuild-check-normal-release.apk
```

两者都不是 ART 校验或真实推送测试，也不证明 Java 方法调用都能在设备上完成链接。

## 构建边界

### SDK 织入

`mipush_hook/build.gradle.kts` 使用 AGP 的 `ScopedArtifacts` 接口，对项目类与原 SDK 进行织入。外部依赖只作为类路径，不参与织入。

- Debug 和 Release 使用同一个任务实现
- 不查找内部任务名称，不原地修改 javac 输出
- AJC 使用 `variant.compileClasspath` 提供的 class JAR，而不是原始 AAR
- SDK 仍以 `compileOnly` 参与编译，由织入产物提供实际类
- AspectJ 运行时作为 `api` 依赖传递，因为处理器接口公开了其类型
- `push` 仍不启用织入，与迁移前的有效配置一致

AAR 本身不内嵌全部依赖，不能将单个 AAR 视为独立可运行的 SDK 或安装包。

### Android 编译占位类

`common` 和 `condom` 仍使用原有的 Android 隐藏 API 编译声明，没有添加新的 stub。

`buildSrc` 中的 `StripAndroidStubs` 通过公开产物接口剔除编译占位类，取代按旧版 AGP 目录删除文件的任务。

`android.app.AppOpsManagerExtender` 是 `AppOpsKit` 正在使用的辅助实现，不是编译占位类，必须保留。不能机械删除全部 `android.*` 类。

### 资源引用

保持 AGP 8 的非传递 `R` 类行为。跨模块资源显式引用资源所属模块，不复制资源或关闭新行为来绕过错误。

## 界面依赖版本

`push/build.gradle` 的 Compose、Material 3 与 AndroidX 界面依赖升到当前稳定版，同时 `minSdk` 从 21 提到 23。

选版受两条硬约束，不是取最新即可：

- **compileSdk 上限。** `compose.ui` 1.12.x、`navigation-compose` 2.10.x、`lifecycle` 2.11.x 的 AAR 元数据要求 `compileSdk` 37 与 AGP 9.1.0，超出本工程的 36 与 8.9.1，`checkAarMetadata` 会直接失败。可用最高组合为 Compose UI 1.11.4、Material 3 1.4.0、navigation-compose 2.9.8、lifecycle 2.10.0。
- **Kotlin 编译器版本。** Compose 1.11.4 依赖的 `kotlin-stdlib` 是 2.1.20，与本工程编译器一致；再往上会引入高于编译器的 stdlib。

`minSdk` 提到 23 由 `ui-tooling-data` 要求，也是 AndroidX 自 2025 年起统一抬高的下限。`ui-tooling` 改为 `debugImplementation`，它只服务 IDE 预览，不进 Release 产物；`ui-tooling-preview` 保持 `implementation`，源码使用 `@Preview` 注解。

`material-icons-core` 固定 1.7.8：material3 不再传递该依赖，而 `Icons.*` 仍被 `SearchBar` 与 `RequestPermissionPage` 使用。该库已停止更新。

`org.jetbrains:markdown` 保持 0.7.3。0.7.4 起改为 KMP 发布，根坐标只含公共元数据，JVM 类在 `markdown-jvm` 下，直接升级会让 `org.intellij.markdown` 无法解析。

Compose 侧的动态取色本就已接入：`top.trumeet.ui.theme.Theme` 在 Android 12+ 用 `dynamicLightColorScheme`／`dynamicDarkColorScheme`，各页面的 `Main`、`SettingsApp` 内部都有 `Theme {}` 包裹，本批未改动这部分。

## 正式签名

`push/build.gradle` 为 release 构建类型读取框架专属密钥，密钥不进入仓库：

| 来源 | 内容 |
|---|---|
| 本机 `%USERPROFILE%\.gradle\mipushframework-signing.properties` | `storeFile`／`storePassword`／`keyAlias`／`keyPassword` |
| CI 环境变量 | `MIPUSH_KEYSTORE_FILE`／`MIPUSH_KEYSTORE_PASSWORD`／`MIPUSH_KEY_ALIAS`／`MIPUSH_KEY_PASSWORD` |

环境变量优先于属性文件。四项缺任意一项时不启用签名，release 停在未签名状态，本地构建和验证工作流都不会因此失败；四个值齐备但文件不存在则直接报错，避免静默改用别的产物。

启用签名时开启 v1、v2、v3 三种方案，覆盖 minSdk 23 到目标系统。

密钥为 PKCS12、RSA 4096、SHA256withRSA，别名 `mipushframework`，主体 `CN=MiPushFramework, OU=Android Signing, O=NeKo7inA`，有效期 36500 天。证书 SHA-256 与恢复步骤见 NAS 备份目录 `\\192.168.7.216\homes\NeKo7inA\dev\Android Signing\MiPushFramework\`。

换签名后无法覆盖安装设备上现有的 `com.xiaomi.xmsf`，卸载会清空事件库与注册状态。这一步需要单独确认，不在本批范围。

## 已知限制

- `MethodHooker.logCheckServices` 的原切点将 `context` 解析成类型，未命中。本批未修复这个既有日志切点，也未屏蔽告警
- `com.nihility.XMPushUtils` 属于应用模块，库织入时不可见。没有为了消除告警开启应用层织入
- Jetifier 为 AspectJ 运行时重命名后，AJC 可能报告无法识别 `aspectjrt.jar`，解析后的类路径实际包含对应版本。保留告警，不以日志无告警作为验收条件
- 旧 API、Compose 和 SDK XML 版本告警尚未全部处理
- normal Release 已完成 D8／打包及静态检查，签名产物已用 apksigner 核对证书与签名方案；ART、设备运行和真实推送仍未验证
- D8 转换仍出现旧 SDK 缺少 StackMap 表的告警，未屏蔽，也未用 `-noverify` 绕过 APK 构建
- `targetSdk` 暂留 30，用于隔离构建变化与 Android 系统行为变化

## 版本号

对外版本号（`versionName`）与 `versionCode` 是两件事，不要混淆。

- `versionName` 由标签推导：`git describe --tags --dirty --exclude 'v*-*'` 取最近的标签，`v1.0.0` 得到 `1.0.0`。本地没有标签时必须用 `-PversionName=` 覆盖，否则兜底成 `VersionNameError`。值里含 `-` 会被 `increaseVersionForPreRelease()` 把第三段加一，正式标签不要带后缀。
- 工作区不干净时 `--dirty` 会给标签加后缀，同样触发上面那个加一。打标签前先确认 `git status` 干净。
- `versionCode` 固定为 `1003003000`（vc105 变体为 105）。它是给接入方 SDK 看的伪装版本号，SDK 要求不低于 105，与对外版本号无关，不随版本号递增。

每个标签对应一份 `docs/release-notes/<tag>.md`，发布工作流优先使用它，找不到才退回 GitHub 自动生成的说明。

## CI 与发布

`test_ci.yml` 不带任何签名密钥，构建未签名的 normal Release APK 和 hook AAR，检查最终产物，只上传 JSON 报告，不上传 SDK 二进制或 APK，也不创建 Release。GitHub CI 尚未实跑。

`release.yml` 只在推送 `v*` 标签或手动指定标签时运行：解码 `MIPUSH_KEYSTORE_BASE64`，构建 normal 与 vc105 两个签名 APK，用 apksigner 核对证书 SHA-256 与 v2 方案，通过后上传工作流产物并创建 GitHub Release，发布说明取自 `docs/release-notes/<tag>.md`。证书不匹配或产物缺失时直接失败，不发布。

Debug 使用 Android 插件的标准开发签名配置。历史 `.yuuta.jks`、环境变量和 `local.properties` 签名读取逻辑已移除，Release 不复用 Debug 密钥，也不复用 Pixel MiPush 模块密钥。

没有 Git 标签的本地工作树可显式传入 `-PversionName=build-check`，它仅是验证版本标识。正式发布走标签：推 `v1.0.0` 得到版本号 `1.0.0`，并触发发布工作流。
