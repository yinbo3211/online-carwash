const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

const DEMO_BILLS = [
  { id: 1, title: '标准清洁 · 分成', no: 'WC20260616-0083', date: '06-16', amountFen: 5320, feeFen: 4655 },
  { id: 2, title: '外观清洁 · 分成', no: 'WC20260615-0061', date: '06-15', amountFen: 3320, feeFen: 2905 },
  { id: 3, title: '精细清洁 · 分成', no: 'WC20260614-0042', date: '06-14', amountFen: 9720, feeFen: 8505 }
];

Page({
  data: {
    withdrawText: '0.00', totalText: '0.00',
    withdrawFen: 18360, totalFen: 18360, bills: []
  },

  onLoad() {
    this.apply(DEMO_BILLS);
    this.load();
  },

  apply(bills) {
    const list = bills.map(b => Object.assign({}, b, {
      amountText: C.fen2Yuan(b.amountFen),
      feeText: C.fen2Yuan(b.feeFen)
    }));
    const totalFen = list.reduce((s, b) => s + b.amountFen, 0);
    this.setData({
      bills: list,
      totalFen,
      totalText: C.fen2Yuan(totalFen),
      withdrawText: C.fen2Yuan(totalFen)
    });
  },

  load() {
    api.worker.getIncome().then(data => {
      if (data && Array.isArray(data.bills) && data.bills.length) this.apply(data.bills);
    }).catch(() => {});
  },

  onWithdraw() {
    wx.showModal({
      title: '模拟提现',
      content: C.SIM_SUCCESS_FULL + '。提现金额 ' + this.data.withdrawText + ' 元（示例）。',
      showCancel: false,
      confirmText: '知道了'
    });
  }
});
