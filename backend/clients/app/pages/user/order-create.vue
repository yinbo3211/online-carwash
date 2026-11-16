<template>
  <view class="page order-create">
    <view class="sim-banner">
      <text class="sim-icon">⚠</text>
      <text>测试环境 · 模拟支付 · 不产生真实资金收付</text>
    </view>

    <view class="container">
      <view class="card">
        <view class="card-title"><text>服务档位</text><text class="muted">示例价</text></view>
        <view v-for="t in tiers" :key="t.key" class="tier" :class="{ on: t.key === tier }" @click="chooseTier(t.key)">
          <view class="tier-main">
            <text class="tier-name">{{ t.name }}</text>
            <text v-if="t.recommend" class="tag tag-warning">推荐</text>
          </view>
          <view class="muted">{{ t.desc }}</view>
          <text class="price tier-price">{{ fen2Yuan(t.priceFen) }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>服务方式</text></view>
        <view class="seg">
          <view class="seg-item" :class="{ on: mode === 'store' }" @click="chooseMode('store')">到店洗</view>
          <view class="seg-item" :class="{ on: mode === 'door', off: doorDisabled }" @click="chooseMode('door')">
            上门取送 <text class="price seg-price">{{ fen2Yuan(doorPriceFen) }}</text>
          </view>
        </view>
        <view v-if="doorDisabled" class="hint">精细清洁暂不支持上门，请选择到店或切换档位</view>
        <view class="form-item">
          <view class="form-label">取送地址（上门必填）</view>
          <input class="input" v-model="address" placeholder="请输入详细地址" />
        </view>
        <view class="form-item">
          <view class="form-label">联系手机（11 位中国大陆手机号）</view>
          <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>增值项目</text><text class="muted">最多 {{ maxAddons }} 项 · 已选 {{ checkedAddons.length }}</text></view>
        <view v-for="a in addons" :key="a.key" class="addon-row" @click="toggleAddon(a)">
          <view class="check" :class="{ checked: a.checked }">{{ a.checked ? '✓' : '' }}</view>
          <text class="grow">{{ a.name }}</text>
          <text class="price">{{ fen2Yuan(a.priceFen) }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card-title"><text>保险保障</text></view>
        <view class="ins-locked">
          <text class="ins-icon">🔒</text>
          <view class="grow">
            <view class="ins-name">L1 平台强制保障 <text class="tag tag-danger">强制</text></view>
            <view class="muted">平台强制保障，不可取消</view>
          </view>
          <text class="muted">¥0</text>
        </view>
        <view class="hint">L1 为平台强制保障，已锁定，不可取消</view>
        <view class="divider"></view>
        <view class="ins-row" @click="l2 = !l2">
          <view class="check" :class="{ checked: l2 }">{{ l2 ? '✓' : '' }}</view>
          <view class="grow">
            <view class="ins-name">L2 增值保险 <text class="tag tag-primary">可选</text></view>
            <view class="muted">可选投保（示例价 ¥12）</view>
          </view>
          <text class="price">{{ fen2Yuan(l2Fen) }}</text>
        </view>
      </view>

      <view class="card summary">
        <view class="between"><text>服务费</text><text class="price">{{ fen2Yuan(baseFen) }}</text></view>
        <view class="between"><text>增值项目</text><text class="price">{{ fen2Yuan(addonFen) }}</text></view>
        <view class="between"><text>L2 增值保险</text><text class="price">{{ fen2Yuan(l2Fen) }}</text></view>
        <view class="between"><text>优惠券</text><text class="price">-{{ fen2Yuan(couponFen) }}</text></view>
        <view class="divider"></view>
        <view class="between total"><text>合计（示例）</text><text class="price total-price">{{ fen2Yuan(totalFen) }}</text></view>
        <view class="price-example center">金额以「分」传给后端，此处展示为元</view>
      </view>
    </view>

    <view class="paybar">
      <view class="paybar-left"><view class="muted">应付</view><view class="price total-price">{{ fen2Yuan(totalFen) }}</view></view>
      <view class="btn btn-sim grow" @click="submit">模拟支付并提交</view>
    </view>
  </view>
</template>

<script>
import api, { C, fen2Yuan } from '../../common/api.js';

export default {
  data() {
    return {
      tiers: C.SERVICE_TIERS,
      addons: C.ADDONS.map(a => Object.assign({}, a, { checked: false })),
      tier: 'standard', mode: 'store', address: '', phone: '',
      l2: false, couponFen: 1000, maxAddons: C.MAX_ADDONS,
      fen2Yuan
    };
  },
  computed: {
    currentTier() { return this.tiers.find(t => t.key === this.tier) || this.tiers[1]; },
    doorDisabled() { return this.tier === 'premium'; },
    doorPriceFen() { return C.DOOR_PRICE_FEN[this.tier] || this.currentTier.priceFen; },
    baseFen() { return this.mode === 'door' ? this.doorPriceFen : this.currentTier.priceFen; },
    checkedAddons() { return this.addons.filter(a => a.checked); },
    addonFen() { return this.checkedAddons.reduce((s, a) => s + a.priceFen, 0); },
    l2Fen() { return this.l2 ? C.INSURANCE.L2.priceFen : 0; },
    totalFen() { return Math.max(0, this.baseFen + this.addonFen + this.l2Fen - this.couponFen); }
  },
  onLoad(query) { if (query.tier) this.tier = query.tier; },
  methods: {
    chooseTier(key) {
      this.tier = key;
      if (key === 'premium') this.mode = 'store';
    },
    chooseMode(mode) {
      if (mode === 'door' && this.doorDisabled) { uni.showToast({ title: '精细清洁暂不支持上门', icon: 'none' }); return; }
      this.mode = mode;
    },
    toggleAddon(a) {
      if (!a.checked && this.checkedAddons.length >= C.MAX_ADDONS) {
        uni.showToast({ title: '最多选 ' + C.MAX_ADDONS + ' 项', icon: 'none' }); return;
      }
      a.checked = !a.checked;
    },
    submit() {
      if (this.mode === 'door') {
        if (!/^1\d{10}$/.test(this.phone)) { uni.showToast({ title: '请输入 11 位手机号', icon: 'none' }); return; }
        if (!this.address.trim()) { uni.showToast({ title: '请输入取送地址', icon: 'none' }); return; }
      }
      const payload = {
        tier: this.tier, serviceMode: this.mode, address: this.address, phone: this.phone,
        addonKeys: this.checkedAddons.map(a => a.key),
        insurance: { l1Accepted: true, l1Cancelable: false, l2Selected: this.l2 },
        couponFen: this.couponFen, amountFen: this.totalFen,
        requestId: 'req_' + Date.now()
      };
      uni.showLoading({ title: '提交中' });
      api.user.createOrder(payload).then(data => {
        uni.hideLoading();
        this.goSuccess((data && (data.orderId || data.id)) || this.demoNo());
      }).catch(() => {
        uni.hideLoading();
        uni.showToast({ title: '后端未就绪，使用演示数据', icon: 'none' });
        setTimeout(() => this.goSuccess(this.demoNo()), 900);
      });
    },
    goSuccess(orderId) {
      uni.redirectTo({ url: '/pages/user/pay-success?orderId=' + encodeURIComponent(orderId) + '&amountFen=' + this.totalFen });
    },
    demoNo() { return 'WC' + Date.now().toString().slice(-10); }
  }
};
</script>

<style scoped>
.order-create { padding-bottom: 180rpx; }
.tier { position: relative; border: 2rpx solid #E4E9F2; border-radius: 12rpx; padding: 20rpx 130rpx 20rpx 20rpx; margin-bottom: 14rpx; }
.tier.on { border-color: #1668E3; background: #EAF2FE; }
.tier-main { display: flex; align-items: center; gap: 12rpx; }
.tier-name { font-size: 30rpx; font-weight: 600; }
.tier .muted { font-size: 24rpx; margin-top: 8rpx; }
.tier-price { position: absolute; right: 20rpx; top: 28rpx; font-size: 34rpx; }
.seg { display: flex; background: #F1F4F9; border-radius: 12rpx; padding: 6rpx; }
.seg-item { flex: 1; text-align: center; padding: 18rpx 0; border-radius: 10rpx; font-size: 26rpx; color: #6B778C; }
.seg-item.on { background: #fff; color: #1668E3; font-weight: 600; }
.seg-item.off { opacity: .45; }
.seg-price { font-size: 24rpx; }
.form-item { padding: 20rpx 0; border-bottom: 2rpx solid #E4E9F2; }
.form-label { font-size: 26rpx; color: #4A5568; margin-bottom: 12rpx; }
.input { width: 100%; height: 72rpx; padding: 0 20rpx; background: #F7F9FC; border-radius: 8rpx; font-size: 28rpx; box-sizing: border-box; }
.addon-row { display: flex; align-items: center; gap: 16rpx; padding: 20rpx 0; border-bottom: 2rpx solid #F1F4F9; }
.check { width: 36rpx; height: 36rpx; border: 2rpx solid #CBD3E0; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; color: #fff; flex-shrink: 0; }
.check.checked { background: #1668E3; border-color: #1668E3; }
.ins-locked { background: #F7F9FC; border-radius: 10rpx; padding: 20rpx; display: flex; align-items: center; gap: 16rpx; }
.ins-icon { font-size: 36rpx; }
.ins-name { font-size: 28rpx; font-weight: 600; margin-bottom: 6rpx; }
.ins-row { display: flex; align-items: center; gap: 16rpx; padding: 16rpx 0; }
.summary .between { padding: 8rpx 0; }
.total { font-weight: 600; }
.total-price { font-size: 44rpx; }
.center { text-align: center; }
.paybar { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 16rpx 24rpx; box-shadow: 0 -4rpx 16rpx rgba(18,24,56,.08); display: flex; align-items: center; gap: 24rpx; z-index: 20; }
.paybar-left { min-width: 140rpx; }
</style>
