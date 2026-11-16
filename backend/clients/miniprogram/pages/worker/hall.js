const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

const DEMO_CERTS = [
  { key: 'C1', name: '基础洗车', passed: true },
  { key: 'C2', name: '标准洗车', passed: true },
  { key: 'C3', name: '精细洗车', passed: false },
  { key: 'D', name: '驾驶资格', passed: true },
  { key: 'O', name: '操作规范', passed: true }
];

// 演示任务：精细清洁订单对未持 C3 的洗车工置灰锁定
const DEMO_TASKS = [
  { orderNo: 'WC20260616-0091', tier: 'standard', serviceName: '标准清洁', modeText: '到店洗', address: '福田区莲花路', feeFen: 5320, countdown: 90, requiredCert: 'C2' },
  { orderNo: 'WC20260616-0092', tier: 'premium', serviceName: '精细清洁', modeText: '上门取送', address: '南山区科技园', feeFen: 9720, countdown: 90, requiredCert: 'C3' },
  { orderNo: 'WC20260616-0093', tier: 'basic', serviceName: '外观清洁', modeText: '到店洗', address: '罗湖区人民南路', feeFen: 3320, countdown: 90, requiredCert: 'C1' }
];

Page({
  data: { certs: DEMO_CERTS, tasks: [], refreshText: '' },
  timer: null,

  onLoad() {
    this.build(DEMO_TASKS);
    this.load();
    this.startCountdown();
  },

  onUnload() { if (this.timer) clearInterval(this.timer); },

  build(list) {
    const passed = {};
    this.data.certs.forEach(c => { passed[c.key] = c.passed; });
    const tasks = list.map(t => {
      const locked = !passed[t.requiredCert];
      return Object.assign({}, t, {
        locked,
        feeText: C.fen2Yuan(t.feeFen),
        countdown: t.countdown || 90
      });
    });
    this.setData({
      tasks,
      refreshText: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    });
  },

  load() {
    Promise.all([api.worker.getCerts(), api.worker.getHall()]).then(([certs, hall]) => {
      if (certs && Array.isArray(certs.list)) this.setData({ certs: certs.list });
      if (hall && Array.isArray(hall.tasks) && hall.tasks.length) this.build(hall.tasks);
    }).catch(() => {});
  },

  startCountdown() {
    this.timer = setInterval(() => {
      const tasks = this.data.tasks.map(t => Object.assign({}, t, {
        countdown: t.countdown > 0 ? t.countdown - 1 : 90
      }));
      this.setData({ tasks });
    }, 1000);
  },

  onGrab(e) {
    const orderNo = e.currentTarget.dataset.order;
    wx.showModal({
      title: '确认抢单',
      content: '订单 ' + orderNo + ' 将在 90 秒内锁定，确认抢单？（模拟）',
      success: (r) => {
        if (!r.confirm) return;
        api.worker.grabOrder(orderNo).then(() => {
          wx.showToast({ title: '抢单成功（模拟）', icon: 'none' });
          this.setData({ tasks: this.data.tasks.filter(t => t.orderNo !== orderNo) });
        }).catch(() => {
          wx.showToast({ title: '抢单成功（模拟）', icon: 'none' });
          this.setData({ tasks: this.data.tasks.filter(t => t.orderNo !== orderNo) });
        });
      }
    });
  },

  goCert(e) {
    const cert = e.currentTarget.dataset.cert;
    if (cert === 'C3') {
      wx.navigateTo({ url: '/pages/worker/exam?cert=C3' });
      return;
    }
    wx.showModal({
      title: cert + ' 补认证',
      content: '请提交对应资质材料至平台审核（模拟）。',
      showCancel: false
    });
  }
});
