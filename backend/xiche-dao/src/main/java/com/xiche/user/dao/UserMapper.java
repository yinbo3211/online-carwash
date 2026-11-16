package com.xiche.user.dao;

import com.xiche.user.entity.User;

import java.util.List;
import java.util.Map;

public interface UserMapper {
    User selectById(Long id);

    User selectByPhone(String phone);

    List<User> selectList(Map<String, Object> params);

    int countList(Map<String, Object> params);

    int insert(User user);

    int updateById(User user);
}
