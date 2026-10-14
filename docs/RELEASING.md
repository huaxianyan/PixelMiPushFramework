# 发布约定

## 版本与说明

发布标签为 `v<版本>`，APK 的 versionName 为去掉 `v` 的标签值。框架的 normal／vc105 versionCode 沿用构建配置，不与产品版本号混用。

每个新标签必须有 `docs/release-notes/<标签>.md`。说明不以一级标题开头，按实际内容分为「新功能」和「问题修复」，每条用加粗小标题。内部重构留在详细文档，不能为了通过门禁归类为修复。

说明必须链接同一标签下真实存在的详细文档。原始说明不写「构建信息」，工作流从实际签名 APK 提取并追加。最终正文最多 40 行。

模板：

```markdown
这版新增……，修复……。

## 新功能

- **功能名**：说明用户获得的能力

## 问题修复

- **问题名**：说明受影响场景和修复结果

完整记录见 [详细说明](https://github.com/huaxianyan/PixelMiPushFramework/blob/v<版本>/docs/<文档>.md)。
```

只包含其中一类改动时，删掉另一节。历史 Release 和附件不自动回填或重命名。

## 产物

- `PixelMiPushFramework-normal-universal-<版本>.apk`
- `PixelMiPushFramework-vc105-universal-<版本>.apk`
- 每个 APK 附带同名 `.sha256`，使用 `sha256sum -c` 可读的 LF 文本格式

当前 APK 没有原生库，使用 `universal`。新增原生库时重新确认架构命名，发布脚本会暂停。

构建信息包含标签、源码提交、每个变体的包名、版本号、架构、min／target SDK、签名证书和 APK SHA-256。包名固定为 `com.xiaomi.xmsf`，签名沿用框架独立长期密钥。

## 流程

1. 准备版本化发布说明，运行 `python scripts/verify_release_notes.py --tag v<版本>`
2. 正式版合入已验收代码，确认普通 CI 通过。审计预发布可从主题分支发布，保留未验收声明，不合入 master
3. 创建并推送注解标签，由 `release.yml` 构建和发布
4. 手动触发只允许构建已经存在的标签
5. 复核附件名称、哈希、实际包信息和签名，再单独安排装机验收

预发布标签使用 `v<版本>-<阶段>`，Release 标记为 prerelease，不替代正式 Latest。普通构建 CI 只在分支推送及 PR 运行，标签推送仅触发发布构建，避免重复执行同一份未签名验证。

发布任务先检查说明，再构建签名 APK。`prepare_release.py` 核对检出提交与标签一致，验证长期证书和 v2 签名，提取构建信息并命名附件。标题只使用标签，不自动生成说明，不覆盖已有 Release 附件。

Obtainium 的 README 入口只筛选新命名规则下的 normal 正式版。首次换签安装仍需数据备份与单独确认，导入更新源不替代迁移流程。
