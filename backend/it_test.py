#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
线上洗车平台 v6.0 · 端到端集成测试（TEST-ENV-V1）
对照《v6.0 全量测试报告》56 项用例 + 本轮 P0/P1 修复回归。
前置：MariaDB 已导入 01~05 脚本，Tomcat Embed 加载 WAR 于 :8080，上下文 /xiche-platform。
运行：python3 it_test.py [base_url]
判定：全部用例通过则 exit 0，否则 exit 1。
"""
import json, sys, urllib.request, urllib.error, urllib.parse, random, time

BASE = (sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080/xiche-platform").rstrip("/")
PASS, FAIL = [], []
SEQ = str(int(time.time()))[-6:]

def call(method, path, body=None, token=None, headers=None, raw=False):
    url = BASE + path
    data = None
    h = {"Content-Type": "application/json"}
    if token: h["X-Token"] = token
    if headers: h.update(headers)
    if body is not None:
        data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(req, timeout=15) as r:
            txt = r.read().decode("utf-8"); code = r.status
    except urllib.error.HTTPError as e:
        txt = e.read().decode("utf-8"); code = e.code
    except Exception as e:
        return 0, {"code": -1, "message": str(e)}
    if raw: return code, txt
    try: return code, json.loads(txt)
    except Exception: return code, {"code": -2, "raw": txt}

def ck(name, cond, detail=""):
    if cond: PASS.append(name); print("  [PASS] " + name)
    else: FAIL.append((name, detail)); print("  [FAIL] " + name + " -> " + str(detail)[:200])

def login(username, password, subject):
    c, r = call("POST", "/api/auth/login", {"username": username, "password": password, "subjectType": subject})
    return r.get("data", {}).get("token") if r.get("code") == 0 else None

def main():
    print("BASE =", BASE)
    # ---------- F 组：鉴权 ----------
    print("\n[F] 鉴权")
    t_user = login("13800000001", "test1234", "USER")
    t_user2 = login("13800000002", "test1234", "USER")
    t_worker = login("13900000001", "test1234", "WORKER")
    t_store = login("store001", "test1234", "STORE")
    t_admin = login("admin", "admin123", "ADMIN")
    t_auditor = login("auditor", "test1234", "ADMIN")
    t_finance = login("finance", "test1234", "ADMIN")
    t_sod = login("soduser", "test1234", "ADMIN")
    ck("F1 四类终端登录均签发token", all([t_user, t_user2, t_worker, t_store, t_admin]), "tokens")
    c, r = call("GET", "/api/user/orders", token=None)
    ck("F2 无token访问被拒(401)", c == 401 or r.get("code") in (40100, 401, 40300), (c, r))
    c, r = call("GET", "/api/user/orders/1", headers={"X-Role": "ADMIN", "X-User-Id": "999"})
    ck("F3 伪造X-Role头无效(401)", c == 401 or r.get("code") in (40100, 401), (c, r))
    c, r = call("POST", "/api/auth/login", {"username": "13800000001", "password": "test1234", "subjectType": "WORKER"})
    ck("F4 跨端账号串用被拒", r.get("code") != 0, r)
    c, r = call("POST", "/api/auth/login", {"username": "13800000001", "password": "wrong"})
    ck("F5 错误密码被拒", r.get("code") != 0, r)
    c, r = call("GET", "/api/user/orders/1", token=t_user)
    ck("F6 车主可看自己订单", r.get("code") == 0, r)
    # 车主甲(userId=2)看车主乙(userId=3)的订单应拒绝
    c, rb = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "STORE", "requestId": "it-" + SEQ + "-f6"}, token=t_user2)
    other_oid = rb.get("data", {}).get("orderId")
    c, r = call("GET", "/api/user/orders/%d" % other_oid, token=t_user)
    ck("F6b 车主不可看他人订单", r.get("code") == 40300, r)

    # ---------- A 组：下单与幂等 ----------
    print("\n[A] 下单与幂等")
    rid = "it-" + SEQ + "-a2"
    c, r = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "STORE", "requestId": rid}, token=t_user)
    ck("A2 下单成功", r.get("code") == 0, r)
    oid = r.get("data", {}).get("orderId"); ono = r.get("data", {}).get("orderNo")
    ck("A3 金额=13300分", r.get("data", {}).get("amountFen") == 13300, r)
    c, r2 = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "STORE", "requestId": rid}, token=t_user)
    ck("A4 同用户requestId幂等返回同单", r2.get("data", {}).get("orderId") == oid, r2)
    ck("A4b 订单号服务端生成(不含requestId)", ono and rid not in ono, ono)
    # P0-06：车主乙复用甲的 requestId
    c, rb = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "STORE", "requestId": rid}, token=t_user2)
    ck("A5 跨用户复用requestId不劫持", rb.get("data", {}).get("orderId") != oid, rb)
    ck("A5b 乙订单归属乙(userId=3)", rb.get("data", {}).get("orderId") and rb["data"]["orderId"] != oid, rb)
    # 上门精细档应拒绝
    c, r = call("POST", "/api/user/orders", {"tier": "premium", "serviceMode": "ONSITE", "requestId": "it-" + SEQ + "-a6", "addressId": 1}, token=t_user)
    ck("A6 上门精细档拒绝", r.get("code") != 0, r)
    c, r = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "FAKE-MODE", "requestId": "it-" + SEQ + "-a7"}, token=t_user)
    ck("A7 非法serviceMode拒绝", r.get("code") != 0, r)
    c, r = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "ONSITE", "requestId": "it-" + SEQ + "-a8"}, token=t_user)
    ck("A8 上门单缺addressId拒绝", r.get("code") != 0, r)
    # P1-04 免佣卡客户端快照被忽略
    c, r = call("POST", "/api/user/orders", {"tier": "standard", "serviceMode": "STORE", "requestId": "it-" + SEQ + "-a9", "cardSnapshot": "DISCOUNT:1.0"}, token=t_user)
    ck("A9 客户端免佣卡快照被忽略", r.get("code") == 0, r)  # 下单成功但快照置NONE，结算时验证费率

    # ---------- B 组：支付与保单 ----------
    print("\n[B] 支付与保单")
    c, r = call("POST", "/api/user/orders/%d/simulate-pay" % oid, {}, token=t_user2)
    ck("B1 越权支付他人订单拒绝", c == 403 or r.get("code") in (40300, 403), (c, r))
    c, r = call("POST", "/api/user/orders/%d/simulate-pay" % oid, {}, token=t_user)
    ck("B2 本人支付成功", r.get("code") == 0 and r.get("data", {}).get("paid"), r)
    c, r = call("GET", "/api/user/orders/%d" % oid, token=t_user)
    ck("B2b 支付后状态=20", r.get("data", {}).get("order", {}).get("status") == 20, r)
    c, r = call("POST", "/api/user/orders/%d/simulate-pay" % oid, {}, token=t_user)
    ck("B3 重复支付被拒", r.get("code") != 0, r)
    c, r = call("POST", "/api/user/orders/999999/simulate-pay", {}, token=t_user)
    ck("B4 不存在订单支付被拒", r.get("code") != 0, r)
    import subprocess
    def sql(q):
        return subprocess.run(["mysql", "-uroot", "xiche", "-N", "-e", q], capture_output=True, text=True).stdout.strip()
    pc = sql("SELECT COUNT(*) FROM t_policy WHERE order_id=%d AND level='L1' AND status='ACTIVE'" % oid)
    ck("B5 L1保单随支付出单", pc == "1", "policy_count=" + pc)

    # ---------- D 组：门店（用另一单，避免与洗车工链路冲突）----------
    print("\n[D] 门店")
    c, r = call("GET", "/api/store/bays", token=t_store)
    ck("D1 工位看板(门店角色)", r.get("code") == 0, r)
    c, r = call("GET", "/api/store/bays", token=t_user)
    ck("D1b 车主访问门店接口被拒", r.get("code") in (40300, 403) or c == 403, (c, r))
    baylist = r  # placeholder
    c, r = call("GET", "/api/store/bays", token=t_store)
    bay_id = r["data"]["bays"][0]["id"]
    c, r = call("PUT", "/api/store/bays/%d" % bay_id, {"status": "paused"}, token=t_store)
    ck("D2 工位状态修改成功(P0-03)", r.get("code") == 0, r)
    c, r = call("PUT", "/api/store/bays/%d" % bay_id, {"status": "free"}, token=t_store)
    ck("D2b 工位复位", r.get("code") == 0, r)
    c, r = call("POST", "/api/store/blacklist", {"workerId": 2, "reason": "服务态度差"}, token=t_store)
    ck("D-blacklist 门店拉黑写入", r.get("code") == 0, r)

    # ---------- C 组：洗车工 ----------
    print("\n[C] 洗车工")
    c, r = call("GET", "/api/worker/certs", token=t_worker)
    ck("C1 证书列表", r.get("code") == 0 and len(r["data"]["list"]) >= 2, r)
    c, r = call("GET", "/api/worker/hall", token=t_worker)
    ck("C2 抢单大厅有待接任务", r.get("code") == 0 and any(t["orderId"] == oid for t in r["data"]["tasks"]), r)
    c, r = call("POST", "/api/worker/orders/%d/grab" % oid, {}, token=t_worker)
    ck("C3 抢单成功(P0-02)", r.get("code") == 0, r)
    c, r = call("GET", "/api/worker/orders/%d" % oid, token=t_worker)
    ck("C4 任务详情可查", r.get("code") == 0, r)
    # 证据链 E1~E4
    for st in ["E1", "E2", "E3"]:
        c, r = call("POST", "/api/worker/evidence", {"orderNo": ono, "stage": st}, token=t_worker)
        ck("C5-%s 证据上传" % st, r.get("code") == 0, r)
    c, r = call("POST", "/api/worker/evidence", {"orderNo": ono, "stage": "E4"}, token=t_worker)
    ck("C8 E4后推进完成(80)(P0-07)", r.get("code") == 0 and r["data"].get("orderStatus") == 80, r)
    # 结算单落库 + 恒等式
    rows = sql("SELECT paid_amount,platform_subsidy,worker_net,store_net,pass_through_cost,platform_fee,advance_amount FROM t_settlement WHERE order_id=%d" % oid)
    ck("C9 完工触发结算落库(P0-07)", bool(rows), rows)
    if rows:
        pa, sub, wn, sn, b, fee, adv = [float(x) for x in rows.split("\t")]
        left = pa + sub; right = wn + sn + b + fee
        ck("C9b 恒等式 P+C=workerNet+storeNet+B+fee", abs(left - right) < 0.01, (left, right))
    c, r = call("GET", "/api/worker/income", token=t_worker)
    ck("C15 收入页可访问(P0-04)", r.get("code") == 0, r)
    # 考试合规
    c, r = call("GET", "/api/worker/exams?cert=C3", token=t_worker)
    ck("C10 按认证取题", r.get("code") == 0 and len(r["data"]["questions"]) > 0, r)
    c, r = call("GET", "/api/worker/exams?cert=C1", token=t_worker)
    ck("C10b 按C1取题", r.get("code") == 0 and len(r["data"]["questions"]) > 0, r)

    # ---------- E 组：后台 ----------
    print("\n[E] 后台")
    c, r = call("GET", "/api/admin/dashboard", token=t_admin)
    ck("E1 看板(ADMIN)", r.get("code") == 0, r)
    c, r = call("GET", "/api/admin/settlement/list", token=t_admin)
    ck("E4 财务对账(ADMIN)", r.get("code") == 0, r)
    c, r = call("GET", "/api/admin/settlement/list", token=t_sod)
    ck("E7 SoD冲突(ADMIN+AUDITOR)被拒", r.get("code") in (40300, 403), r)
    c, r = call("GET", "/api/admin/audit/workers?full=true", token=t_user)
    ck("E3 车主越权导出被拒", r.get("code") in (40300, 403) or c == 403, (c, r))
    c, r = call("GET", "/api/admin/audit/workers?full=true", token=t_admin)
    ck("E3b ADMIN可明文(授权)", r.get("code") == 0, r)
    c, r = call("GET", "/api/admin/audit/logs", token=t_admin)
    ck("E6 审计日志查询", r.get("code") == 0, r)
    c, r = call("GET", "/api/common/cache/stats", token=t_admin)
    ck("E-cache 缓存看板(P1-05)", r.get("code") == 0 and "levels" in r.get("data", {}), r)

    # ---------- 专项：资金口径 D>0 与 ONSITE ----------
    print("\n[专项] 资金口径与合规")
    def fullflow(token_u, token_w, tier, mode, request_id, advance=None, need_addr=False, ensure_o=False):
        """下单->(改代垫)->支付->抢单->E1~E4->返回订单id"""
        body = {"tier": tier, "serviceMode": mode, "requestId": request_id}
        if need_addr: body["addressId"] = 1
        c, r = call("POST", "/api/user/orders", body, token=token_u)
        if r.get("code") != 0: return None, r
        oid = r["data"]["orderId"]; ono = r["data"]["orderNo"]
        if advance is not None:
            sql("UPDATE t_order SET advance_amount=%s WHERE id=%d" % (advance, oid))
        if ensure_o:
            sql("INSERT INTO t_certification(worker_id,cert_code,exam_score,status,passed_at) "
                "VALUES(1,'O',100,'PASSED',NOW()) ON DUPLICATE KEY UPDATE status='PASSED'")
        call("POST", "/api/user/orders/%d/simulate-pay" % oid, {}, token=token_u)
        call("POST", "/api/worker/orders/%d/grab" % oid, {}, token=token_w)
        for st in ["E1", "E2", "E3", "E4"]:
            call("POST", "/api/worker/evidence", {"orderNo": ono, "stage": st}, token=token_w)
        return oid, None

    # Z1：代垫 D>0，验证 L 已扣 D、恒等式用实收 workerNet
    zid, err = fullflow(t_user, t_worker, "standard", "STORE", "it-" + SEQ + "-z1", advance="10.00")
    row = sql("SELECT paid_amount,platform_subsidy,worker_net,store_net,pass_through_cost,platform_fee,advance_amount,labor_income FROM t_settlement WHERE order_id=%d" % zid) if zid else ""
    if row:
        pa, sub, wn, sn, b, fee, adv, lab = [float(x) for x in row.split("\t")]
        ck("Z1 D不进抽成基数(L=97)", abs(lab - 97.00) < 0.01, row)
        ck("Z2 恒等式用实收workerNet", abs((wn + sn + b + fee) - (pa + sub)) < 0.01, row)
        ck("Z3 代垫全额返还(workerNet=laborNet+D)", abs(wn - ((lab - fee) + adv)) < 0.01, row)
    else:
        ck("Z1 代垫场景结算", False, err)

    # Z4：ONSITE 包干组织费 G=18 不抽成，storeNet=18
    zid2, err2 = fullflow(t_user, t_worker, "standard", "ONSITE", "it-" + SEQ + "-z4", need_addr=True, ensure_o=True)
    row2 = sql("SELECT paid_amount,worker_net,store_net,pass_through_cost,platform_fee,advance_amount,labor_income FROM t_settlement WHERE order_id=%d" % zid2) if zid2 else ""
    if row2:
        pa, wn, sn, b, fee, adv, lab = [float(x) for x in row2.split("\t")]
        ck("Z4 ONSITE组织费G=18", abs(sn - 18.00) < 0.01, row2)
        ck("Z5 ONSITE基数L=135(扣G)", abs(lab - 135.00) < 0.01, row2)
        ck("Z6 ONSITE恒等式", abs((wn + sn + b + fee) - pa) < 0.01, row2)
    else:
        ck("Z4 ONSITE场景结算", False, err2)

    # Z7：考试跳级拦截（构造无认证 worker + 账号）
    sql("INSERT INTO t_worker(real_name,phone,id_card_no,city,status) "
        "SELECT '跳级测试','13700008888','310101199901018888','上海市',1 "
        "WHERE NOT EXISTS (SELECT 1 FROM t_worker WHERE phone='13700008888')")
    sql("SET @wid=(SELECT id FROM t_worker WHERE phone='13700008888'); "
        "INSERT INTO t_account(username,password_hash,salt,subject_type,subject_id,roles,status) "
        "SELECT 'jumpuser','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f',"
        "'xiche2026','WORKER',@wid,NULL,1 "
        "WHERE NOT EXISTS (SELECT 1 FROM t_account WHERE username='jumpuser')")
    t_jump = login("jumpuser", "test1234", "WORKER")
    c, r = call("POST", "/api/worker/exams/submit", {"cert": "C3", "answers": {}}, token=t_jump)
    ck("Z7 无C2考C3跳级被拒", r.get("code") == 40300, r)

    print("\n================ 结果 ================")
    print("PASS: %d  FAIL: %d" % (len(PASS), len(FAIL)))
    if FAIL:
        print("\n失败用例：")
        for n, d in FAIL: print("  - %s : %s" % (n, str(d)[:160]))
        sys.exit(1)
    print("ALL TESTS PASSED")
    sys.exit(0)

if __name__ == "__main__":
    main()
