# TVBox 简版 v1.0.6 更新日志

发布: 2026-07-08
versionName: 1.0.6
versionCode: 7

## 🎯 核心改动

### 1. OTA 在线升级功能

设置页新增「应用更新」区域，**用户点「检查更新」即可自动下载新版 APK 并跳转安装**。

- **服务器端**：阿里云 `1x1jt2.cn/app/`
  - `ota-tvbox.json` — 元数据（最新版本号、APK 地址、SHA256、changelog）
  - `tvbox-simple-v1.0.6-release.apk` — 3.2 MB
- **APP 端流程**：
  1. 拉 ota.json
  2. 比对 `versionCode` 决定有无更新
  3. 有更新：弹 dialog 展示 changelog
  4. 用户点「立即更新」→ 弹进度 dialog（带 cancel 按钮）
  5. 下载完成 → **SHA256 校验**（防劫持）
  6. 校验通过 → 调系统安装器
  7. 安装完 APP 自动重启，新版生效
- **兼容性**：
  - Android 7+ 走 FileProvider（不需 SD 卡写权限）
  - Android 8+ 自动引导用户授权"安装未知来源"（无授权也不阻塞，弹窗提示）
- **进度同步**：设置页和 dialog 都有进度条，两边实时同步

### 2. 后续发版流程

只需要：
```bash
# 1. 本地编 v1.0.7
assemble-release.cmd 1.0.7 8

# 2. 一条命令部署 OTA
python D:\OpenClaw-Files\ota_upload_v106.py   # 改个文件名
```
所有 1.0.5+ 用户的 APP 都会在下一次点"检查更新"时收到升级提示。

## 📦 文件

- `tvbox-simple-v1.0.6-release.apk` (3.2MB)
- `tvbox-simple-v1.0.5-release.apk` (3.2MB) — 仍保留，老用户可点升级

## 🔧 新增文件

- `app/src/main/java/com/simple/tvbox/update/OtaService.kt` (拉 JSON / 下载 APK / SHA256 校验 / 启动安装)
- `app/src/main/java/com/simple/tvbox/update/OtaUpdater.kt` (UI 流程封装：dialog + 进度条 + 权限引导)
- `app/src/main/res/layout/dialog_ota_progress.xml` (下载进度 dialog)
- `app/src/main/res/xml/file_paths.xml` (FileProvider 路径)

## 🐛 修复

- 无新增修复

## ⚠️ 注意事项

- 升级过程需要保持网络稳定
- 安装时如果弹「解析软件包出现问题」，多半是 SHA256 校验没过（防止恶意篡改）
- 用户首次点升级会被引导去授权"安装未知来源"