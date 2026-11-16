<template>
  <view class="page store-workbench">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card">
        <view class="card-title"><text>工位看板</text><text class="muted">点击切换状态</text></view>
        <view class="bay-grid">
          <view v-for="b in bays" :key="b.id" class="bay" :class="'bay-' + b.status" @click="cycleBay(b)">
            <view class="bay-name">{{ b.name }}</view>
            <view class="bay-dot"></view>
            <view class="bay-status">{{ statusLabel(b.status) }}</view>
            <view class="bay-sub">{{ b.status === 'free' ? '可接单' : (b.status === 'full' ? '已满 · 不可接' : '暂停服务') }}</view>
          </view>
        </view>
        <view class="legend">
          <view v-for="s in statuses" :key="s.key" class="legend-item">
            <view class="legend-dot" :style="{ background: s.color }"></view>{{ s.label }}
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>待接单任务</text><text class="muted">无空位时接单被拦截</text></view>
        <view v-for="t in tasks" :key="t.orderNo" class="task">
          <view class="between">
            <view>
              <view class="task-no">{{ t.orderNo }}</view>
              <view class="muted">{{ t.serviceName }} · {{ t.modeText }}</view>
            </view>
            <text class="tag tag-primary">待接单</text>
          </view>
          <view class="between mt-16">
            <view><text class="price">{{ fen2Yuan(t.bayFeeFen) }}</text><text class="muted"> 工位费 M（门店结算）</text></view>
            <view class="btn btn-primary btn-sm" @click="accept(t.orderNo)">接单</view>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>结算口径</text></view>
        <view class="muted">门店端结算仅显示工位费 M 部分，不展示用户支付与平台服务费明细。</view>
      </view>
    </view>
  </view>
</template>

<script>
import api, { C, fen2Yuan } from '../../common/api.js';

const DEMO_BAYS = [
  { id: 'B1', name: 'Bay-01', status: 'free' },
  { id: 'B2', name: 'Bay-02', status: 'full' },
  { id: 'B3', name: 'Bay-03', status: 'paused' }
];
const DEMO_TASKS = [
  { orderNo: 'WC20260616-0101', serviceName: '标准清洁', modeText: '到店洗', bayFeeFen: 2000 },
  { orderNo: 'WC20260616-0102', serviceName: '外观清洁', modeText: '到店洗', bayFeeFen: 1500 }
];

export default {
  data() {
    return { bays: DEMO_BAYS, tasks: DEMO_TASKS, statuses: C.BAY_STATUS, fen2Yuan };
  },
  onLoad() {
    api.store.getBays().then(data => {
      if (data && Array.isArray(data.bays) && data.bays.length) this.bays = data.bays;
    }).catch(() => {});
  },
  methods: {
    statusLabel(key) {
      const s = C.BAY_STATUS.find(x => x.key === key);
      return s ? s.label : '未知';
    },
    cycleBay(b) {
      const order = ['free', 'full', 'paused'];
      b.status = order[(order.indexOf(b.status) + 1) % order.length];
      api.store.updateBay(b.id, { status: b.status }).catch(() => {});
      uni.showToast({ title: b.name + ' → ' + this.statusLabel(b.status), icon: 'none' });
    },
    accept(orderNo) {
      if (!this.bays.some(b => b.status === 'free')) {
        // 无空位必须拦截，禁止强制接单
        uni.showModal({
          title: '当前无空位',
          content: '有空位工位为 0。请返回工作台切换工位状态后重试。',
          showCancel: false
        });
        return;
      }
      uni.showModal({
        title: '确认接单', content: orderNo + ' 将占用 1 个空位，接单后工位状态变为「已满」（模拟）。',
        success: (r) => {
          if (!r.confirm) return;
          this.bays.forEach(b => { if (b.status === 'free') b.status = 'full'; });
          this.tasks = this.tasks.filter(t => t.orderNo !== orderNo);
          api.store.acceptOrder(orderNo, {}).catch(() => {});
          uni.showToast({ title: '接单成功（模拟）', icon: 'none' });
        }
      });
    }
  }
};
</script>

<style scoped>
.bay-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.bay { width: calc(33.33% - 11rpx); border: 2rpx solid #E4E9F2; border-radius: 12rpx; padding: 24rpx 12rpx; text-align: center; box-sizing: border-box; }
.bay-name { font-size: 26rpx; font-weight: 600; }
.bay-dot { width: 28rpx; height: 28rpx; border-radius: 50%; margin: 20rpx auto 12rpx; }
.bay-status { font-size: 26rpx; font-weight: 600; }
.bay-sub { font-size: 20rpx; color: #97A3B6; margin-top: 6rpx; }
.bay-free .bay-dot { background: #18A058; } .bay-free .bay-status { color: #18A058; }
.bay-full .bay-dot { background: #E5484D; } .bay-full .bay-status { color: #E5484D; }
.bay-paused .bay-dot { background: #97A3B6; } .bay-paused .bay-status { color: #97A3B6; }
.legend { display: flex; gap: 24rpx; margin-top: 24rpx; }
.legend-item { display: flex; align-items: center; gap: 8rpx; font-size: 22rpx; color: #6B778C; }
.legend-dot { width: 16rpx; height: 16rpx; border-radius: 50%; }
.task { padding: 20rpx 0; border-bottom: 2rpx solid #F1F4F9; }
.task-no { font-size: 28rpx; font-weight: 600; }
</style>
