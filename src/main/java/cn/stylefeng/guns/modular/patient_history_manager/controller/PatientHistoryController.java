package cn.stylefeng.guns.modular.patient_history_manager.controller;

import cn.stylefeng.roses.core.base.controller.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Autowired;
import cn.stylefeng.guns.core.log.LogObjectHolder;
import org.springframework.web.bind.annotation.RequestParam;
import cn.stylefeng.guns.modular.system.model.PatientHistory;
import cn.stylefeng.guns.modular.patient_history_manager.service.IPatientHistoryService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 居民就诊历史管理控制器
 *
 * @author
 * @Date
 */
@Controller
@RequestMapping("/patientHistory")
public class PatientHistoryController extends BaseController {

    private String PREFIX = "/patient_history_manager/patientHistory/";

    @Autowired
    private IPatientHistoryService patientHistoryService;

    /**
     * 跳转到居民就诊历史管理首页
     */
    @RequestMapping("")
    public String index() {
        return PREFIX + "patientHistory.html";
    }

    /**
     * 跳转到添加居民就诊历史管理
     */
    @RequestMapping("/patientHistory_add")
    public String patientHistoryAdd() {
        return PREFIX + "patientHistory_add.html";
    }

    /**
     * 跳转到修改居民就诊历史管理
     */
    @RequestMapping("/patientHistory_update/{patientHistoryId}")
    public String patientHistoryUpdate(@PathVariable Integer patientHistoryId, Model model) {
        PatientHistory patientHistory = patientHistoryService.selectById(patientHistoryId);
        model.addAttribute("item",patientHistory);
        LogObjectHolder.me().set(patientHistory);
        return PREFIX + "patientHistory_edit.html";
    }

    /**
     * 获取居民就诊历史管理列表
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String condition) {
        return patientHistoryService.selectList(null);
    }

    /**
     * 新增居民就诊历史管理
     */
    @RequestMapping(value = "/add")
    @ResponseBody
    public Object add(PatientHistory patientHistory) {
        patientHistoryService.insert(patientHistory);
        return SUCCESS_TIP;
    }

    /**
     * 删除居民就诊历史管理
     */
    @RequestMapping(value = "/delete")
    @ResponseBody
    public Object delete(@RequestParam Integer patientHistoryId) {
        PatientHistory entity = patientHistoryService.selectById(patientHistoryId);
        entity.setIsDeleted(1);
        patientHistoryService.updateById(entity);
        return SUCCESS_TIP;
    }

    /**
     * 修改居民就诊历史管理
     */
    @RequestMapping(value = "/update")
    @ResponseBody
    public Object update(PatientHistory patientHistory) {
        patientHistoryService.updateById(patientHistory);
        return SUCCESS_TIP;
    }

    /**
     * 居民就诊历史管理详情
     */
    @RequestMapping(value = "/detail/{patientHistoryId}")
    @ResponseBody
    public Object detail(@PathVariable("patientHistoryId") Integer patientHistoryId) {
        return patientHistoryService.selectById(patientHistoryId);
    }

    /**
     * 导出单条就诊记录为Excel
     */
    @RequestMapping(value = "/export/{patientHistoryId}")
    public void exportOne(@PathVariable Integer patientHistoryId, HttpServletResponse response) throws Exception {
        PatientHistory record = patientHistoryService.selectById(patientHistoryId);
        if (record == null) {
            response.setContentType("text/html;charset=utf-8");
            response.getWriter().write("<script>alert('记录不存在');window.close();</script>");
            return;
        }
        Workbook workbook = createWorkbook(java.util.Arrays.asList(record));
        writeExcelResponse(response, workbook, "就诊记录_" + record.getId() + ".xlsx");
    }

    /**
     * 按患者身份证号批量导出就诊记录为Excel
     */
    @RequestMapping(value = "/exportByPatientIdcard")
    public void exportByPatientIdcard(@RequestParam String patientIdcard, HttpServletResponse response) throws Exception {
        List<PatientHistory> list = patientHistoryService.selectByPatientIdcard(patientIdcard);
        Workbook workbook = createWorkbook(list);
        writeExcelResponse(response, workbook, "就诊记录_" + patientIdcard + ".xlsx");
    }

    /**
     * 按日期范围批量导出就诊记录为Excel（管理员端）
     */
    @RequestMapping(value = "/exportByDateRange")
    public void exportByDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            HttpServletResponse response) throws Exception {
        // 结束日期加一天以包含当天
        long endMillis = endDate.getTime() + 24L * 60 * 60 * 1000 - 1;
        Date adjustedEnd = new Date(endMillis);
        List<PatientHistory> list = patientHistoryService.selectByDateRange(startDate, adjustedEnd);
        Workbook workbook = createWorkbook(list);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String filename = "就诊记录_" + sdf.format(startDate) + "_" + sdf.format(endDate) + ".xlsx";
        writeExcelResponse(response, workbook, filename);
    }

    /**
     * 跳转到打印页面
     */
    @RequestMapping("/print/{patientHistoryId}")
    public String print(@PathVariable Integer patientHistoryId, Model model) {
        PatientHistory record = patientHistoryService.selectById(patientHistoryId);
        model.addAttribute("item", record);
        return PREFIX + "patientHistory_print.html";
    }

    private Workbook createWorkbook(List<PatientHistory> list) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("就诊记录");
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        Row header = sheet.createRow(0);
        String[] headers = {"编号", "身份证号", "姓名", "症状", "主治医生", "用药", "就诊日期", "费用(元)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        int rowIdx = 1;
        for (PatientHistory h : list) {
            Row row = sheet.createRow(rowIdx++);
            createCell(row, 0, h.getId() != null ? String.valueOf(h.getId()) : "", dataStyle);
            createCell(row, 1, h.getPatientIdcard() != null ? h.getPatientIdcard() : "", dataStyle);
            createCell(row, 2, h.getPatientName() != null ? h.getPatientName() : "", dataStyle);
            createCell(row, 3, h.getPatientSym() != null ? h.getPatientSym() : "", dataStyle);
            createCell(row, 4, h.getPatientDoctor() != null ? h.getPatientDoctor() : "", dataStyle);
            createCell(row, 5, h.getPatientMedicine() != null ? h.getPatientMedicine() : "", dataStyle);
            createCell(row, 6, h.getPatientHistoryDate() != null ? sdf.format(h.getPatientHistoryDate()) : "", dataStyle);
            createCell(row, 7, h.getTakeprice() != null ? String.valueOf(h.getTakeprice()) : "", dataStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return workbook;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeExcelResponse(HttpServletResponse response, Workbook workbook, String filename) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
        OutputStream os = response.getOutputStream();
        workbook.write(os);
        os.flush();
        os.close();
        workbook.close();
    }
}
