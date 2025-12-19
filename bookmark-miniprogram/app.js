// app.js
App({
  globalData: {
    userInfo: null,
    token: null,
    // 开发环境使用 127.0.0.1（模拟器中 localhost 不工作，但 127.0.0.1 可以）
    // 真机调试时需要改成电脑的局域网 IP
    baseUrl: 'http://127.0.0.1:8080/api'
  },

  onLaunch() {
    // 检查登录状态
    this.checkLoginStatus();
  },

  checkLoginStatus() {
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');

    if (token && userInfo) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      return true;
    }
    return false;
  },

  // 保存登录信息
  setLoginInfo(token, userInfo) {
    this.globalData.token = token;
    this.globalData.userInfo = userInfo;
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', userInfo);
  },

  // 清除登录信息
  clearLoginInfo() {
    this.globalData.token = null;
    this.globalData.userInfo = null;
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
  },

  // 跳转到登录页
  navigateToLogin() {
    wx.navigateTo({
      url: '/pages/login/login'
    });
  }
});
