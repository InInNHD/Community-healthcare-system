-- 给 doctor_point 表添加 status 字段
-- status: 0=待参与, 1=已完成, 2=已逾期
ALTER TABLE `doctor_point` ADD COLUMN `status` int(11) DEFAULT 0 COMMENT '预约状态：0=待参与, 1=已完成, 2=已逾期' AFTER `point_place`;

-- 将已有的预约记录按时间自动分类
-- 已过期的预约标记为逾期(2)，未过期的保持待参与(0)
UPDATE `doctor_point` SET `status` = 2 WHERE `point_date` < NOW() AND (`status` IS NULL OR `status` = 0);
