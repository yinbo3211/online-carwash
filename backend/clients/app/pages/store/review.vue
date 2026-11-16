<template>
  <view class="page store-review">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card">
        <view class="card-title"><text>订单评价</text><text class="muted">{{ orderNo }}</text></view>
        <view class="muted">五维评分，1~5 星。任一维度 ≤2 星时强制填写原因。</view>
      </view>

      <view v-for="(d, di) in dims" :key="d.key" class="card dim">
        <view class="between">
          <view class="dim-name">{{ d.label }}</view>
          <view class="stars">
            <text v-for="n in 5" :key="n" class="star" :class="{ on: n <= d.score }" @click="setScore(di, n)">★</text>
          </view>
        </view>
        <view class="muted score-text">{{ d.score ? d.score + ' 星' : '未评分' }}</view>
        <input v-if="d.score && d.score <= lowScore" class="input reason" v-model="d.reason" placeholder="低分原因（必填）" />
      </view>

      <view class="card">
        <view class="form-label">总体评价（选填）</view>
        <input class="input" v-model="comment" placeholder="补充说明服务体验" />
      </view>

      <view class="btn btn-primary btn-block" @click="submit">提交评价</view>
    </view>
  </view>
</template>

<script>
import api, { C } from '../../common/api.js';

export default {
  data() {
    return {
      orderNo: 'WC20260616-0083',
      dims: C.REVIEW_DIMS.map(d => Object.assign({}, d, { score: 0, reason: '' })),
      lowScore: C.LOW_SCORE,
      comment: ''
    };
  },
  onLoad(query) { if (query.orderNo) this.orderNo = query.orderNo; },
  methods: {
    setScore(di, n) { this.dims[di].score = n; },
    submit() {
      if (this.dims.some(d => !d.score)) { uni.showToast({ title: '请完成全部五维评分', icon: 'none' }); return; }
      if (this.dims.some(d => d.score <= C.LOW_SCORE && !d.reason.trim())) {
        uni.showToast({ title: '低分维度需填写原因', icon: 'none' }); return;
      }
      const payload = {
        orderNo: this.orderNo,
        scores: this.dims.map(d => ({ dim: d.key, score: d.score, reason: d.reason })),
        comment: this.comment
      };
      api.store.replyReview(this.orderNo, payload).catch(() => {});
      uni.showModal({
        title: '评价已提交', content: '五维评价已提交（模拟）。', showCancel: false,
        success: () => uni.navigateBack()
      });
    }
  }
};
</script>

<style scoped>
.dim { padding: 24rpx 28rpx; }
.dim-name { font-size: 30rpx; font-weight: 600; }
.stars { display: flex; gap: 10rpx; }
.star { font-size: 44rpx; color: #CBD3E0; line-height: 1; }
.star.on { color: #F5901B; }
.score-text { margin-top: 10rpx; font-size: 22rpx; }
.reason { margin-top: 16rpx; }
.form-label { font-size: 26rpx; color: #4A5568; margin-bottom: 12rpx; }
.input { width: 100%; height: 72rpx; padding: 0 20rpx; background: #F7F9FC; border-radius: 8rpx; font-size: 28rpx; box-sizing: border-box; }
</style>
