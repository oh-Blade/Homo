// 初始化 Quill 编辑器
const quill = new Quill('#editor', {
    theme: 'snow',
    placeholder: '在这里写下你的想法...',
    modules: {
        toolbar: [
            [{ 'header': [1, 2, 3, false] }],
            ['bold', 'italic', 'underline', 'strike'],
            [{ 'color': [] }, { 'background': [] }],
            [{ 'list': 'ordered'}, { 'list': 'bullet' }],
            ['blockquote', 'code-block'],
            ['link'],
            ['clean']
        ]
    }
});

// DOM 元素
const saveBtn = document.getElementById('save-btn');
const clearBtn = document.getElementById('clear-btn');
const refreshBtn = document.getElementById('refresh-btn');
const wordCountSpan = document.getElementById('word-count');
const notesContainer = document.getElementById('notes-container');
const deleteModal = document.getElementById('delete-modal');
const confirmDeleteBtn = document.getElementById('confirm-delete');
const cancelDeleteBtn = document.getElementById('cancel-delete');

let currentDeleteFilename = null;

// 监听编辑器内容变化
quill.on('text-change', function() {
    const text = quill.getText().trim();
    const wordCount = text.length;
    wordCountSpan.textContent = `字数: ${wordCount}`;
    
    // 启用/禁用保存按钮
    saveBtn.disabled = wordCount === 0;
});

// 保存笔记
saveBtn.addEventListener('click', async function() {
    const content = quill.root.innerHTML.trim();
    
    if (!content || quill.getText().trim().length === 0) {
        alert('请输入笔记内容');
        return;
    }
    
    saveBtn.disabled = true;
    saveBtn.textContent = '保存中...';
    
    try {
        const response = await fetch('/api/notes', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ content })
        });
        
        const result = await response.json();
        
        if (response.ok) {
            // 清空编辑器
            quill.setContents([]);
            alert('笔记保存成功！');
            // 刷新笔记列表
            await loadNotes();
        } else {
            alert('保存失败: ' + result.error);
        }
    } catch (error) {
        console.error('保存失败:', error);
        alert('保存失败，请检查网络连接');
    } finally {
        saveBtn.disabled = false;
        saveBtn.textContent = '保存笔记';
    }
});

// 清空编辑器
clearBtn.addEventListener('click', function() {
    if (quill.getText().trim().length > 0) {
        if (confirm('确定要清空当前内容吗？')) {
            quill.setContents([]);
        }
    }
});

// 刷新笔记
refreshBtn.addEventListener('click', loadNotes);

// 加载笔记列表
async function loadNotes() {
    notesContainer.innerHTML = '<div class="loading">加载中...</div>';
    
    try {
        const response = await fetch('/api/notes');
        const notes = await response.json();
        
        if (response.ok) {
            displayNotes(notes);
        } else {
            notesContainer.innerHTML = `<div class="loading">加载失败: ${notes.error}</div>`;
        }
    } catch (error) {
        console.error('加载笔记失败:', error);
        notesContainer.innerHTML = '<div class="loading">加载失败，请检查网络连接</div>';
    }
}

// 显示笔记列表
function displayNotes(notes) {
    if (notes.length === 0) {
        notesContainer.innerHTML = '<div class="empty-state">还没有笔记，快写下第一篇吧！</div>';
        return;
    }
    
    notesContainer.innerHTML = notes.map(note => `
        <div class="note-item">
            <div class="note-header">
                <span class="note-date">${formatDate(note.timestamp)}</span>
                <div class="note-actions">
                    <button class="delete-note-btn" onclick="showDeleteModal('${note.filename}')">删除</button>
                </div>
            </div>
            <div class="note-content">${note.content}</div>
        </div>
    `).join('');
}

// 格式化日期
function formatDate(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    const diffDays = Math.floor(diff / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
        return '今天 ' + date.toLocaleTimeString('zh-CN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    } else if (diffDays === 1) {
        return '昨天 ' + date.toLocaleTimeString('zh-CN', { 
            hour: '2-digit', 
            minute: '2-digit' 
        });
    } else if (diffDays < 7) {
        return `${diffDays}天前`;
    } else {
        return date.toLocaleDateString('zh-CN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

// 显示删除确认模态框
function showDeleteModal(filename) {
    currentDeleteFilename = filename;
    deleteModal.style.display = 'block';
}

// 确认删除
confirmDeleteBtn.addEventListener('click', async function() {
    if (!currentDeleteFilename) return;
    
    confirmDeleteBtn.disabled = true;
    confirmDeleteBtn.textContent = '删除中...';
    
    try {
        const response = await fetch(`/api/notes/${currentDeleteFilename}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (response.ok) {
            deleteModal.style.display = 'none';
            await loadNotes();
        } else {
            alert('删除失败: ' + result.error);
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('删除失败，请检查网络连接');
    } finally {
        confirmDeleteBtn.disabled = false;
        confirmDeleteBtn.textContent = '删除';
        currentDeleteFilename = null;
    }
});

// 取消删除
cancelDeleteBtn.addEventListener('click', function() {
    deleteModal.style.display = 'none';
    currentDeleteFilename = null;
});

// 点击模态框外部关闭
deleteModal.addEventListener('click', function(e) {
    if (e.target === deleteModal) {
        deleteModal.style.display = 'none';
        currentDeleteFilename = null;
    }
});

// 键盘快捷键
document.addEventListener('keydown', function(e) {
    // Ctrl/Cmd + S 保存
    if ((e.ctrlKey || e.metaKey) && e.key === 's') {
        e.preventDefault();
        if (!saveBtn.disabled) {
            saveBtn.click();
        }
    }
    
    // ESC 关闭模态框
    if (e.key === 'Escape' && deleteModal.style.display === 'block') {
        deleteModal.style.display = 'none';
        currentDeleteFilename = null;
    }
});

// 页面加载完成后加载笔记
document.addEventListener('DOMContentLoaded', function() {
    loadNotes();
}); 