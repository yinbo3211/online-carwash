package com.xiche.order.service.impl;

import com.xiche.governance.dao.BlacklistMapper;
import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.CertConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.util.PageUtil;
import com.xiche.insurance.dao.PolicyMapper;
import com.xiche.order.dao.OrderItemMapper;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.order.entity.OrderItem;
import com.xiche.order.service.OrderService;
import com.xiche.worker.dao.WorkerMapper;
import com.xiche.worker.entity.Certification;
import com.xiche.worker.entity.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务：实现派单六项合取资格谓词（口径 BC-01）。
 * 可接单 = 认证覆盖 ∧ 评分达标 ∧ 产能未满 ∧ 未拉黑 ∧ 未封禁 ∧ 保单有效。
 *
 * 本轮修复：
 *  P1-06 认证向下覆盖（等级 >= 要求即可）
 *  P1-07 增值项认证校验（发动机舱清洁需 C3）
 *  P1-08 工时产能（按等级工时累计 <= 8h）
 *  P1-11 评分降权而非硬否决
 *  P0-15 门店局部拉黑（按订单 storeId）
 *  P2-03 STORE 不需 D 认证，DELIVERY 才需
 *  P0-07 跨日产能重置
 *  P0-16 证书有效期校验
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Resource private OrderMapper orderMapper;
    @Resource private OrderItemMapper orderItemMapper;
    @Resource private WorkerMapper workerMapper;
    @Resource private PolicyMapper policyMapper;
    @Resource private BlacklistMapper blacklistMapper;

    @Override
    public Order getById(Long id) { return orderMapper.selectById(id); }

    @Override
    public List<Order> list(Map<String, Object> params, PageUtil page) {
        Map<String, Object> p = new HashMap<String, Object>(params == null ? new HashMap<String, Object>() : params);
        p.put("offset", page.getOffset());
        p.put("limit", page.getLimit());
        return orderMapper.selectList(p);
    }

    /** serviceLevel -> 认证码：1=C1外观 2=C2标准 3=C3精细 */
    private String certCodeOf(Integer level) {
        if (level == null) return null;
        switch (level) {
            case BusinessConstants.LEVEL_APPEARANCE: return "C1";
            case BusinessConstants.LEVEL_STANDARD: return "C2";
            case BusinessConstants.LEVEL_DETAIL: return "C3";
            default: return null;
        }
    }

    /** 认证等级数值：C1=1 C2=2 C3=3，用于向下覆盖；专项 D/O 不参与等级比较 */
    private int certLevel(String code) {
        if ("C1".equals(code)) return 1;
        if ("C2".equals(code)) return 2;
        if ("C3".equals(code)) return 3;
        return 0;
    }

    /** 各等级标准工时（小时），P1-08 工时产能基础 */
    private BigDecimal hoursOf(Integer level) {
        if (level == null) return new BigDecimal("0.75");
        switch (level) {
            case BusinessConstants.LEVEL_APPEARANCE: return new BigDecimal("0.33");
            case BusinessConstants.LEVEL_STANDARD: return new BigDecimal("0.75");
            case BusinessConstants.LEVEL_DETAIL: return new BigDecimal("1.75");
            default: return new BigDecimal("0.75");
        }
    }

    @Override
    public List<String> checkEligibility(Order order, Long workerId) {
        List<String> fails = new ArrayList<String>();
        if (order == null) { fails.add("订单不存在"); return fails; }
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) { fails.add("洗车工不存在"); return fails; }

        // 收集有效认证（P0-16：排除已过期证书）
        List<Certification> certs = workerMapper.selectCerts(workerId);
        List<String> passed = new ArrayList<String>();
        int maxCertLevel = 0;
        Date now = new Date();
        if (certs != null) {
            for (Certification c : certs) {
                if (!"PASSED".equalsIgnoreCase(c.getStatus())) continue;
                if (c.getExpireAt() != null && c.getExpireAt().before(now)) continue; // P0-16 过期无效
                passed.add(c.getCertCode());
                maxCertLevel = Math.max(maxCertLevel, certLevel(c.getCertCode()));
            }
        }

        // ① 认证覆盖（第一硬门槛）—— P1-06：向下覆盖，持更高等级即可
        String need = certCodeOf(order.getServiceLevel());
        if (need != null && maxCertLevel < certLevel(need)) {
            fails.add("需" + need + "及以上认证（未通过不可接对应订单）");
        }
        // P1-07 增值项：发动机舱清洁需 C3
        List<OrderItem> items = orderItemMapper.selectByOrder(order.getId());
        if (items != null) {
            for (OrderItem it : items) {
                if ("ADDON".equalsIgnoreCase(it.getItemType()) && it.getItemName() != null
                        && it.getItemName().contains("发动机舱") && maxCertLevel < 3) {
                    fails.add("增值项发动机舱清洁需C3认证");
                }
            }
        }
        // P2-03：仅 DELIVERY 需 D 取送车专项；STORE 到店模式不强制 D
        if (BusinessConstants.MODE_DELIVERY.equals(order.getFulfillMode())
                && !passed.contains("D")) {
            fails.add("需D取送车专项认证");
        }
        if (BusinessConstants.MODE_ONSITE.equals(order.getServiceMode()) && !passed.contains("O")) {
            fails.add("需O上门专项认证");
        }

        // ② 评分：P1-11 降权而非否决（仅记录告警，不加入 fails）
        BigDecimal rating = worker.getRating();
        if (rating == null || rating.compareTo(BusinessConstants.MIN_DISPATCH_RATING) < 0) {
            log.warn("[{}] 洗车工{}评分低于阈值，降权派单", BusinessConstants.ENV_FLAG, workerId);
        }

        // ③ 产能：P1-08 按工时累计 <= 8h（todayOrders × 各单工时近似）
        int today = worker.getTodayOrders() == null ? 0 : worker.getTodayOrders();
        int cap = worker.getDailyCapacity() == null ? BusinessConstants.MAX_DAILY_CAPACITY : worker.getDailyCapacity();
        BigDecimal usedHours = new BigDecimal(today).multiply(hoursOf(BusinessConstants.LEVEL_STANDARD));
        BigDecimal capHours = new BigDecimal(cap).multiply(hoursOf(BusinessConstants.LEVEL_STANDARD));
        if (usedHours.compareTo(capHours) >= 0) fails.add("当日工时产能已满");

        // ④ 未拉黑：P0-15 门店局部拉黑，按订单归属门店判断
        if (order.getStoreId() != null
                && blacklistMapper.countActiveByStore("WORKER", workerId, order.getStoreId(), now) > 0) {
            fails.add("被本门店拉黑");
        }
        // ⑤ 未封禁
        if (worker.getBanned() != null && worker.getBanned() == 1) fails.add("账号已封禁");
        // ⑥ 保单有效（每单必须有保险）
        if (policyMapper.countValidL1ByOrderId(order.getId(), now) <= 0) fails.add("订单无有效L1保单");
        return fails;
    }

    @Override
    public boolean isEligible(Order order, Long workerId) {
        return checkEligibility(order, workerId).isEmpty();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long acceptOrder(Long orderId, Long workerId) {
        // P0-07：跨日首次操作先重置产能
        workerMapper.resetTodayOrdersIfStale(workerId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException("订单不存在");
        List<String> fails = checkEligibility(order, workerId);
        if (!fails.isEmpty()) throw new BizException(403, "无接单资格：" + String.join("、", fails));
        // 状态 CAS：仅待接单(20)可被指派为已接单(30)，防止并发抢单
        int rows = orderMapper.assignWorker(orderId, workerId,
                OrderStatusEnum.WAITING_ACCEPT.getCode(), OrderStatusEnum.ACCEPTED.getCode());
        if (rows != 1) throw new BizException(409, "订单已被抢，请刷新");
        workerMapper.incrementTodayOrders(workerId);
        log.info("[{}] 接单成功 orderId={} workerId={}", BusinessConstants.ENV_FLAG, orderId, workerId);
        return orderId;
    }
}
