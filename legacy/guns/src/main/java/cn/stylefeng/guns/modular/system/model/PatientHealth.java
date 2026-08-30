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
@TableName("patient_health")
public class PatientHealth extends Model<PatientHealth> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("heart_jump")
    private Integer heartJump;
    @TableField("blood_pressure")
    private Integer bloodPressure;
    @TableField("blood_ox")
    private Integer bloodOx;
    private Integer pulse;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private Date date;
    @TableField("patient_idcard")
    private String patientIdcard;
    @TableField("patient_name")
    private String patientName;
    @TableField("is_deleted")
    private Integer isDeleted;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getHeartJump() {
        return heartJump;
    }

    public void setHeartJump(Integer heartJump) {
        this.heartJump = heartJump;
    }

    public Integer getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(Integer bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public Integer getBloodOx() {
        return bloodOx;
    }

    public void setBloodOx(Integer bloodOx) {
        this.bloodOx = bloodOx;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse) {
        this.pulse = pulse;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
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

    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "PatientHealth{" +
        ", id=" + id +
        ", heartJump=" + heartJump +
        ", bloodPressure=" + bloodPressure +
        ", bloodOx=" + bloodOx +
        ", pulse=" + pulse +
        ", date=" + date +
        ", patientIdcard=" + patientIdcard +
        ", patientName=" + patientName +
        "}";
    }
}
