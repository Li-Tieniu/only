<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listProducts } from '../api/game'
import { formatPrice } from '../utils/format'

const router = useRouter()
const loading = ref(false)
const products = ref([])

const query = reactive({
  keyword: '',
  minPrice: null,
  maxPrice: null,
  sort: ''
})

const sortOptions = [
  { label: '默认排序（最新发布）', value: '' },
  { label: '价格从低到高', value: 'price_asc' },
  { label: '价格从高到低', value: 'price_desc' }
]

async function load() {
  loading.value = true
  try {
    // 只把有值的参数传给后端，避免拼出无效查询条件
    const params = {}
    if (query.keyword) params.keyword = query.keyword
    if (query.minPrice !== null && query.minPrice !== '') params.min_price = query.minPrice
    if (query.maxPrice !== null && query.maxPrice !== '') params.max_price = query.maxPrice
    if (query.sort) params.sort = query.sort

    const data = await listProducts(params)
    products.value = data?.items || []
  } finally {
    loading.value = false
  }
}

function reset() {
  query.keyword = ''
  query.minPrice = null
  query.maxPrice = null
  query.sort = ''
  load()
}

function openDetail(item) {
  router.push(`/products/${item.itemId}`)
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <h2 class="page-title">商品列表</h2>
    <el-form :inline="true" :model="query" @submit.prevent>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="按游戏名称搜索" clearable style="width: 180px"
                  @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="价格区间">
        <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价"
                         style="width: 110px" />
        <span class="dash">-</span>
        <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价"
                         style="width: 110px" />
      </el-form-item>
      <el-form-item label="排序">
        <el-select v-model="query.sort" style="width: 190px">
          <el-option v-for="opt in sortOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </div>

  <div v-loading="loading" class="list-area">
    <el-empty v-if="!loading && products.length === 0" description="没有符合条件的商品" />

    <div v-else class="product-grid">
      <div v-for="item in products" :key="item.itemId" class="product-card" @click="openDetail(item)">
        <img :src="item.coverImage" :alt="item.gameName" />
        <div class="info">
          <div class="name">{{ item.gameName }}</div>
          <div class="price">{{ formatPrice(item.price) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.list-area {
  min-height: 240px;
}

.dash {
  margin: 0 6px;
  color: #909399;
}
</style>
