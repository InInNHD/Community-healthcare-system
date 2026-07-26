-- =============================================
-- 慢病管理模块 - 菜单和权限配置
-- 参考南京"超能家医"模式
-- 支持六大慢病：高血压、糖尿病、冠心病、脑卒中、慢阻肺、慢性肾病
-- =============================================

-- 慢病管理一级菜单
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000001, 'chronicDisease', '0', '[0],', '慢病管理', 'fa-heartbeat', '/chronicDisease', 5, 1, 1, '高血压/糖尿病/冠心病/脑卒中/慢阻肺/慢性肾病', 1, 1);

-- 慢病管理子权限
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000002, 'chronicDisease_list', 'chronicDisease', '[0],[chronicDisease],', '慢病档案列表', '', '/chronicDisease/list', 1, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000003, 'chronicDisease_add', 'chronicDisease', '[0],[chronicDisease],', '新建慢病档案', '', '/chronicDisease/add', 2, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000004, 'chronicDisease_update', 'chronicDisease', '[0],[chronicDisease],', '编辑慢病档案', '', '/chronicDisease/update', 3, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000005, 'chronicDisease_delete', 'chronicDisease', '[0],[chronicDisease],', '删除慢病档案', '', '/chronicDisease/delete', 4, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000006, 'chronicDisease_detail', 'chronicDisease', '[0],[chronicDisease],', '慢病档案详情', '', '/chronicDisease/detail', 5, 2, 0, NULL, 1, 0);

-- 风险评估
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000007, 'chronicDisease_assessRisk', 'chronicDisease', '[0],[chronicDisease],', '风险评估', '', '/chronicDisease/assessRisk', 6, 2, 0, NULL, 1, 0);

-- 随访记录管理
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000008, 'chronicDisease_followup_list', 'chronicDisease', '[0],[chronicDisease],', '随访记录列表', '', '/chronicDisease/followup/list', 7, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000009, 'chronicDisease_followup_add', 'chronicDisease', '[0],[chronicDisease],', '新增随访记录', '', '/chronicDisease/followup/add', 8, 2, 0, NULL, 1, 0);

-- 随访计划管理
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000010, 'chronicDisease_plan_list', 'chronicDisease', '[0],[chronicDisease],', '随访计划列表', '', '/chronicDisease/plan/list', 9, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000011, 'chronicDisease_plan_add', 'chronicDisease', '[0],[chronicDisease],', '新增随访计划', '', '/chronicDisease/plan/add', 10, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000012, 'chronicDisease_plan_delete', 'chronicDisease', '[0],[chronicDisease],', '删除随访计划', '', '/chronicDisease/plan/delete', 11, 2, 0, NULL, 1, 0);

-- 统计与提醒
INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000013, 'chronicDisease_stats', 'chronicDisease', '[0],[chronicDisease],', '慢病统计', '', '/chronicDisease/stats', 12, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000014, 'chronicDisease_followupTemplate', 'chronicDisease', '[0],[chronicDisease],', '随访建议模板', '', '/chronicDisease/followupTemplate', 13, 2, 0, NULL, 1, 0);

INSERT INTO `sys_menu` (`id`, `code`, `pcode`, `pcodes`, `name`, `icon`, `url`, `num`, `levels`, `ismenu`, `tips`, `status`, `isopen`) VALUES
(3000000000000015, 'chronicDisease_pendingReminders', 'chronicDisease', '[0],[chronicDisease],', '待随访提醒', '', '/chronicDisease/pendingReminders', 14, 2, 0, NULL, 1, 0);

-- ==================== 角色权限分配 ====================

-- 角色1(超级管理员) - 拥有所有慢病管理权限
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000001, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000002, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000003, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000004, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000005, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000006, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000007, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000008, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000009, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000010, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000011, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000012, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000013, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000014, 1);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000015, 1);

-- 角色5(医生) - 拥有慢病管理权限
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000001, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000002, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000003, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000004, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000005, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000006, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000007, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000008, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000009, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000010, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000011, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000012, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000013, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000014, 5);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000015, 5);

-- 角色6(病人) - 可查看自己的慢病档案（通过居民端门户）
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000001, 6);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000002, 6);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000006, 6);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000008, 6);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000010, 6);
INSERT INTO `sys_relation` (`menuid`, `roleid`) VALUES (3000000000000013, 6);
