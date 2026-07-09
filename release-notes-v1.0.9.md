# v1.0.9 Release Notes

## 修复

### 1. HTML 视频源分类行不显示（核心修复）
- **问题**: 配置了 HTML 源（如 icaiqi.com）后，首页不显示该源的分类行，用户只看到"加载失败"
- **根因**: `renderSiteCategoryRows()` 使用 `lifecycleScope.launch { }` 异步添加 row，但 Leanback `BrowseSupportFragment` 在 `loadHome()` 同步阶段完成后不再重建 header 列表，导致异步 add 的 row 虽然 `rowsAdapter.size()` 增加了但 header 不显示
- **修复**: 将 `addCategoryRowForSite()` 改为同步调用，先添加占位 row（header 可见），再异步拉取分类卡片填入
- **验证**: 模拟器确认首页显示 4 个 row header（快捷入口 / 视频源 / 热门影视 / HTML 网站），HTML 网站行显示 10 个分类

### 2. SourceRepository 模板识别增强
- 新增 `MACCMS_MX` 模板识别（`/vodtype/`、`/vodplay/`、`/voddetail/` 路径）
- `maccms` 关键字加入默认模板匹配
- `.html` 链接作为兜底识别条件

### 3. 加载失败时点击设置闪退
- **问题**: 源加载失败后，点击"去设置"按钮可能导致闪退
- **根因**: `startActivity(Intent(...))` 未做异常保护，Fragment 未 attach 时抛异常
- **修复**: 所有跳转 `SettingsActivity` 的 `startActivity` 调用包裹 `runCatching { }.onFailure { }`
- 新增 `addLoadFailedSiteRow()` 函数：源加载失败时显示"重试"+"去设置检查"按钮，错误信息也展示给用户

### 4. 错误处理改善
- `addCategoryRowForSite` 增加 `.onFailure` 兜底：fetch 分类失败时自动替换为错误提示行
- 所有错误路径都有 `Log.e` 日志输出，方便后续排查

## 技术细节
- `HomeFragment.kt`: renderSiteCategoryRows 重构 + addLoadFailedSiteRow 新增 + startActivity 保护
- `SourceRepository.kt`: detectHtmlTemplate 增加 MX 模板 + HtmlTemplate enum 新增 MACCMS_MX
- `build.gradle.kts`: versionName 1.0.8 → 1.0.9, versionCode 9 → 10
