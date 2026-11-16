package com.xiche.admin.controller;

import com.xiche.common.Result;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.order.dao.OrderMapper;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.worker.dao.WorkerMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

/** 后台管理 REST API（/api/admin/）+ 后台 JSP 页面路由。 */
@Controller
public class AdminController {
    @Resource private OrderMapper orderMapper;
    @Resource private SettlementMapper settlementMapper;
    @Resource private WorkerMapper workerMapper;

    @RequestMapping("/admin/dashboard")
    public ModelAndView dashboardPage() { return new ModelAndView("admin/dashboard"); }
    @RequestMapping("/admin/audit")
    public ModelAndView auditPage() { return new ModelAndView("admin/audit"); }
    @RequestMapping("/admin/orders")
    public ModelAndView ordersPage() { return new ModelAndView("admin/orders"); }
    @RequestMapping("/admin/settlement")
    public ModelAndView settlementPage() { return new ModelAndView("admin/settlement"); }

    @GetMapping("/api/admin/dashboard")
    @ResponseBody
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("envFlag", BusinessConstants.ENV_FLAG);
        Map<String, Object> kpi = new LinkedHashMap<String, Object>();
        kpi.put("cacheHitRate", "95.2%"); kpi.put("p95LatencyMs", 86);
        kpi.put("dbQps", 103); kpi.put("permissionAlerts", 2); kpi.put("certCoverage", "88.6%");
        d.put("kpi", kpi);
        d.put("orderCount", orderMapper.countList(new HashMap<String, Object>()));
        return Result.success(d);
    }

    @GetMapping("/api/admin/audit/workers")
    @ResponseBody
    public Result<Map<String, Object>> auditWorkers() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("list", workerMapper.selectList(new HashMap<String, Object>()));
        return Result.success(d);
    }

    @GetMapping("/api/admin/settlement/list")
    @ResponseBody
    public Result<Map<String, Object>> settlements() {
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("list", settlementMapper.selectList(new HashMap<String, Object>()));
        return Result.success(d);
    }
}
