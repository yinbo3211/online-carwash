package com.xiche.worker.dao;

import com.xiche.worker.entity.Certification;
import com.xiche.worker.entity.Worker;

import java.util.List;
import java.util.Map;

public interface WorkerMapper {
    Worker selectById(Long id);

    List<Worker> selectByIds(List<Long> ids);

    List<Certification> selectCerts(Long workerId);

    List<Worker> selectList(Map<String, Object> params);

    int insert(Worker worker);

    /** 接单后原子递增今日产能；产能属于 C 级禁止缓存数据 */
    int incrementTodayOrders(Long workerId);

    /** P0-07：跨日首次接单前原子重置今日产能（updated_at 非今日则归零） */
    int resetTodayOrdersIfStale(Long workerId);
}
