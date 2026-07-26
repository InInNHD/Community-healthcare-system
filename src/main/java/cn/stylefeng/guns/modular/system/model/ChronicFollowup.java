package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import java.io.Serializable;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 慢病随访记录模型
 */
@TableName("chronic_followup")
public class ChronicFollowup extends Model<ChronicFollowup> {

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

    @TableField("followup_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date followupDate;

    @TableField("followup_doctor")
    private String followupDoctor;

    @TableField("followup_doctor_id")
    private Integer followupDoctorId;

    @TableField("followup_type")
    private String followupType;

    @TableField("symptoms")
    private String symptoms;

    @TableField("blood_pressure")
    private String bloodPressure;

    @TableField("blood_sugar")
    private String bloodSugar;

    @TableField("heart_rate")
    private Integer heartRate;

    @TableField("medication_compliance")
    private String medicationCompliance;

    @TableField("lifestyle_advice")
    private String lifestyleAdvice;

    @TableField("next_followup_date")
    private Date nextFollowupDate;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("status")
    private Integer status;

    @TableField("is_deleted")
    private Integer isDeleted;

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

    public Date getFollowupDate() {
        return followupDate;
    }

    public void setFollowupDate(Date followupDate) {
        this.followupDate = followupDate;
    }

    public String getFollowupDoctor() {
        return followupDoctor;
    }

    public void setFollowupDoctor(String followupDoctor) {
        this.followupDoctor = followupDoctor;
    }

    public Integer getFollowupDoctorId() {
        return followupDoctorId;
    }

    public void setFollowupDoctorId(Integer followupDoctorId) {
        this.followupDoctorId = followupDoctorId;
    }

    public String getFollowupType() {
        return followupType;
    }

    public void setFollowupType(String followupType) {
        this.followupType = followupType;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public String getBloodSugar() {
        return bloodSugar;
    }

    public void setBloodSugar(String bloodSugar) {
        this.bloodSugar = bloodSugar;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public String getMedicationCompliance() {
        return medicationCompliance;
    }

    public void setMedicationCompliance(String medicationCompliance) {
        this.medicationCompliance = medicationCompliance;
    }

    public String getLifestyleAdvice() {
        return lifestyleAdvice;
    }

    public void setLifestyleAdvice(String lifestyleAdvice) {
        this.lifestyleAdvice = lifestyleAdvice;
    }

    public Date getNextFollowupDate() {
        return nextFollowupDate;
    }

    public void setNextFollowupDate(Date nextFollowupDate) {
        this.nextFollowupDate = nextFollowupDate;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "ChronicFollowup{" +
                "id=" + id +
                ", chronicId=" + chronicId +
                ", patientName='" + patientName + '\'' +
                ", diseaseType='" + diseaseType + '\'' +
                ", followupDate=" + followupDate +
                '}';
    }
}
