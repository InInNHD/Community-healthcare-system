<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { homeForCurrentUser, login, verifyMfa, type Portal } from '../auth'
import { User, Lock, Management, FirstAidKit, House } from '@element-plus/icons-vue'
import { portalConfig } from '../config/portal-config'
import { generatedApiErrorMessage } from '../api/generated-client'
import { ElMessage } from 'element-plus'
import { configuredEntryPortal } from '../config/entry-portal'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive<{ username: string; password: string; portal: Portal }>({ username: '', password: '', portal: 'admin' })
const challengeToken = ref('')
const mfaCode = ref('')
const demoMode = import.meta.env.MODE === 'demo'
const demoAccounts = demoMode ? {
  admin: { username: 'admin', password: 'Admin@123456' },
  staff: { username: 'doctor', password: 'Doctor@123456' },
  resident: { username: 'resident', password: 'Resident@123456' },
} : null
const allPortals = [
  { value: 'admin' as const, label: '管理端', hint: '全局运营与配置', icon: Management },
  { value: 'staff' as const, label: '医护端', hint: '诊疗、随访与处置', icon: FirstAidKit },
  { value: 'resident' as const, label: '居民端', hint: '个人健康与预约', icon: House },
]
const portals = configuredEntryPortal
  ? allPortals.filter(portal => portal.value === configuredEntryPortal)
  : allPortals
function choosePortal(portal: typeof portals[number]) {
  form.portal = portal.value
  const demoAccount = demoAccounts?.[portal.value]
  form.username = demoAccount?.username ?? ''
  form.password = demoAccount?.password ?? ''
}
onMounted(() => {
  const requested = portals.find(item => item.value === route.query.portal)
  if (requested) choosePortal(requested)
  else if (demoMode) choosePortal(portals[0])
})
async function submit() {
  loading.value = true
  try {
    if (challengeToken.value) {
      await verifyMfa(challengeToken.value, mfaCode.value)
    } else {
      const outcome = await login(form.username, form.password, form.portal)
      if (!outcome.authenticated) {
        challengeToken.value = outcome.challenge.challengeToken
        return
      }
    }
    await router.replace(homeForCurrentUser())
  } catch (error) {
    ElMessage.error(generatedApiErrorMessage(error))
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-story">
      <div class="story-top"><span class="story-logo">✦</span><b>{{ portalConfig.organizationName }}</b></div>
      <div class="story-copy"><span class="kicker">CONNECTED COMMUNITY CARE</span><h1>让每一次社区健康服务<br>都有迹可循。</h1><p>连接居民、家庭医生与公共卫生服务，用清晰的数据帮助基层医疗团队做出更及时的判断。</p></div>
      <div class="story-metrics"><div><b>7×24</b><span>健康服务连续守护</span></div><div><b>一体化</b><span>诊疗与公卫协同</span></div></div>
    </section>
    <section class="login-panel">
      <div class="login-box"><span class="welcome">统一门户认证</span><h2>选择您的身份</h2><p>系统将根据角色与数据归属开放对应服务</p>
        <div v-if="!challengeToken" class="portal-options">
          <button v-for="portal in portals" :key="portal.value" type="button" :class="{ active: form.portal === portal.value }" @click="choosePortal(portal)">
            <el-icon><component :is="portal.icon" /></el-icon><span><b>{{ portal.label }}</b><small>{{ portal.hint }}</small></span>
          </button>
        </div>
        <el-form v-if="!challengeToken" :model="form" size="large" @submit.prevent="submit">
          <el-form-item><el-input v-model="form.username" aria-label="用户名" autocomplete="username" placeholder="用户名" :prefix-icon="User" /></el-form-item>
          <el-form-item><el-input v-model="form.password" aria-label="密码" autocomplete="current-password" type="password" show-password placeholder="密码" :prefix-icon="Lock" @keyup.enter="submit" /></el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="login-button">进入系统</el-button>
        </el-form>
        <el-form v-else size="large" @submit.prevent="submit">
          <p>该账号已启用双因素认证，请输入身份验证器中的 6 位动态验证码。</p>
          <el-form-item><el-input v-model="mfaCode" aria-label="动态验证码" inputmode="numeric" maxlength="6" autocomplete="one-time-code" placeholder="6 位动态验证码" /></el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="login-button">完成安全验证</el-button>
        </el-form>
        <div v-if="demoMode && !challengeToken" class="demo-tip"><b>当前演示账号</b><span>{{ form.username }} / {{ form.password }}</span></div>
        <div v-else class="demo-tip"><b>安全提示</b><span>请使用管理员分配的门户账号登录</span></div>
      </div>
      <small class="copyright">Community Healthcare Platform · 安全连接</small>
    </section>
  </div>
</template>

<style scoped>
.login-page{min-height:100vh;display:grid;grid-template-columns:1.08fr .92fr;background:#fff}.login-story{position:relative;overflow:hidden;padding:48px 64px;color:white;background:linear-gradient(145deg,rgba(5,75,72,.96),rgba(6,105,98,.9)),radial-gradient(circle at 80% 20%,#49b8a7,transparent 40%)}.login-story:after{content:"";position:absolute;width:500px;height:500px;right:-170px;bottom:-210px;border:1px solid rgba(255,255,255,.16);border-radius:50%;box-shadow:0 0 0 70px rgba(255,255,255,.035),0 0 0 140px rgba(255,255,255,.025)}.story-top{display:flex;align-items:center;gap:12px;font-size:18px}.story-logo{width:40px;height:40px;display:grid;place-items:center;border-radius:13px;background:#e0f7ef;color:#09645f;font-size:24px}.story-copy{position:relative;z-index:1;margin-top:20vh;max-width:650px}.kicker{color:#8fd5cc;font-size:12px;letter-spacing:.18em}.story-copy h1{margin:18px 0 24px;font-size:48px;line-height:1.28;letter-spacing:-.04em}.story-copy p{max-width:560px;color:#c5e0dd;font-size:16px;line-height:1.9}.story-metrics{position:absolute;z-index:1;left:64px;bottom:50px;display:flex;gap:54px}.story-metrics b,.story-metrics span{display:block}.story-metrics b{font-size:20px}.story-metrics span{margin-top:5px;color:#9bc9c4;font-size:12px}.login-panel{position:relative;display:grid;place-items:center;padding:32px}.login-box{width:430px}.welcome{color:#0b746e;font-weight:700}.login-box h2{margin:8px 0 6px;font-size:31px;color:#173a38}.login-box>p{margin:0 0 20px;color:#526966}.portal-options{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-bottom:20px}.portal-options button{padding:12px 8px;display:flex;align-items:center;gap:8px;text-align:left;border:1px solid #dfe9e7;border-radius:12px;color:#5c7370;background:#fff;cursor:pointer;transition:.18s}.portal-options button:hover{border-color:#77b5ae}.portal-options button.active{border-color:#0b746e;color:#075a56;background:#eaf7f3;box-shadow:0 0 0 2px rgba(11,116,110,.09)}.portal-options .el-icon{font-size:20px;flex:none}.portal-options b,.portal-options small{display:block;white-space:nowrap}.portal-options b{font-size:13px}.portal-options small{margin-top:3px;color:#526966;font-size:9px}.login-box :deep(.el-input__wrapper){height:48px;border-radius:12px;box-shadow:0 0 0 1px #dfe9e7 inset}.login-button{width:100%;height:48px;margin-top:6px;border-radius:12px;font-weight:700}.demo-tip{margin-top:18px;padding:12px 14px;display:flex;justify-content:space-between;border-radius:12px;color:#3f5b57;background:#f1f7f5;font-size:12px}.copyright{position:absolute;bottom:22px;color:#526966}@media(max-width:900px){.login-page{display:block}.login-story{display:none}.login-panel{min-height:100vh;padding:24px}.login-box{width:min(430px,100%)}.copyright{position:static;margin-top:28px}}
</style>
