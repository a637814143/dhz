/*
 Navicat Premium Dump SQL

 Source Server         : bysj
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : silkmall

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 18/09/2025 14:49:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `product_size_allocations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `size_label` varchar(20) NOT NULL,
  `quantity` int NOT NULL,
  `product_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_size_label` (`product_id`,`size_label`),
  CONSTRAINT `fk_product_size_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `order_items`
    ADD COLUMN IF NOT EXISTS `size_label` varchar(20) NULL AFTER `total_price`;

CREATE TABLE IF NOT EXISTS `consumer_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `consumer_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consumer_product` (`consumer_id`,`product_id`),
  CONSTRAINT `fk_favorite_consumer` FOREIGN KEY (`consumer_id`) REFERENCES `consumers` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_favorite_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;
