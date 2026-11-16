const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

Page({
  data: {
    tiers: [],
    selectedTier: 'standard'
  },

  onLoad() {
    // 价格由「分」转「元」展示，示例值
    const tiers = C.SERVICE_TIERS.map(t => Object.assign({}, t, { priceText: C.fen2Yuan(t.priceFen) }));
    this.setData({ tiers });
    this.loadHome();
  },

  loadHome() {
    // 后端 ready 后可用 home 接口覆盖（如活动价）；失败静默保留本地示例价
    api.user.getHome().then(data => {
      if (data && Array.isArray(data.tiers) && data.tiers.length) {
        const tiers = data.tiers.map(t => Object.assign({}, t, {
          priceText: C.fen2Yuan(t.priceFen != null ? t.priceFen : t.price)
        }));
        this.setData({ tiers });
      }
    }).catch(() => {});
  },

  onTierTap(e) {
    const tier = e.currentTarget.dataset.tier;
    this.setData({ selectedTier: tier });
    wx.navigateTo({ url: '/pages/user/order-create?tier=' + tier });
  },

  goCreate() {
    wx.navigateTo({ url: '/pages/user/order-create?tier=' + this.data.selectedTier });
  },

  goProgress() {
    // 演示用订单号；真实场景从订单列表带入
    wx.navigateTo({ url: '/pages/user/order-progress?orderId=WC20260616-0083' });
  },

  goPayDemo() {
    wx.showModal({
      title: '模拟支付说明',
      content: C.SIM_SUCCESS_FULL + '。V1 测试版不接入真实支付通道。',
      showCancel: false,
      confirmText: '知道了'
    });
  }
});
