package com.xiche.common.cache;

import com.xiche.common.constant.CacheConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级缓存的本地缓存层（修复 P0-13）。
 * 策略严格遵循 CacheConstants 的 A/B/C 三级：
 *  - C 级（资金/订单/产能/保单/结算/台账）：禁止缓存，get 永远回源，set 直接拒绝 —— fail-closed；
 *  - B 级：短 TTL（60s），写后应主动失效；
 *  - A 级：长 TTL（由域策略决定）。
 * V1 内部测试版以进程内缓存实现 L1（对标 Caffeine 角色）；后续可替换为 Redis Cluster（L2）
 * 而不改变调用方接口 —— 分层边界在此收敛。
 */
@Component
public class LocalCacheService {
    private static final Logger log = LoggerFactory.getLogger(LocalCacheService.class);

    private static class Entry {
        final Object value;
        final long expireAt;
        Entry(Object value, long ttlMs) {
            this.value = value;
            this.expireAt = ttlMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + ttlMs;
        }
        boolean alive() { return System.currentTimeMillis() < expireAt; }
    }

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Entry>> stores =
            new ConcurrentHashMap<String, ConcurrentHashMap<String, Entry>>();

    /** 读缓存；C 级或miss返回 null（调用方必须回源）。domain 为空按 A 处理。 */
    public Object get(String domain, String key) {
        CacheConstants.CacheLevel level = CacheConstants.levelOf(domain);
        if (level == CacheConstants.CacheLevel.C) {
            return null; // fail-closed：绝不从缓存读 C 级数据
        }
        ConcurrentHashMap<String, Entry> store = stores.get(domain);
        if (store == null) return null;
        Entry e = store.get(key);
        if (e == null) return null;
        if (!e.alive()) { store.remove(key); return null; }
        return e.value;
    }

    /** 写缓存；C 级拒绝写入。 */
    public void put(String domain, String key, Object value) {
        CacheConstants.CacheLevel level = CacheConstants.levelOf(domain);
        if (level == CacheConstants.CacheLevel.C) {
            log.warn("拒绝缓存 C 级数据 domain={} key={}（fail-closed）", domain, key);
            return;
        }
        long ttlMs = level.getTtlSeconds() * 1000L;
        ConcurrentHashMap<String, Entry> store = stores.computeIfAbsent(domain, k -> new ConcurrentHashMap<String, Entry>());
        store.put(key, new Entry(value, ttlMs));
    }

    /** 主动失效（B 级写后调用）。 */
    public void evict(String domain, String key) {
        ConcurrentHashMap<String, Entry> store = stores.get(domain);
        if (store != null) store.remove(key);
    }

    /** 清空某域。 */
    public void clear(String domain) { stores.remove(domain); }

    /** 仅供诊断：当前缓存快照（不含 C 级数据）。 */
    public Map<String, Integer> stats() {
        Map<String, Integer> m = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, ConcurrentHashMap<String, Entry>> e : stores.entrySet()) {
            m.put(e.getKey(), e.getValue().size());
        }
        return m;
    }
}
