<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>线上洗车平台 · TEST-ENV-V1</title>
    <style>
        *{box-sizing:border-box;margin:0;padding:0}
        body{font-family:-apple-system,"PingFang SC","Microsoft YaHei",sans-serif;background:#F7F9FC;color:#1F2738;display:flex;align-items:center;justify-content:center;min-height:100vh}
        .box{background:#fff;border:1px solid #E4E9F2;border-radius:14px;padding:40px;max-width:620px;width:92%;box-shadow:0 12px 32px rgba(18,24,56,.10)}
        .env{background:repeating-linear-gradient(45deg,#FFF4E0 0 8px,#FDE9C4 8px 16px);color:#8A4B08;padding:10px 16px;border-radius:8px;font-size:13px;font-weight:600;margin-bottom:24px}
        h1{font-size:24px;color:#1668E3;margin-bottom:6px}
        .sub{color:#6B778C;font-size:14px;margin-bottom:24px}
        a.item{display:block;padding:16px 18px;border:1px solid #E4E9F2;border-radius:10px;margin-bottom:12px;text-decoration:none;color:#1F2738;transition:.2s}
        a.item:hover{border-color:#1668E3;box-shadow:0 4px 12px rgba(22,104,227,.12)}
        a.item b{color:#1668E3;font-size:15px}
        a.item span{display:block;color:#6B778C;font-size:12px;margin-top:6px}
        .foot{color:#97A3B6;font-size:12px;margin-top:20px;line-height:1.7}
    </style>
</head>
<body>
<div class="box">
    <div class="env">测试环境 · 模拟支付 · 不产生真实资金收付 · 禁止对外经营</div>
    <h1>线上洗车平台 v6.0</h1>
    <div class="sub">SSM 后端服务已启动 · 请选择入口（V1 内部测试）</div>

    <a class="item" href="admin/dashboard"><b>后台管理端 →</b><span>总览看板 / 审核 / 订单 / 结算对账（桌面 Web）</span></a>
    <a class="item" href="api/user/home"><b>车主端 API →</b><span>/api/user/home 返回三档价格与服务目录（JSON）</span></a>
    <a class="item" href="api/worker/certs"><b>洗车工端 API →</b><span>/api/worker/certs 返回认证资质（JSON）</span></a>
    <a class="item" href="api/store/bays"><b>门店端 API →</b><span>/api/store/bays 返回工位三态看板（JSON）</span></a>
    <a class="item" href="api/admin/dashboard"><b>后台数据 API →</b><span>/api/admin/dashboard 返回缓存/权限/认证看板指标（JSON）</span></a>

    <div class="foot">
        三端客户端源码见工程 clients/ 目录（微信小程序 / uni-app App / H5）。<br>
        金额字段对外一律「分」；支付为模拟，成功文案橙色「模拟支付成功」。<br>
        生产上架仍需：持牌分账、持牌保险、ICP 备案、小程序认证与全站 HTTPS。
    </div>
</div>
</body>
</html>
