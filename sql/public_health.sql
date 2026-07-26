-- =============================================
-- 公共卫生服务模块
-- 1.预防接种 2.妇幼保健 3.老年人健康体检 4.传染病上报
-- =============================================

START TRANSACTION;

-- ==================== 1. 预防接种记录表 ====================
CREATE TABLE IF NOT EXISTS vaccination_record (
  id INT NOT NULL AUTO_INCREMENT,
  patient_idcard VARCHAR(50) DEFAULT NULL COMMENT '身份证号',
  patient_name VARCHAR(50) NOT NULL COMMENT '姓名',
  vaccine_name VARCHAR(100) NOT NULL COMMENT '疫苗名称',
  dose_seq INT DEFAULT 1 COMMENT '剂次',
  vacc_date DATE DEFAULT NULL COMMENT '接种日期',
  vacc_site VARCHAR(50) DEFAULT NULL COMMENT '接种部位',
  batch_no VARCHAR(50) DEFAULT NULL COMMENT '疫苗批号',
  manufacturer VARCHAR(100) DEFAULT NULL COMMENT '生产企业',
  vacc_doctor VARCHAR(50) DEFAULT NULL COMMENT '接种医生',
  next_date DATE DEFAULT NULL COMMENT '下次接种日期',
  status INT DEFAULT 1 COMMENT '1=已完成 0=计划中',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_patient_idcard (patient_idcard),
  KEY idx_vaccine (vaccine_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='预防接种记录';

-- ==================== 2. 国家免疫规划程序表 ====================
CREATE TABLE IF NOT EXISTS vaccination_schedule (
  id INT NOT NULL AUTO_INCREMENT,
  vaccine_name VARCHAR(100) NOT NULL COMMENT '疫苗名称',
  target_age VARCHAR(50) NOT NULL COMMENT '接种年龄',
  dose_seq INT DEFAULT 1 COMMENT '剂次',
  description VARCHAR(500) DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='免疫规划程序';

INSERT INTO vaccination_schedule (vaccine_name, target_age, dose_seq, description) VALUES
('乙肝疫苗', '出生24h内', 1, 'HepB-1'),
('乙肝疫苗', '1月龄', 2, 'HepB-2'),
('乙肝疫苗', '6月龄', 3, 'HepB-3'),
('卡介苗', '出生', 1, 'BCG'),
('脊灰灭活疫苗', '2月龄', 1, 'IPV-1'),
('脊灰灭活疫苗', '3月龄', 2, 'IPV-2'),
('脊灰减毒活疫苗', '4月龄', 1, 'bOPV-1'),
('脊灰减毒活疫苗', '4岁', 2, 'bOPV-2'),
('百白破疫苗', '3月龄', 1, 'DTaP-1'),
('百白破疫苗', '4月龄', 2, 'DTaP-2'),
('百白破疫苗', '5月龄', 3, 'DTaP-3'),
('百白破疫苗', '18月龄', 4, 'DTaP-4'),
('白破疫苗', '6岁', 1, 'DT'),
('麻腮风疫苗', '8月龄', 1, 'MMR-1'),
('麻腮风疫苗', '18月龄', 2, 'MMR-2'),
('乙脑减毒活疫苗', '8月龄', 1, 'JE-L-1'),
('乙脑减毒活疫苗', '2岁', 2, 'JE-L-2'),
('A群流脑多糖疫苗', '6月龄', 1, 'MPSV-A-1'),
('A群流脑多糖疫苗', '9月龄', 2, 'MPSV-A-2'),
('A+C群流脑多糖疫苗', '3岁', 1, 'MPSV-AC-1'),
('A+C群流脑多糖疫苗', '6岁', 2, 'MPSV-AC-2'),
('甲肝减毒活疫苗', '18月龄', 1, 'HepA-L'),
('水痘疫苗', '12月龄', 1, 'VarV-1'),
('水痘疫苗', '4岁', 2, 'VarV-2'),
('流感疫苗', '6月龄以上(每年)', 1, '流感季接种'),
('23价肺炎疫苗', '2岁以上', 1, 'PPV23'),
('HPV疫苗', '9-14岁', 1, '二剂次程序'),
('HPV疫苗', '9-14岁', 2, '二剂次程序(间隔6月)');

-- ==================== 3. 孕产妇建册表 ====================
CREATE TABLE IF NOT EXISTS maternal_record (
  id INT NOT NULL AUTO_INCREMENT,
  patient_idcard VARCHAR(50) DEFAULT NULL COMMENT '身份证号',
  patient_name VARCHAR(50) NOT NULL COMMENT '姓名',
  age INT DEFAULT NULL COMMENT '年龄',
  lmp_date DATE DEFAULT NULL COMMENT '末次月经',
  edd_date DATE DEFAULT NULL COMMENT '预产期',
  gravidity INT DEFAULT 1 COMMENT '孕次',
  parity INT DEFAULT 0 COMMENT '产次',
  blood_type VARCHAR(10) DEFAULT NULL COMMENT '血型',
  high_risk_flag TINYINT DEFAULT 0 COMMENT '高危妊娠标识',
  high_risk_reason VARCHAR(500) DEFAULT NULL COMMENT '高危因素',
  register_date DATE DEFAULT NULL COMMENT '建册日期',
  doctor_name VARCHAR(50) DEFAULT NULL COMMENT '建册医生',
  status INT DEFAULT 1 COMMENT '1=妊娠中 2=已分娩 3=已结案',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_patient_idcard (patient_idcard),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='孕产妇建册';

-- ==================== 4. 产后访视表 ====================
CREATE TABLE IF NOT EXISTS maternal_postpartum_visit (
  id INT NOT NULL AUTO_INCREMENT,
  maternal_id INT NOT NULL COMMENT '建册ID',
  patient_idcard VARCHAR(50) DEFAULT NULL,
  patient_name VARCHAR(50) NOT NULL,
  visit_date DATE DEFAULT NULL COMMENT '访视日期',
  visit_day INT DEFAULT NULL COMMENT '产后天数',
  lochia VARCHAR(50) DEFAULT NULL COMMENT '恶露情况',
  uterine_involution VARCHAR(50) DEFAULT NULL COMMENT '子宫复旧',
  wound_healing VARCHAR(50) DEFAULT NULL COMMENT '伤口愈合',
  breastfeeding VARCHAR(50) DEFAULT NULL COMMENT '母乳喂养',
  neonate_weight DECIMAL(5,2) DEFAULT NULL COMMENT '新生儿体重(kg)',
  neonate_jaundice VARCHAR(20) DEFAULT NULL COMMENT '新生儿黄疸',
  visit_doctor VARCHAR(50) DEFAULT NULL COMMENT '访视医生',
  advice VARCHAR(500) DEFAULT NULL COMMENT '指导建议',
  next_visit_date DATE DEFAULT NULL COMMENT '下次访视日期',
  status INT DEFAULT 1 COMMENT '1=已完成 0=计划中',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_maternal_id (maternal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='产后访视';

-- ==================== 5. 儿童体检表 ====================
CREATE TABLE IF NOT EXISTS child_checkup (
  id INT NOT NULL AUTO_INCREMENT,
  patient_idcard VARCHAR(50) DEFAULT NULL COMMENT '母亲身份证号',
  patient_name VARCHAR(50) DEFAULT NULL COMMENT '母亲姓名',
  child_name VARCHAR(50) NOT NULL COMMENT '儿童姓名',
  gender VARCHAR(5) DEFAULT NULL COMMENT '性别',
  birth_date DATE DEFAULT NULL COMMENT '出生日期',
  checkup_date DATE DEFAULT NULL COMMENT '体检日期',
  height DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  weight DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  head_circumference DECIMAL(5,1) DEFAULT NULL COMMENT '头围(cm)',
  hemoglobin DECIMAL(5,1) DEFAULT NULL COMMENT '血红蛋白(g/L)',
  development_assessment VARCHAR(50) DEFAULT NULL COMMENT '发育评估',
  nutrition_status VARCHAR(50) DEFAULT NULL COMMENT '营养状况',
  advice VARCHAR(500) DEFAULT NULL COMMENT '健康指导',
  doctor_name VARCHAR(50) DEFAULT NULL COMMENT '体检医生',
  next_checkup_date DATE DEFAULT NULL COMMENT '下次体检',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_patient_idcard (patient_idcard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='儿童体检';

-- ==================== 6. 老年人健康体检表 ====================
CREATE TABLE IF NOT EXISTS elderly_checkup (
  id INT NOT NULL AUTO_INCREMENT,
  patient_idcard VARCHAR(50) DEFAULT NULL COMMENT '身份证号',
  patient_name VARCHAR(50) NOT NULL COMMENT '姓名',
  age INT DEFAULT NULL COMMENT '年龄',
  gender VARCHAR(5) DEFAULT NULL COMMENT '性别',
  checkup_date DATE DEFAULT NULL COMMENT '体检日期',
  height DECIMAL(5,1) DEFAULT NULL COMMENT '身高(cm)',
  weight DECIMAL(5,2) DEFAULT NULL COMMENT '体重(kg)',
  bmi DECIMAL(4,1) DEFAULT NULL COMMENT 'BMI',
  blood_pressure VARCHAR(20) DEFAULT NULL COMMENT '血压',
  heart_rate INT DEFAULT NULL COMMENT '心率',
  blood_sugar DECIMAL(5,2) DEFAULT NULL COMMENT '空腹血糖',
  blood_lipid VARCHAR(100) DEFAULT NULL COMMENT '血脂',
  liver_function VARCHAR(100) DEFAULT NULL COMMENT '肝功能',
  kidney_function VARCHAR(100) DEFAULT NULL COMMENT '肾功能',
  ecg VARCHAR(100) DEFAULT NULL COMMENT '心电图',
  b_ultrasound VARCHAR(100) DEFAULT NULL COMMENT 'B超',
  urine_routine VARCHAR(100) DEFAULT NULL COMMENT '尿常规',
  vision_left DECIMAL(3,1) DEFAULT NULL COMMENT '左眼视力',
  vision_right DECIMAL(3,1) DEFAULT NULL COMMENT '右眼视力',
  self_care_assessment VARCHAR(20) DEFAULT NULL COMMENT '自理能力',
  health_assessment VARCHAR(50) DEFAULT NULL COMMENT '健康评价',
  advice VARCHAR(500) DEFAULT NULL COMMENT '健康指导',
  doctor_name VARCHAR(50) DEFAULT NULL COMMENT '体检医生',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_patient_idcard (patient_idcard),
  KEY idx_checkup_date (checkup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='老年人健康体检';

-- ==================== 7. 传染病报告卡 ====================
CREATE TABLE IF NOT EXISTS infectious_disease_report (
  id INT NOT NULL AUTO_INCREMENT,
  patient_idcard VARCHAR(50) DEFAULT NULL COMMENT '身份证号',
  patient_name VARCHAR(50) NOT NULL COMMENT '患者姓名',
  age INT DEFAULT NULL COMMENT '年龄',
  gender VARCHAR(5) DEFAULT NULL COMMENT '性别',
  disease_type VARCHAR(50) NOT NULL COMMENT '疾病名称',
  disease_category VARCHAR(10) NOT NULL COMMENT '传染病类别：甲/乙/丙',
  onset_date DATE DEFAULT NULL COMMENT '发病日期',
  diagnosis_date DATE DEFAULT NULL COMMENT '诊断日期',
  report_date DATE NOT NULL COMMENT '报告日期',
  report_doctor VARCHAR(50) DEFAULT NULL COMMENT '报告医生',
  report_hospital VARCHAR(100) DEFAULT NULL COMMENT '报告单位',
  symptoms VARCHAR(500) DEFAULT NULL COMMENT '主要症状',
  isolation_status VARCHAR(20) DEFAULT '否' COMMENT '是否隔离',
  close_contacts_count INT DEFAULT 0 COMMENT '密切接触者数',
  measures VARCHAR(500) DEFAULT NULL COMMENT '处置措施',
  status INT DEFAULT 1 COMMENT '1=已报告 2=已审核 3=已结案',
  is_deleted TINYINT(1) DEFAULT 0,
  create_time DATETIME DEFAULT NULL,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_disease (disease_type),
  KEY idx_category (disease_category),
  KEY idx_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='传染病报告卡';

-- ==================== 测试数据 ====================
-- 预防接种记录（关联现有居民）
INSERT INTO vaccination_record (patient_idcard, patient_name, vaccine_name, dose_seq, vacc_date, vacc_site, batch_no, manufacturer, vacc_doctor, next_date, status, create_time) VALUES
('320102196403151234', '陈秀兰', '流感疫苗', 1, '2025-10-15', '左上臂三角肌', 'FL20251001', '北京科兴', '赵明德', '2026-10-15', 1, NOW()),
('320102195708151234', '张建国', '流感疫苗', 1, '2025-10-18', '左上臂三角肌', 'FL20251001', '北京科兴', '赵明德', '2026-10-18', 1, NOW()),
('320102195708151234', '张建国', '23价肺炎疫苗', 1, '2025-11-01', '右上臂三角肌', 'PN20251101', '默沙东', '赵明德', NULL, 1, NOW()),
('320102195508081234', '赵志强', '流感疫苗', 1, '2025-10-02', '左上臂三角肌', 'FL20251001', '北京科兴', '何伟民', '2026-10-02', 1, NOW()),
('320102196803201133', '李明华', '流感疫苗', 1, '2025-11-20', '左上臂三角肌', 'FL20251101', '华兰生物', '王建国', NULL, 1, NOW());

-- 孕产妇建册
INSERT INTO maternal_record (patient_idcard, patient_name, age, lmp_date, edd_date, gravidity, parity, blood_type, high_risk_flag, register_date, doctor_name, status, create_time) VALUES
('320102196403151234', '陈秀兰', 62, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, 1, NOW());

UPDATE maternal_record SET patient_name='李芳', patient_idcard='320102199405151234', age=32, lmp_date='2025-12-01', edd_date='2026-09-08', gravidity=2, parity=1, blood_type='A', high_risk_flag=0, register_date='2026-01-10', doctor_name='陈慧芳', is_deleted=1 WHERE id=1;

INSERT INTO maternal_record (patient_idcard, patient_name, age, lmp_date, edd_date, gravidity, parity, blood_type, high_risk_flag, high_risk_reason, register_date, doctor_name, status, create_time) VALUES
('320102199003201234', '王丽', 36, '2026-02-15', '2026-11-22', 3, 1, 'B', 1, '高龄初产(≥35岁)+妊娠期糖尿病', '2026-03-01', '陈慧芳', 1, NOW()),
('320102199807151234', '赵静', 28, '2026-03-10', '2026-12-15', 1, 0, 'O', 0, NULL, '2026-04-05', '陈慧芳', 1, NOW()),
('320102199211081234', '孙晓燕', 34, '2026-01-20', '2026-10-27', 2, 1, 'AB', 0, NULL, '2026-02-18', '刘晓燕', 2, NOW());

-- 产后访视
INSERT INTO maternal_postpartum_visit (maternal_id, patient_idcard, patient_name, visit_date, visit_day, lochia, uterine_involution, wound_healing, breastfeeding, neonate_weight, neonate_jaundice, visit_doctor, advice, status, create_time) VALUES
(4, '320102199211081234', '孙晓燕', '2026-11-05', 9, '正常', '良好', '良好', '母乳充足', 3.45, '无', '刘晓燕', '母乳喂养指导，注意补充维生素D', 1, NOW()),
(4, '320102199211081234', '孙晓燕', '2026-11-20', 24, '量少', '正常', '愈合良好', '混合喂养', 3.82, '轻微', '刘晓燕', '增加哺乳频率，观察黄疸', 1, NOW());

-- 儿童体检
INSERT INTO child_checkup (patient_idcard, patient_name, child_name, gender, birth_date, checkup_date, height, weight, head_circumference, hemoglobin, development_assessment, nutrition_status, advice, doctor_name, next_checkup_date, create_time) VALUES
('320102199211081234', '孙晓燕', '宝宝孙', '男', '2026-10-27', '2026-12-27', 58.2, 5.50, 38.0, 125, '正常', '良好', '继续母乳喂养，每日补充维生素D 400IU', '刘晓燕', '2027-02-27', NOW());

-- 老年人健康体检（关联现有老年居民）
INSERT INTO elderly_checkup (patient_idcard, patient_name, age, gender, checkup_date, height, weight, bmi, blood_pressure, heart_rate, blood_sugar, blood_lipid, liver_function, kidney_function, ecg, b_ultrasound, urine_routine, vision_left, vision_right, self_care_assessment, health_assessment, advice, doctor_name, create_time) VALUES
('320102195708151234', '张建国', 68, '男', '2026-03-15', 168.0, 78.0, 27.6, '152/96', 78, 7.8, 'TC 6.2, LDL 4.1', 'ALT 32, AST 28', 'Cr 98, BUN 6.5', '窦性心律，左室高电压', '脂肪肝(轻度)', '蛋白(-)', 0.6, 0.5, '完全自理', '血压偏高，血脂异常，建议控制饮食', '低盐低脂饮食，增加运动，3个月复查', '赵明德', NOW()),
('320102195508081234', '赵志强', 71, '男', '2026-04-10', 172.0, 65.0, 22.0, '144/88', 82, 6.1, 'TC 5.0, LDL 3.2', 'ALT 25, AST 22', 'Cr 82, BUN 5.0', '大致正常', '未见异常', '蛋白(-)', 0.8, 0.7, '完全自理', '慢阻肺稳定期，营养状况可', '继续家庭氧疗，呼吸康复锻炼', '周大勇', NOW()),
('320102196403151234', '陈秀兰', 62, '女', '2026-05-05', 158.0, 58.0, 23.2, '138/84', 75, 7.2, 'TC 5.5, LDL 3.5', 'ALT 20, AST 18', 'Cr 68, BUN 4.8', '正常', '未见异常', '蛋白(-)', 0.7, 0.6, '完全自理', '血糖控制良好，肾功能稳定', '继续控制饮食，定期复查', '黄美玲', NOW());

-- 传染病上报
INSERT INTO infectious_disease_report (patient_idcard, patient_name, age, gender, disease_type, disease_category, onset_date, diagnosis_date, report_date, report_doctor, report_hospital, symptoms, isolation_status, close_contacts_count, measures, status, create_time) VALUES
('320102199805101234', '刘某', 28, '男', '肺结核', '乙', '2026-02-10', '2026-02-18', '2026-02-18', '赵明德', '社区医院', '咳嗽咳痰3周，低热盗汗', '居家隔离', 3, '转诊至结核病定点医院，家庭密切接触者筛查', 1, NOW()),
('320102200203152345', '周某', 8, '女', '手足口病', '丙', '2026-04-05', '2026-04-06', '2026-04-06', '何伟民', '社区医院', '手足皮疹，口腔疱疹，发热38.2℃', '居家隔离', 0, '居家隔离至症状消失后1周，托幼机构休课', 1, NOW()),
('320102197508202345', '王某', 51, '男', '乙肝', '乙', '2026-03-10', '2026-03-15', '2026-03-15', '李秀华', '社区医院', '乏力，纳差，ALT升高', '否', 2, '家庭密切接触者筛查乙肝五项+接种乙肝疫苗', 2, NOW());

SELECT 'tables' AS check_item, COUNT(*) AS cnt FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'bs1' AND TABLE_NAME LIKE 'vaccination%' OR TABLE_NAME LIKE 'maternal%' OR TABLE_NAME LIKE 'child_checkup' OR TABLE_NAME LIKE 'elderly_checkup' OR TABLE_NAME LIKE 'infectious_disease%';

SELECT 'vaccination_record' AS tbl, COUNT(*) AS cnt FROM vaccination_record
UNION ALL SELECT 'vaccination_schedule', COUNT(*) FROM vaccination_schedule
UNION ALL SELECT 'maternal_record', COUNT(*) FROM maternal_record
UNION ALL SELECT 'maternal_postpartum_visit', COUNT(*) FROM maternal_postpartum_visit
UNION ALL SELECT 'child_checkup', COUNT(*) FROM child_checkup
UNION ALL SELECT 'elderly_checkup', COUNT(*) FROM elderly_checkup
UNION ALL SELECT 'infectious_disease_report', COUNT(*) FROM infectious_disease_report;

COMMIT;
