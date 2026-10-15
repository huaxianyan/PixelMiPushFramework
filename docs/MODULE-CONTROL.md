# 模块状态与设置

## 使用范围

首页新增增强模块状态。推送服务增强、系统通知增强和 QQ 样式增强分别显示，不以模块已安装代替运行就绪。

「设置 → 模块设置」提供三个独立选项：QQ 通知角标、放大会话头像和简化汇总通知。首次使用均开启，基础推送增强保持原样。保存成功后显示模块返回的选择，失败时保留原选择并提示重试。

修改用于后续通知呈现，已有通知需重新发布。配置未同步时，页面单独提示。模块暂时不可访问或版本不支持时，设置开关不可操作，可重新检查或更新模块。

## 联合构建

接口只定义在 PixelMiPush 的 `module-api` 项目中。此仓库引用该项目，不维护另一份协议或设置存储。

本地将 PixelMiPush 与本仓库放在同级目录，也可用 Gradle 参数指定其位置：

```sh
./gradlew :push:compileNormalDebugJavaWithJavac \
  -PpixelMiPushDir=/path/to/PixelMiPush -PversionName=module-controls-check
```

CI 通过共用 Action 按 `module-api.ref` 检出固定模块提交。更新接口时，先在模块仓库提交代码，更新此文件中的完整提交号，再提交框架。先推送模块提交，确保 CI 能检出，再推送框架。

## 验证边界

本轮模块 Release 构建、最终源码的定向编译，以及框架 Java、Kotlin 和 Android 测试源码编译通过。没有执行设备测试或查询线上 CI，没有发布、安装或重启。

`ModuleSettingsTest` 保留一条真实页面流程：进入模块设置，分别切换三个选项，核对其他选项保留及宿主确认同步，最后恢复原设置。执行需要另行授权的测试环境，且两个候选和全部作用域均已就绪：

```sh
./gradlew :push:connectedNormalDebugAndroidTest \
  -PversionName=module-controls-check \
  -Pandroid.testInstrumentationRunnerArguments.class=top.trumeet.mipushframework.main.ModuleSettingsTest
```

这是测试环境命令，不作为在当前主设备覆盖安装的授权。正式装机仍使用分别发布的 Release 附件。

接口状态不证明真实 QQ 消息送达。后续仍需真机确认页面观感、三个开关的独立效果，以及进程重启和设备重启后的配置恢复。先检查通知归属及基础推送，再对照头像、角标和汇总内容。
