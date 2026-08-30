-- =============================================
-- 关系表 ID 化改造
-- 1. 添加 doctor_id / followup_doctor_id 列
-- 2. 从 doctor_info 回填 ID 值
-- 3. 添加索引
-- =============================================

START TRANSACTION;

-- ==================== 1. patient_history: 新增 doctor_id ====================
ALTER TABLE patient_history ADD COLUMN doctor_id INT DEFAULT NULL COMMENT '医生ID(关联doctor_info.id)' AFTER patient_doctor;

UPDATE patient_history h
INNER JOIN doctor_info d ON d.doctor_name = h.patient_doctor AND d.status = 1
SET h.doctor_id = d.id
WHERE h.patient_doctor IS NOT NULL AND h.doctor_id IS NULL;

ALTER TABLE patient_history ADD INDEX idx_doctor_id (doctor_id);

-- ==================== 2. chronic_disease: 新增 doctor_id ====================
ALTER TABLE chronic_disease ADD COLUMN doctor_id INT DEFAULT NULL COMMENT '管理医生ID(关联doctor_info.id)' AFTER doctor_name;

UPDATE chronic_disease cd
INNER JOIN doctor_info d ON d.doctor_name = cd.doctor_name AND d.status = 1
SET cd.doctor_id = d.id
WHERE cd.doctor_name IS NOT NULL AND cd.doctor_id IS NULL;

ALTER TABLE chronic_disease ADD INDEX idx_doctor_id (doctor_id);

-- ==================== 3. chronic_followup: 新增 followup_doctor_id ====================
ALTER TABLE chronic_followup ADD COLUMN followup_doctor_id INT DEFAULT NULL COMMENT '随访医生ID(关联doctor_info.id)' AFTER followup_doctor;

UPDATE chronic_followup cf
INNER JOIN doctor_info d ON d.doctor_name = cf.followup_doctor AND d.status = 1
SET cf.followup_doctor_id = d.id
WHERE cf.followup_doctor IS NOT NULL AND cf.followup_doctor_id IS NULL;

ALTER TABLE chronic_followup ADD INDEX idx_followup_doctor_id (followup_doctor_id);

-- ==================== 4. chronic_followup_plan: 新增 doctor_id ====================
ALTER TABLE chronic_followup_plan ADD COLUMN doctor_id INT DEFAULT NULL COMMENT '负责医生ID(关联doctor_info.id)' AFTER doctor_name;

UPDATE chronic_followup_plan cfp
INNER JOIN doctor_info d ON d.doctor_name = cfp.doctor_name AND d.status = 1
SET cfp.doctor_id = d.id
WHERE cfp.doctor_name IS NOT NULL AND cfp.doctor_id IS NULL;

ALTER TABLE chronic_followup_plan ADD INDEX idx_doctor_id (doctor_id);

-- ==================== 5. doctor_point: 回填缺失的 doctor_id ====================
UPDATE doctor_point dp
INNER JOIN doctor_info d ON d.doctor_name = dp.doctor_name AND d.status = 1
SET dp.doctor_id = d.id
WHERE dp.doctor_name IS NOT NULL AND dp.doctor_id IS NULL;

-- ==================== 验证 ====================
SELECT '=== 回填一致性检查 ===' AS '';
SELECT 'patient_history' AS tbl,
  COUNT(*) AS total,
  SUM(CASE WHEN doctor_id IS NULL AND patient_doctor IS NOT NULL THEN 1 ELSE 0 END) AS not_matched
FROM patient_history
UNION ALL
SELECT 'chronic_disease', COUNT(*),
  SUM(CASE WHEN doctor_id IS NULL AND doctor_name IS NOT NULL THEN 1 ELSE 0 END)
FROM chronic_disease
UNION ALL
SELECT 'chronic_followup', COUNT(*),
  SUM(CASE WHEN followup_doctor_id IS NULL AND followup_doctor IS NOT NULL THEN 1 ELSE 0 END)
FROM chronic_followup
UNION ALL
SELECT 'chronic_followup_plan', COUNT(*),
  SUM(CASE WHEN doctor_id IS NULL AND doctor_name IS NOT NULL THEN 1 ELSE 0 END)
FROM chronic_followup_plan
UNION ALL
SELECT 'doctor_point', COUNT(*),
  SUM(CASE WHEN doctor_id IS NULL AND doctor_name IS NOT NULL THEN 1 ELSE 0 END)
FROM doctor_point;

SELECT '=== 新增列结构确认 ===' AS '';
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'bs1'
  AND COLUMN_NAME IN ('doctor_id', 'followup_doctor_id')
  AND TABLE_NAME IN ('patient_history', 'chronic_disease', 'chronic_followup', 'chronic_followup_plan', 'doctor_point')
ORDER BY TABLE_NAME, COLUMN_NAME;

COMMIT;
