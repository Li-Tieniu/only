<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminAddGame,
  adminPageDetails,
  adminSoftDeleteGame,
  adminUpdateGame
} from '../api/game'
import { formatPrice } from '../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const total = ref(0)
const page = reactive({ current: 1, size: 10 })

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const formRef = ref()

const form = reactive({
  itemId: null,
  gameName: '',
  price: '',
  coverImage: '',
  wallImages: '',
  details: ''
})

// 校验规则和后端 GameSaveDTO 上的约束保持一致
const rules = {
  gameName: [
    { required: true, message: '请输入游戏名称', trigger: 'blur' },
    { max: 50, message: '游戏名称长度不能超过 50', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' },
    {
      pattern: /^\d+(\.\d{1,2})?$/,
      message: '价格必须是不小于 0 的数字，最多两位小数',
      trigger: 'blur'
    }
  ],
  coverImage: [{ max: 2000, message: '封面图地址长度不能超过 2000', trigger: 'blur' }],
  wallImages: [{ max: 2000, message: '详情图地址长度不能超过 2000', trigger: 'blur' }],
  details: [{ max: 2000, message: '商品详情长度不能超过 2000', trigger: 'blur' }]
}

async function load() {
  loading.value = true
  try {
    const detailPage = await adminPageDetails({ page: page.current, page_size: page.size })
    total.value = detailPage?.total || 0
    // 下架后若当前页已超出末页，回到仍有数据的页码。
    const lastPage = Math.max(1, Math.ceil(total.value / page.size))
    if (page.current > lastPage) {
      page.current = lastPage
      return await load()
    }
    rows.value = (detailPage?.items || []).map((detail) => ({
      ...detail,
      wallImagesText: (detail.wallImages || []).join('$')
    }))
  } finally {
    loading.value = false
  }
}

function changePage(current) {
  if (page.current === current) return
  page.current = current
  return load()
}

function openCreate() {
  dialogTitle.value = '新增商品'
  Object.assign(form, {
    itemId: null,
    gameName: '',
    price: '',
    coverImage: '',
    wallImages: '',
    details: ''
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

function openEdit(row) {
  dialogTitle.value = `编辑商品 #${row.itemId}`
  Object.assign(form, {
    itemId: row.itemId,
    gameName: row.gameName,
    price: row.price,
    coverImage: row.coverImage,
    wallImages: row.wallImagesText,
    details: row.details
  })
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  const payload = {
    gameName: form.gameName,
    price: form.price,
    coverImage: form.coverImage,
    wallImages: form.wallImages,
    details: form.details
  }
  try {
    if (form.itemId) {
      await adminUpdateGame({ ...payload, itemId: form.itemId })
      ElMessage.success('商品已更新（相关缓存已由后端清理）')
    } else {
      const newId = await adminAddGame(payload)
      ElMessage.success(`商品新增成功，ID=${newId}`)
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function softDelete(row) {
  await ElMessageBox.confirm(
    `确认下架（逻辑删除）商品「${row.gameName}」吗？数据仍保留在数据库中，只是不再对用户展示。`,
    '下架确认',
    { confirmButtonText: '确认下架', cancelButtonText: '取消', type: 'warning' }
  )
  await adminSoftDeleteGame(row.itemId)
  ElMessage.success('已下架')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="header-row">
      <h2 class="page-title">商品管理</h2>
      <el-button type="primary" @click="openCreate">新增商品</el-button>
    </div>
    <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 12px"
              title="本页仅 ADMIN 可用（前端判断只是体验优化，后端 Security 才是真正的权限控制）。为保证数据安全，未提供物理删除入口。" />

    <el-table v-loading="loading" :data="rows" stripe style="width: 100%">
      <el-table-column label="封面" width="90">
        <template #default="{ row }">
          <img v-if="row.coverImage" :src="row.coverImage" alt="cover" class="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="itemId" label="ID" width="70" />
      <el-table-column prop="gameName" label="游戏名称" min-width="130" />
      <el-table-column label="价格" width="110">
        <template #default="{ row }">{{ formatPrice(row.price) }}</template>
      </el-table-column>
      <el-table-column prop="details" label="商品详情" min-width="220" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="最后更新" min-width="160" />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="softDelete(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && rows.length === 0" description="暂无商品" />
    <el-pagination
      :current-page="page.current"
      :page-size="page.size"
      :total="total"
      :disabled="loading"
      layout="total, prev, pager, next"
      style="margin-top: 16px; justify-content: flex-end"
      @current-change="changePage"
    />
  </div>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="游戏名称" prop="gameName">
        <el-input v-model="form.gameName" placeholder="例如：王者荣耀" />
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input v-model="form.price" placeholder="例如：888.00" />
      </el-form-item>
      <el-form-item label="封面图" prop="coverImage">
        <el-input v-model="form.coverImage" placeholder="列表展示小图 URL" />
      </el-form-item>
      <el-form-item label="详情大图" prop="wallImages">
        <el-input v-model="form.wallImages" type="textarea" :rows="2"
                  placeholder="多张图片用 $ 分隔" />
      </el-form-item>
      <el-form-item label="商品详情" prop="details">
        <el-input v-model="form.details" type="textarea" :rows="4" placeholder="账号描述、亮点等" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.cover {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  background: #f0f2f5;
}
</style>
