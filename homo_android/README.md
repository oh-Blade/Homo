# 心路历程 - Android 版

[心路历程] Android 客户端，将笔记直接保存到你的 GitHub 仓库（通过 api.github.com，无需自建后端）。

## 功能

- 编辑并保存笔记（通过 GitHub API 直接提交到仓库的 `notes/` 目录）
- 查看历史笔记列表，从 Git 仓库分页拉取
- 删除笔记（带确认）
- **配置页**：填写 GITHUB_TOKEN、GITHUB_USERNAME、GITHUB_REPO、GITHUB_BRANCH，保存到手机本地

## 环境要求

- Android Studio Ladybug (2024.2.1) 或更高版本（或兼容的 IDE）
- JDK 17
- Android SDK 24+（minSdk 24，targetSdk 34）

## 配置与运行

**从编译到运行的详细步骤**见：[运行与编译指南.md](./运行与编译指南.md)

简要步骤：

1. 用 Android Studio 打开项目根目录下的 `homo_android` 文件夹。
2. 同步 Gradle，连接设备或启动模拟器，运行应用。
3. 首次使用在应用内 **设置（配置）** 中填写四项并保存：
   - **GITHUB_TOKEN**：GitHub 个人访问令牌（需具备 repo 权限）
   - **GITHUB_USERNAME**：GitHub 用户名
   - **GITHUB_REPO**：仓库名
   - **GITHUB_BRANCH**：分支名（默认 `main`）

配置会保存在手机本地，下次打开无需重复填写。保存笔记时通过 **api.github.com** 直接提交到该仓库；查看笔记时从该仓库分页拉取。

## 数据格式

- 笔记以 JSON 文件形式存放在仓库的 `notes/` 目录（如 `note-{timestamp}.json`）。
- 与网页版（若使用同一仓库）数据格式一致，可互通显示。

## 项目结构（简要）

```
homo_android/
├── app/
│   ├── src/main/
│   │   ├── java/com/homo/notes/
│   │   │   ├── data/          # GitHub API、Repository、数据模型、配置存储
│   │   │   ├── di/             # 网络等依赖
│   │   │   └── ui/             # Compose 界面、ViewModel
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 技术栈

- Kotlin + Jetpack Compose
- ViewModel + StateFlow
- Retrofit + OkHttp + Gson（调用 api.github.com）
- DataStore（保存 GitHub 配置到本地）

## 许可证

与主项目一致（MIT）。
