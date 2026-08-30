package cn.stylefeng.guns.modular.system.model;

import java.util.Date;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author zxx
 * @since 2018-12-29
 */
@TableName("patient_history")
public class PatientHistory extends Model<PatientHistory> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("patient_idcard")
    private String patientIdcard;
    @TableField("patient_name")
    private String patientName;
    @TableField("patient_sym")
    private String patientSym;
    @TableField("patient_doctor")
    private String patientDoctor;
    @TableField("doctor_id")
    private Integer doctorId;
    @TableField("is_deleted")
    private Integer isDeleted;
    @TableField("patient_medicine")
    private String patientMedicine;
    @TableField("patient_history_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date patientHistoryDate;
    private Integer takeprice;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getPatientSym() {
        return patientSym;
    }

    public void setPatientSym(String patientSym) {
        this.patientSym = patientSym;
    }

    public String getPatientDoctor() {
        return patientDoctor;
    }

    public void setPatientDoctor(String patientDoctor) {
        this.patientDoctor = patientDoctor;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    public String getPatientMedicine() {
        return patientMedicine;
    }

    public void setPatientMedicine(String patientMedicine) {
        this.patientMedicine = patientMedicine;
    }

    public Date getPatientHistoryDate() {
        return patientHistoryDate;
    }

    public void setPatientHistoryDate(Date patientHistoryDate) {
        this.patientHistoryDate = patientHistoryDate;
    }

    public Integer getTakeprice() {
        return takeprice;
    }

    public void setTakeprice(Integer takeprice) {
        this.takeprice = takeprice;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "PatientHistory{" +
        ", id=" + id +
        ", patientIdcard=" + patientIdcard +
        ", patientName=" + patientName +
        ", patientSym=" + patientSym +
        ", patientDoctor=" + patientDoctor +
        ", patientMedicine=" + patientMedicine +
        ", patientHistoryDate=" + patientHistoryDate +
        ", takeprice=" + takeprice +
        "}";
    }
}
