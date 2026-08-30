var Vaccination = { id:"VaccinationTable", seItem:null, table:null, layerIndex:-1 };
Vaccination.initColumn = function(){return [
    {field:'selectItem',radio:true},
    {title:'姓名',field:'patientName',align:'center'},
    {title:'疫苗名称',field:'vaccineName',align:'center'},
    {title:'剂次',field:'doseSeq',align:'center'},
    {title:'接种日期',field:'vaccDate',align:'center'},
    {title:'批号',field:'batchNo',align:'center'},
    {title:'生产企业',field:'manufacturer',align:'center'}
];};
Vaccination.check=function(){var s=$('#'+this.id).bootstrapTable('getSelections');if(s.length===0){Feng.info("请先选中记录");return false}this.seItem=s[0];return true};
Vaccination.search=function(){this.table.refresh()};
Vaccination.openAdd=function(){this.layerIndex=layer.open({type:2,title:'新增接种',area:['800px','420px'],fix:false,maxmin:true,content:Feng.ctxPath+'/vaccination/add'})};
Vaccination.submitAdd=function(){var a=new $ax(Feng.ctxPath+"/vaccination/doAdd",function(){Feng.success("添加成功");Vaccination.table.refresh();parent.layer.close(Vaccination.layerIndex)},function(d){Feng.error("添加失败!")});a.set("patientName",$("#patientName").val());a.set("patientIdcard",$("#patientIdcard").val());a.set("vaccineName",$("#vaccineName").val());a.set("doseSeq",$("#doseSeq").val());a.set("vaccDate",$("#vaccDate").val());a.set("vaccSite",$("#vaccSite").val());a.set("batchNo",$("#batchNo").val());a.set("manufacturer",$("#manufacturer").val());a.set("remark",$("#remark").val());a.start()};
Vaccination.submitEdit=function(){var a=new $ax(Feng.ctxPath+"/vaccination/doUpdate",function(){Feng.success("修改成功");Vaccination.table.refresh();parent.layer.close(Vaccination.layerIndex)},function(d){Feng.error("修改失败!")});a.set("id",$("#id").val());a.set("patientName",$("#patientName").val());a.set("vaccineName",$("#vaccineName").val());a.set("doseSeq",$("#doseSeq").val());a.set("vaccDate",$("#vaccDate").val());a.set("vaccSite",$("#vaccSite").val());a.set("batchNo",$("#batchNo").val());a.start()};
Vaccination.delete=function(){if(this.check()){Feng.confirm("确定删除吗？",function(){var a=new $ax(Feng.ctxPath+"/vaccination/delete",function(){Feng.success("删除成功");Vaccination.table.refresh()},function(d){Feng.error("删除失败!")});a.set("id",Vaccination.seItem.id);a.start()})}};
Vaccination.close=function(){parent.layer.close(Vaccination.layerIndex)};
Vaccination.viewSchedule=function(){var a=new $ax(Feng.ctxPath+"/vaccination/schedule",function(d){var h='<div class="ibox"><div class="ibox-content"><h4>国家免疫规划程序</h4><table class="table table-striped table-bordered"><tr><th>疫苗</th><th>月龄</th><th>剂次</th><th>说明</th></tr>';for(var i=0;i<d.length;i++)h+='<tr><td>'+d[i].vaccineName+'</td><td>'+d[i].targetAge+'</td><td>'+d[i].doseSeq+'</td><td>'+d[i].description+'</td></tr>';h+='</table></div></div>';layer.open({type:1,title:'免疫规划',area:['700px','500px'],content:h,btn:['关闭'],yes:function(i){layer.close(i)}})});a.start()};
Vaccination.viewReminders=function(){var a=new $ax(Feng.ctxPath+"/vaccination/reminders",function(d){var h='<div class="ibox"><div class="ibox-content"><h4>待接种提醒</h4><table class="table table-striped"><tr><th>姓名</th><th>疫苗</th><th>剂次</th><th>下次接种日期</th></tr>';if(d.length===0)h+='<tr><td colspan="4" class="text-center">暂无待接种</td></tr>';for(var i=0;i<d.length;i++)h+='<tr><td>'+d[i].patientName+'</td><td>'+d[i].vaccineName+'</td><td>'+d[i].doseSeq+'</td><td>'+d[i].nextDate+'</td></tr>';h+='</table></div></div>';layer.open({type:1,title:'接种提醒',area:['600px','400px'],content:h,btn:['关闭'],yes:function(i){layer.close(i)}})});a.start()};
$(function(){var c=Vaccination.initColumn();var t=new BSTable(Vaccination.id,"/vaccination/list",c);t.setPaginationType("client");Vaccination.table=t.init();$("#totalCount").text("-");$("#planCount").text("-")});
