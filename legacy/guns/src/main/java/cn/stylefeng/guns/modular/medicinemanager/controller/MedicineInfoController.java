package cn.stylefeng.guns.modular.medicinemanager.controller;

import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineStockService;
import cn.stylefeng.guns.modular.system.model.MedicineBatch;
import cn.stylefeng.guns.modular.system.model.MedicineStockIn;
import cn.stylefeng.guns.modular.system.model.MedicineStockOut;
import cn.stylefeng.guns.core.shiro.ShiroKit;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import cn.stylefeng.guns.core.log.LogObjectHolder;
import org.springframework.web.bind.annotation.RequestParam;
import cn.stylefeng.guns.modular.system.model.MedicineInfo;
import cn.stylefeng.guns.modular.medicinemanager.service.IMedicineInfoService;
import cn.stylefeng.guns.modular.system.dao.MedicineBatchMapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/medicineInfo")
public class MedicineInfoController extends BaseController {

    private String PREFIX = "/medicinemanager/medicineInfo/";

    @Autowired
    private IMedicineInfoService medicineInfoService;

    @Autowired
    private IMedicineStockService medicineStockService;

    @Autowired
    private MedicineBatchMapper medicineBatchMapper;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    setValue(new BigDecimal(text));
                }
            }
        });
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) {
                    setValue(null);
                } else {
                    try {
                        setValue(new java.text.SimpleDateFormat("yyyy-MM-dd").parse(text));
                    } catch (Exception e) {
                        try {
                            setValue(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm").parse(text));
                        } catch (Exception e2) {
                            setValue(null);
                        }
                    }
                }
            }
        });
    }

    @RequestMapping("")
    public String index() { return PREFIX + "medicineInfo.html"; }

    @RequestMapping("/medicineInfo_add")
    public String medicineInfoAdd() { return PREFIX + "medicineInfo_add.html"; }

    @RequestMapping("/medicineInfo_update/{medicineInfoId}")
    public String medicineInfoUpdate(@PathVariable Integer medicineInfoId, Model model) {
        MedicineInfo medicineInfo = medicineInfoService.selectById(medicineInfoId);
        model.addAttribute("item", medicineInfo);
        LogObjectHolder.me().set(medicineInfo);
        return PREFIX + "medicineInfo_edit.html";
    }

    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list() {
        EntityWrapper<MedicineInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("is_deleted", 0);
        return medicineInfoService.selectList(wrapper);
    }

    @RequestMapping(value = "/add")
    @ResponseBody
    public Object add(MedicineInfo medicineInfo) {
        medicineInfoService.insert(medicineInfo);
        return SUCCESS_TIP;
    }

    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete(@RequestParam Integer medicineInfoId) {
        MedicineInfo entity = medicineInfoService.selectById(medicineInfoId);
        entity.setIsDeleted(1);
        medicineInfoService.updateById(entity);
        return SUCCESS_TIP;
    }

    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update(MedicineInfo medicineInfo) {
        medicineInfoService.updateById(medicineInfo);
        return SUCCESS_TIP;
    }

    @RequestMapping(value = "/restore")
    @ResponseBody
    public Object restore(@RequestParam Integer medicineInfoId) {
        MedicineInfo entity = medicineInfoService.selectById(medicineInfoId);
        if (entity == null) return ResponseData.error("药品不存在");
        entity.setIsDeleted(0);
        medicineInfoService.updateById(entity);
        return SUCCESS_TIP;
    }

    @RequestMapping(value = "/deletedList")
    @ResponseBody
    public Object deletedList() {
        EntityWrapper<MedicineInfo> wrapper = new EntityWrapper<>();
        wrapper.eq("is_deleted", 1);
        return medicineInfoService.selectList(wrapper);
    }

    @RequestMapping(value = "/detail/{medicineInfoId}")
    @ResponseBody
    public Object detail(@PathVariable("medicineInfoId") Integer medicineInfoId) {
        return medicineInfoService.selectById(medicineInfoId);
    }

    // ==================== 库存管理 ====================

    /**
     * 入库操作
     */
    @RequestMapping(value = "/stockIn")
    @ResponseBody
    public Object stockIn(MedicineStockIn record) {
        try {
            record.setOperator(ShiroKit.getUser().getName());
            medicineStockService.stockIn(record);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("入库失败：" + e.getMessage());
        }
    }

    /**
     * 出库操作
     */
    @RequestMapping(value = "/stockOut")
    @ResponseBody
    public Object stockOut(MedicineStockOut record) {
        try {
            record.setOperator(ShiroKit.getUser().getName());
            medicineStockService.stockOut(record);
            return SUCCESS_TIP;
        } catch (Exception e) {
            return ResponseData.error("出库失败：" + e.getMessage());
        }
    }

    /**
     * 低库存预警
     */
    @RequestMapping(value = "/lowStock")
    @ResponseBody
    public Object lowStock() {
        return medicineStockService.getLowStockList();
    }

    /**
     * 批次查询
     */
    @RequestMapping(value = "/batches/{medicineId}")
    @ResponseBody
    public Object batches(@PathVariable Integer medicineId) {
        EntityWrapper<MedicineBatch> wrapper = new EntityWrapper<>();
        wrapper.eq("medicine_id", medicineId);
        wrapper.orderBy("expiry_date", true);
        return medicineBatchMapper.selectList(wrapper);
    }

    /**
     * 出入库记录
     */
    @RequestMapping(value = "/stockLog")
    @ResponseBody
    public Object stockLog(@RequestParam(required = false) Integer medicineId) {
        return medicineStockService.getStockLog(medicineId);
    }

    /**
     * 跳转到入库页
     */
    @RequestMapping("/medicine_stock_in")
    public String stockInPage() { return PREFIX + "medicine_stock_in.html"; }

    /**
     * 跳转到出库页
     */
    @RequestMapping("/medicine_stock_out")
    public String stockOutPage() { return PREFIX + "medicine_stock_out.html"; }
}
