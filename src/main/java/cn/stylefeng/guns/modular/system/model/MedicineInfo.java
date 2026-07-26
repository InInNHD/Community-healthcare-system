package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author zxx
 * @since 2018-12-29
 */
@TableName("medicine_info")
public class MedicineInfo extends Model<MedicineInfo> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("medicine_name")
    private String medicineName;
    @TableField("medicine_price")
    private Integer medicinePrice;
    @TableField("medicine_value")
    private String medicineValue;
    @TableField("medicine_image")
    private String medicineImage;
    @TableField("medicine_category")
    private String medicineCategory;
    @TableField("medicine_stock")
    private Integer medicineStock;
    @TableField("medicine_stock_min")
    private Integer medicineStockMin;
    @TableField("is_deleted")
    private Integer isDeleted;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public Integer getMedicinePrice() {
        return medicinePrice;
    }

    public void setMedicinePrice(Integer medicinePrice) {
        this.medicinePrice = medicinePrice;
    }

    public String getMedicineValue() {
        return medicineValue;
    }

    public void setMedicineValue(String medicineValue) {
        this.medicineValue = medicineValue;
    }

    public String getMedicineImage() {
        return medicineImage;
    }

    public void setMedicineImage(String medicineImage) {
        this.medicineImage = medicineImage;
    }

    public String getMedicineCategory() {
        return medicineCategory;
    }

    public void setMedicineCategory(String medicineCategory) {
        this.medicineCategory = medicineCategory;
    }

    public Integer getMedicineStock() {
        return medicineStock;
    }

    public void setMedicineStock(Integer medicineStock) {
        this.medicineStock = medicineStock;
    }

    public Integer getMedicineStockMin() {
        return medicineStockMin;
    }

    public void setMedicineStockMin(Integer medicineStockMin) {
        this.medicineStockMin = medicineStockMin;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "MedicineInfo{" +
        ", id=" + id +
        ", medicineName=" + medicineName +
        ", medicinePrice=" + medicinePrice +
        ", medicineValue=" + medicineValue +
        ", medicineImage=" + medicineImage +
        ", medicineCategory=" + medicineCategory +
        ", medicineStock=" + medicineStock +
        "}";
    }
}
