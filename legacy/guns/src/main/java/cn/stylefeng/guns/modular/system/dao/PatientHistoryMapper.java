package cn.stylefeng.guns.modular.system.dao;

import cn.stylefeng.guns.modular.system.model.PatientHistory;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author zxx
 * @since 2018-12-29
 */
public interface PatientHistoryMapper extends BaseMapper<PatientHistory> {

    List<PatientHistory> selectByPatientIdcard(@Param("patientIdcard") String patientIdcard);

    List<PatientHistory> selectByDateRange(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
}
