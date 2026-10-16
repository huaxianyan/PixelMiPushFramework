# 模块状态与设置

## 使用范围

首页显示推送服务增强、系统通知增强和个性化设置汇总。个性化设置按定制项目计数，QQ 通知定制算 1 项，三个开关不分别计数。项目列表同时生成设置入口和状态汇总，不随项目增加而增加首页状态行。

汇总显示「1 项正常」「1 项未就绪」，混合时显示「2 项正常 · 1 项未就绪」。尚未取得模块状态时显示待检测。状态依据相关宿主的加载和配置同步结果，主动关闭选项不计为故障，也不以模块已安装代替运行就绪。

「设置 → 模块设置」下方是个性化设置分组，目前只有 QQ 通知定制入口。点击进入后提供 QQ 通知角标、放大会话头像和简化汇总通知三个选项，仍沿用同一套页面转场。首次使用均开启，基础推送增强保持原样。

保存成功后显示模块返回的选择，失败时保留原选择并提示重试。修改用于后续通知呈现，已有通知需重新发布。模块状态不可读取时，QQ 页面先提供检查入口，取得配置后再显示实际开关选择。

## 联合构建

接口只定义在 PixelMiPush 的 `module-api` 项目中。此仓库引用该项目，不维护另一份协议或设置存储。

本地将 PixelMiPush 与本仓库放在同级目录，也可用 Gradle 参数指定其位置：

```sh
./gradlew :push:compileNormalDebugJavaWithJavac \
  -PpixelMiPushDir=/path/to/PixelMiPush -PversionName=module-controls-check
```

CI 通过共用 Action 按 `module-api.ref` 检出固定模块提交。更新接口时，先在模块仓库提交代码，更新此文件中的完整提交号，再提交框架。先推送模块提交，确保 CI 能检出，再推送框架。

## 验证边界

既有构建、CI 和装机结果见 [交接记录](STATUS.md)。个性化项目汇总单测已覆盖全部正常、全部未就绪和混合状态。新导航与汇总候选已编译，尚未装机验证。

`ModuleSettingsTest` 保留一条真实页面流程：进入模块设置，再打开 QQ 通知定制，分别切换三个选项。测试核对其他选择保留及宿主确认同步，返回后仍按一个项目汇总，最后恢复原设置。执行需要另行授权的测试环境，且两个候选和全部作用域均已就绪：

```sh
./gradlew :push:connectedNormalDebugAndroidTest \
  -PversionName=module-controls-check \
  -Pandroid.testInstrumentationRunnerArguments.class=top.trumeet.mipushframework.main.ModuleSettingsTest
```

这是测试环境命令，不作为在当前主设备覆盖安装的授权。正式装机仍使用分别发布的 Release 附件，本次本地签名覆盖测试例外见 STATUS-043。该例外不包含合并或公开发布授权。

接口状态不证明真实 QQ 消息送达。后续仍需真机确认页面观感、三个开关的独立效果，以及进程重启和设备重启后的配置恢复。先检查通知归属及基础推送，再对照头像、角标和汇总内容。
