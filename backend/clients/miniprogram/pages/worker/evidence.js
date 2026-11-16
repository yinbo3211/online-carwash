const api = require('../../utils/api.js');

const STAGE_DEF = [
  { key: 'E1', title: 'E1 接车预检', desc: '核对车辆外观、里程与随身物品，拍摄接车证据。' },
  { key: 'E2', title: 'E2 外观清洗', desc: '高压冲洗、中性泡沫、轮毂清洁，拍摄外观成果。' },
  { key: 'E3', title: 'E3 内饰清洁', desc: '吸尘、缝隙刷洗、中控擦拭，拍摄内饰成果。' },
  { key: 'E4', title: 'E4 交车确认', desc: '与用户复核车辆状态，拍摄交车合影并确认。' }
];

Page({
  data: { orderNo: '', stages: [] },

  onLoad(query) {
    this.orderNo = query.orderNo || 'WC20260616-0091';
    this.setData({
      orderNo: this.orderNo,
      stages: STAGE_DEF.map((s, i) => Object.assign({}, s, {
        photos: [], done: false, unlocked: i === 0
      }))
    });
  },

  onShoot(e) {
    const index = Number(e.currentTarget.dataset.index);
    // 强制相机：sourceType 仅 camera，杜绝相册旧图
    wx.chooseMedia({
      count: 4,
      mediaType: ['image'],
      sourceType: ['camera'],
      success: (res) => {
        const files = (res.tempFiles || []).map(f => f.tempFilePath);
        this._upload(index, files);
      }
    });
  },

  onAlbumTip() {
    // 相册入口灰显：文案固定为「仅支持现场拍摄」
    wx.showToast({ title: '仅支持现场拍摄', icon: 'none' });
  },

  _upload(index, files) {
    wx.showLoading({ title: '上传中' });
    Promise.all(files.map(f => api.uploadEvidence(f, this.orderNo, STAGE_DEF[index].key)))
      .then(urls => {
        wx.hideLoading();
        const stages = this.data.stages.slice();
        stages[index] = Object.assign({}, stages[index], {
          photos: stages[index].photos.concat(urls.map(() => '📷'))
        });
        this.setData({ stages });
        wx.showToast({ title: '证据已上传（模拟水印：' + this.orderNo + '）', icon: 'none' });
      })
      .catch(() => {
        wx.hideLoading();
        const stages = this.data.stages.slice();
        stages[index] = Object.assign({}, stages[index], {
          photos: stages[index].photos.concat(files.map(() => '📷'))
        });
        this.setData({ stages });
        wx.showToast({ title: '已本地留证（模拟）', icon: 'none' });
      });
  },

  onDone(e) {
    const index = Number(e.currentTarget.dataset.index);
    const stages = this.data.stages.slice();
    stages[index] = Object.assign({}, stages[index], { done: true });
    if (stages[index + 1]) stages[index + 1] = Object.assign({}, stages[index + 1], { unlocked: true });
    this.setData({ stages });

    api.worker.submitEvidence({
      orderNo: this.orderNo,
      stage: STAGE_DEF[index].key,
      photos: stages[index].photos
    }).catch(() => {});

    wx.showToast({ title: STAGE_DEF[index].title + ' 已完成（模拟）', icon: 'none' });
  }
});
