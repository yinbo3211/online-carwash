-- =============================================================
-- 线上洗车平台 v6.0 · V1 内部测试数据（禁止用于生产）
-- admin/admin123 为示例明文密码；所有支付为模拟，不产生真实资金收付
-- =============================================================
SET NAMES utf8mb4;

-- ---------- 后台管理员账号（承载于 t_user，role=ADMIN） ----------
INSERT INTO `t_user` (`phone`,`nickname`,`real_name`,`status`) VALUES
('admin','系统管理员','测试管理员',1);
INSERT INTO `t_sys_user_role` (`subject_id`,`subject_type`,`role_id`) VALUES
(1,'ADMIN',1);

-- ---------- 车主 ----------
INSERT INTO `t_user` (`phone`,`nickname`,`real_name`,`status`) VALUES
('13800000001','车主甲','张车主',1),
('13800000002','车主乙','李车主',1);
INSERT INTO `t_user_address` (`user_id`,`receiver_name`,`receiver_phone`,`province`,`city`,`district`,`detail`,`is_default`) VALUES
(2,'张车主','13800000001','上海市','上海市','浦东新区','世纪大道100号',1);

-- ---------- 洗车工（含认证状态：C1/C2通过，C3未通过 -> 不能接精细单） ----------
INSERT INTO `t_worker` (`real_name`,`phone`,`id_card_no`,`city`,`rating`,`completed_orders`,`daily_capacity`,`today_orders`,`banned`,`status`) VALUES
('王师傅','13900000001','310101199001011234','上海市',4.80,120,8,2,0,1),
('赵师傅','13900000002','310101199202025678','上海市',4.60,80,8,5,0,1);
INSERT INTO `t_worker_license` (`worker_id`,`license_no`,`license_type`,`valid_until`,`status`) VALUES
(1,'沪A1234567','C1','2028-12-31',1),
(2,'沪A7654321','C1','2027-06-30',1);
INSERT INTO `t_certification` (`worker_id`,`cert_code`,`exam_score`,`status`,`passed_at`) VALUES
(1,'C1',92,'PASSED',NOW()),(1,'C2',88,'PASSED',NOW()),(1,'C3',NULL,'LOCKED',NULL),(1,'D',85,'PASSED',NOW()),
(2,'C1',90,'PASSED',NOW()),(2,'C2',86,'PASSED',NOW()),(2,'C3',NULL,'LOCKED',NULL);

-- ---------- 合作门店（中性命名，不使用任何品牌商标） ----------
INSERT INTO `t_store` (`store_name`,`owner_user_id`,`phone`,`city`,`address`,`bay_count`,`bay_fee`,`status`) VALUES
('合作养车门店·浦东店',3,'021-50000001','上海市','浦东新区张江路88号',3,24.00,1);
INSERT INTO `t_store_bay` (`store_id`,`bay_no`,`status`) VALUES
(1,'A01',0),(1,'A02',0),(1,'A03',1);
INSERT INTO `t_crew` (`store_id`,`worker_id`,`role`,`status`) VALUES
(1,1,'组长',1),(1,2,'技师',1);

-- ---------- 订单样本 ----------
-- 订单1：标准清洁到店，用户实付P=133，工位费M=24，保险L1=2，代垫0，补贴C=0
-- L = P+C-M-B = 133-24-2 = 107；platform_fee=107*0.40=42.80；worker_net=64.20；store_net=24
-- 恒等式：133 = 64.20 + 24 + 2 + 42.80 = 133 ✓
INSERT INTO `t_order` (`order_no`,`user_id`,`store_id`,`worker_id`,`service_sku_id`,`service_level`,`service_mode`,`fulfill_mode`,`status`,
  `paid_amount`,`platform_subsidy`,`store_bay_fee`,`insurance_fee`,`advance_amount`,`labor_income`,`pass_through_cost`,
  `platform_fee`,`worker_net`,`store_net`,`env_flag`,`remark`) VALUES
('XC202611160001',2,1,1,2,2,'STORE','PICKUP',30,133.00,0.00,24.00,2.00,0.00,107.00,2.00,42.80,64.20,24.00,'TEST-ENV-V1','标准清洁到店-测试中');
-- 订单2：外观清洁上门，P=95，工位费M=0（上门无门店工位），包干组织费经G口径另记；保险2，代垫0
-- 上门抽成35%：L=95-0-2=93；platform_fee=93*0.35=32.55；worker_net=60.45
INSERT INTO `t_order` (`order_no`,`user_id`,`store_id`,`worker_id`,`service_sku_id`,`service_level`,`service_mode`,`fulfill_mode`,`status`,
  `paid_amount`,`platform_subsidy`,`store_bay_fee`,`insurance_fee`,`advance_amount`,`labor_income`,`pass_through_cost`,
  `platform_fee`,`worker_net`,`store_net`,`env_flag`,`remark`) VALUES
('XC202611160002',3,1,NULL,4,1,'ONSITE','ONSITE',20,95.00,0.00,0.00,2.00,0.00,93.00,2.00,32.55,60.45,0.00,'TEST-ENV-V1','外观清洁上门-待接单');
-- 订单3：精细清洁到店，P=243，M=54，保险2
-- L=243-54-2=187；platform_fee=187*0.40=74.80；worker_net=112.20
INSERT INTO `t_order` (`order_no`,`user_id`,`store_id`,`worker_id`,`service_sku_id`,`service_level`,`service_mode`,`fulfill_mode`,`status`,
  `paid_amount`,`platform_subsidy`,`store_bay_fee`,`insurance_fee`,`advance_amount`,`labor_income`,`pass_through_cost`,
  `platform_fee`,`worker_net`,`store_net`,`env_flag`,`remark`) VALUES
('XC202611160003',2,1,NULL,3,3,'STORE','PICKUP',10,243.00,0.00,54.00,2.00,0.00,187.00,2.00,74.80,112.20,54.00,'TEST-ENV-V1','精细清洁到店-待支付');

INSERT INTO `t_order_item` (`order_id`,`sku_id`,`item_name`,`quantity`,`unit_price`,`amount`,`item_type`) VALUES
(1,2,'标准清洁',1,133.00,133.00,'PACKAGE'),
(2,4,'外观清洁(上门)',1,95.00,95.00,'PACKAGE'),
(3,3,'精细清洁',1,243.00,243.00,'PACKAGE');

-- ---------- 模拟支付流水 ----------
INSERT INTO `t_payment_order` (`payment_no`,`order_id`,`order_no`,`user_id`,`amount`,`gateway`,`status`,`success_message`,`success_color`,`env_flag`,`idempotent_key`,`paid_at`) VALUES
('PAY202611160001',1,'XC202611160001',2,133.00,'MOCK','SUCCESS','模拟支付成功·测试数据，不产生真实资金收付','#F5901B','TEST-ENV-V1','IDEM-TEST-001',NOW());

-- ---------- 模拟保单（每单必须有保险；V1模拟台账） ----------
INSERT INTO `t_policy` (`policy_no`,`order_id`,`product_id`,`level`,`premium`,`status`,`effective_from`,`effective_to`) VALUES
('POL-TEST-001',1,1,'L1',2.00,'SIMULATED',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY)),
('POL-TEST-002',2,1,'L1',2.00,'SIMULATED',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY)),
('POL-TEST-003',3,1,'L1',2.00,'SIMULATED',NOW(),DATE_ADD(NOW(),INTERVAL 1 DAY));

-- ---------- 结算单与台账（订单1已结算） ----------
INSERT INTO `t_settlement` (`settlement_no`,`order_id`,`order_no`,`service_mode`,`paid_amount`,`platform_subsidy`,`store_bay_fee`,`pass_through_cost`,`labor_income`,`commission_rate`,`platform_fee`,`worker_net`,`store_net`,`total`,`status`,`env_flag`,`settled_at`) VALUES
('ST-XC202611160001',1,'XC202611160001','STORE',133.00,0.00,24.00,2.00,107.00,0.4000,42.80,64.20,24.00,133.00,'SETTLED','TEST-ENV-V1',NOW());
INSERT INTO `t_ledger` (`ledger_no`,`settlement_id`,`account_type`,`account_id`,`subject`,`direction`,`amount`,`env_flag`) VALUES
('LG-001-1',1,'PLATFORM',0,'平台运营费40%','CREDIT',42.80,'TEST-ENV-V1'),
('LG-001-2',1,'WORKER',1,'洗车工实收','CREDIT',64.20,'TEST-ENV-V1'),
('LG-001-3',1,'STORE',1,'门店工位费M','CREDIT',24.00,'TEST-ENV-V1'),
('LG-001-4',1,'INSURANCE',0,'L1保障代收','CREDIT',2.00,'TEST-ENV-V1');

-- ---------- 四组证据（订单1已取还车） ----------
INSERT INTO `t_order_evidence` (`order_id`,`stage`,`image_url`,`operator_id`) VALUES
(1,'E1','/mock/evidence/XC202611160001_E1.jpg',1),
(1,'E2','/mock/evidence/XC202611160001_E2.jpg',1),
(1,'E3','/mock/evidence/XC202611160001_E3.jpg',1),
(1,'E4','/mock/evidence/XC202611160001_E4.jpg',1);

-- ---------- 评价与投诉样本 ----------
INSERT INTO `t_review` (`order_id`,`user_id`,`worker_id`,`rating`,`content`,`status`) VALUES
(1,2,1,5,'服务很好，工具归位整洁',1);
INSERT INTO `t_complaint` (`complaint_no`,`order_id`,`user_id`,`reason`,`description`,`status`,`env_flag`) VALUES
('CP-TEST-001',1,2,'服务态度','测试投诉样本，待人工调解','OPEN','TEST-ENV-V1');
