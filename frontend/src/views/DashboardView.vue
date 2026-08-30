<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { http } from '../api/http'
import { User, FirstAidKit, Calendar, Warning, Files, Box } from '@element-plus/icons-vue'

const summary = ref<Record<string, number>>({})
const recent = ref<Record<string, any>[]>([])
const cards = [
  ['patients','居民总数',User,'#0b746e'],['doctors','在岗医生',FirstAidKit,'#4c7ce5'],['appointmentsToday','今日预约',Calendar,'#d88931'],
  ['pendingAppointments','待确认预约',Warning,'#d55e61'],['chronicCases','慢病档案',Files,'#845ec2'],['lowStockMedicines','库存预警',Box,'#bd6b30'],
] as const
const statusText: Record<string,string> = { PENDING:'待确认',CONFIRMED:'已确认',COMPLETED:'已完成',CANCELLED:'已取消' }
onMounted(async () => {
  const [stats, appointments] = await Promise.all([http.get('/dashboard/summary'), http.get('/dashboard/recent-appointments')])
  summary.value = stats.data; recent.value = appointments.data
})
</script>

<template>
  <div class="page-heading"><div><h2>早上好，开始今天的健康服务</h2><p>这里汇总了社区卫生服务中心当前的重点数据。</p></div><span class="live"><i />数据实时更新</span></div>
  <div class="stat-grid">
    <article v-for="card in cards" :key="card[0]" class="stat-card surface"><div class="stat-icon" :style="{color:card[3],background:`${card[3]}14`}"><el-icon><component :is="card[2]" /></el-icon></div><div><span>{{ card[1] }}</span><b>{{ summary[card[0]] ?? '—' }}</b></div></article>
  </div>
  <div class="dashboard-grid">
    <section class="surface recent"><div class="section-title"><div><h3>近期预约</h3><p>最新预约与服务进度</p></div><router-link to="/appointments">查看全部 →</router-link></div>
      <el-table :data="recent" style="width:100%"><el-table-column prop="appointmentNo" label="预约号" width="150"/><el-table-column prop="patientId" label="患者 ID"/><el-table-column prop="doctorId" label="医生 ID"/><el-table-column prop="scheduledAt" label="预约时间" width="180"/><el-table-column label="状态" width="100"><template #default="scope"><el-tag effect="light">{{ statusText[scope.row.status] }}</el-tag></template></el-table-column></el-table>
    </section>
    <aside class="surface focus"><div class="section-title"><div><h3>今日重点</h3><p>需要优先处理的事项</p></div></div><div class="focus-item warning"><b>{{ summary.pendingAppointments ?? 0 }}</b><div><strong>待确认预约</strong><span>请及时联系居民确认</span></div></div><div class="focus-item stock"><b>{{ summary.lowStockMedicines ?? 0 }}</b><div><strong>药品库存预警</strong><span>低于安全库存阈值</span></div></div><router-link to="/medicines" class="focus-link">进入库存管理</router-link></aside>
  </div>
</template>

<style scoped>
.live{padding:8px 12px;border-radius:20px;color:#4f7773;background:#e9f4f1;font-size:12px}.live i{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#20a47b;box-shadow:0 0 0 4px #cceadf}.stat-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.stat-card{padding:21px;display:flex;align-items:center;gap:16px}.stat-icon{width:48px;height:48px;display:grid;place-items:center;border-radius:14px;font-size:23px}.stat-card span,.stat-card b{display:block}.stat-card span{color:#78908d;font-size:13px}.stat-card b{margin-top:5px;font-size:27px}.dashboard-grid{display:grid;grid-template-columns:minmax(0,2fr) minmax(270px,.8fr);gap:18px;margin-top:18px}.recent,.focus{padding:22px}.section-title{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.section-title h3{margin:0 0 5px;font-size:17px}.section-title p{margin:0;color:#8aa09d;font-size:12px}.section-title a{color:#0b746e;text-decoration:none;font-size:13px}.focus-item{padding:16px;display:flex;align-items:center;gap:14px;border-radius:14px;margin-top:12px}.focus-item b{width:44px;height:44px;display:grid;place-items:center;border-radius:12px;font-size:19px}.focus-item strong,.focus-item span{display:block}.focus-item strong{font-size:14px}.focus-item span{margin-top:4px;color:#81928f;font-size:11px}.warning{background:#fff6eb}.warning b{color:#b56b18;background:#ffe5c3}.stock{background:#fff0f0}.stock b{color:#c54d52;background:#ffdadd}.focus-link{display:block;margin-top:18px;padding:12px;text-align:center;border:1px solid #dce9e6;border-radius:11px;color:#0b746e;text-decoration:none;font-size:13px}
</style>
