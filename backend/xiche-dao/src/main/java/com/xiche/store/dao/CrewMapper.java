package com.xiche.store.dao;
import com.xiche.store.entity.Crew;
import java.util.List;
/** 上门班组 DAO（修复 P1-14：Crew 维度产能） */
public interface CrewMapper {
    Crew selectById(Long id);
    List<Crew> selectByStore(Long storeId);
    /** 班组成员接单数（工时产能基础） */
    int countMembers(Long storeId);
    int insert(Crew crew);
}
