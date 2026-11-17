package com.xiche.admin.controller;

import com.xiche.auth.service.AuditService;
import com.xiche.common.Result;
import com.xiche.common.annotation.RequireRole;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.util.SensitiveUtil;
import com.xiche.order.dao.OrderMapper;
import com.xiche.settlement.dao.LedgerMapper;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.worker.dao.WorkerMapper;
import com.xiche.worker.entity.Worker;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/** 后台管理 REST API（/api/admin/）+ 后台 JSP 页面路由。 */
@Controller
public class AdminController {
    @Resource private OrderMapper orderMapper;
    @Resource private SettlementMapper settlementMapper;
    @Resource private LedgerMapper ledgerMapper;
    @Resource private WorkerMapper workerMapper;
    @Resource private AuditService auditService;

    @RequestMapping("/admin/dashboard")
    public ModelAndView dashboardPage() { return new ModelAndView("admin/dashboard"); }
    @RequestMapping("/admin/audit")
    public ModelAndView auditPage() { return new ModelAndView("admin/audit"); }
    @RequestMapping("/admin/orders")
    public ModelAndView ordersPage() { return new ModelAndView("admin/orders"); }
    @RequestMapping("/admin/settlement")
    public ModelAndView settlementPage() { return new ModelAndView("admin/settlement"); }

    /** P1-15：看板指标改由真实数据计算，删除硬编码 95.2%/86ms 假指标 */
    @GetMapping("/api/admin/dashboard")
    @RequireRole({"ADMIN", "OPERATIONS"})
    @ResponseBody
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("envFlag", BusinessConstants.ENV_FLAG);
        Map<String, Object> kpi = new LinkedHashMap<String, Object>();
        int totalOrders = orderMapper.countList(new HashMap<String, Object>());
        Map<String, Object> stake = new HashMap<String, Object>();
        stake.put("status", 20);
        int waiting = orderMapper.countList(stake);
        kpi.put("orderCount", totalOrders);
        kpi.put("waitingAccept", waiting);
        kpi.put("settledCount", settlementMapper.selectList(emptyPage()).size());
        kpi.put("note", "真实指标来自库表；缓存命中率/P95需接入监控后补充，V1测试环境不展示假数据");
        d.put("kpi", kpi);
        return Result.success(d);
    }

    private Map<String, Object> emptyPage() {
        Map<String, Object> p = new HashMap<String, Object>();
        p.put("offset", 0); p.put("limit", 200);
        return p;
    }

    /** P0-02 RBAC + P1-24 字段脱敏：仅 AUDITOR/ADMIN 可见完整身份证，其余掩码 */
    @GetMapping("/api/admin/audit/workers")
    @RequireRole({"ADMIN", "AUDITOR"})
    @ResponseBody
    public Result<Map<String, Object>> auditWorkers(@RequestParam(required = false) String full) {
        boolean fullAccess = Boolean.parseBoolean(full)
                && com.xiche.common.context.LoginContext.isAdmin();
        List<Worker> list = workerMapper.selectList(new HashMap<String, Object>());
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (list != null) {
            for (Worker w : list) {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("id", w.getId()); m.put("realName", w.getRealName());
                m.put("phone", SensitiveUtil.phone(w.getPhone(), fullAccess));
                m.put("idCardNo", SensitiveUtil.idCard(w.getIdCardNo(), fullAccess));
                m.put("city", w.getCity()); m.put("rating", w.getRating()); m.put("banned", w.getBanned());
                out.add(m);
            }
        }
        Map<String, Object> d = new HashMap<String, Object>(); d.put("list", out);
        return Result.success(d);
    }

    /** P0-02：财务对账角色，职责分离——不可由 AUDITOR 访问 */
    @GetMapping("/api/admin/settlement/list")
    @RequireRole(value = {"ADMIN", "FINANCE"}, conflictWith = {"AUDITOR"})
    @ResponseBody
    public Result<Map<String, Object>> settlements() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("list", settlementMapper.selectList(emptyPage()));
        return Result.success(d);
    }

    /** P0-18：T+1 三方对账视图（按结算单汇总各账户发生额） */
    @GetMapping("/api/admin/settlement/reconcile")
    @RequireRole(value = {"ADMIN", "FINANCE"}, conflictWith = {"AUDITOR"})
    @ResponseBody
    public Result<Map<String, Object>> reconcile(@RequestParam Long settlementId) {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("settlementId", settlementId);
        d.put("accounts", ledgerMapper.reconcileBySettlement(settlementId));
        return Result.success(d);
    }

    /** P0-17：审计日志查询（WORM 只增不改） */
    @GetMapping("/api/admin/audit/logs")
    @RequireRole({"ADMIN", "COMPLIANCE"})
    @ResponseBody
    public Result<Map<String, Object>> auditLogs() {
        return Result.success(auditService.query(new HashMap<String, Object>()));
    }
}
