-- product_info table
CREATE DATABASE IF NOT EXISTS mybatis_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE test;

DROP TABLE IF EXISTS `product_info`;
CREATE TABLE `product_info` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
    `product_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `brand_name` VARCHAR(100) DEFAULT NULL COMMENT '品牌名称',
    `price` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    `cost_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '成本价',
    `stock_quantity` INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    `weight` DECIMAL(8, 2) DEFAULT NULL COMMENT '重量(kg)',
    `is_on_sale` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否上架: 0-下架, 1-上架',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除, 1-已删除',
    `description` TEXT DEFAULT NULL COMMENT '商品描述',
    `specification` VARCHAR(500) DEFAULT NULL COMMENT '规格参数',
    `shelf_life_days` INT DEFAULT NULL COMMENT '保质期(天)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_category` (`category_id`),
    KEY `idx_on_sale` (`is_on_sale`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息表';
