package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@TableName("child_checkup")
public class ChildCheckup extends Model<ChildCheckup> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    @TableField("child_name") private String childName;
    private String gender;
    @TableField("birth_date") private Date birthDate;
    @TableField("checkup_date") private Date checkupDate;
    private BigDecimal height;
    private BigDecimal weight;
    @TableField("head_circumference") private BigDecimal headCircumference;
    private BigDecimal hemoglobin;
    @TableField("development_assessment") private String developmentAssessment;
    @TableField("nutrition_status") private String nutritionStatus;
    private String advice;
    @TableField("doctor_name") private String doctorName;
    @TableField("next_checkup_date") private Date nextCheckupDate;
    @TableField("is_deleted") private Integer isDeleted;
    @TableField("create_time") private Date createTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getPatientIdcard() { return patientIdcard; }
    public void setPatientIdcard(String patientIdcard) { this.patientIdcard = patientIdcard; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }
    public Date getCheckupDate() { return checkupDate; }
    public void setCheckupDate(Date checkupDate) { this.checkupDate = checkupDate; }
    public BigDecimal getHeight() { return height; }
    public void setHeight(BigDecimal height) { this.height = height; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public BigDecimal getHeadCircumference() { return headCircumference; }
    public void setHeadCircumference(BigDecimal headCircumference) { this.headCircumference = headCircumference; }
    public BigDecimal getHemoglobin() { return hemoglobin; }
    public void setHemoglobin(BigDecimal hemoglobin) { this.hemoglobin = hemoglobin; }
    public String getDevelopmentAssessment() { return developmentAssessment; }
    public void setDevelopmentAssessment(String developmentAssessment) { this.developmentAssessment = developmentAssessment; }
    public String getNutritionStatus() { return nutritionStatus; }
    public void setNutritionStatus(String nutritionStatus) { this.nutritionStatus = nutritionStatus; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public Date getNextCheckupDate() { return nextCheckupDate; }
    public void setNextCheckupDate(Date nextCheckupDate) { this.nextCheckupDate = nextCheckupDate; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    @Override protected Serializable pkVal() { return this.id; }
}
