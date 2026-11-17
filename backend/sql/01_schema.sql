-- =============================================================
-- 线上洗车平台 v6.0 · 建表脚本（MySQL 8 / utf8mb4）
-- TEST-ENV-V1 内部测试版：所有金额 DECIMAL(10,2)；统一软删除 deleted_at
-- 表按 14 业务域分组，字段与 xiche-pojo 实体一一对应
-- =============================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- user 车主域 ----------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid（本期预留）',
  `union_id` VARCHAR(64) DEFAULT NULL,
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号，注册必填唯一',
  `nickname` VARCHAR(64) DEFAULT NULL,
  `avatar_url` VARCHAR(255) DEFAULT NULL,
  `real_name` VARCHAR(64) DEFAULT NULL,
  `id_card_no` VARCHAR(32) DEFAULT NULL COMMENT '实名认证（车主可选）',
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车主用户';

DROP TABLE IF EXISTS `t_user_address`;
CREATE TABLE `t_user_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `receiver_name` VARCHAR(64) NOT NULL,
  `receiver_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(64) DEFAULT NULL, `city` VARCHAR(64) DEFAULT NULL, `district` VARCHAR(64) DEFAULT NULL,
  `detail` VARCHAR(255) NOT NULL,
  `latitude` DECIMAL(10,6) DEFAULT NULL, `longitude` DECIMAL(10,6) DEFAULT NULL,
  `is_default` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车主收货/取车地址';

-- ---------- worker 洗车工域 ----------
DROP TABLE IF EXISTS `t_worker`;
CREATE TABLE `t_worker` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `real_name` VARCHAR(64) NOT NULL COMMENT '实名',
  `phone` VARCHAR(20) NOT NULL,
  `id_card_no` VARCHAR(32) NOT NULL COMMENT '实名认证',
  `city` VARCHAR(64) DEFAULT NULL,
  `rating` DECIMAL(3,2) DEFAULT 5.00,
  `completed_orders` INT DEFAULT 0,
  `daily_capacity` INT DEFAULT 8 COMMENT '当日累计接单上限（工时驱动）',
  `today_orders` INT DEFAULT 0,
  `banned` INT DEFAULT 0 COMMENT '是否封号：封号须人工复核+申诉',
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洗车工';

DROP TABLE IF EXISTS `t_worker_license`;
CREATE TABLE `t_worker_license` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `worker_id` BIGINT NOT NULL, `license_no` VARCHAR(64) NOT NULL COMMENT '驾驶证号',
  `license_type` VARCHAR(32) DEFAULT NULL COMMENT '准驾车型', `valid_until` DATE DEFAULT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洗车工驾驶证（取送车必备）';

DROP TABLE IF EXISTS `t_certification`;
CREATE TABLE `t_certification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `worker_id` BIGINT NOT NULL, `cert_code` VARCHAR(16) NOT NULL COMMENT 'C1/C2/C3/D/O',
  `exam_score` INT DEFAULT NULL, `status` VARCHAR(16) DEFAULT 'LOCKED' COMMENT 'LOCKED/PASSED/EXPIRED',
  `passed_at` DATETIME DEFAULT NULL, `expire_at` DATETIME DEFAULT NULL COMMENT '证书到期时间(12个月)',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_worker_cert` (`worker_id`,`cert_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洗车工认证等级（未通过不可接对应单）';

-- ---------- store 门店域 ----------
DROP TABLE IF EXISTS `t_store`;
CREATE TABLE `t_store` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `store_name` VARCHAR(128) NOT NULL COMMENT '合作门店（界面中性命名，禁止用品牌商标）',
  `owner_user_id` BIGINT DEFAULT NULL, `phone` VARCHAR(20) DEFAULT NULL,
  `province` VARCHAR(64) DEFAULT NULL, `city` VARCHAR(64) DEFAULT NULL, `district` VARCHAR(64) DEFAULT NULL,
  `address` VARCHAR(255) DEFAULT NULL,
  `bay_count` INT DEFAULT 0, `bay_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '工位费M（不抽成）',
  `rating` DECIMAL(3,2) DEFAULT 5.00, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合作门店';

DROP TABLE IF EXISTS `t_store_bay`;
CREATE TABLE `t_store_bay` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL, `bay_no` VARCHAR(32) NOT NULL,
  `status` INT DEFAULT 0 COMMENT '工位三态：0有空位free/1已满full/2暂停paused',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店工位（Bay维度产能）';

DROP TABLE IF EXISTS `t_crew`;
CREATE TABLE `t_crew` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL, `worker_id` BIGINT DEFAULT NULL, `role` VARCHAR(32) DEFAULT NULL,
  `joined_at` DATETIME DEFAULT NULL, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店上门班组（Crew维度产能）';

-- ---------- catalog 服务品类与定价域 ----------
DROP TABLE IF EXISTS `t_service_category`;
CREATE TABLE `t_service_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_code` VARCHAR(32) NOT NULL, `category_name` VARCHAR(64) NOT NULL,
  `service_level` INT DEFAULT NULL COMMENT '1外观/2标准/3精细', `sort_no` INT DEFAULT 0, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务品类';

DROP TABLE IF EXISTS `t_service_sku`;
CREATE TABLE `t_service_sku` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL, `sku_code` VARCHAR(64) NOT NULL, `sku_name` VARCHAR(128) NOT NULL,
  `service_level` INT DEFAULT NULL, `service_mode` VARCHAR(16) DEFAULT 'STORE' COMMENT 'STORE/ONSITE',
  `price` DECIMAL(10,2) NOT NULL, `price_remark` VARCHAR(255) DEFAULT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_sku` (`sku_code`,`service_mode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务SKU（套餐+增值项）';

DROP TABLE IF EXISTS `t_price_version`;
CREATE TABLE `t_price_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_code` VARCHAR(32) NOT NULL, `city` VARCHAR(64) DEFAULT NULL, `sku_id` BIGINT DEFAULT NULL,
  `price` DECIMAL(10,2) DEFAULT NULL, `effective_from` DATETIME DEFAULT NULL, `effective_to` DATETIME DEFAULT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格版本（订单固化price_snapshot，禁止追溯改价）';

-- ---------- order 订单域 ----------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT NOT NULL, `store_id` BIGINT DEFAULT NULL, `worker_id` BIGINT DEFAULT NULL, `service_sku_id` BIGINT DEFAULT NULL,
  `service_level` INT DEFAULT NULL, `service_mode` VARCHAR(16) DEFAULT 'STORE', `fulfill_mode` VARCHAR(16) DEFAULT NULL,
  `status` INT DEFAULT 10 COMMENT '状态机:10待支付 20待接单 30已接单 40取车中 50洗车中 60配送中 70待验收 80完成 90取消 91退款中 92已退款 93异常 94争议',
  `paid_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'P用户实付',
  `platform_subsidy` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'C平台补贴',
  `store_bay_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'M工位费（不抽成）',
  `insurance_fee` DECIMAL(10,2) DEFAULT 0.00 COMMENT '保险费（不抽成）',
  `advance_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '代垫款（不抽成）',
  `labor_income` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'L劳务收入=唯一抽成基数',
  `pass_through_cost` DECIMAL(10,2) DEFAULT 0.00 COMMENT 'B过账成本=保险费(代垫款D拆出单列)',
  `platform_fee` DECIMAL(10,2) DEFAULT 0.00, `worker_net` DECIMAL(10,2) DEFAULT 0.00, `store_net` DECIMAL(10,2) DEFAULT 0.00,
  `rating` DECIMAL(3,2) DEFAULT NULL, `env_flag` VARCHAR(32) DEFAULT 'TEST-ENV-V1', `remark` VARCHAR(255) DEFAULT NULL,
  `price_snapshot` VARCHAR(512) DEFAULT NULL COMMENT '价格快照',
  `rate_snapshot` VARCHAR(64) DEFAULT NULL COMMENT '费率快照',
  `card_snapshot` VARCHAR(128) DEFAULT NULL COMMENT '券与免佣卡快照',
  `address_id` BIGINT DEFAULT NULL COMMENT '上门地址ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_order_no` (`order_no`), KEY `idx_user` (`user_id`), KEY `idx_worker` (`worker_id`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单主表（C级禁止缓存）';

DROP TABLE IF EXISTS `t_order_item`;
CREATE TABLE `t_order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL, `sku_id` BIGINT DEFAULT NULL, `item_name` VARCHAR(128) DEFAULT NULL,
  `quantity` INT DEFAULT 1, `unit_price` DECIMAL(10,2) DEFAULT 0.00, `amount` DECIMAL(10,2) DEFAULT 0.00,
  `item_type` VARCHAR(16) DEFAULT 'PACKAGE' COMMENT 'PACKAGE套餐/ADDON增值',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单服务项明细（price_snapshot逐项固化）';

DROP TABLE IF EXISTS `t_order_evidence`;
CREATE TABLE `t_order_evidence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL, `stage` VARCHAR(8) NOT NULL COMMENT 'E1取车/E2到店/E3离店/E4还车',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '带时间/GPS水印、禁相册',
  `operator_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='取还车四组证据（缺一禁止推进下一段）';

-- ---------- payment 支付域 ----------
DROP TABLE IF EXISTS `t_payment_order`;
CREATE TABLE `t_payment_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(64) NOT NULL, `order_id` BIGINT NOT NULL, `order_no` VARCHAR(64) DEFAULT NULL, `user_id` BIGINT DEFAULT NULL,
  `amount` DECIMAL(10,2) DEFAULT 0.00,
  `gateway` VARCHAR(32) DEFAULT 'MOCK' COMMENT 'V1仅MOCK；预留ALIPAY/WECHAT',
  `status` VARCHAR(16) DEFAULT 'INIT', `success_message` VARCHAR(128) DEFAULT NULL, `success_color` VARCHAR(16) DEFAULT NULL COMMENT '模拟成功=橙色#F5901B',
  `env_flag` VARCHAR(32) DEFAULT 'TEST-ENV-V1', `idempotent_key` VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
  `paid_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_payment_no` (`payment_no`), UNIQUE KEY `uk_idem` (`idempotent_key`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单（V1模拟支付，不产生真实资金）';

-- ---------- insurance 保险域 ----------
DROP TABLE IF EXISTS `t_insurance_product`;
CREATE TABLE `t_insurance_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_code` VARCHAR(64) NOT NULL, `product_name` VARCHAR(128) NOT NULL, `level` VARCHAR(8) DEFAULT 'L1',
  `premium` DECIMAL(10,2) DEFAULT 0.00, `coverage_amount` DECIMAL(12,2) DEFAULT 0.00,
  `mandatory` INT DEFAULT 0 COMMENT 'L1每单强制=1', `status` INT DEFAULT 1, `remark` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险产品（L1强制/L2加购）';

DROP TABLE IF EXISTS `t_policy`;
CREATE TABLE `t_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `policy_no` VARCHAR(64) NOT NULL, `order_id` BIGINT NOT NULL, `product_id` BIGINT DEFAULT NULL,
  `level` VARCHAR(8) DEFAULT 'L1', `premium` DECIMAL(10,2) DEFAULT 0.00,
  `status` VARCHAR(16) DEFAULT 'SIMULATED' COMMENT 'V1模拟记录，真实出单须持牌合作',
  `effective_from` DATETIME DEFAULT NULL, `effective_to` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保单（每单必须有；V1模拟台账）';

-- ---------- settlement 结算分账域 ----------
DROP TABLE IF EXISTS `t_settlement`;
CREATE TABLE `t_settlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `settlement_no` VARCHAR(64) NOT NULL, `order_id` BIGINT NOT NULL, `order_no` VARCHAR(64) DEFAULT NULL,
  `service_mode` VARCHAR(16) DEFAULT 'STORE',
  `paid_amount` DECIMAL(10,2) DEFAULT 0.00, `platform_subsidy` DECIMAL(10,2) DEFAULT 0.00,
  `store_bay_fee` DECIMAL(10,2) DEFAULT 0.00, `pass_through_cost` DECIMAL(10,2) DEFAULT 0.00,
  `advance_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '代垫款独立科目',
  `labor_income` DECIMAL(10,2) DEFAULT 0.00, `commission_rate` DECIMAL(5,4) DEFAULT 0.4000,
  `platform_fee` DECIMAL(10,2) DEFAULT 0.00, `worker_net` DECIMAL(10,2) DEFAULT 0.00, `store_net` DECIMAL(10,2) DEFAULT 0.00,
  `total` DECIMAL(10,2) DEFAULT 0.00, `status` VARCHAR(16) DEFAULT 'SETTLED', `env_flag` VARCHAR(32) DEFAULT 'TEST-ENV-V1',
  `settled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_settlement_no` (`settlement_no`), UNIQUE KEY `uk_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='三方分账结算单（恒等式校验）';

DROP TABLE IF EXISTS `t_ledger`;
CREATE TABLE `t_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `ledger_no` VARCHAR(64) NOT NULL, `settlement_id` BIGINT DEFAULT NULL,
  `account_type` VARCHAR(32) DEFAULT NULL COMMENT 'PLATFORM/WORKER/STORE', `account_id` BIGINT DEFAULT NULL,
  `subject` VARCHAR(128) DEFAULT NULL, `direction` VARCHAR(8) DEFAULT 'CREDIT',
  `amount` DECIMAL(10,2) DEFAULT 0.00, `env_flag` VARCHAR(32) DEFAULT 'TEST-ENV-V1',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_settlement` (`settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资金台账（模拟）';

-- ---------- coupon / commission 营销域 ----------
DROP TABLE IF EXISTS `t_coupon`;
CREATE TABLE `t_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `coupon_code` VARCHAR(64) NOT NULL, `coupon_name` VARCHAR(128) NOT NULL,
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00, `min_order_amount` DECIMAL(10,2) DEFAULT 0.00,
  `borne_by` VARCHAR(16) DEFAULT 'PLATFORM' COMMENT '券成本平台承担，不减洗车工结算',
  `total_count` INT DEFAULT 0, `used_count` INT DEFAULT 0,
  `valid_from` DATETIME DEFAULT NULL, `valid_to` DATETIME DEFAULT NULL, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券定义';

DROP TABLE IF EXISTS `t_user_coupon`;
CREATE TABLE `t_user_coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL, `coupon_id` BIGINT NOT NULL, `status` VARCHAR(16) DEFAULT 'UNUSED',
  `order_id` BIGINT DEFAULT NULL, `used_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

DROP TABLE IF EXISTS `t_commission_card`;
CREATE TABLE `t_commission_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `worker_id` BIGINT NOT NULL, `card_no` VARCHAR(64) NOT NULL,
  `quota` DECIMAL(10,2) DEFAULT 0.00, `used_quota` DECIMAL(10,2) DEFAULT 0.00,
  `discount_rate` DECIMAL(5,4) DEFAULT 0.0000 COMMENT '免佣比例：持卡订单抽成减免',
  `valid_from` DATETIME DEFAULT NULL, `valid_to` DATETIME DEFAULT NULL COMMENT '卡期<=7天',
  `status` VARCHAR(16) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='洗车工免佣卡（日卡/3日/7日）';

-- ---------- learning 学习考试认证域 ----------
DROP TABLE IF EXISTS `t_course`;
CREATE TABLE `t_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_code` VARCHAR(64) NOT NULL, `course_name` VARCHAR(128) NOT NULL,
  `cert_code` VARCHAR(16) DEFAULT NULL, `duration_minutes` INT DEFAULT 0, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程包/SOP流程规范培训（全程免费）';

DROP TABLE IF EXISTS `t_exam`;
CREATE TABLE `t_exam` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_code` VARCHAR(64) NOT NULL, `exam_name` VARCHAR(128) NOT NULL, `cert_code` VARCHAR(16) DEFAULT NULL,
  `pass_score` INT DEFAULT 80, `total_score` INT DEFAULT 100, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试定义';

DROP TABLE IF EXISTS `t_question`;
CREATE TABLE `t_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT NOT NULL, `question_type` VARCHAR(16) DEFAULT 'SINGLE', `title` VARCHAR(512) NOT NULL,
  `option_a` VARCHAR(255) DEFAULT NULL, `option_b` VARCHAR(255) DEFAULT NULL, `option_c` VARCHAR(255) DEFAULT NULL, `option_d` VARCHAR(255) DEFAULT NULL,
  `correct_option` VARCHAR(8) DEFAULT NULL, `score` INT DEFAULT 5,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_exam` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

DROP TABLE IF EXISTS `t_exam_record`;
CREATE TABLE `t_exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `worker_id` BIGINT NOT NULL, `exam_id` BIGINT NOT NULL, `cert_code` VARCHAR(16) DEFAULT NULL,
  `score` INT DEFAULT NULL, `result` VARCHAR(16) DEFAULT NULL COMMENT 'PASS/FAIL', `submitted_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录';

-- ---------- governance 治理域 ----------
DROP TABLE IF EXISTS `t_review`;
CREATE TABLE `t_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT DEFAULT NULL, `user_id` BIGINT DEFAULT NULL, `worker_id` BIGINT DEFAULT NULL,
  `rating` INT DEFAULT NULL, `content` VARCHAR(512) DEFAULT NULL,
  `dim_scores` VARCHAR(512) DEFAULT NULL COMMENT '五维评分JSON', `reason` VARCHAR(512) DEFAULT NULL COMMENT '低分原因',
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_worker` (`worker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价（差评只降权不扣已完单报酬）';

DROP TABLE IF EXISTS `t_blacklist`;
CREATE TABLE `t_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_type` VARCHAR(16) DEFAULT 'WORKER' COMMENT '拉黑对象类型', `subject_id` BIGINT NOT NULL,
  `store_id` BIGINT DEFAULT NULL COMMENT '拉黑所属门店(局部拉黑)',
  `reason` VARCHAR(255) DEFAULT NULL, `effective_from` DATETIME DEFAULT NULL, `effective_to` DATETIME DEFAULT NULL,
  `status` INT DEFAULT 1 COMMENT '24h冷静期复核+申诉仲裁',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_subject` (`subject_type`,`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门店拉黑（仅该店过滤，非平台封号）';

DROP TABLE IF EXISTS `t_complaint`;
CREATE TABLE `t_complaint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `complaint_no` VARCHAR(64) NOT NULL, `order_id` BIGINT DEFAULT NULL, `user_id` BIGINT DEFAULT NULL,
  `reason` VARCHAR(128) DEFAULT NULL, `description` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(16) DEFAULT 'OPEN', `env_flag` VARCHAR(32) DEFAULT 'TEST-ENV-V1',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投诉（封号须人工复核+申诉）';

-- ---------- auth 权限域 ----------
DROP TABLE IF EXISTS `t_sys_role`;
CREATE TABLE `t_sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(64) NOT NULL, `role_name` VARCHAR(128) NOT NULL, `description` VARCHAR(255) DEFAULT NULL,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC角色';

DROP TABLE IF EXISTS `t_sys_permission`;
CREATE TABLE `t_sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `permission_code` VARCHAR(128) NOT NULL, `permission_name` VARCHAR(128) NOT NULL,
  `resource` VARCHAR(255) DEFAULT NULL, `action` VARCHAR(32) DEFAULT NULL, `parent_id` BIGINT DEFAULT 0,
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RBAC功能权限（最小权限+职责分离）';

DROP TABLE IF EXISTS `t_sys_user_role`;
CREATE TABLE `t_sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `subject_id` BIGINT NOT NULL, `subject_type` VARCHAR(16) DEFAULT 'ADMIN', `role_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_subject` (`subject_id`,`subject_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联';

DROP TABLE IF EXISTS `t_audit_log`;
CREATE TABLE `t_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT DEFAULT NULL, `operator_type` VARCHAR(32) DEFAULT NULL, `operation` VARCHAR(128) DEFAULT NULL,
  `target_type` VARCHAR(64) DEFAULT NULL, `target_id` VARCHAR(64) DEFAULT NULL, `detail` VARCHAR(1024) DEFAULT NULL,
  `before_val` TEXT DEFAULT NULL, `after_val` TEXT DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`), KEY `idx_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（WORM语义，保留>=180天）';

-- ---------- cache 缓存策略域 ----------
DROP TABLE IF EXISTS `t_cache_policy`;
CREATE TABLE `t_cache_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `domain` VARCHAR(64) NOT NULL, `level` VARCHAR(8) NOT NULL COMMENT 'A可缓存/B短TTL/C禁止缓存',
  `ttl_seconds` INT DEFAULT 0, `description` VARCHAR(255) DEFAULT NULL, `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缓存分级策略表';

SET FOREIGN_KEY_CHECKS = 1;
-- 建表完成：共 30 张表，覆盖 14 业务域
