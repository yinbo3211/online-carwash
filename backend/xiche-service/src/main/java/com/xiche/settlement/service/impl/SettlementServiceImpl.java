package com.xiche.settlement.service.impl;

import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.util.MoneyUtil;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.settlement.entity.Settlement;
import com.xiche.settlement.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 平台撮合分账（业务口径 4，不得改动）：
 *   STORE 抽成 40% / ONSITE 抽成 35%
 *   抽成基数只含劳务收入 L；工位费 M、保险费、代垫款不抽成
 *   恒等式：P + C = worker_net + store_net + B + platform_fee
 * 其中 B = 保险费 + 代垫款（过账成本），L = P + C - M - B。
 */
@Service
public class SettlementServiceImpl implements SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);

    @Resource
    private SettlementMapper settlementMapper;
    @Resource
    private OrderMapper orderMapper;

    @Override
    public Settlement calculate(Order order) {
        if (order == null || order.getId() == null) {
            throw new BizException("结算失败：订单不存在");
        }
        String mode = order.getServiceMode();
        if (!BusinessConstants.MODE_STORE.equals(mode) && !BusinessConstants.MODE_ONSITE.equals(mode)) {
            throw new BizException("未知服务模式，无法确定抽成比例：" + mode);
        }

        // P 用户实付；C 平台补贴；M 门店工位费（不抽成）
        BigDecimal p = MoneyUtil.scale2(order.getPaidAmount());
        BigDecimal c = MoneyUtil.scale2(order.getPlatformSubsidy());
        BigDecimal m = MoneyUtil.scale2(order.getStoreBayFee());
        // B 过账成本 = 保险费 + 代垫款（二者均不抽成）
        BigDecimal insuranceFee = MoneyUtil.scale2(order.getInsuranceFee());
        BigDecimal advance = MoneyUtil.scale2(order.getAdvanceAmount());
        BigDecimal b = MoneyUtil.add(insuranceFee, advance);

        // L 劳务收入：唯一抽成基数
        BigDecimal l = MoneyUtil.scale2(p.add(c).subtract(m).subtract(b));
        if (MoneyUtil.isNegative(l)) {
            throw new BizException("劳务收入 L 为负，拒绝结算：" + l.toPlainString());
        }

        BigDecimal rate = BusinessConstants.commissionRateOf(mode);
        BigDecimal platformFee = MoneyUtil.multiplyRate(l, rate);   // 平台抽成
        BigDecimal workerNet = MoneyUtil.sub(l, platformFee);       // 洗车工净收入
        BigDecimal storeNet = m;                                    // 门店净收入 = 工位费

        // 恒等式校验：P + C = worker_net + store_net + B + platform_fee
        BigDecimal left = MoneyUtil.scale2(p.add(c));
        BigDecimal right = MoneyUtil.scale2(workerNet.add(storeNet).add(b).add(platformFee));
        assert left.compareTo(right) == 0
                : "恒等式不成立: P+C=" + left + " RHS=" + right;
        if (left.compareTo(right) != 0) {
            throw new BizException("分账恒等式不成立：P+C=" + left.toPlainString()
                    + "，worker_net+store_net+B+platform_fee=" + right.toPlainString());
        }

        Settlement s = new Settlement();
        s.setOrderId(order.getId());
        s.setOrderNo(order.getOrderNo());
        s.setServiceMode(mode);
        s.setPaidAmount(p);
        s.setPlatformSubsidy(c);
        s.setStoreBayFee(m);
        s.setPassThroughCost(b);
        s.setLaborIncome(l);
        s.setCommissionRate(rate);
        s.setPlatformFee(platformFee);
        s.setWorkerNet(workerNet);
        s.setStoreNet(storeNet);
        s.setTotal(left);
        s.setStatus("SETTLED");
        s.setEnvFlag(BusinessConstants.ENV_FLAG);

        log.info("[{}] 结算 orderNo={} L={} rate={} platformFee={} workerNet={} storeNet={} B={}",
                BusinessConstants.ENV_FLAG, order.getOrderNo(), l, rate, platformFee,
                workerNet, storeNet, b);
        return s;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Settlement settle(Order order) {
        Settlement exists = settlementMapper.selectByOrderId(order.getId());
        if (exists != null) {
            log.info("[{}] 结算幂等命中 orderNo={}", BusinessConstants.ENV_FLAG, order.getOrderNo());
            return exists;
        }
        Settlement result = calculate(order);
        result.setSettlementNo("ST-" + order.getOrderNo());
        settlementMapper.insert(result);
        orderMapper.updateSettlement(order);
        return result;
    }

    @Override
    public Settlement queryByOrderId(Long orderId) {
        return settlementMapper.selectByOrderId(orderId);
    }
}
