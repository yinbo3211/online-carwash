package com.xiche.common.constant;

/**
 * 支付口径（业务口径 1，不得改动）：
 * V1 内部测试版仅允许 MockPaymentGateway，禁止接入真实支付宝/微信；
 * 支付成功文案固定为「模拟支付成功·测试数据，不产生真实资金收付」，成功态用橙色。
 */
public final class PaymentConstants {
    private PaymentConstants() {
    }

    /** 测试环境标识，开工单与所有模拟支付流水必须携带 */
    public static final String ENV_FLAG = "TEST-ENV-V1";

    /** 唯一允许的支付网关实现 */
    public static final String GATEWAY_BEAN = "MockPaymentGateway";
    public static final boolean REAL_PAYMENT_ENABLED = false;

    /** 支付成功固定文案与成功态颜色（橙色） */
    public static final String PAY_SUCCESS_MESSAGE = "模拟支付成功·测试数据，不产生真实资金收付";
    public static final String PAY_SUCCESS_COLOR = "#FF9800";

    public static final String PAY_FAIL_MESSAGE = "模拟支付失败·测试数据，不产生真实资金收付";
    public static final String PAY_STATUS_SUCCESS = "SUCCESS";
    public static final String PAY_STATUS_FAILED = "FAILED";

    /** 幂等：同一业务单号只允许成功支付一次 */
    public static final String IDEMPOTENT_KEY_PREFIX = "mockpay:" + ENV_FLAG + ":";
}
