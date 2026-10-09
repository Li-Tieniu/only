<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { findProductDetail } from '../api/game'
import { createOrder } from '../api/order'
import { isLoggedIn } from '../stores/auth'
import { formatPrice } from '../utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const product = ref(null)

async function load() {
  loading.value = true
  try {
    product.value = await findProductDetail(route.params.itemId)
  } finally {
    loading.value = false
  }
}

async function handleCreateOrder() {
  if (!isLoggedIn()) {
    ElMessage.warning('请先登录后再下单')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  submitting.value = true
  try {
    const order = await createOrder(product.value.itemId)
    await ElMessageBox.confirm(
      `下单成功，订单号 ${order.orderNo}，金额 ${formatPrice(order.price)}。是否前往我的订单去支付？`,
      '下单成功',
      { confirmButtonText: '去支付', cancelButtonText: '留在本页', type: 'success' }
    )
    router.push('/orders')
  } catch (e) {
    // 用户点"留在本页"会走到这里，忽略即可；接口错误已由请求拦截器统一提示
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-page-header content="商品详情" @back="router.push('/products')" />

    <el-empty v-if="!loading && !product" description="商品不存在或已下架">
      <el-button type="primary" @click="router.push('/products')">返回商品列表</el-button>
    </el-empty>

    <div v-else-if="product" class="page-card detail-layout">
      <div>
        <el-carousel v-if="product.wallImages && product.wallImages.length" height="320px"
                     indicator-position="outside">
          <el-carousel-item v-for="(img, index) in product.wallImages" :key="index">
            <img :src="img" :alt="product.gameName" style="height: 320px; object-fit: cover" />
          </el-carousel-item>
        </el-carousel>
      </div>

      <div>
        <h2 class="page-title">{{ product.gameName }}</h2>
        <p class="detail-price">{{ formatPrice(product.price) }}</p>
        <el-descriptions :column="1" border size="small" style="margin: 14px 0">
          <el-descriptions-item label="商品编号">{{ product.itemId }}</el-descriptions-item>
          <el-descriptions-item label="上架时间">{{ product.createTime }}</el-descriptions-item>
          <el-descriptions-item label="最后更新">{{ product.updateTime }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-desc">{{ product.details }}</div>

        <div style="margin-top: 20px">
          <el-button type="primary" size="large" :loading="submitting" @click="handleCreateOrder">
            立即下单
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>
