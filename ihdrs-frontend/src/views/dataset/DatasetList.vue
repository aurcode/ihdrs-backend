// views/dataset/DatasetList.vue

<template>
  <div class="dataset-list-container">
    <!-- 动态背景粒子效果 -->
    <div class="background-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="getParticleStyle(i)"></div>
    </div>

    <!-- 背景装饰圆形 -->
    <div class="background-circles">
      <div class="circle circle-top"></div>
      <div class="circle circle-bottom"></div>
      <div class="circle circle-middle"></div>
    </div>

    <!-- 内容区域 -->
    <div class="content-wrapper">
      <!-- 头部Logo区域 -->
      <div class="header-section">
        <div class="logo-container">
          <div class="logo-circle">
            <el-icon size="50" color="#2563eb">
              <Folder />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">数据集管理</h1>
        <p class="header-subtitle">Dataset Management</p>
      </div>

      <!-- 筛选和搜索 -->
      <el-card class="filter-bar modern-card" shadow="hover">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="我的数据集" name="my"></el-tab-pane>
          <el-tab-pane label="公开数据集" name="public"></el-tab-pane>
        </el-tabs>

        <div class="filter-controls">
          <el-form :model="searchForm" inline>
            <el-form-item label="数据集名称">
              <el-input
                  v-model="searchKeyword"
                  placeholder="搜索数据集名称"
                  :prefix-icon="Search"
                  clearable
                  style="width: 300px"
                  @input="handleSearch"
              />
            </el-form-item>

            <el-form-item label="状态筛选">
              <el-select
                  v-model="filterStatus"
                  placeholder="请选择"
                  clearable
                  style="width: 150px"
                  @change="handleFilter"
              >
                <el-option label="全部" value=""></el-option>
                <el-option label="可用" value="AVAILABLE"></el-option>
                <el-option label="处理中" value="PROCESSING"></el-option>
                <el-option label="错误" value="ERROR"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :icon="Plus" @click="goToUpload">上传数据集</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-card>

      <!-- 数据统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Folder/>
                </el-icon>
              </div>
              <div class="stat-label">总数据集数</div>
              <div class="stat-value">{{ statistics.total || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Select/>
                </el-icon>
              </div>
              <div class="stat-label">可用数据集</div>
              <div class="stat-value">{{ statistics.available || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Loading/>
                </el-icon>
              </div>
              <div class="stat-label">处理中</div>
              <div class="stat-value">{{ statistics.processing || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Warning/>
                </el-icon>
              </div>
              <div class="stat-label">错误</div>
              <div class="stat-value">{{ statistics.error || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 数据集列表 -->
      <el-card class="dataset-content modern-card" shadow="hover" v-loading="loading">
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
      </el-card>
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
import {Plus, Search, Folder, Select, Loading, Warning} from '@element-plus/icons-vue'
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

// 搜索表单（用于样式统一）
const searchForm = reactive({
  keyword: '',
  status: ''
})

// 统计数据
const statistics = ref({
  total: 0,
  available: 0,
  processing: 0,
  error: 0
})

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


// 存储所有原始数据用于统计
const allDatasetsForStats = ref([])

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
          let allDatasets = data.records || []
          total.value = data.total || 0

          // 保存原始数据用于统计
          allDatasetsForStats.value = allDatasets

          // 应用本地筛选
          if (searchKeyword.value) {
            const keyword = searchKeyword.value.toLowerCase()
            allDatasets = allDatasets.filter(item =>
                item.datasetName.toLowerCase().includes(keyword) ||
                (item.description && item.description.toLowerCase().includes(keyword))
            )
          }

          if (filterStatus.value) {
            allDatasets = allDatasets.filter(item => item.status === filterStatus.value)
          }

          // 更新统计数据（使用原始数据，不受筛选影响）
          updateStatistics(allDatasetsForStats.value)

          datasetList.value = allDatasets
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

// 更新统计数据
function updateStatistics(allDatasets) {
  statistics.value.total = allDatasets.length
  statistics.value.available = allDatasets.filter(item => item.status === 'AVAILABLE').length
  statistics.value.processing = allDatasets.filter(item => item.status === 'PROCESSING').length
  statistics.value.error = allDatasets.filter(item => item.status === 'ERROR').length
}

function applyLocalFilter() {
  // 重新加载数据时会传入所有数据，这里需要从原始数据筛选
  loadDatasets()
}

function handleTabChange() {
  pagination.page = 1
  searchKeyword.value = ''
  filterStatus.value = ''
  loadDatasets()
}

function handleSearch() {
  pagination.page = 1
  loadDatasets()
}

function handleFilter() {
  pagination.page = 1
  loadDatasets()
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

// 粒子效果样式
const getParticleStyle = (index) => {
  const size = Math.random() * 3 + 1
  const x = Math.random() * 100
  const y = Math.random() * 100
  const duration = Math.random() * 3 + 2
  const delay = Math.random() * 2

  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${x}%`,
    top: `${y}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity: Math.random() * 0.3 + 0.1
  }
}
</script>

<style scoped lang="scss">
.dataset-list-container {
  position: relative;
  min-height: 100vh;
  padding: 24px;
  padding-bottom: 60px;
  overflow: visible;
  width: 100%;
  box-sizing: border-box;

  // 背景粒子效果
  .background-particles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;

    .particle {
      position: absolute;
      background: rgba(59, 130, 246, 0.3);
      border-radius: 50%;
      animation: float-particle infinite ease-in-out;
    }
  }

  // 背景装饰圆形
  .background-circles {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    z-index: 0;
    overflow: hidden;

    .circle {
      position: absolute;
      border-radius: 50%;
      animation: float-circle 20s ease-in-out infinite;

      &.circle-top {
        width: 600px;
        height: 600px;
        background: rgba(147, 197, 253, 0.2);
        top: -200px;
        right: -150px;
      }

      &.circle-bottom {
        width: 500px;
        height: 500px;
        background: rgba(191, 219, 254, 0.2);
        bottom: -150px;
        left: -100px;
        animation-duration: 25s;
        animation-direction: reverse;
      }

      &.circle-middle {
        width: 400px;
        height: 400px;
        background: rgba(224, 242, 254, 0.3);
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        animation-duration: 30s;
      }
    }
  }

  // 内容区域
  .content-wrapper {
    position: relative;
    z-index: 1;
  }

  // 头部Logo区域
  .header-section {
    text-align: center;
    margin-bottom: 30px;
    padding-top: 10px;

    .logo-container {
      margin-bottom: 10px;

      .logo-circle {
        width: 100px;
        height: 100px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.95);
        display: inline-flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        border: 3px solid rgba(59, 130, 246, 0.2);
        transition: all 0.3s ease;

        &:hover {
          transform: scale(1.05);
          box-shadow: 0 15px 40px rgba(0, 0, 0, 0.2);
        }

        .el-icon {
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
        }
      }
    }

    .header-title {
      font-size: 36px;
      font-weight: 800;
      color: #1e293b;
      margin: 0 0 10px 0;
      text-align: center;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      letter-spacing: 1px;
    }

    .header-subtitle {
      font-size: 18px;
      color: #475569;
      font-weight: 500;
      margin: 0;
      text-align: center;
      letter-spacing: 0.5px;
    }
  }

  // 深色模式适配
  html.dark & {
    .header-title {
      color: #ffffff;
      text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5), 0 0 20px rgba(255, 255, 255, 0.3);
    }

    .header-subtitle {
      color: #e2e8f0;
      text-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
    }
  }

  // 通用卡片样式
  .modern-card {
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(20px);
    border-radius: 28px;
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.5);
    transition: all 0.3s ease;
    overflow: visible;
    position: relative;
    z-index: 1;

    &:hover {
      transform: translateY(-6px);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
      background: rgba(255, 255, 255, 0.98);
    }
  }

  .filter-bar {
    margin-bottom: 24px;
    padding: 20px 24px;

    .filter-controls {
      margin-top: 16px;

      :deep(.el-form) {
        display: flex;
        align-items: center;
        flex-wrap: wrap;

        .el-form-item {
          margin-bottom: 0;
          margin-right: 20px;
        }
      }
    }
  }

  .stats-row {
    margin-bottom: 24px;

    .stat-card {
      position: relative;
      height: 130px;
      border-radius: 24px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      backdrop-filter: blur(10px);

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 16px 32px rgba(0, 0, 0, 0.15);

        .stat-background {
          transform: scale(1.1);
        }
      }

      .stat-background {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        transition: transform 0.3s ease;
      }

      .stat-content {
        position: relative;
        z-index: 2;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 16px;
        height: 100%;
        color: white;

        .stat-icon {
          font-size: 32px;
          margin-bottom: 0;
          filter: brightness(1.8);
        }

        .stat-label {
          font-size: 14px;
          opacity: 1;
          font-weight: 400;
          margin-bottom: 6px;
          margin-top: -6px;
          text-align: center;
        }

        .stat-value {
          font-size: 26px;
          font-weight: 700;
          line-height: 1;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }
      }

      // 第2个和第4个卡片的图标更亮
      &.success .stat-content .stat-icon,
      &.danger .stat-content .stat-icon {
        filter: brightness(2.2);
      }

      // 不同主题色
      &.primary .stat-background {
        background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
      }

      &.success .stat-background {
        background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
      }

      &.warning .stat-background {
        background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
      }

      &.danger .stat-background {
        background: linear-gradient(135deg, #ff6b9d 0%, #ff8fab 100%);
      }
    }
  }

  .dataset-content {
    min-height: 400px;
    padding: 24px;
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
    padding: 24px 0 0 0;
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
}

// 动画效果
@keyframes float-particle {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, -20px);
  }
}

@keyframes float-circle {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(30px, -30px) scale(1.1);
  }
}
</style>