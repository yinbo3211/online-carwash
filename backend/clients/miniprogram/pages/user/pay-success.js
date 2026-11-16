const C = require('../../utils/constants.js');

Page({
  data: {
    orderNo: '',
    serviceName: '标准清洁',
    amountText: '0.00'
  },

  onLoad(query) {
    const amountFen = Number(query.amountFen || 0);
    this.setData({
      orderNo: decodeURIComponent(query.orderId || 'WC20260616-0083'),
      serviceName: decodeURIComponent(query.serviceName || '标准清洁'),
      amountText: C.fen2Yuan(amountFen)
    });
    // 再次强调口径：成功态使用橙色文案，禁止绿色「支付成功」
    console.info('[pay]', C.SIM_SUCCESS_FULL);
  },

  goProgress() {
    wx.redirectTo({ url: '/pages/user/order-progress?orderId=' + encodeURIComponent(this.data.orderNo) });
  },

  goHome() {
    wx.switchTab({ url: '/pages/user/index' });
  }
});
