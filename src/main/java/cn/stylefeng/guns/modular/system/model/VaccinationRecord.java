package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.util.Date;

@TableName("vaccination_record")
public class VaccinationRecord extends Model<VaccinationRecord> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    @TableField("vaccine_name") private String vaccineName;
    @TableField("dose_seq") private Integer doseSeq;
    @TableField("vacc_date") private Date vaccDate;
    @TableField("vacc_site") private String vaccSite;
    @TableField("batch_no") private String batchNo;
    private String manufacturer;
    @TableField("vacc_doctor") private String vaccDoctor;
    @TableField("next_date") private Date nextDate;
    private Integer status;
    @TableField("is_deleted") private Integer isDeleted;
    @TableField("create_time") private Date createTime;
    private String remark;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPatientIdcard() { return patientIdcard; }
    public void setPatientIdcard(String patientIdcard) { this.patientIdcard = patientIdcard; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public Integer getDoseSeq() { return doseSeq; }
    public void setDoseSeq(Integer doseSeq) { this.doseSeq = doseSeq; }
    public Date getVaccDate() { return vaccDate; }
    public void setVaccDate(Date vaccDate) { this.vaccDate = vaccDate; }
    public String getVaccSite() { return vaccSite; }
    public void setVaccSite(String vaccSite) { this.vaccSite = vaccSite; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getVaccDoctor() { return vaccDoctor; }
    public void setVaccDoctor(String vaccDoctor) { this.vaccDoctor = vaccDoctor; }
    public Date getNextDate() { return nextDate; }
    public void setNextDate(Date nextDate) { this.nextDate = nextDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    @Override protected Serializable pkVal() { return this.id; }
}
