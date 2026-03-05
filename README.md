# ZZZ Achievement Tracker (Android)

离线本地成就追踪器（绝区零）。

## 目标
- 手动勾选已完成成就
- 分类/版本筛选与搜索
- 本地 JSON 导入导出
- 新版本成就库增量更新（保留用户进度）

## 项目结构
- pp/ Android 应用代码
- data/ 成就主数据与示例
- docs/ PRD 与设计文档
- scripts/ 数据转换与校验脚本

## 成就数据组织（版本化）
- data/index.json：版本索引与统计
- data/versions/vX.Y.json：按游戏版本拆分的成就包
- pp/src/main/assets/achievements_master.json：App 启动种子（聚合）

说明：版本包中的 进度 默认置为 alse，用户完成状态由 App 本地数据库维护。
