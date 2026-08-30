/**
 * 医护端门户
 */
var DoctorPortal = {
    currentTab: 'dashboard',
    myDoctorName: ''  // 当前登录医生姓名，用于判断是否可接诊
};

/**
 * 初始化
 */
DoctorPortal.init = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/my_name',
        type: 'POST',
        async: false,
        success: function (res) {
            DoctorPortal.myDoctorName = res.doctorName || '';
        }
    });
    this.loadDashboard();
};

/**
 * 加载工作台仪表盘
 */
DoctorPortal.loadDashboard = function () {
    this.loadDashboardStats();
    this.loadTodaySchedule();
    this.loadRecentHistory();
    this.loadDashboardAlerts();
    this.loadPublicHealthSummary();
};

DoctorPortal.loadDashboardStats = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/dashboard_stats',
        type: 'POST',
        success: function (s) {
            $('#dashMyPatients').text(s.myPatientCount || 0);
            $('#dashHealthRecords').text(s.healthRecordCount || 0);
            $('#dashMedCount').text(s.medicineCount || 0);
        }
    });
    // 就诊台统计
    $.post(Feng.ctxPath + '/doctor_portal/consultation_stats', function (s) {
        $('#dashWaiting').text(s.waitingCount || 0);
        $('#dashInProgress').text(s.inProgressCount || 0);
        // 今日预约 = 候诊 + 就诊中 + 今日已完成
        var todayTotal = (s.waitingCount || 0) + (s.inProgressCount || 0);
        $('#dashTodayAppt').text(todayTotal);
        $('#dashTodayDone').text(s.completedCount || 0);
    });
    // 慢病统计
    $.post(Feng.ctxPath + '/doctor_portal/chronic_stats', function (d) {
        var total = d.totalCount || 0;
        var pending = d.pendingFollowupCount || 0;
        $('#dashChronicCount').text(total);
        if (pending > 0) {
            $('#dashChronicPending').text('(' + pending + '待随访)');
        } else {
            $('#dashChronicPending').text('');
        }
    });
    // 药品预警
    $.post(Feng.ctxPath + '/medicineInfo/lowStock', function (d) {
        var lowCount = d ? d.length : 0;
        if (lowCount > 0) {
            $('#dashMedAlert').text('(' + lowCount + '库存低)');
        } else {
            $('#dashMedAlert').text('');
        }
    });
};

DoctorPortal.loadTodaySchedule = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/all_appointments',
        type: 'POST',
        success: function (data) {
            var today = new Date().toISOString().substring(0, 10);
            var html = '';
            var list = [];
            if (data) {
                for (var i = 0; i < data.length; i++) {
                    var d = data[i].pointDate || '';
                    if (d.substring(0, 10) === today) list.push(data[i]);
                }
            }
            if (list.length === 0) {
                html = '<tr><td colspan="6" class="empty-data">今日暂无预约</td></tr>';
            } else {
                for (var j = 0; j < list.length; j++) {
                    var item = list[j];
                    var timeStr = (item.pointDate || '').substring(11, 16) || '-';
                    var statusHtml = DoctorPortal.getStatusText(item.status);
                    var isMyPatient = item.doctorName === DoctorPortal.myDoctorName;
                    html += '<tr' + (item.status === 3 ? ' style="background:#e3f2fd;"' : '') + '>';
                    html += '<td><strong>' + timeStr + '</strong></td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + (item.pointPlace || '-') + '</td>';
                    html += '<td>' + statusHtml + '</td>';
                    if (isMyPatient && item.status === 0) {
                        html += '<td><button class="btn btn-xs btn-primary" onclick="DoctorPortal.startConsultation(' + item.id + ')">接诊</button></td>';
                    } else if (item.status === 3) {
                        html += '<td><span style="color:#1a9bfc;font-size:12px;">就诊中</span></td>';
                    } else {
                        html += '<td>-</td>';
                    }
                    html += '</tr>';
                }
            }
            $('#dashTodaySchedule').html(html);
        },
        error: function () {
            $('#dashTodaySchedule').html('<tr><td colspan="6" class="empty-data">加载失败</td></tr>');
        }
    });
};

DoctorPortal.loadRecentHistory = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/all_histories',
        type: 'POST',
        success: function (data) {
            var html = '';
            var list = data ? data.slice(0, 5) : [];
            if (list.length === 0) {
                html = '<tr><td colspan="5" class="empty-data">暂无就诊记录</td></tr>';
            } else {
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    html += '<tr>';
                    html += '<td>' + ((item.patientHistoryDate || '').substring(0, 16) || '-') + '</td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + ((item.patientSym || '').substring(0, 12) || '-') + '</td>';
                    html += '<td>' + ((item.patientMedicine || '').substring(0, 12) || '-') + '</td>';
                    html += '<td>' + (item.patientDoctor || '-') + '</td>';
                    html += '</tr>';
                }
            }
            $('#dashRecentHistory').html(html);
        },
        error: function () {
            $('#dashRecentHistory').html('<tr><td colspan="5" class="empty-data">加载失败</td></tr>');
        }
    });
};

DoctorPortal.loadDashboardAlerts = function () {
    var alertsHtml = '';
    var alertCount = 0;

    // 1. 检查逾期随访计划
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/chronic_followup_plans',
        type: 'POST',
        async: false,
        success: function (plans) {
            if (plans) {
                var overdue = [];
                var now = new Date();
                for (var i = 0; i < plans.length; i++) {
                    if (plans[i].status === 2) { // 已过期
                        overdue.push(plans[i]);
                    } else if (plans[i].status === 0 && new Date(plans[i].planDate) < now) {
                        overdue.push(plans[i]);
                    }
                }
                if (overdue.length > 0) {
                    alertCount += overdue.length;
                    alertsHtml += '<div style="padding:8px 12px;margin:4px 0;background:#ffebee;border-radius:6px;border-left:3px solid #e74c3c;">';
                    alertsHtml += '<i class="fa fa-calendar-times-o" style="color:#e74c3c;"></i> <strong style="color:#c62828;">' + overdue.length + ' 个随访计划已逾期</strong>';
                    alertsHtml += '<br><small style="color:#888;">请及时处理慢病随访</small>';
                    alertsHtml += '</div>';
                }
            }
        }
    });

    // 2. 检查低库存药品
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/lowStock',
        type: 'POST',
        async: false,
        success: function (data) {
            if (data && data.length > 0) {
                alertCount += data.length;
                alertsHtml += '<div style="padding:8px 12px;margin:4px 0;background:#fff3e0;border-radius:6px;border-left:3px solid #f39c12;">';
                alertsHtml += '<i class="fa fa-exclamation-circle" style="color:#f39c12;"></i> <strong style="color:#e65100;">' + data.length + ' 种药品库存不足</strong>';
                alertsHtml += '<br><small style="color:#888;">';
                for (var i = 0; i < Math.min(data.length, 3); i++) {
                    alertsHtml += data[i].medicineName + '(' + data[i].medicineStock + ') ';
                }
                if (data.length > 3) alertsHtml += '...';
                alertsHtml += '</small></div>';
            }
        }
    });

    // 3. 近效期检查
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/lowStock',
        type: 'POST',
        async: false,
        success: function (data) {
            var nearExpiryCount = 0;
            if (data) {
                for (var i = 0; i < data.length; i++) {
                    if (data[i].expiryDate) {
                        var daysLeft = Math.ceil((new Date(data[i].expiryDate) - new Date()) / (1000 * 60 * 60 * 24));
                        if (daysLeft > 0 && daysLeft <= 90) nearExpiryCount++;
                    }
                }
                if (nearExpiryCount > 0) {
                    alertCount += nearExpiryCount;
                    alertsHtml += '<div style="padding:8px 12px;margin:4px 0;background:#fff8e1;border-radius:6px;border-left:3px solid #f1c40f;">';
                    alertsHtml += '<i class="fa fa-clock-o" style="color:#f1c40f;"></i> <strong style="color:#e67e22;">' + nearExpiryCount + ' 个批次临近效期(≤90天)</strong>';
                    alertsHtml += '</div>';
                }
            }
        }
    });

    if (alertCount === 0) {
        alertsHtml = '<div style="text-align:center;padding:20px;color:#27ae60;">';
        alertsHtml += '<i class="fa fa-check-circle" style="font-size:28px;display:block;margin-bottom:8px;"></i>';
        alertsHtml += '一切正常，无待办告警</div>';
    }

    $('#dashAlertList').html(alertsHtml);
};

DoctorPortal.loadPublicHealthSummary = function () {
    $.post(Feng.ctxPath + '/doctor_portal/public_health_stats', function (d) {
        $('#dashVaccCount').text(d.vaccinationCount || 0);
        $('#dashMaternalCount').text(d.maternalCount || 0);
        $('#dashElderlyCount').text(d.elderlyCount || 0);
        $('#dashInfectCount').text(d.infectiousCount || 0);
    });
};

/**
 * 切换Tab
 */
DoctorPortal.switchTab = function (tabName) {
    this.currentTab = tabName;

    // 隐藏所有面板
    $('[id^="panel-"]').hide();
    // 显示目标面板
    $('#panel-' + tabName).show();

    // 更新导航栏高亮
    $('.portal-navbar .nav > li').removeClass('active');
    $('.portal-navbar .nav > li').each(function () {
        var onclick = $(this).find('a').attr('onclick') || '';
        if (onclick.indexOf("'" + tabName + "'") > -1) {
            $(this).addClass('active');
        }
    });

    // 加载对应数据
    if (tabName === 'dashboard') {
        this.loadDashboard();
    } else if (tabName === 'consultation') {
        this.loadConsultationDesk();
    } else if (tabName === 'appointments') {
        this.loadAppointmentList();
    } else if (tabName === 'health') {
        this.loadHealthList();
    } else if (tabName === 'history') {
        this.loadHistoryList();
    } else if (tabName === 'medicine') {
        this.switchMedSubTab('catalog');
        this.loadMedStats();
    } else if (tabName === 'chronic') {
        this.loadChronicList();
        this.loadChronicStats();
        this.loadFollowupPlanList();
        this.loadPendingReminders();
    } else if (tabName === 'public_health') {
        this.loadPublicHealthData();
    }
};

// ==================== 慢病管理 ====================

/**
 * 加载慢病档案列表
 */
DoctorPortal.loadChronicList = function () {
    var diseaseType = $('#chronicDiseaseFilter').val() || '';
    var riskLevel = $('#chronicRiskFilter').val() || '';
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/chronic_list',
        type: 'POST',
        data: {diseaseType: diseaseType, riskLevel: riskLevel},
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="9" class="empty-data">暂无慢病档案</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var riskBadge = '';
                    if (item.riskLevel === '高风险') {
                        riskBadge = '<span style="background:#e74c3c;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">高风险</span>';
                    } else if (item.riskLevel === '中风险') {
                        riskBadge = '<span style="background:#f39c12;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">中风险</span>';
                    } else {
                        riskBadge = '<span style="background:#27ae60;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">低风险</span>';
                    }
                    var statusText = item.status === 1 ? '管理中' : (item.status === 2 ? '已转诊' : '已结案');
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.patientIdcard || '-') + '</td>';
                    html += '<td>' + (item.diseaseType || '-') + '</td>';
                    html += '<td>' + riskBadge + '</td>';
                    html += '<td>' + (item.diagnosisDate ? item.diagnosisDate.substring(0, 10) : '-') + '</td>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + statusText + '</td>';
                    html += '<td>';
                    html += '<button class="btn btn-xs btn-warning" onclick="DoctorPortal.openChronicEdit(' + item.id + ',\'' + (item.patientName || '').replace(/'/g, "\\'") + '\',\'' + (item.patientIdcard || '').replace(/'/g, "\\'") + '\',\'' + (item.diseaseType || '') + '\',\'' + (item.riskLevel || '') + '\',\'' + (item.diagnosisDate || '') + '\',\'' + (item.remark || '').replace(/'/g, "\\'") + '\')"><i class="fa fa-edit"></i> 编辑</button> ';
                    html += '<button class="btn btn-xs btn-info" onclick="DoctorPortal.viewFollowups(' + item.id + ')"><i class="fa fa-file-text-o"></i> 随访</button> ';
                    html += '<button class="btn btn-xs btn-primary" onclick="DoctorPortal.openFollowupAdd(' + item.id + ',\'' + (item.patientName || '') + '\',\'' + (item.diseaseType || '') + '\')"><i class="fa fa-plus"></i> 随访</button> ';
                    html += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteChronic(' + item.id + ')"><i class="fa fa-trash"></i></button>';
                    html += '</td>';
                    html += '</tr>';
                }
            }
            $('#chronicTable').html(html);
        },
        error: function () {
            $('#chronicTable').html('<tr><td colspan="9" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 加载慢病统计数据
 */
DoctorPortal.loadChronicStats = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/chronic_stats',
        type: 'POST',
        success: function (data) {
            $('#chronicTotalCount').text(data.totalCount || 0);
            $('#chronicLowCount').text((data.riskCount && data.riskCount['低风险']) || 0);
            $('#chronicMidCount').text((data.riskCount && data.riskCount['中风险']) || 0);
            $('#chronicHighCount').text((data.riskCount && data.riskCount['高风险']) || 0);
            $('#chronicPendingFollowup').text(data.pendingFollowupCount || 0);
        }
    });
};

/**
 * 加载随访计划列表
 */
DoctorPortal.loadFollowupPlanList = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/chronic_followup_plans',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="7" class="empty-data">暂无随访计划</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var statusBadge = '';
                    if (item.status === 0) {
                        statusBadge = '<span style="color:#3498db;">待执行</span>';
                    } else if (item.status === 1) {
                        statusBadge = '<span style="color:#27ae60;">已执行</span>';
                    } else {
                        statusBadge = '<span style="color:#e74c3c;">已过期</span>';
                    }
                    html += '<tr>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.diseaseType || '-') + '</td>';
                    html += '<td>' + (item.planDate ? item.planDate.substring(0, 16) : '-') + '</td>';
                    html += '<td>' + (item.planType || '-') + '</td>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + statusBadge + '</td>';
                    if (item.status === 0) {
                        html += '<td><button class="btn btn-xs btn-primary" onclick="DoctorPortal.openFollowupAdd(' + item.chronicId + ',\'' + (item.patientName || '') + '\',\'' + (item.diseaseType || '') + '\')"><i class="fa fa-stethoscope"></i> 执行随访</button></td>';
                    } else {
                        html += '<td>-</td>';
                    }
                    html += '</tr>';
                }
            }
            $('#followupPlanTable').html(html);
        },
        error: function () {
            $('#followupPlanTable').html('<tr><td colspan="7" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 打开新增慢病档案模态框
 */
DoctorPortal.openChronicAdd = function () {
    $('#chronicAddModal').modal('show');
};

/**
 * 提交新增慢病档案
 */
DoctorPortal.submitChronic = function () {
    var data = {
        patientName: $('#addChronicPatientName').val(),
        patientIdcard: $('#addChronicPatientIdcard').val(),
        diseaseType: $('#addChronicDiseaseType').val(),
        riskLevel: $('#addChronicRiskLevel').val(),
        diagnosisDate: $('#addChronicDiagnosisDate').val(),
        remark: $('#addChronicRemark').val()
    };
    if (!data.patientName) { Feng.error('请输入患者姓名'); return; }
    if (!data.diseaseType) { Feng.error('请选择慢病类型'); return; }
    if (!data.riskLevel) { Feng.error('请选择风险等级'); return; }
    if (!data.diagnosisDate) { Feng.error('请选择确诊日期'); return; }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_chronic',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('慢病档案添加成功，已自动生成随访计划');
                $('#chronicAddModal').modal('hide');
                DoctorPortal.loadChronicList();
                DoctorPortal.loadChronicStats();
                DoctorPortal.loadFollowupPlanList();
            } else {
                Feng.error('添加失败：' + (res.message || ''));
            }
        },
        error: function () { Feng.error('添加失败！服务器异常'); }
    });
};

/**
 * 删除慢病档案
 */
DoctorPortal.deleteChronic = function (id) {
    layer.confirm('确定删除该慢病档案？关联的随访记录和计划也将被删除', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/delete_chronic',
            type: 'POST',
            data: {id: id},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('删除成功');
                    DoctorPortal.loadChronicList();
                    DoctorPortal.loadChronicStats();
                    DoctorPortal.loadFollowupPlanList();
                }
            }
        });
        layer.close(index);
    });
};

/**
 * 查看随访记录
 */
DoctorPortal.viewFollowups = function (chronicId) {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/chronic_followup_list',
        type: 'POST',
        data: {chronicId: chronicId},
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="10" class="empty-data">暂无随访记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    html += '<tr>';
                    html += '<td>' + (item.followupDate ? item.followupDate.substring(0, 16) : '-') + '</td>';
                    html += '<td>' + (item.followupDoctor || '-') + '</td>';
                    html += '<td>' + (item.followupType || '-') + '</td>';
                    html += '<td>' + (item.symptoms || '-') + '</td>';
                    html += '<td>' + (item.bloodPressure || '-') + '</td>';
                    html += '<td>' + (item.bloodSugar || '-') + '</td>';
                    html += '<td>' + (item.heartRate || '-') + '</td>';
                    html += '<td>' + (item.medicationCompliance || '-') + '</td>';
                    html += '<td>' + (item.riskLevel || '-') + '</td>';
                    html += '<td>' + (item.lifestyleAdvice || '-') + '</td>';
                    html += '</tr>';
                }
            }
            $('#followupDetailTable').html(html);
            $('#followupListModal').modal('show');
        }
    });
};

/**
 * 打开新增随访记录模态框
 */
DoctorPortal.openFollowupAdd = function (chronicId, patientName, diseaseType) {
    $('#addFollowupChronicId').val(chronicId);
    $('#addFollowupPatientName').val(patientName);
    $('#addFollowupDiseaseType').val(diseaseType);
    $('#addFollowupPatientNameDisplay').val(patientName);
    $('#addFollowupDiseaseTypeDisplay').val(diseaseType);
    // 根据病种显示/隐藏专用字段
    if (diseaseType === '高血压' || diseaseType === '冠心病') {
        $('#bloodPressureGroup').show();
    } else {
        $('#bloodPressureGroup').hide();
    }
    if (diseaseType === '糖尿病') {
        $('#bloodSugarGroup').show();
    } else {
        $('#bloodSugarGroup').hide();
    }
    $('#followupAddModal').modal('show');
};

/**
 * 提交新增随访记录
 */
DoctorPortal.submitFollowup = function () {
    var data = {
        chronicId: $('#addFollowupChronicId').val(),
        patientName: $('#addFollowupPatientName').val(),
        patientIdcard: '',
        diseaseType: $('#addFollowupDiseaseType').val(),
        followupType: $('#addFollowupType').val(),
        symptoms: $('#addFollowupSymptoms').val(),
        bloodPressure: $('#addFollowupBloodPressure').val(),
        bloodSugar: $('#addFollowupBloodSugar').val(),
        heartRate: $('#addFollowupHeartRate').val(),
        medicationCompliance: $('#addFollowupCompliance').val(),
        lifestyleAdvice: $('#addFollowupLifestyle').val(),
        riskLevel: $('#addFollowupRiskLevel').val(),
        followupDate: $('#addFollowupDate').val()
    };
    if (!data.followupDate) { Feng.error('请选择随访日期'); return; }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_chronic_followup',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('随访记录添加成功');
                $('#followupAddModal').modal('hide');
                DoctorPortal.loadChronicList();
                DoctorPortal.loadChronicStats();
                DoctorPortal.loadFollowupPlanList();
            } else {
                Feng.error('添加失败：' + (res.message || ''));
            }
        },
        error: function () { Feng.error('添加失败！服务器异常'); }
    });
};

/**
 * 加载预约管理列表
 */
DoctorPortal.loadAppointmentList = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/all_appointments',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="7" class="empty-data">暂无预约记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var isMyPatient = item.doctorName === DoctorPortal.myDoctorName;
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + (item.pointDate || '-') + '</td>';
                    html += '<td>' + (item.pointPlace || '-') + '</td>';
                    html += '<td>' + DoctorPortal.getStatusText(item.status) + '</td>';
                    if (isMyPatient) {
                        html += '<td><button class="btn btn-xs btn-primary" onclick="DoctorPortal.openAppointmentEdit(' + item.id + ',\'' + (item.patientName || '').replace(/'/g, "\\'") + '\',\'' + (item.patientIdcard || '').replace(/'/g, "\\'") + '\',\'' + (item.pointDate || '').replace(/'/g, "\\'") + '\',\'' + (item.pointPlace || '').replace(/'/g, "\\'") + '\')"><i class="fa fa-edit"></i> 编辑</button> ';
                        html += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteAppointment(' + item.id + ')"><i class="fa fa-trash"></i> 删除</button></td>';
                    } else {
                        html += '<td><span class="text-muted">仅可操作本人预约</span></td>';
                    }
                    html += '</tr>';
                }
            }
            $('#appointmentTable').html(html);
        },
        error: function () {
            $('#appointmentTable').html('<tr><td colspan="7" class="empty-data">加载失败</td></tr>');
        }
    });
};

// ==================== 慢病管理 ====================


/**
 * 获取预约状态文字
 */
DoctorPortal.getStatusText = function (status) {
    if (status === 0) return '<span class="label label-warning">待参与</span>';
    if (status === 1) return '<span class="label label-success">已完成</span>';
    if (status === 2) return '<span class="label label-default">已逾期</span>';
    if (status === 3) return '<span class="label label-primary">就诊中</span>';
    return '<span class="label label-default">未知</span>';
};

/**
 * 加载健康监测列表
 */
DoctorPortal.loadHealthList = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/health_records',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="9" class="empty-data">暂无健康记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var statusTag = DoctorPortal.getHealthStatus(item);
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.heartJump || '-') + '</td>';
                    html += '<td>' + (item.bloodPressure || '-') + '</td>';
                    html += '<td>' + (item.bloodOx || '-') + '</td>';
                    html += '<td>' + (item.pulse || '-') + '</td>';
                    html += '<td>' + (item.date || '-') + '</td>';
                    html += '<td>' + statusTag + '</td>';
                    html += '<td><button class="btn btn-xs btn-primary" onclick="DoctorPortal.openHealthEdit(' + item.id + ',' + (item.heartJump || 0) + ',' + (item.bloodPressure || 0) + ',' + (item.bloodOx || 0) + ',' + (item.pulse || 0) + ',\'' + (item.date || '') + '\')"><i class="fa fa-edit"></i> 编辑</button> ';
                    html += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteHealth(' + item.id + ')"><i class="fa fa-trash"></i> 删除</button></td>';
                    html += '</tr>';
                }
            }
            $('#healthTable').html(html);
        },
        error: function () {
            $('#healthTable').html('<tr><td colspan="9" class="empty-data">加载失败</td></tr>');
        }
    });
};

// ==================== 慢病管理 ====================


/**
 * 加载就诊记录列表
 */
DoctorPortal.loadHistoryList = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/all_histories',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="8" class="empty-data">暂无就诊记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.patientName || '-') + '</td>';
                    html += '<td>' + (item.patientSym || '-') + '</td>';
                    html += '<td>' + (item.patientDoctor || '-') + '</td>';
                    html += '<td>' + (item.patientMedicine || '-') + '</td>';
                    html += '<td>' + (item.takeprice || '-') + '</td>';
                    html += '<td>' + (item.patientHistoryDate || '-') + '</td>';
                    html += '<td>';
                    html += '<button class="btn btn-xs btn-primary" onclick="DoctorPortal.openHistoryEdit(' + item.id + ',\'' + (item.patientSym || '').replace(/'/g, "\\'") + '\',\'' + (item.patientMedicine || '').replace(/'/g, "\\'") + '\',' + (item.takeprice || 0) + ',\'' + (item.patientHistoryDate || '') + '\')"><i class="fa fa-edit"></i> 编辑</button> ';
                    html += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteHistory(' + item.id + ')"><i class="fa fa-trash"></i> 删除</button> ';
                    html += '<button class="btn btn-xs btn-info" onclick="DoctorPortal.exportHistoryOne(' + item.id + ')"><i class="fa fa-download"></i> 导出</button> ';
                    html += '<button class="btn btn-xs btn-warning" onclick="DoctorPortal.printHistory(' + item.id + ')"><i class="fa fa-print"></i> 打印</button>';
                    html += '</td>';
                    html += '</tr>';
                }
            }
            $('#historyTable').html(html);
        },
        error: function () {
            $('#historyTable').html('<tr><td colspan="8" class="empty-data">加载失败</td></tr>');
        }
    });
};

// ==================== 慢病管理 ====================


/**
 * 加载药品列表
 */
DoctorPortal.loadMedicineList = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/medicines',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="6" class="empty-data">暂无药品信息</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.medicineName || '-') + '</td>';
                    html += '<td>' + (item.medicinePrice || '-') + '</td>';
                    html += '<td>' + (item.medicineValue || '-') + '</td>';
                    html += '<td>' + DoctorPortal.renderStockBadge(item.medicineStock) + '</td>';
                    html += '<td><button class="btn btn-xs btn-primary" onclick="DoctorPortal.openMedicineEdit(' + item.id + ',\'' + (item.medicineName || '').replace(/'/g, "\\'") + '\',' + (item.medicinePrice || 0) + ',\'' + (item.medicineValue || '').replace(/'/g, "\\'") + '\',' + (item.medicineStock || 0) + ')"><i class="fa fa-edit"></i> 编辑</button> ';
                    html += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteMedicine(' + item.id + ')"><i class="fa fa-trash"></i> 删除</button></td>';
                    html += '</tr>';
                }
            }
            $('#medicineTable').html(html);
        },
        error: function () {
            $('#medicineTable').html('<tr><td colspan="6" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 渲染库存余量标签
 */
DoctorPortal.renderStockBadge = function (stock) {
    var s = stock != null ? stock : 0;
    if (s > 50) {
        return '<span class="stock-badge stock-sufficient">' + s + '</span>';
    } else if (s > 10) {
        return '<span class="stock-badge stock-warning">' + s + '</span>';
    } else if (s > 0) {
        return '<span class="stock-badge stock-low">' + s + '</span>';
    } else {
        return '<span class="stock-badge stock-out">售罄</span>';
    }
};

// ==================== 药品管理子模块 ====================

DoctorPortal.medSubTab = 'catalog';

DoctorPortal.switchMedSubTab = function (subTab) {
    this.medSubTab = subTab;
    $('#subpanel-medicine-catalog, #subpanel-medicine-batch, #subpanel-medicine-stock, #subpanel-medicine-alert').hide();
    $('#subpanel-medicine-' + subTab).show();
    $('#panel-medicine .nav-tabs li').removeClass('active');
    $('#panel-medicine .nav-tabs li a').each(function () {
        if ($(this).attr('onclick') && $(this).attr('onclick').indexOf("'" + subTab + "'") > -1) {
            $(this).parent().addClass('active');
        }
    });
    if (subTab === 'catalog') {
        DoctorPortal.loadMedicineList();
    } else if (subTab === 'batch') {
        DoctorPortal.loadMedicineSelectOptions();
        DoctorPortal.loadBatchList();
    } else if (subTab === 'stock') {
        DoctorPortal.loadMedicineSelectOptions();
        DoctorPortal.loadStockLog();
    } else if (subTab === 'alert') {
        DoctorPortal.loadLowStock();
        DoctorPortal.loadNearExpiry();
    }
};

DoctorPortal.searchMedicine = function () {
    var keyword = ($('#medSearchInput').val() || '').toLowerCase();
    $('#medicineTable tr').each(function () {
        var text = $(this).text().toLowerCase();
        if (text.indexOf(keyword) > -1 || $(this).find('.empty-data').length > 0) {
            $(this).show();
        } else {
            $(this).hide();
        }
    });
};

DoctorPortal.loadMedStats = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/medicines',
        type: 'POST',
        success: function (data) {
            $('#medTotalCount').text(data ? data.length : 0);
        }
    });
    $.post(Feng.ctxPath + '/medicineInfo/lowStock', function (d) {
        $('#medLowStockCount').text(d ? d.length : 0);
        var nearExpiryCount = 0;
        if (d) {
            for (var i = 0; i < d.length; i++) {
                if (d[i].expiryWarning) nearExpiryCount++;
            }
        }
        $('#medNearExpiryCount').text(nearExpiryCount);
        if (nearExpiryCount > 0) {
            $('#medAlertBadge').text(nearExpiryCount).show();
        } else {
            $('#medAlertBadge').hide();
        }
    });
    $('#medBatchCount').text('-');
};

DoctorPortal.loadMedicineSelectOptions = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/medicines',
        type: 'POST',
        success: function (data) {
            var opts = '<option value="">请选择药品</option>';
            if (data) {
                for (var i = 0; i < data.length; i++) {
                    opts += '<option value="' + data[i].id + '">' + data[i].medicineName + '</option>';
                }
            }
            $('#stockInMedicineId, #stockOutMedicineId, #batchMedicineFilter').html(opts);
        }
    });
};

DoctorPortal.loadBatchList = function () {
    var medicineId = $('#batchMedicineFilter').val() || '';
    if (!medicineId) {
        $('#batchTable').html('<tr><td colspan="6" class="empty-data">请先选择一个药品查看批次</td></tr>');
        return;
    }
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/batches/' + medicineId,
        type: 'GET',
        success: function (data) {
            var html = '';
            var items = $.isArray(data) ? data : [];
            if (items.length === 0) {
                html = '<tr><td colspan="6" class="empty-data">暂无批次信息，请先入库</td></tr>';
            } else {
                for (var k = 0; k < items.length; k++) {
                    var b = items[k];
                    var expDate = b.expiryDate || '';
                    var prodDate = b.productionDate || '';
                    var today = new Date();
                    var exp = new Date(expDate);
                    var daysLeft = expDate ? Math.ceil((exp - today) / (1000 * 60 * 60 * 24)) : null;
                    var expBadge = '';
                    if (!expDate) {
                        expBadge = '<span style="color:#999;">未知</span>';
                    } else if (daysLeft < 0) {
                        expBadge = '<span style="background:#e74c3c;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">已过期</span>';
                    } else if (daysLeft <= 90) {
                        expBadge = '<span style="background:#f39c12;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">' + daysLeft + '天</span>';
                    } else {
                        expBadge = '<span style="color:#27ae60;">' + daysLeft + '天</span>';
                    }
                    var statusText = b.status === 1 ? '在用' : (b.status === 0 ? '已用完' : '已过期');
                    html += '<tr>';
                    html += '<td>' + (b.medicineName || '-') + '</td>';
                    html += '<td>' + (b.batchNo || '-') + '</td>';
                    html += '<td>' + (prodDate ? prodDate.substring(0, 10) : '-') + '</td>';
                    html += '<td>' + (expDate ? expDate.substring(0, 10) : '-') + '</td>';
                    html += '<td>' + (b.remainingQuantity || 0) + '</td>';
                    html += '<td>' + expBadge + '</td>';
                    html += '</tr>';
                }
            }
            $('#batchTable').html(html);
        },
        error: function () {
            $('#batchTable').html('<tr><td colspan="6" class="empty-data">加载失败</td></tr>');
        }
    });
};

DoctorPortal.loadStockOutBatches = function () {
    var medicineId = $('#stockOutMedicineId').val();
    if (!medicineId) {
        $('#stockOutBatchId').html('<option value="">请先选择药品</option>');
        return;
    }
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/batches/' + medicineId,
        type: 'GET',
        success: function (data) {
            var opts = '<option value="">请选择批次</option>';
            if (data && data.length > 0) {
                for (var i = 0; i < data.length; i++) {
                    if (data[i].remainingQuantity > 0) {
                        var qty = data[i].remainingQuantity || 0;
                        opts += '<option value="' + data[i].batchNo + '">' + (data[i].batchNo || '批次') + ' (库存:' + qty + ')</option>';
                    }
                }
            }
            $('#stockOutBatchId').html(opts);
        }
    });
};

DoctorPortal.submitStockIn = function () {
    var medicineId = $('#stockInMedicineId').val();
    var batchNo = $('#stockInBatchNo').val();
    var prodDate = $('#stockInProdDate').val();
    var expDate = $('#stockInExpDate').val();
    var quantity = $('#stockInQuantity').val();
    var supplier = $('#stockInSupplier').val();
    var unitPrice = $('#stockInPrice').val();

    if (!medicineId || !batchNo || !expDate || !quantity) {
        Feng.error('请填写完整的入库信息（药品、批次号、有效期、数量为必填）！'); return;
    }
    if (prodDate && expDate && new Date(expDate) <= new Date(prodDate)) {
        Feng.error('有效期必须晚于生产日期！'); return;
    }

    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/stockIn',
        type: 'POST',
        data: {
            medicineId: medicineId, batchNo: batchNo,
            expiryDate: expDate, quantity: quantity, supplier: supplier || '', unitPrice: unitPrice || ''
        },
        success: function (res) {
            if (res.code === 200) {
                Feng.success('入库成功！');
                $('#stockInForm')[0].reset();
                DoctorPortal.loadMedStats();
                DoctorPortal.loadStockLog();
            } else {
                Feng.error('入库失败：' + (res.message || ''));
            }
        },
        error: function () { Feng.error('入库失败！服务器异常'); }
    });
};

DoctorPortal.submitStockOut = function () {
    var medicineId = $('#stockOutMedicineId').val();
    var batchNo = $('#stockOutBatchId').val();
    var quantity = $('#stockOutQuantity').val();
    var patientName = $('#stockOutTarget').val();
    var reason = $('#stockOutReason').val();

    if (!medicineId || !batchNo || !quantity) {
        Feng.error('请填写完整的出库信息（药品、批次、数量为必填）！'); return;
    }

    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/stockOut',
        type: 'POST',
        data: {
            medicineId: medicineId, batchNo: batchNo, quantity: quantity,
            patientName: patientName || '', reason: reason || ''
        },
        success: function (res) {
            if (res.code === 200) {
                Feng.success('出库成功！');
                $('#stockOutForm')[0].reset();
                $('#stockOutBatchId').html('<option value="">请先选择药品</option>');
                DoctorPortal.loadMedStats();
                DoctorPortal.loadStockLog();
                DoctorPortal.loadBatchList();
            } else {
                Feng.error('出库失败：' + (res.message || ''));
            }
        },
        error: function () { Feng.error('出库失败！服务器异常'); }
    });
};

DoctorPortal.loadStockLog = function () {
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/stockLog',
        type: 'POST',
        data: {limit: 20},
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="6" class="empty-data">暂无操作记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var log = data[i];
                    var typeBadge = log.type === '入库' ?
                        '<span style="color:#27ae60;">入库</span>' : '<span style="color:#e74c3c;">出库</span>';
                    html += '<tr>';
                    html += '<td>' + (log.createTime || log.operationTime || '-') + '</td>';
                    html += '<td>' + (log.medicineName || '-') + '</td>';
                    html += '<td>' + typeBadge + '</td>';
                    html += '<td>' + (log.batchNo || '-') + '</td>';
                    html += '<td>' + (log.quantity || '-') + '</td>';
                    html += '<td>' + (log.operatorName || log.operator || '-') + '</td>';
                    html += '</tr>';
                }
            }
            $('#stockLogTable').html(html);
        },
        error: function () {
            $('#stockLogTable').html('<tr><td colspan="6" class="empty-data">加载失败</td></tr>');
        }
    });
};

DoctorPortal.loadLowStock = function () {
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/lowStock',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="4" class="empty-data" style="color:#27ae60;"><i class="fa fa-check-circle"></i> 库存充足，无预警</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var stock = item.medicineStock || item.stock || 0;
                    var threshold = item.minStock || item.lowStockThreshold || 10;
                    html += '<tr>';
                    html += '<td>' + (item.medicineName || '-') + '</td>';
                    html += '<td><span style="color:#e74c3c;font-weight:bold;">' + stock + '</span></td>';
                    html += '<td>' + threshold + '</td>';
                    html += '<td><span style="background:#e74c3c;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">库存不足</span></td>';
                    html += '</tr>';
                }
            }
            $('#lowStockTable').html(html);
        },
        error: function () {
            $('#lowStockTable').html('<tr><td colspan="4" class="empty-data">加载失败</td></tr>');
        }
    });
};

DoctorPortal.loadNearExpiry = function () {
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/lowStock',
        type: 'POST',
        success: function (data) {
            var html = '';
            var nearExpiryItems = [];
            if (data) {
                for (var i = 0; i < data.length; i++) {
                    if (data[i].expiryWarning || data[i].nearExpiry) {
                        nearExpiryItems.push(data[i]);
                    }
                }
            }
            if (nearExpiryItems.length === 0) {
                html = '<tr><td colspan="4" class="empty-data" style="color:#27ae60;"><i class="fa fa-check-circle"></i> 无近效期药品</td></tr>';
            } else {
                for (var j = 0; j < nearExpiryItems.length; j++) {
                    var item = nearExpiryItems[j];
                    var expDate = item.expiryDate || item.expDate || '';
                    var daysLeft = '';
                    if (expDate) {
                        var today = new Date();
                        var exp = new Date(expDate);
                        daysLeft = Math.ceil((exp - today) / (1000 * 60 * 60 * 24));
                    }
                    html += '<tr>';
                    html += '<td>' + (item.medicineName || '-') + '</td>';
                    html += '<td>' + (item.batchNo || '-') + '</td>';
                    html += '<td>' + (expDate || '-') + '</td>';
                    html += '<td><span style="background:#f39c12;color:#fff;padding:2px 8px;border-radius:10px;font-size:12px;">' + (daysLeft || '?') + '天</span></td>';
                    html += '</tr>';
                }
            }
            $('#nearExpiryTable').html(html);
        },
        error: function () {
            $('#nearExpiryTable').html('<tr><td colspan="4" class="empty-data">加载失败</td></tr>');
        }
    });
};

// ========== 新增预约 ==========
DoctorPortal.openAppointmentAdd = function () {
    $('#appointmentAddForm')[0].reset();
    $('#appointmentAddModal').modal('show');
};

DoctorPortal.submitAppointment = function () {
    var patientName = $('#addPatientName').val();
    var patientIdcard = $('#addPatientIdcard').val();
    var pointDate = $('#addPointDate').val();
    var pointPlace = $('#addPointPlace').val();

    if (!patientName || !pointDate || !pointPlace) {
        Feng.error('请填写完整信息！');
        return;
    }

    var data = {
        patientName: patientName,
        pointDate: pointDate,
        pointPlace: pointPlace
    };
    if (patientIdcard) {
        data.patientIdcard = patientIdcard;
    }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_appointment',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('添加成功！');
                $('#appointmentAddModal').modal('hide');
                DoctorPortal.loadAppointmentList();
                DoctorPortal.loadTodaySchedule();
            } else {
                Feng.error('添加失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('添加失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 编辑预约 ==========
DoctorPortal.openAppointmentEdit = function (id, patientName, patientIdcard, pointDate, pointPlace) {
    $('#editAppointmentId').val(id);
    $('#editPatientName').val(patientName);
    $('#editPatientIdcard').val(patientIdcard);
    // datetime-local 需要 YYYY-MM-DDTHH:mm 格式
    if (pointDate && pointDate !== '-') {
        var d = pointDate.replace(' ', 'T').substring(0, 16);
        $('#editPointDate').val(d);
    } else {
        $('#editPointDate').val('');
    }
    $('#editPointPlace').val(pointPlace);
    $('#appointmentEditModal').modal('show');
};

DoctorPortal.submitEditAppointment = function () {
    var id = $('#editAppointmentId').val();
    var patientName = $('#editPatientName').val();
    var pointDate = $('#editPointDate').val();
    var pointPlace = $('#editPointPlace').val();

    if (!id || !patientName || !pointDate || !pointPlace) {
        Feng.error('请填写完整信息！');
        return;
    }

    var data = {
        id: id,
        patientName: patientName,
        pointDate: pointDate,
        pointPlace: pointPlace
    };
    var patientIdcard = $('#editPatientIdcard').val();
    if (patientIdcard) data.patientIdcard = patientIdcard;

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/update_appointment',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('编辑成功！');
                $('#appointmentEditModal').modal('hide');
                DoctorPortal.loadAppointmentList();
                DoctorPortal.loadTodaySchedule();
            } else {
                Feng.error('编辑失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('编辑失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 新增健康记录 ==========
DoctorPortal.openHealthAdd = function () {
    $('#healthAddForm')[0].reset();
    $('#healthAddModal').modal('show');
};

DoctorPortal.submitHealth = function () {
    var patientName = $('#addHealthPatientName').val();
    var heartJump = $('#addHeartJump').val();
    var bloodPressure = $('#addBloodPressure').val();
    var bloodOx = $('#addBloodOx').val();
    var pulse = $('#addPulse').val();
    var date = $('#addHealthDate').val();

    if (!patientName || !heartJump || !bloodPressure || !bloodOx || !pulse || !date) {
        Feng.error('请填写完整信息！');
        return;
    }

    var data = {
        patientName: patientName,
        heartJump: heartJump,
        bloodPressure: bloodPressure,
        bloodOx: bloodOx,
        pulse: pulse,
        date: date
    };
    var patientIdcard = $('#addHealthPatientIdcard').val();
    if (patientIdcard) {
        data.patientIdcard = patientIdcard;
    }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_health',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('添加成功！');
                $('#healthAddModal').modal('hide');
                DoctorPortal.loadHealthList();
            } else {
                Feng.error('添加失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('添加失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 新增就诊记录 ==========
DoctorPortal.openHistoryAdd = function () {
    $('#historyAddForm')[0].reset();
    $('#historyAddModal').modal('show');
};

DoctorPortal.submitHistory = function () {
    var patientName = $('#addHistoryPatientName').val();
    var patientSym = $('#addPatientSym').val();
    var date = $('#addHistoryDate').val();

    if (!patientName || !patientSym || !date) {
        Feng.error('请填写完整信息！');
        return;
    }

    var data = {
        patientName: patientName,
        patientSym: patientSym,
        patientHistoryDate: date
    };
    var patientIdcard = $('#addHistoryPatientIdcard').val();
    var patientMedicine = $('#addPatientMedicine').val();
    var takeprice = $('#addTakeprice').val();
    if (patientIdcard) data.patientIdcard = patientIdcard;
    if (patientMedicine) data.patientMedicine = patientMedicine;
    if (takeprice) data.takeprice = takeprice;

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_history',
        type: 'POST',
        data: data,
        success: function (res) {
            if (res.code === 200) {
                Feng.success('添加成功！');
                $('#historyAddModal').modal('hide');
                DoctorPortal.loadHistoryList();
            } else {
                Feng.error('添加失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('添加失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 新增药品 ==========
DoctorPortal.openMedicineAdd = function () {
    $('#medicineAddForm')[0].reset();
    $('#addMedicineStock').val('0');
    $('#medicineAddModal').modal('show');
};

DoctorPortal.submitMedicine = function () {
    var medicineName = $('#addMedicineName').val();
    var medicinePrice = $('#addMedicinePrice').val();
    var medicineValue = $('#addMedicineValue').val();
    var medicineStock = $('#addMedicineStock').val();

    if (!medicineName || !medicinePrice || !medicineValue) {
        Feng.error('请填写完整信息！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/add_medicine',
        type: 'POST',
        data: {
            medicineName: medicineName,
            medicinePrice: medicinePrice,
            medicineValue: medicineValue,
            medicineStock: medicineStock
        },
        success: function (res) {
            if (res.code === 200) {
                Feng.success('添加成功！');
                $('#medicineAddModal').modal('hide');
                DoctorPortal.loadMedicineList();
            } else {
                Feng.error('添加失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('添加失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 编辑药品 ==========
DoctorPortal.openMedicineEdit = function (id, medicineName, medicinePrice, medicineValue, medicineStock) {
    $('#editMedicineId').val(id);
    $('#editMedicineName').val(medicineName);
    $('#editMedicinePrice').val(medicinePrice);
    $('#editMedicineValue').val(medicineValue);
    $('#editMedicineStock').val(medicineStock != null ? medicineStock : 0);
    $('#medicineEditModal').modal('show');
};

DoctorPortal.submitEditMedicine = function () {
    var id = $('#editMedicineId').val();
    var medicineName = $('#editMedicineName').val();
    var medicinePrice = $('#editMedicinePrice').val();
    var medicineValue = $('#editMedicineValue').val();
    var medicineStock = $('#editMedicineStock').val();

    if (!id || !medicineName || !medicinePrice || !medicineValue) {
        Feng.error('请填写完整信息！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/update_medicine',
        type: 'POST',
        data: {
            id: id,
            medicineName: medicineName,
            medicinePrice: medicinePrice,
            medicineValue: medicineValue,
            medicineStock: medicineStock
        },
        success: function (res) {
            if (res.code === 200) {
                Feng.success('编辑成功！');
                $('#medicineEditModal').modal('hide');
                DoctorPortal.loadMedicineList();
            } else {
                Feng.error('编辑失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('编辑失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ========== 删除操作 ==========
DoctorPortal.deleteAppointment = function (id) {
    layer.confirm('确定要删除该预约吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/delete_appointment',
            type: 'POST',
            data: {id: id},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('删除成功！');
                    DoctorPortal.loadAppointmentList();
                } else {
                    Feng.error('删除失败！');
                }
            },
            error: function () {
                Feng.error('删除失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

// ==================== 慢病管理 ====================


DoctorPortal.deleteHealth = function (id) {
    layer.confirm('确定要删除该健康记录吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/delete_health',
            type: 'POST',
            data: {id: id},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('删除成功！');
                    DoctorPortal.loadHealthList();
                } else {
                    Feng.error('删除失败！');
                }
            },
            error: function () {
                Feng.error('删除失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

// ==================== 慢病管理 ====================


DoctorPortal.deleteHistory = function (id) {
    layer.confirm('确定要删除该就诊记录吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/delete_history',
            type: 'POST',
            data: {id: id},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('删除成功！');
                    DoctorPortal.loadHistoryList();
                } else {
                    Feng.error('删除失败！');
                }
            },
            error: function () {
                Feng.error('删除失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

// ==================== 慢病管理 ====================


DoctorPortal.deleteMedicine = function (id) {
    layer.confirm('确定要删除该药品吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/delete_medicine',
            type: 'POST',
            data: {id: id},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('删除成功！');
                    DoctorPortal.loadMedicineList();
                } else {
                    Feng.error('删除失败！');
                }
            },
            error: function () {
                Feng.error('删除失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

// ==================== 慢病管理 ====================


/**
 * 获取健康状态标签
 */
DoctorPortal.getHealthStatus = function (item) {
    if (!item) return '<span class="status-tag info">未知</span>';
    var warnings = 0;
    if (item.heartJump < 60 || item.heartJump > 100) warnings++;
    if (item.bloodPressure < 90 || item.bloodPressure > 140) warnings++;
    if (item.bloodOx < 95) warnings++;

    if (warnings === 0) {
        return '<span class="status-tag success">正常</span>';
    } else if (warnings === 1) {
        return '<span class="status-tag warning">注意</span>';
    } else {
        return '<span class="status-tag danger">异常</span>';
    }
};

// ========== 就诊台 ==========

/**
 * 加载就诊台数据
 */
DoctorPortal.loadConsultationDesk = function () {
    // 加载统计
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/consultation_stats',
        type: 'POST',
        success: function (stats) {
            $('#consultWaitingCount').text(stats.waitingCount || 0);
            $('#consultInProgressCount').text(stats.inProgressCount || 0);
            $('#consultCompletedCount').text(stats.completedCount || 0);
        }
    });

    // 加载候诊队列
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/consultation_queue',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<div class="empty-data"><i class="fa fa-check-circle" style="font-size:36px;color:#11998e;"></i><p>暂无候诊患者</p></div>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var isMyPatient = item.doctorName === DoctorPortal.myDoctorName;
                    html += '<div class="consult-queue-item' + (isMyPatient ? '' : ' consult-queue-other') + '">';
                    html += '  <div class="consult-queue-number">' + (i + 1) + '</div>';
                    html += '  <div class="consult-queue-info">';
                    html += '    <h5>' + (item.patientName || '-') + '</h5>';
                    html += '    <p><i class="fa fa-user-md"></i> ' + (item.doctorName || '-') + '</p>';
                    html += '    <p><i class="fa fa-clock-o"></i> ' + (item.pointDate || '-') + '</p>';
                    html += '    <p><i class="fa fa-map-marker"></i> ' + (item.pointPlace || '-') + '</p>';
                    html += '  </div>';
                    if (isMyPatient) {
                        html += '  <button class="btn btn-sm btn-primary consult-queue-btn" onclick="DoctorPortal.startConsultation(' + item.id + ')"><i class="fa fa-play"></i> 接诊</button>';
                    } else {
                        html += '  <span class="consult-queue-waiting"><i class="fa fa-clock-o"></i> 等待' + (item.doctorName || '') + '接诊</span>';
                    }
                    html += '</div>';
                }
            }
            $('#waitingQueueBody').html(html);
        },
        error: function () {
            $('#waitingQueueBody').html('<div class="empty-data"><p>加载失败</p></div>');
        }
    });

    // 加载当前就诊中患者
    DoctorPortal.loadCurrentPatient();
};

/**
 * 加载当前就诊中患者
 */
DoctorPortal.loadCurrentPatient = function () {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/current_patient',
        type: 'POST',
        success: function (data) {
            if (data && data.id) {
                DoctorPortal.showCurrentPatient(data);
            } else {
                DoctorPortal.hideCurrentPatient();
            }
        },
        error: function () {
            DoctorPortal.hideCurrentPatient();
        }
    });
};

// ==================== 慢病管理 ====================


/**
 * 显示当前就诊患者
 */
DoctorPortal.showCurrentPatient = function (patient) {
    $('#currentPatientName').text(patient.patientName || '-');
    $('#currentPatientTime').text(patient.pointDate || '-');
    $('#currentPatientPlace').text(patient.pointPlace || '-');
    $('#consultAppointmentId').val(patient.id);
    // 重置表单
    $('#consultPatientSym').val('');
    $('#consultPatientMedicine').val('');
    $('#consultTakeprice').val('');
    // 显示/隐藏卡片
    $('#currentPatientCard').show();
    $('#noCurrentPatientCard').hide();
};

/**
 * 隐藏当前就诊患者
 */
DoctorPortal.hideCurrentPatient = function () {
    $('#currentPatientCard').hide();
    $('#noCurrentPatientCard').show();
};

/**
 * 接诊
 */
DoctorPortal.startConsultation = function (appointmentId) {
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/start_consultation',
        type: 'POST',
        data: {appointmentId: appointmentId},
        success: function (res) {
            if (res.code === 200) {
                Feng.success('已接诊，请填写诊断信息');
                DoctorPortal.loadConsultationDesk();
            } else {
                Feng.error('接诊失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('接诊失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


/**
 * 暂停就诊（将状态改回复诊中待参与）
 */
DoctorPortal.cancelConsultation = function () {
    var appointmentId = $('#consultAppointmentId').val();
    if (!appointmentId) return;

    layer.confirm('确定暂停当前就诊？患者将回到候诊队列', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/doctor_portal/update_appointment',
            type: 'POST',
            data: {id: appointmentId, status: 0},
            success: function (res) {
                if (res.code === 200) {
                    Feng.success('已暂停就诊');
                    DoctorPortal.loadConsultationDesk();
                }
            }
        });
        layer.close(index);
    });
};

// ==================== 慢病管理 ====================


/**
 * 完成就诊
 */
DoctorPortal.finishConsultation = function () {
    var appointmentId = $('#consultAppointmentId').val();
    var patientSym = $('#consultPatientSym').val();
    var patientMedicine = $('#consultPatientMedicine').val();
    var takeprice = $('#consultTakeprice').val();

    if (!appointmentId) {
        Feng.error('无就诊患者！');
        return;
    }
    if (!patientSym) {
        Feng.error('请填写症状诊断！');
        return;
    }
    if (!patientMedicine) {
        Feng.error('请填写用药处方！');
        return;
    }
    if (!takeprice) {
        Feng.error('请填写就诊费用！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/finish_consultation',
        type: 'POST',
        data: {
            appointmentId: appointmentId,
            patientSym: patientSym,
            patientMedicine: patientMedicine,
            takeprice: takeprice
        },
        success: function (res) {
            if (res.code === 200) {
                Feng.success('就诊完成！已生成就诊记录');
                DoctorPortal.loadConsultationDesk();
            } else {
                Feng.error('完成就诊失败！' + (res.message || ''));
            }
        },
        error: function () {
            Feng.error('完成就诊失败！服务器异常');
        }
    });
};

// ==================== 慢病管理 ====================


// ==================== 慢病管理 ====================


// ==================== 公共卫生 ====================
DoctorPortal.loadPublicHealthData = function () {
    this.loadPHStats();
    this.loadVaccinationList();
    this.loadMaternalList();
    this.loadElderlyCheckups();
    this.loadInfectiousReports();
};
DoctorPortal.loadPHStats = function () {
    $.post(Feng.ctxPath + '/doctor_portal/public_health_stats', function (d) {
        $('#phVaccCount2').text(d.vaccinationCount || 0);
        $('#phMaternalCount2').text(d.maternalCount || 0);
        $('#phElderlyCount2').text(d.elderlyCount || 0);
        $('#phInfectCount2').text(d.infectiousCount || 0);
    });
};

// ---- 预防接种 ----
DoctorPortal.loadVaccinationList = function () {
    $.post(Feng.ctxPath + '/doctor_portal/vaccination_list', function (d) {
        var h = '';
        if (d && d.length > 0) {
            for (var i = 0; i < Math.min(d.length, 10); i++) {
                h += '<tr><td>' + d[i].patientName + '</td><td>' + d[i].vaccineName + '</td><td>' + d[i].doseSeq + '</td><td>' + (d[i].vaccDate || '') + '</td><td>';
                h += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteVacc(' + d[i].id + ')"><i class="fa fa-trash"></i></button>';
                h += '</td></tr>';
            }
        }
        if (!h) h = '<tr><td colspan="5" class="text-muted text-center">暂无记录</td></tr>';
        $('#phVaccTable').html(h);
    });
};
DoctorPortal.openVaccAdd = function () {
    $('#vaccAddForm')[0].reset();
    $('#vaccAddModal').modal('show');
};
DoctorPortal.submitVacc = function () {
    var data = {
        patientName: $('#addVaccPatientName').val(),
        vaccineName: $('#addVaccName').val(),
        doseSeq: $('#addVaccDose').val(),
        vaccDate: $('#addVaccDate').val(),
        vaccSite: $('#addVaccSite').val(),
        batchNo: $('#addVaccBatch').val(),
        manufacturer: $('#addVaccManufacturer').val()
    };
    if (!data.patientName || !data.vaccineName || !data.vaccDate) { Feng.error('请填写必要信息！'); return; }
    $.ajax({
        url: Feng.ctxPath + '/vaccination/doAdd',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('添加成功！'); $('#vaccAddModal').modal('hide'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('添加失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('添加失败！服务器异常'); }
    });
};
DoctorPortal.deleteVacc = function (id) {
    layer.confirm('确定删除该接种记录？', {icon: 3, title: '提示'}, function (idx) {
        $.post(Feng.ctxPath + '/vaccination/delete', {id: id}, function (res) {
            if (res.code === 200) { Feng.success('删除成功！'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('删除失败！'); }
        });
        layer.close(idx);
    });
};
DoctorPortal.loadVaccSchedule = function () {
    $.post(Feng.ctxPath + '/vaccination/schedule', function (d) {
        var h = '';
        if (d && d.length > 0) {
            for (var i = 0; i < d.length; i++) {
                h += '<tr><td>' + (d[i].vaccineName || '-') + '</td><td>' + (d[i].suitableAge || '-') + '</td><td>' + (d[i].doses || '-') + '</td><td>' + (d[i].description || '-') + '</td></tr>';
            }
        }
        if (!h) h = '<tr><td colspan="4" class="text-muted text-center">暂无计划表数据</td></tr>';
        $('#vaccScheduleTable').html(h);
        $('#vaccScheduleModal').modal('show');
    });
};

// ---- 孕产妇保健 ----
DoctorPortal.loadMaternalList = function () {
    $.post(Feng.ctxPath + '/doctor_portal/maternal_list', function (d) {
        var h = '';
        if (d && d.length > 0) {
            for (var i = 0; i < Math.min(d.length, 10); i++) {
                h += '<tr><td>' + d[i].patientName + '</td><td>' + (d[i].eddDate || '') + '</td><td>' + (d[i].highRiskFlag === 1 ? '<span class="label label-danger">是</span>' : '<span class="label label-success">否</span>') + '</td><td>' + (d[i].doctorName || '') + '</td><td>';
                h += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteMaternal(' + d[i].id + ')"><i class="fa fa-trash"></i></button>';
                h += '</td></tr>';
            }
        }
        if (!h) h = '<tr><td colspan="5" class="text-muted text-center">暂无记录</td></tr>';
        $('#phMaternalTable').html(h);
    });
};
DoctorPortal.openMaternalAdd = function () {
    $('#maternalAddForm')[0].reset();
    $('#maternalAddModal').modal('show');
};
DoctorPortal.calcEDD = function () {
    var lmp = $('#addMaternalLmp').val();
    if (lmp) {
        var d = new Date(lmp);
        d.setDate(d.getDate() + 280);
        $('#addMaternalEdd').val(d.toISOString().substring(0, 10));
    }
};
DoctorPortal.submitMaternal = function () {
    var data = {
        patientName: $('#addMaternalName').val(),
        lmpDate: $('#addMaternalLmp').val(),
        eddDate: $('#addMaternalEdd').val(),
        gravidity: $('#addMaternalGravidity').val(),
        parity: $('#addMaternalParity').val(),
        bloodType: $('#addMaternalBloodType').val(),
        highRiskFlag: $('#addMaternalHighRisk').val(),
        patientIdcard: $('#addMaternalIdcard').val()
    };
    if (!data.patientName || !data.lmpDate) { Feng.error('请填写必要信息！'); return; }
    $.ajax({
        url: Feng.ctxPath + '/maternal/doAdd',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('建册成功！'); $('#maternalAddModal').modal('hide'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('建册失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('建册失败！服务器异常'); }
    });
};
DoctorPortal.deleteMaternal = function (id) {
    layer.confirm('确定删除该孕产妇档案？', {icon: 3, title: '提示'}, function (idx) {
        $.post(Feng.ctxPath + '/maternal/delete', {id: id}, function (res) {
            if (res.code === 200) { Feng.success('删除成功！'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('删除失败！'); }
        });
        layer.close(idx);
    });
};

// ---- 老年体检 ----
DoctorPortal.loadElderlyCheckups = function () {
    $.post(Feng.ctxPath + '/doctor_portal/elderly_checkups', function (d) {
        var h = '';
        if (d && d.length > 0) {
            for (var i = 0; i < Math.min(d.length, 10); i++) {
                h += '<tr><td>' + d[i].patientName + '</td><td>' + (d[i].checkupDate || '') + '</td><td>' + (d[i].bmi || '-') + '</td><td>' + (d[i].bloodPressure || '-') + '</td><td>' + (d[i].healthAssessment || '-') + '</td><td>';
                h += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteElderly(' + d[i].id + ')"><i class="fa fa-trash"></i></button>';
                h += '</td></tr>';
            }
        }
        if (!h) h = '<tr><td colspan="6" class="text-muted text-center">暂无记录</td></tr>';
        $('#phElderlyTable').html(h);
    });
};
DoctorPortal.openElderlyAdd = function () {
    $('#elderlyAddForm')[0].reset();
    $('#addElderlyBmi').val('');
    $('#elderlyAddModal').modal('show');
};
DoctorPortal.calcBMI = function () {
    var h = parseFloat($('#addElderlyHeight').val()) / 100;
    var w = parseFloat($('#addElderlyWeight').val());
    if (h > 0 && w > 0) {
        $('#addElderlyBmi').val((w / (h * h)).toFixed(1));
    }
};
DoctorPortal.submitElderly = function () {
    var data = {
        patientName: $('#addElderlyName').val(),
        checkupDate: $('#addElderlyDate').val(),
        height: $('#addElderlyHeight').val(),
        weight: $('#addElderlyWeight').val(),
        bmi: $('#addElderlyBmi').val(),
        bloodPressure: $('#addElderlyBp').val(),
        heartRate: $('#addElderlyHr').val(),
        bloodSugar: $('#addElderlyGlu').val(),
        healthAssessment: $('#addElderlyAssessment').val(),
        selfCareAssessment: $('#addElderlySelfCare').val(),
        advice: $('#addElderlyAdvice').val()
    };
    if (!data.patientName || !data.checkupDate) { Feng.error('请填写必要信息！'); return; }
    $.ajax({
        url: Feng.ctxPath + '/elderly/doAdd',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('添加成功！'); $('#elderlyAddModal').modal('hide'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('添加失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('添加失败！服务器异常'); }
    });
};
DoctorPortal.deleteElderly = function (id) {
    layer.confirm('确定删除该体检记录？', {icon: 3, title: '提示'}, function (idx) {
        $.post(Feng.ctxPath + '/elderly/delete', {id: id}, function (res) {
            if (res.code === 200) { Feng.success('删除成功！'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('删除失败！'); }
        });
        layer.close(idx);
    });
};

// ---- 传染病报告 ----
DoctorPortal.loadInfectiousReports = function () {
    $.post(Feng.ctxPath + '/doctor_portal/infectious_reports', function (d) {
        var h = '';
        if (d && d.length > 0) {
            for (var i = 0; i < Math.min(d.length, 10); i++) {
                h += '<tr><td>' + d[i].patientName + '</td><td>' + d[i].diseaseType + '</td><td>' + (d[i].diseaseCategory === '甲' ? '<span class="label label-danger">甲</span>' : d[i].diseaseCategory === '乙' ? '<span class="label label-warning">乙</span>' : '<span class="label label-success">丙</span>') + '</td><td>' + (d[i].reportDate || '') + '</td><td>' + (d[i].status === 1 ? '已报告' : '已审核') + '</td><td>';
                h += '<button class="btn btn-xs btn-danger" onclick="DoctorPortal.deleteInfect(' + d[i].id + ')"><i class="fa fa-trash"></i></button>';
                h += '</td></tr>';
            }
        }
        if (!h) h = '<tr><td colspan="6" class="text-muted text-center">暂无记录</td></tr>';
        $('#phInfectTable').html(h);
    });
};
DoctorPortal.openInfectAdd = function () {
    $('#infectAddForm')[0].reset();
    $('#infectAddModal').modal('show');
};
DoctorPortal.submitInfect = function () {
    var data = {
        patientName: $('#addInfectName').val(),
        diseaseType: $('#addInfectDisease').val(),
        diseaseCategory: $('#addInfectCategory').val(),
        onsetDate: $('#addInfectOnsetDate').val(),
        diagnosisDate: $('#addInfectDiagDate').val(),
        reportDate: $('#addInfectReportDate').val(),
        symptoms: $('#addInfectSymptoms').val(),
        isolationStatus: $('#addInfectIsolation').val(),
        closeContactsCount: $('#addInfectCloseContacts').val(),
        controlMeasures: $('#addInfectMeasures').val()
    };
    if (!data.patientName || !data.diseaseType || !data.reportDate) { Feng.error('请填写必要信息！'); return; }
    if (data.onsetDate && data.diagnosisDate && new Date(data.diagnosisDate) < new Date(data.onsetDate)) { Feng.error('诊断日期不能早于发病日期！'); return; }
    if (data.diagnosisDate && data.reportDate && new Date(data.reportDate) < new Date(data.diagnosisDate)) { Feng.error('报告日期不能早于诊断日期！'); return; }
    $.ajax({
        url: Feng.ctxPath + '/infectious/doAdd',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('报告成功！'); $('#infectAddModal').modal('hide'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('报告失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('报告失败！服务器异常'); }
    });
};
DoctorPortal.deleteInfect = function (id) {
    layer.confirm('确定删除该传染病报告？', {icon: 3, title: '提示'}, function (idx) {
        $.post(Feng.ctxPath + '/infectious/delete', {id: id}, function (res) {
            if (res.code === 200) { Feng.success('删除成功！'); DoctorPortal.loadPublicHealthData(); }
            else { Feng.error('删除失败！'); }
        });
        layer.close(idx);
    });
};

// ==================== 慢病档案编辑 ====================
DoctorPortal.openChronicEdit = function (id, patientName, patientIdcard, diseaseType, riskLevel, diagnosisDate, remark) {
    $('#editChronicId').val(id);
    $('#editChronicPatientName').val(patientName);
    $('#editChronicPatientIdcard').val(patientIdcard || '');
    $('#editChronicDiseaseType').val(diseaseType);
    $('#editChronicRiskLevel').val(riskLevel);
    $('#editChronicDiagnosisDate').val(diagnosisDate ? diagnosisDate.substring(0, 10) : '');
    $('#editChronicRemark').val(remark || '');
    $('#chronicEditModal').modal('show');
};
DoctorPortal.submitEditChronic = function () {
    var data = {
        id: $('#editChronicId').val(),
        patientName: $('#editChronicPatientName').val(),
        patientIdcard: $('#editChronicPatientIdcard').val(),
        diseaseType: $('#editChronicDiseaseType').val(),
        riskLevel: $('#editChronicRiskLevel').val(),
        diagnosisDate: $('#editChronicDiagnosisDate').val(),
        remark: $('#editChronicRemark').val()
    };
    if (!data.patientName) { Feng.error('请输入患者姓名'); return; }
    $.ajax({
        url: Feng.ctxPath + '/doctor_portal/update_chronic',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('编辑成功！'); $('#chronicEditModal').modal('hide'); DoctorPortal.loadChronicList(); }
            else { Feng.error('编辑失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('编辑失败！服务器异常'); }
    });
};

// ==================== 风险评估引擎 ====================
DoctorPortal.openRiskAssess = function () {
    $('#riskAssessForm')[0].reset();
    $('#assessResult').val('');
    DoctorPortal.showAssessFields();
    $('#riskAssessModal').modal('show');
};
DoctorPortal.showAssessFields = function () {
    var diseaseType = $('#assessDiseaseType').val();
    var fieldsHtml = '';
    if (diseaseType === '高血压') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">收缩压(mmHg)</label><div class="col-sm-8"><input type="number" class="form-control" id="assessSbp" placeholder="如 160"></div></div>'
            + '<div class="form-group"><label class="col-sm-3 control-label">舒张压(mmHg)</label><div class="col-sm-8"><input type="number" class="form-control" id="assessDbp" placeholder="如 100"></div></div>';
    } else if (diseaseType === '糖尿病') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">空腹血糖(mmol/L)</label><div class="col-sm-8"><input type="text" class="form-control" id="assessFbg" placeholder="如 7.8"></div></div>'
            + '<div class="form-group"><label class="col-sm-3 control-label">HbA1c(%)</label><div class="col-sm-8"><input type="text" class="form-control" id="assessHba1c" placeholder="如 8.5"></div></div>';
    } else if (diseaseType === '冠心病') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">NYHA分级</label><div class="col-sm-8"><select class="form-control" id="assessNyha"><option value="I">I</option><option value="II">II</option><option value="III">III</option><option value="IV">IV</option></select></div></div>'
            + '<div class="form-group"><label class="col-sm-3 control-label">ACS史</label><div class="col-sm-8"><select class="form-control" id="assessAcs"><option value="0">无</option><option value="1">有</option></select></div></div>';
    } else if (diseaseType === '脑卒中') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">NIHSS评分</label><div class="col-sm-8"><input type="number" class="form-control" id="assessNihss" placeholder="0-42"></div></div>';
    } else if (diseaseType === '慢阻肺') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">FEV1%预计值</label><div class="col-sm-8"><input type="text" class="form-control" id="assessFev1" placeholder="如 60"></div></div>';
    } else if (diseaseType === '慢性肾病') {
        fieldsHtml = '<div class="form-group"><label class="col-sm-3 control-label">eGFR</label><div class="col-sm-8"><input type="text" class="form-control" id="assessEgfr" placeholder="如 45"></div></div>'
            + '<div class="form-group"><label class="col-sm-3 control-label">蛋白尿</label><div class="col-sm-8"><select class="form-control" id="assessProteinuria"><option value="0">无</option><option value="1">微量</option><option value="2">大量</option></select></div></div>';
    }
    $('#assessFields').html(fieldsHtml);
};
DoctorPortal.doRiskAssess = function () {
    var diseaseType = $('#assessDiseaseType').val();
    var result = '';
    if (diseaseType === '高血压') {
        var sbp = parseInt($('#assessSbp').val()) || 0;
        var dbp = parseInt($('#assessDbp').val()) || 0;
        if (sbp >= 180 || dbp >= 110) result = '高风险';
        else if (sbp >= 160 || dbp >= 100) result = '中风险';
        else if (sbp >= 140 || dbp >= 90) result = '低风险';
        else result = '正常';
    } else if (diseaseType === '糖尿病') {
        var fbg = parseFloat($('#assessFbg').val()) || 0;
        var hba1c = parseFloat($('#assessHba1c').val()) || 0;
        if (fbg >= 11.1 || hba1c >= 9) result = '高风险';
        else if (fbg >= 7.0 || hba1c >= 7) result = '中风险';
        else result = '低风险';
    } else if (diseaseType === '冠心病') {
        var nyha = $('#assessNyha').val();
        var acs = $('#assessAcs').val();
        if (nyha === 'IV' || acs === '1') result = '高风险';
        else if (nyha === 'III') result = '中风险';
        else result = '低风险';
    } else if (diseaseType === '脑卒中') {
        var nihss = parseInt($('#assessNihss').val()) || 0;
        if (nihss >= 16) result = '高风险';
        else if (nihss >= 5) result = '中风险';
        else result = '低风险';
    } else if (diseaseType === '慢阻肺') {
        var fev1 = parseFloat($('#assessFev1').val()) || 0;
        if (fev1 < 50) result = '高风险';
        else if (fev1 < 80) result = '中风险';
        else result = '低风险';
    } else if (diseaseType === '慢性肾病') {
        var egfr = parseFloat($('#assessEgfr').val()) || 0;
        var pro = $('#assessProteinuria').val();
        if (egfr < 30 || pro === '2') result = '高风险';
        else if (egfr < 60 || pro === '1') result = '中风险';
        else result = '低风险';
    }
    var color = result === '高风险' ? '#e74c3c' : result === '中风险' ? '#f39c12' : '#27ae60';
    $('#assessResult').val(result).css('color', color);
};

// ==================== 待办提醒 ====================
DoctorPortal.loadPendingReminders = function () {
    $.ajax({
        url: Feng.ctxPath + '/chronicDisease/pendingReminders',
        type: 'POST',
        success: function (data) {
            var count = data ? data.length : 0;
            $('#chronicPendingFollowup').text(count);
        }
    });
};

// ==================== 健康记录编辑 ====================
DoctorPortal.openHealthEdit = function (id, heartJump, bloodPressure, bloodOx, pulse, date) {
    $('#editHealthId').val(id);
    $('#editHeartJump').val(heartJump);
    $('#editBloodPressure').val(bloodPressure);
    $('#editBloodOx').val(bloodOx);
    $('#editPulse').val(pulse);
    if (date && date !== '-') {
        $('#editHealthDate').val(date.replace(' ', 'T').substring(0, 16));
    } else {
        $('#editHealthDate').val('');
    }
    $('#healthEditModal').modal('show');
};
DoctorPortal.submitEditHealth = function () {
    var data = {
        id: $('#editHealthId').val(),
        heartJump: $('#editHeartJump').val(),
        bloodPressure: $('#editBloodPressure').val(),
        bloodOx: $('#editBloodOx').val(),
        pulse: $('#editPulse').val(),
        date: $('#editHealthDate').val()
    };
    if (!data.heartJump || !data.bloodPressure || !data.bloodOx || !data.pulse || !data.date) {
        Feng.error('请填写完整信息！'); return;
    }
    $.ajax({
        url: Feng.ctxPath + '/patientHealth/update',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('编辑成功！'); $('#healthEditModal').modal('hide'); DoctorPortal.loadHealthList(); }
            else { Feng.error('编辑失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('编辑失败！服务器异常'); }
    });
};

// ==================== 就诊记录编辑 ====================
DoctorPortal.openHistoryEdit = function (id, patientSym, patientMedicine, takeprice, patientHistoryDate) {
    $('#editHistoryId').val(id);
    $('#editPatientSym').val(patientSym);
    $('#editPatientMedicine').val(patientMedicine);
    $('#editTakeprice').val(takeprice);
    if (patientHistoryDate && patientHistoryDate !== '-') {
        $('#editHistoryDate').val(patientHistoryDate.replace(' ', 'T').substring(0, 16));
    } else {
        $('#editHistoryDate').val('');
    }
    $('#historyEditModal').modal('show');
};
DoctorPortal.submitEditHistory = function () {
    var data = {
        id: $('#editHistoryId').val(),
        patientSym: $('#editPatientSym').val(),
        patientMedicine: $('#editPatientMedicine').val(),
        takeprice: $('#editTakeprice').val(),
        patientHistoryDate: $('#editHistoryDate').val()
    };
    if (!data.patientSym || !data.patientHistoryDate) {
        Feng.error('请填写症状和就诊时间！'); return;
    }
    $.ajax({
        url: Feng.ctxPath + '/patientHistory/update',
        type: 'POST', data: data,
        success: function (res) {
            if (res.code === 200) { Feng.success('编辑成功！'); $('#historyEditModal').modal('hide'); DoctorPortal.loadHistoryList(); }
            else { Feng.error('编辑失败：' + (res.message || '')); }
        },
        error: function () { Feng.error('编辑失败！服务器异常'); }
    });
};

// ==================== 通用分页工具 ====================
DoctorPortal.pageSize = 10;
DoctorPortal.currentPages = {};

DoctorPortal.renderPagination = function (containerId, totalCount, pageKey, loadFn) {
    var totalPages = Math.ceil(totalCount / DoctorPortal.pageSize);
    if (totalPages <= 1) { $('#' + containerId).html(''); return; }
    if (!DoctorPortal.currentPages[pageKey]) DoctorPortal.currentPages[pageKey] = 1;
    var current = DoctorPortal.currentPages[pageKey];
    var html = '<div class="pagination-info" style="display:inline-block;margin-right:10px;">共' + totalCount + '条，第' + current + '/' + totalPages + '页</div>';
    html += '<ul class="pagination pagination-sm" style="margin:0;">';
    html += '<li class="' + (current <= 1 ? 'disabled' : '') + '"><a href="javascript:void(0);" onclick="DoctorPortal.goPage(\'' + pageKey + '\',' + (current - 1) + ',\'' + loadFn + '\')">&laquo;</a></li>';
    for (var i = 1; i <= totalPages; i++) {
        html += '<li class="' + (i === current ? 'active' : '') + '"><a href="javascript:void(0);" onclick="DoctorPortal.goPage(\'' + pageKey + '\',' + i + ',\'' + loadFn + '\')">' + i + '</a></li>';
    }
    html += '<li class="' + (current >= totalPages ? 'disabled' : '') + '"><a href="javascript:void(0);" onclick="DoctorPortal.goPage(\'' + pageKey + '\',' + (current + 1) + ',\'' + loadFn + '\')">&raquo;</a></li>';
    html += '</ul>';
    $('#' + containerId).html(html);
};

DoctorPortal.goPage = function (pageKey, page, loadFn) {
    DoctorPortal.currentPages[pageKey] = page;
    DoctorPortal[loadFn]();
};

// ==================== 搜索/筛选 ====================
DoctorPortal.searchAppointments = function () {
    var keyword = ($('#apptSearchInput').val() || '').toLowerCase();
    $('#appointmentTable tr').each(function () {
        if ($(this).find('.empty-data').length > 0) return;
        var text = $(this).text().toLowerCase();
        $(this).toggle(text.indexOf(keyword) > -1);
    });
};
DoctorPortal.searchHealth = function () {
    var keyword = ($('#healthSearchInput').val() || '').toLowerCase();
    $('#healthTable tr').each(function () {
        if ($(this).find('.empty-data').length > 0) return;
        var text = $(this).text().toLowerCase();
        $(this).toggle(text.indexOf(keyword) > -1);
    });
};
DoctorPortal.searchHistory = function () {
    var keyword = ($('#historySearchInput').val() || '').toLowerCase();
    $('#historyTable tr').each(function () {
        if ($(this).find('.empty-data').length > 0) return;
        var text = $(this).text().toLowerCase();
        $(this).toggle(text.indexOf(keyword) > -1);
    });
};

/**
 * 导出单条就诊记录为Excel
 */
DoctorPortal.exportHistoryOne = function (id) {
    window.open(Feng.ctxPath + "/patientHistory/export/" + id);
};

/**
 * 按身份证号批量导出就诊记录
 */
DoctorPortal.exportHistoryByIdcard = function () {
    var idcard = $("#doctorExportIdcard").val();
    if (!idcard) {
        Feng.info("请输入患者身份证号");
        return;
    }
    window.open(Feng.ctxPath + "/patientHistory/exportByPatientIdcard?patientIdcard=" + encodeURIComponent(idcard));
};

/**
 * 打印单条就诊记录
 */
DoctorPortal.printHistory = function (id) {
    var url = Feng.ctxPath + "/patientHistory/print/" + id;
    var w = window.open(url, '_blank', 'width=800,height=600,scrollbars=yes');
};
