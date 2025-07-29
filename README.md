# 心路历程 - 个人笔记应用

一个简洁优雅的个人笔记网页应用，支持富文本编辑，并将笔记自动保存到你的私有 GitHub 仓库中。

## ✨ 特性

- 🖋️ **富文本编辑器** - 支持文本格式化、列表、引用等
- 💾 **GitHub 存储** - 笔记自动保存到你的私有 GitHub 仓库
- 📱 **响应式设计** - 完美适配桌面和移动设备
- 🔒 **隐私安全** - 使用你自己的私有仓库存储数据
- ⚡ **无需登录** - 配置好仓库信息即可使用
- 🎨 **美观界面** - 现代化的用户界面设计
- 📄 **分页加载** - 智能分页，快速加载历史记录

## 🚀 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置环境变量

复制 `env.example` 文件为 `.env`：

```bash
cp env.example .env
```

编辑 `.env` 文件，填入你的 GitHub 配置信息：

```env
# GitHub Configuration
GITHUB_TOKEN=your_github_personal_access_token_here
GITHUB_USERNAME=your_github_username
GITHUB_REPO=your_private_repo_name
GITHUB_BRANCH=main

# Server Configuration
PORT=3000
```

### 3. 获取 GitHub Personal Access Token

1. 访问 [GitHub Settings > Developer settings > Personal access tokens](https://github.com/settings/tokens)
2. 点击 "Generate new token (classic)"
3. 设置 Token 名称，选择过期时间
4. 勾选以下权限：
   - `repo` (完整的仓库访问权限)
5. 点击 "Generate token"
6. 复制生成的 token 到 `.env` 文件中

### 4. 创建私有仓库

1. 在 GitHub 上创建一个新的私有仓库
2. 将仓库名填入 `.env` 文件的 `GITHUB_REPO` 字段

### 5. 启动应用

```bash
# 开发模式（自动重启）
npm run dev

# 或者生产模式
npm start
```

访问 [http://localhost:3000](http://localhost:3000) 开始使用！

## 🛡️ 使用 PM2 实现常驻运行和开机自启（推荐）

你可以用 [PM2](https://pm2.keymetrics.io/) 让本项目在服务器或本地电脑上"常驻运行"并"开机自启动"。

### 安装 PM2

```bash
npm install -g pm2
```

### 启动项目

```bash
pm2 start server.js --name journey-notes
```

### 设置开机自启

```bash
pm2 startup
```
终端会输出一条命令（如 `sudo ...`），请复制粘贴执行。

### 保存当前进程列表

```bash
pm2 save
```

### 常用管理命令

- 查看状态：`pm2 status`
- 查看日志：`pm2 logs journey-notes`
- 重启：`pm2 restart journey-notes`
- 停止：`pm2 stop journey-notes`
- 删除：`pm2 delete journey-notes`

---

## 📁 项目结构

```
journey-notes/
├── public/                 # 前端静态文件
│   ├── index.html         # 主页面
│   ├── style.css          # 样式文件
│   └── script.js          # 前端逻辑
├── server.js              # Express 服务器
├── package.json           # 项目配置
├── env.example           # 环境变量示例
├── .env                  # 环境变量（需要配置）
├── .gitignore           # Git 忽略文件
└── README.md            # 项目说明
```

## 🛠️ 技术栈

- **前端**: HTML5, CSS3, JavaScript, Quill.js (富文本编辑器)
- **后端**: Node.js, Express.js
- **存储**: GitHub API
- **样式**: 原生 CSS（响应式设计）

## 📝 使用说明

1. **写笔记**: 在页面顶部的富文本编辑器中输入你的想法
2. **保存**: 点击"保存笔记"按钮，笔记将自动保存到 GitHub 仓库
3. **查看历史**: 在下方的历史记录区域查看所有保存的笔记
4. **删除笔记**: 点击笔记右上角的删除按钮可以删除不需要的笔记
5. **加载更多**: 点击"加载更多笔记"按钮查看更多历史记录

## 🔑 快捷键

- `Ctrl/Cmd + S`: 保存当前笔记
- `ESC`: 关闭模态框

## 🔒 隐私与安全

- 所有笔记数据存储在你自己的私有 GitHub 仓库中
- GitHub Token 和仓库信息存储在本地环境变量中
- `.env` 文件已添加到 `.gitignore`，不会被提交到代码仓库

## 📦 数据格式

笔记以 JSON 格式存储在 GitHub 仓库的 `notes/` 目录下：

```json
{
  "id": 1640995200000,
  "content": "<p>这是一条笔记的 HTML 内容</p>",
  "timestamp": "2023-12-31T16:00:00.000Z",
  "created": "2023-12-31T16:00:00.000Z"
}
```

## 🛟 故障排除

### Token 权限问题
确保你的 GitHub Token 具有 `repo` 权限，能够访问私有仓库。

### 仓库不存在
确保在 `.env` 文件中正确配置了仓库名称，并且该仓库确实存在。

### 网络连接问题
检查网络连接，确保能够访问 GitHub API。

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

享受记录你的心路历程吧！ ✨ 