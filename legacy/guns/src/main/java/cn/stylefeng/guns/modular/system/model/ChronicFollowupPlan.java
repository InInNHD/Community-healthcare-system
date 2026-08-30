package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * 慢病随访计划模型
 */
@TableName("chronic_followup_plan")
public class ChronicFollowupPlan extends Model<ChronicFollowupPlan> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("chronic_id")
    private Integer chronicId;

    @TableField("patient_idcard")
    private String patientIdcard;

    @TableField("patient_name")
    private String patientName;

    @TableField("disease_type")
    private String diseaseType;

    @TableField("plan_date")
    private Date planDate;

    @TableField("plan_type")
    private String planType;

    @TableField("status")
    private Integer status;

    @TableField("is_deleted")
    private Integer isDeleted;

    @TableField("doctor_name")
    private String doctorName;

    @TableField("doctor_id")
    private Integer doctorId;

    @TableField("create_time")
    private Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getChronicId() {
        return chronicId;
    }

    public void setChronicId(Integer chronicId) {
        this.chronicId = chronicId;
    }

    public String getPatientIdcard() {
        return patientIdcard;
    }

    public void setPatientIdcard(String patientIdcard) {
        this.patientIdcard = patientIdcard;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDiseaseType() {
        return diseaseType;
    }

    public void setDiseaseType(String diseaseType) {
        this.diseaseType = diseaseType;
    }

    public Date getPlanDate() {
        return planDate;
    }

    public void setPlanDate(Date planDate) {
        this.planDate = planDate;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "ChronicFollowupPlan{" +
                "id=" + id +
                ", chronicId=" + chronicId +
                ", patientName='" + patientName + '\'' +
                ", diseaseType='" + diseaseType + '\'' +
                ", planDate=" + planDate +
                '}';
    }
}
