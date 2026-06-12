/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : dji_drone_system

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 29/04/2026 11:10:51
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for airspace_zones
-- ----------------------------
DROP TABLE IF EXISTS `airspace_zones`;
CREATE TABLE `airspace_zones`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '空域区域ID',
  `city` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市名称',
  `zone_type` enum('禁飞区','限飞区','适飞区') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '空域类型',
  `locations` json NULL COMMENT '具体地点列表（JSON数组）',
  `update_time` date NULL DEFAULT NULL COMMENT '数据更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '城市适飞空域配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for airworthiness
-- ----------------------------
DROP TABLE IF EXISTS `airworthiness`;
CREATE TABLE `airworthiness`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '适航记录ID',
  `device_id` int NOT NULL COMMENT '关联设备ID',
  `cert_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适航证编号',
  `issue_date` date NULL DEFAULT NULL COMMENT '发证日期',
  `expire_date` date NULL DEFAULT NULL COMMENT '有效期截止日期',
  `status` enum('有效','即将到期','待审核','已过期') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审核' COMMENT '适航证状态',
  `cert_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '适航证书图片路径',
  `apply_status` enum('待审核','已通过','已驳回') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审核' COMMENT '申请审核状态',
  `apply_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE,
  CONSTRAINT `airworthiness_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '无人机适航管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for device_models
-- ----------------------------
DROP TABLE IF EXISTS `device_models`;
CREATE TABLE `device_models`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '设备型号ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '型号名称（如：Matrice 300 RTK）',
  `weight` decimal(5, 2) NULL DEFAULT NULL COMMENT '设备重量（kg）',
  `max_height` int NULL DEFAULT NULL COMMENT '最大飞行高度（米）',
  `max_speed` int NULL DEFAULT NULL COMMENT '最大飞行速度（km/h）',
  `max_distance` int NULL DEFAULT NULL COMMENT '最大飞行距离（km）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '无人机设备型号参数表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for devices
-- ----------------------------
DROP TABLE IF EXISTS `devices`;
CREATE TABLE `devices`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '设备编号（如：DJI-M300-001）',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '设备名称',
  `model_id` int NULL DEFAULT NULL COMMENT '关联设备型号ID',
  `owner_id` int NULL DEFAULT NULL COMMENT '设备所属人ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `purchase_date` date NULL DEFAULT NULL COMMENT '购买日期',
  `created_at` datetime(6) NULL DEFAULT NULL,
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `airworthiness_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '有效',
  `weight` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `max_height` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `device_no`(`device_no` ASC) USING BTREE,
  INDEX `model_id`(`model_id` ASC) USING BTREE,
  INDEX `owner_id`(`owner_id` ASC) USING BTREE,
  CONSTRAINT `devices_ibfk_1` FOREIGN KEY (`model_id`) REFERENCES `device_models` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `devices_ibfk_2` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '无人机设备实例表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flight_applications
-- ----------------------------
DROP TABLE IF EXISTS `flight_applications`;
CREATE TABLE `flight_applications`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '飞行申请ID',
  `user_id` int NOT NULL COMMENT '申请人ID',
  `device_id` int NOT NULL COMMENT '使用的设备ID',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '飞行地点',
  `takeoff_time` datetime NOT NULL COMMENT '计划起飞时间',
  `landing_time` datetime NOT NULL COMMENT '计划降落时间',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '飞行任务说明与安全说明',
  `status` enum('待审核','已通过','已驳回','进行中','已完成','已取消') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审核' COMMENT '审批状态',
  `audit_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE,
  CONSTRAINT `flight_applications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `flight_applications_ibfk_2` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '飞行活动申请表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for flight_records
-- ----------------------------
DROP TABLE IF EXISTS `flight_records`;
CREATE TABLE `flight_records`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '飞行记录ID',
  `device_id` int NOT NULL COMMENT '设备ID',
  `pilot_id` int NOT NULL COMMENT '飞手ID',
  `location` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实际飞行地点',
  `takeoff_time` datetime NOT NULL COMMENT '实际起飞时间',
  `landing_time` datetime NOT NULL COMMENT '实际降落时间',
  `duration` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '飞行时长（如：1小时30分钟）',
  `status` enum('已完成','进行中','已取消') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '已完成' COMMENT '飞行任务状态',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `device_id`(`device_id` ASC) USING BTREE,
  INDEX `pilot_id`(`pilot_id` ASC) USING BTREE,
  CONSTRAINT `flight_records_ibfk_1` FOREIGN KEY (`device_id`) REFERENCES `devices` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `flight_records_ibfk_2` FOREIGN KEY (`pilot_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '历史飞行记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for messages
-- ----------------------------
DROP TABLE IF EXISTS `messages`;
CREATE TABLE `messages`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `receiver_id` int NOT NULL COMMENT '接收人ID',
  `sender` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '系统' COMMENT '发送人（系统或管理员）',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `type` enum('系统','审批') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '系统' COMMENT '消息类型',
  `status` enum('未读','已读') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '未读' COMMENT '阅读状态',
  `send_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `receiver_id`(`receiver_id` ASC) USING BTREE,
  CONSTRAINT `messages_ibfk_1` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统消息中心表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for person_qualification
-- ----------------------------
DROP TABLE IF EXISTS `person_qualification`;
CREATE TABLE `person_qualification`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '资质记录ID',
  `user_id` int NOT NULL COMMENT '关联用户ID',
  `current_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '当前资质等级',
  `apply_level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '申请/目标资质等级',
  `status` enum('待审核','已通过','已驳回') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审核' COMMENT '申请状态',
  `materials` json NULL COMMENT '佐证材料（JSON数组，存储图片路径）',
  `apply_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `audit_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `person_qualification_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '飞手人员资质管理表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for real_name_auth
-- ----------------------------
DROP TABLE IF EXISTS `real_name_auth`;
CREATE TABLE `real_name_auth`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '实名认证记录ID',
  `user_id` int NOT NULL COMMENT '关联用户ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号码',
  `id_card_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证照片路径',
  `submit_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `status` enum('待审核','已通过','已驳回') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '待审核' COMMENT '审核状态',
  `audit_remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '审核意见',
  `audit_time` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `real_name_auth_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户实名认证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for schedules
-- ----------------------------
DROP TABLE IF EXISTS `schedules`;
CREATE TABLE `schedules`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `user_id` int NOT NULL COMMENT '所属用户ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日程标题',
  `type` enum('飞行任务','设备维护','会议','培训') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '日程类型',
  `schedule_time` datetime NOT NULL COMMENT '日程执行时间',
  `remind_type` enum('短信','邮件','站内信') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提醒方式',
  `remind_time` enum('提前1小时','提前3小时','提前1天','提前3天') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '提醒时间',
  `repeat_remind` enum('不重复','每天','每周','每月') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '重复提醒规则',
  `urgency` enum('低','中','高') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '紧急程度',
  `remark` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '日程备注说明',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `schedules_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户日程提醒表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名（登录账号）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（加密存储）',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `role` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `register_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `real_name_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '未认证',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
