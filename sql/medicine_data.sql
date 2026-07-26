-- ============================================================
-- 药品信息：修复数据 + 插入常见药品
-- 数据库：bs1
-- ============================================================
SET NAMES utf8;

-- 修复已有数据的分类（之前编码问题导致乱码）
UPDATE `medicine_info` SET `medicine_image` = '/static/img/medicine/cold.svg', `medicine_category` = '感冒用药' WHERE `id` = 1;
UPDATE `medicine_info` SET `medicine_image` = '/static/img/medicine/fever.svg', `medicine_category` = '解热镇痛' WHERE `id` = 2;
UPDATE `medicine_info` SET `medicine_image` = '/static/img/medicine/cardiovascular.svg', `medicine_category` = '心血管药' WHERE `id` = 4;

-- 插入常见药品信息
INSERT INTO `medicine_info` (`medicine_name`, `medicine_price`, `medicine_value`, `medicine_image`, `medicine_category`) VALUES
('连花清瘟胶囊', 26, '清瘟解毒，宣肺泄热，用于治疗流行性感冒', '/static/img/medicine/detox.svg', '清热解毒'),
('布洛芬缓释胶囊', 18, '解热镇痛，用于缓解轻至中度疼痛及感冒发热', '/static/img/medicine/fever.svg', '解热镇痛'),
('阿莫西林胶囊', 22, '抗菌消炎，用于敏感菌所致的呼吸道、泌尿道感染', '/static/img/medicine/antibiotic.svg', '抗菌消炎'),
('头孢克洛缓释片', 35, '广谱抗菌，用于呼吸道、皮肤软组织感染', '/static/img/medicine/antibiotic.svg', '抗菌消炎'),
('云南白药气雾剂', 42, '活血散瘀，消肿止痛，用于跌打损伤', '/static/img/medicine/external.svg', '外用药'),
('六味地黄丸', 28, '滋阴补肾，用于肾阴亏损、头晕耳鸣', '/static/img/medicine/tonic.svg', '滋补养生'),
('藿香正气水', 15, '解表化湿，理气和中，用于外感风寒、内伤湿滞', '/static/img/medicine/stomach.svg', '肠胃用药'),
('蒙脱石散', 19, '用于成人及儿童急慢性腹泻', '/static/img/medicine/stomach.svg', '肠胃用药'),
('维生素C片', 12, '补充维生素C，增强免疫力，促进铁吸收', '/static/img/medicine/vitamin.svg', '维生素'),
('复方丹参滴丸', 32, '活血化瘀，理气止痛，用于气滞血瘀所致的胸痹', '/static/img/medicine/cardiovascular.svg', '心血管药'),
('速效救心丸', 48, '行气活血，祛瘀止痛，用于气滞血瘀型冠心病', '/static/img/medicine/cardiovascular.svg', '心血管药'),
('开瑞坦', 25, '缓解过敏性鼻炎、慢性荨麻疹等相关症状', '/static/img/medicine/respiratory.svg', '呼吸用药'),
('西瓜霜润喉片', 10, '清热解毒，消肿止痛，用于咽喉肿痛、口舌生疮', '/static/img/medicine/ent.svg', '眼耳鼻喉'),
('红霉素眼膏', 8, '用于沙眼、结膜炎、睑缘炎及眼外部感染', '/static/img/medicine/ent.svg', '眼耳鼻喉'),
('创可贴', 5, '用于小创口、擦伤等浅表性皮肤创伤的急救', '/static/img/medicine/external.svg', '外用药'),
('板蓝根颗粒', 12, '清热解毒，凉血利咽，用于病毒性感冒', '/static/img/medicine/cold.svg', '感冒用药');
