package com.mybatis.test.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息实体，对应 product_info 表。
 * 所有字段使用驼峰命名。
 */
public class ProductInfo {

    private Long id;
    private String productName;
    private String productCode;
    private Long categoryId;
    private String brandName;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Integer stockQuantity;
    private BigDecimal weight;
    private Boolean isOnSale;
    private Boolean isDeleted;
    private String description;
    private String specification;
    private Integer shelfLifeDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductInfo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public Boolean getIsOnSale() { return isOnSale; }
    public void setIsOnSale(Boolean isOnSale) { this.isOnSale = isOnSale; }

    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public Integer getShelfLifeDays() { return shelfLifeDays; }
    public void setShelfLifeDays(Integer shelfLifeDays) { this.shelfLifeDays = shelfLifeDays; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "ProductInfo{" +
                "id=" + id +
                ", productName='" + productName + '\'' +
                ", productCode='" + productCode + '\'' +
                ", categoryId=" + categoryId +
                ", brandName='" + brandName + '\'' +
                ", price=" + price +
                ", costPrice=" + costPrice +
                ", stockQuantity=" + stockQuantity +
                ", weight=" + weight +
                ", isOnSale=" + isOnSale +
                ", isDeleted=" + isDeleted +
                ", description='" + (description != null && description.length() > 50 ? description.substring(0, 50) + "..." : description) + '\'' +
                ", specification='" + specification + '\'' +
                ", shelfLifeDays=" + shelfLifeDays +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
