package cn.stylefeng.guns.modular.chronic_disease.service.impl;

import cn.stylefeng.guns.modular.system.model.ChronicFollowupPlan;
import cn.stylefeng.guns.modular.system.dao.ChronicFollowupPlanMapper;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupPlanService;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 慢病随访计划 Service 实现类
 */
@Service
public class ChronicFollowupPlanServiceImpl extends ServiceImpl<ChronicFollowupPlanMapper, ChronicFollowupPlan>
        implements IChronicFollowupPlanService {
}
