-- 公共卫生服务模块 - 菜单和权限
INSERT INTO sys_menu (id, code, pcode, pcodes, name, icon, url, num, levels, ismenu, tips, status, isopen) VALUES
(4000000000000001, 'public_health', '0', '[0],', '公共卫生服务', 'fa-ambulance', '#', 6, 1, 1, '预防接种/妇幼保健/老年人体检/传染病上报', 1, 1),
(4000000000000002, 'vaccination', 'public_health', '[0],[public_health],', '预防接种管理', 'fa-syringe', '/vaccination', 1, 2, 1, NULL, 1, 1),
(4000000000000003, 'maternal', 'public_health', '[0],[public_health],', '妇幼保健', 'fa-female', '/maternal', 2, 2, 1, NULL, 1, 1),
(4000000000000004, 'elderly', 'public_health', '[0],[public_health],', '老年人健康体检', 'fa-user-md', '/elderly', 3, 2, 1, NULL, 1, 1),
(4000000000000005, 'infectious', 'public_health', '[0],[public_health],', '传染病上报', 'fa-bug', '/infectious', 4, 2, 1, NULL, 1, 1);

-- 角色1(超级管理员) - 全权限
INSERT INTO sys_relation (menuid, roleid) VALUES (4000000000000001,1),(4000000000000002,1),(4000000000000003,1),(4000000000000004,1),(4000000000000005,1);
-- 角色5(医生) - 全权限
INSERT INTO sys_relation (menuid, roleid) VALUES (4000000000000001,5),(4000000000000002,5),(4000000000000003,5),(4000000000000004,5),(4000000000000005,5);
-- 角色6(居民) - 查阅本人
INSERT INTO sys_relation (menuid, roleid) VALUES (4000000000000001,6),(4000000000000002,6),(4000000000000003,6),(4000000000000004,6);
