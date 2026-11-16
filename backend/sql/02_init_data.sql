-- =============================================================
-- 线上洗车平台 v6.0 · 初始化基础数据
-- 依赖 01_schema.sql。仅基础字典/角色/品类/价格/保险/缓存策略/题库。
-- =============================================================
SET NAMES utf8mb4;

-- ---------- RBAC 角色（职责分离：审核≠财务、封号复核≠申诉仲裁） ----------
INSERT INTO `t_sys_role` (`role_code`,`role_name`,`description`) VALUES
('ADMIN','超级管理员','全部权限，需双人复核的高危操作除外'),
('OPERATIONS','运营','服务目录/价格版本/优惠券配置'),
('AUDITOR','审核','实名/驾驶证/门店准入审核，不含财务'),
('FINANCE','财务','对账/结算/提现，不含审核与封号'),
('CS','客服','投诉受理与调解，不含终局封号'),
('COMPLIANCE','法务合规','封号复核与申诉仲裁，不含日常运营');

-- ---------- RBAC 功能权限（最小权限示例） ----------
INSERT INTO `t_sys_permission` (`permission_code`,`permission_name`,`resource`,`action`,`parent_id`) VALUES
('admin:dashboard:view','查看总览','/api/admin/dashboard','GET',0),
('admin:audit:review','审核实名与驾驶证','/api/admin/audit','POST',0),
('admin:governance:ban','封号操作','/api/admin/governance/ban','POST',0),
('admin:governance:appeal','申诉仲裁','/api/admin/governance/appeal','POST',0),
('admin:rate:config','费率与价格版本','/api/admin/rate','POST',0),
('admin:settlement:reconcile','对账','/api/admin/settlement/reconcile','POST',0),
('admin:permission:manage','权限管理','/api/admin/permission','POST',0);

-- ---------- 缓存分级策略（A可缓存 / B短TTL强一致 / C禁止缓存） ----------
INSERT INTO `t_cache_policy` (`domain`,`level`,`ttl_seconds`,`description`) VALUES
('catalog','A',3600,'服务目录/城市配置，可长TTL缓存'),
('learning','A',1800,'课程与题库，可缓存'),
('auth','B',60,'认证状态/权限矩阵，短TTL+变更主动失效'),
('store','B',30,'工位产能状态，短TTL且接单前二次核对主库'),
('order','C',0,'订单状态，禁止缓存'),
('payment','C',0,'资金与支付，禁止缓存'),
('settlement','C',0,'结算金额与分账指令，禁止缓存'),
('insurance','C',0,'保单回写状态，禁止缓存'),
('coupon','C',0,'券与免佣卡余量，禁止缓存');

-- ---------- 保险产品（L1 每单强制不可取消；L2 自愿加购） ----------
INSERT INTO `t_insurance_product` (`product_code`,`product_name`,`level`,`premium`,`coverage_amount`,`mandatory`,`status`,`remark`) VALUES
('INS-L1-BASE','平台基础保障','L1',2.00,50000.00,1,1,'每单强制，成本内含，不可取消；V1仅模拟记录'),
('INS-L2-PLUS','增值保障','L2',12.00,200000.00,0,1,'自愿加购；真实出单须持牌保险合作方书面确认');

-- ---------- 服务品类三档 ----------
INSERT INTO `t_service_category` (`category_code`,`category_name`,`service_level`,`sort_no`) VALUES
('CAT-APPEARANCE','外观清洁',1,1),
('CAT-STANDARD','标准清洁',2,2),
('CAT-DETAIL','精细清洁',3,3),
('CAT-ADDON','个性化增值服务',NULL,4);

-- ---------- 服务 SKU：三档到店 + 两档上门 + 增值项 ----------
INSERT INTO `t_service_sku` (`category_id`,`sku_code`,`sku_name`,`service_level`,`service_mode`,`price`,`price_remark`) VALUES
(1,'SKU-APP-STORE','外观清洁(到店)',1,'STORE',83.00,'引流档，20min'),
(2,'SKU-STD-STORE','标准清洁(到店)',2,'STORE',133.00,'主推档，45min'),
(3,'SKU-DTL-STORE','精细清洁(到店)',3,'STORE',243.00,'利润档，105min'),
(1,'SKU-APP-ONSITE','外观清洁(上门)',1,'ONSITE',95.00,'车不离场，含移动设备'),
(2,'SKU-STD-ONSITE','标准清洁(上门)',2,'ONSITE',155.00,'车不离场，含移动设备'),
(4,'SKU-ADD-OILFILM','玻璃去油膜',NULL,'STORE',39.00,'增值项，随套餐下单'),
(4,'SKU-ADD-TIRE','轮胎补气+胎压检测',NULL,'STORE',19.00,'增值项'),
(4,'SKU-ADD-ODOR','车内除异味',NULL,'STORE',59.00,'增值项'),
(4,'SKU-ADD-WHEEL','轮毂精洗',NULL,'STORE',36.00,'增值项'),
(4,'SKU-ADD-LEATHER','内饰皮革养护',NULL,'STORE',49.00,'增值项'),
(4,'SKU-ADD-ENGINE','发动机舱清洁',NULL,'STORE',65.00,'增值项，仅到店，需固定工位'),
(4,'SKU-ADD-WATER','玻璃水雨刮补给',NULL,'STORE',15.00,'增值项'),
(4,'SKU-ADD-PET','宠物毛发深度清理',NULL,'STORE',45.00,'增值项');

-- ---------- 价格版本 ----------
INSERT INTO `t_price_version` (`version_code`,`city`,`sku_id`,`price`,`effective_from`,`status`)
SELECT 'PV-V1-20261101','全国',id,price,NOW(),1 FROM `t_service_sku` WHERE status=1;

-- ---------- 学习考试：C3 精细认证题库（80分及格） ----------
INSERT INTO `t_course` (`course_code`,`course_name`,`cert_code`,`duration_minutes`) VALUES
('COURSE-C1','外观清洁标准作业','C1',60),
('COURSE-C2','标准清洁与内饰作业','C2',120),
('COURSE-C3','精细清洁与养护','C3',240),
('COURSE-D','取送车与代驾安全规范','D',90),
('COURSE-O','上门作业与污水回收规范','O',90);

INSERT INTO `t_exam` (`exam_code`,`exam_name`,`cert_code`,`pass_score`,`total_score`) VALUES
('EXAM-C3','精细清洁认证考试','C3',80,100),
('EXAM-D','取送车安全考试','D',80,100),
('EXAM-O','上门作业规范考试','O',80,100);

INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`) VALUES
(1,'SINGLE','精细清洁作业前，必须完成哪一步？','直接开始洗','取车验车拍照E1并核对车辆状况','跳过检查','只拍还车照','B',20),
(1,'SINGLE','精细清洁标准工时约为？','10分钟','30分钟','105分钟','5分钟','C',20),
(1,'SINGLE','轮毂内侧清洁应使用？','同一块脏毛巾','专用刷具与中性清洁剂','高压水直冲漆面','钢丝球','B',20),
(1,'SINGLE','发动机舱清洁属于？','上门可做','仅到店固定工位可做','任意场地可做','禁止提供','B',20),
(1,'SINGLE','考试未通过时，对应订单应？','仍可接单','不进抢单池且不被指派','降权后可接','无影响','B',20),
(2,'SINGLE','代驾取送车途中发生刮蹭，第一步应？','私下协商','一键事故上报并冻结结算','继续行驶','删除照片','B',25),
(2,'SINGLE','取车验车照片要求？','手机相册选图','现场拍摄带时间/GPS水印，禁相册','任意旧照','无需拍照','B',25),
(2,'SINGLE','每单保险窗口应覆盖？','仅洗车时','T1去程+T2作业+T3返程全窗口','仅取车','不需要','B',25),
(2,'SINGLE','驾驶证核验属于准入的？','可选项','必备项','加分项','无关项','B',25),
(3,'SINGLE','上门作业的污水应？','就地排放','移动设备回收，带出场地处理','排入雨水口','倒入绿化带','B',25),
(3,'SINGLE','物业禁止商业洗车时应？','强行作业','停止作业并改约到店','换角落偷偷洗','无视','B',25),
(3,'SINGLE','上门精细清洁为何不开放？','利润低','无举升机/固定水电/无尘环境与专业污水系统','用户不喜欢','法规禁止一切上门','B',25),
(3,'SINGLE','上门作业增值工时系数约为？','0.5倍','1.25倍','3倍','不变','B',25);
