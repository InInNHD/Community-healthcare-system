package cn.stylefeng.guns.modular.public_health.controller;

import cn.stylefeng.guns.modular.system.model.InfectiousDiseaseReport;
import cn.stylefeng.guns.modular.system.dao.InfectiousDiseaseReportMapper;
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
@RequestMapping("/infectious")
public class InfectiousDiseaseController extends BaseController {
    private String PREFIX = "/public_health/infectious/";

    @Autowired private InfectiousDiseaseReportMapper mapper;

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

    @RequestMapping("") public String index() { return PREFIX + "infectious.html"; }
    @RequestMapping("/add") public String add() { return PREFIX + "infectious_add.html"; }
    @RequestMapping("/edit/{id}") public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("item", mapper.selectById(id)); return PREFIX + "infectious_edit.html";
    }

    @RequestMapping("/list") @ResponseBody
    public Object list(String diseaseType, String diseaseCategory) {
        EntityWrapper<InfectiousDiseaseReport> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (diseaseType != null && !diseaseType.isEmpty()) w.like("disease_type", diseaseType);
        if (diseaseCategory != null && !diseaseCategory.isEmpty()) w.eq("disease_category", diseaseCategory);
        w.orderBy("report_date", false);
        return mapper.selectList(w);
    }

    @RequestMapping("/doAdd") @ResponseBody
    public Object doAdd(InfectiousDiseaseReport r) {
        r.setCreateTime(new Date()); r.setStatus(1); r.setReportDate(new Date());
        mapper.insert(r); return SUCCESS_TIP;
    }

    @RequestMapping("/doUpdate") @ResponseBody
    public Object doUpdate(InfectiousDiseaseReport r) {
        if (r.getId() == null) return ResponseData.error("缺少ID");
        mapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/delete") @ResponseBody
    public Object delete(@RequestParam Integer id) {
        InfectiousDiseaseReport r = mapper.selectById(id);
        r.setIsDeleted(1); mapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/review") @ResponseBody
    public Object review(@RequestParam Integer id) {
        InfectiousDiseaseReport r = mapper.selectById(id);
        r.setStatus(2); mapper.updateById(r); return SUCCESS_TIP;
    }

    // ===== 统计 =====
    @RequestMapping("/stats") @ResponseBody
    public Object stats() {
        Map<String, Object> s = new HashMap<>();
        EntityWrapper<InfectiousDiseaseReport> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        s.put("total", mapper.selectCount(w));
        s.put("categoryA", mapper.selectCount(new EntityWrapper<InfectiousDiseaseReport>().eq("is_deleted",0).eq("disease_category","甲")));
        s.put("categoryB", mapper.selectCount(new EntityWrapper<InfectiousDiseaseReport>().eq("is_deleted",0).eq("disease_category","乙")));
        s.put("categoryC", mapper.selectCount(new EntityWrapper<InfectiousDiseaseReport>().eq("is_deleted",0).eq("disease_category","丙")));
        s.put("pendingReview", mapper.selectCount(new EntityWrapper<InfectiousDiseaseReport>().eq("is_deleted",0).eq("status",1)));

        // 按病种统计
        Map<String, Long> diseaseCount = new HashMap<>();
        List<InfectiousDiseaseReport> all = mapper.selectList(new EntityWrapper<InfectiousDiseaseReport>().eq("is_deleted",0));
        for (InfectiousDiseaseReport r : all) {
            diseaseCount.put(r.getDiseaseType(), diseaseCount.getOrDefault(r.getDiseaseType(), 0L) + 1);
        }
        s.put("diseaseCount", diseaseCount);
        return s;
    }
}
