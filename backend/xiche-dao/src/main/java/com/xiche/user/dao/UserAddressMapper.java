package com.xiche.user.dao;
import com.xiche.user.entity.UserAddress;
import java.util.List;
/** 上门地址 DAO（修复 P1-20：ONSITE 下单地址能力） */
public interface UserAddressMapper {
    UserAddress selectById(Long id);
    List<UserAddress> selectByUser(Long userId);
    UserAddress selectDefault(Long userId);
    int insert(UserAddress address);
}
