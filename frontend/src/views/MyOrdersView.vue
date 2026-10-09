<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrder, pageMyOrders, payOrder } from '../api/order'
import { formatPrice, orderStatusText, orderStatusType } from '../utils/format'

const loading = ref(false)
const paying = ref(false)
const orders = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detailOrder = ref(null)

const page = reactive({ current: 1, size: 5 })

async function load() {
  loading.value = true
  try {
    // 用户身份由后端从 Token 解析，前端不传 userId
    const data = await pageMyOrders({ page: page.current, page_size: page.size })
    orders.value = data?.items || []
    total.value = data?.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  // 调用后端详情接口（后端会校验订单归属）
  detailOrder.value = await getOrder(row.orderNo)
  detailVisible.value = true
}

async function handlePay(row) {
  await ElMessageBox.confirm(
    `确认支付订单 ${row.orderNo}，金额 ${formatPrice(row.price)}？`,
    '模拟支付',
    { confirmButtonText: '确认支付', cancelButtonText: '取消', type: 'warning' }
  )
  paying.value = true
  try {
    await payOrder(row.orderNo)
    ElMessage.success('支付成功')
    load()
  } finally {
    paying.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <h2 class="page-title">我的订单</h2>

    <el-table v-loading="loading" :data="orders" stripe style="width: 100%">
      <el-table-column prop="orderNo" label="订单号" min-width="200" show-overflow-tooltip />
      <el-table-column prop="gameName" label="商品" min-width="140" />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)" size="small">{{ orderStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="160" />
      <el-table-column label="支付时间" min-width="160">
        <template #default="{ row }">{{ row.payTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="success" :loading="paying"
                     @click="handlePay(row)">模拟支付</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && orders.length === 0" description="还没有订单，去商品列表逛逛吧" />

    <el-pagination
      v-if="total > 0"
      v-model:current-page="page.current"
      v-model:page-size="page.size"
      :total="total"
      :page-sizes="[5, 10, 20]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="load"
      @size-change="load"
    />
  </div>

  <el-dialog v-model="detailVisible" title="订单详情" width="480px">
    <el-descriptions v-if="detailOrder" :column="1" border>
      <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="商品名称">{{ detailOrder.gameName }}</el-descriptions-item>
      <el-descriptions-item label="商品编号">{{ detailOrder.itemId }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">{{ formatPrice(detailOrder.price) }}</el-descriptions-item>
      <el-descriptions-item label="订单状态">
        <el-tag :type="orderStatusType(detailOrder.status)" size="small">
          {{ orderStatusText(detailOrder.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="下单时间">{{ detailOrder.createTime }}</el-descriptions-item>
      <el-descriptions-item label="支付时间">{{ detailOrder.payTime || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>
