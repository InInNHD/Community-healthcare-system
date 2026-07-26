package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName("elderly_checkup")
public class ElderlyCheckup extends Model<ElderlyCheckup> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    private Integer age;
    private String gender;
    @TableField("checkup_date") private Date checkupDate;
    private BigDecimal height;
    private BigDecimal weight;
    private BigDecimal bmi;
    @TableField("blood_pressure") private String bloodPressure;
    @TableField("heart_rate") private Integer heartRate;
    @TableField("blood_sugar") private BigDecimal bloodSugar;
    @TableField("blood_lipid") private String bloodLipid;
    @TableField("liver_function") private String liverFunction;
    @TableField("kidney_function") private String kidneyFunction;
    private String ecg;
    @TableField("b_ultrasound") private String bUltrasound;
    @TableField("urine_routine") private String urineRoutine;
    @TableField("vision_left") private BigDecimal visionLeft;
    @TableField("vision_right") private BigDecimal visionRight;
    @TableField("self_care_assessment") private String selfCareAssessment;
    @TableField("health_assessment") private String healthAssessment;
    private String advice;
    @TableField("doctor_name") private String doctorName;
    @TableField("is_deleted") private Integer isDeleted;
    @TableField("create_time") private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPatientIdcard() { return patientIdcard; }
    public void setPatientIdcard(String patientIdcard) { this.patientIdcard = patientIdcard; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Date getCheckupDate() { return checkupDate; }
    public void setCheckupDate(Date checkupDate) { this.checkupDate = checkupDate; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public BigDecimal getBloodSugar() { return bloodSugar; }
    public void setBloodSugar(BigDecimal bloodSugar) { this.bloodSugar = bloodSugar; }
    public String getBloodLipid() { return bloodLipid; }
    public void setBloodLipid(String bloodLipid) { this.bloodLipid = bloodLipid; }
    public String getLiverFunction() { return liverFunction; }
    public void setLiverFunction(String liverFunction) { this.liverFunction = liverFunction; }
    public String getKidneyFunction() { return kidneyFunction; }
    public void setKidneyFunction(String kidneyFunction) { this.kidneyFunction = kidneyFunction; }
    public String getEcg() { return ecg; }
    public void setEcg(String ecg) { this.ecg = ecg; }
    public String getBUltrasound() { return bUltrasound; }
    public void setBUltrasound(String bUltrasound) { this.bUltrasound = bUltrasound; }
    public String getUrineRoutine() { return urineRoutine; }
    public void setUrineRoutine(String urineRoutine) { this.urineRoutine = urineRoutine; }
    public BigDecimal getVisionLeft() { return visionLeft; }
    public void setVisionLeft(BigDecimal visionLeft) { this.visionLeft = visionLeft; }
    public BigDecimal getVisionRight() { return visionRight; }
    public void setVisionRight(BigDecimal visionRight) { this.visionRight = visionRight; }
    public String getSelfCareAssessment() { return selfCareAssessment; }
    public void setSelfCareAssessment(String selfCareAssessment) { this.selfCareAssessment = selfCareAssessment; }
    public String getHealthAssessment() { return healthAssessment; }
    public void setHealthAssessment(String healthAssessment) { this.healthAssessment = healthAssessment; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    @Override protected Serializable pkVal() { return this.id; }
}
