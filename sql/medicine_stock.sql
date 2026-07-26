-- ============================================================
-- 药品信息表：新增库存余量字段 + 参考社区医院真实数据赋值
-- 数据库：bs1
-- 使用说明：若表已有 medicine_stock 列，跳过 ALTER 直接执行 UPDATE 即可
-- ============================================================
SET NAMES utf8;

-- 新增库存余量字段（若已存在则跳过）
ALTER TABLE `medicine_info` ADD COLUMN `medicine_stock` int(11) DEFAULT 0 COMMENT '库存余量' AFTER `medicine_category`;

-- 按药品名称赋值库存（参考社区医院实际用量）
-- 感冒用药：OTC常备，需求量较大
UPDATE `medicine_info` SET `medicine_stock` = 250 WHERE `medicine_name` = '板蓝根';
UPDATE `medicine_info` SET `medicine_stock` = 200 WHERE `medicine_name` = '板蓝根颗粒';
UPDATE `medicine_info` SET `medicine_stock` = 120 WHERE `medicine_name` = '连花清瘟胶囊';

-- 解热镇痛：常规药，库存较充足
UPDATE `medicine_info` SET `medicine_stock` = 50  WHERE `medicine_name` = '青霉素';
UPDATE `medicine_info` SET `medicine_stock` = 150 WHERE `medicine_name` = '布洛芬缓释胶囊';

-- 抗菌消炎：处方抗生素，严格管控，库存偏紧
UPDATE `medicine_info` SET `medicine_stock` = 35  WHERE `medicine_name` = '阿莫西林胶囊';
UPDATE `medicine_info` SET `medicine_stock` = 22  WHERE `medicine_name` = '头孢克洛缓释片';

-- 外用药
UPDATE `medicine_info` SET `medicine_stock` = 60  WHERE `medicine_name` = '云南白药气雾剂';
UPDATE `medicine_info` SET `medicine_stock` = 600 WHERE `medicine_name` = '创可贴';

-- 心血管药
UPDATE `medicine_info` SET `medicine_stock` = 30  WHERE `medicine_name` = '脑心通胶囊';
UPDATE `medicine_info` SET `medicine_stock` = 45  WHERE `medicine_name` = '复方丹参滴丸';
UPDATE `medicine_info` SET `medicine_stock` = 18  WHERE `medicine_name` = '速效救心丸';

-- 滋补养生
UPDATE `medicine_info` SET `medicine_stock` = 80  WHERE `medicine_name` = '六味地黄丸';

-- 肠胃用药
UPDATE `medicine_info` SET `medicine_stock` = 100 WHERE `medicine_name` = '藿香正气水';
UPDATE `medicine_info` SET `medicine_stock` = 90  WHERE `medicine_name` = '蒙脱石散';

-- 维生素类
UPDATE `medicine_info` SET `medicine_stock` = 400 WHERE `medicine_name` = '维生素C片';

-- 呼吸/抗过敏
UPDATE `medicine_info` SET `medicine_stock` = 65  WHERE `medicine_name` = '开瑞坦';

-- 眼耳鼻喉
UPDATE `medicine_info` SET `medicine_stock` = 160 WHERE `medicine_name` = '西瓜霜润喉片';
UPDATE `medicine_info` SET `medicine_stock` = 25  WHERE `medicine_name` = '红霉素眼膏';
