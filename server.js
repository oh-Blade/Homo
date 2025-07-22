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
    'User-Agent': 'Journey-Notes-App'
};

// 获取所有笔记
app.get('/api/notes', async (req, res) => {
    try {
        const response = await axios.get(
            `${GITHUB_API_BASE}/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes`,
            { headers: githubHeaders }
        );

        const notes = [];
        for (const file of response.data) {
            if (file.name.endsWith('.json')) {
                const fileResponse = await axios.get(file.download_url);
                notes.push({
                    ...fileResponse.data,
                    filename: file.name
                });
            }
        }

        // 按时间戳降序排序
        notes.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        res.json(notes);
    } catch (error) {
        if (error.response && error.response.status === 404) {
            // notes 文件夹不存在，返回空数组
            res.json([]);
        } else {
            console.error('获取笔记失败:', error.message);
            res.status(500).json({ error: '获取笔记失败' });
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

        const timestamp = new Date().toISOString();
        const filename = `note-${Date.now()}.json`;
        const noteData = {
            id: Date.now(),
            content: content.trim(),
            timestamp,
            created: timestamp
        };

        // 将笔记保存到 GitHub
        const response = await axios.put(
            `${GITHUB_API_BASE}/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`,
            {
                message: `添加笔记: ${new Date().toLocaleString('zh-CN')}`,
                content: Buffer.from(JSON.stringify(noteData, null, 2)).toString('base64'),
                branch: GITHUB_BRANCH
            },
            { headers: githubHeaders }
        );

        res.json({ 
            success: true, 
            note: noteData,
            github_response: response.data 
        });
    } catch (error) {
        console.error('保存笔记失败:', error.response?.data || error.message);
        res.status(500).json({ error: '保存笔记失败' });
    }
});

// 删除笔记
app.delete('/api/notes/:filename', async (req, res) => {
    try {
        const { filename } = req.params;
        
        // 获取文件的 SHA
        const fileResponse = await axios.get(
            `${GITHUB_API_BASE}/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`,
            { headers: githubHeaders }
        );

        // 删除文件
        await axios.delete(
            `${GITHUB_API_BASE}/repos/${GITHUB_USERNAME}/${GITHUB_REPO}/contents/notes/${filename}`,
            {
                headers: githubHeaders,
                data: {
                    message: `删除笔记: ${filename}`,
                    sha: fileResponse.data.sha,
                    branch: GITHUB_BRANCH
                }
            }
        );

        res.json({ success: true });
    } catch (error) {
        console.error('删除笔记失败:', error.response?.data || error.message);
        res.status(500).json({ error: '删除笔记失败' });
    }
});

// 启动服务器
app.listen(PORT, () => {
    console.log(`服务器运行在 http://localhost:${PORT}`);
    console.log(`GitHub 仓库: ${GITHUB_USERNAME}/${GITHUB_REPO}`);
}); 