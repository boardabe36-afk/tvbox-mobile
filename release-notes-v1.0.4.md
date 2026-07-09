# TVBox Simple v1.0.4

正式签名版发布。

## 软件说明

TVBox Simple 是一个基于 TVBox 使用思路的极简 Android TV / 电视盒子播放器。项目不内置任何影视内容、视频源、账号、API Key 或爬虫包，仅提供客户端能力。用户需要自行添加合法可访问的视频源。

本项目基于 [NewOrin/TVBox](https://github.com/NewOrin/TVBox) 的设计思路与 TVBox JSON/Spider 协议生态进行简化实现，但不是原版 TVBox，也不是官方版本。

## 主要功能

- TVBox JSON 源解析：站点、分类、搜索、详情、播放。
- 实验性 HTML 源解析：支持部分 maccms/canghai/stui/mx 模板站点。
- Android TV Leanback 遥控器 UI。
- 首页快捷入口、热门影视卡片、无源引导。
- 分类、搜索结果、剧集列表网格化。
- 聚合搜索与精准排序。
- 观看历史自动保存。
- 断点续看。
- 局域网扫码搜索、扫码传文件、扫码设置。
- 简单海报加载与内存缓存。

## 相比原版 TVBox 的差异与优化

- Kotlin + AndroidX + Media3 ExoPlayer 重写，结构更轻。
- 删除大量复杂设置，保留最常用的源添加、浏览、搜索、播放流程。
- 增加无源状态快捷入口，首次使用可直接扫码设置。
- 增加局域网二维码工具，解决电视遥控器输入困难。
- 增加观看历史和断点续看。
- 搜索结果按标题匹配精度排序。
- 增加实验性通用 HTML 模板解析，不完全依赖现成 JSON 配置。

## APK

附件：`tvbox-simple-v1.0.4-release.apk`

这是正式 release 构建，已使用本项目发布签名证书签名，并启用 R8/资源压缩。

签名验证：

```text
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Certificate SHA-256: b6d7545cc2fd2d6241aa6fa2366ebac96fbb6e051144ff8ac6d9b99372a5bb48
```

## 构建验证

```text
./gradlew.bat clean assembleRelease testDebugUnitTest
BUILD SUCCESSFUL
```

## 合规说明

请遵守当地法律法规。项目仅用于学习 Android TV 客户端开发、TVBox 协议解析、媒体播放和局域网交互，不提供内容源，也不鼓励访问侵权内容。
