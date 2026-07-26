package cn.stylefeng.guns.modular.chronic_disease.service;

import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import com.baomidou.mybatisplus.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 慢病档案 Service 接口
 * 参考南京"超能家医"模式：支持六大慢病管理、风险分级、自动化随访
 */
public interface IChronicDiseaseService extends IService<ChronicDisease> {

    /**
     * 根据风险等级和病种统计患者数量（按医生ID过滤）
     */
    Map<String, Object> getStatsByIds(List<Integer> doctorIds);

    /**
     * 根据风险等级和病种统计患者数量（按医生姓名过滤，兼容旧接口）
     */
    Map<String, Object> getStats(List<String> doctorNames);

    /**
     * 根据临床指标评估风险等级
     * 高血压：收缩压≥180或舒张压≥110 → 高风险；收缩压≥160或舒张压≥100 → 中风险；其余低风险
     * 糖尿病：空腹血糖≥11.1或HbA1c≥9.0 → 高风险；空腹血糖≥7.0或HbA1c≥7.5 → 中风险；其余低风险
     * 冠心病：有ACS史或心功能III-IV级 → 高风险；支架术后>1年 → 中风险；其余低风险
     * 脑卒中：NIHSS≥15 → 高风险；NIHSS 5-14 → 中风险；NIHSS<5 → 低风险
     * 慢阻肺：FEV1<50% → 高风险；FEV1 50-80% → 中风险；FEV1>80% → 低风险
     * 慢性肾病：eGFR<30或蛋白尿≥3.5g/24h → 高风险；eGFR 30-60 → 中风险；eGFR>60 → 低风险
     */
    String assessRiskLevel(String diseaseType, Map<String, Object> clinicalData);

    /**
     * 获取每个随访周期需要的天数（高风险14天、中风险30天、低风险90天）
     */
    int getFollowupIntervalDays(String riskLevel);

    /**
     * 生成下一个随访日期
     */
    java.util.Date calculateNextFollowupDate(String riskLevel);

    /**
     * 获取指定病种的随访建议内容模板
     */
    Map<String, String> getFollowupTemplate(String diseaseType);
}
