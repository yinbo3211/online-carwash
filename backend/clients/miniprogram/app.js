/**
 * 线上洗车平台 v6.0 · 微信小程序
 * 全局逻辑：baseURL 集中配置、登录态（token）管理、模拟支付环境标记。
 * 注意：V1 为测试版，所有支付均为模拟，不产生真实资金收付。
 */
App({
  globalData: {
    // 后端 API 统一基址，所有请求经 utils/api.js 拼接，禁止在页面中硬编码域名
    baseURL: '/api/',
    token: '',
    userInfo: null,
    role: 'user', // user | worker | store
    // V1 测试版模拟支付开关：仅控制文案与横幅，不控制真实资金（本版无真实资金通道）
    simPayment: true
  },

  onLaunch() {
    // 从本地缓存恢复登录态（静默），页面按需校验
    try {
      const token = wx.getStorageSync('xc_token');
      const userInfo = wx.getStorageSync('xc_userInfo');
      const role = wx.getStorageSync('xc_role');
      if (token) this.globalData.token = token;
      if (userInfo) this.globalData.userInfo = userInfo;
      if (role) this.globalData.role = role;
    } catch (e) {
      console.warn('[app] 读取本地登录态失败', e);
    }
    console.info('[app] launched, simPayment =', this.globalData.simPayment);
  },

  /** 保存登录态（供登录页/后续扩展调用） */
  setLogin(token, userInfo, role) {
    this.globalData.token = token || '';
    this.globalData.userInfo = userInfo || null;
    if (role) this.globalData.role = role;
    try {
      wx.setStorageSync('xc_token', this.globalData.token);
      wx.setStorageSync('xc_userInfo', this.globalData.userInfo);
      wx.setStorageSync('xc_role', this.globalData.role);
    } catch (e) {
      console.warn('[app] 缓存登录态失败', e);
    }
  },

  /** 退出登录 */
  logout() {
    this.globalData.token = '';
    this.globalData.userInfo = null;
    try {
      wx.removeStorageSync('xc_token');
      wx.removeStorageSync('xc_userInfo');
    } catch (e) {}
  }
});
