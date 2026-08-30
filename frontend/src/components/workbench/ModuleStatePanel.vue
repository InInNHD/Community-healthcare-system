<script setup lang="ts">
defineProps<{
  state: 'idle' | 'loading' | 'success' | 'empty' | 'error'
  errorMessage?: string
  emptyMessage?: string
}>()

defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="state === 'loading'" class="module-state" role="status" aria-live="polite">
    <span class="state-spinner" aria-hidden="true" />
    <div><strong>正在加载业务数据</strong><p>请稍候，不会重复提交业务操作。</p></div>
  </div>
  <div v-else-if="state === 'error'" class="module-state error" role="alert">
    <span aria-hidden="true">!</span>
    <div><strong>模块暂时不可用</strong><p>{{ errorMessage || '服务连接失败，请稍后重试。' }}</p></div>
    <button type="button" @click="$emit('retry')">重新加载</button>
  </div>
  <div v-else-if="state === 'empty'" class="module-state empty" role="status" aria-live="polite">
    <span aria-hidden="true">✓</span>
    <div><strong>{{ emptyMessage || '暂无业务数据' }}</strong><p>后续产生记录后会在这里显示。</p></div>
  </div>
  <slot v-else />
</template>

<style scoped>
.module-state{min-height:170px;padding:28px;display:flex;align-items:center;justify-content:center;gap:14px;text-align:left;border:1px dashed #cfe0dc;border-radius:16px;color:#456c68;background:#f8fbfa}.module-state>span{width:38px;height:38px;display:grid;place-items:center;flex:none;border-radius:12px;color:#0b746e;background:#e1f2ed;font-weight:800}.module-state strong,.module-state p{display:block}.module-state p{margin:5px 0 0;color:#7e9390;font-size:12px}.module-state button{margin-left:12px;padding:9px 14px;border:1px solid #0b746e;border-radius:10px;color:#0b746e;background:white;cursor:pointer}.module-state button:focus-visible{outline:3px solid #8fd4c8;outline-offset:2px}.module-state.error{border-color:#efcfd1;color:#9f3d45;background:#fff8f8}.module-state.error>span{color:#a74047;background:#fae2e4}.module-state.empty>span{color:#607c78;background:#eaf1ef}.state-spinner{border:3px solid #c9e4de;border-top-color:#0b746e;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
</style>
