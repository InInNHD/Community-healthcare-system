-- =============================================
-- 慢病管理模块数据库脚本
-- 参考南京"超能家医"模式
-- =============================================

-- 1. 慢病档案表
CREATE TABLE IF NOT EXISTS `chronic_disease` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `patient_idcard` varchar(50) DEFAULT NULL COMMENT '患者身份证号',
  `patient_name` varchar(50) NOT NULL COMMENT '患者姓名',
  `disease_type` varchar(20) NOT NULL COMMENT '慢病类型：高血压/糖尿病/冠心病/脑卒中/慢阻肺/慢性肾病',
  `risk_level` varchar(10) NOT NULL DEFAULT '低风险' COMMENT '风险等级：低风险/中风险/高风险',
  `diagnosis_date` datetime DEFAULT NULL COMMENT '确诊日期',
  `doctor_name` varchar(50) DEFAULT NULL COMMENT '管理医生',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '状态：1=管理中,2=已转诊,3=已结案',
  `create_time` datetime DEFAULT NULL COMMENT '建档时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_patient_idcard` (`patient_idcard`),
  KEY `idx_disease_type` (`disease_type`),
  KEY `idx_doctor_name` (`doctor_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='慢病档案表';

-- 2. 随访记录表
CREATE TABLE IF NOT EXISTS `chronic_followup` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chronic_id` int(11) NOT NULL COMMENT '关联慢病档案ID',
  `patient_idcard` varchar(50) DEFAULT NULL COMMENT '患者身份证号',
  `patient_name` varchar(50) NOT NULL COMMENT '患者姓名',
  `disease_type` varchar(20) NOT NULL COMMENT '慢病类型',
  `followup_date` datetime NOT NULL COMMENT '随访日期',
  `followup_doctor` varchar(50) DEFAULT NULL COMMENT '随访医生',
  `followup_type` varchar(20) DEFAULT '门诊' COMMENT '随访方式：门诊/电话/家庭/线上',
  `symptoms` varchar(500) DEFAULT NULL COMMENT '症状描述',
  `blood_pressure` varchar(20) DEFAULT NULL COMMENT '血压(高血压专用)',
  `blood_sugar` varchar(20) DEFAULT NULL COMMENT '血糖(糖尿病专用)',
  `heart_rate` int(11) DEFAULT NULL COMMENT '心率',
  `medication_compliance` varchar(10) DEFAULT '良好' COMMENT '服药依从性：良好/一般/差',
  `lifestyle_advice` varchar(500) DEFAULT NULL COMMENT '生活方式建议',
  `next_followup_date` datetime DEFAULT NULL COMMENT '下次随访日期',
  `risk_level` varchar(10) DEFAULT NULL COMMENT '本次评估风险等级',
  `status` int(11) NOT NULL DEFAULT 1 COMMENT '状态：1=已完成,0=待随访',
  PRIMARY KEY (`id`),
  KEY `idx_chronic_id` (`chronic_id`),
  KEY `idx_patient_idcard` (`patient_idcard`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='慢病随访记录表';

-- 3. 随访计划表
CREATE TABLE IF NOT EXISTS `chronic_followup_plan` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `chronic_id` int(11) NOT NULL COMMENT '关联慢病档案ID',
  `patient_idcard` varchar(50) DEFAULT NULL COMMENT '患者身份证号',
  `patient_name` varchar(50) NOT NULL COMMENT '患者姓名',
  `disease_type` varchar(20) NOT NULL COMMENT '慢病类型',
  `plan_date` datetime NOT NULL COMMENT '计划随访日期',
  `plan_type` varchar(20) DEFAULT '门诊' COMMENT '计划随访方式',
  `status` int(11) NOT NULL DEFAULT 0 COMMENT '状态：0=待执行,1=已执行,2=已过期',
  `doctor_name` varchar(50) DEFAULT NULL COMMENT '负责医生',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_chronic_id` (`chronic_id`),
  KEY `idx_status` (`status`),
  KEY `idx_plan_date` (`plan_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='慢病随访计划表';

-- 4. 初始化测试数据
INSERT INTO `chronic_disease` (`patient_idcard`, `patient_name`, `disease_type`, `risk_level`, `diagnosis_date`, `doctor_name`, `status`, `create_time`, `remark`) VALUES
('320102199001011234', '张三', '高血压', '中风险', '2023-03-15 10:00:00', '赵明德', 1, '2023-03-15 10:00:00', '有家族史，需定期监测血压'),
('320102199202022345', '李四', '糖尿病', '高风险', '2022-08-20 14:30:00', '赵明德', 1, '2022-08-20 14:30:00', 'II型糖尿病，需胰岛素治疗'),
('320102198505033456', '王五', '高血压', '低风险', '2024-01-10 09:00:00', '钱学海', 1, '2024-01-10 09:00:00', '轻度高血压，饮食控制中'),
('320102199108044567', '赵六', '冠心病', '高风险', '2021-11-05 16:00:00', '钱学海', 1, '2021-11-05 16:00:00', '冠脉支架术后，需密切随访'),
('320102199309055678', '孙七', '慢阻肺', '中风险', '2023-06-18 11:00:00', '孙丽华', 1, '2023-06-18 11:00:00', '长期吸烟史，已戒烟'),
('320102198712066789', '周八', '慢性肾病', '中风险', '2023-09-22 15:00:00', '孙丽华', 1, '2023-09-22 15:00:00', 'CKD三期，需定期检查肾功能'),
('320102199507077890', '吴九', '脑卒中', '高风险', '2022-04-08 08:30:00', '赵明德', 1, '2022-04-08 08:30:00', '缺血性脑卒中恢复期'),
('320102199006088901', '郑十', '糖尿病', '低风险', '2024-02-14 10:30:00', '钱学海', 1, '2024-02-14 10:30:00', '糖耐量异常阶段');

-- 5. 初始化随访记录
INSERT INTO `chronic_followup` (`chronic_id`, `patient_idcard`, `patient_name`, `disease_type`, `followup_date`, `followup_doctor`, `followup_type`, `symptoms`, `blood_pressure`, `blood_sugar`, `heart_rate`, `medication_compliance`, `lifestyle_advice`, `next_followup_date`, `risk_level`, `status`) VALUES
(1, '320102199001011234', '张三', '高血压', '2025-04-01 10:00:00', '赵明德', '门诊', '偶有头晕', '145/92', NULL, 78, '良好', '低盐饮食，适量运动', '2025-05-01 10:00:00', '中风险', 1),
(2, '320102199202022345', '李四', '糖尿病', '2025-04-05 14:00:00', '赵明德', '门诊', '多饮多尿', NULL, '8.5mmol/L', 82, '一般', '控制碳水摄入，规律用药', '2025-04-20 14:00:00', '高风险', 1),
(4, '320102199108044567', '赵六', '冠心病', '2025-03-20 16:00:00', '钱学海', '家庭', '偶有胸闷', NULL, NULL, 72, '良好', '避免剧烈运动，定期复查', '2025-04-20 16:00:00', '高风险', 1);

-- 6. 初始化随访计划
INSERT INTO `chronic_followup_plan` (`chronic_id`, `patient_idcard`, `patient_name`, `disease_type`, `plan_date`, `plan_type`, `status`, `doctor_name`, `create_time`) VALUES
(1, '320102199001011234', '张三', '高血压', '2025-05-01 10:00:00', '门诊', 0, '赵明德', '2025-04-01 10:00:00'),
(2, '320102199202022345', '李四', '糖尿病', '2025-04-20 14:00:00', '门诊', 0, '赵明德', '2025-04-05 14:00:00'),
(3, '320102198505033456', '王五', '高血压', '2025-04-15 09:00:00', '电话', 0, '钱学海', '2024-01-10 09:00:00'),
(4, '320102199108044567', '赵六', '冠心病', '2025-04-20 16:00:00', '门诊', 0, '钱学海', '2025-03-20 16:00:00'),
(5, '320102199309055678', '孙七', '慢阻肺', '2025-05-01 11:00:00', '门诊', 0, '孙丽华', '2025-04-01 11:00:00'),
(6, '320102198712066789', '周八', '慢性肾病', '2025-05-01 15:00:00', '门诊', 0, '孙丽华', '2025-04-01 15:00:00'),
(7, '320102199507077890', '吴九', '脑卒中', '2025-04-15 08:30:00', '家庭', 0, '赵明德', '2025-04-01 08:30:00'),
(8, '320102199006088901', '郑十', '糖尿病', '2025-05-14 10:30:00', '线上', 0, '钱学海', '2025-02-14 10:30:00');
