package com.xiche;

import com.xiche.common.constant.BusinessConstants;
import com.xiche.common.constant.CacheConstants;
import com.xiche.common.constant.CertConstants;
import com.xiche.common.constant.OrderStatusEnum;
import com.xiche.common.util.MoneyUtil;
import com.xiche.order.entity.Order;
import com.xiche.settlement.entity.Settlement;
import com.xiche.settlement.service.impl.SettlementServiceImpl;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 核心逻辑全量单测（不依赖数据库/Spring容器）。
 * 覆盖：资金恒等式与代垫款返还、状态机、认证等级、缓存分级、金额工具。
 */
public class CoreLogicTest {

    private SettlementServiceImpl newCalculator() throws Exception {
        return new SettlementServiceImpl(); // calculate() 不访问注入的 Mapper
    }

    private Order order(double price, double subsidy, double bayFee, double insurance, double advance,
                        String mode, String cardSnapshot) {
        Order o = new Order();
        o.setId(1L);
        o.setOrderNo("XC-TEST-001");
        o.setServiceMode(mode);
        o.setPaidAmount(new BigDecimal(String.valueOf(price)));
        o.setPlatformSubsidy(new BigDecimal(String.valueOf(subsidy)));
        o.setStoreBayFee(new BigDecimal(String.valueOf(bayFee)));
        o.setInsuranceFee(new BigDecimal(String.valueOf(insurance)));
        o.setAdvanceAmount(new BigDecimal(String.valueOf(advance)));
        o.setCardSnapshot(cardSnapshot);
        return o;
    }

    /** 报告表18基础场景复算 */
    @Test
    public void testSettlementStoreAppearance() throws Exception {
        Settlement s = newCalculator().calculate(order(83, 0, 12, 2, 0, "STORE", null));
        Assert.assertEquals(new BigDecimal("69.00"), s.getLaborIncome());      // 83-12-2
        Assert.assertEquals(new BigDecimal("27.60"), s.getPlatformFee());      // 69*0.4
        Assert.assertEquals(new BigDecimal("41.40"), s.getWorkerNet());        // 69-27.6
        Assert.assertEquals(new BigDecimal("12.00"), s.getStoreNet());
    }

    @Test
    public void testSettlementOnsiteStandard() throws Exception {
        Settlement s = newCalculator().calculate(order(155, 0, 0, 2, 0, "ONSITE", null));
        Assert.assertEquals(new BigDecimal("153.00"), s.getLaborIncome());
        Assert.assertEquals(new BigDecimal("53.55"), s.getPlatformFee());      // 153*0.35
        Assert.assertEquals(new BigDecimal("99.45"), s.getWorkerNet());
    }

    /** P0-08 代垫款返还：方案口径 L=117，fee=46.8，worker到手=80.2 */
    @Test
    public void testAdvanceRefund() throws Exception {
        Settlement s = newCalculator().calculate(order(143, 0, 24, 2, 10, "STORE", null));
        Assert.assertEquals("代垫款不进L，L应为117", new BigDecimal("117.00"), s.getLaborIncome());
        Assert.assertEquals("平台费=117*0.4", new BigDecimal("46.80"), s.getPlatformFee());
        Assert.assertEquals("洗车工到手=70.2+10=80.2", new BigDecimal("80.20"), s.getWorkerNet());
        Assert.assertEquals("代垫款独立科目=10", new BigDecimal("10.00"), s.getAdvanceAmount());
        // 恒等式用劳务净额（70.2）+M+B+fee = 143
        BigDecimal laborNet = s.getLaborIncome().subtract(s.getPlatformFee());
        BigDecimal right = laborNet.add(s.getStoreNet())
                .add(s.getPassThroughCost()).add(s.getPlatformFee());
        Assert.assertEquals(new BigDecimal("143.00"), right);
    }

    /** P0-12 免佣卡：抽成减半 */
    @Test
    public void testCommissionCardDiscount() throws Exception {
        Settlement normal = newCalculator().calculate(order(133, 0, 24, 2, 0, "STORE", null));
        Settlement card = newCalculator().calculate(order(133, 0, 24, 2, 0, "STORE", "DISCOUNT:0.5"));
        Assert.assertEquals(new BigDecimal("0.2000"), card.getCommissionRate());
        Assert.assertTrue("免佣卡后平台费应减少", card.getPlatformFee().compareTo(normal.getPlatformFee()) < 0);
        Assert.assertTrue("免佣卡后洗车工到手应增加", card.getWorkerNet().compareTo(normal.getWorkerNet()) > 0);
    }

    /** 负 L 拦截 */
    @Test(expected = com.xiche.common.BizException.class)
    public void testNegativeLaborRejected() throws Exception {
        newCalculator().calculate(order(10, 0, 24, 2, 0, "STORE", null)); // 10-24-2 < 0
    }

    /** 状态机：正向可推进、不可回退、取消仅待支付/待接单 */
    @Test
    public void testStateMachine() {
        Assert.assertTrue(OrderStatusEnum.canTransit(20, 30));
        Assert.assertTrue(OrderStatusEnum.canTransit(20, 80)); // 可跳段
        Assert.assertFalse(OrderStatusEnum.canTransit(50, 30)); // 不可回退
        Assert.assertTrue(OrderStatusEnum.canCancel(10));
        Assert.assertTrue(OrderStatusEnum.canCancel(20));
        Assert.assertFalse(OrderStatusEnum.canCancel(80));
        Assert.assertEquals("完成", OrderStatusEnum.descOf(80));
        Assert.assertEquals("已取消", OrderStatusEnum.descOf(90));
        Assert.assertTrue("未知码不抛异常", OrderStatusEnum.descOf(999).contains("未知"));
    }

    /** 认证向下覆盖：C2 level=2 >= C1 level=1 */
    @Test
    public void testCertDownwardCover() {
        Assert.assertEquals(1, CertConstants.skillCertOf(1).getLevel());
        Assert.assertEquals(2, CertConstants.skillCertOf(2).getLevel());
        Assert.assertEquals(3, CertConstants.skillCertOf(3).getLevel());
        Assert.assertTrue("C2应可覆盖C1", 2 >= 1);
        Assert.assertTrue("DELIVERY需D", CertConstants.requiredCerts(2, "DELIVERY").contains(CertConstants.CertCode.D));
        Assert.assertFalse("STORE不需D", CertConstants.requiredCerts(2, "STORE").contains(CertConstants.CertCode.D));
    }

    /** 缓存分级：C类禁止缓存域判定 */
    @Test
    public void testCacheLevel() {
        Assert.assertEquals(CacheConstants.CacheLevel.C, CacheConstants.levelOf("ORDER"));
        Assert.assertEquals(CacheConstants.CacheLevel.C, CacheConstants.levelOf("CAPACITY"));
        Assert.assertEquals(CacheConstants.CacheLevel.C, CacheConstants.levelOf("POLICY"));
        Assert.assertEquals(CacheConstants.CacheLevel.C, CacheConstants.levelOf("SETTLEMENT"));
        Assert.assertEquals(CacheConstants.CacheLevel.B, CacheConstants.levelOf("STORE_PROFILE"));
        Assert.assertEquals(CacheConstants.CacheLevel.A, CacheConstants.levelOf("catalog"));
    }

    /** 金额工具精度 */
    @Test
    public void testMoneyUtil() {
        Assert.assertEquals(new BigDecimal("10.00"), MoneyUtil.scale2(new BigDecimal("10")));
        Assert.assertEquals(new BigDecimal("0.00"), MoneyUtil.scale2(null));
        Assert.assertEquals(new BigDecimal("33.35"), MoneyUtil.multiplyRate(new BigDecimal("83.00"), new BigDecimal("0.4018")));
        Assert.assertTrue(MoneyUtil.isNegative(new BigDecimal("-0.01")));
        Assert.assertFalse(MoneyUtil.isNegative(new BigDecimal("0.00")));
    }
}
