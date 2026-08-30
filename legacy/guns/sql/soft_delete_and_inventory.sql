-- =============================================
-- 1. 软删除机制 — 9张业务表新增 is_deleted 列
-- 2. 药品库存管理 — 入库/出库/批次表
-- =============================================

START TRANSACTION;

-- ==================== 1. 软删除列 ====================
ALTER TABLE medicine_info ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER medicine_stock;
ALTER TABLE sys_notice ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER content;
ALTER TABLE patient_info ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER user_id;
ALTER TABLE patient_health ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER patient_name;
ALTER TABLE patient_history ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER doctor_id;
ALTER TABLE doctor_point ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER status;
ALTER TABLE chronic_disease ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER status;
ALTER TABLE chronic_followup ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER status;
ALTER TABLE chronic_followup_plan ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记' AFTER status;

-- ==================== 2. medicine_info 增强 ====================
ALTER TABLE medicine_info ADD COLUMN medicine_stock_min INT NOT NULL DEFAULT 10 COMMENT '低库存预警阈值' AFTER medicine_stock;

-- ==================== 3. 入库记录表 ====================
CREATE TABLE IF NOT EXISTS medicine_stock_in (
  id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  medicine_id INT NOT NULL COMMENT '药品ID',
  batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
  quantity INT NOT NULL COMMENT '入库数量',
  unit_price DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
  supplier VARCHAR(255) DEFAULT NULL COMMENT '供应商',
  expiry_date DATE DEFAULT NULL COMMENT '有效期至',
  operator VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  create_time DATETIME DEFAULT NULL COMMENT '入库时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_medicine_id (medicine_id),
  KEY idx_batch_no (batch_no),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='药品入库记录';

-- ==================== 4. 出库记录表 ====================
CREATE TABLE IF NOT EXISTS medicine_stock_out (
  id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  medicine_id INT NOT NULL COMMENT '药品ID',
  batch_no VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  quantity INT NOT NULL COMMENT '出库数量',
  reason VARCHAR(100) DEFAULT '门诊发药' COMMENT '出库原因：门诊发药/住院发药/退货/报损/其他',
  patient_name VARCHAR(50) DEFAULT NULL COMMENT '患者姓名',
  operator VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  create_time DATETIME DEFAULT NULL COMMENT '出库时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_medicine_id (medicine_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='药品出库记录';

-- ==================== 5. 批次管理表 ====================
CREATE TABLE IF NOT EXISTS medicine_batch (
  id INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  medicine_id INT NOT NULL COMMENT '药品ID',
  batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
  production_date DATE DEFAULT NULL COMMENT '生产日期',
  expiry_date DATE DEFAULT NULL COMMENT '有效期至',
  initial_quantity INT NOT NULL COMMENT '初始数量',
  remaining_quantity INT NOT NULL DEFAULT 0 COMMENT '剩余数量',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1=在用, 0=已用完, 2=已过期',
  create_time DATETIME DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_medicine_id (medicine_id),
  KEY idx_batch_no (batch_no),
  KEY idx_expiry_date (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='药品批次管理';

-- ==================== 6. 为现有药品初始化库存阈值 ====================
UPDATE medicine_info SET medicine_stock_min = CASE
  WHEN medicine_stock < 20 THEN 5
  WHEN medicine_stock < 100 THEN 20
  ELSE 50
END;

-- ==================== 验证 ====================
SELECT 'soft-delete columns' AS item, COUNT(*) AS cnt
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'bs1' AND COLUMN_NAME = 'is_deleted';

SELECT 'medicine_info with stock_min' AS item,
  COUNT(*) AS cnt FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'bs1' AND TABLE_NAME = 'medicine_info'
  AND COLUMN_NAME = 'medicine_stock_min';

SELECT '新表' AS item, TABLE_NAME, TABLE_ROWS
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = 'bs1'
  AND TABLE_NAME IN ('medicine_stock_in', 'medicine_stock_out', 'medicine_batch');

SELECT id, medicine_name, medicine_stock, medicine_stock_min,
  CASE WHEN medicine_stock <= medicine_stock_min THEN 'LOW' ELSE 'OK' END AS alert
FROM medicine_info WHERE is_deleted = 0 ORDER BY medicine_stock;

COMMIT;
