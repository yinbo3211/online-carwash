package com.xiche.order.dao;

import com.xiche.order.entity.Order;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface OrderMapper {
    Order selectById(@Param("id") Long id);

    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /** BUG-P0-06：按 (userId, requestId) 查幂等订单，归属天然限定，杜绝跨用户劫持 */
    Order selectByUserRequestId(@Param("userId") Long userId, @Param("requestId") String requestId);

    /** 分页列表：四端订单查询统一入口（C 级禁止缓存，直查库） */
    List<Order> selectList(Map<String, Object> params);

    int countList(Map<String, Object> params);

    int insert(Order order);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** P0-07：门店接单时记录占用工位，完工时据此释放 */
    int updateBayId(@Param("id") Long id, @Param("bayId") Long bayId);

    /** P0-10/P1-22：门店接单状态 CAS，仅 fromStatus 可推进到 toStatus */
    int updateStatusCas(@Param("id") Long id, @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    /** 派单成功：写入 workerId 并推进状态，同时校验旧状态防止并发重复接单 */
    int assignWorker(@Param("id") Long id, @Param("workerId") Long workerId,
                     @Param("fromStatus") Integer fromStatus, @Param("toStatus") Integer toStatus);

    /** 结算回写：恒等式计算出的全部分账字段 */
    int updateSettlement(Order order);
}
