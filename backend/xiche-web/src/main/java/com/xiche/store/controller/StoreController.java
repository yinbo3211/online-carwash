package com.xiche.store.controller;

import com.xiche.common.Result;
import com.xiche.common.util.PageUtil;
import com.xiche.governance.dao.ReviewMapper;
import com.xiche.governance.entity.Review;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.store.dao.StoreBayMapper;
import com.xiche.store.entity.StoreBay;
import com.xiche.common.constant.OrderStatusEnum;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

/** 门店端 REST API（/api/store/）。 */
@RestController
@RequestMapping("/api/store")
public class StoreController {
    @Resource private StoreBayMapper bayMapper;
    @Resource private OrderMapper orderMapper;
    @Resource private ReviewMapper reviewMapper;

    private Long currentStore() { return 1L; } // V1 测试：浦东店

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
        String status = String.valueOf(body.get("status"));
        int code = "free".equals(status) ? 0 : "full".equals(status) ? 1 : 2;
        bayMapper.updateStatus(id, code);
        Map<String, Object> data = new HashMap<String, Object>(); data.put("updated", true); data.put("status", status);
        return Result.success(data);
    }

    /** 接单：无 free 工位时服务端拦截（前后端双重校验） */
    @PostMapping("/orders/{id}/accept")
    public Result<Map<String, Object>> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (bayMapper.countFree(currentStore()) <= 0) return Result.fail(409, "门店无空闲工位，无法接单");
        Long bayId = body.get("bayId") == null ? null : Long.valueOf(String.valueOf(body.get("bayId")));
        if (bayId != null) bayMapper.updateStatus(bayId, 1);
        orderMapper.updateStatus(id, OrderStatusEnum.ACCEPTED.getCode());
        Map<String, Object> data = new HashMap<String, Object>(); data.put("accepted", true); data.put("bayStatus", "full");
        return Result.success(data);
    }

    @GetMapping("/reviews")
    public Result<Map<String, Object>> reviews(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer pageSize) {
        PageUtil pu = PageUtil.of(page, pageSize);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("offset", pu.getOffset()); params.put("limit", pu.getLimit());
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", reviewMapper.selectList(params)); data.put("total", 0);
        return Result.success(data);
    }

    /** 五维评价：≤2星必须填原因（服务端校验） */
    @PostMapping("/reviews/{id}/reply")
    public Result<Map<String, Object>> reply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) body.get("scores");
        if (scores != null) {
            for (Map<String, Object> s : scores) {
                int v = s.get("score") == null ? 5 : Integer.parseInt(String.valueOf(s.get("score")));
                if (v <= 2 && (s.get("reason") == null || String.valueOf(s.get("reason")).trim().isEmpty()))
                    return Result.fail(40000, "维度" + s.get("dim") + "评分≤2星必须填写原因");
            }
        }
        Map<String, Object> data = new HashMap<String, Object>(); data.put("saved", true);
        return Result.success(data);
    }
}
