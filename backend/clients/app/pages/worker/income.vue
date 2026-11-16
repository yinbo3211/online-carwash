<template>
  <view class="page worker-income">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="wallet">
        <view class="wallet-label">本月可提现（示例）</view>
        <view class="wallet-amount price">{{ fen2Yuan(withdrawFen) }}</view>
        <view class="wallet-sub">平台服务费抽成 35% · 洗车工分成 40%</view>
        <view class="btn btn-sim wallet-btn" @click="withdraw">模拟提现</view>
      </view>

      <view class="card">
        <view class="card-title"><text>收入明细</text><text class="muted">分 → 元</text></view>
        <view v-for="b in bills" :key="b.id" class="bill">
          <view class="between">
            <view>
              <view class="bill-title">{{ b.title }}</view>
              <view class="muted">{{ b.date }} · {{ b.no }}</view>
            </view>
            <view style="text-align:right">
              <view class="price bill-amount">{{ fen2Yuan(b.amountFen) }}</view>
              <view class="muted">服务费 {{ fen2Yuan(b.feeFen) }}</view>
            </view>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>结算口径</text></view>
        <view class="between"><text>平台服务费</text><text>35%</text></view>
        <view class="divider"></view>
        <view class="between"><text>洗车工分成</text><text>40%</text></view>
        <view class="divider"></view>
        <view class="between"><text>提现通道</text><text class="tag tag-warning">V1 模拟，不产生真实资金</text></view>
      </view>
    </view>
  </view>
</template>

<script>
import api, { fen2Yuan, C } from '../../common/api.js';

const DEMO_BILLS = [
  { id: 1, title: '标准清洁 · 分成', no: 'WC20260616-0083', date: '06-16', amountFen: 5320, feeFen: 4655 },
  { id: 2, title: '外观清洁 · 分成', no: 'WC20260615-0061', date: '06-15', amountFen: 3320, feeFen: 2905 },
  { id: 3, title: '精细清洁 · 分成', no: 'WC20260614-0042', date: '06-14', amountFen: 9720, feeFen: 8505 }
];

export default {
  data() {
    return { bills: DEMO_BILLS, withdrawFen: 18360, fen2Yuan };
  },
  onLoad() {
    api.worker.getIncome().then(data => {
      if (data && Array.isArray(data.bills) && data.bills.length) {
        this.bills = data.bills;
        this.withdrawFen = data.bills.reduce((s, b) => s + (b.amountFen || 0), 0);
      }
    }).catch(() => {});
  },
  methods: {
    withdraw() {
      uni.showModal({ title: '模拟提现', content: C.SIM_SUCCESS_FULL, showCancel: false });
    }
  }
};
</script>

<style scoped>
.wallet { background: linear-gradient(135deg, #0E9F9F, #1668E3); border-radius: 16rpx; padding: 36rpx; color: #fff; margin-bottom: 24rpx; }
.wallet-label { font-size: 26rpx; opacity: .9; }
.wallet .price { color: #fff; }
.wallet-amount { font-size: 72rpx; line-height: 1.2; margin: 12rpx 0; }
.wallet-sub { font-size: 22rpx; opacity: .85; }
.wallet-btn { margin-top: 24rpx; }
.bill { padding: 20rpx 0; border-bottom: 2rpx solid #F1F4F9; }
.bill-title { font-size: 28rpx; font-weight: 500; }
.bill-amount { font-size: 32rpx; }
</style>
