const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

// 演示数据：后端未就绪时仍可查看 8 态 Stepper 与证据区
const DEMO_ORDER = {
  orderNo: 'WC20260616-0083',
  tier: 'standard', serviceName: '标准清洁',
  modeText: '上门取送', statusIndex: 6,
  workerName: '王师傅', carNo: '粤B·D12345',
  amountFen: 14800, l2: true
};
const DEMO_EVIDENCE = [
  { stage: 'E1 接车预检', photos: ['🚗', '📷'] },
  { stage: 'E2 外观清洗', photos: ['🫧', '✨'] },
  { stage: 'E3 内饰清洁', photos: ['🧽', '🪑'] },
  { stage: 'E4 交车确认', photos: ['🤝', '📷'] }
];

Page({
  data: {
    order: {}, steps: [], evidence: DEMO_EVIDENCE,
    statusIndex: 6, statusLabel: '待验收', statusToneClass: 'tag-warning',
    certBlocked: false, requiredCert: ''
  },

  onLoad(query) {
    this.orderId = query.orderId || DEMO_ORDER.orderNo;
    this.applyOrder(DEMO_ORDER);
    this.load();
  },

  load() {
    api.user.getOrder(this.orderId).then(data => {
      if (data && data.order) this.applyOrder(Object.assign({}, DEMO_ORDER, data.order));
      if (data && Array.isArray(data.evidence) && data.evidence.length) this.setData({ evidence: data.evidence });
    }).catch(() => {});
  },

  applyOrder(o) {
    const statusIndex = Math.min(7, Math.max(0, Number(o.statusIndex != null ? o.statusIndex : 6)));
    const status = C.ORDER_STATUS[statusIndex];
    const tier = C.SERVICE_TIERS.find(t => t.key === o.tier) || C.SERVICE_TIERS[1];
    const steps = C.ORDER_STATUS.map((s, i) => ({
      key: s.key,
      label: s.label,
      state: i < statusIndex ? 'done' : (i === statusIndex ? 'current' : '')
    }));
    const toneClassMap = { warning: 'tag-warning', success: 'tag-success', primary: 'tag-primary', info: 'tag-primary' };
    const certMap = { basic: 'C1', standard: 'C2', premium: 'C3' };
    this.setData({
      order: Object.assign({}, o, {
        serviceName: o.serviceName || tier.name,
        amountText: C.fen2Yuan(o.amountFen)
      }),
      steps, statusIndex,
      statusLabel: status.label,
      statusToneClass: toneClassMap[status.tone] || 'tag-primary',
      requiredCert: certMap[o.tier] || 'C1',
      certBlocked: !!o.certBlocked
    });
  },

  onContact() {
    wx.showToast({ title: '已拨打洗车工（模拟）', icon: 'none' });
  },

  onAccept() {
    wx.showModal({
      title: '确认验收',
      content: '确认车辆与清洁效果无误？确认后订单进入「完成」（模拟）。',
      success: (r) => {
        if (!r.confirm) return;
        this.setData({ statusIndex: 7, statusLabel: '完成', statusToneClass: 'tag-success' });
        const steps = this.data.steps.map(s => Object.assign({}, s, { state: 'done' }));
        this.setData({ steps });
        wx.showToast({ title: '验收完成（模拟）', icon: 'none' });
      }
    });
  },

  goReview() {
    wx.navigateTo({ url: '/pages/store/review?orderNo=' + encodeURIComponent(this.orderId) });
  }
});
