# TVBox 简版 v1.0.7 更新日志

发布: 2026-07-08
versionName: 1.0.7
versionCode: 8

## 🚨 强制升级（修复 1.0.5/1.0.6 关键 bug）

### 修复 1: OTA 检查更新失败 ✅

**症状**: 设置页 → "应用更新" → "检查更新" 失败，logcat 报：
```
SSLPeerUnverifiedException: Hostname 1x1jt2.cn not verified
  DN: CN=www.1x1jt2.cn
  subjectAltNames: [www.1x1jt2.cn]
```

**原因**: 阿里云证书的 CN 和 SAN 都是 `www.1x1jt2.cn`，但 OTA URL 写成了 `https://1x1jt2.cn/...`（不带 www），hostname verify 直接 fail。

**修复**: 全部统一为 `https://www.1x1jt2.cn/...`，ota.json 里的 apkUrl 同步更新。

### 修复 2: 豆瓣 + 独立浏览页全部"加载失败" ✅

**症状**: 首页"豆瓣热门"行 / 独立 DoubanActivity 全部显示"加载失败"。

**真因** (找到的过程很有意思):
1. PowerShell 终端 GBK 编码渲染 logcat 时把 UTF-8 中文显示成"乱码"（实际数据是对的）
2. 这让我误以为 OkHttp `body.string()` 的 charset 处理有问题
3. 真正真因是 **`parseList()` 里 `jsonParser.getJSONObject(text)` 用错了 API**：
   - `getJSONObject(String)` 是 **按 key 取 nested object**（返回 JSONObject key 的值）
   - 正确应该用 `JSONObject(text)` 构造器，把 text 作为 JSON 字符串 parse
4. 修复后 `parsed 30 items` 完美工作

**附带修**: 加 `OkHttp.proxy(NO_PROXY)` 避免 system proxy（Clash 7897 / 模拟器内置代理）把 APP 拉到 127.0.0.1:7897 这种不可达端口；新增 `DoubanDirect` 用 HttpURLConnection 直连兜底。

## 关键改进

| 改动 | 文件 | 说明 |
|------|------|------|
| OTA URL 加 www 前缀 | `OtaService.kt` | 匹配证书 CN/SAN |
| ota.json apkUrl 加 www | 阿里云 | 同步 |
| 豆瓣 JSON 解析修复 | `DoubanService.kt` | `JSONObject(text)` 而非 `getJSONObject(text)` |
| OkHttp proxy(NO_PROXY) | `HttpUtil.kt` | 避免 system proxy 把 APP 拉黑 |
| DoubanDirect HttpURLConnection | `source/DoubanDirect.kt` | 不依赖 OkHttp 的兜底 |
| 详情 log 增强 | `DoubanService/OtaService` | 关键路径加 `Log.i` 便于排查 |

## 验证（已在 Android Studio 模拟器 Pixel 7 Pro 上跑通）

- ✅ 豆瓣首页/电影/电视剧 各 30 条目解析成功，"痴迷"/"野狗骨头"等中文正常显示
- ✅ OTA 拉取 + 解析 + 版本比对 全部 200 OK
- ✅ 旧版 1.0.5/1.0.6 用户下一次点"检查更新"会**强制升级**到 1.0.7

## 📦 文件

- `tvbox-simple-v1.0.7-release.apk` (3.2MB) ← 推荐安装

## ⚠️ 1.0.5/1.0.6 升级注意事项

- 这次 OTA `forceUpdate=true`，弹窗无法关闭
- 网络环境必须能访问 `https://www.1x1jt2.cn/app/...`
- 如果用户没授权"安装未知来源"，点"立即更新"会被引导去设置授权