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
    COMPLETED(80, "完成");

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
        for (int i = 0; i < values.length - 1; i++) {
            NEXT_CODE.put(values[i].code, values[i + 1].code);
        }
    }

    public static OrderStatusEnum ofCode(int code) {
        OrderStatusEnum s = BY_CODE.get(code);
        if (s == null) {
            throw new IllegalArgumentException("未知订单状态:" + code);
        }
        return s;
    }

    public static String descOf(int code) {
        return ofCode(code).getDesc();
    }

    /** 合法流转：只能按顺序推进到 next，或一次性前进多段（不可回退） */
    public static boolean canTransit(int from, int to) {
        Integer next = NEXT_CODE.get(from);
        return next != null && to >= next && to <= COMPLETED.code;
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
