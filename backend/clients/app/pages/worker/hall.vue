<template>
  <view class="page worker-hall">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card">
        <view class="card-title"><text>我的资质</text><text class="muted">认证不足的订单置灰锁定</text></view>
        <view class="cert-tags">
          <view v-for="c in certs" :key="c.key" class="cert-item" :class="{ passed: c.passed }">
            <text class="cert-code">{{ c.key }}</text>
            <text class="cert-name">{{ c.name }}</text>
            <text class="tag" :class="c.passed ? 'tag-success' : 'tag-gray'">{{ c.passed ? '已通过' : '未通过' }}</text>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>抢单大厅</text><text class="muted">倒计时 90s</text></view>
        <view v-for="t in tasks" :key="t.orderNo" class="order-card" :class="{ locked: t.locked }">
          <view class="between">
            <text class="order-no">{{ t.orderNo }}</text>
            <text class="tag" :class="t.locked ? 'tag-gray' : 'tag-primary'">{{ t.locked ? '资质不足' : '可抢' }}</text>
          </view>
          <view class="muted meta">{{ t.serviceName }} · {{ t.modeText }} · {{ t.address }}</view>
          <view class="between mt-16">
            <view><text class="price fee">{{ fen2Yuan(t.feeFen) }}</text><text class="muted"> 洗车工分成（示例）</text></view>
          </view>

          <view v-if="t.locked" class="lock-mask mt-16">
            <view class="row">
              <text class="lock-icon">🔒</text>
              <view>
                <view class="lock-title">需 {{ t.requiredCert }} 认证方可抢单</view>
                <view class="muted">当前订单已置灰锁定</view>
              </view>
            </view>
            <view class="btn btn-ghost btn-sm" @click="goCert(t.requiredCert)">去补认证</view>
          </view>
          <view v-else class="btn btn-primary btn-block mt-16" @click="grab(t.orderNo)">立即抢单</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import api, { C, fen2Yuan } from '../../common/api.js';

const DEMO_CERTS = [
  { key: 'C1', name: '基础洗车', passed: true },
  { key: 'C2', name: '标准洗车', passed: true },
  { key: 'C3', name: '精细洗车', passed: false },
  { key: 'D', name: '驾驶资格', passed: true },
  { key: 'O', name: '操作规范', passed: true }
];
const DEMO_TASKS = [
  { orderNo: 'WC20260616-0091', tier: 'standard', serviceName: '标准清洁', modeText: '到店洗', address: '福田区莲花路', feeFen: 5320, requiredCert: 'C2' },
  { orderNo: 'WC20260616-0092', tier: 'premium', serviceName: '精细清洁', modeText: '上门取送', address: '南山区科技园', feeFen: 9720, requiredCert: 'C3' },
  { orderNo: 'WC20260616-0093', tier: 'basic', serviceName: '外观清洁', modeText: '到店洗', address: '罗湖区人民南路', feeFen: 3320, requiredCert: 'C1' }
];

export default {
  data() {
    return { certs: DEMO_CERTS, tasks: [], fen2Yuan };
  },
  onLoad() {
    this.build(DEMO_TASKS);
    api.worker.getHall().then(data => {
      if (data && Array.isArray(data.tasks) && data.tasks.length) this.build(data.tasks);
    }).catch(() => {});
  },
  methods: {
    build(list) {
      const passed = {};
      this.certs.forEach(c => { passed[c.key] = c.passed; });
      this.tasks = list.map(t => Object.assign({}, t, { locked: !passed[t.requiredCert] }));
    },
    grab(orderNo) {
      uni.showModal({
        title: '确认抢单', content: '订单 ' + orderNo + ' 将在 90 秒内锁定，确认抢单？（模拟）',
        success: (r) => {
          if (!r.confirm) return;
          api.worker.grabOrder(orderNo).catch(() => {});
          this.tasks = this.tasks.filter(t => t.orderNo !== orderNo);
          uni.showToast({ title: '抢单成功（模拟）', icon: 'none' });
        }
      });
    },
    goCert(cert) {
      if (cert === 'C3') { uni.navigateTo({ url: '/pages/worker/exam?cert=C3' }); return; }
      uni.showModal({ title: cert + ' 补认证', content: '请提交对应资质材料至平台审核（模拟）。', showCancel: false });
    }
  }
};
</script>

<style scoped>
.cert-tags { display: flex; flex-wrap: wrap; gap: 12rpx; }
.cert-item { display: flex; align-items: center; gap: 8rpx; background: #F1F4F9; border-radius: 8rpx; padding: 10rpx 14rpx; opacity: .7; }
.cert-item.passed { background: #E8F7EF; opacity: 1; }
.cert-code { font-weight: 700; }
.cert-name { font-size: 22rpx; color: #6B778C; }
.order-card { border: 2rpx solid #E4E9F2; border-radius: 12rpx; padding: 24rpx; margin-bottom: 18rpx; }
.order-no { font-size: 28rpx; font-weight: 700; }
.meta { font-size: 24rpx; margin-top: 10rpx; }
.fee { font-size: 34rpx; }
.lock-icon { font-size: 40rpx; margin-right: 12rpx; }
.lock-title { font-size: 26rpx; font-weight: 600; color: #4A5568; }
</style>
