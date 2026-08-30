package com.community.healthcare.clinical;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 处理管理端的单药品库存调整。
 *
 * <p>查询使用悲观写锁串行化同一药品的并发调整，实体负责拒绝负库存；
 * R3 批次库存和处方发药则由 pharmacy 模块的事务服务处理。</p>
 */
@Service
class MedicineInventoryService {
    private final MedicineRepository medicines;

    MedicineInventoryService(MedicineRepository medicines) {
        this.medicines = medicines;
    }

    /** 正数入库、负数出库，零变化被视为无效请求。 */
    @Transactional
    MedicineResponse adjust(Long medicineId, int delta) {
        if (delta == 0) throw new IllegalArgumentException("库存调整量不能为 0");
        Medicine medicine = medicines.findByIdForStockUpdate(medicineId)
                .orElseThrow(() -> new EntityNotFoundException("药品不存在"));
        medicine.adjustStock(delta);
        return MedicineResponse.from(medicine);
    }
}
