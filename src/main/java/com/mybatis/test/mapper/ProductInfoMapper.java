package com.mybatis.test.mapper;

import com.mybatis.test.entity.ProductInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ProductInfo MyBatis Mapper interface.
 */
public interface ProductInfoMapper {

    /**
     * Insert a record, ignoring null fields.
     */
    int insertIgnoreNull(ProductInfo record);

    /**
     * Update a record by primary key, ignoring null fields.
     */
    int updateIgnoreNull(ProductInfo record);

    /**
     * Select by primary key.
     */
    ProductInfo selectById(@Param("id") Long id);

    /**
     * Select by product code.
     */
    ProductInfo selectByProductCode(@Param("productCode") String productCode);

    /**
     * Select all records.
     */
    List<ProductInfo> selectAll();

    /**
     * Delete by primary key (soft delete).
     */
    int deleteById(@Param("id") Long id);

    /**
     * Batch insert, ignoring null fields.
     */
    int batchInsertIgnoreNull(@Param("list") List<ProductInfo> list);

    /**
     * Count records by condition.
     */
    long countByCondition(ProductInfo condition);
}
