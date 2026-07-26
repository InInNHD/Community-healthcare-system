package cn.stylefeng.guns.modular.public_health.controller;

import cn.stylefeng.guns.modular.system.model.MaternalRecord;
import cn.stylefeng.guns.modular.system.model.MaternalPostpartumVisit;
import cn.stylefeng.guns.modular.system.model.ChildCheckup;
import cn.stylefeng.guns.modular.system.dao.MaternalRecordMapper;
import cn.stylefeng.guns.modular.system.dao.MaternalPostpartumVisitMapper;
import cn.stylefeng.guns.modular.system.dao.ChildCheckupMapper;
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
@RequestMapping("/maternal")
public class MaternalHealthController extends BaseController {
    private String PREFIX = "/public_health/maternal/";

    @Autowired private MaternalRecordMapper maternalMapper;
    @Autowired private MaternalPostpartumVisitMapper visitMapper;
    @Autowired private ChildCheckupMapper childMapper;

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

    @RequestMapping("") public String index() { return PREFIX + "maternal.html"; }
    @RequestMapping("/add") public String add() { return PREFIX + "maternal_add.html"; }
    @RequestMapping("/edit/{id}") public String edit(@PathVariable Integer id, Model model) {
        model.addAttribute("item", maternalMapper.selectById(id)); return PREFIX + "maternal_edit.html";
    }

    // ===== 建册 CRUD =====
    @RequestMapping("/list") @ResponseBody
    public Object list(String patientName) {
        EntityWrapper<MaternalRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (patientName != null && !patientName.isEmpty()) w.like("patient_name", patientName);
        w.orderBy("create_time", false);
        return maternalMapper.selectList(w);
    }

    @RequestMapping("/doAdd") @ResponseBody
    public Object doAdd(MaternalRecord r) {
        r.setCreateTime(new Date()); r.setStatus(1); maternalMapper.insert(r); return SUCCESS_TIP;
    }

    @RequestMapping("/doUpdate") @ResponseBody
    public Object doUpdate(MaternalRecord r) {
        if (r.getId() == null) return ResponseData.error("缺少ID");
        maternalMapper.updateById(r); return SUCCESS_TIP;
    }

    @RequestMapping("/delete") @ResponseBody
    public Object delete(@RequestParam Integer id) {
        MaternalRecord r = maternalMapper.selectById(id);
        r.setIsDeleted(1); maternalMapper.updateById(r); return SUCCESS_TIP;
    }

    // ===== 产后访视 CRUD =====
    @RequestMapping("/visit/list") @ResponseBody
    public Object visitList(Integer maternalId) {
        EntityWrapper<MaternalPostpartumVisit> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (maternalId != null) w.eq("maternal_id", maternalId);
        w.orderBy("visit_date", false);
        return visitMapper.selectList(w);
    }

    @RequestMapping("/visit/add") @ResponseBody
    public Object visitAdd(MaternalPostpartumVisit v) {
        v.setCreateTime(new Date()); v.setStatus(1); visitMapper.insert(v); return SUCCESS_TIP;
    }

    @RequestMapping("/visit/delete") @ResponseBody
    public Object visitDelete(@RequestParam Integer id) {
        MaternalPostpartumVisit v = visitMapper.selectById(id);
        v.setIsDeleted(1); visitMapper.updateById(v); return SUCCESS_TIP;
    }

    // ===== 高风险管理 =====
    @RequestMapping("/highRisk") @ResponseBody
    public Object highRiskList() {
        EntityWrapper<MaternalRecord> w = new EntityWrapper<>();
        w.eq("is_deleted", 0).eq("high_risk_flag", 1).eq("status", 1);
        return maternalMapper.selectList(w);
    }

    // ===== 儿童体检 =====
    @RequestMapping("/child/list") @ResponseBody
    public Object childList(String patientName) {
        EntityWrapper<ChildCheckup> w = new EntityWrapper<>();
        w.eq("is_deleted", 0);
        if (patientName != null && !patientName.isEmpty()) w.like("patient_name", patientName);
        w.orderBy("checkup_date", false);
        return childMapper.selectList(w);
    }

    @RequestMapping("/child/add") @ResponseBody
    public Object childAdd(ChildCheckup c) {
        c.setCreateTime(new Date()); childMapper.insert(c); return SUCCESS_TIP;
    }

    @RequestMapping("/child/delete") @ResponseBody
    public Object childDelete(@RequestParam Integer id) {
        ChildCheckup c = childMapper.selectById(id);
        c.setIsDeleted(1); childMapper.updateById(c); return SUCCESS_TIP;
    }

    // ===== 统计 =====
    @RequestMapping("/stats") @ResponseBody
    public Object stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("totalMaternal", maternalMapper.selectCount(new EntityWrapper<MaternalRecord>().eq("is_deleted",0).eq("status",1)));
        s.put("highRisk", maternalMapper.selectCount(new EntityWrapper<MaternalRecord>().eq("is_deleted",0).eq("high_risk_flag",1).eq("status",1)));
        s.put("postpartumVisits", visitMapper.selectCount(new EntityWrapper<MaternalPostpartumVisit>().eq("is_deleted",0)));
        s.put("childCheckups", childMapper.selectCount(new EntityWrapper<ChildCheckup>().eq("is_deleted",0)));
        return s;
    }
}
