require('dotenv').config();
const express = require('express');
const axios = require('axios');
const cors = require('cors');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// 中间件
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.static('public'));

// GitHub API 配置
const GITHUB_API_BASE = 'https://api.github.com';
const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const GITHUB_USERNAME = process.env.GITHUB_USERNAME;
const GITHUB_REPO = process.env.GITHUB_REPO;
const GITHUB_BRANCH = process.env.GITHUB_BRANCH || 'main';

// 验证环境变量
if (!GITHUB_TOKEN || !GITHUB_USERNAME || !GITHUB_REPO) {
    console.error('请确保设置了所有必需的环境变量：GITHUB_TOKEN, GITHUB_USERNAME, GITHUB_REPO');
    process.exit(1);
}

// GitHub API 请求头
const githubHeaders = {
    'Authorization': `token ${GITHUB_TOKEN}`,
    'Content-Type': 'application/json',
    'User-Agent': 'Journey-Notes-App',
    'Accept': 'application/vnd.github.v3+json'
};

// 创建 axios 实例，统一配置
const githubApi = axios.create({
    baseURL: GITHUB_API_BASE,
    headers: githubHeaders,
    timeout: 10000 // 10秒超时
});

// 简单的内存缓存（生产环境建议使用 Redis）
const cache = new Map();
const CACHE_TTL = 30000; // 30秒缓存

// 获取所有笔记 - 优化：消除异步瀑布流，并行获取所有文件
app.get('/api/notes', async (req, res) => {
    try {
        const page = parseInt(req.query.page) || 1;
        const limit = parseInt(req.query.limit) || 5;
        const offset = (page - 1) * limit;
        
        // 检查缓存
        const cacheKey = `notes:${page}:${limit}`;
        const cached = cache.get(cacheKey);
        if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
            return res.json(cached.data);
        }

        // 获取文件列表
        const response = await githubApi.get(
            `/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes`
        );
        
        // 过滤 JSON 文件并并行获取所有文件内容（消除异步瀑布流）
        const jsonFiles = response.data.filter(file => file.name.endsWith('.json'));
        
        // 并行获取所有文件内容，而不是串行
        const filePromises = jsonFiles.map(file => 
            axios.get(file.download_url).then(fileResponse => ({
                ...fileResponse.data,
                filename: file.name
            })).catch(err => {
                console.error(`获取文件 ${file.name} 失败:`, err.message);
                return null; // 返回 null，后续过滤掉
            })
        );
        
        const notesResults = await Promise.all(filePromises);
        const notes = notesResults.filter(note => note !== null);

        // 按时间戳降序排序
        notes.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        
        // 分页处理
        const totalNotes = notes.length;
        const paginatedNotes = notes.slice(offset, offset + limit);
        const hasMore = offset + limit < totalNotes;

        const result = {
            notes: paginatedNotes,
            pagination: {
                page,
                limit,
                total: totalNotes,
                hasMore,
                totalPages: Math.ceil(totalNotes / limit)
            }
        };

        // 缓存结果
        cache.set(cacheKey, { data: result, timestamp: Date.now() });

        res.json(result);
    } catch (error) {
        if (error.response && error.response.status === 404) {
            // notes 文件夹不存在，返回空数组
            res.json({
                notes: [],
                pagination: {
                    page: 1,
                    limit: 5,
                    total: 0,
                    hasMore: false,
                    totalPages: 0
                }
            });
        } else {
            console.error('获取笔记失败:', error.response?.data || error.message);
            res.status(error.response?.status || 500).json({ 
                error: '获取笔记失败',
                message: error.response?.data?.message || error.message
            });
        }
    }
});

// 保存新笔记
app.post('/api/notes', async (req, res) => {
    try {
        const { content } = req.body;
        
        if (!content || !content.trim()) {
            return res.status(400).json({ error: '笔记内容不能为空' });
        }

        // 验证内容长度
        const trimmedContent = content.trim();
        if (trimmedContent.length > 100000) {
            return res.status(400).json({ error: '笔记内容过长，请控制在100KB以内' });
        }

        const timestamp = new Date().toISOString();
        const filename = `note-${Date.now()}.json`;
        const noteData = {
            id: Date.now(),
            content: trimmedContent,
            timestamp,
            created: timestamp
        };

        // 将笔记保存到 GitHub
        const response = await githubApi.put(
            `/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`,
            {
                message: `添加笔记: ${new Date().toLocaleString('zh-CN')}`,
                content: Buffer.from(JSON.stringify(noteData, null, 2)).toString('base64'),
                branch: GITHUB_BRANCH
            }
        );

        // 清除缓存
        cache.clear();

        res.json({ 
            success: true, 
            note: noteData
        });
    } catch (error) {
        console.error('保存笔记失败:', error.response?.data || error.message);
        const statusCode = error.response?.status || 500;
        res.status(statusCode).json({ 
            error: '保存笔记失败',
            message: error.response?.data?.message || error.message
        });
    }
});

// 删除笔记
app.delete('/api/notes/:filename', async (req, res) => {
    try {
        const { filename } = req.params;
        
        // 验证文件名格式，防止路径遍历攻击
        if (!filename.match(/^note-\d+\.json$/)) {
            return res.status(400).json({ error: '无效的文件名格式' });
        }
        
        // 获取文件的 SHA
        const fileResponse = await githubApi.get(
            `/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`
        );

        // 删除文件
        await githubApi.delete(
            `/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`,
            {
                data: {
                    message: `删除笔记: ${filename}`,
                    sha: fileResponse.data.sha,
                    branch: GITHUB_BRANCH
                }
            }
        );

        // 清除缓存
        cache.clear();

        res.json({ success: true });
    } catch (error) {
        console.error('删除笔记失败:', error.response?.data || error.message);
        const statusCode = error.response?.status || 500;
        res.status(statusCode).json({ 
            error: '删除笔记失败',
            message: error.response?.data?.message || error.message
        });
    }
});

// 健康检查端点
app.get('/api/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        timestamp: new Date().toISOString(),
        cacheSize: cache.size
    });
});

// 错误处理中间件
app.use((err, req, res, next) => {
    console.error('未处理的错误:', err);
    res.status(500).json({ 
        error: '服务器内部错误',
        message: process.env.NODE_ENV === 'development' ? err.message : undefined
    });
});

// 404 处理
app.use((req, res) => {
    res.status(404).json({ error: '未找到请求的资源' });
});

// 启动服务器
app.listen(PORT, () => {
    console.log(`服务器运行在 http://localhost:${PORT}`);
    console.log(`GitHub 仓库: ${GITHUB_USERNAME}/${GITHUB_REPO}`);
    console.log(`环境: ${process.env.NODE_ENV || 'development'}`);
}); 