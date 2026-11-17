package com.xiche.catalog.dao;

import com.xiche.catalog.entity.ServiceSku;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface ServiceSkuMapper {
    ServiceSku selectById(Long id);

    List<ServiceSku> selectByCategory(Long categoryId);

    /** 按服务模式与等级查在售 SKU（价格版本 B 级短 TTL，SKU 本身走 A 级） */
    ServiceSku selectByLevelAndMode(@Param("serviceLevel") Integer serviceLevel, @Param("serviceMode") String serviceMode);

    List<ServiceSku> selectList(Map<String, Object> params);

    int insert(ServiceSku sku);
}
