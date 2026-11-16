<template>
  <view class="page pay-success">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="success-wrap">
      <view class="success-icon sim-success">✓</view>
      <view class="success-title sim-success">模拟支付成功</view>
      <view class="sim-success-desc success-desc">测试数据，不产生真实资金收付</view>

      <view class="card info-card">
        <view class="between"><text>订单编号</text><text>{{ orderNo }}</text></view>
        <view class="divider"></view>
        <view class="between"><text>模拟支付金额</text><text class="price amount">{{ amountText }}</text></view>
        <view class="divider"></view>
        <view class="between"><text>保险</text><text class="tag tag-danger">L1 强制</text></view>
      </view>

      <view class="sim-banner sim-banner--inline">
        <text>🔒 本页为 V1 测试版模拟结果，未发生任何真实扣款或资金结算。</text>
      </view>

      <view class="btn btn-primary btn-block" @click="goProgress">查看订单进度</view>
      <view class="btn btn-ghost btn-block mt-16" @click="goHome">返回首页</view>
    </view>
  </view>
</template>

<script>
import { fen2Yuan, C } from '../../common/api.js';

export default {
  data() {
    return { orderNo: '', amountText: '0.00' };
  },
  onLoad(query) {
    this.orderNo = decodeURIComponent(query.orderId || 'WC20260616-0083');
    this.amountText = fen2Yuan(query.amountFen || 0);
    console.info('[pay]', C.SIM_SUCCESS_FULL);
  },
  methods: {
    goProgress() { uni.redirectTo({ url: '/pages/user/order-progress?orderId=' + encodeURIComponent(this.orderNo) }); },
    goHome() { uni.switchTab({ url: '/pages/user/index' }); }
  }
};
</script>

<style scoped>
.success-wrap { padding: 80rpx 24rpx 40rpx; text-align: center; }
.success-icon { width: 140rpx; height: 140rpx; line-height: 140rpx; margin: 0 auto 28rpx; border-radius: 50%; border: 6rpx solid #F5901B; background: #FFF4E0; font-size: 76rpx; font-weight: 700; }
.success-title { font-size: 44rpx; font-weight: 700; }
.success-desc { font-size: 26rpx; margin-top: 12rpx; }
.info-card { text-align: left; margin-top: 48rpx; }
.amount { font-size: 36rpx; }
</style>
