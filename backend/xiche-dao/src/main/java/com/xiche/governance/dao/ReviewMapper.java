package com.xiche.governance.dao;

import com.xiche.governance.entity.Review;
import java.util.List;

public interface ReviewMapper {
    int insert(Review review);
    List<Review> selectList(java.util.Map<String, Object> params);

    /** P1-09：按洗车工统计评价均分与条数，用于完单后重算评分 */
    java.util.Map<String, Object> statByWorker(@org.apache.ibatis.annotations.Param("workerId") Long workerId);
}
