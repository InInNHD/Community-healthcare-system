/**
 * 居民端门户
 */
var PatientPortal = {
    currentTab: 'dashboard',
    allMedicines: [],
    currentCategory: 'all',
    allDoctors: [],
    currentDeptCategory: 'all',
    selectedDoctor: null,
    currentAppointTab: 'upcoming',
    allAppointments: []
};

/**
 * 初始化
 */
PatientPortal.init = function () {
    this.loadDashboardHealth();
    this.loadDashboardAppointments();
};

/**
 * 切换Tab
 */
PatientPortal.switchTab = function (tabName) {
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
        this.loadDashboardHealth();
        this.loadDashboardAppointments();
    } else if (tabName === 'consultation') {
        this.loadConsultationDesk();
    } else if (tabName === 'health') {
        this.loadHealthList();
    } else if (tabName === 'appointments') {
        this.loadDoctorList();
        this.loadAppointmentList();
    } else if (tabName === 'history') {
        this.loadHistoryList();
    } else if (tabName === 'medicine') {
        this.loadMedicineList();
    } else if (tabName === 'chronic') {
        this.loadMyChronicList();
        this.loadMyChronicPlans();
        this.loadMyChronicFollowups();
    } else if (tabName === 'public_health') {
        this.loadMyVaccinations();
        this.loadMyElderlyCheckups();
    }
};

/**
 * 加载首页健康数据摘要
 */
PatientPortal.loadDashboardHealth = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_health',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<div class="empty-data"><i class="fa fa-heartbeat" style="font-size:36px;"></i><p>暂无健康记录</p></div>';
            } else {
                var list = data.slice(0, 3);
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    html += '<div class="health-indicator">';
                    html += '  <span class="indicator-label">心跳</span>';
                    html += '  <div class="indicator-bar"><div class="bar-fill" style="width:' + Math.min(item.heartJump / 1.5, 100) + '%;background:#36d1dc;"></div></div>';
                    html += '  <span class="indicator-value">' + item.heartJump + ' 次/分</span>';
                    html += '</div>';
                    html += '<div class="health-indicator">';
                    html += '  <span class="indicator-label">血压</span>';
                    html += '  <div class="indicator-bar"><div class="bar-fill" style="width:' + Math.min(item.bloodPressure / 2, 100) + '%;background:#1a9bfc;"></div></div>';
                    html += '  <span class="indicator-value">' + item.bloodPressure + ' mmHg</span>';
                    html += '</div>';
                    html += '<div class="health-indicator">';
                    html += '  <span class="indicator-label">血氧</span>';
                    html += '  <div class="indicator-bar"><div class="bar-fill" style="width:' + item.bloodOx + '%;background:#11998e;"></div></div>';
                    html += '  <span class="indicator-value">' + item.bloodOx + ' %</span>';
                    html += '</div>';
                    html += '<p style="font-size:12px;color:#999;margin-top:4px;">检测时间：' + (item.date || '-') + '</p>';
                    if (i < list.length - 1) {
                        html += '<hr style="margin:8px 0;border-color:#f0f0f0;">';
                    }
                }
            }
            $('#dashboardHealth').html(html);
        },
        error: function () {
            $('#dashboardHealth').html('<div class="empty-data"><p>加载失败</p></div>');
        }
    });
};

/**
 * 加载首页预约摘要
 */
PatientPortal.loadDashboardAppointments = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_appointments',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="3" class="empty-data">暂无预约记录</td></tr>';
            } else {
                var list = data.slice(0, 5);
                for (var i = 0; i < list.length; i++) {
                    var item = list[i];
                    html += '<tr>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + (item.pointDate || '-') + '</td>';
                    html += '<td>' + (item.pointPlace || '-') + '</td>';
                    html += '</tr>';
                }
            }
            $('#dashboardAppointments').html(html);
        },
        error: function () {
            $('#dashboardAppointments').html('<tr><td colspan="3" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 加载健康档案列表
 */
PatientPortal.loadHealthList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_health',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="7" class="empty-data">暂无健康记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var statusTag = PatientPortal.getHealthStatus(item);
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.heartJump || '-') + '</td>';
                    html += '<td>' + (item.bloodPressure || '-') + '</td>';
                    html += '<td>' + (item.bloodOx || '-') + '</td>';
                    html += '<td>' + (item.pulse || '-') + '</td>';
                    html += '<td>' + (item.date || '-') + '</td>';
                    html += '<td>' + statusTag + '</td>';
                    html += '</tr>';
                }
            }
            $('#healthTable').html(html);
        },
        error: function () {
            $('#healthTable').html('<tr><td colspan="7" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 加载预约列表（分类：待参与/已完成/逾期）
 */
PatientPortal.loadAppointmentList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_appointments',
        type: 'POST',
        success: function (data) {
            PatientPortal.allAppointments = data || [];
            PatientPortal.renderAppointmentCategories();
        },
        error: function () {
            PatientPortal.allAppointments = [];
            PatientPortal.renderAppointmentCategories();
        }
    });
};

/**
 * 按状态分类预约记录并渲染
 */
PatientPortal.renderAppointmentCategories = function () {
    var data = PatientPortal.allAppointments;
    var upcoming = [], completed = [], overdue = [];

    for (var i = 0; i < data.length; i++) {
        var item = data[i];
        var status = item.status;
        if (status === 1) {
            completed.push(item);
        } else if (status === 2) {
            overdue.push(item);
        } else {
            // status === 0 或 null → 待参与
            upcoming.push(item);
        }
    }

    // 更新计数
    $('#upcomingCount').text(upcoming.length);
    $('#completedCount').text(completed.length);
    $('#overdueCount').text(overdue.length);

    // 渲染待参与
    PatientPortal.renderAppointTable('upcomingTable', upcoming, 'upcoming');
    // 渲染已完成
    PatientPortal.renderAppointTable('completedTable', completed, 'completed');
    // 渲染逾期
    PatientPortal.renderAppointTable('overdueTable', overdue, 'overdue');
};

/**
 * 渲染某个分类的预约表格
 */
PatientPortal.renderAppointTable = function (tableId, list, category) {
    var html = '';
    if (list.length === 0) {
        var emptyMsg = category === 'upcoming' ? '暂无待参与的会诊' :
                       category === 'completed' ? '暂无已完成的会诊' : '暂无逾期会诊';
        var emptyIcon = category === 'upcoming' ? 'fa-clock-o' :
                        category === 'completed' ? 'fa-check-circle' : 'fa-exclamation-circle';
        html = '<tr><td colspan="5" class="empty-data"><i class="fa ' + emptyIcon + '" style="font-size:36px;"></i><p>' + emptyMsg + '</p></td></tr>';
    } else {
        for (var i = 0; i < list.length; i++) {
            var item = list[i];
            html += '<tr>';
            html += '<td>' + (item.id || '-') + '</td>';
            html += '<td>' + (item.doctorName || '-') + '</td>';
            html += '<td>' + (item.pointDate || '-') + '</td>';
            html += '<td>' + (item.pointPlace || '-') + '</td>';

            if (category === 'upcoming') {
                html += '<td>';
                html += '<button class="btn btn-xs btn-primary" onclick="PatientPortal.openAppointmentEdit(' + item.id + ',\'' + (item.doctorName || '').replace(/'/g, "\\'") + '\',\'' + (item.pointDate || '').replace(/'/g, "\\'") + '\',\'' + (item.pointPlace || '').replace(/'/g, "\\'") + '\')"><i class="fa fa-edit"></i> 编辑</button> ';
                html += '<button class="btn btn-xs btn-danger" onclick="PatientPortal.deleteAppointment(' + item.id + ')"><i class="fa fa-trash"></i> 取消</button>';
                html += '</td>';
            } else if (category === 'completed') {
                html += '<td><span class="status-tag success"><i class="fa fa-check"></i> 已完成</span></td>';
            } else if (category === 'overdue') {
                html += '<td>';
                html += '<span class="status-tag danger" style="margin-right:6px;"><i class="fa fa-exclamation-triangle"></i> 逾期</span>';
                html += '<button class="btn btn-xs btn-success" onclick="PatientPortal.completeAppointment(' + item.id + ')"><i class="fa fa-check"></i> 补登完成</button> ';
                html += '<button class="btn btn-xs btn-danger" onclick="PatientPortal.deleteAppointment(' + item.id + ')"><i class="fa fa-trash"></i> 删除</button>';
                html += '</td>';
            }
            html += '</tr>';
        }
    }
    $('#' + tableId).html(html);
};

/**
 * 切换预约状态标签
 */
PatientPortal.switchAppointTab = function (tab) {
    PatientPortal.currentAppointTab = tab;
    // 更新标签高亮
    $('.appoint-status-tab').removeClass('active');
    $('.appoint-status-tab[data-status="' + tab + '"]').addClass('active');
    // 显示对应面板
    $('.appoint-status-panel').hide();
    $('#panel-' + tab).show();
};

/**
 * 完成预约（标记为已完成）
 */
PatientPortal.completeAppointment = function (id) {
    layer.confirm('确定将该预约标记为已完成吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/patient_portal/complete_appointment',
            type: 'POST',
            data: {id: id},
            success: function (data) {
                if (data.code === 200) {
                    Feng.success('已标记为完成！');
                    PatientPortal.loadAppointmentList();
                    PatientPortal.loadDashboardAppointments();
                } else {
                    Feng.error('操作失败！');
                }
            },
            error: function () {
                Feng.error('操作失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

/**
 * 加载就诊历史列表
 */
PatientPortal.loadHistoryList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_histories',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="7" class="empty-data">暂无就诊记录</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    html += '<tr>';
                    html += '<td>' + (item.id || '-') + '</td>';
                    html += '<td>' + (item.patientSym || '-') + '</td>';
                    html += '<td>' + (item.patientDoctor || '-') + '</td>';
                    html += '<td>' + (item.patientMedicine || '-') + '</td>';
                    html += '<td>' + (item.takeprice || '-') + '</td>';
                    html += '<td>' + (item.patientHistoryDate || '-') + '</td>';
                    html += '<td>';
                    html += '<button class="btn btn-xs btn-info" onclick="PatientPortal.exportHistoryOne(' + item.id + ')"><i class="fa fa-download"></i> 导出</button> ';
                    html += '<button class="btn btn-xs btn-warning" onclick="PatientPortal.printHistory(' + item.id + ')"><i class="fa fa-print"></i> 打印</button>';
                    html += '</td>';
                    html += '</tr>';
                }
            }
            $('#historyTable').html(html);
        },
        error: function () {
            $('#historyTable').html('<tr><td colspan="6" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 加载药品列表
 */
PatientPortal.loadMedicineList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/medicines',
        type: 'POST',
        success: function (data) {
            PatientPortal.allMedicines = data || [];
            PatientPortal.renderCategoryBar();
            PatientPortal.renderMedicineCards();
        },
        error: function () {
            $('#medicineCardGrid').html('<div class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败</p></div>');
        }
    });
};

/**
 * 渲染分类筛选栏
 */
PatientPortal.renderCategoryBar = function () {
    var categories = ['all'];
    var catSet = {};
    for (var i = 0; i < PatientPortal.allMedicines.length; i++) {
        var cat = PatientPortal.allMedicines[i].medicineCategory;
        if (cat && !catSet[cat]) {
            catSet[cat] = true;
            categories.push(cat);
        }
    }
    var html = '';
    for (var j = 0; j < categories.length; j++) {
        var c = categories[j];
        var label = c === 'all' ? '全部' : c;
        var activeClass = (c === PatientPortal.currentCategory) ? ' active' : '';
        html += '<span class="category-tag' + activeClass + '" onclick="PatientPortal.filterByCategory(\'' + c + '\')">' + label + '</span>';
    }
    $('#medicineCategoryBar').html(html);
};

/**
 * 按分类筛选
 */
PatientPortal.filterByCategory = function (category) {
    PatientPortal.currentCategory = category;
    PatientPortal.renderCategoryBar();
    PatientPortal.renderMedicineCards();
};

/**
 * 搜索过滤药品
 */
PatientPortal.filterMedicines = function () {
    PatientPortal.renderMedicineCards();
};

/**
 * 渲染药品卡片
 */
PatientPortal.renderMedicineCards = function () {
    var data = PatientPortal.allMedicines;
    var category = PatientPortal.currentCategory;
    var searchText = ($('#medicineSearchInput').val() || '').trim().toLowerCase();

    var filtered = [];
    for (var i = 0; i < data.length; i++) {
        var item = data[i];
        var matchCat = (category === 'all') || (item.medicineCategory === category);
        var matchSearch = !searchText || (item.medicineName || '').toLowerCase().indexOf(searchText) > -1;
        if (matchCat && matchSearch) {
            filtered.push(item);
        }
    }

    var html = '';
    if (filtered.length === 0) {
        html = '<div class="empty-data"><i class="fa fa-medkit" style="font-size:48px;"></i><p>暂无药品信息</p></div>';
    } else {
        for (var j = 0; j < filtered.length; j++) {
            var med = filtered[j];
            var imgSrc = med.medicineImage || '/static/img/medicine/vitamin.svg';
            var catLabel = med.medicineCategory || '未分类';
            html += '<div class="col-xs-6 col-sm-4 col-md-3">';
            html += '  <div class="medicine-card">';
            html += '    <div class="medicine-card-img">';
            html += '      <img src="' + Feng.ctxPath + imgSrc + '" alt="' + (med.medicineName || '') + '">';
            html += '    </div>';
            html += '    <div class="medicine-card-body">';
            html += '      <h5 class="medicine-card-title">' + (med.medicineName || '-') + '</h5>';
            html += '      <span class="medicine-card-category">' + catLabel + '</span>';
            html += '      <p class="medicine-card-desc">' + (med.medicineValue || '-') + '</p>';
            html += '      <div class="medicine-card-footer">';
            html += '        <span class="medicine-card-price">&yen;' + (med.medicinePrice || 0) + '</span>';
            html += '        ' + PatientPortal.renderStockBadge(med.medicineStock);
            html += '      </div>';
            html += '    </div>';
            html += '  </div>';
            html += '</div>';
        }
    }
    $('#medicineCardGrid').html(html);
};

/**
 * 渲染库存余量标签
 */
PatientPortal.renderStockBadge = function (stock) {
    var s = stock != null ? stock : 0;
    if (s > 50) {
        return '<span class="stock-badge stock-sufficient">库存：' + s + '</span>';
    } else if (s > 10) {
        return '<span class="stock-badge stock-warning">库存：' + s + '</span>';
    } else if (s > 0) {
        return '<span class="stock-badge stock-low">仅剩：' + s + '</span>';
    } else {
        return '<span class="stock-badge stock-out">已售罄</span>';
    }
};

/**
 * 打开新增预约（从首页快捷入口跳转）
 */
PatientPortal.openAppointmentAdd = function () {
    PatientPortal.switchTab('appointments');
};

/**
 * 加载医生列表
 */
PatientPortal.loadDoctorList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/doctors',
        type: 'POST',
        success: function (data) {
            PatientPortal.allDoctors = data || [];
            PatientPortal.renderDeptCategoryBar();
            PatientPortal.renderDoctorCards();
        },
        error: function () {
            $('#doctorCardGrid').html('<div class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载医生列表失败</p></div>');
        }
    });
};

/**
 * 渲染科室筛选栏
 */
PatientPortal.renderDeptCategoryBar = function () {
    var depts = ['all'];
    var deptSet = {};
    for (var i = 0; i < PatientPortal.allDoctors.length; i++) {
        var d = PatientPortal.allDoctors[i].department;
        if (d && !deptSet[d]) {
            deptSet[d] = true;
            depts.push(d);
        }
    }
    var html = '';
    for (var j = 0; j < depts.length; j++) {
        var dept = depts[j];
        var label = dept === 'all' ? '全部' : dept;
        var activeClass = (dept === PatientPortal.currentDeptCategory) ? ' active' : '';
        html += '<span class="category-tag' + activeClass + '" onclick="PatientPortal.filterByDept(\'' + dept + '\')">' + label + '</span>';
    }
    $('#deptCategoryBar').html(html);
};

/**
 * 按科室筛选
 */
PatientPortal.filterByDept = function (dept) {
    PatientPortal.currentDeptCategory = dept;
    PatientPortal.renderDeptCategoryBar();
    PatientPortal.renderDoctorCards();
};

/**
 * 搜索过滤医生
 */
PatientPortal.filterDoctors = function () {
    PatientPortal.renderDoctorCards();
};

/**
 * 科室配色方案
 */
PatientPortal.getDeptColor = function (dept) {
    var colors = {
        '内科': {bg: 'linear-gradient(135deg, #1a9bfc, #0d6efd)', tag: '#0d6efd'},
        '外科': {bg: 'linear-gradient(135deg, #eb3349, #f45c43)', tag: '#eb3349'},
        '妇科': {bg: 'linear-gradient(135deg, #e91e63, #f06292)', tag: '#e91e63'},
        '儿科': {bg: 'linear-gradient(135deg, #11998e, #38ef7d)', tag: '#11998e'},
        '中医科': {bg: 'linear-gradient(135deg, #f7971e, #ffd200)', tag: '#f7971e'},
        '口腔科': {bg: 'linear-gradient(135deg, #8e2de2, #4a00e0)', tag: '#8e2de2'},
        '眼科': {bg: 'linear-gradient(135deg, #36d1dc, #5b86e5)', tag: '#36d1dc'},
        '皮肤科': {bg: 'linear-gradient(135deg, #ff9a9e, #fad0c4)', tag: '#e91e63'},
        '心理科': {bg: 'linear-gradient(135deg, #667eea, #764ba2)', tag: '#667eea'}
    };
    return colors[dept] || {bg: 'linear-gradient(135deg, #5b86e5, #36d1dc)', tag: '#5b86e5'};
};

/**
 * 渲染医生卡片
 */
PatientPortal.renderDoctorCards = function () {
    var data = PatientPortal.allDoctors;
    var dept = PatientPortal.currentDeptCategory;
    var searchText = ($('#doctorSearchInput').val() || '').trim().toLowerCase();

    var filtered = [];
    for (var i = 0; i < data.length; i++) {
        var item = data[i];
        var matchDept = (dept === 'all') || (item.department === dept);
        var matchSearch = !searchText || (item.doctorName || '').toLowerCase().indexOf(searchText) > -1;
        if (matchDept && matchSearch) {
            filtered.push(item);
        }
    }

    var dayMap = {1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日'};
    var html = '';
    if (filtered.length === 0) {
        html = '<div class="empty-data"><i class="fa fa-user-md" style="font-size:48px;"></i><p>暂无医生信息</p></div>';
    } else {
        for (var j = 0; j < filtered.length; j++) {
            var doc = filtered[j];
            var color = PatientPortal.getDeptColor(doc.department);
            var days = (doc.workDays || '').split(',');
            var dayLabels = [];
            for (var d = 0; d < days.length; d++) {
                if (dayMap[parseInt(days[d])]) dayLabels.push(dayMap[parseInt(days[d])]);
            }
            var schedule = dayLabels.join('、') + ' ' + (doc.workTimeStart || '') + '-' + (doc.workTimeEnd || '');

            html += '<div class="col-xs-6 col-sm-4 col-md-3">';
            html += '  <div class="doctor-card" onclick="PatientPortal.openDoctorDetail(' + doc.id + ')">';
            html += '    <div class="doctor-card-avatar" style="background:' + color.bg + ';">';
            html += '      <i class="fa fa-user-md"></i>';
            html += '    </div>';
            html += '    <div class="doctor-card-body">';
            html += '      <h5 class="doctor-card-name">' + (doc.doctorName || '-') + '</h5>';
            html += '      <div class="doctor-card-tags">';
            html += '        <span class="doctor-card-dept" style="color:' + color.tag + ';background:' + color.tag + '15;">' + (doc.department || '-') + '</span>';
            html += '        <span class="doctor-card-title">' + (doc.title || '-') + '</span>';
            html += '      </div>';
            html += '      <p class="doctor-card-specialty">' + (doc.specialty || '-') + '</p>';
            html += '      <div class="doctor-card-schedule"><i class="fa fa-clock-o"></i> ' + schedule + '</div>';
            html += '      <button class="btn btn-sm btn-primary btn-block doctor-card-book"><i class="fa fa-calendar-plus-o"></i> 预约</button>';
            html += '    </div>';
            html += '  </div>';
            html += '</div>';
        }
    }
    $('#doctorCardGrid').html(html);
};

/**
 * 打开医生详情/预约模态框
 */
PatientPortal.openDoctorDetail = function (doctorId) {
    var doc = null;
    for (var i = 0; i < PatientPortal.allDoctors.length; i++) {
        if (PatientPortal.allDoctors[i].id === doctorId) {
            doc = PatientPortal.allDoctors[i];
            break;
        }
    }
    if (!doc) return;
    PatientPortal.selectedDoctor = doc;

    var color = PatientPortal.getDeptColor(doc.department);
    $('#detailDoctorAvatar').css('background', color.bg);
    $('#detailDoctorName').text(doc.doctorName || '-');
    $('#detailDoctorDept').text(doc.department || '-').css({color: color.tag, background: color.tag + '15'});
    $('#detailDoctorTitle').text(doc.title || '-');
    $('#detailDoctorSpecialty').text(doc.specialty || '-');
    $('#detailDoctorOffice').text(doc.office || '-');

    var dayMap = {1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日'};
    var days = (doc.workDays || '').split(',');
    var dayLabels = [];
    for (var d = 0; d < days.length; d++) {
        if (dayMap[parseInt(days[d])]) dayLabels.push(dayMap[parseInt(days[d])]);
    }
    $('#detailDoctorSchedule').text(dayLabels.join('、') + ' ' + (doc.workTimeStart || '') + '-' + (doc.workTimeEnd || ''));

    // 重置预约表单
    $('#addPointDate').val('');
    $('#addTimeSlots').html('<span class="time-slot-hint">请先选择日期</span>');
    $('#addPointTime').val('');
    $('#addPointPlace').val(doc.office || '');
    $('#addDoctorName').val(doc.doctorName || '');
    $('#addDoctorId').val(doc.id);

    $('#doctorDetailModal').modal('show');
};

/**
 * 日期变化 → 生成可预约时段
 */
PatientPortal.onDateChange = function () {
    var dateVal = $('#addPointDate').val();
    var doctor = PatientPortal.selectedDoctor;
    if (!dateVal || !doctor) {
        $('#addTimeSlots').html('<span class="time-slot-hint">请先选择日期</span>');
        $('#addPointTime').val('');
        return;
    }
    // 检查该日期是否是医生出诊日
    var selectedDate = new Date(dateVal);
    var dayOfWeek = selectedDate.getDay(); // 0=周日
    if (dayOfWeek === 0) dayOfWeek = 7; // 转为7
    var workDays = (doctor.workDays || '').split(',');
    var isWorkDay = false;
    for (var i = 0; i < workDays.length; i++) {
        if (parseInt(workDays[i]) === dayOfWeek) {
            isWorkDay = true;
            break;
        }
    }
    if (!isWorkDay) {
        var dayMap = {1: '周一', 2: '周二', 3: '周三', 4: '周四', 5: '周五', 6: '周六', 7: '周日'};
        var dayLabels = [];
        for (var d = 0; d < workDays.length; d++) {
            if (dayMap[parseInt(workDays[d])]) dayLabels.push(dayMap[parseInt(workDays[d])]);
        }
        $('#addTimeSlots').html('<span class="time-slot-hint" style="color:#eb3349;"><i class="fa fa-times-circle"></i> 该医生' + dayMap[dayOfWeek] + '不出诊，出诊日：' + dayLabels.join('、') + '</span>');
        $('#addPointTime').val('');
        return;
    }
    // 生成时段
    var startHour = parseInt((doctor.workTimeStart || '08:00').split(':')[0]);
    var startMin = parseInt((doctor.workTimeStart || '08:00').split(':')[1]);
    var endHour = parseInt((doctor.workTimeEnd || '17:00').split(':')[0]);
    var endMin = parseInt((doctor.workTimeEnd || '17:00').split(':')[1]);
    var slots = [];
    var h = startHour;
    var m = startMin;
    while (h < endHour || (h === endHour && m < endMin)) {
        var slotStart = (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m;
        // 下一个时段
        m += 30;
        if (m >= 60) { m -= 60; h++; }
        if (h > endHour || (h === endHour && m > endMin)) break;
        var slotEnd = (h < 10 ? '0' : '') + h + ':' + (m < 10 ? '0' : '') + m;
        slots.push({start: slotStart, end: slotEnd, label: slotStart + ' - ' + slotEnd});
    }

    // 查询该医生在该日期已被预约的时段
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/booked_slots',
        type: 'POST',
        data: {
            doctorName: doctor.doctorName,
            date: dateVal
        },
        success: function (bookedSlots) {
            var html = '';
            if (slots.length === 0) {
                html = '<span class="time-slot-hint" style="color:#eb3349;">该时段无可预约时间</span>';
            } else {
                for (var s = 0; s < slots.length; s++) {
                    var isBooked = false;
                    if (bookedSlots && bookedSlots.length > 0) {
                        for (var b = 0; b < bookedSlots.length; b++) {
                            if (bookedSlots[b] === slots[s].start) {
                                isBooked = true;
                                break;
                            }
                        }
                    }
                    if (isBooked) {
                        html += '<span class="time-slot time-slot-booked" title="该时段已被预约">' + slots[s].label + '</span>';
                    } else {
                        html += '<span class="time-slot" onclick="PatientPortal.selectTimeSlot(this, \'' + slots[s].start + '\')">' + slots[s].label + '</span>';
                    }
                }
            }
            $('#addTimeSlots').html(html);
            $('#addPointTime').val('');
        },
        error: function () {
            // 查询失败时仍展示所有时段（不阻塞）
            var html = '';
            if (slots.length === 0) {
                html = '<span class="time-slot-hint" style="color:#eb3349;">该时段无可预约时间</span>';
            } else {
                for (var s = 0; s < slots.length; s++) {
                    html += '<span class="time-slot" onclick="PatientPortal.selectTimeSlot(this, \'' + slots[s].start + '\')">' + slots[s].label + '</span>';
                }
            }
            $('#addTimeSlots').html(html);
            $('#addPointTime').val('');
        }
    });
};

/**
 * 选择时段
 */
PatientPortal.selectTimeSlot = function (el, time) {
    $('.time-slot').removeClass('selected');
    $(el).addClass('selected');
    var dateVal = $('#addPointDate').val();
    $('#addPointTime').val(dateVal + 'T' + time);
};

/**
 * 提交新增预约
 */
PatientPortal.submitAppointment = function () {
    var doctorId = $('#addDoctorId').val();
    var pointTime = $('#addPointTime').val();
    var pointPlace = $('#addPointPlace').val();
    var doctorName = $('#addDoctorName').val();

    if (!doctorId) {
        Feng.error('请选择医生！');
        return;
    }
    if (!pointTime) {
        Feng.error('请选择预约时段！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/patient_portal/add_appointment',
        type: 'POST',
        data: {
            doctorId: doctorId,
            doctorName: doctorName,
            pointDate: pointTime,
            pointPlace: pointPlace
        },
        success: function (data) {
            if (data.code === 200) {
                Feng.success('预约成功！');
                $('#doctorDetailModal').modal('hide');
                PatientPortal.loadAppointmentList();
                PatientPortal.loadDashboardAppointments();
            } else {
                Feng.error('预约失败！' + (data.message || ''));
            }
        },
        error: function () {
            Feng.error('预约失败！服务器异常');
        }
    });
};

/**
 * 打开编辑预约模态框
 */
PatientPortal.openAppointmentEdit = function (id, doctorName, pointDate, pointPlace) {
    $('#editAppointmentId').val(id);
    $('#editDoctorName').val(doctorName);
    if (pointDate && pointDate !== '-') {
        var d = pointDate.replace(' ', 'T').substring(0, 16);
        $('#editPointDate').val(d);
    } else {
        $('#editPointDate').val('');
    }
    $('#editPointPlace').val(pointPlace);
    $('#appointmentEditModal').modal('show');
};

/**
 * 提交编辑预约
 */
PatientPortal.submitEditAppointment = function () {
    var id = $('#editAppointmentId').val();
    var doctorName = $('#editDoctorName').val();
    var pointDate = $('#editPointDate').val();
    var pointPlace = $('#editPointPlace').val();

    if (!id || !doctorName || !pointDate || !pointPlace) {
        Feng.error('请填写完整信息！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/patient_portal/update_appointment',
        type: 'POST',
        data: {
            id: id,
            doctorName: doctorName,
            pointDate: pointDate,
            pointPlace: pointPlace
        },
        success: function (data) {
            if (data.code === 200) {
                Feng.success('编辑成功！');
                $('#appointmentEditModal').modal('hide');
                PatientPortal.loadAppointmentList();
                PatientPortal.loadDashboardAppointments();
            } else {
                Feng.error('编辑失败！' + (data.message || ''));
            }
        },
        error: function () {
            Feng.error('编辑失败！服务器异常');
        }
    });
};

/**
 * 删除预约
 */
PatientPortal.deleteAppointment = function (id) {
    layer.confirm('确定要取消该预约吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/patient_portal/delete_appointment',
            type: 'POST',
            data: {id: id},
            success: function (data) {
                if (data.code === 200) {
                    Feng.success('取消成功！');
                    PatientPortal.loadAppointmentList();
                    PatientPortal.loadDashboardAppointments();
                } else {
                    Feng.error('取消失败！');
                }
            },
            error: function () {
                Feng.error('取消失败！服务器异常');
            }
        });
        layer.close(index);
    });
};

// ========== 药品增删改 ==========

/**
 * 打开新增药品模态框
 */
PatientPortal.openMedicineAdd = function () {
    $('#medicineAddForm')[0].reset();
    $('#addMedicineImage').val('/static/img/medicine/vitamin.svg');
    $('#addMedicineStock').val('0');
    $('#medicineAddModal').modal('show');
};

/**
 * 提交新增药品
 */
PatientPortal.submitMedicine = function () {
    var medicineName = $('#addMedicineName').val();
    var medicinePrice = $('#addMedicinePrice').val();
    var medicineValue = $('#addMedicineValue').val();
    var medicineCategory = $('#addMedicineCategory').val();
    var medicineImage = $('#addMedicineImage').val();
    var medicineStock = $('#addMedicineStock').val();

    if (!medicineName || !medicinePrice || !medicineValue) {
        Feng.error('请填写完整信息！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/patient_portal/add_medicine',
        type: 'POST',
        data: {
            medicineName: medicineName,
            medicinePrice: medicinePrice,
            medicineValue: medicineValue,
            medicineCategory: medicineCategory,
            medicineImage: medicineImage,
            medicineStock: medicineStock
        },
        success: function (data) {
            if (data.code === 200) {
                Feng.success('添加成功！');
                $('#medicineAddModal').modal('hide');
                PatientPortal.loadMedicineList();
            } else {
                Feng.error('添加失败！' + (data.message || ''));
            }
        },
        error: function () {
            Feng.error('添加失败！服务器异常');
        }
    });
};

/**
 * 打开编辑药品模态框
 */
PatientPortal.openMedicineEdit = function (id, medicineName, medicinePrice, medicineValue, medicineCategory, medicineImage, medicineStock) {
    $('#editMedicineId').val(id);
    $('#editMedicineName').val(medicineName);
    $('#editMedicinePrice').val(medicinePrice);
    $('#editMedicineValue').val(medicineValue);
    $('#editMedicineCategory').val(medicineCategory || '其他');
    $('#editMedicineImage').val(medicineImage || '');
    $('#editMedicineStock').val(medicineStock != null ? medicineStock : 0);
    $('#medicineEditModal').modal('show');
};

/**
 * 提交编辑药品
 */
PatientPortal.submitEditMedicine = function () {
    var id = $('#editMedicineId').val();
    var medicineName = $('#editMedicineName').val();
    var medicinePrice = $('#editMedicinePrice').val();
    var medicineValue = $('#editMedicineValue').val();
    var medicineCategory = $('#editMedicineCategory').val();
    var medicineImage = $('#editMedicineImage').val();
    var medicineStock = $('#editMedicineStock').val();

    if (!id || !medicineName || !medicinePrice || !medicineValue) {
        Feng.error('请填写完整信息！');
        return;
    }

    $.ajax({
        url: Feng.ctxPath + '/patient_portal/update_medicine',
        type: 'POST',
        data: {
            id: id,
            medicineName: medicineName,
            medicinePrice: medicinePrice,
            medicineValue: medicineValue,
            medicineCategory: medicineCategory,
            medicineImage: medicineImage,
            medicineStock: medicineStock
        },
        success: function (data) {
            if (data.code === 200) {
                Feng.success('编辑成功！');
                $('#medicineEditModal').modal('hide');
                PatientPortal.loadMedicineList();
            } else {
                Feng.error('编辑失败！' + (data.message || ''));
            }
        },
        error: function () {
            Feng.error('编辑失败！服务器异常');
        }
    });
};

/**
 * 删除药品
 */
PatientPortal.deleteMedicine = function (id) {
    layer.confirm('确定要删除该药品吗？', {icon: 3, title: '提示'}, function (index) {
        $.ajax({
            url: Feng.ctxPath + '/patient_portal/delete_medicine',
            type: 'POST',
            data: {id: id},
            success: function (data) {
                if (data.code === 200) {
                    Feng.success('删除成功！');
                    PatientPortal.loadMedicineList();
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

/**
 * 获取健康状态标签
 */
PatientPortal.getHealthStatus = function (item) {
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

/**
 * 打开健康贴士模态框（加载个性化贴士）
 */
PatientPortal.openHealthTips = function () {
    $('#healthTipsList').html('<div class="empty-data"><i class="fa fa-spinner fa-spin"></i><p>加载中...</p></div>');
    $('#healthTipsModal').modal('show');

    $.ajax({
        url: Feng.ctxPath + '/patient_portal/health_tips?_t=' + new Date().getTime(),
        type: 'POST',
        dataType: 'json',
        timeout: 10000,
        success: function (tips) {
            if (!tips || tips.length === 0) {
                $('#healthTipsList').html('<div class="empty-data"><i class="fa fa-heartbeat"></i><p>暂无健康数据，请先完善健康档案</p></div>');
                return;
            }
            var html = '';
            for (var i = 0; i < tips.length; i++) {
                var t = tips[i];
                html += '<div class="tip-item">';
                html += '  <div class="tip-icon" style="background:' + t.bg + ';color:' + t.color + ';"><i class="fa ' + t.icon + '"></i></div>';
                html += '  <div class="tip-content">';
                html += '    <h5>' + t.title + '</h5>';
                html += '    <p>' + t.content + '</p>';
                html += '  </div>';
                html += '</div>';
            }
            $('#healthTipsList').html(html);
        },
        error: function (xhr, status, err) {
            $('#healthTipsList').html('<div class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败(' + status + ')，请稍后重试</p></div>');
        }
    });
};

/**
 * 打开紧急呼叫模态框
 */
PatientPortal.openEmergencyCall = function () {
    $('#emergencyCallModal').modal('show');
};

/**
 * 拨打电话
 */
PatientPortal.dialNumber = function (number) {
    layer.confirm('确定拨打 ' + number + ' 吗？', {
        icon: 3,
        title: '拨号确认',
        btn: ['确定', '取消']
    }, function (index) {
        window.location.href = 'tel:' + number;
        layer.close(index);
    });
};

// ========== 就诊台 ==========

/**
 * 加载居民端就诊台数据
 */
PatientPortal.loadConsultationDesk = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_consultation_status',
        type: 'POST',
        success: function (data) {
            PatientPortal.renderConsultationDesk(data);
        },
        error: function () {
            $('#patientWaitingBody').html('<div class="empty-data"><p>加载失败</p></div>');
        }
    });
};

/**
 * 渲染居民端就诊台
 */
PatientPortal.renderConsultationDesk = function (data) {
    var waitingList = data.waitingList || [];
    var inProgressList = data.inProgressList || [];
    var recentHistory = data.recentHistory || [];

    // 渲染候诊中
    if (waitingList.length === 0) {
        $('#patientWaitingBody').html(
            '<div style="padding:20px;text-align:center;">' +
            '<i class="fa fa-check-circle" style="font-size:36px;color:#11998e;"></i>' +
            '<p style="color:#999;margin-top:10px;">暂无候诊中的预约</p>' +
            '</div>'
        );
    } else {
        var wHtml = '';
        for (var i = 0; i < waitingList.length; i++) {
            var item = waitingList[i];
            wHtml += '<div class="consult-queue-item consult-queue-item-patient">';
            wHtml += '  <div class="consult-queue-number">' + (i + 1) + '</div>';
            wHtml += '  <div class="consult-queue-info">';
            wHtml += '    <h5><i class="fa fa-user-md"></i> ' + (item.doctorName || '-') + '</h5>';
            wHtml += '    <p><i class="fa fa-clock-o"></i> ' + (item.pointDate || '-') + '</p>';
            wHtml += '    <p><i class="fa fa-map-marker"></i> ' + (item.pointPlace || '-') + '</p>';
            wHtml += '  </div>';
            wHtml += '  <span class="status-tag warning" style="align-self:center;">候诊中</span>';
            wHtml += '</div>';
        }
        $('#patientWaitingBody').html(wHtml);
    }

    // 渲染就诊中
    if (inProgressList.length === 0) {
        $('#patientInProgressCard').hide();
        $('#patientNoConsultCard').show();
    } else {
        $('#patientNoConsultCard').hide();
        $('#patientInProgressCard').show();
        var ipHtml = '';
        for (var j = 0; j < inProgressList.length; j++) {
            var p = inProgressList[j];
            ipHtml += '<div style="text-align:center;padding:10px 0;">';
            ipHtml += '  <div style="width:72px;height:72px;border-radius:50%;background:linear-gradient(135deg,#1a9bfc,#0d6efd);margin:0 auto 12px;display:flex;align-items:center;justify-content:center;">';
            ipHtml += '    <i class="fa fa-stethoscope" style="font-size:32px;color:#fff;"></i>';
            ipHtml += '  </div>';
            ipHtml += '  <h4 style="margin:0 0 8px;color:#333;"><i class="fa fa-user-md"></i> ' + (p.doctorName || '-') + '</h4>';
            ipHtml += '  <p style="margin:0;color:#666;"><i class="fa fa-clock-o"></i> ' + (p.pointDate || '-') + '</p>';
            ipHtml += '  <p style="margin:4px 0 0;color:#666;"><i class="fa fa-map-marker"></i> ' + (p.pointPlace || '-') + '</p>';
            ipHtml += '  <div style="margin-top:12px;"><span class="status-tag info" style="font-size:13px;padding:5px 16px;"><i class="fa fa-spinner fa-spin"></i> 医生正在为您诊疗</span></div>';
            ipHtml += '</div>';
        }
        $('#patientInProgressBody').html(ipHtml);
    }

    // 渲染最近就诊记录
    if (recentHistory.length === 0) {
        $('#recentHistoryTable').html('<tr><td colspan="5" class="empty-data">暂无就诊记录</td></tr>');
    } else {
        var hHtml = '';
        for (var k = 0; k < recentHistory.length; k++) {
            var h = recentHistory[k];
            hHtml += '<tr>';
            hHtml += '<td>' + (h.patientDoctor || '-') + '</td>';
            hHtml += '<td>' + (h.patientSym || '-') + '</td>';
            hHtml += '<td>' + (h.patientMedicine || '-') + '</td>';
            hHtml += '<td>' + (h.takeprice || 0) + '</td>';
            hHtml += '<td>' + (h.patientHistoryDate || '-') + '</td>';
            hHtml += '</tr>';
        }
        $('#recentHistoryTable').html(hHtml);
    }
};

// ==================== 慢病管理 ====================

/**
 * 加载我的慢病档案
 */
PatientPortal.loadMyChronicList = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_chronic_list',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<div class="col-sm-12"><div class="empty-data"><i class="fa fa-heartbeat"></i><p>暂无慢病档案</p></div></div>';
            } else {
                var diseaseIcons = {
                    '高血压': 'fa-heart', '糖尿病': 'fa-tint', '冠心病': 'fa-heartbeat',
                    '脑卒中': 'fa-brain', '慢阻肺': 'fa-lungs', '慢性肾病': 'fa-kidneys'
                };
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var riskColor = item.riskLevel === '高风险' ? '#e74c3c' : (item.riskLevel === '中风险' ? '#f39c12' : '#27ae60');
                    var icon = diseaseIcons[item.diseaseType] || 'fa-heartbeat';
                    html += '<div class="col-sm-4" style="margin-bottom:15px;">';
                    html += '<div style="border:1px solid #e0e0e0;border-radius:10px;padding:20px;border-left:4px solid ' + riskColor + ';">';
                    html += '<div style="display:flex;align-items:center;margin-bottom:12px;">';
                    html += '<div style="width:48px;height:48px;border-radius:50%;background:' + riskColor + '15;display:flex;align-items:center;justify-content:center;margin-right:12px;">';
                    html += '<i class="fa ' + icon + '" style="font-size:20px;color:' + riskColor + ';"></i>';
                    html += '</div>';
                    html += '<div>';
                    html += '<h4 style="margin:0;color:#333;">' + (item.diseaseType || '-') + '</h4>';
                    html += '<span style="background:' + riskColor + ';color:#fff;padding:2px 8px;border-radius:10px;font-size:11px;">' + (item.riskLevel || '-') + '</span>';
                    html += '</div>';
                    html += '</div>';
                    html += '<p style="margin:4px 0;color:#666;font-size:13px;"><i class="fa fa-user-md"></i> 管理医生：' + (item.doctorName || '-') + '</p>';
                    html += '<p style="margin:4px 0;color:#666;font-size:13px;"><i class="fa fa-calendar"></i> 确诊日期：' + (item.diagnosisDate ? item.diagnosisDate.substring(0, 10) : '-') + '</p>';
                    if (item.remark) {
                        html += '<p style="margin:4px 0;color:#888;font-size:12px;"><i class="fa fa-info-circle"></i> ' + item.remark + '</p>';
                    }
                    html += '</div></div>';
                }
            }
            $('#myChronicCards').html(html);
        },
        error: function () {
            $('#myChronicCards').html('<div class="col-sm-12"><div class="empty-data">加载失败</div></div>');
        }
    });
};

/**
 * 加载我的随访计划
 */
PatientPortal.loadMyChronicPlans = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_chronic_plans',
        type: 'POST',
        success: function (data) {
            var html = '';
            if (!data || data.length === 0) {
                html = '<tr><td colspan="5" class="empty-data">暂无随访计划</td></tr>';
            } else {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    var statusText = '';
                    if (item.status === 0) {
                        statusText = '<span style="color:#3498db;"><i class="fa fa-clock-o"></i> 待执行</span>';
                    } else if (item.status === 1) {
                        statusText = '<span style="color:#27ae60;"><i class="fa fa-check-circle"></i> 已完成</span>';
                    } else {
                        statusText = '<span style="color:#e74c3c;"><i class="fa fa-exclamation-circle"></i> 已过期</span>';
                    }
                    html += '<tr>';
                    html += '<td>' + (item.diseaseType || '-') + '</td>';
                    html += '<td>' + (item.planDate ? item.planDate.substring(0, 16) : '-') + '</td>';
                    html += '<td>' + (item.planType || '-') + '</td>';
                    html += '<td>' + (item.doctorName || '-') + '</td>';
                    html += '<td>' + statusText + '</td>';
                    html += '</tr>';
                }
            }
            $('#myChronicPlanTable').html(html);
        },
        error: function () {
            $('#myChronicPlanTable').html('<tr><td colspan="5" class="empty-data">加载失败</td></tr>');
        }
    });
};

/**
 * 加载我的随访记录
 */
PatientPortal.loadMyChronicFollowups = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_chronic_followups',
        type: 'POST',
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
            $('#myChronicFollowupTable').html(html);
        },
        error: function () {
            $('#myChronicFollowupTable').html('<tr><td colspan="10" class="empty-data">加载失败</td></tr>');
        }
    });
};

// ==================== 公共卫生（居民个人） ====================
PatientPortal.loadMyVaccinations = function () {
    $.post(Feng.ctxPath + '/patient_portal/my_vaccinations', function (d) {
        var h = ''; for (var i = 0; i < Math.min(d.length, 10); i++) h += '<tr><td>' + d[i].vaccineName + '</td><td>' + d[i].doseSeq + '</td><td>' + (d[i].vaccDate || '') + '</td><td>' + (d[i].vaccSite || '-') + '</td></tr>';
        if (!h) h = '<tr><td colspan="4" class="text-muted text-center">暂无接种记录</td></tr>';
        $('#phMyVaccTable').html(h);
    });
};
PatientPortal.loadMyElderlyCheckups = function () {
    $.post(Feng.ctxPath + '/patient_portal/my_checkups', function (d) {
        var h = ''; for (var i = 0; i < Math.min(d.length, 10); i++) h += '<tr><td>' + (d[i].checkupDate || '') + '</td><td>' + (d[i].bmi || '-') + '</td><td>' + (d[i].bloodPressure || '-') + '</td><td>' + (d[i].bloodSugar || '-') + '</td><td>' + (d[i].healthAssessment || '-') + '</td></tr>';
        if (!h) h = '<tr><td colspan="5" class="text-muted text-center">暂无体检记录</td></tr>';
        $('#phMyCheckupTable').html(h);
    });
};

// ==================== 导出和打印 ====================

/**
 * 导出单条就诊记录为Excel
 */
PatientPortal.exportHistoryOne = function (id) {
    window.open(Feng.ctxPath + "/patientHistory/export/" + id);
};

/**
 * 批量导出我的所有就诊记录
 */
PatientPortal.exportMyHistories = function () {
    $.ajax({
        url: Feng.ctxPath + '/patient_portal/my_histories',
        type: 'POST',
        success: function (data) {
            if (!data || data.length === 0) {
                Feng.info("暂无就诊记录可导出");
                return;
            }
            var idcard = data[0].patientIdcard;
            if (!idcard) {
                Feng.error("无法获取身份证号，请完善个人信息");
                return;
            }
            window.open(Feng.ctxPath + "/patientHistory/exportByPatientIdcard?patientIdcard=" + encodeURIComponent(idcard));
        }
    });
};

/**
 * 打印单条就诊记录
 */
PatientPortal.printHistory = function (id) {
    var url = Feng.ctxPath + "/patientHistory/print/" + id;
    var w = window.open(url, '_blank', 'width=800,height=600,scrollbars=yes');
};
