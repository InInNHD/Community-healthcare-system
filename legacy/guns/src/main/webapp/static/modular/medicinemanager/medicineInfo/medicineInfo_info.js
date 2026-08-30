var MedicineInfoInfoDlg = {
    medicineInfoInfoData : {}
};

MedicineInfoInfoDlg.clearData = function() {
    this.medicineInfoInfoData = {};
}

MedicineInfoInfoDlg.set = function(key, val) {
    this.medicineInfoInfoData[key] = (typeof val == "undefined") ? $("#" + key).val() : val;
    return this;
}

MedicineInfoInfoDlg.get = function(key) {
    return $("#" + key).val();
}

MedicineInfoInfoDlg.close = function() {
    parent.layer.close(window.parent.MedicineInfo.layerIndex);
}

MedicineInfoInfoDlg.collectData = function() {
    this
    .set('id')
    .set('medicineName')
    .set('medicinePrice')
    .set('medicineValue')
    .set('medicineCategory')
    .set('medicineStock')
    .set('medicineStockMin');
}

MedicineInfoInfoDlg.addSubmit = function() {
    this.clearData();
    this.collectData();
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/add", function(data){
        Feng.success("添加成功!");
        window.parent.MedicineInfo.table.refresh();
        MedicineInfoInfoDlg.close();
    },function(data){
        Feng.error("添加失败!" + data.responseJSON.message + "!");
    });
    ajax.set(this.medicineInfoInfoData);
    ajax.start();
}

MedicineInfoInfoDlg.editSubmit = function() {
    this.clearData();
    this.collectData();
    var ajax = new $ax(Feng.ctxPath + "/medicineInfo/update", function(data){
        Feng.success("修改成功!");
        window.parent.MedicineInfo.table.refresh();
        MedicineInfoInfoDlg.close();
    },function(data){
        Feng.error("修改失败!" + data.responseJSON.message + "!");
    });
    ajax.set(this.medicineInfoInfoData);
    ajax.start();
}

$(function() { });
