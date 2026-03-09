# ZZZ成就（Android）

离线本地的《绝区零》成就追踪 App。

- 应用名：`ZZZ成就`
- 包名：`com.aitidi.zzzachievementtracker`
- 当前形态：本地数据库 + 本地 JSON 导入导出 + GitHub Actions 自动构建

---

## 功能

- 成就列表追踪（勾选完成 / 未完成）
- 版本筛选、分类筛选、关键词搜索
- 排序（仅按版本：新→旧 / 旧→新）
- 统计页（总完成率 + 分版本进度）
- 设置页：
  - 紧凑模式切换
  - 版本模块启用/隐藏
  - 导入 / 导出进度 JSON
  - 重置进度（5 次点击确认，带进度提示）

> 默认深色主题；主题切换入口已隐藏。

---

## 项目结构

```text
app/                  Android 应用代码
  src/main/assets/    应用内置成就数据
  src/main/java/      Compose UI + ViewModel + Repo + DB
  src/main/res/       图标、字符串、资源

data/                 成就源数据与示例
  versions/           分版本成就包（v1.0...v2.6）
  index.json          版本索引
  achievements_master.json

docs/                 PRD / 开发记录 / 发版检查清单
ui-prototype/         Web 原型页（视觉参考）
.github/workflows/    CI 与 Release 工作流
```

---

## 本地开发

### 环境要求

- JDK 17
- Android SDK 35（含 build-tools 35.0.0）
- Gradle 8.x（CI 当前用 8.10.2）
- Android Studio（推荐）

### 构建 Debug 包

```bash
gradle :app:assembleDebug
```

生成路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 数据说明

- 成就数据按版本拆分在 `data/versions/*.json`
- 用户进度存本地数据库（不写回源数据）
- 导出/导入格式示例见：`data/user_progress.sample.json`

---

## CI / Release

### CI（Debug）

工作流：`.github/workflows/ci.yml`

- PR / main push 自动构建 Debug APK
- Artifact 名称：`zzz-achievement-tracker-debug-apk`

### Release（签名保护）

工作流：`.github/workflows/release-build.yml`

- 仅接受**已签名**的 release APK
- 会自动执行签名校验（`apksigner verify`）
- 若只产出 `app-release-unsigned.apk` 会直接失败（这是预期保护）

需要配置 GitHub Actions Secrets：

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

产物命名：

```text
zzz-achievement-tracker-v<versionName>+<versionCode>-release.apk
```

例如：`zzz-achievement-tracker-v1.0.0+100-release.apk`

---

## 常见问题

### 1) APK 无法覆盖安装

通常是签名不一致（debug/release 或旧签名不同）。

处理：先卸载旧版，再安装新版。

### 2) Release Build 失败但 CI 成功

多半是未配置签名 secrets，导致 workflow 拒绝 unsigned 包。

### 3) 为什么文件名有 `+100`

`100` 是 `versionCode`（内部版本号，系统用来比较升级）。

---

## License

内部项目（未单独声明开源协议）。
