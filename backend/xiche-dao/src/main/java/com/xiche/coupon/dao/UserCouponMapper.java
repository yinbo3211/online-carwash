package com.xiche.coupon.dao;
import com.xiche.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Param;
import java.util.List;
/** 用户优惠券 DAO（修复 P0-12） */
public interface UserCouponMapper {
    UserCoupon selectById(@Param("id") Long id);
    List<UserCoupon> selectByUser(@Param("userId") Long userId);
    /** 核销：CAS 状态 UNUSED->USED，防止重复使用 */
    int consume(@Param("id") Long id, @Param("orderId") Long orderId);
    int insert(UserCoupon uc);
}
