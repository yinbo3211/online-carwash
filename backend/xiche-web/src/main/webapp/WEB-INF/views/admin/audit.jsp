<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>审核中心 · 后台管理</title><style>
:root{--nav:#1F2738;--pri:#1668E3;--accent:#0E9F9F;--ok:#18A058;--warn:#F0A020;--danger:#E5484D;--bg:#F7F9FC;--line:#E4E9F2;--mut:#6B778C}
*{box-sizing:border-box;margin:0;padding:0}
body{font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;background:var(--bg);color:#1F2738;font-size:14px}
.layout{display:flex;min-height:100vh}
.side{width:220px;background:var(--nav);color:#fff;flex-shrink:0}
.side .logo{padding:22px 20px;font-size:17px;font-weight:700;border-bottom:1px solid rgba(255,255,255,.08)}
.side .logo small{display:block;font-size:11px;color:#8b98ad;font-weight:400;margin-top:4px}
.side a{display:block;padding:13px 22px;color:#b9c2d0;text-decoration:none;font-size:14px}
.side a:hover,.side a.on{background:rgba(22,104,227,.18);color:#fff;border-left:3px solid var(--pri)}
.main{flex:1;min-width:0}
.top{height:60px;background:#fff;border-bottom:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;padding:0 26px}
.top .env{background:repeating-linear-gradient(45deg,#FFF4E0 0 8px,#FDE9C4 8px 16px);color:#8A4B08;padding:6px 14px;border-radius:6px;font-size:12px;font-weight:600}
.top .user{color:var(--mut)}
.wrap{padding:24px 26px}
h1{font-size:20px;margin-bottom:18px}
.cards{display:grid;grid-template-columns:repeat(5,1fr);gap:14px;margin-bottom:22px}
.card{background:#fff;border:1px solid var(--line);border-radius:10px;padding:18px;box-shadow:0 1px 3px rgba(18,24,56,.05)}
.card .k{color:var(--mut);font-size:12px;margin-bottom:8px}
.card .v{font-size:26px;font-weight:700;color:var(--pri)}
.panel{background:#fff;border:1px solid var(--line);border-radius:10px;padding:18px;margin-bottom:18px}
.panel h2{font-size:15px;margin-bottom:14px}
table{width:100%;border-collapse:collapse;font-size:13px}
th,td{text-align:left;padding:11px 12px;border-bottom:1px solid var(--line)}
th{background:#F1F4F9;color:var(--mut);font-weight:600}
.tag{display:inline-block;padding:3px 10px;border-radius:99px;font-size:12px}
.tag.ok{background:#E8F7EF;color:var(--ok)} .tag.warn{background:#FFF5E5;color:#B97613} .tag.danger{background:#FDECEC;color:var(--danger)} .tag.info{background:#EAF2FE;color:var(--pri)}
.btn{border:0;border-radius:6px;padding:7px 14px;font-size:13px;cursor:pointer;color:#fff;background:var(--pri)}
.btn.ghost{background:#fff;color:var(--pri);border:1px solid var(--pri)}
.btn.danger{background:var(--danger)}
.bar{display:flex;gap:6px;align-items:center}
.chart{display:flex;align-items:flex-end;gap:10px;height:150px;padding-top:10px}
.chart .b{flex:1;background:linear-gradient(180deg,var(--pri),#7FA9FA);border-radius:4px 4px 0 0;position:relative}
.chart .b span{position:absolute;bottom:-22px;width:100%;text-align:center;font-size:11px;color:var(--mut)}
.note{background:#F1F4F9;color:var(--mut);font-size:12px;padding:10px 14px;border-radius:6px;margin-top:12px}
</style></head>
<body><div class="layout">
<aside class="side"><div class="logo">线上洗车平台<small>后台管理端 · v6.0</small></div><a href="/admin/dashboard" class="">总览看板</a><a href="/admin/audit" class="on">审核中心</a><a href="/admin/orders" class="">订单管理</a><a href="/admin/settlement" class="">结算对账</a></aside>
<div class="main">
<div class="top"><span class="env">测试环境 · 模拟支付 · 不产生真实资金收付</span><span class="user">管理员 admin ▾</span></div>
<div class="wrap">
<h1>审核中心</h1>
<div class="panel"><h2>洗车工实名 / 驾驶证审核（审核角色不含财务与封号权限 · 职责分离）</h2>
<table><thead><tr><th>姓名</th><th>手机号</th><th>实名</th><th>驾驶证</th><th>准驾车型</th><th>有效期</th><th>状态</th><th>操作</th></tr></thead>
<tbody>
<tr><td>王师傅</td><td>139****0001</td><td>已认证</td><td>沪A1234567</td><td>C1</td><td>2028-12-31</td><td><span class="tag ok">已通过</span></td><td class="bar"><button class="btn ghost">证件预览</button><button class="btn">通过</button><button class="btn danger">驳回</button></td></tr>
<tr><td>赵师傅</td><td>139****0002</td><td>已认证</td><td>沪A7654321</td><td>C1</td><td>2027-06-30</td><td><span class="tag ok">已通过</span></td><td class="bar"><button class="btn ghost">证件预览</button><button class="btn">通过</button><button class="btn danger">驳回</button></td></tr>
</tbody></table>
<div class="note">审核通过仅准入；C1/C2/C3 认证等级须另行通过对应考试，未通过不可接对应订单。</div>
</div>
</div></div></div></body></html>
