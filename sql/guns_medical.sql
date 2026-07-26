/*
 Navicat MySQL Data Transfer

 Source Server         : localhost
 Source Server Version : 50720
 Source Host           : localhost:3306
 Source Database       : guns

 Target Server Type    : MYSQL
 Target Server Version : 50720
 File Encoding         : 65001

 Date: 2026-04-27
 Description: 社区医疗服务平台管理系统 - 含居民端/医护端门户功能
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for code_dbinfo
-- ----------------------------
DROP TABLE IF EXISTS `code_dbinfo`;
CREATE TABLE `code_dbinfo` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(20) DEFAULT NULL COMMENT '别名',
  `db_driver` varchar(100) NOT NULL COMMENT '数据库驱动',
  `db_url` varchar(200) NOT NULL COMMENT '数据库地址',
  `db_user_name` varchar(100) NOT NULL COMMENT '数据库账户',
  `db_password` varchar(100) NOT NULL COMMENT '连接密码',
  `db_type` varchar(10) DEFAULT NULL COMMENT '数据库类型',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='数据库链接信息';

-- ----------------------------
-- Records of code_dbinfo
-- ----------------------------

-- ----------------------------
-- Table structure for doctor_point (医生预约点 - 支持门户新增预约)
-- ----------------------------
DROP TABLE IF EXISTS `doctor_point`;
CREATE TABLE `doctor_point` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `patient_idcard` int(64) DEFAULT 0 COMMENT '居民身份证号(可选)',
  `patient_name` varchar(255) NOT NULL COMMENT '居民姓名',
  `doctor_name` varchar(255) NOT NULL COMMENT '医生姓名',
  `point_date` datetime NOT NULL COMMENT '预约时间',
  `point_place` varchar(255) NOT NULL COMMENT '预约地点',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='医生预约表';

-- ----------------------------
-- Records of doctor_point
-- ----------------------------
INSERT INTO `doctor_point` VALUES (1, 5221211, '张三', '赵医生', '2018-12-19 17:56:59', '赵医生办公室');

-- ----------------------------
-- Table structure for medicine_info (药品信息 - 支持门户新增药品)
-- ----------------------------
DROP TABLE IF EXISTS `medicine_info`;
CREATE TABLE `medicine_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `medicine_name` varchar(255) NOT NULL COMMENT '药品名称',
  `medicine_price` int(10) NOT NULL COMMENT '药品价格',
  `medicine_value` varchar(255) NOT NULL COMMENT '药品功效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='药品信息表';

-- ----------------------------
-- Records of medicine_info
-- ----------------------------
INSERT INTO `medicine_info` VALUES (1, '板蓝根', 12, '治疗感冒');
INSERT INTO `medicine_info` VALUES (2, '青霉素', 30, '退烧药');

-- ----------------------------
-- Table structure for patient_health (居民健康信息 - 支持门户新增健康记录)
-- ----------------------------
DROP TABLE IF EXISTS `patient_health`;
CREATE TABLE `patient_health` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `heart_jump` int(3) NOT NULL COMMENT '心跳(次/分)',
  `blood_pressure` int(3) NOT NULL COMMENT '血压(mmHg)',
  `blood_ox` int(3) NOT NULL COMMENT '血氧(%)',
  `pulse` int(3) NOT NULL COMMENT '脉搏(次/分)',
  `date` datetime NOT NULL COMMENT '检测时间',
  `patient_idcard` int(64) DEFAULT 0 COMMENT '居民身份证号(可选)',
  `patient_name` varchar(255) NOT NULL COMMENT '居民姓名',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8 COMMENT='居民健康信息表';

-- ----------------------------
-- Records of patient_health
-- ----------------------------
INSERT INTO `patient_health` VALUES (1, 80, 123, 96, 111, '2018-11-14 16:37:49', 5221211, '张三');
INSERT INTO `patient_health` VALUES (2, 89, 142, 97, 108, '2018-11-15 16:43:03', 5221212, '李四');
INSERT INTO `patient_health` VALUES (3, 77, 108, 88, 124, '2018-11-16 16:43:31', 5221213, '王五');
INSERT INTO `patient_health` VALUES (4, 89, 141, 95, 108, '2018-11-17 16:43:03', 5221212, '李四');
INSERT INTO `patient_health` VALUES (5, 73, 132, 93, 108, '2018-11-29 16:43:03', 5221212, '李四');
INSERT INTO `patient_health` VALUES (6, 85, 127, 92, 108, '2018-12-01 16:43:03', 5221212, '李四');
INSERT INTO `patient_health` VALUES (7, 99, 122, 99, 108, '2018-12-13 16:43:03', 5221212, '李四');

-- ----------------------------
-- Table structure for patient_history (就诊记录 - 支持门户新增就诊记录)
-- ----------------------------
DROP TABLE IF EXISTS `patient_history`;
CREATE TABLE `patient_history` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `patient_idcard` varchar(64) DEFAULT '' COMMENT '居民身份证号(可选)',
  `patient_name` varchar(255) NOT NULL COMMENT '居民姓名',
  `patient_sym` varchar(255) NOT NULL COMMENT '症状',
  `patient_doctor` varchar(255) NOT NULL COMMENT '主治医生',
  `patient_medicine` varchar(255) NOT NULL COMMENT '用药',
  `patient_history_date` datetime NOT NULL COMMENT '就诊时间',
  `takeprice` int(10) NOT NULL COMMENT '费用(元)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='就诊记录表';

-- ----------------------------
-- Records of patient_history
-- ----------------------------
INSERT INTO `patient_history` VALUES (1, '5221211', '张三', '轻微感冒', '赵医生', '板蓝根', '2018-12-18 17:11:41', 12);
INSERT INTO `patient_history` VALUES (2, '5221212', '李四', '身子不舒服，恶心，头痛', '黄医生', '青霉素', '2018-12-27 17:14:27', 30);

-- ----------------------------
-- Table structure for patient_info (居民医保信息)
-- ----------------------------
DROP TABLE IF EXISTS `patient_info`;
CREATE TABLE `patient_info` (
  `paient_idcard` int(64) NOT NULL COMMENT '身份证号(主键)',
  `paient_name` varchar(255) NOT NULL COMMENT '姓名',
  `paient_money` varchar(255) NOT NULL DEFAULT '0' COMMENT '余额',
  `user_id` int(11) DEFAULT NULL COMMENT '关联sys_user.id',
  PRIMARY KEY (`paient_idcard`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='居民医保信息表';

-- ----------------------------
-- Records of patient_info
-- ----------------------------
INSERT INTO `patient_info` VALUES (52212114, '王竑超', '12000');

-- ----------------------------
-- Table structure for sys_dept (部门表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `num` int(11) DEFAULT NULL COMMENT '排序',
  `pid` int(11) DEFAULT NULL COMMENT '父部门id',
  `pids` varchar(255) DEFAULT NULL COMMENT '父级ids',
  `simplename` varchar(45) DEFAULT NULL COMMENT '简称',
  `fullname` varchar(255) DEFAULT NULL COMMENT '全称',
  `tips` varchar(255) DEFAULT NULL COMMENT '提示',
  `version` int(11) DEFAULT NULL COMMENT '版本（乐观锁保留字段）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8 COMMENT='部门表';

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (24, 1, 0, '[0],', '总医院', '总医院', '', NULL);
INSERT INTO `sys_dept` VALUES (25, 2, 24, '[0],[24],', '医生部', '医生部', '', NULL);
INSERT INTO `sys_dept` VALUES (26, 3, 24, '[0],[24],', '病人部', '病人部', '', NULL);
INSERT INTO `sys_dept` VALUES (27, 4, 24, '[0],[24],', '战略部', '战略部', '', NULL);

-- ----------------------------
-- Table structure for sys_dict (字典表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict`;
CREATE TABLE `sys_dict` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `num` int(11) DEFAULT NULL COMMENT '排序',
  `pid` int(11) DEFAULT NULL COMMENT '父级字典',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `tips` varchar(255) DEFAULT NULL COMMENT '提示',
  `code` varchar(255) DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC COMMENT='字典表';

-- ----------------------------
-- Records of sys_dict
-- ----------------------------
INSERT INTO `sys_dict` VALUES (50, 0, 0, '性别', NULL, 'sys_sex');
INSERT INTO `sys_dict` VALUES (51, 1, 50, '男', NULL, '1');
INSERT INTO `sys_dict` VALUES (52, 2, 50, '女', NULL, '2');
INSERT INTO `sys_dict` VALUES (53, 0, 0, '状态', NULL, 'sys_state');
INSERT INTO `sys_dict` VALUES (54, 1, 53, '启用', NULL, '1');
INSERT INTO `sys_dict` VALUES (55, 2, 53, '禁用', NULL, '2');
INSERT INTO `sys_dict` VALUES (56, 0, 0, '账号状态', NULL, 'account_state');
INSERT INTO `sys_dict` VALUES (57, 1, 56, '启用', NULL, '1');
INSERT INTO `sys_dict` VALUES (58, 2, 56, '冻结', NULL, '2');
INSERT INTO `sys_dict` VALUES (59, 3, 56, '已删除', NULL, '3');

-- ----------------------------
-- Table structure for sys_expense (报销表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_expense`;
CREATE TABLE `sys_expense` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `money` decimal(20,2) DEFAULT NULL COMMENT '报销金额',
  `desc` varchar(255) DEFAULT '' COMMENT '描述',
  `createtime` datetime DEFAULT NULL COMMENT '创建时间',
  `state` int(11) DEFAULT NULL COMMENT '状态: 1.待提交  2:待审核   3.审核通过 4:驳回',
  `userid` int(11) DEFAULT NULL COMMENT '用户id',
  `processId` varchar(255) DEFAULT NULL COMMENT '流程定义id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='报销表';

-- ----------------------------
-- Records of sys_expense
-- ----------------------------

-- ----------------------------
-- Table structure for sys_login_log (登录日志表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_login_log`;
CREATE TABLE `sys_login_log` (
  `id` int(65) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `logname` varchar(255) DEFAULT NULL COMMENT '日志名称',
  `userid` int(65) DEFAULT NULL COMMENT '管理员id',
  `createtime` datetime DEFAULT NULL COMMENT '创建时间',
  `succeed` varchar(255) DEFAULT NULL COMMENT '是否执行成功',
  `message` text COMMENT '具体消息',
  `ip` varchar(255) DEFAULT NULL COMMENT '登录ip',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='登录记录';

-- ----------------------------
-- Records of sys_login_log (清空旧日志，从1开始)
-- ----------------------------

-- ----------------------------
-- Table structure for sys_menu (菜单表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `code` varchar(255) DEFAULT NULL COMMENT '菜单编号',
  `pcode` varchar(255) DEFAULT NULL COMMENT '菜单父编号',
  `pcodes` varchar(255) DEFAULT NULL COMMENT '当前菜单的所有父菜单编号',
  `name` varchar(255) DEFAULT NULL COMMENT '菜单名称',
  `icon` varchar(255) DEFAULT NULL COMMENT '菜单图标',
  `url` varchar(255) DEFAULT NULL COMMENT 'url地址',
  `num` int(65) DEFAULT NULL COMMENT '菜单排序号',
  `levels` int(65) DEFAULT NULL COMMENT '菜单层级',
  `ismenu` int(11) DEFAULT NULL COMMENT '是否是菜单（1：是  0：不是）',
  `tips` varchar(255) DEFAULT NULL COMMENT '备注',
  `status` int(65) DEFAULT NULL COMMENT '菜单状态 :  1:启用   0:不启用',
  `isopen` int(11) DEFAULT NULL COMMENT '是否打开:    1:打开   0:不打开',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='菜单表';

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
-- 系统管理
INSERT INTO `sys_menu` VALUES (105, 'system', '0', '[0],', '系统管理', 'fa-user', '#', 4, 1, 1, NULL, 1, 1);
INSERT INTO `sys_menu` VALUES (106, 'mgr', 'system', '[0],[system],', '用户管理', '', '/mgr', 1, 2, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (107, 'mgr_add', 'mgr', '[0],[system],[mgr],', '添加用户', NULL, '/mgr/add', 1, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (108, 'mgr_edit', 'mgr', '[0],[system],[mgr],', '修改用户', NULL, '/mgr/edit', 2, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (109, 'mgr_delete', 'mgr', '[0],[system],[mgr],', '删除用户', NULL, '/mgr/delete', 3, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (110, 'mgr_reset', 'mgr', '[0],[system],[mgr],', '重置密码', NULL, '/mgr/reset', 4, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (111, 'mgr_freeze', 'mgr', '[0],[system],[mgr],', '冻结用户', NULL, '/mgr/freeze', 5, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (112, 'mgr_unfreeze', 'mgr', '[0],[system],[mgr],', '解除冻结用户', NULL, '/mgr/unfreeze', 6, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (113, 'mgr_setRole', 'mgr', '[0],[system],[mgr],', '分配角色', NULL, '/mgr/setRole', 7, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (114, 'role', 'system', '[0],[system],', '角色管理', NULL, '/role', 2, 2, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (115, 'role_add', 'role', '[0],[system],[role],', '添加角色', NULL, '/role/add', 1, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (116, 'role_edit', 'role', '[0],[system],[role],', '修改角色', NULL, '/role/edit', 2, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (117, 'role_remove', 'role', '[0],[system],[role],', '删除角色', NULL, '/role/remove', 3, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (118, 'role_setAuthority', 'role', '[0],[system],[role],', '配置权限', NULL, '/role/setAuthority', 4, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (119, 'menu', 'system', '[0],[system],', '菜单管理', NULL, '/menu', 4, 2, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (120, 'menu_add', 'menu', '[0],[system],[menu],', '添加菜单', NULL, '/menu/add', 1, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (121, 'menu_edit', 'menu', '[0],[system],[menu],', '修改菜单', NULL, '/menu/edit', 2, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (122, 'menu_remove', 'menu', '[0],[system],[menu],', '删除菜单', NULL, '/menu/remove', 3, 3, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (128, 'log', 'system', '[0],[system],', '业务日志', NULL, '/log', 6, 2, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (131, 'dept', 'system', '[0],[system],', '部门管理', NULL, '/dept', 3, 2, 1, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (132, 'dict', 'system', '[0],[system],', '字典管理', NULL, '/dict', 4, 2, 1, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (133, 'loginLog', 'system', '[0],[system],', '登录日志', NULL, '/loginLog', 6, 2, 1, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (134, 'log_clean', 'log', '[0],[system],[log],', '清空日志', NULL, '/log/delLog', 3, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (135, 'dept_add', 'dept', '[0],[system],[dept],', '添加部门', NULL, '/dept/add', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (136, 'dept_update', 'dept', '[0],[system],[dept],', '修改部门', NULL, '/dept/update', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (137, 'dept_delete', 'dept', '[0],[system],[dept],', '删除部门', NULL, '/dept/delete', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (138, 'dict_add', 'dict', '[0],[system],[dict],', '添加字典', NULL, '/dict/add', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (139, 'dict_update', 'dict', '[0],[system],[dict],', '修改字典', NULL, '/dict/update', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (140, 'dict_delete', 'dict', '[0],[system],[dict],', '删除字典', NULL, '/dict/delete', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (141, 'notice', 'system', '[0],[system],', '通知管理', NULL, '/notice', 9, 2, 1, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (142, 'notice_add', 'notice', '[0],[system],[notice],', '添加通知', NULL, '/notice/add', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (143, 'notice_update', 'notice', '[0],[system],[notice],', '修改通知', NULL, '/notice/update', 2, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (144, 'notice_delete', 'notice', '[0],[system],[notice],', '删除通知', NULL, '/notice/delete', 3, 3, 0, NULL, 1, NULL);
-- 系统管理子页面
INSERT INTO `sys_menu` VALUES (150, 'to_menu_edit', 'menu', '[0],[system],[menu],', '菜单编辑跳转', '', '/menu/menu_edit', 4, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (151, 'menu_list', 'menu', '[0],[system],[menu],', '菜单列表', '', '/menu/list', 5, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (152, 'to_dept_update', 'dept', '[0],[system],[dept],', '修改部门跳转', '', '/dept/dept_update', 4, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (153, 'dept_list', 'dept', '[0],[system],[dept],', '部门列表', '', '/dept/list', 5, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (154, 'dept_detail', 'dept', '[0],[system],[dept],', '部门详情', '', '/dept/detail', 6, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (155, 'to_dict_edit', 'dict', '[0],[system],[dict],', '修改菜单跳转', '', '/dict/dict_edit', 4, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (156, 'dict_list', 'dict', '[0],[system],[dict],', '字典列表', '', '/dict/list', 5, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (157, 'dict_detail', 'dict', '[0],[system],[dict],', '字典详情', '', '/dict/detail', 6, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (158, 'log_list', 'log', '[0],[system],[log],', '日志列表', '', '/log/list', 2, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (159, 'log_detail', 'log', '[0],[system],[log],', '日志详情', '', '/log/detail', 3, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (160, 'del_login_log', 'loginLog', '[0],[system],[loginLog],', '清空登录日志', '', '/loginLog/delLoginLog', 1, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (161, 'login_log_list', 'loginLog', '[0],[system],[loginLog],', '登录日志列表', '', '/loginLog/list', 2, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (162, 'to_role_edit', 'role', '[0],[system],[role],', '修改角色跳转', '', '/role/role_edit', 5, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (163, 'to_role_assign', 'role', '[0],[system],[role],', '角色分配跳转', '', '/role/role_assign', 6, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (164, 'role_list', 'role', '[0],[system],[role],', '角色列表', '', '/role/list', 7, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (165, 'to_assign_role', 'mgr', '[0],[system],[mgr],', '分配角色跳转', '', '/mgr/role_assign', 8, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (166, 'to_user_edit', 'mgr', '[0],[system],[mgr],', '编辑用户跳转', '', '/mgr/user_edit', 9, 3, 0, NULL, 1, NULL);
INSERT INTO `sys_menu` VALUES (167, 'mgr_list', 'mgr', '[0],[system],[mgr],', '用户列表', '', '/mgr/list', 10, 3, 0, NULL, 1, NULL);

-- ==================== 业务管理菜单 ====================
-- 居民医保信息管理
INSERT INTO `sys_menu` VALUES (1078922896050376705, 'patientInfo', '0', '[0],', '居民医保信息', '', '/patientInfo', 99, 1, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078922896050376706, 'patientInfo_list', 'patientInfo', '[0],[patientInfo],', '居民管理列表', '', '/patientInfo/list', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078922896050376707, 'patientInfo_add', 'patientInfo', '[0],[patientInfo],', '居民管理添加', '', '/patientInfo/add', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078922896050376708, 'patientInfo_update', 'patientInfo', '[0],[patientInfo],', '居民管理更新', '', '/patientInfo/update', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078922896050376709, 'patientInfo_delete', 'patientInfo', '[0],[patientInfo],', '居民管理删除', '', '/patientInfo/delete', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078922896050376710, 'patientInfo_detail', 'patientInfo', '[0],[patientInfo],', '居民管理详情', '', '/patientInfo/detail', 99, 2, 0, NULL, 1, 0);

-- 药物信息管理
INSERT INTO `sys_menu` VALUES (1078929243038953474, 'medicineInfo', '0', '[0],', '药物信息管理', '', '/medicineInfo', 99, 1, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078929243038953475, 'medicineInfo_list', 'medicineInfo', '[0],[medicineInfo],', '药物管理列表', '', '/medicineInfo/list', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078929243038953476, 'medicineInfo_add', 'medicineInfo', '[0],[medicineInfo],', '药物管理添加', '', '/medicineInfo/add', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078929243038953477, 'medicineInfo_update', 'medicineInfo', '[0],[medicineInfo],', '药物管理更新', '', '/medicineInfo/update', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078929243038953478, 'medicineInfo_delete', 'medicineInfo', '[0],[medicineInfo],', '药物管理删除', '', '/medicineInfo/delete', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078929243038953479, 'medicineInfo_detail', 'medicineInfo', '[0],[medicineInfo],', '药物管理详情', '', '/medicineInfo/detail', 99, 2, 0, NULL, 1, 0);

-- 居民健康信息管理
INSERT INTO `sys_menu` VALUES (1078936438652784641, 'patientHealth', '0', '[0],', '居民健康信息', '', '/patientHealth', 99, 1, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078936438652784642, 'patientHealth_list', 'patientHealth', '[0],[patientHealth],', '居民健康信息管理列表', '', '/patientHealth/list', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078936438652784643, 'patientHealth_add', 'patientHealth', '[0],[patientHealth],', '居民健康信息管理添加', '', '/patientHealth/add', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078936438652784644, 'patientHealth_update', 'patientHealth', '[0],[patientHealth],', '居民健康信息管理更新', '', '/patientHealth/update', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078936438652784645, 'patientHealth_delete', 'patientHealth', '[0],[patientHealth],', '居民健康信息管理删除', '', '/patientHealth/delete', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078936438652784646, 'patientHealth_detail', 'patientHealth', '[0],[patientHealth],', '居民健康信息管理详情', '', '/patientHealth/detail', 99, 2, 0, NULL, 1, 0);

-- 就诊记录管理
INSERT INTO `sys_menu` VALUES (1078942865115041793, 'patientHistory', '0', '[0],', '就诊记录管理', '', '/patientHistory', 99, 1, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078942865119236097, 'patientHistory_list', 'patientHistory', '[0],[patientHistory],', '就诊记录管理列表', '', '/patientHistory/list', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078942865119236098, 'patientHistory_add', 'patientHistory', '[0],[patientHistory],', '就诊记录管理添加', '', '/patientHistory/add', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078942865119236099, 'patientHistory_update', 'patientHistory', '[0],[patientHistory],', '就诊记录管理更新', '', '/patientHistory/update', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078942865119236100, 'patientHistory_delete', 'patientHistory', '[0],[patientHistory],', '就诊记录管理删除', '', '/patientHistory/delete', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078942865119236101, 'patientHistory_detail', 'patientHistory', '[0],[patientHistory],', '就诊记录管理详情', '', '/patientHistory/detail', 99, 2, 0, NULL, 1, 0);

-- 医生预约管理
INSERT INTO `sys_menu` VALUES (1078953266368131073, 'doctorPoint', '0', '[0],', '医生预约管理', '', '/doctorPoint', 99, 1, 1, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078953266368131074, 'doctorPoint_list', 'doctorPoint', '[0],[doctorPoint],', '医生预约管理列表', '', '/doctorPoint/list', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078953266368131075, 'doctorPoint_add', 'doctorPoint', '[0],[doctorPoint],', '医生预约管理添加', '', '/doctorPoint/add', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078953266368131076, 'doctorPoint_update', 'doctorPoint', '[0],[doctorPoint],', '医生预约管理更新', '', '/doctorPoint/update', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078953266368131077, 'doctorPoint_delete', 'doctorPoint', '[0],[doctorPoint],', '医生预约管理删除', '', '/doctorPoint/delete', 99, 2, 0, NULL, 1, 0);
INSERT INTO `sys_menu` VALUES (1078953266368131078, 'doctorPoint_detail', 'doctorPoint', '[0],[doctorPoint],', '医生预约管理详情', '', '/doctorPoint/detail', 99, 2, 0, NULL, 1, 0);

-- ==================== 门户入口菜单 ====================
-- 居民端门户
INSERT INTO `sys_menu` VALUES (2000000000000001, 'patient_portal', '0', '[0],', '居民健康服务', 'fa-home', '/patient_portal', 1, 1, 1, NULL, 1, 1);

-- 医护端门户
INSERT INTO `sys_menu` VALUES (2000000000000002, 'doctor_portal', '0', '[0],', '医护工作台', 'fa-stethoscope', '/doctor_portal', 2, 1, 1, NULL, 1, 1);

-- ----------------------------
-- Table structure for sys_notice (通知表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_notice`;
CREATE TABLE `sys_notice` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(255) DEFAULT NULL COMMENT '标题',
  `type` int(11) DEFAULT NULL COMMENT '类型',
  `content` text COMMENT '内容',
  `createtime` datetime DEFAULT NULL COMMENT '创建时间',
  `creater` varchar(255) DEFAULT NULL COMMENT '创建人',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='通知表';

-- ----------------------------
-- Records of sys_notice
-- ----------------------------

-- ----------------------------
-- Table structure for sys_operation_log (业务日志表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id` int(65) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `logtype` varchar(255) DEFAULT NULL COMMENT '日志类型',
  `logname` varchar(255) DEFAULT NULL COMMENT '日志名称',
  `userid` int(65) DEFAULT NULL COMMENT '用户id',
  `classname` varchar(255) DEFAULT NULL COMMENT 'action类',
  `method` text COMMENT 'action方法',
  `createtime` datetime DEFAULT NULL COMMENT '创建时间',
  `succeed` varchar(255) DEFAULT NULL COMMENT '是否执行成功',
  `message` text COMMENT '具体消息',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='业务日志';

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------

-- ----------------------------
-- Table structure for sys_relation (角色-菜单关联表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_relation`;
CREATE TABLE `sys_relation` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `menuid` bigint(20) DEFAULT NULL COMMENT '菜单id',
  `roleid` int(11) DEFAULT NULL COMMENT '角色id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='角色-菜单关联表';

-- ----------------------------
-- Records of sys_relation
-- ----------------------------
-- 角色1(超级管理员) - 拥有所有菜单
INSERT INTO `sys_relation` VALUES (1, 105, 1);
INSERT INTO `sys_relation` VALUES (2, 106, 1);
INSERT INTO `sys_relation` VALUES (3, 107, 1);
INSERT INTO `sys_relation` VALUES (4, 108, 1);
INSERT INTO `sys_relation` VALUES (5, 109, 1);
INSERT INTO `sys_relation` VALUES (6, 110, 1);
INSERT INTO `sys_relation` VALUES (7, 111, 1);
INSERT INTO `sys_relation` VALUES (8, 112, 1);
INSERT INTO `sys_relation` VALUES (9, 113, 1);
INSERT INTO `sys_relation` VALUES (10, 114, 1);
INSERT INTO `sys_relation` VALUES (11, 115, 1);
INSERT INTO `sys_relation` VALUES (12, 116, 1);
INSERT INTO `sys_relation` VALUES (13, 117, 1);
INSERT INTO `sys_relation` VALUES (14, 118, 1);
INSERT INTO `sys_relation` VALUES (15, 119, 1);
INSERT INTO `sys_relation` VALUES (16, 120, 1);
INSERT INTO `sys_relation` VALUES (17, 121, 1);
INSERT INTO `sys_relation` VALUES (18, 122, 1);
INSERT INTO `sys_relation` VALUES (19, 128, 1);
INSERT INTO `sys_relation` VALUES (21, 131, 1);
INSERT INTO `sys_relation` VALUES (22, 132, 1);
INSERT INTO `sys_relation` VALUES (23, 133, 1);
INSERT INTO `sys_relation` VALUES (24, 134, 1);
INSERT INTO `sys_relation` VALUES (25, 135, 1);
INSERT INTO `sys_relation` VALUES (26, 136, 1);
INSERT INTO `sys_relation` VALUES (27, 137, 1);
INSERT INTO `sys_relation` VALUES (28, 138, 1);
INSERT INTO `sys_relation` VALUES (29, 139, 1);
INSERT INTO `sys_relation` VALUES (30, 140, 1);
INSERT INTO `sys_relation` VALUES (31, 141, 1);
INSERT INTO `sys_relation` VALUES (32, 142, 1);
INSERT INTO `sys_relation` VALUES (33, 143, 1);
INSERT INTO `sys_relation` VALUES (34, 144, 1);
INSERT INTO `sys_relation` VALUES (38, 150, 1);
INSERT INTO `sys_relation` VALUES (39, 151, 1);
INSERT INTO `sys_relation` VALUES (40, 152, 1);
INSERT INTO `sys_relation` VALUES (41, 153, 1);
INSERT INTO `sys_relation` VALUES (42, 154, 1);
INSERT INTO `sys_relation` VALUES (43, 155, 1);
INSERT INTO `sys_relation` VALUES (44, 156, 1);
INSERT INTO `sys_relation` VALUES (45, 157, 1);
INSERT INTO `sys_relation` VALUES (46, 158, 1);
INSERT INTO `sys_relation` VALUES (47, 159, 1);
INSERT INTO `sys_relation` VALUES (48, 160, 1);
INSERT INTO `sys_relation` VALUES (49, 161, 1);
INSERT INTO `sys_relation` VALUES (50, 162, 1);
INSERT INTO `sys_relation` VALUES (51, 163, 1);
INSERT INTO `sys_relation` VALUES (52, 164, 1);
INSERT INTO `sys_relation` VALUES (53, 165, 1);
INSERT INTO `sys_relation` VALUES (54, 166, 1);
INSERT INTO `sys_relation` VALUES (55, 167, 1);
-- 角色1(超级管理员) - 业务菜单
INSERT INTO `sys_relation` VALUES (100, 1078922896050376705, 1);
INSERT INTO `sys_relation` VALUES (101, 1078922896050376706, 1);
INSERT INTO `sys_relation` VALUES (102, 1078922896050376707, 1);
INSERT INTO `sys_relation` VALUES (103, 1078922896050376708, 1);
INSERT INTO `sys_relation` VALUES (104, 1078922896050376709, 1);
INSERT INTO `sys_relation` VALUES (105, 1078922896050376710, 1);
INSERT INTO `sys_relation` VALUES (106, 1078929243038953474, 1);
INSERT INTO `sys_relation` VALUES (107, 1078929243038953475, 1);
INSERT INTO `sys_relation` VALUES (108, 1078929243038953476, 1);
INSERT INTO `sys_relation` VALUES (109, 1078929243038953477, 1);
INSERT INTO `sys_relation` VALUES (110, 1078929243038953478, 1);
INSERT INTO `sys_relation` VALUES (111, 1078929243038953479, 1);
INSERT INTO `sys_relation` VALUES (112, 1078936438652784641, 1);
INSERT INTO `sys_relation` VALUES (113, 1078936438652784642, 1);
INSERT INTO `sys_relation` VALUES (114, 1078936438652784643, 1);
INSERT INTO `sys_relation` VALUES (115, 1078936438652784644, 1);
INSERT INTO `sys_relation` VALUES (116, 1078936438652784645, 1);
INSERT INTO `sys_relation` VALUES (117, 1078936438652784646, 1);
INSERT INTO `sys_relation` VALUES (118, 1078942865115041793, 1);
INSERT INTO `sys_relation` VALUES (119, 1078942865119236097, 1);
INSERT INTO `sys_relation` VALUES (120, 1078942865119236098, 1);
INSERT INTO `sys_relation` VALUES (121, 1078942865119236099, 1);
INSERT INTO `sys_relation` VALUES (122, 1078942865119236100, 1);
INSERT INTO `sys_relation` VALUES (123, 1078942865119236101, 1);
INSERT INTO `sys_relation` VALUES (124, 1078953266368131073, 1);
INSERT INTO `sys_relation` VALUES (125, 1078953266368131074, 1);
INSERT INTO `sys_relation` VALUES (126, 1078953266368131075, 1);
INSERT INTO `sys_relation` VALUES (127, 1078953266368131076, 1);
INSERT INTO `sys_relation` VALUES (128, 1078953266368131077, 1);
INSERT INTO `sys_relation` VALUES (129, 1078953266368131078, 1);
-- 角色1(超级管理员) - 门户菜单
INSERT INTO `sys_relation` VALUES (130, 2000000000000001, 1);
INSERT INTO `sys_relation` VALUES (131, 2000000000000002, 1);

-- 角色5(医生) - 居民医保+居民健康+就诊记录+医生预约+医护端门户
INSERT INTO `sys_relation` VALUES (200, 1078922896050376705, 5);
INSERT INTO `sys_relation` VALUES (202, 1078922896050376706, 5);
INSERT INTO `sys_relation` VALUES (203, 1078922896050376707, 5);
INSERT INTO `sys_relation` VALUES (204, 1078922896050376708, 5);
INSERT INTO `sys_relation` VALUES (205, 1078922896050376709, 5);
INSERT INTO `sys_relation` VALUES (206, 1078922896050376710, 5);
INSERT INTO `sys_relation` VALUES (207, 1078929243038953474, 5);
INSERT INTO `sys_relation` VALUES (208, 1078929243038953475, 5);
INSERT INTO `sys_relation` VALUES (209, 1078929243038953476, 5);
INSERT INTO `sys_relation` VALUES (210, 1078929243038953477, 5);
INSERT INTO `sys_relation` VALUES (211, 1078929243038953478, 5);
INSERT INTO `sys_relation` VALUES (212, 1078929243038953479, 5);
INSERT INTO `sys_relation` VALUES (213, 1078936438652784641, 5);
INSERT INTO `sys_relation` VALUES (214, 1078936438652784642, 5);
INSERT INTO `sys_relation` VALUES (215, 1078936438652784643, 5);
INSERT INTO `sys_relation` VALUES (216, 1078936438652784644, 5);
INSERT INTO `sys_relation` VALUES (217, 1078936438652784645, 5);
INSERT INTO `sys_relation` VALUES (218, 1078936438652784646, 5);
INSERT INTO `sys_relation` VALUES (219, 1078942865115041793, 5);
INSERT INTO `sys_relation` VALUES (220, 1078942865119236097, 5);
INSERT INTO `sys_relation` VALUES (221, 1078942865119236098, 5);
INSERT INTO `sys_relation` VALUES (222, 1078942865119236099, 5);
INSERT INTO `sys_relation` VALUES (223, 1078942865119236100, 5);
INSERT INTO `sys_relation` VALUES (224, 1078942865119236101, 5);
INSERT INTO `sys_relation` VALUES (225, 1078953266368131073, 5);
INSERT INTO `sys_relation` VALUES (226, 1078953266368131074, 5);
INSERT INTO `sys_relation` VALUES (227, 1078953266368131075, 5);
INSERT INTO `sys_relation` VALUES (228, 1078953266368131076, 5);
INSERT INTO `sys_relation` VALUES (229, 1078953266368131077, 5);
INSERT INTO `sys_relation` VALUES (230, 1078953266368131078, 5);
INSERT INTO `sys_relation` VALUES (231, 2000000000000002, 5);

-- 角色6(病人) - 通知+居民医保+居民健康+就诊记录+医生预约+居民端门户
INSERT INTO `sys_relation` VALUES (300, 1078922896050376705, 6);
INSERT INTO `sys_relation` VALUES (301, 1078922896050376706, 6);
INSERT INTO `sys_relation` VALUES (302, 1078922896050376707, 6);
INSERT INTO `sys_relation` VALUES (303, 1078922896050376708, 6);
INSERT INTO `sys_relation` VALUES (304, 1078922896050376709, 6);
INSERT INTO `sys_relation` VALUES (305, 1078922896050376710, 6);
INSERT INTO `sys_relation` VALUES (306, 1078936438652784641, 6);
INSERT INTO `sys_relation` VALUES (307, 1078936438652784642, 6);
INSERT INTO `sys_relation` VALUES (308, 1078936438652784643, 6);
INSERT INTO `sys_relation` VALUES (309, 1078936438652784644, 6);
INSERT INTO `sys_relation` VALUES (310, 1078936438652784645, 6);
INSERT INTO `sys_relation` VALUES (311, 1078936438652784646, 6);
INSERT INTO `sys_relation` VALUES (312, 1078942865115041793, 6);
INSERT INTO `sys_relation` VALUES (313, 1078942865119236097, 6);
INSERT INTO `sys_relation` VALUES (314, 1078942865119236098, 6);
INSERT INTO `sys_relation` VALUES (315, 1078942865119236099, 6);
INSERT INTO `sys_relation` VALUES (316, 1078942865119236100, 6);
INSERT INTO `sys_relation` VALUES (317, 1078942865119236101, 6);
INSERT INTO `sys_relation` VALUES (318, 1078953266368131073, 6);
INSERT INTO `sys_relation` VALUES (319, 1078953266368131074, 6);
INSERT INTO `sys_relation` VALUES (320, 1078953266368131075, 6);
INSERT INTO `sys_relation` VALUES (321, 1078953266368131076, 6);
INSERT INTO `sys_relation` VALUES (322, 1078953266368131077, 6);
INSERT INTO `sys_relation` VALUES (323, 1078953266368131078, 6);
INSERT INTO `sys_relation` VALUES (324, 2000000000000001, 6);

-- ----------------------------
-- Table structure for sys_role (角色表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `num` int(11) DEFAULT NULL COMMENT '序号',
  `pid` int(11) DEFAULT NULL COMMENT '父角色id',
  `name` varchar(255) DEFAULT NULL COMMENT '角色名称',
  `deptid` int(11) DEFAULT NULL COMMENT '部门名称',
  `tips` varchar(255) DEFAULT NULL COMMENT '提示',
  `version` int(11) DEFAULT NULL COMMENT '保留字段(暂时没用）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8 COMMENT='角色表';

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 1, 0, '超级管理员', 24, 'administrator', 1);
INSERT INTO `sys_role` VALUES (5, 2, 1, '医生', 25, '医生', NULL);
INSERT INTO `sys_role` VALUES (6, NULL, 1, '病人', 26, '病人', NULL);

-- ----------------------------
-- Table structure for sys_user (管理员/用户表)
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `account` varchar(45) DEFAULT NULL COMMENT '账号',
  `password` varchar(45) DEFAULT NULL COMMENT '密码',
  `salt` varchar(45) DEFAULT NULL COMMENT 'md5密码盐',
  `name` varchar(45) DEFAULT NULL COMMENT '名字',
  `birthday` datetime DEFAULT NULL COMMENT '生日',
  `sex` int(11) DEFAULT NULL COMMENT '性别（1：男 2：女）',
  `email` varchar(45) DEFAULT NULL COMMENT '电子邮件',
  `phone` varchar(45) DEFAULT NULL COMMENT '电话',
  `roleid` varchar(255) DEFAULT NULL COMMENT '角色id',
  `deptid` int(11) DEFAULT NULL COMMENT '部门id',
  `status` int(11) DEFAULT NULL COMMENT '状态(1：启用  2：冻结  3：删除）',
  `createtime` datetime DEFAULT NULL COMMENT '创建时间',
  `version` int(11) DEFAULT NULL COMMENT '保留字段',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=50 DEFAULT CHARSET=utf8 COMMENT='管理员表';

-- ----------------------------
-- Records of sys_user
-- ----------------------------
-- 超级管理员(角色1) 密码: admin
INSERT INTO `sys_user` VALUES (1, 'b864a8e3-77d9-4869-b1ce-9a3d1d8885ad.png', 'admin', 'ecfadcde9305f8891bcfe5a1e28c253e', '8pgby', '赵鑫鑫', '2017-05-05 00:00:00', 1, 'sn93@qq.com', '18200000000', '1', 24, 1, '2016-01-29 08:49:53', 25);
-- 医生(角色5) 密码: doctor
INSERT INTO `sys_user` VALUES (47, '99c7a3d6-0694-49dd-b19f-73c1971e87d1.png', 'doctor', 'aadc22d1732bb641140ef39ffc4e6880', 'x7bl3', '赵医生', '2018-12-29 00:00:00', 1, '', '', '5', 25, 1, '2018-12-29 14:40:12', NULL);
-- 病人(角色6) 密码: patient
INSERT INTO `sys_user` VALUES (48, 'd4efe70d-9f79-4627-b126-b2a259513f26.jpg', 'patient', 'f39c4b4052706f3ef807e1d122c03ba4', 'pr8t2', '王洪超', NULL, 1, '', '', '6', 26, 1, '2018-12-29 15:06:33', NULL);
-- 病人(角色6) 密码: hy
INSERT INTO `sys_user` VALUES (49, '', 'hy', 'dfcd0ab49eb4aa9fe084dc4eb9bd47ce', 'wqhy5', 'hy', '2018-12-03 00:00:00', 1, '', '', '6', 26, 1, '2018-12-29 15:26:00', NULL);

-- ----------------------------
-- Table structure for test
-- ----------------------------
DROP TABLE IF EXISTS `test`;
CREATE TABLE `test` (
  `aaa` int(11) NOT NULL AUTO_INCREMENT,
  `bbb` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`aaa`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of test
-- ----------------------------

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `user_id` int(20) NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `user_idcard` varchar(255) NOT NULL,
  `user_password` varchar(255) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of user_info
-- ----------------------------
