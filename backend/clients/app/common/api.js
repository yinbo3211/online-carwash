/**
 * 线上洗车平台 v6.0 · uni-app 公共层（API + 业务常量）
 * baseURL 从 App.globalData.baseURL 读取，禁止页面硬编码域名。
 * 金额一律「分」传输，展示用 fen2Yuan 转换；价格为示例值。
 */

const FALLBACK_BASE_URL = '/api/';

// 与微信小程序端保持同一口径
export const C = {
  SUCCESS_CODES: [0, 200],
  SERVICE_TIERS: [
    { key: 'basic', name: '外观清洁', priceFen: 8300, desc: '高压冲洗 · 中性泡沫 · 轮毂清洁', recommend: false },
    { key: 'standard', name: '标准清洁', priceFen: 13300, desc: '外观清洁 · 内饰吸尘 · 中控擦拭', recommend: true },
    { key: 'premium', name: '精细清洁', priceFen: 24300, desc: '标准清洁 · 深度去污 · 打蜡养护', recommend: false }
  ],
  DOOR_PRICE_FEN: { basic: 9500, standard: 15500 },
  ADDONS: [
    { key: 'wax', name: '漆面打蜡', priceFen: 3000 },
    { key: 'interior', name: '内饰深度清洁', priceFen: 5000 },
    { key: 'disinfect', name: '车内消毒', priceFen: 2000 },
    { key: 'tire', name: '轮胎养护', priceFen: 1500 }
  ],
  MAX_ADDONS: 4,
  INSURANCE: {
    L1: { key: 'L1', name: 'L1 平台强制保障', priceFen: 0, required: true, cancelable: false, desc: '平台强制保障，不可取消' },
    L2: { key: 'L2', name: 'L2 增值保险', priceFen: 1200, required: false, cancelable: true, desc: '可选投保（示例价 ¥12）' }
  },
  CERTS: {
    C1: { name: '基础洗车认证' }, C2: { name: '标准洗车认证' },
    C3: { name: '精细洗车认证', desc: '需在线考试 ≥ 80 分' },
    D: { name: '驾驶资格认证' }, O: { name: '操作规范认证' }
  },
  ORDER_STATUS: [
    { key: 'unpaid', label: '待支付' }, { key: 'accepted_pending', label: '待接单' },
    { key: 'accepted', label: '已接单' }, { key: 'picking', label: '取车中' },
    { key: 'washing', label: '洗车中' }, { key: 'delivering', label: '配送中' },
    { key: 'verifying', label: '待验收' }, { key: 'done', label: '完成' }
  ],
  BAY_STATUS: [
    { key: 'free', label: '有空位', color: '#18A058' },
    { key: 'full', label: '已满', color: '#E5484D' },
    { key: 'paused', label: '暂停', color: '#97A3B6' }
  ],
  REVIEW_DIMS: [
    { key: 'attitude', label: '服务态度' }, { key: 'quality', label: '清洁质量' },
    { key: 'punctual', label: '准时交付' }, { key: 'protect', label: '车辆保护' },
    { key: 'communicate', label: '沟通响应' }
  ],
  LOW_SCORE: 2,
  SIM_BANNER_TEXT: '测试环境 · 模拟支付 · 不产生真实资金收付',
  SIM_SUCCESS_TEXT: '模拟支付成功',
  SIM_SUCCESS_FULL: '模拟支付成功 · 测试数据，不产生真实资金收付'
};

export function fen2Yuan(fen) {
  return (Number(fen || 0) / 100).toFixed(2);
}

function getBaseUrl() {
  try {
    const app = getApp();
    if (app && app.globalData && app.globalData.baseURL) return app.globalData.baseURL;
  } catch (e) {}
  return FALLBACK_BASE_URL;
}

function getToken() {
  try {
    const app = getApp();
    return app && app.globalData ? app.globalData.token : '';
  } catch (e) { return ''; }
}

function buildUrl(url) {
  const base = String(getBaseUrl()).replace(/\/$/, '');
  if (/^https?:\/\//i.test(url)) return url;
  return base + '/' + String(url).replace(/^\//, '');
}

export function request(options) {
  const opts = options || {};
  const needAuth = opts.needAuth !== false;
  const header = { 'Content-Type': 'application/json' };
  if (needAuth && getToken()) header.Authorization = 'Bearer ' + getToken();
  return new Promise((resolve, reject) => {
    uni.request({
      url: buildUrl(opts.url || ''),
      method: opts.method || 'GET',
      data: opts.data || {},
      header,
      timeout: opts.timeout || 10000,
      success(res) {
        const body = res.data || {};
        if (res.statusCode >= 200 && res.statusCode < 300 && C.SUCCESS_CODES.indexOf(body.code) !== -1) {
          resolve(body.data); return;
        }
        const msg = body.msg || body.message || '请求失败';
        uni.showToast({ title: msg, icon: 'none' });
        const err = new Error(msg); err.code = body.code;
        reject(err);
      },
      fail(err) {
        uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' });
        reject(err);
      }
    });
  });
}

const get = (url, data) => request({ url, method: 'GET', data });
const post = (url, data) => request({ url, method: 'POST', data });

export default {
  request, get, post, fen2Yuan,
  user: {
    getHome: () => get('/user/home'),
    createOrder: (data) => post('/user/orders', data),
    getOrder: (id) => get('/user/orders/' + id)
  },
  worker: {
    getHall: () => get('/worker/hall'),
    grabOrder: (id) => post('/worker/orders/' + id + '/grab'),
    submitEvidence: (data) => post('/worker/evidence', data),
    getExams: () => get('/worker/exams'),
    submitExam: (data) => post('/worker/exams/submit', data),
    getCerts: () => get('/worker/certs'),
    getIncome: () => get('/worker/income')
  },
  store: {
    getBays: () => get('/store/bays'),
    updateBay: (id, data) => request({ url: '/store/bays/' + id, method: 'PUT', data }),
    acceptOrder: (id, data) => post('/store/orders/' + id + '/accept', data),
    getReviews: (params) => get('/store/reviews', params),
    replyReview: (id, data) => post('/store/reviews/' + id + '/reply', data)
  }
};
