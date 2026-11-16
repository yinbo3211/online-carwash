const api = require('../../utils/api.js');
const C = require('../../utils/constants.js');

Page({
  data: {
    orderNo: '', dims: [], lowScore: C.LOW_SCORE, comment: ''
  },

  onLoad(query) {
    this.setData({
      orderNo: query.orderNo || 'WC20260616-0083',
      dims: C.REVIEW_DIMS.map(d => Object.assign({}, d, { score: 0, reason: '' }))
    });
  },

  onStar(e) {
    const di = Number(e.currentTarget.dataset.dim);
    const score = Number(e.currentTarget.dataset.score);
    const dims = this.data.dims.slice();
    dims[di] = Object.assign({}, dims[di], { score });
    this.setData({ dims });
  },

  onReason(e) {
    const di = Number(e.currentTarget.dataset.dim);
    const dims = this.data.dims.slice();
    dims[di] = Object.assign({}, dims[di], { reason: e.detail.value });
    this.setData({ dims });
  },

  onComment(e) { this.setData({ comment: e.detail.value }); },

  onSubmit() {
    const missing = this.data.dims.filter(d => !d.score);
    if (missing.length) {
      wx.showToast({ title: '请完成全部五维评分', icon: 'none' }); return;
    }
    const lowNoReason = this.data.dims.filter(d => d.score <= C.LOW_SCORE && !d.reason.trim());
    if (lowNoReason.length) {
      wx.showToast({ title: '低分维度需填写原因', icon: 'none' }); return;
    }
    const payload = {
      orderNo: this.data.orderNo,
      scores: this.data.dims.map(d => ({ dim: d.key, score: d.score, reason: d.reason })),
      comment: this.data.comment
    };
    api.store.replyReview(this.data.orderNo, payload).catch(() => {});
    wx.showModal({
      title: '评价已提交',
      content: '五维评价已提交（模拟）。',
      showCancel: false,
      success: () => wx.navigateBack()
    });
  }
});
