<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, SwitchButton } from '@element-plus/icons-vue'
import { auth, logout } from '../auth'
import { http } from '../api/http'

const router = useRouter()
const saving = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const policy = reactive({
  minLength: 12,
  maxLength: 128,
  maxUtf8Bytes: 72,
  requireUppercase: true,
  requireLowercase: true,
  requireDigit: true,
  requireSpecial: true,
})
const passwordIsStrong = computed(() =>
  form.newPassword.length >= policy.minLength
  && form.newPassword.length <= policy.maxLength
  && new TextEncoder().encode(form.newPassword).length <= policy.maxUtf8Bytes
  && (!policy.requireUppercase || /[A-Z]/.test(form.newPassword))
  && (!policy.requireLowercase || /[a-z]/.test(form.newPassword))
  && (!policy.requireDigit || /\d/.test(form.newPassword))
  && (!policy.requireSpecial || /[^A-Za-z0-9]/.test(form.newPassword))
  && !/\s/.test(form.newPassword),
)
const ruleDescription = computed(() => {
  const requirements = [
    policy.requireUppercase && '大写字母',
    policy.requireLowercase && '小写字母',
    policy.requireDigit && '数字',
    policy.requireSpecial && '特殊字符',
  ].filter(Boolean)
  const characterRule = requirements.length ? `，且包含${requirements.join('、')}` : ''
  return `${policy.minLength}–${policy.maxLength} 位${characterRule}，UTF-8 编码不超过 ${policy.maxUtf8Bytes} 字节，且不得包含空格。`
})

onMounted(async () => {
  const { data } = await http.get<typeof policy>('/auth/password-policy')
  Object.assign(policy, data)
})

async function submit() {
  if (!form.currentPassword) return ElMessage.warning('请输入当前密码')
  if (!passwordIsStrong.value) return ElMessage.warning('新密码不符合安全要求')
  if (form.newPassword !== form.confirmPassword) return ElMessage.warning('两次输入的新密码不一致')
  if (form.currentPassword === form.newPassword) return ElMessage.warning('新密码不能与当前密码相同')

  saving.value = true
  try {
    await http.put('/auth/change-password', {
      currentPassword: form.currentPassword,
      newPassword: form.newPassword,
    })
    const portal = auth.user?.portal
    await logout()
    ElMessage.success('密码修改成功，请使用新密码重新登录')
    await router.replace({ path: '/login', query: portal ? { portal, passwordChanged: '1' } : {} })
  } finally {
    saving.value = false
  }
}

async function signOut() {
  const portal = auth.user?.portal
  await logout()
  await router.replace({ path: '/login', query: portal ? { portal } : {} })
}
</script>

<template>
  <main class="password-page">
    <section class="password-card">
      <div class="security-mark"><el-icon><Lock /></el-icon></div>
      <span class="eyebrow">账号安全</span>
      <h1>首次登录需要修改密码</h1>
      <p>当前账号使用的是初始密码。完成修改后，旧令牌会立即失效，需使用新密码重新登录。</p>

      <el-form :model="form" label-position="top" size="large" @submit.prevent="submit">
        <el-form-item label="当前密码">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" @keyup.enter="submit" />
        </el-form-item>
        <div class="password-rules" :class="{ valid: passwordIsStrong }">
          {{ ruleDescription }}
        </div>
        <el-button type="primary" native-type="submit" :loading="saving" class="submit-button">保存新密码</el-button>
        <el-button text :icon="SwitchButton" class="logout-button" @click="signOut">退出并返回登录页</el-button>
      </el-form>
    </section>
  </main>
</template>

<style scoped>
.password-page{min-height:100vh;display:grid;place-items:center;padding:24px;background:radial-gradient(circle at 18% 10%,#d9f0e9,transparent 32%),#f3f7f6}.password-card{width:min(480px,100%);padding:38px;border:1px solid #dce9e6;border-radius:22px;background:#fff;box-shadow:0 20px 60px rgba(28,78,72,.1)}.security-mark{width:54px;height:54px;display:grid;place-items:center;margin-bottom:20px;border-radius:16px;color:#08706a;background:#e3f4ef;font-size:25px}.eyebrow{color:#08706a;font-size:12px;font-weight:800;letter-spacing:.12em}.password-card h1{margin:8px 0 10px;color:#173a38;font-size:28px}.password-card>p{margin:0 0 24px;color:#708481;line-height:1.7}.password-rules{margin:-4px 0 18px;padding:10px 12px;border-radius:10px;color:#8b6a2d;background:#fff7e8;font-size:12px;line-height:1.6}.password-rules.valid{color:#0b6f58;background:#e9f7f1}.submit-button{width:100%;height:46px;border-radius:11px;font-weight:700}.logout-button{width:100%;margin:10px 0 0}@media(max-width:520px){.password-card{padding:28px 22px;border-radius:18px}}
</style>
