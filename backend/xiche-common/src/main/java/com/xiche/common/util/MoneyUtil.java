package com.xiche.common.util;

import com.xiche.common.constant.BusinessConstants;

import java.math.BigDecimal;

/**
 * 金额工具：所有金额用 java.math.BigDecimal，落库 DECIMAL(10,2)，
 * 统一 HALF_UP 两位小数，禁止 double/float 参与金额运算。
 */
public final class MoneyUtil {
    private MoneyUtil() {
    }

    /** null 视为 0 */
    public static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(BusinessConstants.MONEY_SCALE) : value;
    }

    /** 统一两位小数 */
    public static BigDecimal scale2(BigDecimal value) {
        return nvl(value).setScale(BusinessConstants.MONEY_SCALE, BusinessConstants.MONEY_ROUNDING);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return scale2(nvl(a).add(nvl(b)));
    }

    public static BigDecimal sub(BigDecimal a, BigDecimal b) {
        return scale2(nvl(a).subtract(nvl(b)));
    }

    /** 按抽成比例计提，结果两位小数 */
    public static BigDecimal multiplyRate(BigDecimal base, BigDecimal rate) {
        return nvl(base).multiply(nvl(rate)).setScale(BusinessConstants.MONEY_SCALE,
                BusinessConstants.MONEY_ROUNDING);
    }

    public static int compare(BigDecimal a, BigDecimal b) {
        return nvl(a).compareTo(nvl(b));
    }

    public static boolean isNegative(BigDecimal value) {
        return nvl(value).compareTo(BigDecimal.ZERO) < 0;
    }

    /** 展示用，加人民币符号 */
    public static String format(BigDecimal value) {
        return "¥" + scale2(value).toPlainString();
    }
}
