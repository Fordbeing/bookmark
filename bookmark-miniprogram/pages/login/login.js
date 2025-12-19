// pages/login/login.js
const { loginAPI, registerAPI } = require('../../api/auth');

Page({
    data: {
        isLogin: true,
        loading: false,
        loginForm: {
            email: '',
            password: '',
            remember: false
        },
        registerForm: {
            username: '',
            email: '',
            password: '',
            confirmPassword: ''
        }
    },

    onLoad() {
        // 检查是否已登录
        const token = wx.getStorageSync('token');
        if (token) {
            this.navigateBack();
        }
    },

    // 切换到登录
    switchToLogin() {
        this.setData({ isLogin: true });
    },

    // 切换到注册
    switchToRegister() {
        this.setData({ isLogin: false });
    },

    // 登录表单输入
    onLoginEmailInput(e) {
        this.setData({ 'loginForm.email': e.detail.value });
    },

    onLoginPasswordInput(e) {
        this.setData({ 'loginForm.password': e.detail.value });
    },

    onRememberChange(e) {
        this.setData({ 'loginForm.remember': e.detail.value });
    },

    // 注册表单输入
    onRegisterUsernameInput(e) {
        this.setData({ 'registerForm.username': e.detail.value });
    },

    onRegisterEmailInput(e) {
        this.setData({ 'registerForm.email': e.detail.value });
    },

    onRegisterPasswordInput(e) {
        this.setData({ 'registerForm.password': e.detail.value });
    },

    onRegisterConfirmPasswordInput(e) {
        this.setData({ 'registerForm.confirmPassword': e.detail.value });
    },

    // 登录
    async handleLogin() {
        const { email, password } = this.data.loginForm;

        if (!email || !password) {
            wx.showToast({ title: '请输入邮箱和密码', icon: 'none' });
            return;
        }

        this.setData({ loading: true });

        try {
            const result = await loginAPI({ email, password });

            if (result.data && result.data.token) {
                // 保存登录信息
                const app = getApp();
                app.setLoginInfo(result.data.token, result.data);

                wx.showToast({ title: '登录成功', icon: 'success' });

                // 返回上一页或跳转首页
                setTimeout(() => {
                    this.navigateBack();
                }, 1500);
            }
        } catch (error) {
            console.error('登录失败:', error);
        } finally {
            this.setData({ loading: false });
        }
    },

    // 注册
    async handleRegister() {
        const { username, email, password, confirmPassword } = this.data.registerForm;

        if (!username || !email || !password) {
            wx.showToast({ title: '请填写所有字段', icon: 'none' });
            return;
        }

        if (password !== confirmPassword) {
            wx.showToast({ title: '两次输入的密码不一致', icon: 'none' });
            return;
        }

        if (password.length < 6) {
            wx.showToast({ title: '密码长度至少6位', icon: 'none' });
            return;
        }

        this.setData({ loading: true });

        try {
            const result = await registerAPI({ username, email, password });

            if (result.data && result.data.token) {
                // 保存登录信息
                const app = getApp();
                app.setLoginInfo(result.data.token, result.data);

                wx.showToast({ title: '注册成功', icon: 'success' });

                // 返回上一页或跳转首页
                setTimeout(() => {
                    this.navigateBack();
                }, 1500);
            }
        } catch (error) {
            console.error('注册失败:', error);
        } finally {
            this.setData({ loading: false });
        }
    },

    // 返回
    navigateBack() {
        const pages = getCurrentPages();
        if (pages.length > 1) {
            wx.navigateBack();
        } else {
            wx.switchTab({ url: '/pages/index/index' });
        }
    }
});
