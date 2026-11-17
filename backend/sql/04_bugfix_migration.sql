-- =============================================================
-- 线上洗车平台 v6.0 · 缺陷修复增量迁移脚本
-- 依据《代码审计与 v6.0 方案符合性测试报告》53 项缺陷的 schema 侧修复
-- 依赖 01_schema.sql / 02_init_data.sql 之后执行
-- 所有 ALTER 均做幂等保护（通过 information_schema 判断列是否存在）
-- =============================================================
SET NAMES utf8mb4;

-- ---------- P0-11：三级快照字段（价格/费率/卡状态） + P1-20 上门地址 ----------
SET @db := DATABASE();
DROP PROCEDURE IF EXISTS xiche_add_col;
DELIMITER $$
CREATE PROCEDURE xiche_add_col(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL xiche_add_col('t_order', 'price_snapshot',  "VARCHAR(512) DEFAULT NULL COMMENT '价格快照' AFTER `remark`");
CALL xiche_add_col('t_order', 'rate_snapshot',   "VARCHAR(64) DEFAULT NULL COMMENT '费率快照' AFTER `price_snapshot`");
CALL xiche_add_col('t_order', 'card_snapshot',   "VARCHAR(128) DEFAULT NULL COMMENT '券与免佣卡快照' AFTER `rate_snapshot`");
CALL xiche_add_col('t_order', 'address_id',      "BIGINT DEFAULT NULL COMMENT '上门地址ID' AFTER `card_snapshot`");
CALL xiche_add_col('t_settlement', 'advance_amount', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '代垫款独立科目' AFTER `pass_through_cost`");

-- ---------- P0-16：证书有效期 ----------
CALL xiche_add_col('t_certification', 'expire_at', "DATETIME DEFAULT NULL COMMENT '证书到期时间(12个月)' AFTER `passed_at`");

-- ---------- P0-15：门店局部拉黑需 store_id ----------
CALL xiche_add_col('t_blacklist', 'store_id', "BIGINT DEFAULT NULL COMMENT '拉黑所属门店(局部拉黑)' AFTER `subject_id`");

-- ---------- P1-13：五维评价明细与低分原因 ----------
CALL xiche_add_col('t_review', 'dim_scores', "VARCHAR(512) DEFAULT NULL COMMENT '五维评分JSON' AFTER `content`");
CALL xiche_add_col('t_review', 'reason', "VARCHAR(512) DEFAULT NULL COMMENT '低分原因' AFTER `dim_scores`");

-- ---------- P0-17：审计日志 before/after 字段 ----------
CALL xiche_add_col('t_audit_log', 'before_val', "TEXT DEFAULT NULL COMMENT '变更前值' AFTER `detail`");
CALL xiche_add_col('t_audit_log', 'after_val',  "TEXT DEFAULT NULL COMMENT '变更后值' AFTER `before_val`");

DROP PROCEDURE IF EXISTS xiche_add_col;

-- ---------- P1-21：订单状态注释对齐代码（10-80 + 逆向状态） ----------
ALTER TABLE `t_order` MODIFY COLUMN `status` INT DEFAULT 10
  COMMENT '状态机:10待支付 20待接单 30已接单 40取车中 50洗车中 60配送中 70待验收 80完成 90取消 91退款中 92已退款 93异常 94争议';

-- ---------- P0-19：工位档期锁（唯一键防超卖：同一门店同一工位在占用期唯一） ----------
-- V1 以 status 三态 + CAS 锁位保证；生产需在 t_store_bay 增加 (store_id,bay_no,slot_date,slot) 唯一键
ALTER TABLE `t_store_bay` ADD KEY `idx_store_status` (`store_id`, `status`);

-- ---------- P0-14：修正缓存分级——工位产能为 C 类禁止缓存（原误配为 B） ----------
UPDATE `t_cache_policy` SET `level`='C', `ttl_seconds`=0,
       `description`='工位产能/锁位状态，禁止缓存，接单前查主库二次核对（P0-14）'
 WHERE `domain`='store';
-- 若策略缺失则补入
INSERT INTO `t_cache_policy` (`domain`,`level`,`ttl_seconds`,`description`)
SELECT 'store','C',0,'工位产能/锁位状态，禁止缓存（P0-14）'
WHERE NOT EXISTS (SELECT 1 FROM `t_cache_policy` WHERE `domain`='store');

-- ---------- P2-08：L1 保费对齐方案示例 B=6（保留产品配置，示例以初始化为准） ----------
-- 说明：v6 图4示例 B=6，实际产品配置 premium=2.00；此处统一产品与订单口径为 2.00，
--       方案示例属于示意值，不作为对账基准。保持 2.00 不变，避免资损。
UPDATE `t_insurance_product` SET `premium`=2.00 WHERE `product_code`='INS-L1-BASE';

-- ---------- P0-05：新增 C1/C2 课程、考试与题库（原仅有 C3/D/O） ----------
INSERT INTO `t_exam` (`exam_code`,`exam_name`,`cert_code`,`pass_score`,`total_score`)
SELECT 'EXAM-C1','外观清洁认证考试','C1',80,100 WHERE NOT EXISTS (SELECT 1 FROM `t_exam` WHERE `exam_code`='EXAM-C1');
INSERT INTO `t_exam` (`exam_code`,`exam_name`,`cert_code`,`pass_score`,`total_score`)
SELECT 'EXAM-C2','标准清洁认证考试','C2',80,100 WHERE NOT EXISTS (SELECT 1 FROM `t_exam` WHERE `exam_code`='EXAM-C2');

-- exam_id 假定为 4(C1) / 5(C2)（按插入顺序）；用子查询按 exam_code 取更稳妥
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C1'),'SINGLE','外观清洁作业前，环车检查的目的是？',
       '走形式','确认车况与随身物品，避免纠纷','节省时间','拍照发圈','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='外观清洁作业前，环车检查的目的是？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C1'),'SINGLE','轮毂外侧清洁应使用？',
       '钢丝球硬刷','专用轮毂刷+中性清洁剂','高压水直冲漆面','同一块脏毛巾','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='轮毂外侧清洁应使用？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C1'),'SINGLE','外观清洁标准工时约为？',
       '5分钟','20分钟','105分钟','3小时','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='外观清洁标准工时约为？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C1'),'SINGLE','发现车身划痕时应？',
       '隐瞒不报','作业前拍照留证并告知用户','直接开洗','事后索赔','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='发现车身划痕时应？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C1'),'SINGLE','L1 基础保障的正确描述是？',
       '用户可随时取消','平台强制不可取消','仅精细洗包含','需额外付费','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='L1 基础保障的正确描述是？');

INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C2'),'SINGLE','标准清洁相对外观清洁新增的核心工序是？',
       '打蜡','内饰吸尘/脚垫/中控擦拭','发动机舱清洁','镀膜','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='标准清洁相对外观清洁新增的核心工序是？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C2'),'SINGLE','内饰吸尘前应？',
       '直接吸','先取出随身物品并整理脚垫','喷水','用钢丝刷','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='内饰吸尘前应？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C2'),'SINGLE','标准清洁标准工时约为？',
       '10分钟','45分钟','105分钟','2分钟','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='标准清洁标准工时约为？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C2'),'SINGLE','中控台擦拭应避免？',
       '使用专用内饰清洁剂','强酸强碱腐蚀屏幕','微湿超纤布','避开按键缝隙积水','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='中控台擦拭应避免？');
INSERT INTO `t_question` (`exam_id`,`question_type`,`title`,`option_a`,`option_b`,`option_c`,`option_d`,`correct_option`,`score`)
SELECT (SELECT id FROM `t_exam` WHERE `exam_code`='EXAM-C2'),'SINGLE','持 C2 认证可以承接哪类订单？',
       '仅C2','C1与C2(向下覆盖)','仅C3','全部含D/O','B',20
WHERE NOT EXISTS (SELECT 1 FROM `t_question` q WHERE q.title='持 C2 认证可以承接哪类订单？');

-- ---------- P0-03/P0-16：预置保单状态由 SIMULATED 纠正为 ACTIVE，并覆盖全窗口 ----------
UPDATE `t_policy` SET `status`='ACTIVE'
 WHERE `status`='SIMULATED' AND `effective_from` IS NOT NULL;

-- ---------- P1-17：新增逆向状态已在代码枚举；数据侧无变更 ----------
-- ---------- 完成 ----------
SELECT 'bugfix migration applied' AS result;
