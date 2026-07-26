/**
 * 管理员端门户
 */
var AdminPortal = {

    ctxPath: Feng.ctxPath,

    /**
     * 初始化
     */
    init: function () {
        this.loadDashboardAppointments();
        this.loadDoctorInfoList();
        this.loadPublicHealthStats();
    },

    loadPublicHealthStats: function () {
        var self = this;
        Feng.ajaxAsync(Feng.ctxPath + '/admin_portal/public_health_stats', null, function (data) {
            $('#phVaccCount').text(data.vaccinationCount || 0);
            $('#phMaternalCount').text(data.maternalCount || 0);
            $('#phElderlyCount').text(data.elderlyCount || 0);
            $('#phInfectCount').text(data.infectiousCount || 0);
            $('#phPendingCount').text(data.pendingInfectious || 0);
        });
    },

    /**
     * 切换Tab面板
     */
    switchTab: function (tabName) {
        // 隐藏所有面板
        $('[id^="panel-"]').hide();

        // 显示目标面板
        $('#panel-' + tabName).show();

        // 更新导航高亮
        $('.portal-navbar .nav > li').removeClass('active');
        $('.portal-navbar .nav > li a').each(function () {
            if ($(this).attr('onclick') && $(this).attr('onclick').indexOf(tabName) > -1) {
                $(this).parent('li').addClass('active');
            }
        });

        // 加载iframe内容（延迟加载）
        this.loadIframeContent(tabName);
    },

    /**
     * 延迟加载iframe内容
     */
    loadIframeContent: function (tabName) {
        var urlMap = {
            medicine: '/medicineInfo',
            health: '/patientHealth',
            history: '/patientHistory'
        };

        if (urlMap[tabName]) {
            var $iframe = $('#iframe-' + tabName);
            if ($iframe.attr('src') === '' || $iframe.attr('src') === undefined) {
                $iframe.attr('src', this.ctxPath + urlMap[tabName]);
            }
        }

        // 预约管理：加载数据列表
        if (tabName === 'appointment') {
            this.loadAppointmentList();
            this.loadAppointmentStats();
        }

        // 慢病管理：加载数据
        if (tabName === 'chronic') {
            this.loadChronicList();
            this.loadChronicStats();
        }

        // 系统管理默认加载用户管理
        if (tabName === 'system') {
            var $iframe = $('#iframe-system');
            if ($iframe.attr('src') === '' || $iframe.attr('src') === undefined) {
                $iframe.attr('src', this.ctxPath + '/mgr');
            }
        }
    },

    /**
     * 系统管理子Tab切换
     */
    switchSystemTab: function (subTab) {
        var urlMap = {
            user: '/mgr',
            role: '/role',
            dept: '/dept',
            menu: '/menu',
            dict: '/dict',
            log: '/log',
            loginLog: '/loginLog',
            notice: '/notice'
        };

        // 更新子Tab高亮
        $('#systemTabs li').removeClass('active');
        $('#systemTabs li a').each(function () {
            if ($(this).attr('onclick') && $(this).attr('onclick').indexOf(subTab) > -1) {
                $(this).parent('li').addClass('active');
            }
        });

        // 加载iframe
        if (urlMap[subTab]) {
            $('#iframe-system').attr('src', this.ctxPath + urlMap[subTab]);
        }
    },

    /**
     * 自适应iframe高度
     */
    resizeIframe: function (iframe) {
        try {
            var doc = iframe.contentDocument || iframe.contentWindow.document;
            var height = doc.body.scrollHeight || doc.documentElement.scrollHeight;
            iframe.style.height = Math.max(height + 30, 600) + 'px';
        } catch (e) {
            iframe.style.height = '600px';
        }
    },

    /**
     * 加载工作台最近预约
     */
    loadDashboardAppointments: function () {
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/recent_appointments",
            success: function (data) {
                var html = '';
                if (!data || data.length === 0) {
                    html = '<tr><td colspan="4" class="empty-data"><i class="fa fa-calendar-times-o"></i><p>暂无预约记录</p></td></tr>';
                } else {
                    for (var i = 0; i < data.length; i++) {
                        var item = data[i];
                        var dateStr = item.pointDate ? new Date(item.pointDate).toLocaleString('zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '-';
                        html += '<tr>';
                        html += '<td>' + (item.patientName || '-') + '</td>';
                        html += '<td>' + (item.doctorName || '-') + '</td>';
                        html += '<td>' + dateStr + '</td>';
                        html += '<td>' + (item.pointPlace || '-') + '</td>';
                        html += '</tr>';
                    }
                }
                $('#dashboardAppointments').html(html);
            },
            error: function () {
                $('#dashboardAppointments').html('<tr><td colspan="4" class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败</p></td></tr>');
            }
        });
    },

    /**
     * 用户信息子Tab切换
     */
    switchUserinfoTab: function (subTab) {
        $('#userinfoTabs li').removeClass('active');
        $('#userinfoTabs li a').each(function () {
            if ($(this).attr('onclick') && $(this).attr('onclick').indexOf(subTab) > -1) {
                $(this).parent('li').addClass('active');
            }
        });

        if (subTab === 'doctor') {
            $('#userinfo-doctor').show();
            $('#userinfo-patient').hide();
            this.loadDoctorInfoList();
        } else if (subTab === 'patient') {
            $('#userinfo-doctor').hide();
            $('#userinfo-patient').show();
            this.loadPatientInfoList();
        }
    },

    /**
     * 加载医生信息列表
     */
    loadDoctorInfoList: function () {
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/doctor_list",
            success: function (data) {
                var html = '';
                if (!data || data.length === 0) {
                    html = '<tr><td colspan="9" class="empty-data"><i class="fa fa-user-md"></i><p>暂无医生信息</p></td></tr>';
                } else {
                    for (var i = 0; i < data.length; i++) {
                        var item = data[i];
                        var statusText = item.status === 1 ? '<span style="color:#27ae60">正常</span>' : '<span style="color:#e74c3c">停诊</span>';
                        var accountHtml = item.hasAccount
                            ? '<span style="color:#27ae60"><i class="fa fa-check-circle"></i> ' + (item.account || '-') + '</span>'
                            : '<span style="color:#e74c3c"><i class="fa fa-times-circle"></i> 未注册</span>';
                        var userStatusHtml = '-';
                        if (item.hasAccount && item.userStatus !== undefined) {
                            if (item.userStatus === 1) userStatusHtml = '<span style="color:#27ae60">正常</span>';
                            else if (item.userStatus === 2) userStatusHtml = '<span style="color:#e67e22">冻结</span>';
                            else userStatusHtml = '<span style="color:#e74c3c">已删除</span>';
                        }
                        html += '<tr>';
                        html += '<td>' + (item.id || '-') + '</td>';
                        html += '<td>' + (item.doctorName || '-') + '</td>';
                        html += '<td>' + (item.department || '-') + '</td>';
                        html += '<td>' + (item.title || '-') + '</td>';
                        html += '<td>' + (item.specialty || '-') + '</td>';
                        html += '<td>' + statusText + '</td>';
                        html += '<td>' + accountHtml + '</td>';
                        html += '<td>' + (item.phone || '-') + '</td>';
                        html += '<td>' + userStatusHtml + '</td>';
                        html += '</tr>';
                    }
                }
                $('#doctorInfoList').html(html);
            },
            error: function () {
                $('#doctorInfoList').html('<tr><td colspan="9" class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败</p></td></tr>');
            }
        });
    },

    /**
     * 加载居民信息列表
     */
    loadPatientInfoList: function () {
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/patient_list",
            success: function (data) {
                var html = '';
                if (!data || data.length === 0) {
                    html = '<tr><td colspan="6" class="empty-data"><i class="fa fa-users"></i><p>暂无居民信息</p></td></tr>';
                } else {
                    for (var i = 0; i < data.length; i++) {
                        var item = data[i];
                        var accountHtml = item.hasAccount
                            ? '<span style="color:#27ae60"><i class="fa fa-check-circle"></i> ' + (item.account || '-') + '</span>'
                            : '<span style="color:#e74c3c"><i class="fa fa-times-circle"></i> 未注册</span>';
                        var userStatusHtml = '-';
                        if (item.hasAccount && item.userStatus !== undefined) {
                            if (item.userStatus === 1) userStatusHtml = '<span style="color:#27ae60">正常</span>';
                            else if (item.userStatus === 2) userStatusHtml = '<span style="color:#e67e22">冻结</span>';
                            else userStatusHtml = '<span style="color:#e74c3c">已删除</span>';
                        }
                        html += '<tr>';
                        html += '<td>' + (item.paientIdcard || '-') + '</td>';
                        html += '<td>' + (item.paientName || '-') + '</td>';
                        html += '<td>' + (item.paientMoney || '0') + '</td>';
                        html += '<td>' + accountHtml + '</td>';
                        html += '<td>' + (item.phone || '-') + '</td>';
                        html += '<td>' + userStatusHtml + '</td>';
                        html += '</tr>';
                    }
                }
                $('#patientInfoList').html(html);
            },
            error: function () {
                $('#patientInfoList').html('<tr><td colspan="6" class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败</p></td></tr>');
            }
        });
    },

    /**
     * 同步医生账号
     */
    syncDoctors: function () {
        var index = layer.load(1, {shade: [0.3, '#000']});
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/sync_doctors",
            success: function (data) {
                layer.close(index);
                if (data.code === 0) {
                    layer.msg('同步成功！已为 ' + (data.data || 0) + ' 位医生生成登录账号', {icon: 1, time: 3000});
                } else {
                    layer.msg(data.message || '同步失败', {icon: 2, time: 3000});
                }
                AdminPortal.loadDoctorInfoList();
            },
            error: function () {
                layer.close(index);
                layer.msg('同步请求失败', {icon: 2, time: 3000});
            }
        });
    },

    /**
     * 同步居民账号
     */
    syncPatients: function () {
        var index = layer.load(1, {shade: [0.3, '#000']});
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/sync_patients",
            success: function (data) {
                layer.close(index);
                if (data.code === 0) {
                    layer.msg('同步成功！已为 ' + (data.data || 0) + ' 位居民生成登录账号', {icon: 1, time: 3000});
                } else {
                    layer.msg(data.message || '同步失败', {icon: 2, time: 3000});
                }
                AdminPortal.loadPatientInfoList();
            },
            error: function () {
                layer.close(index);
                layer.msg('同步请求失败', {icon: 2, time: 3000});
            }
        });
    },

    /**
     * 加载预约列表
     */
    loadAppointmentList: function () {
        var status = $('#aptStatusFilter').val() || '';
        var doctorName = $('#aptDoctorFilter').val() || '';
        var patientName = $('#aptPatientFilter').val() || '';

        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/appointment_list",
            data: { status: status, doctorName: doctorName, patientName: patientName },
            success: function (data) {
                var html = '';
                if (!data || data.length === 0) {
                    html = '<tr><td colspan="8" class="empty-data"><i class="fa fa-calendar-times-o"></i><p>暂无预约记录</p></td></tr>';
                } else {
                    for (var i = 0; i < data.length; i++) {
                        var item = data[i];
                        var dateStr = item.pointDate ? new Date(item.pointDate).toLocaleString('zh-CN', {year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '-';

                        var statusHtml = '-';
                        if (item.status === 0) statusHtml = '<span style="color:#3498db"><i class="fa fa-clock-o"></i> 待参与</span>';
                        else if (item.status === 1) statusHtml = '<span style="color:#27ae60"><i class="fa fa-check-circle"></i> 已完成</span>';
                        else if (item.status === 2) statusHtml = '<span style="color:#e74c3c"><i class="fa fa-exclamation-circle"></i> 已逾期</span>';
                        else if (item.status === 3) statusHtml = '<span style="color:#e67e22"><i class="fa fa-stethoscope"></i> 就诊中</span>';

                        var sourceHtml = item.source === '医护端'
                            ? '<span style="color:#2980b9"><i class="fa fa-user-md"></i> 医护端</span>'
                            : '<span style="color:#8e44ad"><i class="fa fa-user"></i> 居民端</span>';

                        html += '<tr>';
                        html += '<td>' + (item.id || '-') + '</td>';
                        html += '<td>' + (item.patientName || '-') + '</td>';
                        html += '<td>' + (item.patientIdcard ? item.patientIdcard : '-') + '</td>';
                        html += '<td>' + (item.doctorName || '-') + '</td>';
                        html += '<td>' + dateStr + '</td>';
                        html += '<td>' + (item.pointPlace || '-') + '</td>';
                        html += '<td>' + statusHtml + '</td>';
                        html += '<td>' + sourceHtml + '</td>';
                        html += '</tr>';
                    }
                }
                $('#appointmentList').html(html);
            },
            error: function () {
                $('#appointmentList').html('<tr><td colspan="8" class="empty-data"><i class="fa fa-exclamation-circle"></i><p>加载失败</p></td></tr>');
            }
        });
    },

    /**
     * 加载预约统计
     */
    loadAppointmentStats: function () {
        $.ajax({
            type: "POST",
            url: AdminPortal.ctxPath + "/admin_portal/appointment_stats",
            success: function (data) {
                if (data) {
                    $('#aptTotalCount').text(data.total || 0);
                    $('#aptWaitingCount').text(data.waiting || 0);
                    $('#aptCompletedCount').text(data.completed || 0);
                    $('#aptInProgressCount').text(data.inProgress || 0);
                }
            }
        });
    },

    // ==================== 慢病管理 ====================

    /**
     * 加载慢病档案列表
     */
    loadChronicList: function () {
        var diseaseType = $('#adminChronicDiseaseFilter').val() || '';
        var riskLevel = $('#adminChronicRiskFilter').val() || '';
        var patientName = $('#adminChronicNameFilter').val() || '';
        $.ajax({
            url: AdminPortal.ctxPath + '/admin_portal/chronic_list',
            type: 'POST',
            data: {diseaseType: diseaseType, riskLevel: riskLevel, patientName: patientName},
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
                        html += '<td>' + (item.remark || '-') + '</td>';
                        html += '</tr>';
                    }
                }
                $('#adminChronicTable').html(html);
            },
            error: function () {
                $('#adminChronicTable').html('<tr><td colspan="9" class="empty-data">加载失败</td></tr>');
            }
        });
    },

    /**
     * 加载慢病统计数据
     */
    loadChronicStats: function () {
        $.ajax({
            url: AdminPortal.ctxPath + '/admin_portal/chronic_stats',
            type: 'POST',
            success: function (data) {
                $('#adminChronicTotal').text(data.totalCount || 0);
                $('#adminChronicLow').text((data.riskCount && data.riskCount['低风险']) || 0);
                $('#adminChronicMid').text((data.riskCount && data.riskCount['中风险']) || 0);
                $('#adminChronicHigh').text((data.riskCount && data.riskCount['高风险']) || 0);
                $('#adminChronicPending').text(data.pendingFollowupCount || 0);
            }
        });
    }
};
