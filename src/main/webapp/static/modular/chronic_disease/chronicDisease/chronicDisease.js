/**
 * 慢病管理模块 - 参考南京"超能家医"模式
 */
var ChronicDisease = {
    id: "ChronicDiseaseTable",
    seItem: null,
    table: null,
    layerIndex: -1
};

/**
 * 风险等级着色
 */
ChronicDisease.riskColor = function (value) {
    if (value === '高风险') return '<span class="label label-danger">高风险</span>';
    if (value === '中风险') return '<span class="label label-warning">中风险</span>';
    return '<span class="label label-success">低风险</span>';
};

ChronicDisease.statusText = function (value) {
    if (value === 1) return '管理中';
    if (value === 2) return '已转诊';
    if (value === 3) return '已结案';
    return '未知';
};

ChronicDisease.initColumn = function () {
    return [
        {field: 'selectItem', radio: true},
        {title: '患者姓名', field: 'patientName', align: 'center', valign: 'middle', sortable: true},
        {title: '身份证号', field: 'patientIdcard', align: 'center', valign: 'middle'},
        {title: '慢病类型', field: 'diseaseType', align: 'center', valign: 'middle', sortable: true},
        {title: '风险等级', field: 'riskLevel', align: 'center', valign: 'middle', sortable: true,
            formatter: function(value) { return ChronicDisease.riskColor(value); }},
        {title: '管理医生', field: 'doctorName', align: 'center', valign: 'middle'},
        {title: '状态', field: 'status', align: 'center', valign: 'middle',
            formatter: function(value) { return ChronicDisease.statusText(value); }},
        {title: '确诊日期', field: 'diagnosisDate', align: 'center', valign: 'middle'},
        {title: '备注', field: 'remark', align: 'center', valign: 'middle'}
    ];
};

ChronicDisease.check = function () {
    var selected = $('#' + this.id).bootstrapTable('getSelections');
    if (selected.length === 0) {
        Feng.info("请先选中表格中的某一记录！");
        return false;
    }
    ChronicDisease.seItem = selected[0];
    return true;
};

// ==================== 仪表盘 ====================

ChronicDisease.loadDashboard = function () {
    var ajax = new $ax(Feng.ctxPath + "/chronicDisease/stats", function (data) {
        $('#totalCount').text(data.totalCount || 0);
        $('#highRiskCount').text((data.riskCount && data.riskCount['高风险']) || 0);
        $('#pendingPlanCount').text(data.pendingFollowupCount || 0);

        // 病种分布
        var dc = data.diseaseCount || {};
        var diseaseHtml = '';
        var diseaseTypes = ['高血压', '糖尿病', '冠心病', '脑卒中', '慢阻肺', '慢性肾病'];
        for (var i = 0; i < diseaseTypes.length; i++) {
            diseaseHtml += '<tr><td>' + diseaseTypes[i] + '</td><td>' + (dc[diseaseTypes[i]] || 0) + '</td></tr>';
        }
        $('#diseaseDistBody').html(diseaseHtml);

        // 风险分布
        var rc = data.riskCount || {};
        var riskHtml = '';
        riskHtml += '<tr><td><span class="label label-danger">高风险</span></td><td>' + (rc['高风险'] || 0) + '</td></tr>';
        riskHtml += '<tr><td><span class="label label-warning">中风险</span></td><td>' + (rc['中风险'] || 0) + '</td></tr>';
        riskHtml += '<tr><td><span class="label label-success">低风险</span></td><td>' + (rc['低风险'] || 0) + '</td></tr>';
        $('#riskDistBody').html(riskHtml);
    }, function () {});
    ajax.start();
};

ChronicDisease.loadPendingReminders = function () {
    var ajax = new $ax(Feng.ctxPath + "/chronicDisease/pendingReminders", function (data) {
        var html = '';
        var todayStr = new Date().toISOString().slice(0, 10);
        var todayCount = 0;
        for (var i = 0; i < data.length; i++) {
            var item = data[i];
            var planDate = item.planDate ? item.planDate.slice(0, 10) : '';
            if (planDate === todayStr) todayCount++;
            html += '<li class="list-group-item">';
            html += '<span class="label label-' + (planDate <= todayStr ? 'danger' : 'info') + ' pull-right">' + planDate + '</span>';
            html += item.patientName + ' - ' + item.diseaseType;
            html += '</li>';
        }
        if (!html) html = '<li class="list-group-item text-muted">暂无待随访计划</li>';
        $('#pendingReminderList').html(html);
        $('#todayFollowupCount').text(todayCount);
    }, function () {});
    ajax.start();
};

// ==================== 搜索 ====================

ChronicDisease.search = function () {
    var queryData = {};
    queryData['diseaseType'] = $("#filterDiseaseType").val();
    queryData['riskLevel'] = $("#filterRiskLevel").val();
    queryData['patientName'] = $("#filterPatientName").val();
    ChronicDisease.table.refresh({query: queryData});
    ChronicDisease.loadDashboard();
    ChronicDisease.loadPendingReminders();
};

ChronicDisease.resetSearch = function () {
    $("#filterDiseaseType").val("");
    $("#filterRiskLevel").val("");
    $("#filterPatientName").val("");
    ChronicDisease.search();
};

// ==================== CRUD操作 ====================

ChronicDisease.openAdd = function () {
    var index = layer.open({
        type: 2,
        title: '新建慢病档案',
        area: ['900px', '550px'],
        fix: false,
        maxmin: true,
        content: Feng.ctxPath + '/chronicDisease/chronicDisease_add'
    });
    this.layerIndex = index;
};

ChronicDisease.addSubmit = function () {
    var ajax = new $ax(Feng.ctxPath + "/chronicDisease/add", function () {
        Feng.success("建档成功，已自动生成随访计划！");
        window.parent.ChronicDisease.table.refresh();
        window.parent.ChronicDisease.loadDashboard();
        parent.layer.close(parent.ChronicDisease.layerIndex);
    }, function (data) {
        Feng.error("建档失败！" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.set("patientName", $("#patientName").val());
    ajax.set("patientIdcard", $("#patientIdcard").val());
    ajax.set("diseaseType", $("#diseaseType").val());
    ajax.set("riskLevel", $("#riskLevel").val());
    ajax.set("diagnosisDate", $("#diagnosisDate").val());
    ajax.set("doctorName", $("#doctorName").val());
    ajax.set("status", $("#status").val());
    ajax.set("remark", $("#remark").val());
    ajax.start();
};

ChronicDisease.openEdit = function () {
    if (this.check()) {
        var index = layer.open({
            type: 2,
            title: '编辑慢病档案',
            area: ['800px', '420px'],
            fix: false,
            maxmin: true,
            content: Feng.ctxPath + '/chronicDisease/chronicDisease_update/' + ChronicDisease.seItem.id
        });
        this.layerIndex = index;
    }
};

ChronicDisease.editSubmit = function () {
    var ajax = new $ax(Feng.ctxPath + "/chronicDisease/update", function () {
        Feng.success("修改成功！若风险等级变更，已重新生成随访计划");
        window.parent.ChronicDisease.table.refresh();
        window.parent.ChronicDisease.loadDashboard();
        parent.layer.close(parent.ChronicDisease.layerIndex);
    }, function (data) {
        Feng.error("修改失败！" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.set("id", $("#id").val());
    ajax.set("patientName", $("#patientName").val());
    ajax.set("patientIdcard", $("#patientIdcard").val());
    ajax.set("diseaseType", $("#diseaseType").val());
    ajax.set("riskLevel", $("#riskLevel").val());
    ajax.set("diagnosisDate", $("#diagnosisDate").val());
    ajax.set("doctorName", $("#doctorName").val());
    ajax.set("status", $("#status").val());
    ajax.set("remark", $("#remark").val());
    ajax.start();
};

ChronicDisease.delete = function () {
    if (this.check()) {
        var item = ChronicDisease.seItem;
        Feng.confirm("确定要删除患者【" + item.patientName + "】的慢病档案吗？关联的随访记录和计划也将一并删除！", function () {
            var ajax = new $ax(Feng.ctxPath + "/chronicDisease/delete", function () {
                Feng.success("删除成功！");
                ChronicDisease.table.refresh();
                ChronicDisease.loadDashboard();
            }, function (data) {
                Feng.error("删除失败！" + (data.responseJSON ? data.responseJSON.message : ""));
            });
            ajax.set("chronicDiseaseId", item.id);
            ajax.start();
        });
    }
};

ChronicDisease.close = function () {
    parent.layer.close(parent.ChronicDisease.layerIndex);
};

// ==================== 风险评估 ====================

ChronicDisease.updateRiskInputs = function () {
    var diseaseType = $("#diseaseType").val();
    var container = $("#riskAssessmentInputs");
    if (!diseaseType) {
        container.html('<div class="col-sm-12"><p class="text-muted">请先选择慢病类型</p></div>');
        return;
    }

    var html = '';
    if (diseaseType === '高血压') {
        html += '<div class="col-sm-4"><label>收缩压 (mmHg)</label><input class="form-control" id="systolic" type="number" placeholder="如: 145"/></div>';
        html += '<div class="col-sm-4"><label>舒张压 (mmHg)</label><input class="form-control" id="diastolic" type="number" placeholder="如: 92"/></div>';
    } else if (diseaseType === '糖尿病') {
        html += '<div class="col-sm-4"><label>空腹血糖 (mmol/L)</label><input class="form-control" id="bloodSugar" type="number" step="0.1" placeholder="如: 8.5"/></div>';
        html += '<div class="col-sm-4"><label>糖化血红蛋白 (%)</label><input class="form-control" id="hba1c" type="number" step="0.1" placeholder="如: 7.2"/></div>';
    } else if (diseaseType === '冠心病') {
        html += '<div class="col-sm-4"><label>NYHA心功能分级</label><select class="form-control" id="nyha"><option value="1">I级</option><option value="2">II级</option><option value="3">III级</option><option value="4">IV级</option></select></div>';
        html += '<div class="col-sm-4"><label>ACS史</label><select class="form-control" id="acsHistory"><option value="0">无</option><option value="1">有</option></select></div>';
    } else if (diseaseType === '脑卒中') {
        html += '<div class="col-sm-4"><label>NIHSS评分</label><input class="form-control" id="nihss" type="number" placeholder="0-42"/></div>';
    } else if (diseaseType === '慢阻肺') {
        html += '<div class="col-sm-4"><label>FEV1%预计值</label><input class="form-control" id="fev1" type="number" step="0.1" placeholder="如: 65"/></div>';
    } else if (diseaseType === '慢性肾病') {
        html += '<div class="col-sm-4"><label>eGFR (ml/min/1.73m²)</label><input class="form-control" id="egfr" type="number" step="0.1" placeholder="如: 45"/></div>';
        html += '<div class="col-sm-4"><label>蛋白尿 (g/24h)</label><input class="form-control" id="proteinuria" type="number" step="0.1" placeholder="如: 0.5"/></div>';
    }
    container.html(html);
};

ChronicDisease.assessRiskFromForm = function () {
    var diseaseType = $("#diseaseType").val();
    if (!diseaseType) {
        Feng.info("请先选择慢病类型");
        return;
    }

    var params = {};
    params.diseaseType = diseaseType;

    var systolic = $("#systolic").val();
    if (systolic) params.systolic = systolic;
    var diastolic = $("#diastolic").val();
    if (diastolic) params.diastolic = diastolic;
    var bloodSugar = $("#bloodSugar").val();
    if (bloodSugar) params.bloodSugar = bloodSugar;
    var hba1c = $("#hba1c").val();
    if (hba1c) params.hba1c = hba1c;
    var nyha = $("#nyha").val();
    if (nyha) params.nyha = nyha;
    var acsHistory = $("#acsHistory").val();
    if (acsHistory) params.acsHistory = acsHistory;
    var nihss = $("#nihss").val();
    if (nihss) params.nihss = nihss;
    var fev1 = $("#fev1").val();
    if (fev1) params.fev1 = fev1;
    var egfr = $("#egfr").val();
    if (egfr) params.egfr = egfr;
    var proteinuria = $("#proteinuria").val();
    if (proteinuria) params.proteinuria = proteinuria;

    var queryStr = Object.keys(params).map(function(k) {
        return encodeURIComponent(k) + '=' + encodeURIComponent(params[k]);
    }).join('&');

    var url = Feng.ctxPath + "/chronicDisease/assessRisk?" + queryStr;
    var ajax = new $ax(url, function (data) {
        var riskHtml = '';
        if (data.riskLevel === '高风险') riskHtml = '<span class="label label-danger">高风险</span>';
        else if (data.riskLevel === '中风险') riskHtml = '<span class="label label-warning">中风险</span>';
        else riskHtml = '<span class="label label-success">低风险</span>';
        $("#assessRiskResult").html("评估结果：" + riskHtml);
        $("#assessFollowupInterval").text("建议随访周期：每" + data.intervalDays + "天，预计下次随访：" + (data.nextFollowupDate || '').slice(0, 10));
        // 自动填充风险等级
        $("#riskLevel").val(data.riskLevel);
    }, function (data) {
        Feng.error("评估失败：" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.start();
};

// ==================== 随访管理 ====================

ChronicDisease.openAddFollowup = function () {
    if (!this.check()) return;
    var item = ChronicDisease.seItem;

    // 获取该病种的随访模板
    var templateAjax = new $ax(Feng.ctxPath + "/chronicDisease/followupTemplate?diseaseType=" + encodeURIComponent(item.diseaseType), function (template) {
        var html = '<div class="ibox float-e-margins"><div class="ibox-content"><div class="form-horizontal">';
        html += '<h4 style="margin-bottom:15px;">新增随访记录 - ' + item.patientName + ' (' + item.diseaseType + ')</h4>';

        html += '<div class="row"><div class="col-sm-6 b-r">';
        html += '<div class="form-group"><label class="col-sm-4 control-label">随访日期</label><div class="col-sm-8"><input class="form-control" id="followupDate" type="date" value="' + new Date().toISOString().slice(0, 10) + '"/></div></div>';
        html += '<div class="form-group"><label class="col-sm-4 control-label">随访方式</label><div class="col-sm-8"><select class="form-control" id="followupType"><option value="门诊">门诊</option><option value="电话">电话</option><option value="家庭">家庭</option><option value="线上">线上</option></select></div></div>';
        html += '<div class="form-group"><label class="col-sm-4 control-label">症状描述</label><div class="col-sm-8"><textarea class="form-control" id="symptoms" rows="2" placeholder="' + (template.symptoms || '') + '"></textarea></div></div>';
        html += '<div class="form-group"><label class="col-sm-4 control-label">服药依从性</label><div class="col-sm-8"><select class="form-control" id="medicationCompliance"><option value="良好">良好</option><option value="一般">一般</option><option value="差">差</option></select></div></div>';

        // 病种特异指标
        if (template.bloodPressure) {
            html += '<div class="form-group"><label class="col-sm-4 control-label">血压</label><div class="col-sm-4"><input class="form-control" id="bloodPressure" placeholder="收缩压/舒张压 如: 140/90"/></div></div>';
        }
        if (template.bloodSugar) {
            html += '<div class="form-group"><label class="col-sm-4 control-label">血糖</label><div class="col-sm-4"><input class="form-control" id="bloodSugarFw" placeholder="mmol/L 如: 7.5"/></div></div>';
        }
        if (template.heartRate) {
            html += '<div class="form-group"><label class="col-sm-4 control-label">心率</label><div class="col-sm-4"><input class="form-control" id="heartRate" type="number" placeholder="次/分"/></div></div>';
        }
        html += '</div><div class="col-sm-6">';

        // 临床评估指标（用于自动风险评估）
        html += '<div class="form-group"><label class="col-sm-4 control-label">本次评估风险</label><div class="col-sm-8"><select class="form-control" id="followupRiskLevel"><option value="低风险">低风险</option><option value="中风险">中风险</option><option value="高风险">高风险</option></select></div></div>';
        html += '<div class="form-group"><label class="col-sm-4 control-label">生活方式建议</label><div class="col-sm-8"><textarea class="form-control" id="lifestyleAdvice" rows="3">' + (template.lifestyleAdvice || '') + '</textarea></div></div>';
        html += '<div class="form-group"><label class="col-sm-4 control-label">用药提醒</label><div class="col-sm-8"><textarea class="form-control" id="medicationReminder" rows="2">' + (template.medicationReminder || '') + '</textarea></div></div>';
        html += '</div></div>';

        html += '<div class="row btn-group-m-t"><div class="col-sm-10">';
        html += '<button class="btn btn-info" onclick="ChronicDisease.submitFollowup(' + item.id + ')"><i class="fa fa-check"></i> 提交随访</button>';
        html += '<button class="btn btn-danger" onclick="parent.layer.close(parent.ChronicDisease.layerIndex)"><i class="fa fa-eraser"></i> 取消</button>';
        html += '</div></div>';

        html += '</div></div></div>';

        var index = layer.open({
            type: 1,
            title: '慢病随访 - ' + item.diseaseType,
            area: ['900px', '520px'],
            fix: false,
            maxmin: true,
            content: html
        });
        ChronicDisease.layerIndex = index;
    }, function () {
        Feng.error("获取随访模板失败");
    });
    templateAjax.start();
};

ChronicDisease.submitFollowup = function (chronicId) {
    var ajax = new $ax(Feng.ctxPath + "/chronicDisease/followup/add", function (data) {
        Feng.success(data.message || "随访完成，已自动生成下次计划");
        ChronicDisease.table.refresh();
        ChronicDisease.loadDashboard();
        ChronicDisease.loadPendingReminders();
        parent.layer.close(ChronicDisease.layerIndex);
    }, function (data) {
        Feng.error("随访失败！" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.set("chronicId", chronicId);
    ajax.set("patientIdcard", ChronicDisease.seItem.patientIdcard);
    ajax.set("patientName", ChronicDisease.seItem.patientName);
    ajax.set("diseaseType", ChronicDisease.seItem.diseaseType);
    ajax.set("followupDate", $("#followupDate").val());
    ajax.set("followupType", $("#followupType").val());
    ajax.set("symptoms", $("#symptoms").val());
    ajax.set("bloodPressure", $("#bloodPressure").val());
    ajax.set("bloodSugar", $("#bloodSugarFw").val());
    ajax.set("heartRate", $("#heartRate").val());
    ajax.set("medicationCompliance", $("#medicationCompliance").val());
    ajax.set("lifestyleAdvice", $("#lifestyleAdvice").val());
    ajax.set("riskLevel", $("#followupRiskLevel").val());
    ajax.set("nextFollowupDate", "");
    ajax.start();
};

// ==================== 查看随访记录 ====================

ChronicDisease.viewFollowups = function () {
    if (!this.check()) return;
    var item = ChronicDisease.seItem;
    var url = Feng.ctxPath + "/chronicDisease/followup/list?chronicId=" + item.id;

    var ajax = new $ax(url, function (data) {
        var html = '<div class="ibox float-e-margins"><div class="ibox-content">';
        html += '<h4>' + item.patientName + ' - ' + item.diseaseType + ' 随访记录</h4>';
        html += '<table class="table table-striped table-bordered"><thead><tr>';
        html += '<th>随访日期</th><th>随访医生</th><th>方式</th><th>症状</th><th>血压</th><th>血糖</th><th>依从性</th><th>风险</th>';
        html += '</tr></thead><tbody>';

        if (data.length === 0) {
            html += '<tr><td colspan="8" class="text-center text-muted">暂无随访记录</td></tr>';
        }
        for (var i = 0; i < data.length; i++) {
            var f = data[i];
            html += '<tr>';
            html += '<td>' + (f.followupDate ? f.followupDate.slice(0, 10) : '') + '</td>';
            html += '<td>' + (f.followupDoctor || '') + '</td>';
            html += '<td>' + (f.followupType || '') + '</td>';
            html += '<td>' + (f.symptoms || '') + '</td>';
            html += '<td>' + (f.bloodPressure || '-') + '</td>';
            html += '<td>' + (f.bloodSugar || '-') + '</td>';
            html += '<td>' + (f.medicationCompliance || '') + '</td>';
            html += '<td>' + ChronicDisease.riskColor(f.riskLevel) + '</td>';
            html += '</tr>';
        }
        html += '</tbody></table>';

        // 随访计划
        html += '<h4 style="margin-top:20px;">随访计划</h4>';
        var planUrl = Feng.ctxPath + "/chronicDisease/plan/list?chronicId=" + item.id;
        var planAjax = new $ax(planUrl, function (plans) {
            var planHtml = '<table class="table table-striped table-bordered"><thead><tr>';
            planHtml += '<th>计划日期</th><th>随访方式</th><th>负责医生</th><th>状态</th>';
            planHtml += '</tr></thead><tbody>';
            if (plans.length === 0) {
                planHtml += '<tr><td colspan="4" class="text-center text-muted">暂无随访计划</td></tr>';
            }
            for (var j = 0; j < plans.length; j++) {
                var p = plans[j];
                planHtml += '<tr>';
                planHtml += '<td>' + (p.planDate ? p.planDate.slice(0, 10) : '') + '</td>';
                planHtml += '<td>' + (p.planType || '') + '</td>';
                planHtml += '<td>' + (p.doctorName || '') + '</td>';
                var statusText = p.status === 0 ? '<span class="label label-info">待执行</span>' :
                    (p.status === 1 ? '<span class="label label-success">已执行</span>' :
                    '<span class="label label-default">已过期</span>');
                planHtml += '<td>' + statusText + '</td>';
                planHtml += '</tr>';
            }
            planHtml += '</tbody></table>';
            $('#followupPlanSection').html(planHtml);
        }, function () {});
        planAjax.start();

        html += '<div id="followupPlanSection"></div>';
        html += '</div></div>';

        var index = layer.open({
            type: 1,
            title: '随访记录 - ' + item.patientName,
            area: ['900px', '550px'],
            fix: false,
            maxmin: true,
            content: html,
            btn: ['关闭'],
            yes: function (layIdx) { layer.close(layIdx); }
        });
    }, function () {
        Feng.error("获取随访记录失败");
    });
    ajax.start();
};

// ==================== 监听病种选择变化 ====================

$(document).on('change', '#diseaseType', function () {
    if (typeof ChronicDisease.updateRiskInputs === 'function') {
        ChronicDisease.updateRiskInputs();
    }
});

// ==================== 初始化 ====================

$(function () {
    var defaultColunms = ChronicDisease.initColumn();
    var table = new BSTable(ChronicDisease.id, "/chronicDisease/list", defaultColunms);
    table.setPaginationType("client");
    ChronicDisease.table = table.init();

    ChronicDisease.loadDashboard();
    ChronicDisease.loadPendingReminders();

    // 定时刷新仪表盘（每60秒）
    setInterval(function () {
        ChronicDisease.loadDashboard();
        ChronicDisease.loadPendingReminders();
    }, 60000);
});
