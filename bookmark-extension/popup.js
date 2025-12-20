// DOM Elements
const loginView = document.getElementById('loginView');
const mainView = document.getElementById('mainView');
const loginForm = document.getElementById('loginForm');
const logoutBtn = document.getElementById('logoutBtn');
const bookmarkForm = document.getElementById('bookmarkForm');
const searchInput = document.getElementById('searchInput');
const bookmarkList = document.getElementById('bookmarkList');
const tabs = document.querySelectorAll('.tab');
const tabContents = document.querySelectorAll('.tab-content');
const toast = document.getElementById('toast');

// State
let categories = [];
let currentBookmarks = [];

// Initialize
document.addEventListener('DOMContentLoaded', init);

async function init() {
    await api.loadToken();

    if (api.isLoggedIn()) {
        showMainView();
        await loadPageInfo();
        await loadCategories();
    } else {
        showLoginView();
    }

    setupEventListeners();
}

// View switching
function showLoginView() {
    loginView.style.display = 'block';
    mainView.style.display = 'none';
    logoutBtn.style.display = 'none';
}

function showMainView() {
    loginView.style.display = 'none';
    mainView.style.display = 'block';
    logoutBtn.style.display = 'block';
}

// Event Listeners
function setupEventListeners() {
    // Login form
    loginForm.addEventListener('submit', handleLogin);

    // Logout button
    logoutBtn.addEventListener('click', handleLogout);

    // Bookmark form
    bookmarkForm.addEventListener('submit', handleSaveBookmark);

    // Tab switching
    tabs.forEach(tab => {
        tab.addEventListener('click', () => switchTab(tab.dataset.tab));
    });

    // Search
    let searchTimeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => searchBookmarks(e.target.value), 300);
    });
}

// Login handler
async function handleLogin(e) {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    const btn = loginForm.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.textContent = '登录中...';

    const result = await api.login(email, password);

    btn.disabled = false;
    btn.textContent = '登录';

    if (result.success) {
        showToast('登录成功', 'success');
        showMainView();
        await loadPageInfo();
        await loadCategories();
    } else {
        showToast(result.message, 'error');
    }
}

// Logout handler
async function handleLogout() {
    await api.logout();
    showToast('已退出登录', 'success');
    showLoginView();
    loginForm.reset();
}

// Load current page info
async function loadPageInfo() {
    try {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab) {
            document.getElementById('title').value = tab.title || '';
            document.getElementById('url').value = tab.url || '';

            // Try to get meta description via content script
            try {
                const results = await chrome.scripting.executeScript({
                    target: { tabId: tab.id },
                    func: () => {
                        const metaDesc = document.querySelector('meta[name="description"]');
                        return metaDesc ? metaDesc.content : '';
                    }
                });
                if (results && results[0] && results[0].result) {
                    document.getElementById('description').value = results[0].result;
                }
            } catch (e) {
                // Content script may not work on some pages (chrome://, etc.)
                console.log('Could not get page description:', e);
            }
        }
    } catch (error) {
        console.error('Error loading page info:', error);
    }
}

// Load categories
async function loadCategories() {
    const result = await api.getCategories();
    if (result.success) {
        categories = result.data || [];
        const select = document.getElementById('category');
        select.innerHTML = '<option value="">选择分类...</option>';
        categories.forEach(cat => {
            const option = document.createElement('option');
            option.value = cat.id;
            option.textContent = cat.name;
            select.appendChild(option);
        });
    }
}

// Save bookmark handler
async function handleSaveBookmark(e) {
    e.preventDefault();

    const bookmark = {
        title: document.getElementById('title').value,
        url: document.getElementById('url').value,
        description: document.getElementById('description').value || null,
        categoryId: document.getElementById('category').value || null,
        tags: document.getElementById('tags').value
            ? document.getElementById('tags').value.split(',').map(t => t.trim()).filter(t => t)
            : [],
        isFavorite: document.getElementById('isFavorite').checked ? 1 : 0
    };

    const btn = bookmarkForm.querySelector('button[type="submit"]');
    btn.disabled = true;
    btn.innerHTML = '<svg class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg> 保存中...';

    const result = await api.createBookmark(bookmark);

    btn.disabled = false;
    btn.innerHTML = '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 21H5a2 2 0 01-2-2V5a2 2 0 012-2h11l5 5v11a2 2 0 01-2 2z"/><polyline points="17,21 17,13 7,13 7,21"/><polyline points="7,3 7,8 15,8"/></svg> 保存书签';

    if (result.success) {
        showToast('书签保存成功！', 'success');
        // Reset form fields except URL and title (for easy re-save)
        document.getElementById('description').value = '';
        document.getElementById('tags').value = '';
        document.getElementById('isFavorite').checked = false;
    } else {
        showToast(result.message, 'error');
    }
}

// Tab switching
function switchTab(tabName) {
    tabs.forEach(tab => {
        tab.classList.toggle('active', tab.dataset.tab === tabName);
    });

    tabContents.forEach(content => {
        content.classList.toggle('active', content.id === tabName + 'Tab');
    });

    if (tabName === 'list') {
        loadBookmarks();
    }
}

// Load bookmarks
async function loadBookmarks() {
    bookmarkList.innerHTML = '<div class="loading">加载中...</div>';

    const result = await api.getBookmarks(1, 20);

    if (result.success) {
        currentBookmarks = result.data?.list || result.data || [];
        renderBookmarks(currentBookmarks);
    } else {
        bookmarkList.innerHTML = `<div class="empty-state">${result.message}</div>`;
    }
}

// Search bookmarks
async function searchBookmarks(keyword) {
    if (!keyword.trim()) {
        loadBookmarks();
        return;
    }

    bookmarkList.innerHTML = '<div class="loading">搜索中...</div>';

    const result = await api.searchBookmarks(keyword);

    if (result.success) {
        currentBookmarks = result.data || [];
        renderBookmarks(currentBookmarks);
    } else {
        bookmarkList.innerHTML = `<div class="empty-state">${result.message}</div>`;
    }
}

// Render bookmarks
function renderBookmarks(bookmarks) {
    if (!bookmarks || bookmarks.length === 0) {
        bookmarkList.innerHTML = `
            <div class="empty-state">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M5 4h14a1 1 0 011 1v16.586l-7-5.25-7 5.25V5a1 1 0 011-1z"/>
                </svg>
                <p>暂无书签</p>
            </div>
        `;
        return;
    }

    bookmarkList.innerHTML = bookmarks.map(bookmark => `
        <div class="bookmark-item" data-id="${bookmark.id}" data-url="${bookmark.url}">
            <div class="bookmark-favicon">
                <img src="https://www.google.com/s2/favicons?domain=${encodeURIComponent(bookmark.url)}&sz=32" 
                     onerror="this.style.display='none'"
                     alt="">
            </div>
            <div class="bookmark-info">
                <div class="bookmark-title">${escapeHtml(bookmark.title)}</div>
                <div class="bookmark-url">${escapeHtml(bookmark.url)}</div>
            </div>
            <div class="bookmark-actions">
                <button class="bookmark-action-btn favorite ${bookmark.isFavorite ? 'active' : ''}" 
                        data-id="${bookmark.id}" 
                        data-favorite="${bookmark.isFavorite}"
                        title="收藏">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="${bookmark.isFavorite ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
                        <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14l-5-4.87 6.91-1.01L12 2z"/>
                    </svg>
                </button>
                <button class="bookmark-action-btn delete" data-id="${bookmark.id}" title="删除">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3,6 5,6 21,6"/>
                        <path d="M19,6v14a2,2 0 01-2,2H7a2,2 0 01-2-2V6m3,0V4a2,2 0 012-2h4a2,2 0 012,2v2"/>
                    </svg>
                </button>
            </div>
        </div>
    `).join('');

    // Add click handlers
    bookmarkList.querySelectorAll('.bookmark-item').forEach(item => {
        item.addEventListener('click', (e) => {
            if (!e.target.closest('.bookmark-action-btn')) {
                chrome.tabs.create({ url: item.dataset.url });
            }
        });
    });

    // Favorite button handlers
    bookmarkList.querySelectorAll('.bookmark-action-btn.favorite').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            const id = btn.dataset.id;
            const currentFavorite = btn.dataset.favorite === '1' || btn.dataset.favorite === 'true';
            const newFavorite = currentFavorite ? 0 : 1;

            const result = await api.updateFavorite(id, newFavorite);
            if (result.success) {
                btn.dataset.favorite = newFavorite;
                btn.classList.toggle('active', newFavorite === 1);
                btn.querySelector('svg').setAttribute('fill', newFavorite === 1 ? 'currentColor' : 'none');
                showToast(newFavorite === 1 ? '已添加收藏' : '已取消收藏', 'success');
            }
        });
    });

    // Delete button handlers
    bookmarkList.querySelectorAll('.bookmark-action-btn.delete').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            if (confirm('确定要删除这个书签吗？')) {
                const id = btn.dataset.id;
                const result = await api.deleteBookmark(id);
                if (result.success) {
                    btn.closest('.bookmark-item').remove();
                    showToast('书签已删除', 'success');
                }
            }
        });
    });
}

// Utility functions
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function showToast(message, type = 'success') {
    toast.textContent = message;
    toast.className = `toast ${type} show`;

    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}
