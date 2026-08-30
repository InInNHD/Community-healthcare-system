package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName("maternal_postpartum_visit")
public class MaternalPostpartumVisit extends Model<MaternalPostpartumVisit> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("maternal_id") private Integer maternalId;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    @TableField("visit_date") private Date visitDate;
    @TableField("visit_day") private Integer visitDay;
    private String lochia;
    @TableField("uterine_involution") private String uterineInvolution;
    @TableField("wound_healing") private String woundHealing;
    private String breastfeeding;
    @TableField("neonate_weight") private BigDecimal neonateWeight;
    @TableField("neonate_jaundice") private String neonateJaundice;
    @TableField("visit_doctor") private String visitDoctor;
    private String advice;
    @TableField("next_visit_date") private Date nextVisitDate;
    private Integer status;
    @TableField("is_deleted") private Integer isDeleted;
    @TableField("create_time") private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getMaternalId() { return maternalId; }
    public void setMaternalId(Integer maternalId) { this.maternalId = maternalId; }
    public String getPatientIdcard() { return patientIdcard; }
    public void setPatientIdcard(String patientIdcard) { this.patientIdcard = patientIdcard; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Date getVisitDate() { return visitDate; }
    public void setVisitDate(Date visitDate) { this.visitDate = visitDate; }
    public Integer getVisitDay() { return visitDay; }
    public void setVisitDay(Integer visitDay) { this.visitDay = visitDay; }
    public String getLochia() { return lochia; }
    public void setLochia(String lochia) { this.lochia = lochia; }
    public String getUterineInvolution() { return uterineInvolution; }
    public void setUterineInvolution(String uterineInvolution) { this.uterineInvolution = uterineInvolution; }
    public String getWoundHealing() { return woundHealing; }
    public void setWoundHealing(String woundHealing) { this.woundHealing = woundHealing; }
    public String getBreastfeeding() { return breastfeeding; }
    public void setBreastfeeding(String breastfeeding) { this.breastfeeding = breastfeeding; }
    public BigDecimal getNeonateWeight() { return neonateWeight; }
    public void setNeonateWeight(BigDecimal neonateWeight) { this.neonateWeight = neonateWeight; }
    public String getNeonateJaundice() { return neonateJaundice; }
    public void setNeonateJaundice(String neonateJaundice) { this.neonateJaundice = neonateJaundice; }
    public String getVisitDoctor() { return visitDoctor; }
    public void setVisitDoctor(String visitDoctor) { this.visitDoctor = visitDoctor; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public Date getNextVisitDate() { return nextVisitDate; }
    public void setNextVisitDate(Date nextVisitDate) { this.nextVisitDate = nextVisitDate; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    @Override protected Serializable pkVal() { return this.id; }
}
