-- =============================================================
-- 线上洗车平台 v6.0 · 鉴权与治理修复迁移脚本
-- 依据《v6.0 全量测试报告》P0-05/P1-06/P1-11/P1-14 等 schema 侧修复
-- 依赖 01~04 脚本之后执行。所有 DDL 幂等。
-- =============================================================
SET NAMES utf8mb4;

-- ---------- BUG-P0-05：统一登录账号表（身份唯一可信源） ----------
CREATE TABLE IF NOT EXISTS `t_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL COMMENT '登录名（手机号或后台账号）',
  `password_hash` VARCHAR(128) NOT NULL COMMENT 'SHA-256(salt+明文)',
  `salt` VARCHAR(64) NOT NULL,
  `subject_type` VARCHAR(16) NOT NULL COMMENT 'USER/WORKER/STORE/ADMIN',
  `subject_id` BIGINT NOT NULL COMMENT '业务主体主键',
  `roles` VARCHAR(255) DEFAULT NULL COMMENT '逗号分隔角色码，可空',
  `status` INT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一登录账号';

-- ---------- P1-06：角色-权限关联表（权限挂接到角色） ----------
CREATE TABLE IF NOT EXISTS `t_sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色-权限关联';

-- ---------- P1-06：补城市经理(CITY_MANAGER)、审计员(AUDITOR 已存在，补审核复核角色) ----------
INSERT INTO `t_sys_role` (`role_code`,`role_name`,`description`)
SELECT 'CITY_MANAGER','城市经理','本城市门店与产能运营，数据范围=本城市'
WHERE NOT EXISTS (SELECT 1 FROM `t_sys_role` WHERE `role_code`='CITY_MANAGER');

-- ---------- 角色-权限绑定（最小权限示例） ----------
INSERT INTO `t_sys_role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `t_sys_role` r, `t_sys_permission` p
WHERE r.role_code='ADMIN'
  AND NOT EXISTS (SELECT 1 FROM `t_sys_role_permission` rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO `t_sys_role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `t_sys_role` r, `t_sys_permission` p
WHERE r.role_code='FINANCE' AND p.permission_code IN ('admin:settlement:reconcile')
  AND NOT EXISTS (SELECT 1 FROM `t_sys_role_permission` rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
INSERT INTO `t_sys_role_permission` (`role_id`,`permission_id`)
SELECT r.id, p.id FROM `t_sys_role` r, `t_sys_permission` p
WHERE r.role_code='AUDITOR' AND p.permission_code IN ('admin:audit:review')
  AND NOT EXISTS (SELECT 1 FROM `t_sys_role_permission` rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);

-- ---------- 统一测试账号（salt=xiche2026；test1234 的 SHA-256 = 6b93...733f；admin123 = 5598...da7b） ----------
-- 车主甲(subjectId=2) / 车主乙(subjectId=3)
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT '13800000001','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','USER',2,NULL,1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='13800000001');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT '13800000002','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','USER',3,NULL,1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='13800000002');
-- 洗车工王师傅(1)/赵师傅(2)
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT '13900000001','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','WORKER',1,NULL,1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='13900000001');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT '13900000002','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','WORKER',2,NULL,1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='13900000002');
-- 门店(subjectId=1)
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'store001','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','STORE',1,NULL,1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='store001');
-- 后台：超管 / 审计员 / 财务 / 合规 / 运营；soduser 同时具 ADMIN+AUDITOR 用于 SoD 冲突验证
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'admin','5598aa2f3b3ea514da0fa439a711af8db8f97bf0661e54af00cb567f4822da7b','xiche2026','ADMIN',1,'ADMIN',1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='admin');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'auditor','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','ADMIN',10,'AUDITOR',1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='auditor');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'finance','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','ADMIN',11,'FINANCE',1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='finance');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'compliance','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','ADMIN',12,'COMPLIANCE',1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='compliance');
INSERT INTO `t_account` (`username`,`password_hash`,`salt`,`subject_type`,`subject_id`,`roles`,`status`)
SELECT 'soduser','6b93751b059c50e58c794420d1af1ddcf535bb250fc7978699aa45391210733f','xiche2026','ADMIN',13,'ADMIN,AUDITOR,FINANCE',1
WHERE NOT EXISTS (SELECT 1 FROM `t_account` WHERE `username`='soduser');

-- 后台账号角色同步到 t_sys_user_role（库加载来源之一）
INSERT INTO `t_sys_user_role` (`subject_id`,`subject_type`,`role_id`)
SELECT 10,'ADMIN',(SELECT id FROM `t_sys_role` WHERE role_code='AUDITOR')
WHERE NOT EXISTS (SELECT 1 FROM `t_sys_user_role` ur JOIN `t_sys_role` r ON r.id=ur.role_id
                  WHERE ur.subject_id=10 AND ur.subject_type='ADMIN' AND r.role_code='AUDITOR');
INSERT INTO `t_sys_user_role` (`subject_id`,`subject_type`,`role_id`)
SELECT 11,'ADMIN',(SELECT id FROM `t_sys_role` WHERE role_code='FINANCE')
WHERE NOT EXISTS (SELECT 1 FROM `t_sys_user_role` ur JOIN `t_sys_role` r ON r.id=ur.role_id
                  WHERE ur.subject_id=11 AND ur.subject_type='ADMIN' AND r.role_code='FINANCE');
INSERT INTO `t_sys_user_role` (`subject_id`,`subject_type`,`role_id`)
SELECT 12,'ADMIN',(SELECT id FROM `t_sys_role` WHERE role_code='COMPLIANCE')
WHERE NOT EXISTS (SELECT 1 FROM `t_sys_user_role` ur JOIN `t_sys_role` r ON r.id=ur.role_id
                  WHERE ur.subject_id=12 AND ur.subject_type='ADMIN' AND r.role_code='COMPLIANCE');

-- ---------- P1-11：人工复核表（考试通过进入 PENDING_REVIEW，48h 复核后转 PASSED） ----------
CREATE TABLE IF NOT EXISTS `t_cert_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `worker_id` BIGINT NOT NULL,
  `cert_code` VARCHAR(16) NOT NULL,
  `exam_record_id` BIGINT DEFAULT NULL,
  `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `reviewer_id` BIGINT DEFAULT NULL,
  `review_note` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `reviewed_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_worker_cert` (`worker_id`,`cert_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证人工复核';

-- ---------- P1-10：考试记录补切屏计数/试卷快照/冷却字段 ----------
SET @db := DATABASE();
DROP PROCEDURE IF EXISTS xiche_add_col2;
DELIMITER $$
CREATE PROCEDURE xiche_add_col2(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = @db AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', ddl);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;
CALL xiche_add_col2('t_exam_record','switch_count',"INT DEFAULT 0 COMMENT '切屏次数' AFTER `result`");
CALL xiche_add_col2('t_exam_record','paper_snapshot',"VARCHAR(512) DEFAULT NULL COMMENT '试卷快照(题号/正确答案哈希)' AFTER `switch_count`");
DROP PROCEDURE IF EXISTS xiche_add_col2;

SELECT 'auth & governance migration applied' AS result;

-- ---------- BUG-P0-06：幂等键改为 (user_id, request_id) 联合唯一，orderNo 服务端生成 ----------
DROP PROCEDURE IF EXISTS xiche_add_col3;
DELIMITER $$
CREATE PROCEDURE xiche_add_col3(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', ddl);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;
CALL xiche_add_col3('t_order','request_id',"VARCHAR(128) DEFAULT NULL COMMENT '客户端幂等键(与user_id联合唯一)' AFTER `order_no`");
DROP PROCEDURE IF EXISTS xiche_add_col3;
-- 联合唯一键（历史数据 request_id 为 NULL，不冲突）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME='t_order' AND INDEX_NAME='uk_user_request');
SET @s = IF(@idx_exists=0,
            'ALTER TABLE `t_order` ADD UNIQUE KEY `uk_user_request` (`user_id`,`request_id`)',
            'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SELECT 'p0-06 migration applied' AS result;

-- ---------- P0-07/P1-09：订单记录占用工位，便于完工释放 ----------
DROP PROCEDURE IF EXISTS xiche_add_col4;
DELIMITER $$
CREATE PROCEDURE xiche_add_col4(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
    SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', ddl);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;
CALL xiche_add_col4('t_order','bay_id',"BIGINT DEFAULT NULL COMMENT '占用工位ID(完工释放)' AFTER `store_id`");
DROP PROCEDURE IF EXISTS xiche_add_col4;
SELECT 'p0-07 bay_id migration applied' AS result;
