<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>订单管理 · 后台管理</title><style>
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
<aside class="side"><div class="logo">线上洗车平台<small>后台管理端 · v6.0</small></div><a href="/admin/dashboard" class="">总览看板</a><a href="/admin/audit" class="">审核中心</a><a href="/admin/orders" class="on">订单管理</a><a href="/admin/settlement" class="">结算对账</a></aside>
<div class="main">
<div class="top"><span class="env">测试环境 · 模拟支付 · 不产生真实资金收付</span><span class="user">管理员 admin ▾</span></div>
<div class="wrap">
<h1>订单管理</h1>
<div class="panel"><h2>订单状态机：待支付→待接单→已接单→取车中→洗车中→配送中→待验收→完成</h2>
<table><thead><tr><th>订单号</th><th>车主</th><th>服务档</th><th>形态</th><th>门店</th><th>洗车工</th><th>金额</th><th>状态</th></tr></thead>
<tbody>
<tr><td>XC202611160001</td><td>车主甲</td><td>标准清洁</td><td>到店</td><td>浦东店</td><td>王师傅</td><td>¥133.00</td><td><span class="tag ok">已接单</span></td></tr>
<tr><td>XC202611160002</td><td>车主乙</td><td>外观清洁</td><td>上门</td><td>浦东店</td><td>—</td><td>¥95.00</td><td><span class="tag info">待接单</span></td></tr>
<tr><td>XC202611160003</td><td>车主甲</td><td>精细清洁</td><td>到店</td><td>浦东店</td><td>—</td><td>¥243.00</td><td><span class="tag warn">待支付</span></td></tr>
</tbody></table>
<div class="note">订单属 C 级禁止缓存数据；取还车四组证据（E1取车/E2到店/E3离店/E4还车）缺一不可推进下一段。</div>
</div>
</div></div></div></body></html>
