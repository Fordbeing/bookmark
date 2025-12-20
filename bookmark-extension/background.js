// Background Service Worker for BookUtil Extension

const API_BASE_URL = 'http://115.159.219.125:88/api';

// Handle context menu clicks
chrome.contextMenus.onClicked.addListener(async (info, tab) => {
    if (info.menuItemId === 'saveBookmark' || info.menuItemId === 'saveLink') {
        const url = info.linkUrl || info.pageUrl;
        const title = info.menuItemId === 'saveLink' ? (info.linkUrl || tab.title) : tab.title;

        // Check if logged in
        const { token } = await chrome.storage.local.get(['token']);

        if (!token) {
            // Open popup for login
            chrome.action.openPopup();
            return;
        }

        // Save bookmark directly
        try {
            const response = await fetch(`${API_BASE_URL}/bookmarks`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    title: title,
                    url: url,
                    description: null,
                    categoryId: null,
                    tags: [],
                    isFavorite: 0
                })
            });

            const data = await response.json();

            if (data.code === 200) {
                // Show success notification
                chrome.notifications.create({
                    type: 'basic',
                    iconUrl: 'icons/icon128.png',
                    title: 'BookUtil',
                    message: '书签保存成功！'
                });
            } else {
                chrome.notifications.create({
                    type: 'basic',
                    iconUrl: 'icons/icon128.png',
                    title: 'BookUtil',
                    message: data.message || '保存失败'
                });
            }
        } catch (error) {
            console.error('Error saving bookmark:', error);
            chrome.notifications.create({
                type: 'basic',
                iconUrl: 'icons/icon128.png',
                title: 'BookUtil',
                message: '网络错误，请稍后重试'
            });
        }
    }
});

// Handle keyboard shortcut
chrome.commands.onCommand.addListener(async (command) => {
    if (command === 'save-bookmark') {
        // Get current tab
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });

        if (!tab) return;

        // Check if logged in
        const { token } = await chrome.storage.local.get(['token']);

        if (!token) {
            // Open popup for login
            chrome.action.openPopup();
            return;
        }

        // Save bookmark directly
        try {
            const response = await fetch(`${API_BASE_URL}/bookmarks`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    title: tab.title,
                    url: tab.url,
                    description: null,
                    categoryId: null,
                    tags: [],
                    isFavorite: 0
                })
            });

            const data = await response.json();

            if (data.code === 200) {
                chrome.notifications.create({
                    type: 'basic',
                    iconUrl: 'icons/icon128.png',
                    title: 'BookUtil',
                    message: `已保存: ${tab.title.substring(0, 30)}...`
                });
            } else {
                chrome.notifications.create({
                    type: 'basic',
                    iconUrl: 'icons/icon128.png',
                    title: 'BookUtil',
                    message: data.message || '保存失败'
                });
            }
        } catch (error) {
            console.error('Error saving bookmark:', error);
            chrome.notifications.create({
                type: 'basic',
                iconUrl: 'icons/icon128.png',
                title: 'BookUtil',
                message: '网络错误，请稍后重试'
            });
        }
    }
});

// Listen for messages from popup
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === 'BOOKMARK_SAVED') {
        // Could be used for badge updates, etc.
        console.log('Bookmark saved:', message.data);
    }
    if (message.type === 'CHECK_UPDATE') {
        checkForUpdates();
    }
    return true;
});

// ========== 版本更新检查 ==========
const VERSION_CHECK_URL = 'http://115.159.219.125:88/extension-version.json';
const CURRENT_VERSION = '1.0.0';

// 比较版本号
function compareVersions(v1, v2) {
    const parts1 = v1.split('.').map(Number);
    const parts2 = v2.split('.').map(Number);

    for (let i = 0; i < Math.max(parts1.length, parts2.length); i++) {
        const p1 = parts1[i] || 0;
        const p2 = parts2[i] || 0;
        if (p1 > p2) return 1;
        if (p1 < p2) return -1;
    }
    return 0;
}

// 检查更新
async function checkForUpdates() {
    try {
        const response = await fetch(VERSION_CHECK_URL + '?t=' + Date.now(), {
            cache: 'no-store'
        });

        if (!response.ok) return;

        const data = await response.json();
        const latestVersion = data.version;

        if (compareVersions(latestVersion, CURRENT_VERSION) > 0) {
            // 有新版本
            const { lastNotifiedVersion } = await chrome.storage.local.get(['lastNotifiedVersion']);

            // 避免重复通知同一版本
            if (lastNotifiedVersion !== latestVersion) {
                chrome.notifications.create('update-available', {
                    type: 'basic',
                    iconUrl: 'icons/icon128.svg',
                    title: 'BookUtil 有新版本！',
                    message: `新版本 v${latestVersion} 可用。${data.changelog || ''}`,
                    buttons: [
                        { title: '立即更新' },
                        { title: '稍后提醒' }
                    ],
                    requireInteraction: true
                });

                // 保存更新信息供弹窗使用
                await chrome.storage.local.set({
                    updateAvailable: true,
                    latestVersion: latestVersion,
                    changelog: data.changelog,
                    downloadUrl: data.downloadUrl
                });
            }
        } else {
            // 没有更新
            await chrome.storage.local.set({ updateAvailable: false });
        }
    } catch (error) {
        console.error('检查更新失败:', error);
    }
}

// 通知按钮点击处理
chrome.notifications.onButtonClicked.addListener(async (notificationId, buttonIndex) => {
    if (notificationId === 'update-available') {
        if (buttonIndex === 0) {
            // 立即更新 - 打开扩展管理页面
            chrome.tabs.create({ url: 'chrome://extensions/' });

            // 记录已通知的版本
            const { latestVersion } = await chrome.storage.local.get(['latestVersion']);
            await chrome.storage.local.set({ lastNotifiedVersion: latestVersion });
        }
        chrome.notifications.clear(notificationId);
    }
});

// 安装/更新时检查
chrome.runtime.onInstalled.addListener((details) => {
    // 创建右键菜单
    chrome.contextMenus.create({
        id: 'saveBookmark',
        title: '保存到 BookUtil',
        contexts: ['page', 'link']
    });

    chrome.contextMenus.create({
        id: 'saveLink',
        title: '保存链接到 BookUtil',
        contexts: ['link']
    });

    // 首次安装或更新后检查新版本
    if (details.reason === 'install' || details.reason === 'update') {
        setTimeout(checkForUpdates, 3000);
    }
});

// 定时检查更新（每6小时）
chrome.alarms.create('checkUpdates', { periodInMinutes: 360 });

chrome.alarms.onAlarm.addListener((alarm) => {
    if (alarm.name === 'checkUpdates') {
        checkForUpdates();
    }
});
