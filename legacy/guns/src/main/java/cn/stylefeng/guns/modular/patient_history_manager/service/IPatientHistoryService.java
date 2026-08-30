package cn.stylefeng.guns.modular.patient_history_manager.service;

import cn.stylefeng.guns.modular.system.model.PatientHistory;
import com.baomidou.mybatisplus.service.IService;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author zxx
 * @since 2018-12-29
 */
public interface IPatientHistoryService extends IService<PatientHistory> {

    List<PatientHistory> selectByPatientIdcard(String patientIdcard);

    List<PatientHistory> selectByDateRange(Date startDate, Date endDate);
}
