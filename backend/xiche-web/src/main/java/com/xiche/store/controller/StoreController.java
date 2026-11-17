package com.xiche.store.controller;

import com.xiche.common.Result;
import com.xiche.common.annotation.RequireRole;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.context.LoginContext;
import com.xiche.common.util.PageUtil;
import com.xiche.governance.dao.ReviewMapper;
import com.xiche.governance.dao.BlacklistMapper;
import com.xiche.governance.entity.Review;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.store.dao.StoreBayMapper;
import com.xiche.store.entity.StoreBay;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/** 门店端 REST API（/api/store/）。 */
@RestController
@RequestMapping("/api/store")
@RequireRole({"STORE", "ADMIN"}) // BUG-P1-07：门店端接口强制门店角色，杜绝车主越权操作
public class StoreController {
    @Resource private StoreBayMapper bayMapper;
    @Resource private OrderMapper orderMapper;
    @Resource private ReviewMapper reviewMapper;
    @Resource private BlacklistMapper blacklistMapper;

    private Long currentStore() { return LoginContext.storeId(); } // P0-01

    /** P1-08：门店拉黑洗车工（局部拉黑，仅对本门店订单生效） */
    @PostMapping("/blacklist")
    public Result<Map<String, Object>> blacklist(@RequestBody Map<String, Object> body) {
        Long workerId = body.get("workerId") == null ? null : Long.valueOf(String.valueOf(body.get("workerId")));
        String reason = body.get("reason") == null ? "" : String.valueOf(body.get("reason"));
        if (workerId == null || reason.trim().isEmpty()) {
            return Result.fail(40000, "workerId 与 reason 必填");
        }
        Date from = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 30); // 冷静期 30 天
        blacklistMapper.insert(currentStore(), "WORKER", workerId, reason, from, cal.getTime());
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("blacklisted", true); d.put("workerId", workerId); d.put("cooldownDays", 30);
        return Result.success(d);
    }

    /** 工位三态看板：0有空位free / 1已满full / 2暂停paused */
    @GetMapping("/bays")
    public Result<Map<String, Object>> bays() {
        List<StoreBay> bays = bayMapper.selectByStore(currentStore());
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (StoreBay b : bays) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("id", b.getId()); m.put("name", b.getBayNo());
            int s = b.getStatus() == null ? 0 : b.getStatus();
            m.put("status", s == 0 ? "free" : s == 1 ? "full" : "paused");
            out.add(m);
        }
        Map<String, Object> data = new HashMap<String, Object>(); data.put("bays", out);
        return Result.success(data);
    }

    @PutMapping("/bays/{id}")
    public Result<Map<String, Object>> setBay(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        StoreBay bay = bayMapper.selectById(id);
        if (bay == null || !Objects.equals(bay.getStoreId(), currentStore())) {
            return Result.fail(40300, "工位不存在或不属于本门店");
        }
        String status = String.valueOf(body.get("status"));
        int code = "free".equals(status) ? 0 : "full".equals(status) ? 1 : 2;
        bayMapper.updateStatus(id, code);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("updated", true); data.put("status", status);
        return Result.success(data);
    }

    /**
     * 接单（P0-10/P1-22）：校验订单归属门店 + 状态 CAS + 工位 CAS，全程事务，
     * 消除"先 count 再 update"的并发超卖与覆盖他人 worker_id 问题。
     */
    @PostMapping("/orders/{id}/accept")
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Order o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40000, "订单不存在");
        if (!Objects.equals(o.getStoreId(), currentStore()) && !LoginContext.isAdmin()) {
            return Result.fail(40300, "订单不属于本门店");
        }
        if (!Objects.equals(o.getStatus(), OrderStatusEnum.WAITING_ACCEPT.getCode())) {
            return Result.fail(40900, "订单状态不可接单：" + OrderStatusEnum.descOf(o.getStatus()));
        }
        Long bayId = body.get("bayId") == null ? null : Long.valueOf(String.valueOf(body.get("bayId")));
        if (bayId == null) return Result.fail(40000, "请选择工位 bayId");
        // 工位 CAS 锁位：仅 free->full 成功才可继续
        int locked = bayMapper.lockBay(bayId, currentStore());
        if (locked != 1) return Result.fail(409, "工位已被占用或不存在，请刷新");
        // 订单 CAS：仅 WAITING_ACCEPT 可推进，防止并发覆盖
        int rows = orderMapper.updateStatusCas(id,
                OrderStatusEnum.WAITING_ACCEPT.getCode(), OrderStatusEnum.ACCEPTED.getCode());
        if (rows != 1) throw new RuntimeException("接单并发冲突，事务回滚");
        // P0-07：记录占用工位，完工时据此释放
        orderMapper.updateBayId(id, bayId);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("accepted", true); data.put("bayStatus", "full");
        return Result.success(data);
    }

    @GetMapping("/reviews")
    public Result<Map<String, Object>> reviews(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        List<Review> list = reviewMapper.selectList(params);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", list); data.put("total", list.size());
        return Result.success(data);
    }

    /** 五维评价（P1-13）：≤2星必须填原因，评分明细落库，评分可归因 */
    @PostMapping("/reviews/{id}/reply")
    public Result<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        StringBuilder reasons = new StringBuilder();
        int sum = 0, cnt = 0;
        if (scores != null) {
            for (Map<String, Object> s : scores) {
                int v = s.get("score") == null ? 5 : Integer.parseInt(String.valueOf(s.get("score")));
                if (v <= 2) {
                    String reason = s.get("reason") == null ? "" : String.valueOf(s.get("reason")).trim();
                    if (reason.isEmpty()) return Result.fail(40000, "维度" + s.get("dim") + "评分≤2星必须填写原因");
                    reasons.append(s.get("dim")).append(":").append(reason).append(";");
                }
                sum += v; cnt++;
            }
        }
        Review r = new Review();
        r.setOrderId(Long.valueOf(id));
        r.setWorkerId(body.get("workerId") == null ? null : Long.valueOf(String.valueOf(body.get("workerId"))));
        r.setUserId(body.get("userId") == null ? null : Long.valueOf(String.valueOf(body.get("userId"))));
        r.setStatus(1);
        r.setRating(cnt == 0 ? 5 : sum / cnt);
        r.setDimScores(scores == null ? "[]" : scores.toString());
        r.setReason(reasons.toString());
        r.setContent(body.get("content") == null ? null : String.valueOf(body.get("content")));
        reviewMapper.insert(r);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("saved", true); data.put("reviewId", r.getId());
        return Result.success(data);
    }
}
