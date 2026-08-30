-- =============================================
-- 医生姓名实名化 + 管理员端数据整合
-- 赵医生 → 赵明德（内科主任医师）
-- 人参医生 → 何伟民（儿科主治医师）
-- =============================================

START TRANSACTION;

-- ==================== 1. 赵医生 → 赵明德 ====================

-- sys_user
UPDATE sys_user SET name = '赵明德' WHERE id = 47 AND name = '赵医生';

-- doctor_info
UPDATE doctor_info SET doctor_name = '赵明德' WHERE id = 1 AND doctor_name = '赵医生';

-- chronic_disease
UPDATE chronic_disease SET doctor_name = '赵明德' WHERE doctor_name = '赵医生';

-- chronic_followup
UPDATE chronic_followup SET followup_doctor = '赵明德' WHERE followup_doctor = '赵医生';

-- chronic_followup_plan
UPDATE chronic_followup_plan SET doctor_name = '赵明德' WHERE doctor_name = '赵医生';

-- patient_history
UPDATE patient_history SET patient_doctor = '赵明德' WHERE patient_doctor = '赵医生';

-- doctor_point
UPDATE doctor_point SET doctor_name = '赵明德' WHERE doctor_name = '赵医生';

-- ==================== 2. 人参医生 → 何伟民 ====================

-- sys_user
UPDATE sys_user SET name = '何伟民' WHERE id = 54 AND name = '人参医生';

-- doctor_info
UPDATE doctor_info SET doctor_name = '何伟民' WHERE id = 15 AND doctor_name = '人参医生';

-- ==================== 3. 验证 ====================

SELECT '=== 旧名残留检查 ===' AS '';
SELECT 'sys_user' AS tbl, COUNT(*) AS remaining FROM sys_user WHERE name IN ('赵医生','人参医生')
UNION ALL
SELECT 'doctor_info', COUNT(*) FROM doctor_info WHERE doctor_name IN ('赵医生','人参医生')
UNION ALL
SELECT 'chronic_disease', COUNT(*) FROM chronic_disease WHERE doctor_name IN ('赵医生','人参医生')
UNION ALL
SELECT 'chronic_followup', COUNT(*) FROM chronic_followup WHERE followup_doctor IN ('赵医生','人参医生')
UNION ALL
SELECT 'chronic_followup_plan', COUNT(*) FROM chronic_followup_plan WHERE doctor_name IN ('赵医生','人参医生')
UNION ALL
SELECT 'patient_history', COUNT(*) FROM patient_history WHERE patient_doctor IN ('赵医生','人参医生')
UNION ALL
SELECT 'doctor_point', COUNT(*) FROM doctor_point WHERE doctor_name IN ('赵医生','人参医生');

SELECT '=== 全部医生(doctor_info + sys_user) ===' AS '';
SELECT d.id, d.doctor_name, d.department, d.title,
       u.account, u.name AS login_name, u.status
FROM doctor_info d
LEFT JOIN sys_user u ON d.user_id = u.id
WHERE d.status = 1
ORDER BY d.id;

SELECT '=== 业务表医生引用统计 ===' AS '';
SELECT 'chronic_disease' AS tbl, doctor_name, COUNT(*) AS cnt FROM chronic_disease GROUP BY doctor_name
UNION ALL
SELECT 'chronic_followup', followup_doctor, COUNT(*) FROM chronic_followup GROUP BY followup_doctor
UNION ALL
SELECT 'chronic_followup_plan', doctor_name, COUNT(*) FROM chronic_followup_plan GROUP BY doctor_name
UNION ALL
SELECT 'patient_history', patient_doctor, COUNT(*) FROM patient_history GROUP BY patient_doctor
UNION ALL
SELECT 'doctor_point', doctor_name, COUNT(*) FROM doctor_point GROUP BY doctor_name;

COMMIT;
