# tvbox-simple 当前状态检查点 — 2026-06-30 17:56 GMT+8

## 结论
手机/模拟器使用目前看正常。当前版本已保存，方便后续优化升级。

## 项目
- 路径：D:\claude-code\projects\tvbox-simple
- 包名：com.simple.tvbox
- Git：当前目录不是 git repository，因此本检查点用文件快照 + APK + 证据截图保存。

## 当前可用功能
- 支持 TVBox JSON 源。
- 支持直接配置 HTML 影视站根 URL。
- 已实测 HTML 源：https://www.icaiqi.com，源名 icaiqi。
- 首页可显示 icaiqi / HTML 网站 与分类。
- 搜索可用：ADB smoke test 镖人 显示 icaiqi · 118 条结果。
- 详情页剧集进入播放器后动态解析 m3u8。
- ExoPlayer 播放请求带 UA/Referer。
- master playlist 相对路径已正确解析。

## 关键修复点
1. HttpUtil.kt：按 bytes 智能解码，兼容响应头 charset、HTML meta charset、UTF-8/GBK/GB2312，解决 HTML 源中文解析问题。
2. GenericHtmlClient.kt：适配 canghai/stui/maccms/generic HTML 模板；修复 icaiqi 分类、分页、详情、剧集与播放页解析。
3. GenericHtmlClient.resolveMasterPlaylist()：子 m3u8 用 java.net.URL(base, child) 相对父目录解析，避免错误拼成 .../index.m3u8/3000k/...。
4. PlayerActivity.kt：进入播放器后在 IO 线程 resolve 原始剧集 URL；找不到真实 m3u8/mp4 时显示错误覆盖层，不把 HTML 页面交给播放器。
5. SourceRepository.htmlSiteKey(raw)：统一 HTML 源 siteKey，修复首页与仓库/播放器 key 不一致。
6. SearchActivity.kt：支持可选 query extra，便于后续自动化验证。

## 最终验证
- 构建/单测命令：
  `powershell
  .\gradlew.bat :app:testDebugUnitTest --tests com.simple.tvbox.source.GenericHtmlParserTest :app:assembleDebug
  `
- 结果：BUILD SUCCESSFUL
- APK：$(D:\claude-code\projects\tvbox-simple\build-fixes\checkpoint-20260630-1756\app-debug-20260630-1756.apk.FullName)
- APK SHA256：$hash

## 证据文件
- 播放成功：evidence\47-player-final-running.png/.xml
- 首页正常：evidence\49-final-home-after-wait.png/.xml
- 搜索正常：evidence\51-search-final.png/.xml
- 关键源码快照：source-snapshot\
- 源码快照压缩包：	vbox-simple-source-snapshot-20260630-1756.zip

## 后续优化建议
- 把项目初始化为 git 仓库，后续用 commit/tag 保存版本。
- 继续验证并优化分页：/sortlist/{id}/last-2.html。
- 继续验证 	hisUrl = "" 或失效视频的错误覆盖层、重试、返回按钮。
- 改善手机端手动添加源体验，减少 DPAD/软键盘焦点问题。
- 考虑补更多 HTML 模板站点 fixture 与 parser 单测。
