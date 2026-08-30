-- V7：空库启用策略下禁止旧居民证件明文继续留存；旧系统仅供独立历史查询。
UPDATE patient
SET id_card = CONCAT('****', RIGHT(id_card, 4), '-', id)
WHERE id_card IS NOT NULL
  AND id_card NOT LIKE '****%';
