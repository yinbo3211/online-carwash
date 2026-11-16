const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

Page({
  data: {
    tiers: [], selectedTier: 'standard', serviceMode: 'store',
    address: '', phone: '',
    addons: [], addonCount: 0,
    l1: C.INSURANCE.L1, l2: C.INSURANCE.L2, l2Checked: false,
    couponOn: true, couponFen: 1000,
    maxAddons: C.MAX_ADDONS, doorDisabled: false,
    baseText: '0.00', addonSumText: '0.00', l2SumText: '0.00',
    couponText: '0.00', totalText: '0.00', doorPriceText: '0.00'
  },

  onLoad(query) {
    const tiers = C.SERVICE_TIERS.map(t => Object.assign({}, t, { priceText: C.fen2Yuan(t.priceFen) }));
    const addons = C.ADDONS.map(a => Object.assign({}, a, { checked: false, priceText: C.fen2Yuan(a.priceFen) }));
    this.setData({
      tiers, addons,
      selectedTier: query.tier || 'standard',
      couponText: C.fen2Yuan(this.data.couponFen),
      l2: Object.assign({}, C.INSURANCE.L2, { priceText: C.fen2Yuan(C.INSURANCE.L2.priceFen) })
    }, this.recalc);
  },

  /* ---------- 交互 ---------- */
  onTierTap(e) { this.setData({ selectedTier: e.currentTarget.dataset.tier }, this.recalc); },
  onModeTap(e) {
    const mode = e.currentTarget.dataset.mode;
    if (mode === 'door' && this.data.doorDisabled) {
      wx.showToast({ title: '精细清洁暂不支持上门', icon: 'none' });
      return;
    }
    this.setData({ serviceMode: mode }, this.recalc);
  },
  onAddressInput(e) { this.setData({ address: e.detail.value }); },
  onPhoneInput(e) { this.setData({ phone: e.detail.value }); },
  onAddonTap(e) {
    const key = e.currentTarget.dataset.key;
    const addons = this.data.addons.map(a => {
      if (a.key !== key) return a;
      if (!a.checked && this.data.addonCount >= C.MAX_ADDONS) {
        wx.showToast({ title: '最多选 ' + C.MAX_ADDONS + ' 项', icon: 'none' });
        return a;
      }
      return Object.assign({}, a, { checked: !a.checked });
    });
    this.setData({ addons }, this.recalc);
  },
  onL2Tap() { this.setData({ l2Checked: !this.data.l2Checked }, this.recalc); },
  onCouponTap() { this.setData({ couponOn: !this.data.couponOn }, this.recalc); },

  /* ---------- 金额：内部一律用「分」计算 ---------- */
  recalc() {
    const tier = C.SERVICE_TIERS.find(t => t.key === this.data.selectedTier) || C.SERVICE_TIERS[1];
    const doorDisabled = tier.key === 'premium';
    const mode = doorDisabled ? 'store' : this.data.serviceMode;
    const baseFen = mode === 'door'
      ? (C.DOOR_PRICE_FEN[tier.key] || tier.priceFen)
      : tier.priceFen;
    const selectedAddons = this.data.addons.filter(a => a.checked);
    const addonFen = selectedAddons.reduce((s, a) => s + a.priceFen, 0);
    const l2Fen = this.data.l2Checked ? C.INSURANCE.L2.priceFen : 0;
    const couponFen = this.data.couponOn ? this.data.couponFen : 0;
    const totalFen = Math.max(0, baseFen + addonFen + l2Fen - couponFen);

    this.setData({
      doorDisabled, serviceMode: mode,
      addonCount: selectedAddons.length,
      baseText: C.fen2Yuan(baseFen),
      addonSumText: C.fen2Yuan(addonFen),
      l2SumText: C.fen2Yuan(l2Fen),
      couponText: C.fen2Yuan(couponFen),
      doorPriceText: C.fen2Yuan(C.DOOR_PRICE_FEN[tier.key] || tier.priceFen),
      totalText: C.fen2Yuan(totalFen)
    });
    this._amount = { baseFen, addonFen, l2Fen, couponFen, totalFen };
  },

  /* ---------- 提交 ---------- */
  onSubmit() {
    if (this.data.serviceMode === 'door') {
      if (!/^1\d{10}$/.test(this.data.phone)) {
        wx.showToast({ title: '请输入 11 位手机号', icon: 'none' }); return;
      }
      if (!this.data.address.trim()) {
        wx.showToast({ title: '请输入取送地址', icon: 'none' }); return;
      }
    }
    const a = this._amount || {};
    const payload = {
      tier: this.data.selectedTier,
      serviceMode: this.data.serviceMode,
      address: this.data.address,
      phone: this.data.phone,
      addonKeys: this.data.addons.filter(x => x.checked).map(x => x.key),
      insurance: {
        l1Accepted: true,   // L1 强制锁定，用户不可取消
        l1Cancelable: false,
        l2Selected: this.data.l2Checked
      },
      couponFen: a.couponFen || 0,
      amountFen: a.totalFen || 0, // 单位：分
      requestId: 'req_' + Date.now() + '_' + Math.floor(Math.random() * 1e6) // 幂等键
    };

    wx.showLoading({ title: '提交中' });
    api.user.createOrder(payload).then(data => {
      wx.hideLoading();
      const orderId = (data && (data.orderId || data.id)) || this._demoOrderNo();
      wx.redirectTo({ url: '/pages/user/pay-success?orderId=' + encodeURIComponent(orderId) + '&amountFen=' + (a.totalFen || 0) });
    }).catch(err => {
      wx.hideLoading();
      // 后端未就绪时保留演示流程，明确提示为演示数据
      wx.showToast({ title: '后端未就绪，使用演示数据', icon: 'none' });
      const orderId = this._demoOrderNo();
      setTimeout(() => {
        wx.redirectTo({ url: '/pages/user/pay-success?orderId=' + encodeURIComponent(orderId) + '&amountFen=' + (a.totalFen || 0) });
      }, 900);
    });
  },

  _demoOrderNo() {
    const d = new Date();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return 'WC' + d.getFullYear() + mm + dd + '-' + String(Math.floor(Math.random() * 900) + 100);
  }
});
