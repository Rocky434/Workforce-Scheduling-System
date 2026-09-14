<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
const auth=useAuthStore(), route=useRoute(), router=useRouter()
const title=computed(()=>route.meta.title??'排班系統')
function logout(){auth.logout();router.push('/login')}
</script>
<template><div v-if="auth.user" class="app-shell"><header class="topbar"><div><span class="brand-mark">班</span><strong>好班表</strong></div><nav><RouterLink v-if="auth.user.role==='EMPLOYEE'" to="/schedule">我的班表</RouterLink><RouterLink v-if="auth.user.role==='OWNER'" to="/dashboard">管理總覽</RouterLink></nav><div class="profile"><span>{{ auth.user.displayName }} · {{ auth.user.role==='OWNER'?'老闆':'員工' }}</span><button class="ghost" @click="logout">登出</button></div></header><main><h1>{{ title }}</h1><RouterView /></main></div><RouterView v-else /></template>
