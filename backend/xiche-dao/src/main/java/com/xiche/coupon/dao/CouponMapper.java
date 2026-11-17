package com.xiche.coupon.dao;
import com.xiche.coupon.entity.Coupon;
import java.util.List;
/** 优惠券定义 DAO（修复 P0-12） */
public interface CouponMapper {
    Coupon selectById(Long id);
    Coupon selectByCode(String code);
    List<Coupon> selectAvailable(java.util.Date now);
    int insert(Coupon coupon);
    int incrementUsed(Long id);
}
