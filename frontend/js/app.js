/* ================================================================
 * 线上洗车平台 v6.0 · 四端交互原型逻辑
 * 纯前端、无构建、无后端依赖。所有支付均为模拟，不产生真实资金收付。
 *
 * 性能取舍：
 *  - 全局事件委托（一个 click / input 监听），避免逐元素绑定；
 *  - 倒计时与搜索联想分别使用 setInterval + debounce，避免高频重排；
 *  - 页面视图用 display 切换而非重建 DOM，减少 GC；
 *  - 证据缩略图/图表/证件预览使用 CSS 与 emoji，零网络图片请求；
 *  - 注释中的 IntersectionObserver 方案可用于真实项目的分页/证据懒加载。
 * ================================================================ */
(function () {
  'use strict';

  var $ = function (sel, root) { return (root || document).querySelector(sel); };
  var $$ = function (sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); };

  /* ---------------- 静态数据 ---------------- */
  var TIERS = {
    basic:    { name: '外观洗', price: 83 },
    standard: { name: '标准洗', price: 133 },
    premium:  { name: '精细洗', price: 243 }
  };
  var STATUS = ['待支付', '待接单', '已接单', '取车中', '洗车中', '配送中', '待验收', '完成'];
  var TASK_STEPS = [
    { title: 'E1 接车预检', desc: '核对车辆外观、里程与随身物品，拍摄接车证据。' },
    { title: 'E2 外观清洗', desc: '高压冲洗、中性泡沫、轮毂清洁，拍摄外观成果。' },
    { title: 'E3 内饰清洁', desc: '吸尘、缝隙刷洗、中控擦拭，拍摄内饰成果。' },
    { title: 'E4 交车确认', desc: '与用户复核车辆状态，拍摄交车合影并确认。' }
  ];
  var QUESTIONS = [
    { q: '洗车作业开始前，第一步应当做什么？', o: ['环车检查并确认随身物品', '直接高压冲洗', '先抛光打蜡', '先向用户收费'], a: 0 },
    { q: '高压水枪冲洗车身时，推荐的水流角度是？', o: ['约 45° 斜向冲洗', '垂直 90° 直冲', '贴面 0° 平扫', '任意角度均可'], a: 0 },
    { q: 'L1 基础保险的正确描述是？', o: ['平台强制保障，不可取消', '用户可随时取消', '仅精细洗包含', '需额外付费'], a: 0 },
    { q: 'C3 精细认证的在线考试及格线是？', o: ['80 分', '60 分', '70 分', '90 分'], a: 0 },
    { q: '现场证据拍摄要求正确的是？', o: ['必须现场拍摄并带水印', '可用相册旧图', '只需拍 1 张', '无需订单号'], a: 0 },
    { q: '订单 Stepper 中「待验收」使用的语义色是？', o: ['警告色', '成功色', '危险色', '信息色'], a: 0 },
    { q: '取车信息页中联系手机应满足？', o: ['11 位中国大陆手机号', '任意长度数字', '必须含横线', '可为空'], a: 0 },
    { q: '增值项目最多可选几项？', o: ['4 项', '2 项', '6 项', '不限'], a: 0 },
    { q: '洗车工收入中平台服务费抽成比例是？', o: ['35%', '25%', '40%', '50%'], a: 0 },
    { q: '洗车工分成比例是？', o: ['40%', '25%', '35%', '30%'], a: 0 },
    { q: '工位状态「已满」对应的指示色是？', o: ['危险色 #E5484D', '成功色 #18A058', '灰色 #97A3B6', '警告色 #F0A020'], a: 0 },
    { q: '门店端结算仅显示哪一部分？', o: ['工位费 M 部分', '全部三方金额', '仅用户支付', '仅平台服务费'], a: 0 },
    { q: '认证不足的订单卡应如何展示？', o: ['置灰 + 锁图标 + 去补认证', '正常展示可抢', '直接隐藏', '标红即可'], a: 0 },
    { q: 'V1 测试版支付成功态应使用什么颜色？', o: ['橙色 #F5901B', '绿色 #18A058', '蓝色 #1668E3', '灰色'], a: 0 },
    { q: '模拟支付成功页的正确文案是？', o: ['模拟支付成功 · 测试数据，不产生真实资金收付', '支付成功', '交易已完成', '扣款成功'], a: 0 },
    { q: '抢单确认倒计时时长为？', o: ['90 秒', '30 秒', '60 秒', '120 秒'], a: 0 },
    { q: '任务执行 E1-E4 的顺序规则是？', o: ['完成上一步才解锁下一步', '可任意跳步', '只需完成 E4', '无顺序要求'], a: 0 },
    { q: '相册入口灰显提示的正确文案是？', o: ['仅支持现场拍摄', '可上传任意图片', '相册暂未开放', '请先登录'], a: 0 },
    { q: '五维评价中出现低分时应？', o: ['强制填写原因', '直接提交', '自动改为 5 星', '忽略该维度'], a: 0 },
    { q: '门店接单时若无空位应？', o: ['拦截并提示切换工位状态', '强制接单', '自动新增工位', '静默失败'], a: 0 }
  ];

  /* ---------------- 运行时状态 ---------------- */
  var state = {
    app: 'user',
    tier: 'standard',
    addons: {},
    addonPrice: {},
    l2: false,
    time: '今天 14:00-16:00',
    car: '',
    orderStatus: 6,
    aftersale: false,
    grabTimer: null,
    grabLeft: 90,
    grabOrder: '',
    examTimer: null,
    examLeft: 600,
    examIndex: 0,
    examAnswers: new Array(20).fill(null),
    examRunning: false,
    taskStep: 0,
    evidenceDone: 0,
    bays: ['free', 'free', 'full', 'free', 'paused', 'full'],
    review: {},
    genericTitle: '治理'
  };

  /* ---------------- 工具函数 ---------------- */
  function toast(message, type) {
    var root = $('#toast-root');
    var el = document.createElement('div');
    el.className = 'toast' + (type ? ' ' + type : '');
    el.textContent = message;
    root.appendChild(el);
    while (root.children.length > 3) root.removeChild(root.firstChild);
    window.setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, 2000);
  }

  function modal(options) {
    var cfg = options || {};
    var root = $('#modal-root');
    var mask = document.createElement('div');
    mask.className = 'modal-mask';
    mask.setAttribute('role', 'dialog');
    mask.setAttribute('aria-modal', 'true');
    mask.innerHTML =
      '<div class="modal">' +
        '<div class="modal-title"></div>' +
        '<div class="modal-body"></div>' +
        '<div class="modal-actions">' +
          '<button class="btn btn-secondary" data-modal-cancel>' + (cfg.cancelText || '取消') + '</button>' +
          '<button class="btn ' + (cfg.danger ? 'btn-danger' : 'btn-primary') + '" data-modal-ok>' + (cfg.okText || '确认') + '</button>' +
        '</div>' +
      '</div>';
    $('.modal-title', mask).textContent = cfg.title || '提示';
    $('.modal-body', mask).textContent = cfg.body || '';
    function close() { if (mask.parentNode) mask.parentNode.removeChild(mask); }
    $('[data-modal-cancel]', mask).addEventListener('click', close);
    $('[data-modal-ok]', mask).addEventListener('click', function () { close(); if (cfg.onOk) cfg.onOk(); });
    mask.addEventListener('click', function (e) { if (e.target === mask) close(); });
    root.appendChild(mask);
    $('[data-modal-ok]', mask).focus();
    return close;
  }

  function debounce(fn, wait) {
    var t = null;
    return function () {
      var ctx = this, args = arguments;
      window.clearTimeout(t);
      t = window.setTimeout(function () { fn.apply(ctx, args); }, wait || 150);
    };
  }
  function throttle(fn, wait) {
    var last = 0;
    return function () {
      var now = Date.now();
      if (now - last >= (wait || 200)) { last = now; fn.apply(this, arguments); }
    };
  }
  // 真实项目中可用 IntersectionObserver 对证据缩略图/长表格做进入视口再渲染，
  // 这里原型数据量小，直接全量渲染以保证双击即开、交互可点。

  function money(n) { return '¥' + Number(n).toFixed(0); }

  /* ---------------- 端切换与页面导航 ---------------- */
  function switchApp(appName) {
    state.app = appName;
    $$('.app-shell').forEach(function (shell) {
      shell.classList.toggle('is-active', shell.getAttribute('data-app') === appName);
    });
    $$('.switch-tab').forEach(function (tab) {
      var on = tab.getAttribute('data-switch-app') === appName;
      tab.classList.toggle('is-active', on);
      tab.setAttribute('aria-selected', on ? 'true' : 'false');
    });
  }

  function setTabActive(pageId) {
    var shell = $('[data-page="' + pageId + '"]');
    if (!shell) return;
    var owner = shell.closest('.app-shell');
    if (!owner) return;
    $$('.tab-item', owner).forEach(function (tab) {
      tab.classList.toggle('is-active', tab.getAttribute('data-tab') === pageId);
    });
  }

  function stopGrabTimer() {
    if (state.grabTimer) { window.clearInterval(state.grabTimer); state.grabTimer = null; }
  }
  function stopExamTimer() {
    if (state.examTimer) { window.clearInterval(state.examTimer); state.examTimer = null; }
    state.examRunning = false;
  }

  function navigate(pageId, opts) {
    opts = opts || {};
    var target = $('[data-page="' + pageId + '"]');
    if (!target) { toast('页面未找到：' + pageId, 'warn'); return; }
    var isAdmin = target.classList.contains('admin-page');
    // 只在目标所属的端内查找当前页，避免其他端残留 is-active 导致计时器漏清理
    var scope = isAdmin ? $('#app-admin') : target.closest('.app-shell');
    var current = $('.page.is-active, .admin-page.is-active', scope);
    var currentId = current ? current.getAttribute('data-page') : '';

    if (currentId !== pageId) {
      if (currentId === 'worker-grab') stopGrabTimer();
      if (currentId === 'worker-exam' && pageId !== 'worker-examresult') stopExamTimer();
    }

    if (isAdmin) {
      switchApp('admin');
      $$('.admin-page').forEach(function (p) { p.classList.toggle('is-active', p === target); });
      if (opts.crumb) $('#admin-crumb').textContent = opts.crumb;
    } else {
      var owner = target.closest('.app-shell');
      switchApp(owner.getAttribute('data-app'));
      $$('.page', owner).forEach(function (p) { p.classList.toggle('is-active', p === target); });
      setTabActive(pageId);
      var screen = $('.phone-screen', owner);
      if (screen) screen.scrollTop = 0;
    }

    var enter = {
      'user-tiers': renderTiers,
      'user-confirm': renderConfirm,
      'user-progress': function () { renderStepper(state.orderStatus); },
      'worker-grab': renderGrabPage,
      'worker-task': renderTask,
      'worker-evidence': renderEvidence,
      'worker-exam': renderExamQuestion,
      'store-workbench': renderBays,
      'store-review': renderStars,
      'admin-generic': function () { renderGeneric(state.genericTitle); }
    };
    if (enter[pageId]) enter[pageId]();
  }

  /* ---------------- 用户端逻辑 ---------------- */
  function selectedAddonNames() {
    return $$('#addon-grid .addon-item.is-on').map(function (el) {
      return { name: el.querySelector('.a-name').textContent.replace(/^\S+\s*/, ''), price: Number(el.getAttribute('data-price')) };
    });
  }
  function currentTotal() {
    var sum = TIERS[state.tier].price;
    selectedAddonNames().forEach(function (a) { sum += a.price; });
    if (state.l2) sum += 12;
    return sum;
  }
  function updateTotal() {
    var sum = currentTotal();
    var text = money(sum);
    $('#tier-total').textContent = text;
    $('#sum-subtotal').textContent = text;
    $('#sum-total').textContent = money(Math.max(0, sum - 20));
    $('#bar-total').textContent = money(Math.max(0, sum - 20));
    $('#addon-count').textContent = Object.keys(state.addons).length;
  }
  function selectTier(tier) {
    if (!TIERS[tier]) return;
    state.tier = tier;
    $$('.tier-card').forEach(function (el) {
      el.classList.toggle('is-selected', el.getAttribute('data-tier') === tier);
    });
    updateTotal();
  }
  function toggleAddon(el) {
    var key = el.getAttribute('data-addon');
    if (state.addons[key]) {
      delete state.addons[key];
      el.classList.remove('is-on');
    } else {
      if (Object.keys(state.addons).length >= 4) {
        toast('最多选择 4 项增值服务', 'warn');
        return;
      }
      state.addons[key] = true;
      el.classList.add('is-on');
    }
    var full = Object.keys(state.addons).length >= 4;
    $$('#addon-grid .addon-item').forEach(function (item) {
      if (!item.classList.contains('is-on')) item.disabled = full;
    });
    updateTotal();
  }
  function toggleInsurance() {
    state.l2 = !state.l2;
    var sw = $('#ins2-switch');
    sw.classList.toggle('is-on', state.l2);
    sw.setAttribute('aria-checked', state.l2 ? 'true' : 'false');
    updateTotal();
    toast(state.l2 ? '已选购 L2 增值保险（+¥12，模拟）' : '已取消 L2 增值保险');
  }
  function renderTiers() { updateTotal(); }
  function renderConfirm() {
    var addons = selectedAddonNames();
    $('#sum-base').textContent = TIERS[state.tier].name + ' ' + money(TIERS[state.tier].price);
    $('#sum-addons').textContent = addons.length ? addons.map(function (a) { return a.name + ' ' + money(a.price); }).join('、') : '未选择';
    $('#sum-ins').textContent = state.l2 ? 'L2 增值保险 +¥12' : '未选购';
    $('#sum-address').textContent = $('#pickup-address').value;
    $('#sum-time').textContent = state.time;
    $('#sum-car').textContent = state.car || '未填写';
    updateTotal();
  }
  function validatePickup(showErrors) {
    var addr = $('#pickup-address').value.trim();
    var phone = $('#pickup-phone').value.trim();
    var ok = true;
    function setErr(id, msg) { $(id).textContent = showErrors ? msg : ''; }
    if (addr.length < 5) { setErr('#address-error', '请填写完整的取车地址（至少 5 个字）'); ok = false; } else setErr('#address-error', '');
    if (!/^1\d{10}$/.test(phone)) { setErr('#phone-error', '请输入 11 位中国大陆手机号'); ok = false; } else setErr('#phone-error', '');
    if (!state.time) { setErr('#time-error', '请选择一个取车时段'); ok = false; } else setErr('#time-error', '');
    if (!state.car) { setErr('#car-error', '请从联想列表中选择车型'); ok = false; } else setErr('#car-error', '');
    $('#pickup-next').disabled = !ok;
    return ok;
  }
  function doPay() {
    var btn = $('#pay-btn');
    if (btn.classList.contains('is-loading')) return; // 防重复提交
    btn.classList.add('is-loading');
    btn.setAttribute('aria-busy', 'true');
    window.setTimeout(function () {
      btn.classList.remove('is-loading');
      btn.removeAttribute('aria-busy');
      $('#paid-amount').textContent = $('#sum-total').textContent;
      navigate('user-paysuccess');
      toast('模拟支付成功 · 测试数据，不产生真实资金收付', 'success');
    }, 1200);
  }
  function renderStepper(index) {
    $$('#user-stepper .step').forEach(function (el, i) {
      el.classList.toggle('is-done', i < index);
      el.classList.toggle('is-current', i === index);
      var dot = el.querySelector('.step-dot');
      if (i < index) dot.textContent = '✓';
      else dot.textContent = String(i + 1);
    });
    $('#progress-status').textContent = state.aftersale ? '售后中' : STATUS[index];
    $('#progress-status-text').textContent = state.aftersale ? '售后处理中，平台介入' : (index === 7 ? '订单已完成' : '待你确认验收');
    var cta = $('#progress-cta');
    if (state.aftersale) {
      cta.textContent = '售后处理中';
      cta.setAttribute('data-action', 'aftersale');
      cta.disabled = true;
    } else if (index >= 7) {
      cta.textContent = '去评价';
      cta.setAttribute('data-action', 'go-review');
      cta.setAttribute('aria-label', '去评价');
      cta.disabled = false;
    } else {
      cta.textContent = '确认验收';
      cta.setAttribute('data-action', 'confirm-accept');
      cta.setAttribute('aria-label', '确认验收');
      cta.disabled = false;
    }
  }
  function confirmAccept() {
    state.orderStatus = 7;
    var tag = $('#progress-status');
    tag.className = 'tag tag-success';
    var current = $('#user-stepper .step.is-current');
    if (current) { current.classList.remove('step-warn'); current.classList.add('step-success'); }
    renderStepper(7);
    toast('验收完成，订单已结束（模拟）', 'success');
  }
  function requestAftersale() {
    modal({
      title: '申请售后',
      body: '确认对订单 WC20260616-0083 申请售后？平台客服将介入处理（模拟）。',
      okText: '确认申请',
      danger: true,
      onOk: function () {
        state.aftersale = true;
        var tag = $('#progress-status');
        tag.className = 'tag tag-danger';
        var current = $('#user-stepper .step.is-current');
        if (current) { current.classList.remove('step-warn', 'step-success'); current.classList.add('step-danger'); }
        $('#progress-hint').textContent = '售后中（危险色）';
        renderStepper(state.orderStatus);
        toast('售后申请已提交（模拟）', 'warn');
      }
    });
  }

  /* ---------------- 洗车工端逻辑 ---------------- */
  function applyFilter(filter) {
    var hall = $('[data-page="worker-hall"]');
    var cards = $$('.order-card', hall);
    cards.sort(function (a, b) {
      if (filter === 'near') return Number(a.dataset.distance) - Number(b.dataset.distance);
      if (filter === 'amount') return Number(b.dataset.amount) - Number(a.dataset.amount);
      return Number(a.getAttribute('data-bay-order') || 0) - Number(b.getAttribute('data-bay-order') || 0);
    });
    if (!cards.length) return;
    var wrap = cards[0].parentNode;
    cards.forEach(function (card) {
      var show = filter !== 'door' || card.getAttribute('data-type') === 'door';
      card.style.display = show ? '' : 'none';
      wrap.appendChild(card);
    });
    $$('#filter-bar .chip').forEach(function (chip) {
      chip.classList.toggle('is-on', chip.getAttribute('data-filter') === filter);
    });
    $('#order-count').textContent = '共 ' + cards.filter(function (c) { return c.style.display !== 'none'; }).length + ' 单';
  }
  function renderGrabPage() {
    var num = $('#grab-num');
    num.textContent = state.grabLeft;
    $('#grab-countdown').classList.remove('is-timeout');
    $('#grab-accept').disabled = false;
    $('#grab-accept').textContent = '立即接单';
  }
  function startGrabTimer(orderNo) {
    stopGrabTimer();
    state.grabOrder = orderNo;
    state.grabLeft = 90;
    var card = $$('[data-action="grab"]').filter(function (el) { return el.getAttribute('data-order') === orderNo; })[0];
    if (card) {
      var orderCard = card.closest('.order-card');
      $('#grab-title').textContent = orderCard.querySelector('b').textContent;
      $('#grab-amount').textContent = orderCard.querySelector('.order-amount').textContent;
      $('#grab-order').textContent = orderNo;
    }
    navigate('worker-grab');
    var num = $('#grab-num');
    num.textContent = '90';
    state.grabTimer = window.setInterval(function () {
      state.grabLeft -= 1;
      num.textContent = String(Math.max(0, state.grabLeft));
      if (state.grabLeft <= 0) {
        stopGrabTimer();
        $('#grab-countdown').classList.add('is-timeout');
        var btn = $('#grab-accept');
        btn.disabled = true;
        btn.textContent = '已超时';
        toast('抢单超时，订单已自动指派', 'warn');
      }
    }, 1000);
  }
  function acceptGrab() {
    if ($('#grab-accept').disabled) return;
    stopGrabTimer();
    navigate('worker-task');
    toast('抢单成功，任务已进入执行流程', 'success');
  }
  function applyEStepClasses() {
    $$('#worker-estepper .e-step').forEach(function (el, i) {
      var dot = el.querySelector('.e-step-dot');
      el.classList.toggle('is-done', i < state.taskStep);
      el.classList.toggle('is-current', i === state.taskStep);
      el.classList.toggle('is-locked', i > state.taskStep);
      if (i < state.taskStep) dot.textContent = '✓';
      else if (i === state.taskStep) dot.textContent = 'E' + (i + 1);
      else dot.textContent = '🔒';
    });
  }
  function renderTask() {
    applyEStepClasses();
    var step = TASK_STEPS[state.taskStep];
    $('#task-step-title').textContent = step.title;
    $('#task-step-desc').textContent = step.desc;
    $('#task-step-tag').textContent = state.taskStep === 3 ? '待确认' : '进行中';
    $('#task-next').textContent = state.taskStep === 3 ? '完成任务 ›' : '完成本步骤 ›';
  }
  function completeStep() {
    if (state.taskStep < 3) {
      state.taskStep += 1;
      renderTask();
      toast('E' + state.taskStep + ' 完成，下一步已解锁', 'success');
    } else {
      toast('任务全部完成，等待用户验收（模拟）', 'success');
    }
  }
  function renderEvidence() {
    $$('#evidence-checklist .check-item').forEach(function (el, i) {
      var done = i < state.evidenceDone;
      el.classList.toggle('is-checked', done);
      el.querySelector('.mini').textContent = done ? '已拍 ✓' : '待拍';
    });
    var complete = state.evidenceDone >= 4;
    $('#evidence-submit').disabled = !complete;
  }
  function shutter() {
    if (state.evidenceDone >= 4) { toast('必拍项已全部完成', 'warn'); return; }
    var item = $$('#evidence-checklist .check-item')[state.evidenceDone];
    item.classList.add('is-checked');
    item.querySelector('.mini').textContent = '已拍 ✓';
    var code = item.getAttribute('data-check');
    state.evidenceDone += 1;
    var cam = $('#camera-view');
    var flash = document.createElement('div');
    flash.className = 'capture-flash';
    cam.appendChild(flash);
    window.setTimeout(function () { flash.remove(); }, 300);
    toast(code + ' 现场证据拍摄完成（带时间/GPS/订单号水印）', 'success');
    renderEvidence();
  }
  function submitEvidence() {
    if (state.evidenceDone < 4) { toast('请先完成全部 4 项必拍', 'warn'); return; }
    modal({
      title: '确认上传证据',
      body: '将上传 E1-E4 共 4 张现场水印证据，上传后不可修改。',
      okText: '确认上传',
      onOk: function () {
        toast('证据已上传（模拟），对应步骤自动完成', 'success');
        if (state.taskStep < 3) state.taskStep += 1;
        navigate('worker-task');
      }
    });
  }

  /* ---------------- 考试逻辑 ---------------- */
  function fmtTime(seconds) {
    var m = Math.floor(seconds / 60), s = seconds % 60;
    return (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;
  }
  function renderExamQuestion() {
    var idx = state.examIndex;
    var item = QUESTIONS[idx];
    $('#exam-index').textContent = '第 ' + (idx + 1) + ' / 20 题';
    $('#exam-text').textContent = item.q + '（演示题：正确答案均为 A）';
    var box = $('#exam-options');
    box.innerHTML = '';
    item.o.forEach(function (opt, i) {
      var btn = document.createElement('button');
      btn.className = 'opt' + (state.examAnswers[idx] === i ? ' is-selected' : '');
      btn.setAttribute('data-action', 'exam-answer');
      btn.setAttribute('data-opt', String(i));
      btn.setAttribute('aria-label', '选项 ' + 'ABCD'[i] + '：' + opt);
      btn.innerHTML = '<span class="opt-key">' + 'ABCD'[i] + '</span><span>' + opt + '</span>';
      box.appendChild(btn);
    });
    var sheet = $('#answer-sheet');
    if (!sheet.children.length) {
      for (var i = 0; i < 20; i++) {
        var b = document.createElement('button');
        b.className = 'sheet-num';
        b.setAttribute('data-action', 'exam-jump');
        b.setAttribute('data-q', String(i));
        b.setAttribute('aria-label', '跳转到第 ' + (i + 1) + ' 题');
        b.textContent = String(i + 1);
        sheet.appendChild(b);
      }
    }
    $$('.sheet-num', sheet).forEach(function (b, i) {
      b.classList.toggle('is-answered', state.examAnswers[i] !== null);
      b.classList.toggle('is-current', i === idx);
    });
    var answered = state.examAnswers.filter(function (a) { return a !== null; }).length;
    $('#exam-answered').textContent = '已答 ' + answered + ' / 20';
    $('#exam-prev').disabled = idx === 0;
    $('#exam-next').disabled = idx === 19;
    $('#exam-next').textContent = idx === 19 ? '已是最后一题' : '下一题 ›';
    $('#exam-timer').textContent = fmtTime(state.examLeft);
  }
  function startExamTimer() {
    stopExamTimer();
    state.examLeft = 600;
    state.examRunning = true;
    $('#exam-timer').textContent = '10:00';
    state.examTimer = window.setInterval(function () {
      state.examLeft -= 1;
      $('#exam-timer').textContent = fmtTime(Math.max(0, state.examLeft));
      if (state.examLeft <= 0) {
        stopExamTimer();
        toast('考试时间到，已自动交卷', 'warn');
        finishExam();
      }
    }, 1000);
  }
  function examAnswer(opt) { state.examAnswers[state.examIndex] = opt; renderExamQuestion(); }
  function examJump(i) { state.examIndex = Math.max(0, Math.min(19, i)); renderExamQuestion(); }
  function examStep(delta) { examJump(state.examIndex + delta); }
  function examSubmit() {
    var answered = state.examAnswers.filter(function (a) { return a !== null; }).length;
    modal({
      title: '交卷确认',
      body: '已答 ' + answered + '/20 题，未答题计 0 分。交卷后不可修改，确认提交？',
      okText: '确认交卷',
      onOk: finishExam
    });
  }
  function finishExam() {
    stopExamTimer();
    var score = 0;
    QUESTIONS.forEach(function (item, i) { if (state.examAnswers[i] === item.a) score += 5; });
    var pass = score >= 80;
    $('#exam-score').textContent = String(score);
    var badge = $('#exam-pass');
    badge.className = 'exam-pass ' + (pass ? 'ok' : 'no');
    badge.textContent = pass ? '通过' : '未通过';
    $('#exam-result-desc').textContent = pass
      ? '考试通过，C3 精细认证已解锁，可承接高阶订单。'
      : '未达 80 分及格线，请重新学习后再次考试。';
    navigate('worker-examresult');
    toast(pass ? '考试通过（模拟）' : '考试未通过（模拟）', pass ? 'success' : 'warn');
  }
  function examRetry() {
    state.examAnswers = new Array(20).fill(null);
    state.examIndex = 0;
    navigate('worker-exam');
    startExamTimer();
    renderExamQuestion();
  }

  /* ---------------- 收入 / 钱包 ---------------- */
  function setWalletState(key) {
    var labels = {
      normal: '● 正常 · 可提现',
      frozen: '◐ 冻结中 · 暂不可提现',
      settled: '✓ 已结算 · 本周期完成'
    };
    $('#wallet-state').textContent = labels[key] || labels.normal;
    $$('#wallet-states .ws').forEach(function (el) {
      el.classList.toggle('is-active', el.getAttribute('data-state') === key);
    });
  }
  function buyCard() {
    modal({
      title: '购买免佣卡（7 日卡）',
      body: '确认使用 ¥19.9 模拟购买 7 日免佣卡？本操作不产生真实资金收付。',
      okText: '模拟支付',
      onOk: function () { toast('模拟支付成功 · 测试数据，不产生真实资金收付', 'success'); }
    });
  }

  /* ---------------- 门店端逻辑 ---------------- */
  var BAY_LABEL = { free: '有空位', full: '已满', paused: '暂停' };
  var BAY_CLASS = { free: 'bay-free', full: 'bay-full', paused: 'bay-paused' };
  function renderBays() {
    $$('#bay-grid .bay').forEach(function (el, i) {
      var key = state.bays[i];
      el.className = 'bay ' + BAY_CLASS[key];
      el.querySelector('.bay-status-text').textContent = BAY_LABEL[key];
      el.setAttribute('aria-label', 'Bay-0' + (i + 1) + '，当前' + BAY_LABEL[key] + '，点击切换状态');
    });
  }
  function toggleBay(i) {
    var order = ['free', 'full', 'paused'];
    var next = (order.indexOf(state.bays[i]) + 1) % 3;
    state.bays[i] = order[next];
    renderBays();
    toast('Bay-0' + (i + 1) + ' 已切换为「' + BAY_LABEL[state.bays[i]] + '」');
  }
  function storeAssign(orderNo) {
    var freeIndex = state.bays.indexOf('free');
    if (freeIndex === -1) {
      $('#no-bay-note').hidden = false;
      modal({
        title: '接单已被拦截',
        body: '当前无空位（有空位工位为 0）。请返回工作台切换工位状态后重试。',
        okText: '我知道了',
        cancelText: '取消',
        danger: true,
        onOk: function () { toast('接单失败：当前无空位', 'warn'); }
      });
      return;
    }
    modal({
      title: '确认接单',
      body: '订单 ' + orderNo + ' 将占用 Bay-0' + (freeIndex + 1) + '，接单后工位状态变为「已满」。',
      okText: '确认接单',
      onOk: function () {
        $('#no-bay-note').hidden = true;
        state.bays[freeIndex] = 'full';
        renderBays();
        toast('接单成功（模拟），已占用 1 个空位', 'success');
      }
    });
  }
  function reconcile() { toast('T+1 对账任务已创建（模拟）', 'success'); }

  /* ---------------- 五维评价 ---------------- */
  function renderStars() {
    $$('#review-list .review-dim').forEach(function (dim) {
      var key = dim.getAttribute('data-dim');
      var value = state.review[key] || 0;
      var box = $('.stars', dim);
      if (!box.children.length) {
        for (var i = 1; i <= 5; i++) {
          var b = document.createElement('button');
          b.className = 'star';
          b.textContent = '★';
          b.setAttribute('data-action', 'rate');
          b.setAttribute('data-star', String(i));
          b.setAttribute('aria-label', '评分 ' + i + ' 星');
          box.appendChild(b);
        }
      }
      $$('.star', box).forEach(function (b, i) { b.classList.toggle('is-on', i < value); });
      $('.dim-score', dim).textContent = value ? value + ' 星' : '未评分';
      $('.dim-score', dim).className = 'dim-score' + (value ? '' : ' muted');
    });
  }
  function rateStar(el) {
    var dim = el.closest('.review-dim');
    var key = dim.getAttribute('data-dim');
    var value = Number(el.getAttribute('data-star'));
    state.review[key] = value;
    var reason = $('.reason-box', dim);
    var textarea = $('textarea', reason);
    if (value <= 2) {
      reason.classList.add('is-show');
      textarea.setAttribute('aria-required', 'true');
    } else {
      reason.classList.remove('is-show');
      textarea.classList.remove('is-error');
      textarea.removeAttribute('aria-required');
    }
    renderStars();
  }
  function submitReview() {
    var dims = $$('#review-list .review-dim');
    var missing = false;
    dims.forEach(function (dim) {
      var key = dim.getAttribute('data-dim');
      var value = state.review[key] || 0;
      if (!value) missing = true;
      if (value > 0 && value <= 2) {
        var ta = $('textarea', dim);
        if (!ta.value.trim()) { ta.classList.add('is-error'); missing = true; }
        else ta.classList.remove('is-error');
      }
    });
    if (missing) { toast('请完成全部维度评分，低分需填写原因', 'warn'); return; }
    modal({
      title: '提交五维评价',
      body: '确认提交本次作业的工具归位、场地收拾、设备复位、工位占用时长、配合规范评分？',
      okText: '确认提交',
      onOk: function () { toast('五维评价已提交（模拟）', 'success'); }
    });
  }

  /* ---------------- 后台端逻辑 ---------------- */
  function renderGeneric(title) {
    state.genericTitle = title;
    $('#generic-title').textContent = title;
    var descriptions = {
      '治理': '投诉与纠纷治理 · 状态标签 + 行内操作',
      '费率': '抽成规则：用户侧服务费 / 洗车工 40% / 平台 35% / 门店工位费 M 25%',
      '保单': 'L1 平台强制保障（不可取消）与 L2 增值保险配置',
      '对账': 'T+1 对账任务 · 金额均为测试数据，不产生真实资金收付'
    };
    $('#generic-sub').textContent = (descriptions[title] || '测试环境模拟数据') + ' · 行内操作仅演示';
    var rows = {
      '治理': [
        ['G-2201', '订单 WC…0091', '服务投诉', '—', 'pending', '待处理', '06-16 14:20'],
        ['G-2202', '洗车工 陈师傅', '认证投诉', '—', 'pass', '已处理', '06-16 11:05'],
        ['G-2203', '门店 张江店', '工位纠纷', '—', 'reject', '已驳回', '06-15 18:40'],
        ['G-2204', '用户 138****8000', '验收争议', '—', 'pending', '待处理', '06-15 16:12']
      ],
      '费率': [
        ['F-01', '标准洗 ¥133', '用户服务费', '¥20.00', 'normal', '生效中', '06-01'],
        ['F-02', '洗车工分成', '40%', '40%', 'pass', '生效中', '06-01'],
        ['F-03', '平台服务费', '35%', '35%', 'pass', '生效中', '06-01'],
        ['F-04', '门店工位费 M', '25%', '25%', 'pass', '生效中', '06-01']
      ],
      '保单': [
        ['P-L1', 'L1 基础保险', '强制保障', '¥0', 'pass', '生效中', '长期'],
        ['P-L2', 'L2 增值保险', '可选投保', '¥12', 'pass', '生效中', '长期'],
        ['P-EXP', '旧版保障计划', '已过期', '—', 'reject', '已停用', '2025-12-31']
      ],
      '对账': [
        ['R-0616', '用户支付汇总', 'T+1', '¥12,486.00', 'pending', '待对账', '06-17 09:00'],
        ['R-0615', '洗车工分成', 'T+1', '¥4,994.40', 'pass', '已对账', '06-16 09:00'],
        ['R-0615', '门店工位费 M', 'T+1', '¥3,121.50', 'pass', '已对账', '06-16 09:00'],
        ['R-0614', '争议冻结', '异常', '¥0.00', 'normal', '无争议', '06-15 09:00']
      ]
    };
    var data = rows[title] || rows['治理'];
    var body = $('#generic-body');
    body.innerHTML = '';
    data.forEach(function (r) {
      var tr = document.createElement('tr');
      tr.innerHTML = '<td class="num">' + r[0] + '</td><td>' + r[1] + '</td><td>' + r[2] +
        '</td><td class="num">' + r[3] + '</td><td><span class="pill-status ' + r[4] + '">' + r[5] +
        '</span></td><td>' + r[6] + '</td><td><button class="op" data-action="generic-op" data-op="查看">查看</button>' +
        '<button class="op danger" data-action="generic-op" data-op="标记">标记</button></td>';
      body.appendChild(tr);
    });
  }
  function auditAction(row, type) {
    var tr = $('[data-audit-row="' + row + '"]');
    if (!tr) return;
    if (type === '查看') { toast('查看申请 ' + row + ' 的审核详情（模拟）'); return; }
    modal({
      title: type === '通过' ? '通过审核' : '驳回申请',
      body: '确认对申请 ' + row + ' 执行「' + type + '」？操作将写入审计日志（模拟）。',
      okText: '确认' + type,
      danger: type === '驳回',
      onOk: function () {
        var pill = $('.pill-status', tr);
        pill.className = 'pill-status ' + (type === '通过' ? 'pass' : 'reject');
        pill.textContent = type === '通过' ? '已通过' : '已驳回';
        toast('申请 ' + row + ' 已' + type + '（模拟）', 'success');
      }
    });
  }
  function rbacAction(role, op) {
    if (op === '编辑') { toast('编辑角色「' + role + '」权限（模拟）'); return; }
    modal({
      title: '禁用角色',
      body: '确认禁用角色「' + role + '」？该角色的所有成员将立即失去对应权限，变更记入权限审计。',
      okText: '确认禁用',
      danger: true,
      onOk: function () {
        var tr = $$('#rbac-body tr').filter(function (r) { return r.getAttribute('data-role') === role; })[0];
        if (tr) { var pill = $('.pill-status', tr); pill.className = 'pill-status reject'; pill.textContent = '已停用'; }
        toast('角色「' + role + '」已禁用，权限审计已记录（模拟）', 'warn');
      }
    });
  }

  /* ---------------- 全局事件委托 ---------------- */
  document.addEventListener('click', function (e) {
    var t = e.target;

    // 四端切换
    var sw = t.closest('[data-switch-app]');
    if (sw) {
      var app = sw.getAttribute('data-switch-app');
      var defaultPage = { user: 'user-home', worker: 'worker-hall', store: 'store-workbench', admin: 'admin-overview' }[app];
      navigate(defaultPage);
      return;
    }

    // 后台侧栏导航
    var adminNav = t.closest('[data-admin-page]');
    if (adminNav) {
      $$('#admin-side-nav .side-item').forEach(function (el) { el.classList.toggle('is-active', el === adminNav); });
      var title = adminNav.getAttribute('data-title');
      if (adminNav.getAttribute('data-admin-page') === 'admin-generic') renderGeneric(title);
      navigate(adminNav.getAttribute('data-admin-page'), { crumb: title });
      return;
    }

    // 通用动作
    var el = t.closest('[data-action]');
    if (el) {
      var action = el.getAttribute('data-action');
      switch (action) {
        case 'select-tier':
          selectTier(el.getAttribute('data-tier'));
          if (el.classList.contains('quick-item')) navigate('user-tiers');
          break;
        case 'toggle-addon': toggleAddon(el); break;
        case 'toggle-insurance': toggleInsurance(); break;
        case 'toggle-expand': {
          var body = $('#expand-body');
          var open = body.classList.toggle('is-open');
          el.setAttribute('aria-expanded', open ? 'true' : 'false');
          break;
        }
        case 'reorder': selectTier('premium'); navigate('user-tiers'); toast('已带入上次服务档位：精细洗'); break;
        case 'pay': doPay(); break;
        case 'aftersale': requestAftersale(); break;
        case 'confirm-accept': confirmAccept(); break;
        case 'go-review': switchApp('store'); navigate('store-review'); toast('演示：已进入门店五维评价页'); break;
        case 'filter': applyFilter(el.getAttribute('data-filter')); break;
        case 'filter-toggle': $('#filter-bar').scrollLeft += 80; break;
        case 'grab': startGrabTimer(el.getAttribute('data-order')); break;
        case 'accept-grab': acceptGrab(); break;
        case 'complete-step': completeStep(); break;
        case 'shutter': shutter(); break;
        case 'album-disabled': toast('仅支持现场拍摄，相册入口不可用', 'warn'); break;
        case 'evidence-submit': submitEvidence(); break;
        case 'exam-answer': examAnswer(Number(el.getAttribute('data-opt'))); break;
        case 'exam-jump': examJump(Number(el.getAttribute('data-q'))); break;
        case 'exam-prev': examStep(-1); break;
        case 'exam-next': examStep(1); break;
        case 'exam-submit': examSubmit(); break;
        case 'exam-retry': examRetry(); break;
        case 'wallet-state': setWalletState(el.getAttribute('data-state')); break;
        case 'buy-card': buyCard(); break;
        case 'store-assign': storeAssign(el.getAttribute('data-order')); break;
        case 'reconcile': reconcile(); break;
        case 'rate': rateStar(el); break;
        case 'submit-review': submitReview(); break;
        case 'audit-pass': auditAction(el.getAttribute('data-row'), '通过'); break;
        case 'audit-reject': auditAction(el.getAttribute('data-row'), '驳回'); break;
        case 'rbac-op': rbacAction(el.getAttribute('data-role'), el.getAttribute('data-op')); break;
        case 'generic-op': toast(el.getAttribute('data-op') + '操作已执行（模拟）· 已记入审计日志'); break;
        case 'admin-row-op': toast(el.getAttribute('data-op') + '操作已执行（模拟）· 已记入审计日志'); break;
        default: break;
      }
      return;
    }

    // 时段选择
    var slot = t.closest('[data-time]');
    if (slot) {
      state.time = slot.getAttribute('data-time');
      $$('#time-slots .time-slot').forEach(function (s) { s.classList.toggle('is-on', s === slot); });
      validatePickup(false);
      return;
    }

    // 车型联想选择
    var car = t.closest('[data-car]');
    if (car) {
      state.car = car.getAttribute('data-car');
      $('#car-input').value = state.car;
      $('#car-list').hidden = true;
      validatePickup(false);
      return;
    }

    // 工位切换
    var bay = t.closest('[data-bay]');
    if (bay) { toggleBay(Number(bay.getAttribute('data-bay'))); return; }

    // 页面跳转（放在最后，避免与动作冲突）
    var go = t.closest('[data-go]');
    if (go) {
      var pageId = go.getAttribute('data-go');
      if (go.id === 'pickup-next' && !validatePickup(true)) {
        toast('请先完善取车信息', 'warn');
        return;
      }
      navigate(pageId);
      return;
    }
  });

  /* ---------------- 输入事件（防抖联想 + 实时校验） ---------------- */
  var suggest = debounce(function () {
    var value = $('#car-input').value.trim();
    var list = $('#car-list');
    list.hidden = value.length === 0;
    if (!value) return;
    $$('.car-suggestion', list).forEach(function (item) {
      var name = item.getAttribute('data-car');
      item.style.display = name.indexOf(value) >= 0 || value.length <= 1 ? '' : 'none';
    });
  }, 180);

  document.addEventListener('input', function (e) {
    var id = e.target.id;
    if (id === 'pickup-address' || id === 'pickup-phone') validatePickup(false);
    if (id === 'car-input') {
      state.car = '';
      validatePickup(false);
      suggest();
    }
  });

  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
      var mask = $('.modal-mask');
      if (mask && mask.parentNode) mask.parentNode.removeChild(mask);
    }
  });

  // 节流示例：滚动时更新水印时间定位（原型中仅刷新 GPS 水印时间戳）
  var refreshWatermark = throttle(function () {
    var wm = $('#wm-time');
    if (wm) {
      var d = new Date();
      wm.textContent = ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2) + ':' + ('0' + d.getSeconds()).slice(-2);
    }
  }, 1000);
  document.addEventListener('scroll', refreshWatermark, true);

  /* ---------------- 初始化 ---------------- */
  function init() {
    // 档位与金额
    $$('#addon-grid .addon-item').forEach(function (el) { state.addonPrice[el.getAttribute('data-addon')] = Number(el.getAttribute('data-price')); });
    selectTier('standard');
    // 取车信息校验
    validatePickup(false);
    renderConfirm();
    // 订单进度
    renderStepper(state.orderStatus);
    // 抢单大厅默认排序
    applyFilter('all');
    // 任务步骤
    renderTask();
    renderEvidence();
    // 考试题目（不启动计时，进入考试页时启动）
    renderExamQuestion();
    // 钱包三态
    setWalletState('normal');
    // 工位
    renderBays();
    // 五维评价星标
    renderStars();
    // 后台通用页
    renderGeneric('治理');
  }
  init();
})();
