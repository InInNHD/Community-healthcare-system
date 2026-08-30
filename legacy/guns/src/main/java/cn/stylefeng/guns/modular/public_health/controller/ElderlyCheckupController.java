package cn.stylefeng.guns.modular.public_health.controller;

import cn.stylefeng.guns.modular.system.model.ElderlyCheckup;
import cn.stylefeng.guns.modular.system.dao.ElderlyCheckupMapper;
import cn.stylefeng.roses.core.base.controller.BaseController;
import cn.stylefeng.roses.core.reqres.response.ResponseData;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/elderly")
public class ElderlyCheckupController extends BaseController {
    private String PREFIX = "/public_health/elderly/";

    @Autowired private ElderlyCheckupMapper mapper;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); return; }
                try { setValue(new SimpleDateFormat("yyyy-MM-dd").parse(text)); } catch (Exception e) { setValue(null); }
            }
        });
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); }
                else { setValue(new BigDecimal(text)); }
            }
        });
        binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.trim().isEmpty()) { setValue(null); }
                else { setValue(Integer.valueOf(text)); }
            }
        });
    }

    @RequestMapping("") public String index() { return PREFIX + "elderly.html"; }
    @RequestMapping("/add") public String add() { return PREFIX + "elderly_add.html"; }
    @RequestMapping("/edit/{id}") public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("item", mapper.selectById(id)); return PREFIX + "elderly_edit.html";
    }

    @RequestMapping("/list") @ResponseBody
    public Object list(String patientName, Integer year) {
        EntityWrapper<ElderlyCheckup> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (patientName != null && !patientName.isEmpty()) w.like("patient_name", patientName);
        w.orderBy("checkup_date", false);
        return mapper.selectList(w);
    }

    @RequestMapping("/doAdd") @ResponseBody
    public Object doAdd(ElderlyCheckup r) {
        if (r.getHeight() != null && r.getWeight() != null && r.getWeight().doubleValue() > 0) {
            double h = r.getHeight().doubleValue() / 100;
            r.setBmi(new java.math.BigDecimal(r.getWeight().doubleValue() / (h * h)).setScale(1, java.math.RoundingMode.HALF_UP));
        }
        r.setCreateTime(new Date()); mapper.insert(r); return SUCCESS_TIP;
    }

    @RequestMapping("/doUpdate") @ResponseBody
    public Object doUpdate(ElderlyCheckup r) {
        if (r.getId() == null) return ResponseData.error("缺少ID");
        if (r.getHeight() != null && r.getWeight() != null && r.getWeight().doubleValue() > 0) {
            double h = r.getHeight().doubleValue() / 100;
            r.setBmi(new java.math.BigDecimal(r.getWeight().doubleValue() / (h * h)).setScale(1, java.math.RoundingMode.HALF_UP));
        }
        mapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/delete") @ResponseBody
    public Object delete(@RequestParam Integer id) {
        ElderlyCheckup r = mapper.selectById(id);
        r.setIsDeleted(1); mapper.updateById(r); return SUCCESS_TIP;
    }

    // ===== 统计 =====
    @RequestMapping("/stats") @ResponseBody
    public Object stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalCheckups", mapper.selectCount(new EntityWrapper<ElderlyCheckup>().eq("is_deleted", 0)));
        // 按年度统计
        List<ElderlyCheckup> all = mapper.selectList(new EntityWrapper<ElderlyCheckup>().eq("is_deleted", 0));
        Map<String, Long> yearCount = new HashMap<>();
        java.text.SimpleDateFormat yf = new java.text.SimpleDateFormat("yyyy");
        for (ElderlyCheckup c : all) {
            if (c.getCheckupDate() != null) {
                String y = yf.format(c.getCheckupDate());
                yearCount.put(y, yearCount.getOrDefault(y, 0L) + 1);
            }
        }
        s.put("yearCount", yearCount);
        return s;
    }

    @RequestMapping("/dueReminders") @ResponseBody
    public Object dueReminders() {
        // 查找65岁以上、距上次体检超过1年的居民体检记录
        EntityWrapper<ElderlyCheckup> w = new EntityWrapper<>();
        w.eq("is_deleted", 0).ge("age", 65);
        w.orderBy("checkup_date", false);
        List<ElderlyCheckup> all = mapper.selectList(w);
        // 按患者去重，保留最新一条
        Map<String, ElderlyCheckup> latest = new LinkedHashMap<>();
        for (ElderlyCheckup c : all) {
            latest.putIfAbsent(c.getPatientIdcard(), c);
        }
        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);
        List<ElderlyCheckup> dueList = new ArrayList<>();
        for (ElderlyCheckup c : latest.values()) {
            if (c.getCheckupDate() == null || c.getCheckupDate().before(oneYearAgo.getTime())) {
                dueList.add(c);
            }
        }
        return dueList;
    }
}
