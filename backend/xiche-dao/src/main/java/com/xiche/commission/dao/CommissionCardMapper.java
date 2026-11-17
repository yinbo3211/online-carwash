package com.xiche.commission.dao;
import com.xiche.commission.entity.CommissionCard;
import java.util.Date;
import java.util.List;
/** 免佣卡 DAO（修复 P0-12：card_snapshot 决定有效费率） */
public interface CommissionCardMapper {
    CommissionCard selectById(Long id);
    /** 查询洗车工当前有效的免佣卡 */
    List<CommissionCard> selectActiveByWorker(Long workerId, Date now);
    int insert(CommissionCard card);
}
