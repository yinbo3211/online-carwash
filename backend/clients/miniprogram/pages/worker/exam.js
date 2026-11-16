const api = require('../../utils/api.js');

// 题库口径与产品原型一致：80 分及格、V1 橙色模拟支付、L1 不可取消、工位已满为危险色
const QUESTIONS = [
  { id: 1, q: '洗车作业开始前，第一步应当做什么？', options: ['环车检查并确认随身物品', '直接高压冲洗', '先抛光打蜡', '先向用户收费'], answer: 0 },
  { id: 2, q: '高压水枪冲洗车身时，推荐的水流角度是？', options: ['约 45° 斜向冲洗', '垂直 90° 直冲', '贴面 0° 平扫', '任意角度均可'], answer: 0 },
  { id: 3, q: 'L1 基础保险的正确描述是？', options: ['平台强制保障，不可取消', '用户可随时取消', '仅精细洗包含', '需额外付费'], answer: 0 },
  { id: 4, q: 'C3 精细认证的在线考试及格线是？', options: ['80 分', '60 分', '70 分', '90 分'], answer: 0 },
  { id: 5, q: '现场证据拍摄要求正确的是？', options: ['必须现场拍摄并带水印', '可用相册旧图', '只需拍 1 张', '无需订单号'], answer: 0 },
  { id: 6, q: '订单 Stepper 中「待验收」使用的语义色是？', options: ['警告色', '成功色', '危险色', '信息色'], answer: 0 },
  { id: 7, q: '取车信息页中联系手机应满足？', options: ['11 位中国大陆手机号', '任意长度数字', '必须含横线', '可为空'], answer: 0 },
  { id: 8, q: '增值项目最多可选几项？', options: ['4 项', '2 项', '6 项', '不限'], answer: 0 },
  { id: 9, q: '工位状态「已满」对应的指示色是？', options: ['危险色 #E5484D', '成功色 #18A058', '灰色 #97A3B6', '警告色 #F0A020'], answer: 0 },
  { id: 10, q: 'V1 测试版支付成功态应使用什么颜色？', options: ['橙色 #F5901B', '绿色 #18A058', '蓝色 #1668E3', '灰色'], answer: 0 }
];

Page({
  data: {
    cert: 'C3', questions: [], submitted: false, passed: false, score: 0
  },

  onLoad(query) {
    this.setData({ cert: query.cert || 'C3' });
    this.reset();
    this.load();
  },

  load() {
    api.worker.getExams().then(data => {
      if (data && Array.isArray(data.questions) && data.questions.length) {
        this.setData({ questions: data.questions.map(q => Object.assign({}, q, { selected: null })) });
      }
    }).catch(() => {});
  },

  reset() {
    this.setData({
      questions: QUESTIONS.map(q => Object.assign({}, q, { selected: null })),
      submitted: false, passed: false, score: 0
    });
  },

  onPick(e) {
    const qi = Number(e.currentTarget.dataset.q);
    const oi = Number(e.currentTarget.dataset.o);
    const questions = this.data.questions.slice();
    questions[qi] = Object.assign({}, questions[qi], { selected: oi });
    this.setData({ questions });
  },

  onSubmit() {
    const unanswered = this.data.questions.filter(q => q.selected === null || q.selected === undefined);
    if (unanswered.length) {
      wx.showToast({ title: '还有 ' + unanswered.length + ' 题未作答', icon: 'none' });
      return;
    }
    // q.answer = 正确答案索引；q.selected = 用户作答索引。生产以服务端判分为准。
    const score = this.data.questions.reduce((s, q) => s + (q.selected === q.answer ? 10 : 0), 0);
    const passed = score >= 80;
    this.setData({ submitted: true, score, passed });

    api.worker.submitExam({ cert: this.data.cert, score, answers: this.data.questions.map(q => q.selected) })
      .catch(() => {});

    wx.showModal({
      title: passed ? '考试通过' : '考试未通过',
      content: passed ? 'C3 精细认证已解锁，可承接高阶订单（模拟）。' : '未达 80 分，请复习后重新考试。',
      showCancel: false
    });
  },

  onReset() { this.reset(); }
});
