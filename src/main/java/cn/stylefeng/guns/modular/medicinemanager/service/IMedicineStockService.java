package cn.stylefeng.guns.modular.medicinemanager.service;

import cn.stylefeng.guns.modular.system.model.MedicineInfo;
import cn.stylefeng.guns.modular.system.model.MedicineStockIn;
import cn.stylefeng.guns.modular.system.model.MedicineStockOut;

import java.util.Map;

/**
 * 药品库存管理 Service
 */
public interface IMedicineStockService {

    /**
     * 入库：增加库存 + 写入库记录 + 创建/更新批次
     */
    void stockIn(MedicineStockIn record);

    /**
     * 出库：扣减库存 + 写出库记录 + 扣减批次
     * @return 出库记录
     */
    MedicineStockOut stockOut(MedicineStockOut record);

    /**
     * 获取低库存药品列表（库存 <= 预警阈值）
     */
    java.util.List<MedicineInfo> getLowStockList();

    /**
     * 获取出入库记录（含药品名，按时间倒序，最近50条）
     */
    java.util.List<Map<String, Object>> getStockLog(Integer medicineId);
}
