# 影图+

TVBox 风格的开源影视播放器，支持订阅 TVBox 配置的 JSON 源进行内容播放。

## 功能

- 首页推荐 / 分类浏览 / 直播 / 我的 四个 Tab
- 搜索电影、电视剧
- 影片详情 + 线路切换 + 选集播放
- 视频源管理（输入 TVBox 订阅地址自动解析）
- 观看历史记录（本地 Hawk 持久化）
- 深色模式
- Media3 ExoPlayer 播放器

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
