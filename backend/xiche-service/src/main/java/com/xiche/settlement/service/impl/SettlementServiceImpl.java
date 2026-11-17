package com.xiche.settlement.service.impl;

import com.xiche.common.BizException;
import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.util.MoneyUtil;
import com.xiche.order.dao.OrderMapper;
import com.xiche.order.entity.Order;
import com.xiche.settlement.dao.LedgerMapper;
import com.xiche.settlement.dao.SettlementMapper;
import com.xiche.settlement.entity.Ledger;
import com.xiche.settlement.entity.Settlement;
import com.xiche.settlement.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 平台撮合分账（业务口径 4，不得改动）：
 *   STORE 抽成 40% / ONSITE 抽成 35%
 *   抽成基数只含劳务收入 L；工位费 M、包干组织费 G、保险费 B、代垫款 D 均不抽成
 *   STORE: storeNet = M(工位费)；ONSITE: storeNet = G(包干组织费18)
 *   L = P + C - storeNet - B - D（BUG-P1-01：代垫 D 与组织费 G 均不进抽成基数）
 *   platformFee = L × r
 *   workerLaborNet = L - platformFee
 *   workerNet(实收) = workerLaborNet + D（代垫款全额返还洗车工）
 *   恒等式：P + C = workerNet + storeNet + B + platformFee（BUG-P1-01：用实收 workerNet 校验）
 */
@Service
public class SettlementServiceImpl implements SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementServiceImpl.class);

    @Resource private SettlementMapper settlementMapper;
    @Resource private LedgerMapper ledgerMapper;
    @Resource private OrderMapper orderMapper;

    /** P0-12：解析 cardSnapshot 免佣折扣率。约定 "DISCOUNT:0.5" 表示抽成减半；null/其它为 0 */
    private BigDecimal cardDiscountOf(String cardSnapshot) {
        if (cardSnapshot == null || !cardSnapshot.startsWith("DISCOUNT:")) return BigDecimal.ZERO;
        try {
            BigDecimal d = new BigDecimal(cardSnapshot.substring("DISCOUNT:".length()).trim());
            return d.compareTo(BigDecimal.ZERO) >= 0 && d.compareTo(BigDecimal.ONE) <= 0
                    ? d : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Settlement calculate(Order order) {
        if (order == null || order.getId() == null) {
            throw new BizException("结算失败：订单不存在");
        }
        String mode = order.getServiceMode();
        if (!BusinessConstants.MODE_STORE.equals(mode) && !BusinessConstants.MODE_ONSITE.equals(mode)) {
            throw new BizException("未知服务模式，无法确定抽成比例：" + mode);
        }

        BigDecimal p = MoneyUtil.scale2(order.getPaidAmount());
        BigDecimal c = MoneyUtil.scale2(order.getPlatformSubsidy());
        BigDecimal insuranceFee = MoneyUtil.scale2(order.getInsuranceFee());
        BigDecimal advance = MoneyUtil.scale2(order.getAdvanceAmount()); // D 代垫款
        BigDecimal b = insuranceFee; // 过账成本只含保险，代垫款拆出单列

        // BUG-P1-01：门店侧净收入 STORE=工位费M；ONSITE=包干组织费G(18)，二者均不抽成
        BigDecimal storeNet = BusinessConstants.MODE_ONSITE.equals(mode)
                ? BusinessConstants.ONSITE_ORG_FEE : MoneyUtil.scale2(order.getStoreBayFee());

        // L 劳务收入：唯一抽成基数（D 与 storeNet 均不进 L）
        // BUG-P1-01：L = P + C - storeNet - B - D
        BigDecimal l = MoneyUtil.scale2(p.add(c).subtract(storeNet).subtract(b).subtract(advance));
        if (MoneyUtil.isNegative(l)) {
            throw new BizException("劳务收入 L 为负，拒绝结算：" + l.toPlainString());
        }

        // 免佣卡减免抽成；快照固定当时口径（下单已不接受客户端快照，恒为 NONE）
        BigDecimal baseRate = BusinessConstants.commissionRateOf(mode);
        BigDecimal discount = cardDiscountOf(order.getCardSnapshot());
        BigDecimal rate = baseRate.multiply(BigDecimal.ONE.subtract(discount))
                .setScale(4, BusinessConstants.MONEY_ROUNDING);
        BigDecimal platformFee = MoneyUtil.multiplyRate(l, rate);        // 平台抽成
        BigDecimal workerLaborNet = MoneyUtil.sub(l, platformFee);        // 劳务净额
        BigDecimal workerNet = MoneyUtil.add(workerLaborNet, advance);    // 实收 = 劳务净额 + 代垫返还

        // BUG-P1-01：恒等式用实收 workerNet 校验（此前用 workerLaborNet 掩盖 D>0 时资金错位）
        // P + C = workerNet + storeNet + B + platformFee
        BigDecimal left = MoneyUtil.scale2(p.add(c));
        BigDecimal right = MoneyUtil.scale2(workerNet.add(storeNet).add(b).add(platformFee));
        if (left.compareTo(right) != 0) {
            throw new BizException("分账恒等式不成立：P+C=" + left.toPlainString()
                    + "，workerNet+storeNet+B+platformFee=" + right.toPlainString());
        }

        Settlement s = new Settlement();
        s.setOrderId(order.getId());
        s.setOrderNo(order.getOrderNo());
        s.setServiceMode(mode);
        s.setPaidAmount(p);
        s.setPlatformSubsidy(c);
        s.setStoreBayFee(MoneyUtil.scale2(order.getStoreBayFee()));
        s.setPassThroughCost(b);
        s.setAdvanceAmount(advance);       // P0-08：代垫款独立科目
        s.setLaborIncome(l);
        s.setCommissionRate(rate);
        s.setPlatformFee(platformFee);
        s.setWorkerNet(workerNet);         // 对外到手额（含代垫返还）
        s.setStoreNet(storeNet);
        s.setTotal(left);
        s.setStatus("SETTLED");
        s.setEnvFlag(BusinessConstants.ENV_FLAG);

        log.info("[{}] 结算 orderNo={} L={} rate={} fee={} workerNet={} workerLaborNet={} storeNet={} B={} D={}",
                BusinessConstants.ENV_FLAG, order.getOrderNo(), l, rate, platformFee,
                workerNet, workerLaborNet, storeNet, b, advance);
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
        try {
            settlementMapper.insert(result);
        } catch (DuplicateKeyException e) {
            // 报告5.4：并发重复结算返回既有结果，而非抛 50000
            log.info("[{}] 结算并发幂等命中 orderNo={}", BusinessConstants.ENV_FLAG, order.getOrderNo());
            return settlementMapper.selectByOrderId(order.getId());
        }
        writeLedger(result); // P0-18：三方台账
        orderMapper.updateSettlement(order);
        return result;
    }

    /** P0-18：写入 PLATFORM / WORKER / STORE 三方资金台账（模拟） */
    private void writeLedger(Settlement s) {
        ledger(s, "STORE", s.getOrderId(), "工位费M", "CREDIT", s.getStoreNet());
        ledger(s, "WORKER", s.getOrderId(), "劳务净额+代垫返还", "CREDIT", s.getWorkerNet());
        ledger(s, "PLATFORM", s.getOrderId(), "平台抽成", "CREDIT", s.getPlatformFee());
    }

    private void ledger(Settlement s, String type, Long accountId, String subject, String direction, BigDecimal amount) {
        Ledger l = new Ledger();
        l.setLedgerNo("LG-" + s.getSettlementNo() + "-" + type);
        l.setSettlementId(s.getId());
        l.setAccountType(type);
        l.setAccountId(accountId);
        l.setSubject(subject);
        l.setDirection(direction);
        l.setAmount(MoneyUtil.scale2(amount));
        l.setEnvFlag(BusinessConstants.ENV_FLAG);
        ledgerMapper.insert(l);
    }

    @Override
    public Settlement queryByOrderId(Long orderId) {
        return settlementMapper.selectByOrderId(orderId);
    }
}
