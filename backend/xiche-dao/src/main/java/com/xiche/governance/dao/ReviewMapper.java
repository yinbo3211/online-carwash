package com.xiche.governance.dao;

import com.xiche.governance.entity.Review;
import java.util.List;

public interface ReviewMapper {
    int insert(Review review);
    List<Review> selectList(java.util.Map<String, Object> params);
}
