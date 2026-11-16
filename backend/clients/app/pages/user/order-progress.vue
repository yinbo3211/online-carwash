<template>
  <view class="page order-progress">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card">
        <view class="between">
          <view>
            <view class="order-no">{{ order.orderNo }}</view>
            <view class="muted">{{ order.serviceName }} · {{ order.modeText }}</view>
          </view>
          <text class="tag" :class="statusToneClass">{{ statusLabel }}</text>
        </view>
        <view class="divider"></view>
        <view class="stepper">
          <view v-for="(s, i) in steps" :key="s.key" class="stepper-item" :class="s.state">
            <view class="stepper-dot">{{ s.state === 'done' ? '✓' : (i + 1) }}</view>
            <view class="stepper-label">{{ s.label }}</view>
          </view>
        </view>
        <view v-if="statusIndex === 6" class="hint">待验收：请核对车辆与清洁效果（警告色提示）</view>
      </view>

      <view class="card">
        <view class="card-title"><text>取还车证据</text><text class="muted">现场拍摄留痕</text></view>
        <view v-for="e in evidence" :key="e.stage" class="evidence-item">
          <view class="evidence-stage">{{ e.stage }}</view>
          <view class="thumbs">
            <view v-for="(p, pi) in e.photos" :key="pi" class="thumb">{{ p }}</view>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>服务信息</text></view>
        <view class="between"><text>洗车工</text><text>{{ order.workerName }}</text></view>
        <view class="divider"></view>
        <view class="between"><text>车型 / 车牌</text><text>{{ order.carNo }}</text></view>
        <view class="divider"></view>
        <view class="between"><text>实付（示例）</text><text class="price">{{ fen2Yuan(order.amountFen) }}</text></view>
        <view class="divider"></view>
        <view class="between"><text>保险</text><text class="tag tag-danger">L1 强制</text></view>
      </view>
    </view>
  </view>
</template>

<script>
import api, { C, fen2Yuan } from '../../common/api.js';

const DEMO_ORDER = {
  orderNo: 'WC20260616-0083', tier: 'standard', serviceName: '标准清洁',
  modeText: '上门取送', statusIndex: 6, workerName: '王师傅',
  carNo: '粤B·D12345', amountFen: 14800
};
const DEMO_EVIDENCE = [
  { stage: 'E1 接车预检', photos: ['🚗', '📷'] },
  { stage: 'E2 外观清洗', photos: ['🫧', '✨'] },
  { stage: 'E3 内饰清洁', photos: ['🧽', '🪑'] },
  { stage: 'E4 交车确认', photos: ['🤝', '📷'] }
];

export default {
  data() {
    return {
      order: DEMO_ORDER, evidence: DEMO_EVIDENCE, steps: [],
      statusIndex: 6, statusLabel: '待验收', statusToneClass: 'tag-warning', fen2Yuan
    };
  },
  onLoad(query) {
    this.orderNo = query.orderId || DEMO_ORDER.orderNo;
    this.apply(DEMO_ORDER);
    api.user.getOrder(this.orderNo).then(data => {
      if (data && data.order) this.apply(Object.assign({}, DEMO_ORDER, data.order));
    }).catch(() => {});
  },
  methods: {
    apply(o) {
      const idx = Math.min(7, Math.max(0, Number(o.statusIndex != null ? o.statusIndex : 6)));
      const status = C.ORDER_STATUS[idx];
      const toneMap = { warning: 'tag-warning', success: 'tag-success', primary: 'tag-primary', info: 'tag-primary' };
      this.order = Object.assign({}, o);
      this.statusIndex = idx;
      this.statusLabel = status.label;
      this.statusToneClass = toneMap[status.tone] || 'tag-primary';
      this.steps = C.ORDER_STATUS.map((s, i) => ({
        key: s.key, label: s.label,
        state: i < idx ? 'done' : (i === idx ? 'current' : '')
      }));
    }
  }
};
</script>

<style scoped>
.order-no { font-size: 32rpx; font-weight: 700; }
.hint { font-size: 22rpx; color: #8A4B08; background: #FFF4E0; padding: 12rpx 16rpx; border-radius: 8rpx; margin-top: 12rpx; }
.evidence-item { margin-bottom: 20rpx; }
.evidence-stage { font-size: 26rpx; color: #4A5568; margin-bottom: 12rpx; }
.thumbs { display: flex; gap: 16rpx; }
.thumb { width: 140rpx; height: 140rpx; border-radius: 10rpx; background: #F1F4F9; display: flex; align-items: center; justify-content: center; font-size: 56rpx; border: 2rpx dashed #E4E9F2; }
</style>
