package cn.stylefeng.guns.modular.chronic_disease.service.impl;

import cn.stylefeng.guns.modular.system.model.ChronicDisease;
import cn.stylefeng.guns.modular.system.model.ChronicFollowupPlan;
import cn.stylefeng.guns.modular.system.dao.ChronicDiseaseMapper;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicDiseaseService;
import cn.stylefeng.guns.modular.chronic_disease.service.IChronicFollowupPlanService;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 慢病档案 Service 实现类
 * 参考南京"超能家医"模式
 */
@Service
public class ChronicDiseaseServiceImpl extends ServiceImpl<ChronicDiseaseMapper, ChronicDisease>
        implements IChronicDiseaseService {

    @Autowired
    private IChronicFollowupPlanService chronicFollowupPlanService;

    private static final String[] DISEASE_TYPES = {"高血压", "糖尿病", "冠心病", "脑卒中", "慢阻肺", "慢性肾病"};
    private static final String[] RISK_LEVELS = {"低风险", "中风险", "高风险"};

    @Override
    public Map<String, Object> getStatsByIds(List<Integer> doctorIds) {
        Map<String, Object> stats = new HashMap<>();

        EntityWrapper<ChronicDisease> allWrapper = new EntityWrapper<>();
        if (doctorIds != null && !doctorIds.isEmpty()) {
            allWrapper.in("doctor_id", doctorIds);
        }
        allWrapper.eq("status", 1);
        stats.put("totalCount", this.selectCount(allWrapper));

        Map<String, Integer> diseaseCount = new HashMap<>();
        for (String type : DISEASE_TYPES) {
            EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
            if (doctorIds != null && !doctorIds.isEmpty()) {
                wrapper.in("doctor_id", doctorIds);
            }
            wrapper.eq("disease_type", type);
            wrapper.eq("status", 1);
            diseaseCount.put(type, this.selectCount(wrapper));
        }
        stats.put("diseaseCount", diseaseCount);

        Map<String, Integer> riskCount = new HashMap<>();
        for (String level : RISK_LEVELS) {
            EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
            if (doctorIds != null && !doctorIds.isEmpty()) {
                wrapper.in("doctor_id", doctorIds);
            }
            wrapper.eq("risk_level", level);
            wrapper.eq("status", 1);
            riskCount.put(level, this.selectCount(wrapper));
        }
        stats.put("riskCount", riskCount);

        EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
        if (doctorIds != null && !doctorIds.isEmpty()) {
            planWrapper.in("doctor_id", doctorIds);
        }
        planWrapper.eq("status", 0);
        stats.put("pendingFollowupCount", chronicFollowupPlanService.selectCount(planWrapper));

        return stats;
    }

    @Override
    public Map<String, Object> getStats(List<String> doctorNames) {
        Map<String, Object> stats = new HashMap<>();

        EntityWrapper<ChronicDisease> allWrapper = new EntityWrapper<>();
        if (doctorNames != null && !doctorNames.isEmpty()) {
            allWrapper.in("doctor_name", doctorNames);
        }
        allWrapper.eq("status", 1);
        stats.put("totalCount", this.selectCount(allWrapper));

        Map<String, Integer> diseaseCount = new HashMap<>();
        for (String type : DISEASE_TYPES) {
            EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
            if (doctorNames != null && !doctorNames.isEmpty()) {
                wrapper.in("doctor_name", doctorNames);
            }
            wrapper.eq("disease_type", type);
            wrapper.eq("status", 1);
            diseaseCount.put(type, this.selectCount(wrapper));
        }
        stats.put("diseaseCount", diseaseCount);

        Map<String, Integer> riskCount = new HashMap<>();
        for (String level : RISK_LEVELS) {
            EntityWrapper<ChronicDisease> wrapper = new EntityWrapper<>();
            if (doctorNames != null && !doctorNames.isEmpty()) {
                wrapper.in("doctor_name", doctorNames);
            }
            wrapper.eq("risk_level", level);
            wrapper.eq("status", 1);
            riskCount.put(level, this.selectCount(wrapper));
        }
        stats.put("riskCount", riskCount);

        EntityWrapper<ChronicFollowupPlan> planWrapper = new EntityWrapper<>();
        if (doctorNames != null && !doctorNames.isEmpty()) {
            planWrapper.in("doctor_name", doctorNames);
        }
        planWrapper.eq("status", 0);
        stats.put("pendingFollowupCount", chronicFollowupPlanService.selectCount(planWrapper));

        return stats;
    }

    @Override
    public String assessRiskLevel(String diseaseType, Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return "低风险";
        }
        try {
            switch (diseaseType) {
                case "高血压":
                    return assessHypertensionRisk(data);
                case "糖尿病":
                    return assessDiabetesRisk(data);
                case "冠心病":
                    return assessCHDRisk(data);
                case "脑卒中":
                    return assessStrokeRisk(data);
                case "慢阻肺":
                    return assessCOPDRisk(data);
                case "慢性肾病":
                    return assessCKDRisk(data);
                default:
                    return "低风险";
            }
        } catch (Exception e) {
            return "低风险";
        }
    }

    private String assessHypertensionRisk(Map<String, Object> data) {
        Object sbpObj = data.get("systolic"); // 收缩压
        Object dbpObj = data.get("diastolic"); // 舒张压
        int sbp = sbpObj != null ? toInt(sbpObj) : 0;
        int dbp = dbpObj != null ? toInt(dbpObj) : 0;
        if (sbp >= 180 || dbp >= 110) return "高风险";
        if (sbp >= 160 || dbp >= 100) return "中风险";
        return "低风险";
    }

    private String assessDiabetesRisk(Map<String, Object> data) {
        Object bgObj = data.get("bloodSugar"); // 空腹血糖 mmol/L
        Object hba1cObj = data.get("hba1c"); // 糖化血红蛋白 %
        double bg = bgObj != null ? toDouble(bgObj) : 0;
        double hba1c = hba1cObj != null ? toDouble(hba1cObj) : 0;
        if (bg >= 11.1 || hba1c >= 9.0) return "高风险";
        if (bg >= 7.0 || hba1c >= 7.5) return "中风险";
        return "低风险";
    }

    private String assessCHDRisk(Map<String, Object> data) {
        Object acsObj = data.get("acsHistory"); // 急性冠脉综合征史
        Object nYhaObj = data.get("nyha"); // NYHA心功能分级
        boolean acsHistory = acsObj != null && ("true".equals(acsObj.toString()) || "1".equals(acsObj.toString()));
        int nyha = nYhaObj != null ? toInt(nYhaObj) : 0;
        if (acsHistory || nyha >= 3) return "高风险";
        if (nyha == 2) return "中风险";
        return "低风险";
    }

    private String assessStrokeRisk(Map<String, Object> data) {
        Object nihssObj = data.get("nihss"); // NIHSS评分
        int nihss = nihssObj != null ? toInt(nihssObj) : 0;
        if (nihss >= 15) return "高风险";
        if (nihss >= 5) return "中风险";
        return "低风险";
    }

    private String assessCOPDRisk(Map<String, Object> data) {
        Object fev1Obj = data.get("fev1"); // FEV1%预计值
        double fev1 = fev1Obj != null ? toDouble(fev1Obj) : 100;
        if (fev1 < 50) return "高风险";
        if (fev1 < 80) return "中风险";
        return "低风险";
    }

    private String assessCKDRisk(Map<String, Object> data) {
        Object egfrObj = data.get("egfr"); // eGFR ml/min/1.73m²
        Object proteinObj = data.get("proteinuria"); // 蛋白尿 g/24h
        double egfr = egfrObj != null ? toDouble(egfrObj) : 90;
        double proteinuria = proteinObj != null ? toDouble(proteinObj) : 0;
        if (egfr < 30 || proteinuria >= 3.5) return "高风险";
        if (egfr < 60) return "中风险";
        return "低风险";
    }

    @Override
    public int getFollowupIntervalDays(String riskLevel) {
        if ("高风险".equals(riskLevel)) return 14;
        if ("中风险".equals(riskLevel)) return 30;
        return 90;
    }

    @Override
    public Date calculateNextFollowupDate(String riskLevel) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, getFollowupIntervalDays(riskLevel));
        return cal.getTime();
    }

    @Override
    public Map<String, String> getFollowupTemplate(String diseaseType) {
        Map<String, String> template = new HashMap<>();
        switch (diseaseType) {
            case "高血压":
                template.put("symptoms", "头晕/头痛/心悸/视力模糊");
                template.put("bloodPressure", "必测");
                template.put("lifestyleAdvice", "低盐饮食（<6g/日），规律运动，戒烟限酒，保持情绪稳定");
                template.put("medicationReminder", "请按时服用降压药，不可随意停药");
                break;
            case "糖尿病":
                template.put("symptoms", "多饮/多尿/多食/体重减轻/视力模糊/四肢麻木");
                template.put("bloodSugar", "必测");
                template.put("lifestyleAdvice", "控制碳水摄入，定时定量进餐，适当餐后运动，注意足部护理");
                template.put("medicationReminder", "请按时服用降糖药或注射胰岛素，定期监测血糖");
                break;
            case "冠心病":
                template.put("symptoms", "胸闷/胸痛/心悸/气短/乏力");
                template.put("heartRate", "必测");
                template.put("lifestyleAdvice", "避免剧烈运动和情绪激动，低脂饮食，控制体重，戒烟");
                template.put("medicationReminder", "请按时服用抗血小板药物和他汀类药物");
                break;
            case "脑卒中":
                template.put("symptoms", "肢体无力/言语障碍/面部歪斜/行走不稳/头晕");
                template.put("nihss", "建议评估NIHSS");
                template.put("lifestyleAdvice", "康复训练，防止跌倒，低盐低脂饮食，监测血压");
                template.put("medicationReminder", "请按时服用抗血小板/抗凝药物，控制血压血脂");
                break;
            case "慢阻肺":
                template.put("symptoms", "咳嗽/咳痰/呼吸困难/喘息/胸闷");
                template.put("fev1", "建议检测肺功能FEV1");
                template.put("lifestyleAdvice", "戒烟，避免粉尘和有害气体，呼吸功能锻炼，接种流感疫苗");
                template.put("medicationReminder", "请正确使用吸入剂，按时用药，勿随意增减剂量");
                break;
            case "慢性肾病":
                template.put("symptoms", "水肿/乏力/食欲减退/尿量改变/皮肤瘙痒");
                template.put("egfr", "建议检测eGFR和尿蛋白");
                template.put("lifestyleAdvice", "优质低蛋白饮食，控制盐和钾摄入，适量饮水，避免肾毒性药物");
                template.put("medicationReminder", "请按时服药，定期复查肾功能和尿常规");
                break;
            default:
                template.put("symptoms", "");
                template.put("lifestyleAdvice", "合理饮食，适量运动，定期复诊");
                template.put("medicationReminder", "请按医嘱服药");
        }
        return template;
    }

    private int toInt(Object obj) {
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private double toDouble(Object obj) {
        try {
            return Double.parseDouble(obj.toString());
        } catch (Exception e) {
            return 0;
        }
    }
}
