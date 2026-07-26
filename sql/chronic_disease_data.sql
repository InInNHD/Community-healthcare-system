-- =============================================
-- 慢病管理模块 - 测试数据填充
-- 基于现有用户和医生信息生成
-- 参考南京"超能家医"模式
-- =============================================

-- 清空现有测试数据（保留表结构）
DELETE FROM chronic_followup_plan;
DELETE FROM chronic_followup;
DELETE FROM chronic_disease;
ALTER TABLE chronic_disease AUTO_INCREMENT = 1;
ALTER TABLE chronic_followup AUTO_INCREMENT = 1;
ALTER TABLE chronic_followup_plan AUTO_INCREMENT = 1;

-- ==================== 1. 慢病档案 ====================
-- 分配原则：4位居民 x 5~6个病种，共23条档案，覆盖每种病3-5例

-- 患者一：王竑超 (52212114) — 5种慢病
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('52212114', '王竑超', '高血压', '高风险', '2022-05-10', '赵医生', 1, '2022-05-10', '肥胖(BMI 31)，有家族史，血压控制不理想'),
('52212114', '王竑超', '糖尿病', '高风险', '2022-08-15', '陈慧芳', 1, '2022-08-15', 'II型糖尿病，合并高血压，HbA1c 9.2%，需胰岛素治疗'),
('52212114', '王竑超', '冠心病', '中风险', '2023-01-20', '李秀华', 1, '2023-01-20', '稳定性心绞痛，冠脉CTA示前降支中度狭窄'),
('52212114', '王竑超', '慢阻肺', '中风险', '2023-06-05', '周大勇', 1, '2023-06-05', '40年吸烟史，FEV1=62%预计值，活动后气促'),
('52212114', '王竑超', '慢性肾病', '高风险', '2024-03-12', '孙丽萍', 1, '2024-03-12', 'CKD 4期，eGFR=26，大量蛋白尿3.8g/24h');

-- 患者二：冲突测试 (320102199901011133) — 4种慢病
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('320102199901011133', '冲突测试', '高血压', '中风险', '2023-02-14', '李秀华', 1, '2023-02-14', '舒张压偏高(100-105)，工作压力大，睡眠差'),
('320102199901011133', '冲突测试', '糖尿病', '低风险', '2024-06-20', '陈慧芳', 1, '2024-06-20', '新诊断，糖耐量异常，空腹血糖7.2mmol/L，饮食控制中'),
('320102199901011133', '冲突测试', '脑卒中', '高风险', '2023-09-08', '王建国', 1, '2023-09-08', '缺血性脑卒中，NIHSS评分16，左侧肢体偏瘫，恢复期康复中'),
('320102199901011133', '冲突测试', '冠心病', '低风险', '2025-01-10', '李秀华', 1, '2025-01-10', '偶发胸闷，运动平板阴性，定期随访观察');

-- 患者三：王洪超 (无身份证号，用user_id关联) — 4种慢病
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('320102197803151234', '王洪超', '糖尿病', '中风险', '2021-11-08', '黄美玲', 1, '2021-11-08', '糖尿病史5年，合并视网膜病变I期，HbA1c 8.1%'),
('320102197803151234', '王洪超', '高血压', '低风险', '2022-03-22', '周大勇', 1, '2022-03-22', '轻度高血压，收缩压140-145，生活方式干预为主'),
('320102197803151234', '王洪超', '慢性肾病', '中风险', '2024-07-15', '孙丽萍', 1, '2024-07-15', 'CKD 3a期，eGFR=52，微量白蛋白尿，糖尿病肾病可能'),
('320102197803151234', '王洪超', '脑卒中', '低风险', '2025-04-01', '王建国', 1, '2025-04-01', 'TIA发作1次，NIHSS=2，二级预防良好');

-- 患者四：hy — 3种慢病
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('320102199508081234', 'hy', '慢阻肺', '高风险', '2023-04-18', '周大勇', 1, '2023-04-18', '重度COPD，FEV1=38%预计值，长期家庭氧疗'),
('320102199508081234', 'hy', '冠心病', '中风险', '2024-02-28', '李秀华', 1, '2024-02-28', '非ST段抬高型心梗史，PCI术后1年，双联抗血小板中'),
('320102199508081234', 'hy', '高血压', '中风险', '2023-04-18', '赵医生', 1, '2023-04-18', '与COPD合并，收缩压155-165，需联合用药');

-- 额外：为覆盖六病种的多样性，给冲突测试补一条慢性肾病
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('320102199901011133', '冲突测试', '慢性肾病', '低风险', '2025-08-20', '孙丽萍', 1, '2025-08-20', 'CKD 1期，eGFR=82，体检发现微量蛋白尿，定期随访');

-- 额外：给王竑超补脑卒中以覆盖全病种
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('52212114', '王竑超', '脑卒中', '低风险', '2025-11-30', '王建国', 2, '2025-11-30', '腔隙性脑梗，NIHSS=3，恢复良好已转诊康复科');

-- 额外：已结案的高血压病例（测试结案状态）
INSERT INTO chronic_disease (patient_idcard, patient_name, disease_type, risk_level, diagnosis_date, doctor_name, status, create_time, remark) VALUES
('52212114', '王竑超', '高血压', '低风险', '2021-01-05', '赵医生', 3, '2021-01-05', '早期轻度高血压，生活方式干预后血压恢复正常，已结案');

-- ==================== 2. 随访记录 ====================
-- 生成覆盖2025年1月至2026年5月的随访记录，每档案2-6条

-- 王竑超 - 高血压(高风险) ID=1
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_pressure, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(1, '52212114', '王竑超', '高血压', '2025-06-10', '赵医生', '门诊', '偶有头晕、后颈僵硬', '168/105', 82, '一般', '严格低盐饮食(<5g/日)，每日自测血压并记录，增加有氧运动', '高风险', 1),
(1, '52212114', '王竑超', '高血压', '2025-08-10', '赵医生', '门诊', '头晕减轻，睡眠改善', '155/98', 78, '良好', '继续保持低盐饮食，体重已减2kg', '高风险', 1),
(1, '52212114', '王竑超', '高血压', '2025-10-12', '赵医生', '电话', '无明显不适', '150/95', 76, '良好', '继续用药，注意天气转凉时监测血压', '高风险', 1),
(1, '52212114', '王竑超', '高血压', '2025-12-15', '赵医生', '门诊', '冬季血压偏高，偶有心悸', '162/102', 85, '良好', '冬季注意保暖，增加氨氯地平剂量至5mg', '高风险', 1),
(1, '52212114', '王竑超', '高血压', '2026-02-15', '赵医生', '门诊', '血压较前稳定', '152/96', 74, '良好', '体重累计降5kg，继续坚持', '高风险', 1),
(1, '52212114', '王竑超', '高血压', '2026-04-18', '赵医生', '线上', '无明显症状', '148/92', 72, '良好', '继续保持，夏季适当减量但勿停药', '高风险', 1);

-- 王竑超 - 糖尿病(高风险) ID=2
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_sugar, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(2, '52212114', '王竑超', '糖尿病', '2025-07-01', '陈慧芳', '门诊', '多饮多尿，体重下降3kg', '9.8mmol/L', '差', '严格控制碳水分摄入(<150g/日)，规律监测血糖，增加二甲双胍剂量', '高风险', 1),
(2, '52212114', '王竑超', '糖尿病', '2025-09-05', '陈慧芳', '门诊', '症状改善，体重稳定', '8.2mmol/L', '良好', '继续饮食控制，餐后步行30min', '高风险', 1),
(2, '52212114', '王竑超', '糖尿病', '2025-11-08', '陈慧芳', '家庭', '无明显异常', '7.5mmol/L', '良好', '注意足部护理，穿合适鞋袜，发现破溃及时就医', '中风险', 1),
(2, '52212114', '王竑超', '糖尿病', '2026-01-10', '陈慧芳', '门诊', '偶感手脚麻木', '7.1mmol/L', '良好', '糖尿病周围神经病变筛查，补充B族维生素', '中风险', 1),
(2, '52212114', '王竑超', '糖尿病', '2026-03-15', '陈慧芳', '线上', '稳定', '6.8mmol/L', '良好', 'HbA1c降至7.8%，继续保持', '低风险', 1);

-- 王竑超 - 冠心病(中风险) ID=3
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(3, '52212114', '王竑超', '冠心病', '2025-08-20', '李秀华', '门诊', '饭后偶有胸闷，休息可缓解', 76, '一般', '低脂饮食，避免过饱和剧烈运动，随身携带硝酸甘油', '中风险', 1),
(3, '52212114', '王竑超', '冠心病', '2025-11-22', '李秀华', '门诊', '胸闷发作减少', 72, '良好', '继续药物治疗，心功能良好', '中风险', 1),
(3, '52212114', '王竑超', '冠心病', '2026-02-25', '李秀华', '电话', '稳定，无不适', 74, '良好', '坚持服药，定期复查血脂', '中风险', 1);

-- 王竑超 - 慢阻肺(中风险) ID=4
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(4, '52212114', '王竑超', '慢阻肺', '2025-07-15', '周大勇', '门诊', '活动后气促，偶有咳嗽咳痰', '良好', '正确使用吸入剂(SABA+ICS)，腹式呼吸训练，接种流感疫苗', '中风险', 1),
(4, '52212114', '王竑超', '慢阻肺', '2025-10-18', '周大勇', '门诊', '咳嗽减轻，活动耐力改善', '良好', '坚持呼吸康复锻炼，戒烟成功整1年', '低风险', 1),
(4, '52212114', '王竑超', '慢阻肺', '2026-01-20', '周大勇', '电话', '冬季症状稍重，夜间偶有喘息', '良好', '冬季减少外出，室内保持湿度，必要时临时加用SABA', '低风险', 1);

-- 王竑超 - 慢性肾病(高风险) ID=5
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(5, '52212114', '王竑超', '慢性肾病', '2024-05-20', '孙丽萍', '门诊', '下肢轻度水肿，乏力', '一般', '优质低蛋白饮食(0.6g/kg/日)，限盐限钾，监测24h尿蛋白', '高风险', 1),
(5, '52212114', '王竑超', '慢性肾病', '2024-07-25', '孙丽萍', '门诊', '水肿改善，食欲尚可', '良好', 'eGFR稳定在28，继续控制蛋白摄入，避免肾毒性药物', '高风险', 1),
(5, '52212114', '王竑超', '慢性肾病', '2024-10-30', '孙丽萍', '电话', '无明显不适', '良好', '定期复查肾功能、电解质、尿常规', '高风险', 1),
(5, '52212114', '王竑超', '慢性肾病', '2025-01-05', '孙丽萍', '门诊', '肌酐略升，尿素氮偏高', '一般', '严格控制蛋白摄入，准备动静脉瘘评估', '高风险', 1);

-- 冲突测试 - 高血压(中风险) ID=6
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_pressure, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(6, '320102199901011133', '冲突测试', '高血压', '2025-05-20', '李秀华', '门诊', '工作压力大，睡眠差，偶有头痛', '158/100', 88, '一般', '合理安排作息，学会减压，必要时心理咨询', '中风险', 1),
(6, '320102199901011133', '冲突测试', '高血压', '2025-08-22', '李秀华', '门诊', '头痛减轻，睡眠改善', '148/96', 82, '良好', '继续用药，增加运动量', '中风险', 1),
(6, '320102199901011133', '冲突测试', '高血压', '2025-11-25', '李秀华', '线上', '血压稳定', '142/90', 78, '良好', '减盐成功，继续坚持', '低风险', 1),
(6, '320102199901011133', '冲突测试', '高血压', '2026-02-28', '李秀华', '电话', '无明显症状', '138/88', 76, '良好', '目标血压<140/90，近期达标良好', '低风险', 1);

-- 冲突测试 - 脑卒中(高风险) ID=8
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(8, '320102199901011133', '冲突测试', '脑卒中', '2025-04-15', '王建国', '家庭', '左侧肢体肌力III级，言语稍含糊', '良好', '坚持康复训练，家属辅助关节活动度训练，防跌倒', '高风险', 1),
(8, '320102199901011133', '冲突测试', '脑卒中', '2025-06-20', '王建国', '门诊', '肢体功能改善，肌力IV-级', '良好', '加强康复训练频率，言语治疗1次/周', '中风险', 1),
(8, '320102199901011133', '冲突测试', '脑卒中', '2025-08-25', '王建国', '门诊', '可扶拐行走，言语较前清晰', '良好', '继续康复，口服抗血小板药勿停', '中风险', 1),
(8, '320102199901011133', '冲突测试', '脑卒中', '2025-10-28', '王建国', '电话', '稳定进步中', '良好', '社区康复中心继续锻炼', '中风险', 1),
(8, '320102199901011133', '冲突测试', '脑卒中', '2026-01-05', '王建国', '门诊', '可独立行走，言语基本清晰', '良好', 'NIHSS降至6分，考虑职业康复', '低风险', 1);

-- 王洪超 - 糖尿病(中风险) ID=10
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_sugar, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(10, '320102197803151234', '王洪超', '糖尿病', '2025-06-18', '黄美玲', '门诊', '视物模糊，手足偶有麻木', '8.8mmol/L', '一般', '眼底检查，控制血糖波动，补充硫辛酸', '中风险', 1),
(10, '320102197803151234', '王洪超', '糖尿病', '2025-09-20', '黄美玲', '门诊', '视物模糊改善，血糖控制好转', '7.6mmol/L', '良好', '眼底稳定，继续降糖方案', '中风险', 1),
(10, '320102197803151234', '王洪超', '糖尿病', '2026-01-15', '黄美玲', '电话', '自述血糖控制稳定', '7.2mmol/L', '良好', 'HbA1c 7.6%，距目标仍有差距', '中风险', 1);

-- 王洪超 - 慢性肾病(中风险) ID=12
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(12, '320102197803151234', '王洪超', '慢性肾病', '2024-09-20', '孙丽萍', '门诊', '无明显症状', '良好', '低蛋白饮食，eGFR稳定在52，定期监测', '中风险', 1),
(12, '320102197803151234', '王洪超', '慢性肾病', '2025-01-25', '孙丽萍', '门诊', '偶有乏力', '良好', 'eGFR=50，微降，注意休息避免劳累', '中风险', 1),
(12, '320102197803151234', '王洪超', '慢性肾病', '2025-06-15', '孙丽萍', '电话', '稳定', '良好', 'eGFR=51，控制血糖血压对保肾很重要', '中风险', 1);

-- hy - 慢阻肺(高风险) ID=14
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(14, '320102199508081234', 'hy', '慢阻肺', '2025-05-12', '周大勇', '门诊', '明显活动后气喘，咳黄痰', '一般', '加强吸入治疗，必要时短期口服激素，痰培养+药敏', '高风险', 1),
(14, '320102199508081234', 'hy', '慢阻肺', '2025-07-18', '周大勇', '家庭', '咳嗽咳痰好转', '良好', '继续家庭氧疗>15h/日，避免吸烟环境', '高风险', 1),
(14, '320102199508081234', 'hy', '慢阻肺', '2025-09-22', '周大勇', '门诊', '换季稍有加重', '良好', '秋季接种流感+肺炎疫苗', '高风险', 1),
(14, '320102199508081234', 'hy', '慢阻肺', '2025-11-28', '周大勇', '电话', '入冬后气促加重', '一般', '冬季注意保暖，必要时加用噻托溴铵', '高风险', 1),
(14, '320102199508081234', 'hy', '慢阻肺', '2026-01-12', '周大勇', '门诊', '冬季加重经治疗后好转', '良好', 'FEV1=40%，比去年略改善', '高风险', 1),
(14, '320102199508081234', 'hy', '慢阻肺', '2026-03-20', '周大勇', '线上', '春季症状减轻', '良好', '继续康复锻炼，呼吸肌训练', '高风险', 1);

-- hy - 冠心病(中风险) ID=15
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(15, '320102199508081234', 'hy', '冠心病', '2025-07-10', '李秀华', '门诊', '偶有胸闷，活动后明显', 80, '良好', '控制活动强度，定期心电图', '中风险', 1),
(15, '320102199508081234', 'hy', '冠心病', '2025-10-15', '李秀华', '门诊', '胸闷减轻，心肌酶正常', 76, '良好', '坚持双联抗血小板，控制血脂LDL<1.8', '中风险', 1),
(15, '320102199508081234', 'hy', '冠心病', '2026-02-20', '李秀华', '电话', '稳定，无胸闷', 78, '良好', '术后满1年，可考虑降级为单抗', '低风险', 1);

-- 冲突测试 - 糖尿病(低风险) ID=7
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_sugar, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(7, '320102199901011133', '冲突测试', '糖尿病', '2025-10-15', '陈慧芳', '门诊', '无明显症状', '6.9mmol/L', '良好', '糖耐量异常期，饮食运动控制效果好，继续努力', '低风险', 1),
(7, '320102199901011133', '冲突测试', '糖尿病', '2026-04-20', '陈慧芳', '线上', '稳定', '6.5mmol/L', '良好', '空腹血糖接近正常，坚持生活方式干预', '低风险', 1);

-- 冲突测试 - 冠心病(低风险) ID=9
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(9, '320102199901011133', '冲突测试', '冠心病', '2025-05-10', '李秀华', '门诊', '偶发胸闷，持续时间短', 72, '良好', '运动平板阴性，继续观察', '低风险', 1),
(9, '320102199901011133', '冲突测试', '冠心病', '2026-02-10', '李秀华', '电话', '无明显症状', 70, '良好', '心电图正常，保持健康生活方式', '低风险', 1);

-- 王洪超 - 高血压(低风险) ID=11
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_pressure, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(11, '320102197803151234', '王洪超', '高血压', '2025-07-08', '周大勇', '门诊', '无明显不适', '140/85', 74, '良好', '低盐饮食，每周运动3次以上', '低风险', 1),
(11, '320102197803151234', '王洪超', '高血压', '2026-01-10', '周大勇', '电话', '血压稳定', '136/84', 72, '良好', '继续保持，每年体检一次', '低风险', 1);

-- 王洪超 - 脑卒中(低风险) ID=13
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(13, '320102197803151234', '王洪超', '脑卒中', '2025-06-10', '王建国', '门诊', '无神经功能缺损', '良好', '继续阿司匹林二级预防，控制血压血糖', '低风险', 1),
(13, '320102197803151234', '王洪超', '脑卒中', '2025-12-15', '王建国', '门诊', '无异常', '良好', 'NIHSS=1，恢复极好，继续保持', '低风险', 1);

-- hy - 高血压(中风险) ID=16
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, blood_pressure, heart_rate, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(16, '320102199508081234', 'hy', '高血压', '2025-08-15', '赵医生', '门诊', '头晕，与降压药副作用有关', '155/95', 80, '一般', '调整降压药方案，减少利尿剂剂量', '中风险', 1),
(16, '320102199508081234', 'hy', '高血压', '2025-11-20', '赵医生', '门诊', '头晕改善', '148/90', 76, '良好', '新方案效果良好', '中风险', 1),
(16, '320102199508081234', 'hy', '高血压', '2026-02-25', '赵医生', '线上', '血压稳定', '144/88', 74, '良好', '继续当前方案', '中风险', 1);

-- 冲突测试 - 慢性肾病(低风险) ID=17
INSERT INTO chronic_followup (chronic_id, patient_idcard, patient_name, disease_type, followup_date, followup_doctor, followup_type, symptoms, medication_compliance, lifestyle_advice, risk_level, status) VALUES
(17, '320102199901011133', '冲突测试', '慢性肾病', '2025-11-25', '孙丽萍', '门诊', '无不适', '良好', 'eGFR=80，尿蛋白(-)，继续定期随访', '低风险', 1),
(17, '320102199901011133', '冲突测试', '慢性肾病', '2026-04-30', '孙丽萍', '电话', '无异常', '良好', '定期查肾功能和尿常规', '低风险', 1);

-- ==================== 3. 随访计划 ====================
-- 每条档案配1条待执行/已执行/已过期计划，覆盖各种状态

-- 王竑超 高血压(高风险) - 待执行计划(近期)
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(1, '52212114', '王竑超', '高血压', '2026-05-18', '门诊', 0, '赵医生', NOW());

-- 王竑超 糖尿病(中→低风险) - 待执行计划
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(2, '52212114', '王竑超', '糖尿病', '2026-06-15', '门诊', 0, '陈慧芳', NOW());

-- 王竑超 冠心病(中风险) - 待执行计划
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(3, '52212114', '王竑超', '冠心病', '2026-05-25', '门诊', 0, '李秀华', NOW());

-- 王竑超 慢阻肺(低风险) - 待执行计划
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(4, '52212114', '王竑超', '慢阻肺', '2026-07-20', '电话', 0, '周大勇', NOW());

-- 王竑超 慢性肾病(高风险) - 待执行计划(14天后)
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(5, '52212114', '王竑超', '慢性肾病', '2026-05-25', '门诊', 0, '孙丽萍', NOW());

-- 冲突测试 高血压 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(6, '320102199901011133', '冲突测试', '高血压', '2026-05-28', '门诊', 0, '李秀华', NOW());

-- 冲突测试 糖尿病 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(7, '320102199901011133', '冲突测试', '糖尿病', '2026-07-20', '线上', 0, '陈慧芳', NOW());

-- 冲突测试 脑卒中 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(8, '320102199901011133', '冲突测试', '脑卒中', '2026-06-05', '家庭', 0, '王建国', NOW());

-- 冲突测试 冠心病 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(9, '320102199901011133', '冲突测试', '冠心病', '2026-08-10', '门诊', 0, '李秀华', NOW());

-- 王洪超 糖尿病 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(10, '320102197803151234', '王洪超', '糖尿病', '2026-06-15', '门诊', 0, '黄美玲', NOW());

-- 王洪超 高血压 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(11, '320102197803151234', '王洪超', '高血压', '2026-08-10', '电话', 0, '周大勇', NOW());

-- 王洪超 慢性肾病 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(12, '320102197803151234', '王洪超', '慢性肾病', '2026-06-15', '门诊', 0, '孙丽萍', NOW());

-- 王洪超 脑卒中 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(13, '320102197803151234', '王洪超', '脑卒中', '2026-06-10', '门诊', 0, '王建国', NOW());

-- hy 慢阻肺 - 待执行(2周后，高风险)
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(14, '320102199508081234', 'hy', '慢阻肺', '2026-05-25', '门诊', 0, '周大勇', NOW());

-- hy 冠心病(转为低风险) - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(15, '320102199508081234', 'hy', '冠心病', '2026-05-20', '门诊', 0, '李秀华', NOW());

-- hy 高血压 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(16, '320102199508081234', 'hy', '高血压', '2026-05-28', '门诊', 0, '赵医生', NOW());

-- 冲突测试 慢性肾病 - 待执行
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(17, '320102199901011133', '冲突测试', '慢性肾病', '2026-07-30', '门诊', 0, '孙丽萍', NOW());

-- ===== 已执行的计划（对应上面的随访记录）=====
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(1, '52212114', '王竑超', '高血压', '2026-04-18', '线上', 1, '赵医生', '2026-04-01'),
(2, '52212114', '王竑超', '糖尿病', '2026-03-15', '线上', 1, '陈慧芳', '2026-03-01'),
(3, '52212114', '王竑超', '冠心病', '2026-02-25', '电话', 1, '李秀华', '2026-02-01');

-- ===== 已过期的计划（过去的时间，未执行）=====
INSERT INTO chronic_followup_plan (chronic_id, patient_idcard, patient_name, disease_type, plan_date, plan_type, status, doctor_name, create_time) VALUES
(8, '320102199901011133', '冲突测试', '脑卒中', '2026-03-01', '门诊', 2, '王建国', '2026-02-01'),
(14, '320102199508081234', 'hy', '慢阻肺', '2026-04-28', '电话', 2, '周大勇', '2026-04-10'),
(5, '52212114', '王竑超', '慢性肾病', '2026-04-15', '门诊', 2, '孙丽萍', '2026-03-25');

-- 验证统计
SELECT '=== 数据统计 ===' AS '';
SELECT 'chronic_disease' AS tbl, COUNT(*) AS cnt FROM chronic_disease
UNION ALL
SELECT 'chronic_followup', COUNT(*) FROM chronic_followup
UNION ALL
SELECT 'chronic_followup_plan', COUNT(*) FROM chronic_followup_plan;

SELECT '=== 病种分布 ===' AS '';
SELECT disease_type, COUNT(*) AS cnt FROM chronic_disease WHERE status=1 GROUP BY disease_type ORDER BY cnt DESC;

SELECT '=== 风险分布 ===' AS '';
SELECT risk_level, COUNT(*) AS cnt FROM chronic_disease WHERE status=1 GROUP BY risk_level ORDER BY FIELD(risk_level, '高风险','中风险','低风险');

SELECT '=== 随访计划状态 ===' AS '';
SELECT CASE status WHEN 0 THEN '待执行' WHEN 1 THEN '已执行' WHEN 2 THEN '已过期' END AS status_name, COUNT(*) AS cnt FROM chronic_followup_plan GROUP BY status;
