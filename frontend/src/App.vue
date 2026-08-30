<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { auth, logout } from './auth'
import { DataAnalysis, User, FirstAidKit, Calendar, TrendCharts, Box, Files, Fold, Expand, House, SwitchButton } from '@element-plus/icons-vue'
import { ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { loadPortalConfig, portalConfig } from './config/portal-config'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const isLogin = computed(() => route.path === '/login')
const isPortal = computed(() => Boolean(route.meta.standalone))
const isSelfContained = computed(() => Boolean(route.meta.selfContained))
const portalLabel = computed(() => auth.user?.portal === 'resident' ? '居民健康门户' : '医护工作门户')
const businessCenterPath = computed(() => auth.user?.portal === 'resident' ? '/resident/services' : '/staff/operations')
const roleLabel = computed(() => auth.user?.roles.includes('DOCTOR') ? '医生' : auth.user?.roles.includes('NURSE') ? '护士' : auth.user?.roles.includes('PHARMACIST') ? '药师' : auth.user?.roles.includes('REGISTRAR') ? '挂号员' : auth.user?.roles.includes('RESIDENT') ? '居民' : '系统管理员')
const menu = [
  ['/admin', '工作台', DataAnalysis], ['/admin/platform', '平台治理', Files], ['/admin/patients', '居民档案', User], ['/admin/doctors', '医生团队', FirstAidKit],
  ['/admin/appointments', '预约诊疗', Calendar], ['/admin/health', '健康监测', TrendCharts], ['/admin/medicines', '药品库存', Box], ['/admin/chronic', '慢病管理', Files],
] as const
onMounted(() => { void loadPortalConfig().catch(() => undefined) })
async function signOut() { const portal = auth.user?.portal; await logout(); await router.replace({ path: '/login', query: portal ? { portal } : {} }) }
</script>

<template>
  <el-config-provider :locale="zhCn">
    <template v-if="isLogin || isSelfContained">
      <router-view />
      <router-link v-if="isSelfContained && auth.user?.portal === 'resident'" to="/resident/services" class="resident-service-entry">进入完整业务中心</router-link>
    </template>
    <div v-else-if="isPortal" class="portal-shell">
    <header class="portal-topbar">
      <div class="portal-brand"><span>✦</span><div><b>{{ portalConfig.organizationName }}</b><small>{{ portalLabel }}</small></div></div>
      <div class="portal-user"><router-link :to="businessCenterPath" class="portal-center-link">业务中心</router-link><div class="portal-avatar"><el-icon><component :is="auth.user?.portal === 'resident' ? House : FirstAidKit" /></el-icon></div><div><b>{{ auth.user?.displayName }}</b><small>{{ roleLabel }} · 已安全认证</small></div><el-button text :icon="SwitchButton" @click="signOut">退出</el-button></div>
    </header>
    <main class="portal-main"><router-view /></main>
    </div>
    <div v-else class="app-shell">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand"><span class="brand-mark">✦</span><div v-if="!collapsed"><b>{{ portalConfig.organizationName }}</b><small>Community Care</small></div></div>
      <nav>
        <router-link v-for="item in menu" :key="item[0]" :to="item[0]" class="nav-item">
          <el-icon><component :is="item[2]" /></el-icon><span v-if="!collapsed">{{ item[1] }}</span>
        </router-link>
      </nav>
      <button class="collapse-button" @click="collapsed = !collapsed"><el-icon><component :is="collapsed ? Expand : Fold" /></el-icon><span v-if="!collapsed">收起导航</span></button>
    </aside>
    <main class="main-area">
      <header class="topbar">
        <div><span class="eyebrow">{{ portalConfig.organizationName }}</span><h1>{{ route.meta.title }}</h1></div>
        <el-dropdown @command="signOut">
          <div class="user-chip"><span class="avatar">{{ auth.user?.displayName?.slice(0, 1) }}</span><div><b>{{ auth.user?.displayName }}</b><small>系统管理员</small></div></div>
          <template #dropdown><el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </header>
      <section class="page-content"><router-view /></section>
    </main>
    </div>
  </el-config-provider>
</template>

<style scoped>
.portal-shell{min-height:100vh;background:radial-gradient(circle at 82% 0,#e1f3ee 0,transparent 29%),#f3f7f6}.portal-topbar{height:72px;padding:0 max(24px,calc((100vw - 1440px)/2));display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #dfeae8;background:rgba(255,255,255,.88);backdrop-filter:blur(16px);position:sticky;top:0;z-index:30}.portal-brand,.portal-user{display:flex;align-items:center;gap:11px}.portal-brand>span{width:38px;height:38px;display:grid;place-items:center;border-radius:12px;color:white;background:#0b6e69;font-size:22px}.portal-brand b,.portal-brand small,.portal-user b,.portal-user small{display:block}.portal-brand b{font-size:16px}.portal-brand small,.portal-user small{margin-top:2px;color:#839794;font-size:10px}.portal-avatar{width:36px;height:36px;display:grid;place-items:center;border-radius:11px;color:#0b6e69;background:#e0f2ed}.portal-user>.el-button{margin-left:8px;color:#718683}.portal-main{width:min(1440px,100%);margin:0 auto;padding:22px 28px 48px}@media(max-width:700px){.portal-topbar{height:64px;padding:0 16px}.portal-brand small,.portal-user>div:nth-child(2){display:none}.portal-main{padding:14px 12px 34px}}
.portal-center-link,.resident-service-entry{padding:9px 13px;border:1px solid #cfe0dc;border-radius:10px;color:#0b6e69;background:#fff;text-decoration:none;font-size:12px;font-weight:700}.portal-center-link:focus-visible,.resident-service-entry:focus-visible{outline:3px solid #8fd4c8;outline-offset:2px}.resident-service-entry{position:fixed;right:22px;bottom:22px;z-index:90;color:#fff;background:#0b746e;box-shadow:0 10px 28px rgba(8,85,80,.28)}
</style>
