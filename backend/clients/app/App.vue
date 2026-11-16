<script>
/**
 * 线上洗车平台 v6.0 · uni-app 全局逻辑
 * V1 测试版：模拟支付，不产生真实资金收付。
 */
export default {
  globalData: {
    baseURL: '/api/',
    token: '',
    simPayment: true
  },
  onLaunch() {
    try {
      this.globalData.token = uni.getStorageSync('xc_token') || '';
    } catch (e) {}
    console.info('[app] uni-app launched, simPayment =', this.globalData.simPayment);
  },
  methods: {
    setLogin(token) {
      this.globalData.token = token || '';
      try { uni.setStorageSync('xc_token', this.globalData.token); } catch (e) {}
    }
  }
};
</script>

<style>
/* 全局样式：净水蓝 #1668E3 体系，与小程序/H5 保持一致 */
page {
  --primary: #1668E3;
  --sim-accent: #F5901B;
  --sim-bg: #FFF4E0;
  --sim-stripe: #FDE9C4;
  --sim-border: #F5C26B;
  --sim-fg: #8A4B08;
  background: #F7F9FC;
  color: #121826;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", sans-serif;
  font-size: 28rpx;
}
.container { padding: 24rpx; }
.card { background: #fff; border-radius: 12rpx; padding: 28rpx; box-shadow: 0 2rpx 12rpx rgba(18,24,56,.05); margin-bottom: 24rpx; }
.card-title { font-size: 32rpx; font-weight: 600; margin-bottom: 20rpx; display: flex; align-items: center; justify-content: space-between; }
.muted { color: #97A3B6; font-size: 24rpx; }
.row { display: flex; align-items: center; }
.between { display: flex; align-items: center; justify-content: space-between; }
.grow { flex: 1; }
.divider { height: 2rpx; background: #E4E9F2; margin: 20rpx 0; }

/* 模拟支付常驻横幅：琥珀斜纹 */
.sim-banner {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 18rpx 24rpx;
  color: var(--sim-fg);
  background-color: var(--sim-bg);
  background-image: repeating-linear-gradient(45deg, var(--sim-stripe) 0, var(--sim-stripe) 16rpx, transparent 16rpx, transparent 32rpx);
  border-bottom: 2rpx solid var(--sim-border);
  font-size: 24rpx;
  font-weight: 600;
  line-height: 1.4;
}
.sim-banner--inline { border: 2rpx solid var(--sim-border); border-radius: 10rpx; margin-bottom: 20rpx; }
/* 模拟支付成功：橙色，严禁绿色 */
.sim-success { color: var(--sim-accent) !important; }
.sim-success-desc { color: var(--sim-fg); }

.tag { display: inline-flex; align-items: center; padding: 4rpx 16rpx; border-radius: 999rpx; font-size: 22rpx; }
.tag-primary { background: #EAF2FE; color: #1668E3; }
.tag-success { background: #E8F7EF; color: #18A058; }
.tag-warning { background: #FFF5E5; color: #F0A020; }
.tag-danger { background: #FDECEC; color: #E5484D; }
.tag-gray { background: #F1F4F9; color: #97A3B6; }
.price { color: #E5484D; font-weight: 700; }
.price::before { content: '¥'; font-size: .7em; }
.price-example { color: #97A3B6; font-size: 20rpx; }
.locked { opacity: .55; filter: grayscale(1); }
.lock-mask { display: flex; align-items: center; justify-content: space-between; background: #F1F4F9; border-radius: 10rpx; padding: 20rpx; }
.btn { display: inline-flex; align-items: center; justify-content: center; height: 88rpx; padding: 0 36rpx; border-radius: 12rpx; font-size: 30rpx; font-weight: 600; border: none; }
.btn-primary { background: #1668E3; color: #fff; }
.btn-sim { background: #F5901B; color: #fff; }
.btn-ghost { background: #EAF2FE; color: #1668E3; }
.btn-block { width: 100%; }
.btn-sm { height: 60rpx; padding: 0 24rpx; font-size: 24rpx; border-radius: 8rpx; }
.mt-16 { margin-top: 16rpx; }
.hint { font-size: 22rpx; color: #8A4B08; background: #FFF4E0; padding: 12rpx 16rpx; border-radius: 8rpx; margin-top: 14rpx; }
.stepper { display: flex; flex-wrap: wrap; }
.stepper-item { width: 25%; text-align: center; margin-bottom: 28rpx; }
.stepper-dot { width: 40rpx; height: 40rpx; line-height: 40rpx; border-radius: 50%; margin: 0 auto 10rpx; background: #E4E9F2; color: #97A3B6; font-size: 22rpx; }
.stepper-label { font-size: 22rpx; color: #97A3B6; }
.stepper-item.done .stepper-dot { background: #1668E3; color: #fff; }
.stepper-item.current .stepper-dot { background: #F5901B; color: #fff; box-shadow: 0 0 0 6rpx rgba(245,144,27,.18); }
.stepper-item.current .stepper-label { color: #8A4B08; font-weight: 600; }
</style>
