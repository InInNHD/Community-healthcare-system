package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.util.Date;

@TableName("maternal_record")
public class MaternalRecord extends Model<MaternalRecord> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    private Integer age;
    @TableField("lmp_date") private Date lmpDate;
    @TableField("edd_date") private Date eddDate;
    private Integer gravidity;
    private Integer parity;
    @TableField("blood_type") private String bloodType;
    @TableField("high_risk_flag") private Integer highRiskFlag;
    @TableField("high_risk_reason") private String highRiskReason;
    @TableField("register_date") private Date registerDate;
    @TableField("doctor_name") private String doctorName;
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
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Date getLmpDate() { return lmpDate; }
    public void setLmpDate(Date lmpDate) { this.lmpDate = lmpDate; }
    public Date getEddDate() { return eddDate; }
    public void setEddDate(Date eddDate) { this.eddDate = eddDate; }
    public Integer getGravidity() { return gravidity; }
    public void setGravidity(Integer gravidity) { this.gravidity = gravidity; }
    public Integer getParity() { return parity; }
    public void setParity(Integer parity) { this.parity = parity; }
    public String getBloodType() { return bloodType; }
    public void setBloodType(String bloodType) { this.bloodType = bloodType; }
    public Integer getHighRiskFlag() { return highRiskFlag; }
    public void setHighRiskFlag(Integer highRiskFlag) { this.highRiskFlag = highRiskFlag; }
    public String getHighRiskReason() { return highRiskReason; }
    public void setHighRiskReason(String highRiskReason) { this.highRiskReason = highRiskReason; }
    public Date getRegisterDate() { return registerDate; }
    public void setRegisterDate(Date registerDate) { this.registerDate = registerDate; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
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
