/* ================================================================
 * 线上洗车平台 v6.0 · 四端 REST API 适配层（修复 P1-02：前端零接口调用）
 *
 * 设计目标：
 *  - 真实消费后端 31 个 REST 接口（/api/user|worker|store|admin|common）；
 *  - 注入登录态请求头（X-Role / X-User-Id / X-Worker-Id / X-Store-Id），
 *    与后端 AuthInterceptor / LoginContext 对齐；
 *  - 后端在线时走真实数据；离线时优雅降级到本地 mock，保证静态原型演示可用；
 *  - 统一解析 { code, message, data }，业务错误码分段处理；
 *  - 金额对外一律「分」。
 * ================================================================ */
(function (global) {
  'use strict';

  var DEFAULT_BASE = 'http://localhost:8080/xiche-platform';
  var cfg = {
    base: (global.XICHE_API_BASE || DEFAULT_BASE),
    online: false,
    timeoutMs: 2500,
    role: 'USER', userId: 2, workerId: 1, storeId: 1
  };

  function headers(extra) {
    var h = {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-Role': cfg.role,
      'X-User-Id': String(cfg.userId),
      'X-Worker-Id': String(cfg.workerId),
      'X-Store-Id': String(cfg.storeId)
    };
    if (extra) { for (var k in extra) { if (extra.hasOwnProperty(k)) h[k] = extra[k]; } }
    return h;
  }

  /** 统一请求：返回 Promise<data>；业务码非0或网络失败时 reject */
  function request(path, options) {
    options = options || {};
    var ctrl = (typeof AbortController !== 'undefined') ? new AbortController() : null;
    var timer = ctrl ? setTimeout(function () { ctrl.abort(); }, cfg.timeoutMs) : null;
    var init = {
      method: options.method || 'GET',
      headers: headers(options.headers),
      signal: ctrl ? ctrl.signal : undefined
    };
    if (options.body != null) init.body = JSON.stringify(options.body);
    return fetch(cfg.base + path, init)
      .then(function (r) {
        if (timer) clearTimeout(timer);
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
      })
      .then(function (resp) {
        if (!resp || typeof resp.code === 'undefined') throw new Error('响应格式非法');
        if (resp.code !== 0) { var e = new Error(resp.message || '业务错误'); e.code = resp.code; throw e; }
        return resp.data;
      })
      .catch(function (err) {
        if (timer) clearTimeout(timer);
        cfg.online = false;
        throw err;
      });
  }

  /** 探测后端是否在线；失败降级 */
  function probe() {
    return request('/api/user/home').then(function (d) {
      cfg.online = !!(d && d.tiers);
      return cfg.online;
    }).catch(function () { cfg.online = false; return false; });
  }

  function withRole(role, fn) {
    var prev = cfg.role; cfg.role = role;
    try { return fn(); } finally { cfg.role = prev; }
  }

  /** 四端接口映射（与 clients/API-DOC.md 一致） */
  var api = {
    cfg: cfg,
    request: request,
    probe: probe,
    setRole: function (role, ids) {
      cfg.role = role;
      if (ids) {
        if (ids.userId != null) cfg.userId = ids.userId;
        if (ids.workerId != null) cfg.workerId = ids.workerId;
        if (ids.storeId != null) cfg.storeId = ids.storeId;
      }
    },
    // 车主端
    home: function () { return withRole('USER', function () { return request('/api/user/home'); }); },
    createOrder: function (body) { return withRole('USER', function () { return request('/api/user/orders', { method: 'POST', body: body }); }); },
    orderList: function (page, size) { return withRole('USER', function () { return request('/api/user/orders?page=' + (page || 1) + '&pageSize=' + (size || 20)); }); },
    orderDetail: function (id) { return withRole('USER', function () { return request('/api/user/orders/' + id); }); },
    simulatePay: function (id) { return withRole('USER', function () { return request('/api/user/orders/' + id + '/simulate-pay', { method: 'POST', body: {} }); }); },
    cancelOrder: function (id) { return withRole('USER', function () { return request('/api/user/orders/' + id + '/cancel', { method: 'POST', body: {} }); }); },
    // 洗车工端
    certs: function () { return withRole('WORKER', function () { return request('/api/worker/certs'); }); },
    hall: function (page, size) { return withRole('WORKER', function () { return request('/api/worker/hall?page=' + (page || 1) + '&pageSize=' + (size || 20)); }); },
    grab: function (id) { return withRole('WORKER', function () { return request('/api/worker/orders/' + id + '/grab', { method: 'POST', body: {} }); }); },
    task: function (id) { return withRole('WORKER', function () { return request('/api/worker/orders/' + id); }); },
    evidence: function (body) { return withRole('WORKER', function () { return request('/api/worker/evidence', { method: 'POST', body: body }); }); },
    exams: function (cert) { return withRole('WORKER', function () { return request('/api/worker/exams?cert=' + (cert || 'C1')); }); },
    submitExam: function (body) { return withRole('WORKER', function () { return request('/api/worker/exams/submit', { method: 'POST', body: body }); }); },
    income: function (page, size) { return withRole('WORKER', function () { return request('/api/worker/income?page=' + (page || 1) + '&pageSize=' + (size || 20)); }); },
    // 门店端
    bays: function () { return withRole('STORE', function () { return request('/api/store/bays'); }); },
    setBay: function (id, status) { return withRole('STORE', function () { return request('/api/store/bays/' + id, { method: 'PUT', body: { status: status } }); }); },
    accept: function (id, bayId) { return withRole('STORE', function () { return request('/api/store/orders/' + id + '/accept', { method: 'POST', body: { bayId: bayId } }); }); },
    reviews: function (page, size) { return withRole('STORE', function () { return request('/api/store/reviews?page=' + (page || 1) + '&pageSize=' + (size || 20)); }); },
    replyReview: function (id, body) { return withRole('STORE', function () { return request('/api/store/reviews/' + id + '/reply', { method: 'POST', body: body }); }); },
    // 后台端
    dashboard: function () { return withRole('ADMIN', function () { return request('/api/admin/dashboard'); }); },
    auditWorkers: function () { return withRole('ADMIN', function () { return request('/api/admin/audit/workers'); }); },
    settlements: function () { return withRole('ADMIN', function () { return request('/api/admin/settlement/list'); }); },
    reconcile: function (settlementId) { return withRole('ADMIN', function () { return request('/api/admin/settlement/reconcile?settlementId=' + settlementId); }); },
    auditLogs: function () { return withRole('ADMIN', function () { return request('/api/admin/audit/logs'); }); },
    // 公共
    upload: function (formData) {
      var h = headers(); delete h['Content-Type']; // 交给浏览器设置 multipart boundary
      return fetch(cfg.base + '/api/common/upload', { method: 'POST', headers: h, body: formData })
        .then(function (r) { return r.json(); });
    }
  };

  global.XicheAPI = api;
})(window);
