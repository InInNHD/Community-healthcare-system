package cn.stylefeng.guns.modular.public_health.controller;

import cn.stylefeng.guns.core.shiro.ShiroKit;
import cn.stylefeng.guns.modular.system.model.VaccinationRecord;
import cn.stylefeng.guns.modular.system.dao.VaccinationRecordMapper;
import cn.stylefeng.guns.modular.system.dao.VaccinationScheduleMapper;
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
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/vaccination")
public class VaccinationController extends BaseController {
    private String PREFIX = "/public_health/vaccination/";

    @Autowired private VaccinationRecordMapper recordMapper;
    @Autowired private VaccinationScheduleMapper scheduleMapper;

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

    @RequestMapping("") public String index() { return PREFIX + "vaccination.html"; }
    @RequestMapping("/add") public String add() { return PREFIX + "vaccination_add.html"; }
    @RequestMapping("/edit/{id}") public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("item", recordMapper.selectById(id)); return PREFIX + "vaccination_edit.html";
    }

    @RequestMapping("/list") @ResponseBody
    public Object list(String patientName) {
        EntityWrapper<VaccinationRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (patientName != null && !patientName.isEmpty()) w.like("patient_name", patientName);
        w.orderBy("vacc_date", false);
        return recordMapper.selectList(w);
    }

    @RequestMapping("/schedule") @ResponseBody
    public Object schedule() { return scheduleMapper.selectList(null); }

    @RequestMapping("/doAdd") @ResponseBody
    public Object doAdd(VaccinationRecord r) {
        r.setCreateTime(new Date()); if (r.getStatus() == null) r.setStatus(1);
        recordMapper.insert(r); return SUCCESS_TIP;
    }

    @RequestMapping("/doUpdate") @ResponseBody
    public Object doUpdate(VaccinationRecord r) {
        if (r.getId() == null) return ResponseData.error("缺少ID");
        recordMapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/delete") @ResponseBody
    public Object delete(@RequestParam Integer id) {
        VaccinationRecord r = recordMapper.selectById(id);
        r.setIsDeleted(1); recordMapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/reminders") @ResponseBody
    public Object reminders() {
        EntityWrapper<VaccinationRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0).eq("status", 0);
        w.orderBy("next_date", true);
        return recordMapper.selectList(w);
    }
}
