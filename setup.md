# 快速设置指南

## 配置步骤

### 1. 复制环境变量文件
```bash
cp env.example .env
```

### 2. 获取 GitHub Personal Access Token
1. 访问：https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 设置名称：`Journey Notes App`
4. 勾选权限：`repo` (完整的仓库访问权限)
5. 点击 "Generate token"
6. 复制生成的 token

### 3. 创建私有仓库
1. 在 GitHub 创建新的私有仓库
2. 记住仓库名称

### 4. 编辑 .env 文件
```env
GITHUB_TOKEN=你的_github_token_这里
GITHUB_USERNAME=你的github用户名
GITHUB_REPO=你的私有仓库名
GITHUB_BRANCH=main
PORT=3000
```

### 5. 启动应用
```bash
npm start
```

然后访问 http://localhost:3000

## 注意事项
- 确保 GitHub Token 有 `repo` 权限
- 仓库必须是你可以访问的私有仓库
- `.env` 文件已被 gitignore，不会被提交到代码仓库 