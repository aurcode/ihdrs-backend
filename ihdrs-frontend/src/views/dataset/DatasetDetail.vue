// views/dataset/DatasetDetail.vue

<template>
  <div class="dataset-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button icon="ArrowLeft" @click="goBack">返回</el-button>
      <h2>数据集详情</h2>
    </div>

    <div v-loading="loading" class="detail-content">
      <template v-if="!loading && dataset">
        <!-- 基本信息卡片 -->
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon :size="24" color="#409EFF">
                  <Folder />
                </el-icon>
                <span class="dataset-name">{{ dataset.datasetName }}</span>
                <el-tag :type="statusInfo.type" size="small">
                  {{ statusInfo.text }}
                </el-tag>
                <el-tag v-if="dataset.isPublic" type="success" size="small">
                  公开
                </el-tag>
              </div>
              <div class="header-right">
                <el-button size="small" @click="handleEdit">编辑</el-button>
                <el-button
                    size="small"
                    type="danger"
                    @click="handleDelete"
                >
                  删除
                </el-button>
              </div>
            </div>
          </template>

          <div class="info-section">
            <h3>基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="数据集ID">
                {{ dataset.datasetId }}
              </el-descriptions-item>
              <el-descriptions-item label="数据集类型">
                {{ formattedType }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formattedCreateTime }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ formattedUpdateTime }}
              </el-descriptions-item>
              <el-descriptions-item label="文件大小" :span="2">
                {{ dataset.fileSizeFormatted }}
              </el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">
                {{ dataset.description || '暂无描述' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section">
            <h3>数据统计</h3>
            <el-row :gutter="20">
              <el-col :span="6">
                <div class="stat-card">
                  <div class="stat-value">{{ dataset.numClasses || '-' }}</div>
                  <div class="stat-label">类别数量</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card">
                  <div class="stat-value">{{ dataset.numSamples || '-' }}</div>
                  <div class="stat-label">样本总数</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card">
                  <div class="stat-value">{{ dataset.trainSamples || '-' }}</div>
                  <div class="stat-label">训练集样本</div>
                </div>
              </el-col>
              <el-col :span="6">
                <div class="stat-card">
                  <div class="stat-value">{{ dataset.testSamples || '-' }}</div>
                  <div class="stat-label">测试集样本</div>
                </div>
              </el-col>
            </el-row>
          </div>

          <div class="info-section" v-if="dataset.imageWidth && dataset.imageHeight">
            <h3>图像信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="图像尺寸">
                {{ dataset.imageWidth }} × {{ dataset.imageHeight }} 像素
              </el-descriptions-item>
              <el-descriptions-item label="图像格式">
                JPG, PNG, BMP
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section" v-if="dataset.classNames && dataset.classNames.length > 0">
            <h3>类别列表</h3>
            <div class="class-tags">
              <el-tag
                  v-for="(className, index) in dataset.classNames"
                  :key="index"
                  size="large"
                  style="margin: 4px"
              >
                {{ className }}
              </el-tag>
            </div>
          </div>

          <!-- 错误信息 -->
          <el-alert
              v-if="dataset.status === 'ERROR' && dataset.errorMessage"
              :title="dataset.errorMessage"
              type="error"
              :closable="false"
              style="margin-top: 20px"
          />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Folder, ArrowLeft } from '@element-plus/icons-vue'
import { getDatasetDetail, deleteDataset } from '@/api/dataset'
import { formatDateTime, formatDatasetStatus, formatDatasetType } from '@/utils/format'

const router = useRouter()
const route = useRoute()

// 数据
const loading = ref(false)
const dataset = ref(null)

// 计算属性
const statusInfo = computed(() => {
  return dataset.value ? formatDatasetStatus(dataset.value.status) : {}
})

const formattedType = computed(() => {
  return dataset.value ? formatDatasetType(dataset.value.datasetType) : '-'
})

const formattedCreateTime = computed(() => {
  return dataset.value ? formatDateTime(dataset.value.createTime) : '-'
})

const formattedUpdateTime = computed(() => {
  return dataset.value ? formatDateTime(dataset.value.updateTime) : '-'
})

// 生命周期
onMounted(() => {
  loadDataset()
})

// 方法
function loadDataset() {
  const datasetId = route.params.id
  if (!datasetId) {
    ElMessage.error('数据集ID不存在')
    goBack()
    return
  }

  loading.value = true
  getDatasetDetail(datasetId)
      .then(response => {
        if (response.code === 200) {
          dataset.value = response.data
        } else {
          ElMessage.error(response.message || '加载数据集详情失败')
          goBack()
        }
      })
      .catch(error => {
        console.error('加载数据集详情失败:', error)
        ElMessage.error('加载数据集详情失败')
        goBack()
      })
      .finally(() => {
        loading.value = false
      })
}

function goBack() {
  router.back()
}

function handleEdit() {
  router.push(`/dataset/list`)
  // 可以传递编辑标识，让列表页打开编辑对话框
}

function handleDelete() {
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
        deleteDataset(dataset.value.datasetId)
            .then(response => {
              if (response.code === 200) {
                ElMessage.success('删除成功')
                router.push('/dataset/list')
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
.dataset-detail-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
}

.detail-content {
  max-width: 1200px;
  margin: 0 auto;
}

.info-card {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .dataset-name {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }

    .header-right {
      display: flex;
      gap: 8px;
    }
  }
}

.info-section {
  margin-top: 24px;

  h3 {
    margin: 0 0 16px 0;
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }
}

.stat-card {
  background: linear-gradient(135deg, #aab8ff, #c7a8ff);
  border-radius: 8px;
  padding: 24px;
  text-align: center;
  color: white;

  .stat-value {
    font-size: 32px;
    font-weight: bold;
    margin-bottom: 8px;
  }

  .stat-label {
    font-size: 14px;
    opacity: 0.9;
  }

  &:nth-child(2) {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }

  &:nth-child(3) {
    background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  }

  &:nth-child(4) {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  }
}

.class-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>