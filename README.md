# TVBox Mobile (Android 平板 / 手机版)

基于 [tvbox-simple v1.0.14](../tvbox-simple) 的 **手机/平板**专用版。
不是 TV 版，是为 Android 平板和手机 UX 重新设计的 Compose 实现。

技术栈: **Kotlin + Jetpack Compose + Material 3**

## 与 TV 版的核心差异

| 维度 | TVBox (TV) | TVBox Mobile |
|------|------------|--------------|
| UI 框架 | Leanback BrowseSupportFragment (TV 焦点导航) | Compose + Material 3 (触摸 + 大屏) |
| 导航 | DPAD 横向 Row Headers | 抽屉 + 底部导航 + 双栏 (≥600dp) |
| 方向 | 横屏 only | 双方向自适应 |
| 主题 | 全屏深色 | 深色为主 + 卡片化海报感 |
| 交互 | 遥控器焦点 | 触摸、滑动、捏合缩放 |

## 复用业务层 (~70% 代码共享)

`app/src/main/java/com/simple/tvbox/` 整个 data + model + source + util 都是从 tvbox-simple 1:1 复用:

- `data/SourceRepository.kt` — 源管理（SharedPreferences + Gson）
- `source/GenericHtmlClient.kt` — HTML 视频站抓取 (MACCMS 模板识别)
- `source/SpiderClient.kt` — JSON 协议源
- `source/VideoClientFactory.kt` — 视频客户端工厂
- `util/MatchScorer.kt` — v1.0.13 智能匹配算法
- `util/HttpUtil.kt` — OkHttp 封装

Compose 层 (`com.simple.tvboxmobile.ui.*`) 全部新写。

## 功能清单 (v1.0.1 MVP)

- ✅ 添加视频源 (HTML / JSON)
- ✅ 删除视频源
- ✅ 首页：源列表 + 搜索入口
- ✅ 设置：源增删
- ✅ 搜索：全源聚合 + 智能匹配（复用 tvbox v1.0.13 MatchScorer）
- ✅ 播放：Media3 ExoPlayer
- ⏳ 详情页：v1.0.2 加
- ⏳ 豆瓣热门：v1.0.2 加
- ⏳ 历史记录：v1.0.3 加
- ⏳ OTA 升级：v1.0.3 加

## 构建

```bash
./gradlew assembleDebug        # debug APK
./gradlew assembleRelease      # release APK
adb install app/build/outputs/apk/release/app-release.apk
```

最低 SDK 24 (Android 7.0)，compileSdk 34。

## 仓库

- GitHub: https://github.com/boardabe36-afk/tvbox-mobile
- 与 [tvbox-simple](https://github.com/boardabe36-afk/tvbox-simple) 同一作者
