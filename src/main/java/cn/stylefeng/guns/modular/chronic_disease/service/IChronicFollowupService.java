package cn.stylefeng.guns.modular.chronic_disease.service;

import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.model.ChronicFollowup;
import com.baomidou.mybatisplus.service.IService;

import java.util.Map;

/**
 * 慢病随访记录 Service 接口
 */
public interface IChronicFollowupService extends IService<ChronicFollowup> {

    /**
     * 执行完整随访流程：保存随访记录 → 更新档案风险等级 → 标记计划已完成 → 生成下次计划
     * @param followup 随访记录
     * @param clinicalData 临床指标数据（用于自动风险评级）
     * @return 操作结果
     */
    Map<String, Object> executeFollowup(ChronicFollowup followup, Map<String, Object> clinicalData);

    /**
     * 根据慢病档案ID和临床数据，自动评估并更新风险等级
     */
    String reassessRisk(Integer chronicId, Map<String, Object> clinicalData);

    /**
     * 根据慢性病档案信息自动生成随访计划
     */
    void generateFollowupPlan(ChronicDisease chronic);
}
