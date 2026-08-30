package cn.stylefeng.guns.modular.patient_history_manager.service.impl;

import cn.stylefeng.guns.modular.system.model.PatientHistory;
import cn.stylefeng.guns.modular.system.dao.PatientHistoryMapper;
import cn.stylefeng.guns.modular.patient_history_manager.service.IPatientHistoryService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zxx
 * @since 2018-12-29
 */
@Service
public class PatientHistoryServiceImpl extends ServiceImpl<PatientHistoryMapper, PatientHistory> implements IPatientHistoryService {

    @Autowired
    private PatientHistoryMapper patientHistoryMapper;

    @Override
    public List<PatientHistory> selectByPatientIdcard(String patientIdcard) {
        return patientHistoryMapper.selectByPatientIdcard(patientIdcard);
    }

    @Override
    public List<PatientHistory> selectByDateRange(Date startDate, Date endDate) {
        return patientHistoryMapper.selectByDateRange(startDate, endDate);
    }
}
