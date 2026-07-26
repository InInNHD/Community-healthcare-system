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
@TableName("doctor_point")
public class DoctorPoint extends Model<DoctorPoint> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("patient_idcard")
    private String patientIdcard;
    @TableField("patient_name")
    private String patientName;
    @TableField("doctor_name")
    private String doctorName;
    @TableField("doctor_id")
    private Integer doctorId;
    @TableField("point_date")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date pointDate;
    @TableField("point_place")
    private String pointPlace;
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

    public Date getPointDate() {
        return pointDate;
    }

    public void setPointDate(Date pointDate) {
        this.pointDate = pointDate;
    }

    public String getPointPlace() {
        return pointPlace;
    }

    public void setPointPlace(String pointPlace) {
        this.pointPlace = pointPlace;
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
        return "DoctorPoint{" +
        ", id=" + id +
        ", patientIdcard=" + patientIdcard +
        ", patientName=" + patientName +
        ", doctorName=" + doctorName +
        ", doctorId=" + doctorId +
        ", pointDate=" + pointDate +
        ", pointPlace=" + pointPlace +
        ", status=" + status +
        "}";
    }
}
