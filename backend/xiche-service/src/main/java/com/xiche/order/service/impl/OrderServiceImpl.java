package com.xiche.order.service.impl;

import com.xiche.governance.dao.BlacklistMapper;
import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.util.PageUtil;
import com.xiche.insurance.dao.PolicyMapper;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.order.service.OrderService;
import com.xiche.worker.dao.WorkerMapper;
import com.xiche.worker.entity.Certification;
import com.xiche.worker.entity.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务：实现派单六项合取资格谓词（口径 BC-01）。
 * 可接单 = 认证覆盖 ∧ 评分达标 ∧ 产能未满 ∧ 未拉黑 ∧ 未封禁 ∧ 保单有效。
 * 认证等级按 order.serviceLevel 映射 C1/C2/C3；ONSITE 额外需 O，取送车额外需 D。
 */
@Service
public class OrderServiceImpl implements OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Resource private OrderMapper orderMapper;
    @Resource private WorkerMapper workerMapper;
    @Resource private PolicyMapper policyMapper;
    @Resource private BlacklistMapper blacklistMapper;

    @Override
    public Order getById(Long id) {
        return orderMapper.selectById(id);
    }

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

    @Override
    public List<String> checkEligibility(Order order, Long workerId) {
        List<String> fails = new ArrayList<String>();
        if (order == null) { fails.add("订单不存在"); return fails; }
        Worker worker = workerMapper.selectById(workerId);
        if (worker == null) { fails.add("洗车工不存在"); return fails; }

        // ① 认证覆盖（第一硬门槛）
        List<Certification> certs = workerMapper.selectCerts(workerId);
        List<String> passed = new ArrayList<String>();
        if (certs != null) {
            for (Certification c : certs) {
                if ("PASSED".equalsIgnoreCase(c.getStatus())) passed.add(c.getCertCode());
            }
        }
        String need = certCodeOf(order.getServiceLevel());
        if (need != null && !passed.contains(need)) fails.add("需" + need + "认证（未通过不可接对应订单）");
        if (BusinessConstants.MODE_ONSITE.equals(order.getServiceMode()) && !passed.contains("O")) {
            fails.add("需O上门专项认证");
        }
        if (BusinessConstants.MODE_STORE.equals(order.getServiceMode()) && !passed.contains("D")) {
            fails.add("需D取送车专项认证");
        }

        // ② 评分降权（注意：合规校准——只影响派单，不扣已完单报酬）
        if (worker.getRating() == null || worker.getRating().compareTo(BusinessConstants.MIN_DISPATCH_RATING) < 0) {
            fails.add("评分低于派单阈值" + BusinessConstants.MIN_DISPATCH_RATING);
        }
        // ③ 产能
        int today = worker.getTodayOrders() == null ? 0 : worker.getTodayOrders();
        int cap = worker.getDailyCapacity() == null ? BusinessConstants.MAX_DAILY_CAPACITY : worker.getDailyCapacity();
        if (today >= cap) fails.add("当日产能已满");
        // ④ 未拉黑
        if (blacklistMapper.countActive("WORKER", workerId, new Date()) > 0) fails.add("被门店拉黑");
        // ⑤ 未封禁
        if (worker.getBanned() != null && worker.getBanned() == 1) fails.add("账号已封禁");
        // ⑥ 保单有效（每单必须有保险）
        if (policyMapper.countValidL1ByOrderId(order.getId(), new Date()) <= 0) fails.add("订单无有效L1保单");
        return fails;
    }

    @Override
    public boolean isEligible(Order order, Long workerId) {
        return checkEligibility(order, workerId).isEmpty();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long acceptOrder(Long orderId, Long workerId) {
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
