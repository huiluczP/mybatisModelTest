package com.mybatis.test.mapper;

import com.mybatis.test.entity.ProductInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品信息 MyBatis Mapper 接口。
 */
public interface ProductInfoMapper {

    /**
     * 插入记录，忽略 null 字段。
     */
    int insertIgnoreNull(ProductInfo record);

    /**
     * 按主键更新记录，忽略 null 字段。
     */
    int updateIgnoreNull(ProductInfo record);

    /**
     * 按主键查询。
     */
    ProductInfo selectById(@Param("id") Long id);

    /**
     * 按商品编码查询。
     */
    ProductInfo selectByProductCode(@Param("productCode") String productCode);

    /**
     * 查询所有记录。
     */
    List<ProductInfo> selectAll();

    /**
     * 按主键删除（软删除）。
     */
    int deleteById(@Param("id") Long id);

    /**
     * 批量插入，忽略 null 字段。
     */
    int batchInsertIgnoreNull(@Param("list") List<ProductInfo> list);

    /**
     * 按条件计数。
     */
    long countByCondition(ProductInfo condition);

    /**
     * 按主键和商品编码查询。
     */
    ProductInfo selectByIdAndProductCode(@Param("id") Long id, @Param("productCode") String productCode);
}
