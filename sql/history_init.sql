-- ============================================================
-- 社区医院就诊记录 — 与健康档案配套的就诊数据
-- 覆盖全部4位居民，每人2-5次就诊（2025-2026年）
-- 清空旧数据，插入新的合理就诊记录
-- 数据库：bs1
-- ============================================================
SET NAMES utf8;

DELETE FROM patient_history;

-- 冲突测试 (27F): 感冒+皮肤过敏
INSERT INTO patient_history (patient_idcard, patient_name, patient_sym, patient_doctor, patient_medicine, patient_history_date, takeprice) VALUES
('320102199901011133', '冲突测试', '鼻塞流涕三天，伴打喷嚏', '赵医生', '连花清瘟胶囊 0.35gx24', '2025-03-15 09:30:00', 26),
('320102199901011133', '冲突测试', '皮肤瘙痒，红疹一周', '郑国安', '开瑞坦 10mgx6 + 红霉素眼膏', '2025-12-05 14:15:00', 33);

-- 王竑超 (55M): 高血压慢病管理+急性肠胃炎
INSERT INTO patient_history (patient_idcard, patient_name, patient_sym, patient_doctor, patient_medicine, patient_history_date, takeprice) VALUES
('52212114', '王竑超', '头晕头胀两周，血压偏高', '赵医生', '复方丹参滴丸 27mgx180 + 降压药', '2025-01-10 10:00:00', 96),
('52212114', '王竑超', '胸闷、心慌，活动后加重', '赵医生', '速效救心丸 40mg + 复方丹参滴丸', '2025-07-25 08:45:00', 80),
('52212114', '王竑超', '恶心呕吐，腹泻一天', '王建国', '藿香正气水 10mlx10 + 蒙脱石散 3gx6', '2026-04-22 16:30:00', 34);

-- 王洪超 (48M): 颈椎病+高血脂+感冒
INSERT INTO patient_history (patient_idcard, patient_name, patient_sym, patient_doctor, patient_medicine, patient_history_date, takeprice) VALUES
('0', '王洪超', '颈椎酸痛两月，伏案工作加重', '周大勇', '云南白药气雾剂 85g + 推拿', '2025-05-20 14:00:00', 92),
('0', '王洪超', '年度体检，血脂偏高', '赵医生', '复方丹参滴丸 27mgx180', '2025-11-12 10:30:00', 32),
('0', '王洪超', '头痛乏力', '赵医生', '布洛芬缓释胶囊 0.3gx20', '2026-05-04 04:23:13', 50),
('0', '王洪超', '感冒', '赵医生', '板蓝根颗粒 10gx20', '2026-05-06 01:12:37', 12),
('0', '王洪超', '感冒复发', '赵医生', '阿莫西林胶囊 0.5gx24', '2026-05-06 17:07:16', 11);

-- hy (35F): 轻度贫血+免疫力低下
INSERT INTO patient_history (patient_idcard, patient_name, patient_sym, patient_doctor, patient_medicine, patient_history_date, takeprice) VALUES
('0', 'hy', '头晕乏力两月，面色苍白', '李秀华', '维生素C片 100mgx100 + 六味地黄丸', '2025-06-15 11:20:00', 40),
('0', 'hy', '反复感冒，免疫力下降', '赵医生', '板蓝根颗粒 10gx20 + 连花清瘟胶囊', '2026-03-10 09:00:00', 38);
