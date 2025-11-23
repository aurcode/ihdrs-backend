// views/dataset/DatasetList.vue

<template>
  <div class="dataset-list-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">我的数据集</h2>
        <p class="page-subtitle">管理和查看您的训练数据集</p>
      </div>

      <div class="header-right">
        <el-button
            type="primary"
            icon="Plus"
            size="large"
            class="upload-btn"
            @click="goToUpload"
        >
          上传数据集
        </el-button>
      </div>
    </div>

    <!-- 筛选和搜索 -->
    <div class="filter-bar">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="我的数据集" name="my"></el-tab-pane>
        <el-tab-pane label="公开数据集" name="public"></el-tab-pane>
      </el-tabs>

      <div class="filter-controls">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索数据集名称"
            prefix-icon="Search"
            clearable
            style="width: 300px"
            @input="handleSearch"
        />

        <el-select
            v-model="filterStatus"
            placeholder="状态筛选"
            clearable
            style="width: 150px; margin-left: 10px"
            @change="handleFilter"
        >
          <el-option label="全部" value=""></el-option>
          <el-option label="可用" value="AVAILABLE"></el-option>
          <el-option label="处理中" value="PROCESSING"></el-option>
          <el-option label="错误" value="ERROR"></el-option>
        </el-select>
      </div>
    </div>

    <!-- 数据集列表 -->
    <div v-loading="loading" class="dataset-content">
      <!-- 空状态 -->
      <el-empty
          v-if="!loading && datasetList.length === 0"
          :description="emptyDescription"
      >
        <el-button type="primary" @click="goToUpload" v-if="activeTab === 'my'">
          立即上传
        </el-button>
      </el-empty>

      <!-- 数据集网格 -->
      <div v-else class="dataset-grid">
        <dataset-card
            v-for="dataset in datasetList"
            :key="dataset.datasetId"
            :dataset="dataset"
            @view="handleView"
            @edit="handleEdit"
            @delete="handleDelete"
            @toggle-public="handleTogglePublic"
        />
      </div>

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑对话框 -->
    <el-dialog
        v-model="editDialogVisible"
        title="编辑数据集"
        width="500px"
        @close="handleEditDialogClose"
    >
      <el-form
          ref="editFormRef"
          :model="editForm"
          :rules="editFormRules"
          label-width="100px"
      >
        <el-form-item label="数据集名称" prop="datasetName">
          <el-input v-model="editForm.datasetName" placeholder="请输入数据集名称"/>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
              v-model="editForm.description"
              type="textarea"
              :rows="4"
              placeholder="请输入数据集描述"
          />
        </el-form-item>

        <el-form-item label="公开设置">
          <el-switch
              v-model="editForm.isPublic"
              active-text="公开"
              inactive-text="私有"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {Plus, Search} from '@element-plus/icons-vue'
import DatasetCard from '@/components/dataset/DatasetCard.vue'
import {getMyDatasets, getPublicDatasets, updateDataset, deleteDataset, setDatasetPublic} from '@/api/dataset'

const router = useRouter()

// 数据
const loading = ref(false)
const activeTab = ref('my')
const searchKeyword = ref('')
const filterStatus = ref('')
const datasetList = ref([])
const total = ref(0)

const pagination = reactive({
  page: 1,
  size: 10
})

// 编辑对话框
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const currentEditId = ref(null)
const editForm = reactive({
  datasetName: '',
  description: '',
  isPublic: false
})

const editFormRules = {
  datasetName: [
    {required: true, message: '请输入数据集名称', trigger: 'blur'},
    {min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur'}
  ]
}

// 计算属性
const emptyDescription = computed(() => {
  if (activeTab.value === 'my') {
    return searchKeyword.value ? '未找到匹配的数据集' : '还没有上传任何数据集'
  }
  return searchKeyword.value ? '未找到匹配的公开数据集' : '暂无公开数据集'
})

// 生命周期
onMounted(() => {
  loadDatasets()

  const interval = setInterval(() => {
    // 判断是否存在正在处理的 dataset
    const hasProcessing = datasetList.value.some(
        d => d.status === 'PROCESSING'
    )
    if (hasProcessing) {
      loadDatasets()
    }
  }, 5000)
})


// 方法
function loadDatasets() {
  loading.value = true

  const params = {
    page: pagination.page,
    size: pagination.size
  }

  const apiMethod = activeTab.value === 'my' ? getMyDatasets : getPublicDatasets

  apiMethod(params)
      .then(response => {
        if (response.code === 200) {
          const data = response.data
          datasetList.value = data.records || []
          total.value = data.total || 0

          // 应用本地筛选
          applyLocalFilter()
        } else {
          ElMessage.error(response.message || '加载数据集失败')
        }
      })
      .catch(error => {
        console.error('加载数据集失败:', error)
        ElMessage.error('加载数据集失败')
      })
      .finally(() => {
        loading.value = false
      })
}

function applyLocalFilter() {
  let filtered = [...datasetList.value]

  // 搜索筛选
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(item =>
        item.datasetName.toLowerCase().includes(keyword) ||
        (item.description && item.description.toLowerCase().includes(keyword))
    )
  }

  // 状态筛选
  if (filterStatus.value) {
    filtered = filtered.filter(item => item.status === filterStatus.value)
  }

  datasetList.value = filtered
}

function handleTabChange() {
  pagination.page = 1
  searchKeyword.value = ''
  filterStatus.value = ''
  loadDatasets()
}

function handleSearch() {
  applyLocalFilter()
}

function handleFilter() {
  applyLocalFilter()
}

function handlePageChange(page) {
  pagination.page = page
  loadDatasets()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadDatasets()
}

function goToUpload() {
  router.push('/dataset/upload')
}

function handleView(dataset) {
  router.push(`/dataset/detail/${dataset.datasetId}`)
}

function handleEdit(dataset) {
  currentEditId.value = dataset.datasetId
  editForm.datasetName = dataset.datasetName
  editForm.description = dataset.description || ''
  editForm.isPublic = dataset.isPublic
  editDialogVisible.value = true
}

function handleEditDialogClose() {
  editFormRef.value?.resetFields()
  currentEditId.value = null
}

function handleEditSubmit() {
  editFormRef.value?.validate((valid) => {
    if (valid) {
      editLoading.value = true

      const data = {
        datasetName: editForm.datasetName,
        description: editForm.description,
        datasetType: 'IMAGE_CLASSIFICATION',
        isPublic: editForm.isPublic
      }

      updateDataset(currentEditId.value, data)
          .then(response => {
            if (response.code === 200) {
              ElMessage.success('更新成功')
              editDialogVisible.value = false
              loadDatasets()
            } else {
              ElMessage.error(response.message || '更新失败')
            }
          })
          .catch(error => {
            console.error('更新失败:', error)
            ElMessage.error('更新失败')
          })
          .finally(() => {
            editLoading.value = false
          })
    }
  })
}

function handleTogglePublic(dataset) {
  const action = dataset.isPublic ? '设为私有' : '设为公开'
  const tip = dataset.isPublic
      ? '设为私有后，其他用户将无法查看和使用此数据集'
      : '设为公开后，所有用户都可以查看和使用此数据集'

  ElMessageBox.confirm(tip, `确认${action}？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
      .then(() => {
        setDatasetPublic(dataset.datasetId, !dataset.isPublic)
            .then(response => {
              if (response.code === 200) {
                ElMessage.success(`${action}成功`)
                loadDatasets()
              } else {
                ElMessage.error(response.message || `${action}失败`)
              }
            })
            .catch(error => {
              console.error(`${action}失败:`, error)
              ElMessage.error(`${action}失败`)
            })
      })
      .catch(() => {
        // 用户取消
      })
}

function handleDelete(dataset) {
  ElMessageBox.confirm(
      '删除后数据将无法恢复，是否继续？',
      '确认删除？',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  )
      .then(() => {
        deleteDataset(dataset.datasetId)
            .then(response => {
              if (response.code === 200) {
                ElMessage.success('删除成功')
                loadDatasets()
              } else {
                ElMessage.error(response.message || '删除失败')
              }
            })
            .catch(error => {
              console.error('删除失败:', error)
              ElMessage.error('删除失败')
            })
      })
      .catch(() => {
        // 用户取消
      })
}
</script>

<style scoped lang="scss">
.dataset-list-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  color: #333;
}

.page-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: #808080;
}

.upload-btn {
  padding: 0 18px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 8px;
}

.filter-bar {
  background: white;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;

  .filter-controls {
    display: flex;
    align-items: center;
    margin-top: 16px;
  }
}

.dataset-content {
  min-height: 400px;
}

.dataset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}
</style>