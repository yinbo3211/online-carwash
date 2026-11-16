const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

const DEMO_BAYS = [
  { id: 'B1', name: 'Bay-01', status: 'free' },
  { id: 'B2', name: 'Bay-02', status: 'full' },
  { id: 'B3', name: 'Bay-03', status: 'paused' }
];
const DEMO_TASKS = [
  { orderNo: 'WC20260616-0101', tier: 'standard', serviceName: '标准清洁', modeText: '到店洗', bayFeeFen: 2000 },
  { orderNo: 'WC20260616-0102', tier: 'basic', serviceName: '外观清洁', modeText: '到店洗', bayFeeFen: 1500 }
];

Page({
  data: { bays: [], tasks: [], legend: C.BAY_STATUS, todayFeeText: '0.00' },

  onLoad() {
    this.applyBays(DEMO_BAYS);
    this.applyTasks(DEMO_TASKS);
    this.load();
  },

  applyBays(list) {
    const map = {};
    C.BAY_STATUS.forEach(s => { map[s.key] = s.label; });
    const bays = list.map(b => Object.assign({}, b, {
      statusText: map[b.status] || '未知',
      subText: b.status === 'free' ? '可接单' : (b.status === 'full' ? '已满 · 不可接' : '暂停服务')
    }));
    this.setData({ bays });
  },

  applyTasks(list) {
    const tasks = list.map(t => Object.assign({}, t, { bayFeeText: C.fen2Yuan(t.bayFeeFen) }));
    const todayFen = tasks.reduce((s, t) => s + t.bayFeeFen, 0);
    this.setData({ tasks, todayFeeText: C.fen2Yuan(todayFen) });
  },

  load() {
    Promise.all([api.store.getBays(), api.store.getReviews({ page: 1, pageSize: 5 })]).then(([bayData]) => {
      if (bayData && Array.isArray(bayData.bays) && bayData.bays.length) this.applyBays(bayData.bays);
    }).catch(() => {});
  },

  onBayTap(e) {
    const id = e.currentTarget.dataset.id;
    const bays = this.data.bays.map(b => {
      if (b.id !== id) return b;
      const order = ['free', 'full', 'paused'];
      const next = order[(order.indexOf(b.status) + 1) % order.length];
      return Object.assign({}, b, { status: next });
    });
    this.applyBays(bays);
    const changed = bays.find(b => b.id === id);
    api.store.updateBay(id, { status: changed.status }).catch(() => {});
    wx.showToast({ title: id + ' → ' + changed.statusText, icon: 'none' });
  },

  onAccept(e) {
    const orderNo = e.currentTarget.dataset.order;
    const hasFree = this.data.bays.some(b => b.status === 'free');
    if (!hasFree) {
      // 无空位必须拦截，禁止强制接单
      wx.showModal({
        title: '当前无空位',
        content: '有空位工位为 0。请返回工作台切换工位状态后重试。',
        showCancel: false,
        confirmText: '知道了'
      });
      return;
    }
    wx.showModal({
      title: '确认接单',
      content: orderNo + ' 将占用 1 个空位，接单后工位状态变为「已满」（模拟）。',
      success: (r) => {
        if (!r.confirm) return;
        const bays = this.data.bays.map(b => b.status === 'free' ? Object.assign({}, b, { status: 'full' }) : b);
        this.applyBays(bays);
        this.setData({ tasks: this.data.tasks.filter(t => t.orderNo !== orderNo) });
        api.store.acceptOrder(orderNo, {}).catch(() => {});
        wx.showToast({ title: '接单成功（模拟）', icon: 'none' });
      }
    });
  }
});
