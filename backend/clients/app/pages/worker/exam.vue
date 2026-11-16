<template>
  <view class="page worker-exam">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card exam-head">
        <view class="card-title"><text>C3 精细认证考试</text><text class="tag tag-warning">及格线 80 分</text></view>
        <view class="muted">共 {{ questions.length }} 题，每题 10 分。通过后解锁精细清洁订单抢单权限。</view>
      </view>

      <view v-for="(q, qi) in questions" :key="q.id" class="card question">
        <view class="q-title">{{ qi + 1 }}. {{ q.q }}</view>
        <view v-for="(opt, oi) in q.options" :key="oi" class="option" :class="{ on: q.selected === oi }" @click="pick(qi, oi)">
          <view class="radio" :class="{ checked: q.selected === oi }"></view>
          <text>{{ opt }}</text>
        </view>
      </view>

      <view v-if="submitted" class="card">
        <view class="result-score" :class="passed ? 'pass' : 'fail'">{{ score }} 分</view>
        <view class="muted center">{{ passed ? '考试通过，C3 精细认证已解锁，可承接高阶订单。' : '未达 80 分，请复习后重新考试。' }}</view>
      </view>

      <view class="btn btn-primary btn-block" @click="submit">提交试卷</view>
      <view class="btn btn-ghost btn-block mt-16" @click="reset">重新考试</view>
    </view>
  </view>
</template>

<script>
import api from '../../common/api.js';

const QUESTIONS = [
  { id: 1, q: '洗车作业开始前，第一步应当做什么？', options: ['环车检查并确认随身物品', '直接高压冲洗', '先抛光打蜡', '先向用户收费'], answer: 0 },
  { id: 2, q: '高压水枪冲洗车身时，推荐的水流角度是？', options: ['约 45° 斜向冲洗', '垂直 90° 直冲', '贴面 0° 平扫', '任意角度均可'], answer: 0 },
  { id: 3, q: 'L1 基础保险的正确描述是？', options: ['平台强制保障，不可取消', '用户可随时取消', '仅精细洗包含', '需额外付费'], answer: 0 },
  { id: 4, q: 'C3 精细认证的在线考试及格线是？', options: ['80 分', '60 分', '70 分', '90 分'], answer: 0 },
  { id: 5, q: '现场证据拍摄要求正确的是？', options: ['必须现场拍摄并带水印', '可用相册旧图', '只需拍 1 张', '无需订单号'], answer: 0 },
  { id: 6, q: '订单 Stepper 中「待验收」使用的语义色是？', options: ['警告色', '成功色', '危险色', '信息色'], answer: 0 },
  { id: 7, q: '增值项目最多可选几项？', options: ['4 项', '2 项', '6 项', '不限'], answer: 0 },
  { id: 8, q: '工位状态「已满」对应的指示色是？', options: ['危险色 #E5484D', '成功色 #18A058', '灰色 #97A3B6', '警告色 #F0A020'], answer: 0 },
  { id: 9, q: '认证不足的订单卡应如何展示？', options: ['置灰 + 锁图标 + 去补认证', '正常展示可抢', '直接隐藏', '标红即可'], answer: 0 },
  { id: 10, q: 'V1 测试版支付成功态应使用什么颜色？', options: ['橙色 #F5901B', '绿色 #18A058', '蓝色 #1668E3', '灰色'], answer: 0 }
];

export default {
  data() {
    return {
      questions: QUESTIONS.map(q => Object.assign({}, q, { selected: null })),
      submitted: false, passed: false, score: 0
    };
  },
  methods: {
    pick(qi, oi) { this.questions[qi].selected = oi; },
    reset() {
      this.questions = QUESTIONS.map(q => Object.assign({}, q, { selected: null }));
      this.submitted = false; this.passed = false; this.score = 0;
    },
    submit() {
      if (this.questions.some(q => q.selected === null)) {
        uni.showToast({ title: '请完成全部题目', icon: 'none' }); return;
      }
      const score = this.questions.reduce((s, q) => s + (q.selected === q.answer ? 10 : 0), 0);
      this.score = score;
      this.passed = score >= 80;
      this.submitted = true;
      api.worker.submitExam({ cert: 'C3', score, answers: this.questions.map(q => q.selected) }).catch(() => {});
      uni.showModal({
        title: this.passed ? '考试通过' : '考试未通过',
        content: this.passed ? 'C3 精细认证已解锁（模拟）。' : '未达 80 分，请复习后重新考试。',
        showCancel: false
      });
    }
  }
};
</script>

<style scoped>
.exam-head { background: linear-gradient(135deg, #EAF2FE, #E6F7F7); }
.q-title { font-size: 28rpx; font-weight: 600; margin-bottom: 16rpx; }
.option { display: flex; align-items: center; gap: 16rpx; padding: 16rpx; border: 2rpx solid #E4E9F2; border-radius: 8rpx; margin-bottom: 12rpx; font-size: 26rpx; }
.option.on { border-color: #1668E3; background: #EAF2FE; }
.radio { width: 32rpx; height: 32rpx; border: 2rpx solid #CBD3E0; border-radius: 50%; flex-shrink: 0; }
.radio.checked { border: 8rpx solid #1668E3; }
.result-score { font-size: 64rpx; font-weight: 700; text-align: center; }
.pass { color: #0E9F9F; } .fail { color: #E5484D; }
.center { text-align: center; }
</style>
