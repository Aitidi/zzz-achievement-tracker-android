# Release 校验清单

> 适用于 zzz-achievement-tracker-android 每次正式发布前。

## 1) 版本信息
- [ ] `app/build.gradle.kts` 的 `versionCode` 已递增
- [ ] `app/build.gradle.kts` 的 `versionName` 已更新
- [ ] 版本变更已记录在发布说明

## 2) 签名与构建
- [ ] 本地 `keystore.properties` 已配置（仅本地，不提交仓库）
- [ ] GitHub Secrets 已配置：
  - [ ] `ANDROID_KEYSTORE_BASE64`
  - [ ] `ANDROID_KEYSTORE_PASSWORD`
  - [ ] `ANDROID_KEY_ALIAS`
  - [ ] `ANDROID_KEY_PASSWORD`
- [ ] `Release Build` 工作流构建成功
- [ ] 产物命名符合规范：`zzz-achievement-tracker-v<versionName>+<versionCode>-release.apk`

## 3) 功能回归
- [ ] 启动应用正常，图标与名称正确
- [ ] 成就列表加载正常
- [ ] 搜索/筛选可用
- [ ] 勾选与取消勾选状态正确保存
- [ ] 导入/导出可用

## 4) 发布说明
- [ ] 采用 `.github/release-template.md`
- [ ] 写明本次新增、修复、已知问题
- [ ] 提供升级建议（如需备份）
