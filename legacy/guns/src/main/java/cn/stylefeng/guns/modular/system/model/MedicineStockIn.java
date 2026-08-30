package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

@TableName("medicine_stock_in")
public class MedicineStockIn extends Model<MedicineStockIn> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("medicine_id")
    private Integer medicineId;
    @TableField("batch_no")
    private String batchNo;
    private Integer quantity;
    @TableField("unit_price")
    private BigDecimal unitPrice;
    private String supplier;
    @TableField("expiry_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date expiryDate;
    private String operator;
    @TableField("create_time")
    private Date createTime;
    private String remark;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMedicineId() { return medicineId; }
    public void setMedicineId(Integer medicineId) { this.medicineId = medicineId; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    protected Serializable pkVal() { return this.id; }
}
