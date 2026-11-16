<template>
  <view class="page user-index">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="hero">
      <view class="hero-title">线上洗车 · 取送车服务</view>
      <view class="hero-sub">一键下单，洗车工上门取送，全程留证</view>
      <view class="hero-tags">
        <text class="tag tag-primary">L1 平台强制保障</text>
        <text class="tag tag-gray">价格为示例值</text>
      </view>
    </view>

    <view class="container">
      <view class="card">
        <view class="card-title"><text>选择服务档位</text><text class="muted">金额单位：分 → 元</text></view>
        <view v-for="t in tiers" :key="t.key"
              class="tier" :class="{ on: t.key === selectedTier }"
              @click="goCreate(t.key)">
          <view class="tier-head">
            <text class="tier-name">{{ t.name }}</text>
            <text v-if="t.recommend" class="tag tag-warning">推荐</text>
            <text class="grow"></text>
            <text class="price">{{ fen2Yuan(t.priceFen) }}</text>
          </view>
          <view class="muted tier-desc">{{ t.desc }}</view>
        </view>
        <view class="price-example">* 以上为示例价，实际以服务端返回为准</view>
      </view>

      <view class="card">
        <view class="card-title"><text>快捷入口</text></view>
        <view class="entries">
          <view class="entry" @click="goCreate('standard')"><view class="entry-icon">🚗</view><view class="entry-label">立即下单</view></view>
          <view class="entry" @click="goProgress"><view class="entry-icon">📋</view><view class="entry-label">订单进度</view></view>
          <view class="entry" @click="goPayDemo"><view class="entry-icon">💳</view><view class="entry-label">模拟支付</view></view>
        </view>
      </view>
    </view>

    <view class="fixed-cta btn btn-primary" @click="goCreate(selectedTier)">去下单</view>
  </view>
</template>

<script>
import api, { C, fen2Yuan } from '../../common/api.js';

export default {
  data() {
    return { tiers: C.SERVICE_TIERS, selectedTier: 'standard', fen2Yuan };
  },
  onLoad() {
    api.user.getHome().then(data => {
      if (data && Array.isArray(data.tiers) && data.tiers.length) this.tiers = data.tiers;
    }).catch(() => {});
  },
  methods: {
    goCreate(tier) { uni.navigateTo({ url: '/pages/user/order-create?tier=' + tier }); },
    goProgress() { uni.navigateTo({ url: '/pages/user/order-progress?orderId=WC20260616-0083' }); },
    goPayDemo() {
      uni.showModal({ title: '模拟支付说明', content: C.SIM_SUCCESS_FULL, showCancel: false });
    }
  }
};
</script>

<style scoped>
.hero { padding: 40rpx 32rpx 56rpx; background: linear-gradient(135deg, #1668E3, #0E9F9F); color: #fff; }
.hero-title { font-size: 44rpx; font-weight: 700; }
.hero-sub { font-size: 26rpx; opacity: .9; margin-top: 12rpx; }
.hero-tags { margin-top: 24rpx; display: flex; gap: 12rpx; }
.hero .tag-primary, .hero .tag-gray { background: rgba(255,255,255,.2); color: #fff; }
.tier { border: 2rpx solid #E4E9F2; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx; }
.tier.on { border-color: #1668E3; background: #EAF2FE; }
.tier-head { display: flex; align-items: center; gap: 12rpx; }
.tier-name { font-size: 30rpx; font-weight: 600; }
.tier-desc { margin-top: 10rpx; font-size: 24rpx; }
.entries { display: flex; gap: 16rpx; }
.entry { flex: 1; text-align: center; padding: 20rpx 0; background: #F7F9FC; border-radius: 12rpx; }
.entry-icon { font-size: 44rpx; }
.entry-label { font-size: 24rpx; color: #4A5568; margin-top: 8rpx; }
.fixed-cta { position: fixed; left: 24rpx; right: 24rpx; bottom: 24rpx; z-index: 10; }
.user-index { padding-bottom: 140rpx; }
</style>
