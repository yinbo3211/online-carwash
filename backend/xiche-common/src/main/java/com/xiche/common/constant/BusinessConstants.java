package com.xiche.common.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 平台核心业务常量（业务口径 3 / 4 / 2，不得改动）：
 * 价格：三档 外观 83 / 标准 133 / 精细 243；上门 95 / 155。
 * 抽成：STORE 40% / ONSITE 35%；抽成基数只含劳务收入 L。
 * 保险：L1 每单强制不可取消，L2 自愿加购，每单必须有保险。
 */
public final class BusinessConstants {
    private BusinessConstants() {
    }

    public static final String ENV_FLAG = "TEST-ENV-V1";

    /** 服务模式 */
    public static final String MODE_STORE = "STORE";
    public static final String MODE_ONSITE = "ONSITE";
    public static final String MODE_DELIVERY = "DELIVERY";

    /** 服务等级：1 外观 / 2 标准 / 3 精细 */
    public static final int LEVEL_APPEARANCE = 1;
    public static final int LEVEL_STANDARD = 2;
    public static final int LEVEL_DETAIL = 3;

    /** 到店价格（元） */
    public static final BigDecimal PRICE_STORE_APPEARANCE = new BigDecimal("83.00");
    public static final BigDecimal PRICE_STORE_STANDARD = new BigDecimal("133.00");
    public static final BigDecimal PRICE_STORE_DETAIL = new BigDecimal("243.00");

    /** 上门价格（元）；上门精细价 v6 未给定，暂不设值，禁止臆造 */
    public static final BigDecimal PRICE_ONSITE_APPEARANCE = new BigDecimal("95.00");
    public static final BigDecimal PRICE_ONSITE_STANDARD = new BigDecimal("155.00");
    public static final BigDecimal PRICE_ONSITE_DETAIL = null;

    /** 抽成比例：仅对劳务收入 L 计提；工位费 M、保险费、代垫款不抽成 */
    public static final BigDecimal RATE_STORE = new BigDecimal("0.40");
    public static final BigDecimal RATE_ONSITE = new BigDecimal("0.35");

    /** 保险：L1 基础保障每单强制，不可取消；L2 增值保障自愿加购 */
    public static final String INSURANCE_L1 = "L1";
    public static final String INSURANCE_L2 = "L2";
    public static final boolean INSURANCE_L1_MANDATORY = true;
    public static final boolean INSURANCE_L2_VOLUNTARY = true;
    public static final BigDecimal INSURANCE_L1_PREMIUM = new BigDecimal("2.00");

    /** 金额统一精度：DECIMAL(10,2) */
    public static final int MONEY_SCALE = 2;
    public static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

    /** 四端 RESTful API 前缀 */
    public static final String API_USER = "/api/user/";
    public static final String API_WORKER = "/api/worker/";
    public static final String API_STORE = "/api/store/";
    public static final String API_ADMIN = "/api/admin/";

    /** 派单六项合取的阈值 */
    public static final BigDecimal MIN_DISPATCH_RATING = new BigDecimal("4.50");
    public static final int MAX_DAILY_CAPACITY = 8;

    public static BigDecimal priceOf(int level, String mode) {
        if (MODE_ONSITE.equals(mode) || MODE_DELIVERY.equals(mode)) {
            switch (level) {
                case LEVEL_APPEARANCE: return PRICE_ONSITE_APPEARANCE;
                case LEVEL_STANDARD: return PRICE_ONSITE_STANDARD;
                case LEVEL_DETAIL: return PRICE_ONSITE_DETAIL; // 口径未给定，返回 null 由调用方拦截
                default: return null;
            }
        }
        switch (level) {
            case LEVEL_APPEARANCE: return PRICE_STORE_APPEARANCE;
            case LEVEL_STANDARD: return PRICE_STORE_STANDARD;
            case LEVEL_DETAIL: return PRICE_STORE_DETAIL;
            default: return null;
        }
    }

    public static BigDecimal commissionRateOf(String mode) {
        return MODE_STORE.equals(mode) ? RATE_STORE : RATE_ONSITE;
    }
}
