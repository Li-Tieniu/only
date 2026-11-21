<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authState, clearLogin } from './stores/auth'

const router = useRouter()

const user = computed(() => authState.user)
const logged = computed(() => Boolean(authState.token))
const admin = computed(() => authState.user?.role === 'ADMIN')

function logout() {
  clearLogin()
  ElMessage.success('已退出登录')
  router.push('/products')
}
</script>

<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand" @click="router.push('/products')">游戏交易平台</div>

      <nav class="nav">
        <router-link to="/products">商品列表</router-link>
        <router-link v-if="logged" to="/orders">我的订单</router-link>
        <router-link v-if="admin" to="/admin/games">商品管理</router-link>
      </nav>

      <div class="user-area">
        <template v-if="logged">
          <span class="nickname">{{ user?.nickname || user?.username }}</span>
          <el-tag size="small" :type="admin ? 'danger' : 'info'">{{ user?.role }}</el-tag>
          <el-button link type="primary" @click="logout">退出</el-button>
        </template>
        <el-button v-else type="primary" size="small" @click="router.push('/login')">
          登录 / 注册
        </el-button>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>

    <el-footer class="app-footer">
      Vue 3 + Vite 前端 · Spring Boot 后端（app 8080 / console 8081）
    </el-footer>
  </el-container>
</template>
