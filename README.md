# 影图+

TVBox 风格的开源影视播放器，支持订阅 TVBox 配置的 JSON 源进行内容播放，并支持 **WebHome 网页自定义首页**（参考 [webhtv/webhtv](https://github.com/webhtv/webhtv)）。

包名：`com.yingtu.plus`

## 功能

- 首页快捷入口宫格 + 5 个 Tab：主页 / 网页首页 / 本地文件 / 直链收藏 / 分享收藏
- **WebHome 网页首页**：用 HTML/CSS/JS 定制首页，运行在内置 WebView 并注入 App Native SDK（`window.fm` / `window.fongmi`），页面可调用搜索、播放、跨域请求、资源代理、缓存、最近观看等原生能力
  - 支持三种来源：远程网址 / **设备本地 .html 文件** / 内置示例
  - 内置「能力演示主页」「基础模板主页」两个示例页
  - 本地资源网关 `http://127.0.0.1:<port>/webResource`（`fm.res`），绕过浏览器 CORS、注入自定义请求头、透传 Range 分段
- 搜索电影、电视剧
- 影片详情 + 线路切换 + 选集播放 + 二次解析（JSON 接口 / WebView 嗅探）
- 视频源管理（输入 TVBox 订阅地址自动解析）
- 观看历史记录（本地 Hawk 持久化）+ 续播
- 直链收藏 / 分享收藏 / 本地视频扫描
- 深色模式 · Media3 ExoPlayer 播放器

## WebHome 开发

网页首页通过注入的 `window.fm` 调用原生能力，核心接口：

| 接口 | 作用 |
| --- | --- |
| `fm.req(url, options)` | Native OkHttp 请求，绕过 CORS，返回 Promise |
| `fm.res(url, options)` | 为 DOM 资源生成本地网关地址（图片/视频/字幕） |
| `fm.play(url, title, options)` | 播放直链（可注入 header） |
| `fm.vod(siteKey, vodId, ...)` | 进入站源详情播放 |
| `fm.vodInline(payload)` | 临时多集直链播放 |
| `fm.search(keyword, options)` | 进入原生搜索 |
| `fm.history()` | 读取最近观看 |
| `fm.cache.get/set/del` | 跨重启持久化缓存 |
| `fm.device() / fm.site() / fm.config()` | 设备 / 站点 / 配置信息 |
| `fm.ui.setChrome / getViewport` | chrome 模式与安全区 |

页面监听 `fmsdk`（SDK 就绪）、`fmresume` / `fmpause`（前后台）、`fmviewport`（视口变化）等事件。写法基线建议 ES2017，兼容旧 WebView 内核。

在 App 内「网页首页」Tab 点右下角 **+** 添加你自己的网址或 `.html` 文件即可。

## 构建

### 本地构建
```bash
git clone https://github.com/SyntaxJester/YingTu.git
cd YingTu
./gradlew :app:assembleDebug
```

### GitHub Actions
Push 到 `main` 或创建 `v*` tag 后自动触发构建，产出 Debug APK 作为 artifact 下载。

## 添加视频源

1. 打开 App → 底部「我的」→ 「视频源」
2. 输入 TVBox 订阅地址（JSON 格式）
3. 点击「加载」解析源列表
4. 选择任意一个源即可开始使用

示例源地址：
- [tvbox.tv](https://raw.githubusercontent.com/gaotianliuyun/gao/master/jars.txt) （仅示例，需自行验证可用性）
- 可从 [TVBoxOSC](https://github.com/zwvmf/TVBoxOSC) 社区获取社区维护的源配置

## 声明

本项目仅供学习研究使用。所有视频内容均来自用户自行配置的第三方源，开发者不提供、不存储任何影视资源。

## License

MIT
