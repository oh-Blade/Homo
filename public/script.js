// 工具函数：防抖
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 工具函数：节流
function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

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
let currentPage = 1;
let hasMoreNotes = true;
let isLoading = false;
let abortController = null; // 用于取消正在进行的请求

// 更新字数统计（使用防抖优化性能）
const updateWordCount = debounce(() => {
    const text = quill.getText().trim();
    const wordCount = text.length;
    wordCountSpan.textContent = `字数: ${wordCount}`;
    
    // 启用/禁用保存按钮
    saveBtn.disabled = wordCount === 0;
}, 150);

// 监听编辑器内容变化
quill.on('text-change', updateWordCount);

// 保存笔记
saveBtn.addEventListener('click', async function() {
    const content = quill.root.innerHTML.trim();
    const text = quill.getText().trim();
    
    if (!content || text.length === 0) {
        showNotification('请输入笔记内容', 'warning');
        return;
    }
    
    // 防止重复提交
    if (saveBtn.disabled && saveBtn.textContent === '保存中...') {
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
            showNotification('笔记保存成功！', 'success');
            // 刷新笔记列表
            await loadNotes(true);
        } else {
            showNotification('保存失败: ' + (result.message || result.error), 'error');
        }
    } catch (error) {
        console.error('保存失败:', error);
        showNotification('保存失败，请检查网络连接', 'error');
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

// 刷新笔记（使用节流防止频繁点击）
refreshBtn.addEventListener('click', throttle(() => {
    loadNotes(true);
}, 1000));

// 加载笔记列表（优化：取消之前的请求，避免竞态条件）
async function loadNotes(reset = false) {
    if (isLoading) return;
    
    // 取消之前的请求
    if (abortController) {
        abortController.abort();
    }
    abortController = new AbortController();
    
    if (reset) {
        currentPage = 1;
        hasMoreNotes = true;
        notesContainer.innerHTML = '<div class="loading">加载中...</div>';
    }
    
    if (!hasMoreNotes && !reset) return;
    
    isLoading = true;
    
    try {
        const response = await fetch(`/api/notes?page=${currentPage}&limit=5`, {
            signal: abortController.signal
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        
        if (reset) {
            displayNotes(data.notes, data.pagination);
        } else {
            appendNotes(data.notes, data.pagination);
        }
    } catch (error) {
        // 忽略取消的请求
        if (error.name === 'AbortError') {
            return;
        }
        
        console.error('加载笔记失败:', error);
        if (reset) {
            notesContainer.innerHTML = '<div class="error-state">加载失败，请检查网络连接</div>';
        } else {
            showNotification('加载失败，请检查网络连接', 'error');
        }
    } finally {
        isLoading = false;
    }
}

// 显示笔记列表（重置模式）- 优化：使用 DocumentFragment 减少重排
function displayNotes(notes, pagination) {
    if (notes.length === 0) {
        notesContainer.innerHTML = '<div class="empty-state">还没有笔记，快写下第一篇吧！</div>';
        return;
    }
    
    // 使用 DocumentFragment 优化 DOM 操作
    const fragment = document.createDocumentFragment();
    const tempDiv = document.createElement('div');
    
    const notesHTML = notes.map(note => createNoteHTML(note)).join('');
    const loadMoreHTML = pagination.hasMore ? createLoadMoreHTML() : '';
    
    tempDiv.innerHTML = notesHTML + loadMoreHTML;
    while (tempDiv.firstChild) {
        fragment.appendChild(tempDiv.firstChild);
    }
    
    notesContainer.innerHTML = '';
    notesContainer.appendChild(fragment);
    hasMoreNotes = pagination.hasMore;
}

// 追加笔记（加载更多模式）- 优化：使用事件委托
function appendNotes(notes, pagination) {
    if (notes.length === 0) return;
    
    const notesHTML = notes.map(note => createNoteHTML(note)).join('');
    const loadMoreHTML = pagination.hasMore ? createLoadMoreHTML() : '';
    
    // 移除现有的加载更多按钮
    const existingLoadMore = notesContainer.querySelector('.load-more-container');
    if (existingLoadMore) {
        existingLoadMore.remove();
    }
    
    // 追加新笔记
    notesContainer.insertAdjacentHTML('beforeend', notesHTML + loadMoreHTML);
    hasMoreNotes = pagination.hasMore;
}

// 创建单个笔记的HTML - 优化：转义 HTML 防止 XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function createNoteHTML(note) {
    // 转义文件名防止 XSS
    const safeFilename = escapeHtml(note.filename);
    return `
        <div class="note-item">
            <div class="note-header">
                <span class="note-date">${escapeHtml(formatDate(note.timestamp))}</span>
                <div class="note-actions">
                    <button class="delete-note-btn" data-filename="${safeFilename}">删除</button>
                </div>
            </div>
            <div class="note-content">${note.content}</div>
        </div>
    `;
}

// 创建加载更多按钮的HTML
function createLoadMoreHTML() {
    return `
        <div class="load-more-container" style="text-align: center; margin: 20px 0;">
            <button id="load-more-btn" class="load-more-btn">加载更多笔记</button>
        </div>
    `;
}

// 使用事件委托处理容器内的所有点击事件（优化性能，减少事件监听器）
notesContainer.addEventListener('click', function(e) {
    // 处理删除按钮点击
    if (e.target.classList.contains('delete-note-btn')) {
        const filename = e.target.getAttribute('data-filename');
        if (filename) {
            showDeleteModal(filename);
        }
        return;
    }
    
    // 处理加载更多按钮点击
    if (e.target.id === 'load-more-btn' || e.target.closest('#load-more-btn')) {
        if (isLoading || !hasMoreNotes) return;
        currentPage++;
        loadNotes(false);
    }
});

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
    deleteModal.setAttribute('aria-hidden', 'false');
    // 聚焦到取消按钮（更好的可访问性）
    cancelDeleteBtn.focus();
}

// 确认删除
confirmDeleteBtn.addEventListener('click', async function() {
    if (!currentDeleteFilename) return;
    
    // 防止重复提交
    if (confirmDeleteBtn.disabled) return;
    
    confirmDeleteBtn.disabled = true;
    confirmDeleteBtn.textContent = '删除中...';
    
    try {
        // 转义文件名防止路径遍历
        const safeFilename = encodeURIComponent(currentDeleteFilename);
        const response = await fetch(`/api/notes/${safeFilename}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (response.ok) {
            deleteModal.style.display = 'none';
            deleteModal.setAttribute('aria-hidden', 'true');
            showNotification('笔记删除成功', 'success');
            await loadNotes(true);
        } else {
            showNotification('删除失败: ' + (result.message || result.error), 'error');
        }
    } catch (error) {
        console.error('删除失败:', error);
        showNotification('删除失败，请检查网络连接', 'error');
    } finally {
        confirmDeleteBtn.disabled = false;
        confirmDeleteBtn.textContent = '删除';
        currentDeleteFilename = null;
    }
});

// 取消删除
cancelDeleteBtn.addEventListener('click', function() {
    deleteModal.style.display = 'none';
    deleteModal.setAttribute('aria-hidden', 'true');
    currentDeleteFilename = null;
});

// 点击模态框外部关闭
deleteModal.addEventListener('click', function(e) {
    if (e.target === deleteModal) {
        deleteModal.style.display = 'none';
        deleteModal.setAttribute('aria-hidden', 'true');
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
        deleteModal.setAttribute('aria-hidden', 'true');
        currentDeleteFilename = null;
    }
});

// 通知提示函数（替代 alert，更好的用户体验）
function showNotification(message, type = 'info') {
    // 移除已存在的通知
    const existingNotification = document.querySelector('.notification');
    if (existingNotification) {
        existingNotification.remove();
    }
    
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.setAttribute('role', 'alert');
    
    document.body.appendChild(notification);
    
    // 触发动画
    setTimeout(() => notification.classList.add('show'), 10);
    
    // 自动移除
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// 页面加载完成后加载笔记
document.addEventListener('DOMContentLoaded', function() {
    loadNotes(true);
}); 