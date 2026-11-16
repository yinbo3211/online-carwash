/* ================================================================
 * 线上洗车平台 v6.0 · H5 用户端（纯静态，浏览器直接打开）
 * - 无构建、无依赖；hash 路由；示例数据本地降级，后端 ready 后可选接入
 * - API baseURL 集中配置：API_BASE，指向后端 /api/
 * - 金额一律以「分」计算，展示用 fen2Yuan()；价格为示例值
 * - V1 模拟支付：成功态为橙色「模拟支付成功」，严禁绿色「支付成功」
 * ================================================================ */
(function () {
  'use strict';

  /* ---------- 集中配置 ---------- */
  var API_BASE = '/api/';
  var USE_BACKEND = false; // 演示默认走本地示例数据；后端就绪后改为 true

  /* ---------- 业务常量（三端口径一致） ---------- */
  var C = {
    SERVICE_TIERS: [
      { key: 'basic', name: '外观清洁', priceFen: 8300, desc: '高压冲洗 · 中性泡沫 · 轮毂清洁',
        features: ['高压冲洗车身', '中性泡沫去污', '轮毂清洁', '外观擦干'], recommend: false },
      { key: 'standard', name: '标准清洁', priceFen: 13300, desc: '外观清洁 · 内饰吸尘 · 中控擦拭',
        features: ['含外观清洁全部项目', '内饰吸尘', '中控台擦拭', '车窗清洁'], recommend: true },
      { key: 'premium', name: '精细清洁', priceFen: 24300, desc: '标准清洁 · 深度去污 · 打蜡养护',
        features: ['含标准清洁全部项目', '缝隙深度刷洗', '漆面去污', '打蜡养护'], recommend: false }
    ],
    DOOR_PRICE_FEN: { basic: 9500, standard: 15500 },
    ADDONS: [
      { key: 'wax', name: '漆面打蜡', priceFen: 3000 },
      { key: 'interior', name: '内饰深度清洁', priceFen: 5000 },
      { key: 'disinfect', name: '车内消毒', priceFen: 2000 },
      { key: 'tire', name: '轮胎养护', priceFen: 1500 }
    ],
    MAX_ADDONS: 4,
    L1: { name: 'L1 平台强制保障', required: true, cancelable: false, priceFen: 0 },
    L2: { name: 'L2 增值保险', priceFen: 1200 },
    CERTS: [
      { key: 'C1', name: '基础洗车认证', passed: true },
      { key: 'C2', name: '标准洗车认证', passed: true },
      { key: 'C3', name: '精细洗车认证', passed: false },
      { key: 'D', name: '驾驶资格认证', passed: true },
      { key: 'O', name: '操作规范认证', passed: true }
    ],
    ORDER_STATUS: ['待支付', '待接单', '已接单', '取车中', '洗车中', '配送中', '待验收', '完成'],
    BAY_STATUS: [
      { key: 'free', label: '有空位' }, { key: 'full', label: '已满' }, { key: 'paused', label: '暂停' }
    ],
    REVIEW_DIMS: ['服务态度', '清洁质量', '准时交付', '车辆保护', '沟通响应'],
    LOW_SCORE: 2,
    SIM_SUCCESS: '模拟支付成功',
    SIM_SUCCESS_FULL: '模拟支付成功 · 测试数据，不产生真实资金收付'
  };

  /* ---------- 运行时状态 ---------- */
  var state = {
    view: 'home',
    tier: 'standard', mode: 'store', address: '', phone: '',
    addons: {}, l2: false, couponOn: true, couponFen: 1000,
    orderNo: '', amountFen: 0, statusIndex: 6,
    bays: [
      { name: 'Bay-01', status: 'free' },
      { name: 'Bay-02', status: 'full' },
      { name: 'Bay-03', status: 'paused' }
    ],
    review: {}
  };

  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  function fen2Yuan(fen) { return (Number(fen || 0) / 100).toFixed(2); }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }
  function tag(text, cls) { return '<span class="tag ' + (cls || 'tag-primary') + '">' + text + '</span>'; }

  /* ---------- 金额计算（分） ---------- */
  function tier() { return C.SERVICE_TIERS.find(function (t) { return t.key === state.tier; }) || C.SERVICE_TIERS[1]; }
  function doorDisabled() { return state.tier === 'premium'; }
  function baseFen() {
    if (state.mode === 'door' && !doorDisabled()) return C.DOOR_PRICE_FEN[state.tier] || tier().priceFen;
    return tier().priceFen;
  }
  function checkedAddons() { return C.ADDONS.filter(function (a) { return state.addons[a.key]; }); }
  function addonFen() { return checkedAddons().reduce(function (s, a) { return s + a.priceFen; }, 0); }
  function l2Fen() { return state.l2 ? C.L2.priceFen : 0; }
  function couponFen() { return state.couponOn ? state.couponFen : 0; }
  function totalFen() { return Math.max(0, baseFen() + addonFen() + l2Fen() - couponFen()); }

  /* ---------- Toast / Modal ---------- */
  var toastTimer = null;
  function toast(msg) {
    var el = $('#toast');
    el.textContent = msg;
    el.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.classList.remove('show'); }, 2600);
  }
  function openModal(html) {
    $('#modal-root').innerHTML = '<div class="modal-mask" data-act="modal-mask"><div class="modal">' + html + '</div></div>';
  }
  function closeModal() { $('#modal-root').innerHTML = ''; }

  /* ---------- 视图：首页 ---------- */
  function renderHome() {
    var tierCards = C.SERVICE_TIERS.map(function (t) {
      var features = t.features.map(function (f) { return '<li>' + f + '</li>'; }).join('');
      return '<div class="tier ' + (t.key === state.tier ? 'on' : '') + (t.recommend ? ' recommend' : '') +
        '" data-act="tier" data-key="' + t.key + '">' +
        '<div class="tier-name">' + t.name + '</div>' +
        '<div class="tier-desc">' + t.desc + '</div>' +
        '<div class="price tier-price">' + fen2Yuan(t.priceFen) + '</div>' +
        '<ul class="tier-features">' + features + '</ul>' +
        '<div class="price-example">示例价</div></div>';
    }).join('');

    var bays = state.bays.map(function (b) {
      var label = C.BAY_STATUS.find(function (s) { return s.key === b.status; }).label;
      return '<div class="bay" data-status="' + b.status + '" data-act="bay" data-name="' + b.name + '">' +
        '<div class="bay-name">' + b.name + '</div><div class="bay-dot"></div>' +
        '<div class="bay-status">' + label + '</div></div>';
    }).join('');

    $('#view-home').innerHTML =
      '<div class="hero"><h1>线上洗车 · 取送车服务</h1>' +
      '<p>一键下单，洗车工上门取送，全程留证</p>' +
      '<div class="hero-tags">' + tag('L1 平台强制保障') + tag('价格为示例值', 'tag-gray') + '</div></div>' +

      '<div class="card"><div class="card-title"><span>选择服务档位</span><span class="muted">到店价 · 金额单位：分 → 元</span></div>' +
      '<div class="tier-grid">' + tierCards + '</div>' +
      '<div class="section-note">* 以上为示例价，实际以服务端返回为准；上门价：外观洗 ¥95 / 标准洗 ¥155。</div></div>' +

      '<div class="card"><div class="card-title"><span>快捷入口</span></div><div class="entries">' +
      '<a class="entry" href="#/order"><div class="entry-icon">🚗</div><div class="entry-label">立即下单</div></a>' +
      '<a class="entry" href="#/progress"><div class="entry-icon">📋</div><div class="entry-label">订单进度</div></a>' +
      '<a class="entry" data-act="sim-info" href="#/pay-success"><div class="entry-icon">💳</div><div class="entry-label">模拟支付</div></a>' +
      '</div></div>' +

      '<div class="card"><div class="card-title"><span>附近门店工位</span><span class="muted">点击卡片切换三态</span></div>' +
      '<div class="bay-grid">' + bays + '</div>' +
      '<div class="legend"><span><i style="background:#18A058"></i>有空位</span>' +
      '<span><i style="background:#E5484D"></i>已满</span><span><i style="background:#97A3B6"></i>暂停</span></div></div>';
  }

  /* ---------- 视图：下单 ---------- */
  function renderOrder() {
    var tierHtml = C.SERVICE_TIERS.map(function (t) {
      return '<div class="tier ' + (t.key === state.tier ? 'on' : '') + (t.recommend ? ' recommend' : '') +
        '" data-act="tier" data-key="' + t.key + '"><div class="tier-name">' + t.name + '</div>' +
        '<div class="tier-desc">' + t.desc + '</div><div class="price tier-price">' + fen2Yuan(t.priceFen) + '</div></div>';
    }).join('');

    var doorPrice = doorDisabled() ? tier().priceFen : (C.DOOR_PRICE_FEN[state.tier] || tier().priceFen);
    var addonHtml = C.ADDONS.map(function (a) {
      var on = !!state.addons[a.key];
      return '<div class="check-row" data-act="addon" data-key="' + a.key + '">' +
        '<div class="checkbox ' + (on ? 'checked' : '') + '">' + (on ? '✓' : '') + '</div>' +
        '<span class="grow">' + a.name + '</span><span class="price">' + fen2Yuan(a.priceFen) + '</span></div>';
    }).join('');

    $('#view-order').innerHTML =
      '<div class="card"><div class="card-title"><span>1 · 服务档位</span><span class="muted">示例价</span></div>' +
      '<div class="tier-grid">' + tierHtml + '</div></div>' +

      '<div class="card"><div class="card-title"><span>2 · 服务方式</span></div>' +
      '<div class="seg"><div class="seg-item ' + (state.mode === 'store' ? 'on' : '') + '" data-act="mode" data-mode="store">到店洗</div>' +
      '<div class="seg-item ' + (state.mode === 'door' ? 'on' : '') + (doorDisabled() ? ' off' : '') +
      '" data-act="mode" data-mode="door">上门取送 <span class="price">' + fen2Yuan(doorPrice) + '</span></div></div>' +
      (doorDisabled() ? '<div class="hint">精细清洁暂不支持上门，请选择到店或切换档位</div>' : '') +
      '<div class="form-row"><label class="form-label">取送地址（上门时必填）</label>' +
      '<input class="input" id="f-address" placeholder="请输入详细地址" value="' + esc(state.address) + '" /></div>' +
      '<div class="form-row"><label class="form-label">联系手机（11 位中国大陆手机号）</label>' +
      '<input class="input" id="f-phone" type="tel" maxlength="11" placeholder="请输入手机号" value="' + esc(state.phone) + '" /></div></div>' +

      '<div class="card"><div class="card-title"><span>3 · 增值项目</span><span class="muted">最多 ' + C.MAX_ADDONS + ' 项 · 已选 ' + checkedAddons().length + '</span></div>' +
      addonHtml + '</div>' +

      '<div class="card"><div class="card-title"><span>4 · 保险保障</span></div>' +
      '<div class="ins-locked"><span class="lock-icon">🔒</span><div class="checkbox checked">✓</div>' +
      '<div class="grow"><div><b>L1 平台强制保障</b> ' + tag('强制', 'tag-danger') + '</div>' +
      '<div class="muted">平台强制保障，不可取消（¥0）</div></div></div>' +
      '<div class="hint">L1 为平台强制保障，已锁定，不可取消</div>' +
      '<div class="divider"></div>' +
      '<div class="check-row" data-act="l2"><div class="checkbox ' + (state.l2 ? 'checked' : '') + '">' + (state.l2 ? '✓' : '') + '</div>' +
      '<div class="grow"><b>L2 增值保险</b> ' + tag('可选') + '<div class="muted">可选投保（示例价 ¥12）</div></div>' +
      '<span class="price">' + fen2Yuan(C.L2.priceFen) + '</span></div>' +

      '<div class="divider"></div>' +
      '<div class="check-row" data-act="coupon"><div class="checkbox ' + (state.couponOn ? 'checked' : '') + '">' + (state.couponOn ? '✓' : '') + '</div>' +
      '<span class="grow">新客立减券（示例）</span><span class="price">-' + fen2Yuan(state.couponFen) + '</span></div></div>' +

      '<div class="card"><div class="card-title"><span>费用明细</span><span class="muted">内部以「分」计算</span></div>' +
      '<div class="summary-row"><span>服务费</span><span class="price">' + fen2Yuan(baseFen()) + '</span></div>' +
      '<div class="summary-row"><span>增值项目</span><span class="price">' + fen2Yuan(addonFen()) + '</span></div>' +
      '<div class="summary-row"><span>L2 增值保险</span><span class="price">' + fen2Yuan(l2Fen()) + '</span></div>' +
      '<div class="summary-row"><span>优惠券</span><span class="price">-' + fen2Yuan(couponFen()) + '</span></div>' +
      '<div class="summary-total"><span>合计（示例）</span><span class="price">' + fen2Yuan(totalFen()) + '</span></div></div>' +

      '<div class="paybar"><div class="grow"><div class="muted">应付合计</div><div class="price">' + fen2Yuan(totalFen()) + '</div></div>' +
      '<button class="btn btn-sim" data-act="submit-order">模拟支付并提交</button></div>';

    $('#f-address').addEventListener('input', function (e) { state.address = e.target.value; });
    $('#f-phone').addEventListener('input', function (e) { state.phone = e.target.value; });
  }

  /* ---------- 视图：模拟支付成功 ---------- */
  function renderPaySuccess() {
    $('#view-pay-success').innerHTML =
      '<div class="success-wrap">' +
      '<div class="success-icon sim-success">✓</div>' +
      '<div class="success-title sim-success">' + C.SIM_SUCCESS + '</div>' +
      '<div class="success-desc sim-success-desc">测试数据，不产生真实资金收付</div>' +

      '<div class="card success-card">' +
      '<div class="between"><span>订单编号</span><b>' + esc(state.orderNo || 'WC20260616-0083') + '</b></div>' +
      '<div class="divider"></div>' +
      '<div class="between"><span>服务项目</span><span>' + tier().name + '</span></div>' +
      '<div class="divider"></div>' +
      '<div class="between"><span>模拟支付金额</span><span class="price" style="font-size:22px">' + fen2Yuan(state.amountFen) + '</span></div>' +
      '<div class="divider"></div>' +
      '<div class="between"><span>保险</span>' + tag('L1 强制', 'tag-danger') + '</div></div>' +

      '<div class="sim-banner sim-banner--inline"><span>🔒</span><span>本页为 V1 测试版模拟结果，未发生任何真实扣款或资金结算。</span></div>' +
      '<div class="success-actions">' +
      '<a class="btn btn-primary btn-block" href="#/progress">查看订单进度</a>' +
      '<a class="btn btn-ghost btn-block" href="#/home">返回首页</a></div></div>';
  }

  /* ---------- 视图：订单进度（8 态 + 证据 + 资质 + 评价） ---------- */
  var DEMO_EVIDENCE = [
    { stage: 'E1 接车预检', photos: ['🚗', '📷'] },
    { stage: 'E2 外观清洗', photos: ['🫧', '✨'] },
    { stage: 'E3 内饰清洁', photos: ['🧽', '🪑'] },
    { stage: 'E4 交车确认', photos: ['🤝', '📷'] }
  ];
  function renderProgress() {
    var steps = C.ORDER_STATUS.map(function (label, i) {
      var cls = i < state.statusIndex ? 'done' : (i === state.statusIndex ? 'current' : '');
      return '<div class="stepper-item ' + cls + '"><div class="stepper-dot">' +
        (i < state.statusIndex ? '✓' : (i + 1)) + '</div><div class="stepper-label">' + label + '</div></div>';
    }).join('');

    var evidence = DEMO_EVIDENCE.map(function (e) {
      return '<div><div class="evidence-stage">' + e.stage + '</div><div class="thumbs">' +
        e.photos.map(function (p) { return '<div class="thumb">' + p + '</div>'; }).join('') + '</div></div>';
    }).join('');

    var certs = C.CERTS.map(function (c) {
      return tag(c.key + ' ' + c.name, c.passed ? 'tag-success' : 'tag-gray');
    }).join(' ');

    $('#view-progress').innerHTML =
      '<div class="card"><div class="card-title"><span>' + esc(state.orderNo || 'WC20260616-0083') + '</span>' +
      tag(C.ORDER_STATUS[state.statusIndex], state.statusIndex === 6 ? 'tag-warning' : 'tag-primary') + '</div>' +
      '<div class="stepper">' + steps + '</div>' +
      (state.statusIndex === 6 ? '<div class="hint">待验收：请核对车辆与清洁效果（警告色提示）</div>' : '') + '</div>' +

      '<div class="card"><div class="card-title"><span>取还车证据</span><span class="muted">现场拍摄留痕 · 带订单号水印</span></div>' + evidence + '</div>' +

      '<div class="card"><div class="card-title"><span>服务信息</span></div>' +
      '<div class="between"><span>洗车工资质</span><div>' + certs + '</div></div>' +
      '<div class="divider"></div><div class="between"><span>车型 / 车牌</span><b>粤B·D12345</b></div>' +
      '<div class="divider"></div><div class="between"><span>实付（示例）</span><span class="price">' + fen2Yuan(state.amountFen || 14800) + '</span></div>' +
      '<div class="divider"></div><div class="between"><span>保险</span>' + tag('L1 强制', 'tag-danger') + tag('L2 已投保') + '</div>' +
      '<div class="divider"></div><div class="muted">C3 精细认证未通过：对应精细清洁订单置灰锁定 + 去补认证入口。</div></div>' +

      '<div class="between" style="gap:12px;flex-wrap:wrap">' +
      '<button class="btn btn-ghost" data-act="contact">联系洗车工</button>' +
      '<button class="btn btn-primary" data-act="open-review">五维评价</button></div>';
  }

  /* ---------- 五维评价弹窗 ---------- */
  function reviewModal() {
    var rows = C.REVIEW_DIMS.map(function (d, di) {
      var score = state.review[di] || 0;
      var stars = [1, 2, 3, 4, 5].map(function (n) {
        return '<span class="star ' + (n <= score ? 'on' : '') + '" data-act="star" data-dim="' + di + '" data-score="' + n + '">★</span>';
      }).join('');
      var reason = score && score <= C.LOW_SCORE
        ? '<input class="input" style="margin-top:8px" placeholder="低分原因（必填）" data-act="reason" data-dim="' + di +
          '" value="' + esc(state.review['r' + di] || '') + '" />'
        : '';
      return '<div class="dim-row"><span class="dim-name">' + d + '</span><div class="stars">' + stars + '</div></div>' + reason;
    }).join('');
    openModal('<h3>五维评价</h3><div class="muted" style="margin-bottom:12px">任一维度 ≤2 星需强制填写原因。</div>' +
      rows + '<div class="modal-actions"><button class="btn btn-outline" data-act="close-modal">取消</button>' +
      '<button class="btn btn-primary" data-act="submit-review">提交评价</button></div>');
  }

  /* ---------- 路由与渲染 ---------- */
  function route() {
    var hash = (location.hash || '#/home').replace('#/', '');
    if (['home', 'order', 'pay-success', 'progress'].indexOf(hash) === -1) hash = 'home';
    state.view = hash;
    $$('.view').forEach(function (el) { el.hidden = true; });
    var view = $('#view-' + hash);
    if (view) view.hidden = false;
    $$('.nav-link').forEach(function (a) { a.classList.toggle('active', a.dataset.nav === hash || (hash === 'pay-success' && a.dataset.nav === 'order')); });
    if (hash === 'home') renderHome();
    if (hash === 'order') renderOrder();
    if (hash === 'pay-success') renderPaySuccess();
    if (hash === 'progress') renderProgress();
    window.scrollTo(0, 0);
  }

  /* ---------- 事件委托 ---------- */
  document.addEventListener('click', function (e) {
    var t = e.target.closest('[data-act]');
    if (!t) return;
    var act = t.dataset.act;

    if (act === 'tier') {
      state.tier = t.dataset.key;
      if (state.tier === 'premium') state.mode = 'store';
      toast('已选择：' + C.SERVICE_TIERS.find(function (x) { return x.key === state.tier; }).name);
      route();
    } else if (act === 'mode') {
      if (t.dataset.mode === 'door' && doorDisabled()) { toast('精细清洁暂不支持上门'); return; }
      state.mode = t.dataset.mode;
      renderOrder();
    } else if (act === 'addon') {
      var key = t.dataset.key;
      if (!state.addons[key] && checkedAddons().length >= C.MAX_ADDONS) { toast('最多选 ' + C.MAX_ADDONS + ' 项'); return; }
      state.addons[key] = !state.addons[key];
      renderOrder();
    } else if (act === 'l2') {
      state.l2 = !state.l2;
      toast(state.l2 ? '已选购 L2 增值保险（+¥12，模拟）' : '已取消 L2 增值保险');
      renderOrder();
    } else if (act === 'coupon') {
      state.couponOn = !state.couponOn;
      renderOrder();
    } else if (act === 'bay') {
      var order = ['free', 'full', 'paused'];
      var b = state.bays.find(function (x) { return x.name === t.dataset.name; });
      b.status = order[(order.indexOf(b.status) + 1) % order.length];
      toast(b.name + ' → ' + C.BAY_STATUS.find(function (s) { return s.key === b.status; }).label);
      renderHome();
    } else if (act === 'sim-info') {
      toast(C.SIM_SUCCESS_FULL);
    } else if (act === 'submit-order') {
      submitOrder();
    } else if (act === 'contact') {
      toast('已拨打洗车工（模拟）');
    } else if (act === 'open-review') {
      reviewModal();
    } else if (act === 'star') {
      var di = Number(t.dataset.dim);
      state.review[di] = Number(t.dataset.score);
      reviewModal();
    } else if (act === 'reason') {
      return;
    } else if (act === 'submit-review') {
      var missing = C.REVIEW_DIMS.some(function (_, i) { return !state.review[i]; });
      var lowNoReason = C.REVIEW_DIMS.some(function (_, i) {
        return state.review[i] && state.review[i] <= C.LOW_SCORE && !String(state.review['r' + i] || '').trim();
      });
      if (missing) { toast('请完成全部五维评分'); return; }
      if (lowNoReason) { toast('低分维度需填写原因'); return; }
      closeModal(); toast('五维评价已提交（模拟）');
    } else if (act === 'close-modal' || act === 'modal-mask') {
      closeModal();
    }
  });

  document.addEventListener('input', function (e) {
    if (e.target.dataset.act === 'reason') state.review['r' + e.target.dataset.dim] = e.target.value;
  });

  /* ---------- 提交下单（模拟支付） ---------- */
  function submitOrder() {
    if (state.mode === 'door') {
      if (!/^1\d{10}$/.test(state.phone)) { toast('请输入 11 位中国大陆手机号'); return; }
      if (!state.address.trim()) { toast('请输入取送地址'); return; }
    }
    var d = new Date();
    state.orderNo = 'WC' + d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') +
      String(d.getDate()).padStart(2, '0') + '-' + String(Math.floor(Math.random() * 900) + 100);
    state.amountFen = totalFen();

    var payload = {
      tier: state.tier, serviceMode: state.mode, address: state.address, phone: state.phone,
      addonKeys: checkedAddons().map(function (a) { return a.key; }),
      insurance: { l1Accepted: true, l1Cancelable: false, l2Selected: state.l2 },
      couponFen: couponFen(), amountFen: state.amountFen,
      requestId: 'req_' + Date.now()
    };

    // 后端就绪时打开 USE_BACKEND，统一走 /api/ 并使用 {code,msg,data} 响应
    if (USE_BACKEND) {
      fetch(API_BASE.replace(/\/$/, '') + '/user/orders', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
      }).catch(function () { /* 演示环境降级为本地模拟成功 */ });
    }
    location.hash = '#/pay-success';
    toast(C.SIM_SUCCESS_FULL);
  }

  window.addEventListener('hashchange', route);
  route();
})();
