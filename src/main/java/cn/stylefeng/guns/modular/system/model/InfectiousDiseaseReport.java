package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;
import java.util.Date;

@TableName("infectious_disease_report")
public class InfectiousDiseaseReport extends Model<InfectiousDiseaseReport> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("patient_idcard") private String patientIdcard;
    @TableField("patient_name") private String patientName;
    private Integer age;
    private String gender;
    @TableField("disease_type") private String diseaseType;
    @TableField("disease_category") private String diseaseCategory;
    @TableField("onset_date") private Date onsetDate;
    @TableField("diagnosis_date") private Date diagnosisDate;
    @TableField("report_date") private Date reportDate;
    @TableField("report_doctor") private String reportDoctor;
    @TableField("report_hospital") private String reportHospital;
    private String symptoms;
    @TableField("isolation_status") private String isolationStatus;
    @TableField("close_contacts_count") private Integer closeContactsCount;
    private String measures;
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
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }
    public String getDiseaseCategory() { return diseaseCategory; }
    public void setDiseaseCategory(String diseaseCategory) { this.diseaseCategory = diseaseCategory; }
    public Date getOnsetDate() { return onsetDate; }
    public void setOnsetDate(Date onsetDate) { this.onsetDate = onsetDate; }
    public Date getDiagnosisDate() { return diagnosisDate; }
    public void setDiagnosisDate(Date diagnosisDate) { this.diagnosisDate = diagnosisDate; }
    public Date getReportDate() { return reportDate; }
    public void setReportDate(Date reportDate) { this.reportDate = reportDate; }
    public String getReportDoctor() { return reportDoctor; }
    public void setReportDoctor(String reportDoctor) { this.reportDoctor = reportDoctor; }
    public String getReportHospital() { return reportHospital; }
    public void setReportHospital(String reportHospital) { this.reportHospital = reportHospital; }
    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }
    public String getIsolationStatus() { return isolationStatus; }
    public void setIsolationStatus(String isolationStatus) { this.isolationStatus = isolationStatus; }
    public Integer getCloseContactsCount() { return closeContactsCount; }
    public void setCloseContactsCount(Integer closeContactsCount) { this.closeContactsCount = closeContactsCount; }
    public String getMeasures() { return measures; }
    public void setMeasures(String measures) { this.measures = measures; }
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
