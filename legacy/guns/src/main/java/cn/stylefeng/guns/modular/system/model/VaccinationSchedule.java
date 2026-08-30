package cn.stylefeng.guns.modular.system.model;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;
import java.io.Serializable;

@TableName("vaccination_schedule")
public class VaccinationSchedule extends Model<VaccinationSchedule> {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO) private Integer id;
    @TableField("vaccine_name") private String vaccineName;
    @TableField("target_age") private String targetAge;
    @TableField("dose_seq") private Integer doseSeq;
    private String description;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getVaccineName() { return vaccineName; }
    public void setVaccineName(String vaccineName) { this.vaccineName = vaccineName; }
    public String getTargetAge() { return targetAge; }
    public void setTargetAge(String targetAge) { this.targetAge = targetAge; }
    public Integer getDoseSeq() { return doseSeq; }
    public void setDoseSeq(Integer doseSeq) { this.doseSeq = doseSeq; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    @Override protected Serializable pkVal() { return this.id; }
}
