package cn.stylefeng.guns.modular.medicinemanager.service.impl;

import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineStockService;
import cn.stylefeng.guns.modular.system.dao.MedicineBatchMapper;
import cn.stylefeng.guns.modular.system.dao.MedicineStockInMapper;
import cn.stylefeng.guns.modular.system.dao.MedicineStockOutMapper;
import cn.stylefeng.guns.modular.system.model.MedicineBatch;
import cn.stylefeng.guns.modular.system.model.MedicineInfo;
import cn.stylefeng.guns.modular.system.model.MedicineStockIn;
import cn.stylefeng.guns.modular.system.model.MedicineStockOut;
import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineInfoService;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MedicineStockServiceImpl implements IMedicineStockService {

    @Autowired
    private MedicineStockInMapper stockInMapper;
    @Autowired
    private MedicineStockOutMapper stockOutMapper;
    @Autowired
    private MedicineBatchMapper batchMapper;
    @Autowired
    private IMedicineInfoService medicineInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockIn(MedicineStockIn record) {
        MedicineInfo medicine = medicineInfoService.selectById(record.getMedicineId());
        if (medicine == null) {
            throw new RuntimeException("药品不存在");
        }
        // 更新库存
        int newStock = medicine.getMedicineStock() + record.getQuantity();
        medicine.setMedicineStock(newStock);
        medicineInfoService.updateById(medicine);

        // 写入库记录
        record.setCreateTime(new Date());
        stockInMapper.insert(record);

        // 批次管理：查找或创建批次
        EntityWrapper<MedicineBatch> batchWrapper = new EntityWrapper<>();
        batchWrapper.eq("medicine_id", record.getMedicineId());
        batchWrapper.eq("batch_no", record.getBatchNo());
        MedicineBatch batch = batchMapper.selectList(batchWrapper).stream().findFirst().orElse(null);

        if (batch == null) {
            batch = new MedicineBatch();
            batch.setMedicineId(record.getMedicineId());
            batch.setBatchNo(record.getBatchNo());
            batch.setExpiryDate(record.getExpiryDate());
            batch.setInitialQuantity(record.getQuantity());
            batch.setRemainingQuantity(record.getQuantity());
            batch.setStatus(1);
            batch.setCreateTime(new Date());
            batchMapper.insert(batch);
        } else {
            batch.setRemainingQuantity(batch.getRemainingQuantity() + record.getQuantity());
            if (record.getExpiryDate() != null) {
                batch.setExpiryDate(record.getExpiryDate());
            }
            batch.setStatus(1);
            batchMapper.updateById(batch);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MedicineStockOut stockOut(MedicineStockOut record) {
        MedicineInfo medicine = medicineInfoService.selectById(record.getMedicineId());
        if (medicine == null) {
            throw new RuntimeException("药品不存在");
        }
        if (medicine.getMedicineStock() < record.getQuantity()) {
            throw new RuntimeException("库存不足，当前库存：" + medicine.getMedicineStock());
        }
        // 扣减库存
        int newStock = medicine.getMedicineStock() - record.getQuantity();
        medicine.setMedicineStock(newStock);
        medicineInfoService.updateById(medicine);

        // 写出库记录
        record.setCreateTime(new Date());
        stockOutMapper.insert(record);

        // 扣减批次剩余量
        if (record.getBatchNo() != null && !record.getBatchNo().isEmpty()) {
            EntityWrapper<MedicineBatch> batchWrapper = new EntityWrapper<>();
            batchWrapper.eq("medicine_id", record.getMedicineId());
            batchWrapper.eq("batch_no", record.getBatchNo());
            batchWrapper.eq("status", 1);
            MedicineBatch batch = batchMapper.selectList(batchWrapper).stream().findFirst().orElse(null);
            if (batch != null) {
                int remaining = batch.getRemainingQuantity() - record.getQuantity();
                batch.setRemainingQuantity(Math.max(0, remaining));
                if (batch.getRemainingQuantity() <= 0) {
                    batch.setStatus(0);
                }
                batchMapper.updateById(batch);
            }
        }

        return record;
    }

    @Override
    public List<MedicineInfo> getLowStockList() {
        EntityWrapper<MedicineInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("is_deleted", 0);
        wrapper.le("medicine_stock", "medicine_stock_min");
        return medicineInfoService.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> getStockLog(Integer medicineId) {
        List<Map<String, Object>> result = new ArrayList<>();
        Set<Integer> medicineIds = new HashSet<>();

        EntityWrapper<MedicineStockIn> inWrapper = new EntityWrapper<>();
        if (medicineId != null) inWrapper.eq("medicine_id", medicineId);
        inWrapper.orderBy("create_time", false);
        inWrapper.last("LIMIT 50");
        List<MedicineStockIn> inList = stockInMapper.selectList(inWrapper);
        for (MedicineStockIn in : inList) {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "入库");
            m.put("medicineId", in.getMedicineId());
            m.put("batchNo", in.getBatchNo());
            m.put("quantity", "+" + in.getQuantity());
            m.put("operator", in.getOperator());
            m.put("createTime", in.getCreateTime());
            m.put("remark", in.getRemark());
            result.add(m);
            medicineIds.add(in.getMedicineId());
        }

        EntityWrapper<MedicineStockOut> outWrapper = new EntityWrapper<>();
        if (medicineId != null) outWrapper.eq("medicine_id", medicineId);
        outWrapper.orderBy("create_time", false);
        outWrapper.last("LIMIT 50");
        List<MedicineStockOut> outList = stockOutMapper.selectList(outWrapper);
        for (MedicineStockOut out : outList) {
            Map<String, Object> m = new HashMap<>();
            m.put("type", "出库");
            m.put("medicineId", out.getMedicineId());
            m.put("batchNo", out.getBatchNo());
            m.put("quantity", "-" + out.getQuantity());
            m.put("operator", out.getOperator());
            m.put("createTime", out.getCreateTime());
            m.put("reason", out.getReason());
            m.put("patientName", out.getPatientName());
            result.add(m);
            medicineIds.add(out.getMedicineId());
        }

        // 批量加载药品名称
        Map<Integer, String> nameMap = new HashMap<>();
        for (Integer id : medicineIds) {
            MedicineInfo info = medicineInfoService.selectById(id);
            if (info != null) {
                nameMap.put(id, info.getMedicineName());
            }
        }
        for (Map<String, Object> m : result) {
            Integer mid = (Integer) m.get("medicineId");
            m.put("medicineName", nameMap.getOrDefault(mid, ""));
        }

        result.sort((a, b) -> {
            Date da = (Date) a.get("createTime");
            Date db = (Date) b.get("createTime");
            if (da == null || db == null) return 0;
            return db.compareTo(da);
        });
        if (result.size() > 50) {
            result = result.subList(0, 50);
        }
        return result;
    }
}
