# 构建现代化基线

## 当前范围

这一批只迁移构建链路，不代表 Android 16 运行适配已经完成。

| 项目 | 当前配置 |
|---|---|
| JDK | 17 |
| Gradle | 8.11.1，使用仓库 Wrapper |
| Android Gradle Plugin | 8.9.1 |
| compileSdk / targetSdk / minSdk | 36 / 30 / 21 |
| Kotlin / Compose 编译插件 | 2.1.20 / 2.1.20 |
| AspectJ 编译器与运行时 | 1.9.7 |
| GreenDAO 插件 / 运行时 / schema | 3.3.1 / 3.3.0 / 17 |
| 小米 SDK | 仓库原有 `miuipushsdkshared_3_7_9.jar` |

Compose UI 和 Material 3 库暂时保持原版本。此次升级的是编译插件，不捆绑界面依赖升级。

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

生成未签名的 normal Release APK：

```sh
./gradlew :push:assembleNormalRelease -PversionName=build-check
```

安装 Android Build Tools 36.0.0 后，检查最终 APK：

```sh
python scripts/verify_apk_artifact.py \
  push/build/outputs/apk/normal/release/xmsf-vbuild-check-normal-release-unsigned.apk \
  --build-tools "$ANDROID_HOME/build-tools/36.0.0"
```

脚本检查包名和版本码、minSdk／targetSdk、Release 调试标记、ZIP 对齐、未签名状态、全部 DEX 的校验和与重复类、SDK 类完整性、清单组件和关键方法代码。DEX 读取逻辑统一在 `scripts/dex_inventory.py`。

这不是 ART 校验或真实推送测试，也不证明 Java 方法调用都能在设备上完成链接。

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

## 已知限制

- `MethodHooker.logCheckServices` 的原切点将 `context` 解析成类型，未命中。本批未修复这个既有日志切点，也未屏蔽告警
- `com.nihility.XMPushUtils` 属于应用模块，库织入时不可见。没有为了消除告警开启应用层织入
- Jetifier 为 AspectJ 运行时重命名后，AJC 可能报告无法识别 `aspectjrt.jar`，解析后的类路径实际包含对应版本。保留告警，不以日志无告警作为验收条件
- 旧 API、Compose 和 SDK XML 版本告警尚未全部处理
- normal Release 未签名 APK 已完成 D8／打包及静态检查，但未签名产物不用于安装；正式签名、ART、设备运行和真实推送仍未验证
- D8 转换仍出现旧 SDK 缺少 StackMap 表的告警，未屏蔽，也未用 `-noverify` 绕过 APK 构建
- `targetSdk` 暂留 30，用于隔离构建变化与 Android 系统行为变化

## CI 与发布

`test_ci.yml` 构建未签名的 normal Release APK 和 hook AAR，检查最终产物，只上传 JSON 报告，不上传 SDK 二进制或 APK，也不创建 Release。GitHub CI 尚未实跑。

Debug 使用 Android 插件的标准开发签名配置，Release 保持未签名。历史 `.yuuta.jks`、环境变量和 `local.properties` 签名读取逻辑已移除，Release 不再沿用 Debug 密钥。本批未构建 Debug APK，未生成或切换正式密钥。

正式签名应在单独确认的步骤中对未签名 APK 进行，使用框架专属密钥，不复用 Pixel MiPush 模块密钥。正式签名和发布流程尚未建立。没有 Git 标签的本地工作树可显式传入 `-PversionName=build-check`，它仅是验证版本标识。

公开发布前仍须完成来源与许可核验、独立签名配置和设备验收。
