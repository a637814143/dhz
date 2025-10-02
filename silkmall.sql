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

USE `silkmall`;

-- 默认多角色登录账号（密码均已通过 BCrypt 哈希处理）：
--   消费者：consumer_01 / Consumer@123
--   供应商：supplier_01 / Supplier@123
--   管理员：admin_01 / Admin@123
-- 运行本脚本或启动 Spring Boot 服务（会自动初始化同名账号）即可使用以上凭据登录。

-- 预置统一登录所需的核心账号
INSERT INTO `consumers` (
  `id`,
  `username`,
  `password`,
  `email`,
  `phone`,
  `address`,
  `created_at`,
  `updated_at`,
  `role`,
  `enabled`,
  `real_name`,
  `id_card`,
  `avatar`,
  `points`,
  `membership_level`
) VALUES (
  1001,
  'consumer_01',
  '$2b$12$7ypKSVf7k9z0HuiS2vhd..NY342XQ3dwKP5QPKgiLmxgVxbFiOLgq',
  'consumer01@silkmall.cn',
  '13800000001',
  '江苏省苏州市工业园区蚕桑大道88号',
  NOW(),
  NOW(),
  'consumer',
  1,
  '张小丝',
  '320500199001011234',
  NULL,
  1200,
  2
) ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `phone` = VALUES(`phone`),
  `address` = VALUES(`address`),
  `role` = VALUES(`role`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW(),
  `real_name` = VALUES(`real_name`),
  `id_card` = VALUES(`id_card`),
  `avatar` = VALUES(`avatar`),
  `points` = VALUES(`points`),
  `membership_level` = VALUES(`membership_level`);

INSERT INTO `suppliers` (
  `id`,
  `username`,
  `password`,
  `email`,
  `phone`,
  `address`,
  `created_at`,
  `updated_at`,
  `role`,
  `enabled`,
  `company_name`,
  `business_license`,
  `contact_person`,
  `join_date`,
  `supplier_level`,
  `status`
) VALUES (
  2001,
  'supplier_01',
  '$2b$12$OImu..TfxXwcajVW4VgVMeAsTY1.C.N/.oB6SI.FAPIad3b6/niOu',
  'supplier01@silkmall.cn',
  '13900000001',
  '浙江省杭州市蚕桑智谷66号',
  NOW(),
  NOW(),
  'supplier',
  1,
  '丝路供应链科技有限公司',
  '91330100MA2HXXXXXX',
  '李云',
  CURDATE(),
  'A级合作伙伴',
  'active'
) ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `phone` = VALUES(`phone`),
  `address` = VALUES(`address`),
  `role` = VALUES(`role`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW(),
  `company_name` = VALUES(`company_name`),
  `business_license` = VALUES(`business_license`),
  `contact_person` = VALUES(`contact_person`),
  `join_date` = VALUES(`join_date`),
  `supplier_level` = VALUES(`supplier_level`),
  `status` = VALUES(`status`);

INSERT INTO `admins` (
  `id`,
  `username`,
  `password`,
  `email`,
  `phone`,
  `address`,
  `created_at`,
  `updated_at`,
  `role`,
  `enabled`,
  `department`,
  `position`,
  `permissions`
) VALUES (
  3001,
  'admin_01',
  '$2b$12$DFM88bML5r2BL4mK6e0qwe8LQakyDChhdui28r/J24ZQ2/6VCcGKq',
  'admin01@silkmall.cn',
  '13700000001',
  '上海市浦东新区数字贸易中心28F',
  NOW(),
  NOW(),
  'admin',
  1,
  '平台运营中心',
  '系统管理员',
  'PRODUCT_MANAGE,ORDER_REVIEW,USER_AUDIT'
) ON DUPLICATE KEY UPDATE
  `password` = VALUES(`password`),
  `email` = VALUES(`email`),
  `phone` = VALUES(`phone`),
  `address` = VALUES(`address`),
  `role` = VALUES(`role`),
  `enabled` = VALUES(`enabled`),
  `updated_at` = NOW(),
  `department` = VALUES(`department`),
  `position` = VALUES(`position`),
  `permissions` = VALUES(`permissions`);

SET FOREIGN_KEY_CHECKS = 1;
