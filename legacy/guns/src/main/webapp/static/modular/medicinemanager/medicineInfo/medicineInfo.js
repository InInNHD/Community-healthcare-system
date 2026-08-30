/**
 * 药品管理
 */
var MedicineInfo = {
    id: "MedicineInfoTable",
    seItem: null,
    table: null,
    layerIndex: -1,
    allMedicines: [],
    currentCategory: 'all'
};

MedicineInfo.stockBadge = function (stock, stockMin) {
    if (stock == null) return '-';
    var s = stock || 0, m = stockMin || 10;
    if (s <= 0) return '<span class="label label-default">售罄</span>';
    if (s <= m) return '<span class="label label-danger">' + s + ' ⚠</span>';
    if (s <= m * 2) return '<span class="label label-warning">' + s + '</span>';
    return '<span class="label label-success">' + s + '</span>';
};

MedicineInfo.initColumn = function () {
    return [
        {field: 'selectItem', radio: true},
        {title: '编号', field: 'id', align: 'center', width: '60px'},
        {title: '名称', field: 'medicineName', align: 'center', sortable: true},
        {title: '分类', field: 'medicineCategory', align: 'center'},
        {title: '价格(元)', field: 'medicinePrice', align: 'center'},
        {title: '库存', field: 'medicineStock', align: 'center', sortable: true,
            formatter: function (v, r) { return MedicineInfo.stockBadge(v, r.medicineStockMin || 10); }},
        {title: '预警阈值', field: 'medicineStockMin', align: 'center'},
        {title: '疗效', field: 'medicineValue', align: 'center'}
    ];
};

MedicineInfo.check = function () {
    var selected = $('#' + this.id).bootstrapTable('getSelections');
    if (selected.length === 0) { Feng.info("请先选中表格中的某一记录！"); return false; }
    MedicineInfo.seItem = selected[0];
    return true;
};

MedicineInfo.openAddMedicineInfo = function () {
    var index = layer.open({
        type: 2, title: '添加药品', area: ['800px', '400px'], fix: false, maxmin: true,
        content: Feng.ctxPath + '/medicineInfo/medicineInfo_add'
    });
    this.layerIndex = index;
};

MedicineInfo.openMedicineInfoDetail = function () {
    if (this.check()) {
        var index = layer.open({
            type: 2, title: '编辑药品', area: ['800px', '420px'], fix: false, maxmin: true,
            content: Feng.ctxPath + '/medicineInfo/medicineInfo_update/' + MedicineInfo.seItem.id
        });
        this.layerIndex = index;
    }
};

MedicineInfo.delete = function () {
    if (this.check()) {
        var item = MedicineInfo.seItem;
        Feng.confirm("确定要下架【" + item.medicineName + "】吗？（软删除，可恢复）", function () {
            var ajax = new $ax(Feng.ctxPath + "/medicineInfo/delete", function (data) {
                Feng.success("下架成功!");
                MedicineInfo.table.refresh();
                MedicineInfo.loadCards();
            }, function (data) {
                Feng.error("下架失败!" + data.responseJSON.message + "!");
            });
            ajax.set("medicineInfoId", item.id);
            ajax.start();
        });
    }
};

MedicineInfo.search = function () {
    MedicineInfo.table.refresh();
};

// ==================== 卡片视图（独立于表格） ====================

MedicineInfo.loadCards = function () {
    $.ajax({
        url: Feng.ctxPath + '/medicineInfo/list',
        type: 'POST',
        success: function (d) {
            MedicineInfo.allMedicines = d || [];
            MedicineInfo.renderCategoryBar();
            MedicineInfo.renderMedicineCards();
        },
        error: function () {
            $('#medicineCardGrid').html('<div class="col-sm-12 text-center text-muted" style="padding:20px;">加载失败</div>');
        }
    });
};

MedicineInfo.renderCategoryBar = function () {
    var cats = ['all'], catSet = {};
    for (var i = 0; i < MedicineInfo.allMedicines.length; i++) {
        var c = MedicineInfo.allMedicines[i].medicineCategory;
        if (c && !catSet[c]) { catSet[c] = true; cats.push(c); }
    }
    var h = '';
    for (var j = 0; j < cats.length; j++) {
        var c = cats[j], label = c === 'all' ? '全部' : c;
        h += '<span class="label label-' + (MedicineInfo.currentCategory === c ? 'primary' : 'default') + '" style="cursor:pointer;margin:2px 4px;font-size:13px;display:inline-block;" onclick="MedicineInfo.filterCategory(\'' + c + '\')">' + label + '</span>';
    }
    $('#medicineCategoryBar').html(h);
};

MedicineInfo.filterCategory = function (cat) {
    MedicineInfo.currentCategory = cat;
    MedicineInfo.renderCategoryBar();
    MedicineInfo.renderMedicineCards();
};

MedicineInfo.filterCards = function () { MedicineInfo.renderMedicineCards(); };

MedicineInfo.renderMedicineCards = function () {
    var data = MedicineInfo.allMedicines,
        cat = MedicineInfo.currentCategory,
        kw = ($('#medicineSearchInput').val() || '').trim().toLowerCase(),
        filtered = [];
    for (var i = 0; i < data.length; i++) {
        var m = data[i];
        if ((cat === 'all' || m.medicineCategory === cat) &&
            (!kw || (m.medicineName || '').toLowerCase().indexOf(kw) > -1 ||
             (m.medicineCategory || '').toLowerCase().indexOf(kw) > -1)) {
            filtered.push(m);
        }
    }
    var h = '';
    if (filtered.length === 0) {
        h = '<div class="col-sm-12"><div class="text-center text-muted" style="padding:40px;"><i class="fa fa-medkit" style="font-size:48px;display:block;margin-bottom:10px;"></i>暂无药品信息</div></div>';
    } else {
        for (var j = 0; j < filtered.length; j++) {
            var med = filtered[j];
            h += '<div class="col-sm-3" style="margin-bottom:16px;">';
            h += '<div class="ibox float-e-margins" style="margin-bottom:0;">';
            h += '<div class="ibox-content" style="padding:15px;">';
            h += '<div class="text-center" style="margin-bottom:10px;">';
            h += '<i class="fa fa-medkit" style="font-size:36px;color:#1ab394;"></i>';
            h += '</div>';
            h += '<h5 style="text-align:center;margin:4px 0;">' + (med.medicineName || '-') + '</h5>';
            h += '<p class="text-center text-muted" style="font-size:11px;margin:2px 0;">' + (med.medicineCategory || '未分类') + '</p>';
            h += '<p class="text-center" style="font-size:12px;color:#888;margin:4px 0;height:32px;overflow:hidden;">' + (med.medicineValue || '-') + '</p>';
            h += '<div class="text-center" style="margin:8px 0;">';
            h += '<span style="font-size:16px;color:#e67e22;font-weight:bold;">&yen;' + (med.medicinePrice || 0) + '</span>';
            h += '</div>';
            h += '<div class="text-center" style="margin:4px 0;">' + MedicineInfo.stockBadge(med.medicineStock, med.medicineStockMin || 10) + '</div>';
            h += '<div class="text-center" style="margin-top:8px;">';
            h += '<button class="btn btn-xs btn-success" onclick="MedicineInfo.cardStockIn(' + med.id + ')" title="入库"><i class="fa fa-sign-in"></i> 入库</button> ';
            h += '<button class="btn btn-xs btn-warning" onclick="MedicineInfo.cardStockOut(' + med.id + ')" title="出库"><i class="fa fa-sign-out"></i> 出库</button> ';
            h += '<button class="btn btn-xs btn-info" onclick="MedicineInfo.cardBatches(' + med.id + ')" title="批次"><i class="fa fa-barcode"></i></button> ';
            h += '<button class="btn btn-xs btn-default" onclick="MedicineInfo.cardEdit(' + med.id + ')" title="编辑"><i class="fa fa-edit"></i></button>';
            h += '</div>';
            h += '</div></div></div>';
        }
    }
    $('#medicineCardGrid').html(h);
};

MedicineInfo.cardStockIn = function (id) {
    MedicineInfo.seItem = MedicineInfo.findMedicine(id);
    MedicineInfo.openStockIn();
};

MedicineInfo.cardStockOut = function (id) {
    MedicineInfo.seItem = MedicineInfo.findMedicine(id);
    MedicineInfo.openStockOut();
};

MedicineInfo.cardBatches = function (id) {
    MedicineInfo.seItem = MedicineInfo.findMedicine(id);
    MedicineInfo.viewBatches();
};

MedicineInfo.cardEdit = function (id) {
    MedicineInfo.seItem = MedicineInfo.findMedicine(id);
    MedicineInfo.openMedicineInfoDetail();
};

MedicineInfo.findMedicine = function (id) {
    for (var i = 0; i < MedicineInfo.allMedicines.length; i++) {
        if (MedicineInfo.allMedicines[i].id === id) return MedicineInfo.allMedicines[i];
    }
    return { id: id, medicineName: '', medicineStock: 0 };
};

// ==================== 入库 ====================
MedicineInfo.openStockIn = function () {
    if (!this.seItem) return Feng.info("请先选择药品");
    var item = this.seItem,
        s = item.medicineStock != null ? item.medicineStock : 0,
        h = '<div class="ibox"><div class="ibox-content"><div class="form-horizontal">';
    h += '<h4>入库 - ' + item.medicineName + '（库存：<span style="color:#e67e22">' + s + '</span>）</h4>';
    h += '<input type="hidden" id="stockInMedicineId" value="' + item.id + '">';
    h += '<div class="row"><div class="col-sm-6 b-r">';
    h += '<div class="form-group"><label class="col-sm-4">批次号</label><div class="col-sm-8"><input class="form-control" id="batchNo" placeholder="如: B20260601"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">数量</label><div class="col-sm-8"><input class="form-control" id="inQuantity" type="number"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">单价</label><div class="col-sm-8"><input class="form-control" id="unitPrice" type="number" step="0.01"/></div></div>';
    h += '</div><div class="col-sm-6">';
    h += '<div class="form-group"><label class="col-sm-4">供应商</label><div class="col-sm-8"><input class="form-control" id="supplier"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">有效期至</label><div class="col-sm-8"><input class="form-control" id="expiryDate" type="date"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">备注</label><div class="col-sm-8"><textarea class="form-control" id="remark" rows="2"></textarea></div></div>';
    h += '</div></div>';
    h += '<div class="row btn-group-m-t"><div class="col-sm-10"><button class="btn btn-info" onclick="MedicineInfo.doStockIn()"><i class="fa fa-check"></i> 确认入库</button><button class="btn btn-danger" onclick="parent.layer.close(MedicineInfo.layerIndex)"><i class="fa fa-eraser"></i> 取消</button></div></div>';
    h += '</div></div>';
    this.layerIndex = layer.open({ type: 1, title: '药品入库', area: ['700px', '420px'], content: h });
};

MedicineInfo.doStockIn = function () {
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/stockIn", function (data) {
        Feng.success("入库成功，库存已更新！");
        MedicineInfo.table.refresh();
        MedicineInfo.loadCards();
        parent.layer.close(MedicineInfo.layerIndex);
    }, function (data) {
        Feng.error("入库失败：" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.set("medicineId", $("#stockInMedicineId").val());
    ajax.set("batchNo", $("#batchNo").val());
    ajax.set("quantity", $("#inQuantity").val());
    ajax.set("unitPrice", $("#unitPrice").val());
    ajax.set("supplier", $("#supplier").val());
    ajax.set("expiryDate", $("#expiryDate").val());
    ajax.set("remark", $("#remark").val());
    ajax.start();
};

// ==================== 出库 ====================
MedicineInfo.openStockOut = function () {
    if (!this.seItem) return Feng.info("请先选择药品");
    var item = this.seItem,
        s = item.medicineStock != null ? item.medicineStock : 0,
        h = '<div class="ibox"><div class="ibox-content"><div class="form-horizontal">';
    h += '<h4>出库 - ' + item.medicineName + '（库存：<span style="color:#e67e22">' + s + '</span>）</h4>';
    h += '<input type="hidden" id="stockOutMedicineId" value="' + item.id + '">';
    h += '<div class="row"><div class="col-sm-6 b-r">';
    h += '<div class="form-group"><label class="col-sm-4">批次号</label><div class="col-sm-8"><input class="form-control" id="outBatchNo" placeholder="可选"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">数量</label><div class="col-sm-8"><input class="form-control" id="outQuantity" type="number"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">出库原因</label><div class="col-sm-8"><select class="form-control" id="outReason"><option value="门诊发药">门诊发药</option><option value="住院发药">住院发药</option><option value="退货">退货</option><option value="报损">报损</option><option value="盘点">盘点</option><option value="其他">其他</option></select></div></div>';
    h += '</div><div class="col-sm-6">';
    h += '<div class="form-group"><label class="col-sm-4">患者</label><div class="col-sm-8"><input class="form-control" id="outPatientName"/></div></div>';
    h += '<div class="form-group"><label class="col-sm-4">备注</label><div class="col-sm-8"><textarea class="form-control" id="outRemark" rows="2"></textarea></div></div>';
    h += '</div></div>';
    h += '<div class="row btn-group-m-t"><div class="col-sm-10"><button class="btn btn-info" onclick="MedicineInfo.doStockOut()"><i class="fa fa-check"></i> 确认出库</button><button class="btn btn-danger" onclick="parent.layer.close(MedicineInfo.layerIndex)"><i class="fa fa-eraser"></i> 取消</button></div></div>';
    h += '</div></div>';
    this.layerIndex = layer.open({ type: 1, title: '药品出库', area: ['700px', '380px'], content: h });
};

MedicineInfo.doStockOut = function () {
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/stockOut", function (data) {
        Feng.success("出库成功，库存已扣减！");
        MedicineInfo.table.refresh();
        MedicineInfo.loadCards();
        parent.layer.close(MedicineInfo.layerIndex);
    }, function (data) {
        Feng.error("出库失败：" + (data.responseJSON ? data.responseJSON.message : ""));
    });
    ajax.set("medicineId", $("#stockOutMedicineId").val());
    ajax.set("batchNo", $("#outBatchNo").val());
    ajax.set("quantity", $("#outQuantity").val());
    ajax.set("reason", $("#outReason").val());
    ajax.set("patientName", $("#outPatientName").val());
    ajax.set("remark", $("#outRemark").val());
    ajax.start();
};

// ==================== 批次 ====================
MedicineInfo.viewBatches = function () {
    if (!this.seItem) return Feng.info("请先选择药品");
    var item = this.seItem;
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/batches/" + item.id, function (d) {
        var h = '<div class="ibox"><div class="ibox-content"><h4>批次管理 - ' + item.medicineName + '</h4>';
        h += '<table class="table table-striped table-bordered"><thead><tr><th>批次号</th><th>有效期至</th><th>初始量</th><th>剩余量</th><th>状态</th></tr></thead><tbody>';
        if (d.length === 0) { h += '<tr><td colspan="5" class="text-center text-muted">暂无批次记录</td></tr>'; }
        for (var i = 0; i < d.length; i++) {
            var b = d[i],
                st = b.status === 1 ? '<span class="label label-success">在用</span>' : (b.status === 0 ? '<span class="label label-default">已用完</span>' : '<span class="label label-danger">已过期</span>');
            h += '<tr><td>' + (b.batchNo || '-') + '</td><td>' + (b.expiryDate || '-') + '</td><td>' + b.initialQuantity + '</td><td>' + b.remainingQuantity + '</td><td>' + st + '</td></tr>';
        }
        h += '</tbody></table></div></div>';
        layer.open({ type: 1, title: '批次管理', area: ['650px', '400px'], content: h, btn: ['关闭'], yes: function (i) { layer.close(i); } });
    }, function () { Feng.error("获取批次信息失败"); });
    ajax.start();
};

// ==================== 低库存预警 ====================
MedicineInfo.lowStockAlert = function () {
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/lowStock", function (d) {
        var h = '<div class="ibox"><div class="ibox-content"><h4><i class="fa fa-exclamation-triangle" style="color:#e74c3c"></i> 低库存预警</h4>';
        h += '<table class="table table-striped table-bordered"><thead><tr><th>药品</th><th>当前库存</th><th>预警阈值</th><th>状态</th></tr></thead><tbody>';
        if (d.length === 0) { h += '<tr><td colspan="4" class="text-center text-success">✓ 所有药品库存充足</td></tr>'; }
        for (var i = 0; i < d.length; i++) {
            h += '<tr><td>' + d[i].medicineName + '</td><td><span class="label label-danger">' + d[i].medicineStock + '</span></td><td>' + (d[i].medicineStockMin || 10) + '</td><td><span class="label label-danger">需补货</span></td></tr>';
        }
        h += '</tbody></table></div></div>';
        layer.open({ type: 1, title: '低库存预警', area: ['600px', '380px'], content: h, btn: ['关闭'], yes: function (i) { layer.close(i); } });
    }, function () { Feng.error("获取预警信息失败"); });
    ajax.start();
};

// ==================== 上架（恢复已下架药品） ====================
MedicineInfo.openRestore = function () {
    var self = this;
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/deletedList", function (d) {
        var h = '<div class="ibox"><div class="ibox-content"><h4><i class="fa fa-archive"></i> 已下架药品</h4>';
        h += '<table class="table table-striped table-bordered"><thead><tr><th>编号</th><th>名称</th><th>分类</th><th>价格</th><th>操作</th></tr></thead><tbody>';
        if (d.length === 0) { h += '<tr><td colspan="5" class="text-center text-muted">暂无已下架药品</td></tr>'; }
        for (var i = 0; i < d.length; i++) {
            var m = d[i];
            h += '<tr><td>' + m.id + '</td><td>' + m.medicineName + '</td><td>' + (m.medicineCategory || '-') + '</td><td>¥' + (m.medicinePrice || 0) + '</td>';
            h += '<td><button class="btn btn-xs btn-success" onclick="MedicineInfo.doRestore(' + m.id + ')"><i class="fa fa-upload"></i> 上架</button></td></tr>';
        }
        h += '</tbody></table></div></div>';
        self.layerIndex = layer.open({ type: 1, title: '药品上架', area: ['650px', '420px'], content: h, btn: ['关闭'], yes: function (i) { layer.close(i); } });
    }, function () { Feng.error("获取已下架列表失败"); });
    ajax.start();
};

MedicineInfo.doRestore = function (id) {
    Feng.confirm("确定要重新上架该药品吗？", function () {
        var ajax = new $ax(Feng.ctxPath + "/medicineInfo/restore", function () {
            Feng.success("上架成功！");
            MedicineInfo.table.refresh();
            MedicineInfo.loadCards();
            parent.layer.close(MedicineInfo.layerIndex);
        }, function (d) { Feng.error("上架失败：" + (d.responseJSON ? d.responseJSON.message : "")); });
        ajax.set("medicineInfoId", id);
        ajax.start();
    });
};

// ==================== 出入库记录 ====================
MedicineInfo.viewStockLog = function () {
    var mid = this.seItem ? this.seItem.id : '';
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/stockLog?medicineId=" + mid, function (d) {
        var h = '<div class="ibox"><div class="ibox-content"><h4>出入库记录</h4>';
        h += '<table class="table table-striped table-bordered"><thead><tr><th>类型</th><th>数量</th><th>批次</th><th>操作人</th><th>时间</th><th>备注</th></tr></thead><tbody>';
        if (d.length === 0) { h += '<tr><td colspan="6" class="text-center text-muted">暂无记录</td></tr>'; }
        for (var i = 0; i < d.length; i++) {
            var r = d[i], cls = r.type === '入库' ? 'label-success' : 'label-warning';
            h += '<tr><td><span class="label ' + cls + '">' + r.type + '</span></td><td>' + r.quantity + '</td><td>' + (r.batchNo || '-') + '</td><td>' + (r.operator || '-') + '</td><td>' + (r.createTime || '-') + '</td><td>' + (r.remark || r.reason || '-') + '</td></tr>';
        }
        h += '</tbody></table></div></div>';
        layer.open({ type: 1, title: '出入库记录', area: ['800px', '500px'], content: h, btn: ['关闭'], yes: function (i) { layer.close(i); } });
    }, function () { Feng.error("获取出入库记录失败"); });
    ajax.start();
};

// ==================== 初始化 ====================
$(function () {
    var defaultColunms = MedicineInfo.initColumn();
    var table = new BSTable(MedicineInfo.id, "/medicineInfo/list", defaultColunms);
    table.setPaginationType("client");
    MedicineInfo.table = table.init();
    // 独立加载卡片数据
    MedicineInfo.loadCards();
});
