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

DROP TABLE IF EXISTS `return_requests`;
DROP TABLE IF EXISTS `product_reviews`;
DROP TABLE IF EXISTS `order_items`;
DROP TABLE IF EXISTS `orders`;
DROP TABLE IF EXISTS `product_images`;
DROP TABLE IF EXISTS `products`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `suppliers`;
DROP TABLE IF EXISTS `consumers`;
DROP TABLE IF EXISTS `admins`;

CREATE TABLE `admins` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `department` VARCHAR(100) DEFAULT NULL,
  `position` VARCHAR(100) DEFAULT NULL,
  `permissions` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `consumers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `real_name` VARCHAR(100) DEFAULT NULL,
  `id_card` VARCHAR(50) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT NULL,
  `points` INT DEFAULT 0,
  `membership_level` INT DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `suppliers` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `email` VARCHAR(150) DEFAULT NULL,
  `phone` VARCHAR(50) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `role` VARCHAR(50) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `company_name` VARCHAR(150) NOT NULL,
  `business_license` VARCHAR(100) DEFAULT NULL,
  `contact_person` VARCHAR(100) DEFAULT NULL,
  `join_date` DATETIME DEFAULT NULL,
  `supplier_level` VARCHAR(50) DEFAULT NULL,
  `status` VARCHAR(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `categories` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(255) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `icon` VARCHAR(255) DEFAULT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `parent_id` BIGINT DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_categories_parent` (`parent_id`),
  CONSTRAINT `fk_categories_parent` FOREIGN KEY (`parent_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `products` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(150) NOT NULL,
  `description` TEXT,
  `price` DECIMAL(10,2) NOT NULL,
  `stock` INT NOT NULL,
  `sales` INT NOT NULL DEFAULT 0,
  `main_image` VARCHAR(255) DEFAULT NULL,
  `status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `category_id` BIGINT NOT NULL,
  `supplier_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_products_category` (`category_id`),
  KEY `idx_products_supplier` (`supplier_id`),
  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`),
  CONSTRAINT `fk_products_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `suppliers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_images` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `image_url` VARCHAR(255) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_product_images_product` (`product_id`),
  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL,
  `total_amount` DECIMAL(10,2) NOT NULL,
  `total_quantity` INT NOT NULL,
  `status` VARCHAR(50) NOT NULL,
  `payment_method` VARCHAR(50) DEFAULT NULL,
  `shipping_address` VARCHAR(255) DEFAULT NULL,
  `recipient_name` VARCHAR(100) DEFAULT NULL,
  `recipient_phone` VARCHAR(50) DEFAULT NULL,
  `order_time` DATETIME NOT NULL,
  `payment_time` DATETIME DEFAULT NULL,
  `shipping_time` DATETIME DEFAULT NULL,
  `delivery_time` DATETIME DEFAULT NULL,
  `consumer_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_consumer` (`consumer_id`),
  CONSTRAINT `fk_orders_consumer` FOREIGN KEY (`consumer_id`) REFERENCES `consumers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `order_items` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `quantity` INT NOT NULL,
  `unit_price` DECIMAL(10,2) NOT NULL,
  `total_price` DECIMAL(10,2) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order` (`order_id`),
  KEY `idx_order_items_product` (`product_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `product_reviews` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rating` INT NOT NULL,
  `comment` VARCHAR(1000) DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `order_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `consumer_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_reviews_order_item` (`order_item_id`),
  KEY `idx_product_reviews_order` (`order_id`),
  KEY `idx_product_reviews_product` (`product_id`),
  KEY `idx_product_reviews_consumer` (`consumer_id`),
  CONSTRAINT `fk_product_reviews_consumer` FOREIGN KEY (`consumer_id`) REFERENCES `consumers` (`id`),
  CONSTRAINT `fk_product_reviews_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_product_reviews_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`),
  CONSTRAINT `fk_product_reviews_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `return_requests` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `status` VARCHAR(50) NOT NULL,
  `reason` VARCHAR(1000) DEFAULT NULL,
  `resolution` VARCHAR(1000) DEFAULT NULL,
  `requested_at` DATETIME NOT NULL,
  `processed_at` DATETIME DEFAULT NULL,
  `order_id` BIGINT NOT NULL,
  `order_item_id` BIGINT NOT NULL,
  `consumer_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_return_requests_order` (`order_id`),
  KEY `idx_return_requests_order_item` (`order_item_id`),
  KEY `idx_return_requests_consumer` (`consumer_id`),
  KEY `idx_return_requests_product` (`product_id`),
  CONSTRAINT `fk_return_requests_consumer` FOREIGN KEY (`consumer_id`) REFERENCES `consumers` (`id`),
  CONSTRAINT `fk_return_requests_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_return_requests_order_item` FOREIGN KEY (`order_item_id`) REFERENCES `order_items` (`id`),
  CONSTRAINT `fk_return_requests_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `admins` (`id`, `username`, `password`, `email`, `phone`, `address`, `created_at`, `updated_at`, `role`, `enabled`, `department`, `position`, `permissions`) VALUES
(1, 'admin_zhang', '$2a$10$examplehashedpassword1', 'admin.zhang@silkmall.com', '+86-21-55558888', '上海市黄浦区淮海中路300号', '2025-01-05 08:30:00', '2025-01-10 09:15:00', 'ROLE_ADMIN', 1, '运营中心', '平台管理员', 'USER_MANAGE,ORDER_MANAGE,PRODUCT_MANAGE');

INSERT INTO `consumers` (`id`, `username`, `password`, `email`, `phone`, `address`, `created_at`, `updated_at`, `role`, `enabled`, `real_name`, `id_card`, `avatar`, `points`, `membership_level`) VALUES
(1, 'lihua', '$2a$10$examplehashedpassword2', 'lihua@example.com', '+86-10-66668888', '北京市朝阳区望京街道星耀国际1号楼1903', '2025-01-03 10:00:00', '2025-01-12 14:20:00', 'ROLE_CONSUMER', 1, '李华', '110101199205087654', 'https://cdn.silkmall.com/avatars/lihua.png', 1280, 3),
(2, 'wangmei', '$2a$10$examplehashedpassword3', 'wangmei@example.com', '+86-20-77779999', '广州市天河区体育东路88号1205', '2025-01-06 11:45:00', '2025-01-14 16:05:00', 'ROLE_CONSUMER', 1, '王美', '440106199803123456', 'https://cdn.silkmall.com/avatars/wangmei.png', 860, 2);

INSERT INTO `suppliers` (`id`, `username`, `password`, `email`, `phone`, `address`, `created_at`, `updated_at`, `role`, `enabled`, `company_name`, `business_license`, `contact_person`, `join_date`, `supplier_level`, `status`) VALUES
(1, 'silk_master', '$2a$10$examplehashedpassword4', 'contact@silkmaster.cn', '+86-28-55557777', '四川省成都市武侯区人民南路99号', '2024-12-20 09:00:00', '2025-01-15 10:30:00', 'ROLE_SUPPLIER', 1, '蜀锦丝绸工坊', 'BL2023SC001', '刘敏', '2024-12-25 10:00:00', 'A级', 'APPROVED'),
(2, 'jiangnan_silk', '$2a$10$examplehashedpassword5', 'service@jiangnansilk.com', '+86-25-66669999', '江苏省苏州市姑苏区观前街128号', '2024-11-30 14:15:00', '2025-01-09 09:50:00', 'ROLE_SUPPLIER', 1, '江南丝绸织造厂', 'BL2023JS002', '沈波', '2024-12-05 15:20:00', 'A级', 'APPROVED');

INSERT INTO `categories` (`id`, `name`, `description`, `sort_order`, `icon`, `enabled`, `created_at`, `updated_at`, `parent_id`) VALUES
(1, '丝绸臻品', '丝绸原创精品分类', 1, 'https://cdn.silkmall.com/icons/category-premium.png', 1, '2025-01-01 09:00:00', '2025-01-10 08:30:00', NULL),
(2, '丝绸面料', '高端桑蚕丝面料', 2, 'https://cdn.silkmall.com/icons/category-fabric.png', 1, '2025-01-02 09:30:00', '2025-01-11 10:00:00', 1),
(3, '丝绸服饰', '成衣与围巾配饰', 3, 'https://cdn.silkmall.com/icons/category-fashion.png', 1, '2025-01-02 10:00:00', '2025-01-12 09:45:00', 1);

INSERT INTO `products` (`id`, `name`, `description`, `price`, `stock`, `sales`, `main_image`, `status`, `created_at`, `updated_at`, `category_id`, `supplier_id`) VALUES
(1, '手工桑蚕丝绸面料·云水谣', '精选四川高山桑蚕丝，纹理细腻，适合高级礼服与婚纱制作。', 199.80, 150, 46, 'https://cdn.silkmall.com/products/fabric-yunshuiyao/main.jpg', 'AVAILABLE', '2025-01-04 08:50:00', '2025-01-15 09:10:00', 2, 1),
(2, '江南手绘真丝披肩·烟雨', '苏州工匠纯手绘，丝滑亲肤，四色可选。', 200.00, 80, 32, 'https://cdn.silkmall.com/products/shawl-yanwu/main.jpg', 'AVAILABLE', '2025-01-05 09:10:00', '2025-01-14 17:40:00', 3, 2),
(3, '经典桑蚕丝真丝衬衫·雪白', '无缝拼接设计，透气轻盈，商务与休闲皆宜。', 318.00, 60, 24, 'https://cdn.silkmall.com/products/shirt-xuebai/main.jpg', 'AVAILABLE', '2025-01-07 10:20:00', '2025-01-16 11:05:00', 3, 1);

INSERT INTO `product_images` (`id`, `image_url`, `sort_order`, `created_at`, `product_id`) VALUES
(1, 'https://cdn.silkmall.com/products/fabric-yunshuiyao/detail1.jpg', 1, '2025-01-04 09:00:00', 1),
(2, 'https://cdn.silkmall.com/products/fabric-yunshuiyao/detail2.jpg', 2, '2025-01-04 09:05:00', 1),
(3, 'https://cdn.silkmall.com/products/shawl-yanwu/detail1.jpg', 1, '2025-01-05 09:30:00', 2),
(4, 'https://cdn.silkmall.com/products/shawl-yanwu/detail2.jpg', 2, '2025-01-05 09:35:00', 2),
(5, 'https://cdn.silkmall.com/products/shirt-xuebai/detail1.jpg', 1, '2025-01-07 10:35:00', 3),
(6, 'https://cdn.silkmall.com/products/shirt-xuebai/detail2.jpg', 2, '2025-01-07 10:40:00', 3);

INSERT INTO `orders` (`id`, `order_no`, `total_amount`, `total_quantity`, `status`, `payment_method`, `shipping_address`, `recipient_name`, `recipient_phone`, `order_time`, `payment_time`, `shipping_time`, `delivery_time`, `consumer_id`) VALUES
(1, 'SM20250101001', 599.60, 3, 'DELIVERED', 'WECHAT_PAY', '北京市朝阳区望京街道星耀国际1号楼1903', '李华', '+86-10-66668888', '2025-01-09 13:20:00', '2025-01-09 13:21:30', '2025-01-10 09:00:00', '2025-01-12 18:40:00', 1),
(2, 'SM20250105002', 318.00, 1, 'RETURN_REQUESTED', 'ALIPAY', '广州市天河区体育东路88号1205', '王美', '+86-20-77779999', '2025-01-13 15:45:00', '2025-01-13 15:46:10', '2025-01-14 10:20:00', NULL, 2);

INSERT INTO `order_items` (`id`, `quantity`, `unit_price`, `total_price`, `created_at`, `order_id`, `product_id`) VALUES
(1, 2, 199.80, 399.60, '2025-01-09 13:21:00', 1, 1),
(2, 1, 200.00, 200.00, '2025-01-09 13:21:05', 1, 2),
(3, 1, 318.00, 318.00, '2025-01-13 15:46:00', 2, 3);

INSERT INTO `product_reviews` (`id`, `rating`, `comment`, `created_at`, `order_id`, `order_item_id`, `product_id`, `consumer_id`) VALUES
(1, 5, '面料非常顺滑，颜色跟描述完全一致。', '2025-01-13 20:15:00', 1, 1, 1, 1),
(2, 4, '披肩图案很美，手感很好，就是希望再推出大号款。', '2025-01-14 09:10:00', 1, 2, 2, 1);

INSERT INTO `return_requests` (`id`, `status`, `reason`, `resolution`, `requested_at`, `processed_at`, `order_id`, `order_item_id`, `consumer_id`, `product_id`) VALUES
(1, 'PENDING', '尺码略大，想更换为小一码。', NULL, '2025-01-16 08:30:00', NULL, 2, 3, 2, 3);

SET FOREIGN_KEY_CHECKS = 1;
