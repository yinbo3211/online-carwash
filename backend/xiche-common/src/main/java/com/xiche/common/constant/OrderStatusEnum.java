package com.xiche.common.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单状态机（业务口径 6，不得改动）：
 * 待支付 -> 待接单 -> 已接单 -> 取车中 -> 洗车中 -> 配送中 -> 待验收 -> 完成。
 * 取车中/配送中仅取送车与上门单经过；可跳段但不可回退（取消走独立逆向单）。
 */
public enum OrderStatusEnum {
    PENDING_PAYMENT(10, "待支付"),
    WAITING_ACCEPT(20, "待接单"),
    ACCEPTED(30, "已接单"),
    PICKING_UP(40, "取车中"),
    WASHING(50, "洗车中"),
    DELIVERING(60, "配送中"),
    WAITING_INSPECTION(70, "待验收"),
    COMPLETED(80, "完成"),
    /* P1-17：逆向/异常状态，取消不再用裸值 90 */
    CANCELLED(90, "已取消"),
    REFUNDING(91, "退款中"),
    REFUNDED(92, "已退款"),
    EXCEPTION(93, "异常待处理"),
    DISPUTE(94, "争议处理中");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    private static final Map<Integer, OrderStatusEnum> BY_CODE = new HashMap<Integer, OrderStatusEnum>();
    private static final Map<Integer, Integer> NEXT_CODE = new HashMap<Integer, Integer>();

    static {
        OrderStatusEnum[] values = values();
        for (OrderStatusEnum s : values) {
            BY_CODE.put(s.code, s);
        }
        // 只链接主链 10..80，逆向/异常状态不进入正向流转链
        for (int i = 0; i < values.length - 1; i++) {
            if (values[i].code >= PENDING_PAYMENT.code && values[i].code < COMPLETED.code
                    && values[i + 1].code == values[i].code + 10) {
                NEXT_CODE.put(values[i].code, values[i + 1].code);
            }
        }
    }

    public static OrderStatusEnum ofCode(int code) {
        OrderStatusEnum s = BY_CODE.get(code);
        if (s == null) {
            throw new IllegalArgumentException("未知订单状态:" + code);
        }
        return s;
    }

    /** P1-17：未知码安全降级为"未知状态"，不再抛异常 */
    public static String descOf(int code) {
        OrderStatusEnum s = BY_CODE.get(code);
        return s == null ? "未知状态(" + code + ")" : s.getDesc();
    }

    /** 合法流转：只能按顺序推进到 next，或一次性前进多段（不可回退） */
    public static boolean canTransit(int from, int to) {
        Integer next = NEXT_CODE.get(from);
        return next != null && to >= next && to <= COMPLETED.code;
    }

    /** P0-09/P1-17：仅待支付、待接单的订单允许取消；已完成/退款/争议不可取消 */
    public static boolean canCancel(int from) {
        return from == PENDING_PAYMENT.code || from == WAITING_ACCEPT.code;
    }

    public static void assertTransit(int from, int to) {
        if (!canTransit(from, to)) {
            throw new IllegalStateException("非法订单状态流转:" + descOf(from) + " -> " + descOf(to));
        }
    }

    public static Map<Integer, String> allDescs() {
        Map<Integer, String> m = new HashMap<Integer, String>();
        for (OrderStatusEnum s : values()) {
            m.put(s.code, s.getDesc());
        }
        return Collections.unmodifiableMap(m);
    }
}
