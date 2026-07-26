-- ============================================================
-- 医生信息表 + 医生数据 + doctor_point增加doctor_id
-- 数据库：bs1
-- ============================================================
SET NAMES utf8;

-- 1. 创建医生信息表
CREATE TABLE IF NOT EXISTS `doctor_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doctor_name` varchar(50) NOT NULL COMMENT '医生姓名',
  `department` varchar(50) NOT NULL COMMENT '所属科室',
  `title` varchar(30) DEFAULT NULL COMMENT '职称',
  `specialty` varchar(255) DEFAULT NULL COMMENT '擅长领域',
  `work_days` varchar(30) DEFAULT '1,2,3,4,5' COMMENT '出诊日期(1=周一...7=周日)',
  `work_time_start` varchar(10) DEFAULT '08:00' COMMENT '开诊时间',
  `work_time_end` varchar(10) DEFAULT '17:00' COMMENT '结诊时间',
  `office` varchar(100) DEFAULT NULL COMMENT '诊室地点',
  `image` varchar(255) DEFAULT NULL COMMENT '头像路径',
  `status` int(1) DEFAULT 1 COMMENT '状态(1=正常 0=停诊)',
  `user_id` int(11) DEFAULT NULL COMMENT '关联sys_user.id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='医生信息表';

-- 2. 插入医生数据
INSERT INTO `doctor_info` (`doctor_name`, `department`, `title`, `specialty`, `work_days`, `work_time_start`, `work_time_end`, `office`, `image`, `status`) VALUES
('赵明德', '内科', '主任医师', '高血压、糖尿病、冠心病等慢性病诊治', '1,2,3,4,5', '08:00', '12:00', '内科一诊室', NULL, 1),
('李秀华', '内科', '副主任医师', '呼吸系统疾病、老年病综合诊治', '1,2,4,5', '08:00', '12:00', '内科二诊室', NULL, 1),
('王建国', '外科', '主任医师', '普通外科、腹腔镜微创手术', '1,3,5', '08:00', '12:00', '外科一诊室', NULL, 1),
('张伟民', '外科', '主治医师', '骨伤科、骨折复位与康复', '2,4,6', '08:00', '12:00', '外科二诊室', NULL, 1),
('陈慧芳', '妇科', '主任医师', '妇科炎症、更年期综合征', '1,2,3,4,5', '14:00', '17:00', '妇科诊室', NULL, 1),
('刘晓燕', '儿科', '副主任医师', '小儿呼吸系统疾病、儿童保健', '1,2,3,4,5', '08:00', '12:00', '儿科诊室', NULL, 1),
('周大勇', '中医科', '主任中医师', '中医内科、脾胃病、失眠调理', '1,2,3,4,5', '08:00', '17:00', '中医一诊室', NULL, 1),
('孙丽萍', '中医科', '副主任中医师', '针灸推拿、颈肩腰腿痛', '1,3,5', '14:00', '17:00', '中医二诊室', NULL, 1),
('吴志强', '口腔科', '主治医师', '牙体牙髓病、义齿修复', '1,2,4,5', '08:00', '12:00', '口腔诊室', NULL, 1),
('黄美玲', '眼科', '副主任医师', '白内障、青光眼、眼底病', '2,3,4,5', '08:00', '12:00', '眼科诊室', NULL, 1),
('郑国安', '皮肤科', '主治医师', '湿疹、荨麻疹、银屑病', '1,2,3,5', '08:00', '12:00', '皮肤科诊室', NULL, 1),
('林婷婷', '心理科', '心理咨询师', '焦虑抑郁、失眠、心理咨询', '1,3,5', '14:00', '17:00', '心理咨询室', NULL, 1);

-- 3. doctor_point 表增加 doctor_id 字段
ALTER TABLE `doctor_point` ADD COLUMN `doctor_id` int(11) DEFAULT NULL COMMENT '医生ID' AFTER `doctor_name`;
