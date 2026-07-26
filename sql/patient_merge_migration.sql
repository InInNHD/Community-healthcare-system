-- =============================================
-- 居民数据整合迁移
-- 1. 将测试名替换为真人姓名
-- 2. 统一身份证号，补齐 patient_info 缺失记录
-- 3. 修复所有关联表（6表、~139条记录）
-- 4. 与居民端(patient_info/sys_user)完全对齐
-- =============================================

-- ===== 映射表 =====
-- 王竑超 → 张建国  男 68岁  320102195708151234  user_id=68  account=patient_212114
-- 冲突测试 → 李明华  男 58岁  320102196803201133  user_id=67  account=patient_011133
-- 王洪超 → 陈秀兰  女 62岁  320102196403151234  user_id=48  account=patient
-- hy     → 赵志强  男 71岁  320102195508081234  user_id=49  account=hy

START TRANSACTION;

-- ==================== 1. sys_user 改名 ====================
UPDATE sys_user SET name = '张建国' WHERE id = 68 AND name = '王竑超';
UPDATE sys_user SET name = '李明华' WHERE id = 67 AND name = '冲突测试';
UPDATE sys_user SET name = '陈秀兰' WHERE id = 48 AND name = '王洪超';
UPDATE sys_user SET name = '赵志强' WHERE id = 49 AND name = 'hy';

-- ==================== 2. patient_info 重建 ====================
-- 2a. 改名
UPDATE patient_info SET paient_name = '张建国' WHERE paient_idcard = '52212114';
UPDATE patient_info SET paient_name = '李明华' WHERE paient_idcard = '320102199901011133';

-- 2b. 张建国: 更新身份证号（先改关联表，再改主键）
--    （关联表在下面统一更新）

-- 2c. 陈秀兰: 补齐 patient_info 记录（原来只有 sys_user 无 patient_info）
INSERT INTO patient_info (paient_idcard, paient_name, paient_money, user_id)
SELECT '320102196403151234', '陈秀兰', '8500', 48
WHERE NOT EXISTS (SELECT 1 FROM patient_info WHERE user_id = 48);

-- 2d. 赵志强: 补齐 patient_info 记录
INSERT INTO patient_info (paient_idcard, paient_name, paient_money, user_id)
SELECT '320102195508081234', '赵志强', '3200', 49
WHERE NOT EXISTS (SELECT 1 FROM patient_info WHERE user_id = 49);

-- ==================== 3. chronic_disease 更新 ====================
-- 张建国：ID 52212114 → 320102195708151234
UPDATE chronic_disease SET patient_idcard = '320102195708151234', patient_name = '张建国'
WHERE patient_name = '王竑超';

-- 李明华：身份证不变，只改名
UPDATE chronic_disease SET patient_name = '李明华'
WHERE patient_name = '冲突测试';

-- 陈秀兰：ID 320102197803151234 → 320102196403151234
UPDATE chronic_disease SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

-- 赵志强：ID 320102199508081234 → 320102195508081234
UPDATE chronic_disease SET patient_idcard = '320102195508081234', patient_name = '赵志强'
WHERE patient_name = 'hy';

-- ==================== 4. chronic_followup 更新 ====================
UPDATE chronic_followup SET patient_idcard = '320102195708151234', patient_name = '张建国'
WHERE patient_name = '王竑超';

UPDATE chronic_followup SET patient_name = '李明华'
WHERE patient_name = '冲突测试';

UPDATE chronic_followup SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

UPDATE chronic_followup SET patient_idcard = '320102195508081234', patient_name = '赵志强'
WHERE patient_name = 'hy';

-- ==================== 5. chronic_followup_plan 更新 ====================
UPDATE chronic_followup_plan SET patient_idcard = '320102195708151234', patient_name = '张建国'
WHERE patient_name = '王竑超';

UPDATE chronic_followup_plan SET patient_name = '李明华'
WHERE patient_name = '冲突测试';

UPDATE chronic_followup_plan SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

UPDATE chronic_followup_plan SET patient_idcard = '320102195508081234', patient_name = '赵志强'
WHERE patient_name = 'hy';

-- ==================== 6. patient_health 更新 ====================
UPDATE patient_health SET patient_idcard = '320102195708151234', patient_name = '张建国'
WHERE patient_name = '王竑超';

UPDATE patient_health SET patient_name = '李明华'
WHERE patient_name = '冲突测试';

UPDATE patient_health SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

UPDATE patient_health SET patient_idcard = '320102195508081234', patient_name = '赵志强'
WHERE patient_name = 'hy';

-- ==================== 7. patient_history 更新 ====================
UPDATE patient_history SET patient_idcard = '320102195708151234', patient_name = '张建国'
WHERE patient_name = '王竑超';

UPDATE patient_history SET patient_name = '李明华'
WHERE patient_name = '冲突测试';

UPDATE patient_history SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

UPDATE patient_history SET patient_idcard = '320102195508081234', patient_name = '赵志强'
WHERE patient_name = 'hy';

-- ==================== 8. doctor_point 更新 ====================
UPDATE doctor_point SET patient_idcard = '320102196403151234', patient_name = '陈秀兰'
WHERE patient_name = '王洪超';

-- ==================== 9. 更新 patient_info 主键（最后操作，避免中途引用丢失）====================
UPDATE patient_info SET paient_idcard = '320102195708151234'
WHERE paient_idcard = '52212114' AND paient_name = '张建国';

-- ==================== 10. 验证 ====================
SELECT '=== patient_info ===' AS '';
SELECT paient_idcard, paient_name, paient_money, user_id FROM patient_info ORDER BY user_id;

SELECT '=== sys_user(居民) ===' AS '';
SELECT id, account, name, roleid FROM sys_user WHERE roleid='6' ORDER BY id;

SELECT '=== 病种分布 ===' AS '';
SELECT disease_type, COUNT(*) AS cnt FROM chronic_disease WHERE status=1 GROUP BY disease_type ORDER BY cnt DESC;

SELECT '=== 各表引用一致性检查 ===' AS '';
SELECT 'chronic_disease' AS tbl, COUNT(*) AS cnt FROM chronic_disease WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强')
UNION ALL
SELECT 'chronic_followup', COUNT(*) FROM chronic_followup WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强')
UNION ALL
SELECT 'chronic_followup_plan', COUNT(*) FROM chronic_followup_plan WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强')
UNION ALL
SELECT 'patient_health', COUNT(*) FROM patient_health WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强')
UNION ALL
SELECT 'patient_history', COUNT(*) FROM patient_history WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强')
UNION ALL
SELECT 'doctor_point', COUNT(*) FROM doctor_point WHERE patient_name IN ('张建国','李明华','陈秀兰','赵志强');

SELECT '=== 是否残留旧名 ===' AS '';
SELECT 'chronic_disease' AS tbl, COUNT(*) AS remaining FROM chronic_disease WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy')
UNION ALL
SELECT 'chronic_followup', COUNT(*) FROM chronic_followup WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy')
UNION ALL
SELECT 'chronic_followup_plan', COUNT(*) FROM chronic_followup_plan WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy')
UNION ALL
SELECT 'patient_health', COUNT(*) FROM patient_health WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy')
UNION ALL
SELECT 'patient_history', COUNT(*) FROM patient_history WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy')
UNION ALL
SELECT 'doctor_point', COUNT(*) FROM doctor_point WHERE patient_name IN ('王竑超','冲突测试','王洪超','hy');

COMMIT;
