package com.xiche.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 三级缓存口径（业务口径 6）：
 * A 可缓存（长 TTL，域名/字典等）；
 * B 短 TTL 强一致（评分、门店展示等）；
 * C 禁止缓存：资金 / 订单 / 产能 / 保单。
 *
 * V1 内部测试版不引入 Redis，本类作为缓存策略的唯一声明源，
 * 后续接入缓存组件时必须先查 levelOf() 再决定是否缓存。
 */
public final class CacheConstants {
    private CacheConstants() {
    }

    public enum CacheLevel {
        A(86400L, "可缓存"),
        B(60L, "短TTL强一致"),
        C(0L, "禁止缓存");

        private final long ttlSeconds;
        private final String desc;

        CacheLevel(long ttlSeconds, String desc) {
            this.ttlSeconds = ttlSeconds;
            this.desc = desc;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public String getDesc() {
            return desc;
        }
    }

    /** C 级业务域：任何情况下都不得进入缓存 */
    public static final Set<String> NO_CACHE_DOMAINS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(
                    "PAYMENT", "ORDER", "CAPACITY", "POLICY", "SETTLEMENT", "LEDGER")));

    /** B 级业务域：短 TTL + 写后主动失效 */
    public static final Set<String> SHORT_TTL_DOMAINS = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("WORKER_RATING", "STORE_PROFILE", "PRICE_VERSION")));

    public static CacheLevel levelOf(String domain) {
        if (domain == null) {
            return CacheLevel.A;
        }
        String key = domain.trim().toUpperCase();
        if (NO_CACHE_DOMAINS.contains(key)) {
            return CacheLevel.C;
        }
        if (SHORT_TTL_DOMAINS.contains(key)) {
            return CacheLevel.B;
        }
        return CacheLevel.A;
    }
}
