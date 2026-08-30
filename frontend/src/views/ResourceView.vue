<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { http, type PageResult } from '../api/http'
import type { FieldConfig, ResourceConfig } from '../resource-config'

const props = defineProps<{ config: ResourceConfig }>()
const loading = ref(false), dialog = ref(false), saving = ref(false), editingId = ref<number | null>(null)
const stockDialog = ref(false), stockSaving = ref(false), stockDelta = ref(0), stockRow = ref<Record<string, any> | null>(null)
const rows = ref<Record<string, any>[]>([]), total = ref(0), page = ref(1), size = ref(10), keyword = ref('')
const form = reactive<Record<string, any>>({}), formRef = ref<any>()
const visibleFields = computed(() => props.config.fields)
const rules = computed(() => Object.fromEntries(props.config.fields.filter(f=>f.required).map(f=>[f.key,[{required:true,message:`请填写${f.label}`,trigger:'blur'}]])))
function resetForm() { Object.keys(form).forEach(key=>delete form[key]); props.config.fields.forEach(field=>form[field.key]=field.default ?? '') }
function format(row:Record<string,any>, field:FieldConfig) { const value=row[field.key]; if(value===null||value===undefined||value==='')return '—'; return field.type==='select'?field.options?.find(o=>o.value===value)?.label??value:value }
async function load() {
  loading.value=true
  try { const params:Record<string,any>={page:page.value-1,size:size.value}; if(props.config.endpoint==='/health-records'){if(keyword.value)params.patientId=keyword.value}else params.keyword=keyword.value
    const {data}=await http.get<PageResult>(props.config.endpoint,{params});rows.value=data.items;total.value=data.total
  } finally { loading.value=false }
}
function openCreate(){editingId.value=null;resetForm();dialog.value=true}
function openEdit(row:Record<string,any>){editingId.value=row.id;resetForm();Object.assign(form,row);dialog.value=true}
function fieldDisabled(field: FieldConfig) { return Boolean(field.readonly || (editingId.value && field.immutableAfterCreate)) }
async function save(){if(!(await formRef.value?.validate()))return;saving.value=true;try{if(editingId.value)await http.put(`${props.config.endpoint}/${editingId.value}`,form);else await http.post(props.config.endpoint,form);ElMessage.success('保存成功');dialog.value=false;await load()}finally{saving.value=false}}
async function remove(row:Record<string,any>){await ElMessageBox.confirm('删除后将不再出现在业务列表中，确定继续吗？','确认删除',{type:'warning'});await http.delete(`${props.config.endpoint}/${row.id}`);ElMessage.success('已删除');await load()}
const statusTransitions: Record<string, { value: string; label: string }[]> = {
  PENDING: [{ value: 'CONFIRMED', label: '确认预约' }, { value: 'CANCELLED', label: '取消预约' }],
  CONFIRMED: [{ value: 'COMPLETED', label: '完成接诊' }, { value: 'CANCELLED', label: '取消预约' }],
}
function availableStatusActions(row: Record<string, any>) { return statusTransitions[String(row.status)] ?? [] }
async function changeStatus(row: Record<string, any>, status: string) {
  const action = availableStatusActions(row).find(item => item.value === status)
  if (!action) return
  await ElMessageBox.confirm(`确定执行“${action.label}”吗？状态变更将受业务状态机约束。`, '预约状态流转', { type: 'warning' })
  await http.patch(`${props.config.endpoint}/${row.id}/status`, { status })
  ElMessage.success('预约状态已更新')
  await load()
}
function openStockAdjustment(row: Record<string, any>) { stockRow.value = row; stockDelta.value = 0; stockDialog.value = true }
async function adjustStock() {
  if (!stockRow.value || !Number.isInteger(stockDelta.value) || stockDelta.value === 0) return ElMessage.warning('请输入非零整数调整量')
  if (Number(stockRow.value.stock) + stockDelta.value < 0) return ElMessage.warning('调整后库存不能为负数')
  stockSaving.value = true
  try {
    await http.patch(`${props.config.endpoint}/${stockRow.value.id}/stock`, { delta: stockDelta.value })
    ElMessage.success('库存调整成功')
    stockDialog.value = false
    await load()
  } finally { stockSaving.value = false }
}
watch(()=>props.config,()=>{page.value=1;keyword.value='';load()})
onMounted(load)
</script>

<template>
  <div class="page-heading"><div><h2>{{ config.title }}</h2><p>{{ config.description }}</p></div><el-button v-if="config.creatable!==false" type="primary" :icon="Plus" @click="openCreate">新增记录</el-button></div>
  <section class="surface">
    <div class="toolbar"><el-input v-model="keyword" :placeholder="config.searchPlaceholder" :prefix-icon="Search" clearable style="width:320px" @keyup.enter="page=1;load()" @clear="page=1;load()"><template #append><el-button @click="page=1;load()">查询</el-button></template></el-input><el-button :icon="Refresh" circle @click="load" /></div>
    <div class="table-wrap"><el-table v-loading="loading" :data="rows" stripe><el-table-column type="index" label="#" width="55" />
      <el-table-column v-for="field in visibleFields" :key="field.key" :label="field.label" :width="field.width" min-width="110" show-overflow-tooltip><template #default="scope">{{ format(scope.row,field) }}</template></el-table-column>
      <el-table-column label="操作" fixed="right" width="250"><template #default="scope">
        <el-dropdown v-if="config.statusWorkflow && availableStatusActions(scope.row).length" @command="(status: string) => changeStatus(scope.row, status)"><el-button link type="success">流转状态</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-for="action in availableStatusActions(scope.row)" :key="action.value" :command="action.value">{{ action.label }}</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
        <el-button v-if="config.stockAdjustable" link type="success" @click="openStockAdjustment(scope.row)">调整库存</el-button>
        <el-button v-if="config.editable!==false" link type="primary" @click="openEdit(scope.row)">编辑</el-button><el-button link type="danger" @click="remove(scope.row)">删除</el-button>
      </template></el-table-column>
    </el-table><div class="pagination"><el-pagination v-model:current-page="page" v-model:page-size="size" layout="total, sizes, prev, pager, next" :total="total" @current-change="load" @size-change="page=1;load()" /></div></div>
  </section>
  <el-dialog v-model="dialog" :title="editingId ? `编辑${config.title}` : `新增${config.title}`" width="620px" destroy-on-close>
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top"><div class="form-grid"><el-form-item v-for="field in config.fields" :key="field.key" :label="field.label" :prop="field.key" :class="{wide:field.type==='textarea'}">
      <el-input v-if="!field.type||field.type==='text'" v-model="form[field.key]" :disabled="field.key==='appointmentNo'||fieldDisabled(field)" />
      <el-input-number v-else-if="field.type==='number'" v-model="form[field.key]" :disabled="fieldDisabled(field)" :min="0" :precision="field.key==='price'||field.key==='balance'||field.key==='weight'?2:0" style="width:100%" />
      <el-date-picker v-else-if="field.type==='date'" v-model="form[field.key]" :disabled="fieldDisabled(field)" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      <el-date-picker v-else-if="field.type==='datetime'" v-model="form[field.key]" :disabled="fieldDisabled(field)" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
      <el-select v-else-if="field.type==='select'" v-model="form[field.key]" :disabled="fieldDisabled(field)" clearable style="width:100%"><el-option v-for="option in field.options" :key="option.value" :label="option.label" :value="option.value" /></el-select>
      <el-input v-else v-model="form[field.key]" :disabled="fieldDisabled(field)" type="textarea" :rows="3" />
    </el-form-item></div></el-form>
    <template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
  <el-dialog v-model="stockDialog" title="调整药品库存" width="440px">
    <p class="stock-summary">{{ stockRow?.name }} 当前库存 <b>{{ stockRow?.stock }}</b>，调整后库存 <b>{{ Number(stockRow?.stock ?? 0) + stockDelta }}</b></p>
    <el-form label-position="top"><el-form-item label="调整量（入库为正数，出库为负数）"><el-input-number v-model="stockDelta" :precision="0" :step="1" style="width:100%" /></el-form-item></el-form>
    <template #footer><el-button @click="stockDialog=false">取消</el-button><el-button type="primary" :loading="stockSaving" @click="adjustStock">确认调整</el-button></template>
  </el-dialog>
</template>

<style scoped>
.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 18px}.form-grid .wide{grid-column:1/-1}
.stock-summary{margin:0 0 18px;padding:12px 14px;border-radius:10px;color:#536b68;background:#f1f7f5}.stock-summary b{color:#0b716a}
</style>
