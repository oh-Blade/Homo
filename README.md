# 心路历程 (Homo)

个人笔记应用项目，支持将笔记保存到你的私有 GitHub 仓库。提供 **Web 版** 与 **Android 版** 两种客户端，数据格式互通，可共用同一仓库。

## 项目概览

| 客户端 | 目录 | 说明 |
|--------|------|------|
| Web 版 | [homo_web/](./homo_web/) | 网页应用，富文本编辑，Node.js + Express 后端 |
| Android 版 | [homo_android/](./homo_android/) | 原生 App，Kotlin + Jetpack Compose，直连 GitHub API |

- **存储**：笔记均保存在你配置的 GitHub 仓库 `notes/` 目录下（JSON 格式）。
- **隐私**：使用你自己的私有仓库与 Personal Access Token，无需自建后端（Android 直连 api.github.com；Web 版为本地/自托管服务）。

## 快速导航

- **Web 版**：安装依赖、配置 `.env`（GITHUB_TOKEN、GITHUB_USERNAME、GITHUB_REPO 等）→ 运行 `npm run dev` 或 `npm start`。详见 [homo_web/README.md](./homo_web/README.md)。
- **Android 版**：用 Android Studio 打开 `homo_android`，同步 Gradle 后运行；首次使用在应用内「设置」中填写 GitHub 配置。详见 [homo_android/README.md](./homo_android/README.md)。

## 目录结构

```
Homo/
├── README.md           # 本文件（项目总览）
├── .gitignore
├── homo_web/           # Web 客户端（心路历程 - 网页版）
│   ├── README.md
│   ├── server.js
│   ├── public/
│   ├── package.json
│   └── ...
└── homo_android/       # Android 客户端（心路历程 - Android 版）
    ├── README.md
    ├── app/
    ├── build.gradle.kts
    └── ...
```

## 许可证

MIT License。各子项目与主项目一致。
