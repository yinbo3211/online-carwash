/**
 * 线上洗车平台 v6.0 · 小程序 API 封装
 * - baseURL 集中配置：优先取 app.globalData.baseURL，回退 constants.BASE_URL
 * - 统一解包后端 { code, msg, data }：成功 resolve(data)，失败 reject(Error)
 * - 401 清除本地登录态（不自动跳页，避免与业务页导航冲突；页面可监听 globalData.token）
 */
const { BASE_URL, SUCCESS_CODES } = require('./constants.js');

function getBaseUrl() {
  try {
    const app = getApp();
    if (app && app.globalData && app.globalData.baseURL) return app.globalData.baseURL;
  } catch (e) {}
  return BASE_URL;
}

function buildUrl(url) {
  const base = String(getBaseUrl()).replace(/\/$/, '');
  if (/^https?:\/\//i.test(url)) return url;
  return base + '/' + String(url).replace(/^\//, '');
}

function getToken() {
  try {
    const app = getApp();
    return app && app.globalData ? app.globalData.token : '';
  } catch (e) {
    return '';
  }
}

function request(options) {
  const opts = options || {};
  const method = (opts.method || 'GET').toUpperCase();
  const needAuth = opts.needAuth !== false;
  const showError = opts.showError !== false;
  const header = { 'Content-Type': 'application/json' };
  if (needAuth && getToken()) header['Authorization'] = 'Bearer ' + getToken();

  return new Promise((resolve, reject) => {
    wx.request({
      url: buildUrl(opts.url || ''),
      method,
      data: opts.data || {},
      header,
      timeout: opts.timeout || 10000,
      success(res) {
        const body = res.data || {};
        const code = body.code;
        const httpOk = res.statusCode >= 200 && res.statusCode < 300;
        if (httpOk && SUCCESS_CODES.indexOf(code) !== -1) {
          resolve(body.data);
          return;
        }
        if (res.statusCode === 401 || code === 401) {
          try {
            const app = getApp();
            if (app && app.logout) app.logout();
          } catch (e) {}
        }
        const msg = body.msg || body.message || '请求失败（' + res.statusCode + '）';
        const err = new Error(msg);
        err.code = code;
        err.response = res;
        if (showError) wx.showToast({ title: msg, icon: 'none', duration: 2500 });
        reject(err);
      },
      fail(err) {
        const e = new Error((err && err.errMsg) || '网络异常，请稍后重试');
        if (showError) wx.showToast({ title: e.message, icon: 'none' });
        reject(e);
      }
    });
  });
}

const get = (url, data, opt) => request(Object.assign({ url, method: 'GET', data }, opt));
const post = (url, data, opt) => request(Object.assign({ url, method: 'POST', data }, opt));
const put = (url, data, opt) => request(Object.assign({ url, method: 'PUT', data }, opt));
const del = (url, data, opt) => request(Object.assign({ url, method: 'DELETE', data }, opt));

/** 上传证据图（现场拍摄，返回后端文件访问 URL） */
function uploadEvidence(filePath, orderNo, stage) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: buildUrl('/common/upload'),
      filePath,
      name: 'file',
      // P1-12：后端 CommonController 接收 orderNo（非 orderId）
      formData: { orderNo: String(orderNo || ''), stage: stage || '' },
      header: getToken() ? { Authorization: 'Bearer ' + getToken() } : {},
      success(res) {
        let body = {};
        try { body = JSON.parse(res.data); } catch (e) {}
        if (SUCCESS_CODES.indexOf(body.code) !== -1) resolve(body.data);
        else reject(new Error(body.msg || '上传失败'));
      },
      fail: reject
    });
  });
}

module.exports = {
  request, get, post, put, del, uploadEvidence,
  // P0-05/P1-12：统一登录端点，换取服务端签名令牌
  auth: {
    login: (data) => post('/auth/login', data, { needAuth: false }),
    ping: () => get('/auth/ping', {}, { needAuth: false })
  },
  user: {
    getHome: () => get('/user/home'),
    createOrder: (data) => post('/user/orders', data),
    getOrder: (id) => get('/user/orders/' + id),
    listOrders: (params) => get('/user/orders', params),
    simulatePay: (orderId, data) => post('/user/orders/' + orderId + '/simulate-pay', data),
    cancelOrder: (orderId, data) => post('/user/orders/' + orderId + '/cancel', data)
  },
  worker: {
    getHall: (params) => get('/worker/hall', params),
    grabOrder: (orderId) => post('/worker/orders/' + orderId + '/grab'),
    getTask: (orderId) => get('/worker/orders/' + orderId),
    submitEvidence: (data) => post('/worker/evidence', data),
    getExams: (cert) => get('/worker/exams', cert ? { cert } : {}),
    submitExam: (data) => post('/worker/exams/submit', data),
    getIncome: (params) => get('/worker/income', params),
    getCerts: () => get('/worker/certs')
  },
  store: {
    getBays: () => get('/store/bays'),
    updateBay: (bayId, data) => put('/store/bays/' + bayId, data),
    acceptOrder: (orderId, data) => post('/store/orders/' + orderId + '/accept', data),
    getReviews: (params) => get('/store/reviews', params),
    replyReview: (reviewId, data) => post('/store/reviews/' + reviewId + '/reply', data)
  }
};
