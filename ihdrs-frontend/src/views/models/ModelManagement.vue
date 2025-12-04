// views/models/ModelManagement.vue

<template>
  <div class="model-management">
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
              <DataAnalysis/>
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">模型管理</h1>
        <p class="header-subtitle">Model Management</p>
      </div>

      <!-- 搜索筛选区域 -->
      <el-card class="search-card modern-card" shadow="hover">
        <el-form :model="filterForm" inline>
          <el-form-item label="搜索">
            <el-input
                v-model="filterForm.keyword"
                placeholder="模型名称/版本/描述"
                clearable
                style="width: 250px"
                @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon>
                  <Search/>
                </el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item label="状态">
            <el-select
                v-model="filterForm.status"
                placeholder="全部状态"
                clearable
                style="width: 150px"
            >
              <el-option label="训练中" value="TRAINING"/>
              <el-option label="已完成" value="COMPLETED"/>
              <el-option label="活跃中" value="ACTIVE"/>
              <el-option label="已停用" value="DISABLED"/>
            </el-select>
          </el-form-item>
          <el-form-item label="模型类型">
            <el-select
                v-model="filterForm.modelType"
                placeholder="全部类型"
                clearable
                style="width: 150px"
            >
              <el-option label="CNN" value="CNN"/>
              <el-option label="ResNet" value="ResNet"/>
              <el-option label="VGG" value="VGG"/>
              <el-option label="MobileNet" value="MobileNet"/>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch" icon="Search">
              搜索
            </el-button>
            <el-button @click="resetFilter" icon="Refresh">
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <DataAnalysis/>
                </el-icon>
              </div>
              <div class="stat-label">总模型数</div>
              <div class="stat-value">{{ statistics.totalModels }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <CircleCheck/>
                </el-icon>
              </div>
              <div class="stat-label">活跃模型</div>
              <div class="stat-value">{{ statistics.activeModels }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Timer/>
                </el-icon>
              </div>
              <div class="stat-label">平均准确率</div>
              <div class="stat-value">{{ (statistics.avgAccuracy * 100).toFixed(2) }}%</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <TrendCharts/>
                </el-icon>
              </div>
              <div class="stat-label">最高准确率</div>
              <div class="stat-value">{{ (statistics.bestAccuracy * 100).toFixed(2) }}%</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 模型列表 -->
      <el-card class="table-card modern-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">模型列表</span>
            <div class="actions">
              <el-button
                  type="danger"
                  :disabled="selectedModels.length === 0"
                  @click="handleBatchDelete"
              >
                批量删除
              </el-button>
            </div>
          </div>
        </template>

        <el-table
            v-loading="loading"
            :data="modelList"
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55"/>

          <el-table-column prop="modelName" label="模型名称" min-width="180">
            <template #default="{ row }">
              <div class="model-name-cell">
                <!-- 活跃状态指示器 -->
                <span v-if="row.isActive" class="active-indicator">
        <span class="pulse-dot"></span>
      </span>

                <!-- 模型名称 -->
                <div class="model-info">
                  <div class="name-row">
          <span class="name-text" :class="{ 'active-name': row.isActive }">
            {{ row.modelName }}
          </span>
                    <el-tag v-if="row.isActive" type="success" size="small" effect="dark" class="active-tag">
                      <el-icon class="tag-icon">
                        <CircleCheck/>
                      </el-icon>
                      活跃
                    </el-tag>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column prop="modelVersion" label="版本" width="120"/>

          <el-table-column prop="modelType" label="类型" width="120"/>

          <el-table-column prop="accuracy" label="准确率" width="120">
            <template #default="{ row }">
              <el-progress
                  :percentage="parseFloat((row.accuracy * 100).toFixed(2))"
                  :color="getAccuracyColor(row.accuracy)"
              />
            </template>
          </el-table-column>

          <el-table-column prop="loss" label="损失值" width="120">
            <template #default="{ row }">
              {{ row.loss ? row.loss.toFixed(4) : '-' }}
            </template>
          </el-table-column>

          <el-table-column prop="trainingSamples" label="训练样本" width="120">
            <template #default="{ row }">
              {{ row.trainingSamples ? row.trainingSamples.toLocaleString() : '-' }}
            </template>
          </el-table-column>

          <el-table-column prop="modelSize" label="模型大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.modelSize) }}
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="creatorName" label="创建者" width="120"/>

          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" fixed="right" width="280">
            <template #default="{ row }">
              <el-button
                  size="small"
                  type="primary"
                  link
                  @click="viewDetail(row)"
              >
                详情
              </el-button>
              <el-button
                  v-if="!row.isActive && row.status === 'COMPLETED'"
                  size="small"
                  type="success"
                  link
                  @click="activateModel(row)"
              >
                激活
              </el-button>
              <el-button
                  v-if="row.status === 'COMPLETED' || row.status === 'ACTIVE'"
                  size="small"
                  type="warning"
                  link
                  @click="disableModel(row)"
              >
                停用
              </el-button>
              <el-button
                  v-if="row.status === 'DISABLED'"
                  size="small"
                  type="info"
                  link
                  @click="enableModel(row)"
              >
                启用
              </el-button>
              <el-button
                  size="small"
                  link
                  @click="viewVersions(row)"
              >
                版本
              </el-button>
              <el-button
                  v-if="!row.isActive"
                  size="small"
                  type="danger"
                  link
                  @click="deleteModel(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
              v-model:current-page="pagination.current"
              v-model:page-size="pagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

      <!-- 模型详情对话框 -->
      <el-dialog
          v-model="detailDialog.visible"
          title="模型详情"
          width="800px"
          :close-on-click-modal="false"
      >
        <el-descriptions :column="2" border v-if="detailDialog.model">
          <el-descriptions-item label="模型ID">
            {{ detailDialog.model.modelId }}
          </el-descriptions-item>
          <el-descriptions-item label="模型名称">
            {{ detailDialog.model.modelName }}
          </el-descriptions-item>
          <el-descriptions-item label="版本号">
            {{ detailDialog.model.modelVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="模型类型">
            {{ detailDialog.model.modelType }}
          </el-descriptions-item>
          <el-descriptions-item label="准确率">
            {{ (detailDialog.model.accuracy * 100).toFixed(2) }}%
          </el-descriptions-item>
          <el-descriptions-item label="损失值">
            {{ detailDialog.model.loss ? detailDialog.model.loss.toFixed(6) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="训练样本">
            {{ detailDialog.model.trainingSamples?.toLocaleString() || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="测试样本">
            {{ detailDialog.model.testSamples?.toLocaleString() || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="模型大小">
            {{ formatFileSize(detailDialog.model.modelSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(detailDialog.model.status)">
              {{ getStatusText(detailDialog.model.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建者">
            {{ detailDialog.model.creatorName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDate(detailDialog.model.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(detailDialog.model.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="是否活跃">
            <el-tag v-if="detailDialog.model.isActive" type="success">是</el-tag>
            <el-tag v-else type="info">否</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">
            {{ detailDialog.model.description || '无描述' }}
          </el-descriptions-item>
        </el-descriptions>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="detailDialog.visible = false">关闭</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 版本列表对话框 -->
      <el-dialog
          v-model="versionDialog.visible"
          title="模型版本列表"
          width="900px"
          :close-on-click-modal="false"
      >
        <el-table :data="versionDialog.versions" stripe>
          <el-table-column prop="modelVersion" label="版本" width="100"/>
          <el-table-column prop="accuracy" label="准确率" width="120">
            <template #default="{ row }">
              {{ (row.accuracy * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="accuracyImprovement" label="准确率提升" width="120">
            <template #default="{ row }">
            <span v-if="row.accuracyImprovement" :class="row.accuracyImprovement > 0 ? 'text-success' : 'text-danger'">
              {{ row.accuracyImprovement > 0 ? '+' : '' }}{{ row.accuracyImprovement }}%
            </span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="loss" label="损失值" width="120">
            <template #default="{ row }">
              {{ row.loss ? row.loss.toFixed(4) : '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button
                  v-if="!row.isActive && row.status === 'COMPLETED'"
                  size="small"
                  type="success"
                  @click="activateModelVersion(row)"
              >
                激活
              </el-button>
              <el-button
                  size="small"
                  type="primary"
                  @click="compareWithCurrent(row)"
              >
                对比
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <template #footer>
          <el-button @click="versionDialog.visible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 模型对比对话框 -->
      <el-dialog
          v-model="compareDialog.visible"
          title="模型对比"
          width="900px"
          :close-on-click-modal="false"
      >
        <div v-if="compareDialog.comparison">
          <el-row :gutter="20">
            <el-col :span="11">
              <h3>模型 1</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="模型名称">
                  {{ compareDialog.comparison.model1.modelName }}
                </el-descriptions-item>
                <el-descriptions-item label="版本">
                  {{ compareDialog.comparison.model1.modelVersion }}
                </el-descriptions-item>
                <el-descriptions-item label="准确率">
                  {{ (compareDialog.comparison.model1.accuracy * 100).toFixed(2) }}%
                </el-descriptions-item>
                <el-descriptions-item label="损失值">
                  {{ compareDialog.comparison.model1.loss?.toFixed(6) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="训练样本">
                  {{ compareDialog.comparison.model1.trainingSamples?.toLocaleString() || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="模型大小">
                  {{ formatFileSize(compareDialog.comparison.model1.modelSize) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
            <el-col :span="2" class="comparison-arrow">
              <el-icon :size="30">
                <Right/>
              </el-icon>
            </el-col>
            <el-col :span="11">
              <h3>模型 2</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="模型名称">
                  {{ compareDialog.comparison.model2.modelName }}
                </el-descriptions-item>
                <el-descriptions-item label="版本">
                  {{ compareDialog.comparison.model2.modelVersion }}
                </el-descriptions-item>
                <el-descriptions-item label="准确率">
                  {{ (compareDialog.comparison.model2.accuracy * 100).toFixed(2) }}%
                </el-descriptions-item>
                <el-descriptions-item label="损失值">
                  {{ compareDialog.comparison.model2.loss?.toFixed(6) || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="训练样本">
                  {{ compareDialog.comparison.model2.trainingSamples?.toLocaleString() || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="模型大小">
                  {{ formatFileSize(compareDialog.comparison.model2.modelSize) }}
                </el-descriptions-item>
              </el-descriptions>
            </el-col>
          </el-row>

          <el-divider/>

          <h3>对比结果</h3>
          <el-alert
              :title="compareDialog.comparison.comparison.recommendation"
              type="info"
              :closable="false"
              show-icon
          />

          <el-descriptions :column="2" border style="margin-top: 20px">
            <el-descriptions-item label="准确率差异">
            <span :class="compareDialog.comparison.comparison.accuracyDiff > 0 ? 'text-success' : 'text-danger'">
              {{ compareDialog.comparison.comparison.accuracyDiff > 0 ? '+' : '' }}
              {{ (compareDialog.comparison.comparison.accuracyDiff * 100).toFixed(2) }}%
            </span>
            </el-descriptions-item>
            <el-descriptions-item label="损失值差异">
            <span :class="compareDialog.comparison.comparison.lossDiff < 0 ? 'text-success' : 'text-danger'">
              {{ compareDialog.comparison.comparison.lossDiff > 0 ? '+' : '' }}
              {{ compareDialog.comparison.comparison.lossDiff?.toFixed(6) || '-' }}
            </span>
            </el-descriptions-item>
            <el-descriptions-item label="样本差异">
              {{ compareDialog.comparison.comparison.samplesDiff?.toLocaleString() || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="大小差异">
              {{ formatFileSize(compareDialog.comparison.comparison.sizeDiff) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <template #footer>
          <el-button @click="compareDialog.visible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  DataAnalysis,
  CircleCheck,
  Timer,
  TrendCharts,
  Search,
  Refresh,
  Plus,
  Right
} from '@element-plus/icons-vue'
import {
  getModelList,
  getActiveModel,
  switchActiveModel,
  disableModel as disableModelApi,
  enableModel as enableModelApi,
  deleteModel as deleteModelApi,
  batchDeleteModels,
  getModelVersions,
  compareModels,
  getModelStatistics
} from '@/api/model'

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

// 统计数据
const statistics = reactive({
  totalModels: 0,
  activeModels: 0,
  avgAccuracy: 0,
  bestAccuracy: 0
})

// 筛选表单
const filterForm = reactive({
  keyword: '',
  status: '',
  modelType: ''
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 模型列表
const modelList = ref([])
const loading = ref(false)
const selectedModels = ref([])

// 对话框
const detailDialog = reactive({
  visible: false,
  model: null
})

const versionDialog = reactive({
  visible: false,
  modelName: '',
  versions: []
})

const compareDialog = reactive({
  visible: false,
  comparison: null
})

const tableRowClassName = ({row}) => {
  if (row.isActive) {
    return 'active-model-row'
  }
  switch (row.status) {
    case 'TRAINING':
      return 'training-row'
    case 'COMPLETED':
      return 'completed-row'
    case 'ACTIVE':
      return 'active-row'
    case 'DISABLED':
      return 'disabled-row'
    default:
      return ''
  }
}

const getStatusDotClass = (status) => {
  const classes = {
    TRAINING: 'dot-training',
    COMPLETED: 'dot-completed',
    ACTIVE: 'dot-active',
    DISABLED: 'dot-disabled'
  }
  return classes[status] || ''
}

// 加载模型列表
const loadModelList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      ...filterForm
    }
    const res = await getModelList(params)
    if (res.code === 200) {
      modelList.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    ElMessage.error('加载模型列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计信息
const loadStatistics = async () => {
  try {
    const res = await getModelStatistics()
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch (error) {
    console.error('加载统计信息失败', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadModelList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.status = ''
  filterForm.modelType = ''
  handleSearch()
}

// 查看详情
const viewDetail = (model) => {
  detailDialog.model = model
  detailDialog.visible = true
}

// 激活模型
const activateModel = async (model) => {
  try {
    await ElMessageBox.confirm(
        `确定要将模型 "${model.modelName} ${model.modelVersion}" 设置为活跃状态吗？`,
        '激活模型',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const res = await switchActiveModel(model.modelId)
    if (res.code === 200) {
      ElMessage.success('模型已激活')
      loadModelList()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('激活模型失败')
    }
  }
}

// 停用模型
const disableModel = async (model) => {
  try {
    await ElMessageBox.confirm(
        `确定要停用模型 "${model.modelName} ${model.modelVersion}" 吗？`,
        '停用模型',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const res = await disableModelApi(model.modelId)
    if (res.code === 200) {
      ElMessage.success('模型已停用')
      loadModelList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停用模型失败')
    }
  }
}

// 启用模型
const enableModel = async (model) => {
  try {
    const res = await enableModelApi(model.modelId)
    if (res.code === 200) {
      ElMessage.success('模型已启用')
      loadModelList()
    }
  } catch (error) {
    ElMessage.error('启用模型失败')
  }
}

// 删除模型
const deleteModel = async (model) => {
  try {
    await ElMessageBox.confirm(
        `确定要删除模型 "${model.modelName} ${model.modelVersion}" 吗？此操作不可恢复！`,
        '删除模型',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'error'
        }
    )

    const res = await deleteModelApi(model.modelId)
    if (res.code === 200) {
      ElMessage.success('模型已删除')
      loadModelList()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除模型失败')
    }
  }
}

// 查看版本列表
const viewVersions = async (model) => {
  try {
    const res = await getModelVersions(model.modelName)
    if (res.code === 200) {
      versionDialog.modelName = model.modelName
      versionDialog.versions = res.data
      versionDialog.visible = true
    }
  } catch (error) {
    ElMessage.error('加载版本列表失败')
  }
}

// 激活指定版本
const activateModelVersion = async (version) => {
  await activateModel(version)
  versionDialog.visible = false
}

// 与当前版本对比
const compareWithCurrent = async (version) => {
  try {
    const activeRes = await getActiveModel()
    if (activeRes.code === 200) {
      const res = await compareModels(activeRes.data.modelId, version.modelId)
      if (res.code === 200) {
        compareDialog.comparison = res.data
        compareDialog.visible = true
        versionDialog.visible = false
      }
    }
  } catch (error) {
    ElMessage.error('模型对比失败')
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedModels.value.length} 个模型吗？`,
        '批量删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'error'
        }
    )

    const modelIds = selectedModels.value.map(m => m.modelId)
    const res = await batchDeleteModels(modelIds)
    if (res.code === 200) {
      ElMessage.success('批量删除成功')
      loadModelList()
      loadStatistics()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败')
    }
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedModels.value = selection
}

// 分页变化
const handleSizeChange = () => {
  loadModelList()
}

const handleCurrentChange = () => {
  loadModelList()
}

// 辅助函数
const getStatusType = (status) => {
  const types = {
    TRAINING: 'info',
    COMPLETED: '',
    ACTIVE: 'success',
    DISABLED: 'danger'
  }
  return types[status] || ''
}

const getStatusText = (status) => {
  const texts = {
    TRAINING: '训练中',
    COMPLETED: '已完成',
    ACTIVE: '活跃',
    DISABLED: '已停用'
  }
  return texts[status] || status
}

const getAccuracyColor = (accuracy) => {
  if (accuracy >= 0.95) return '#67C23A'
  if (accuracy >= 0.90) return '#E6A23C'
  return '#F56C6C'
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// 初始化
onMounted(() => {
  loadModelList()
  loadStatistics()
})
</script>

<style scoped lang="scss">
.model-management {
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

  .search-card {
    margin-bottom: 24px;
    padding: 8px 12px;
    overflow: visible;

    :deep(.el-card__body) {
      padding: 0;
    }

    :deep(.el-form) {
      display: flex;
      align-items: center;
      flex-wrap: nowrap;
      width: 100%;

      .el-form-item {
        margin-bottom: 0;
        margin-right: 20px;
        flex-shrink: 0;
        white-space: nowrap;

        &:first-child {
          margin-left: 20px;
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

  .table-card {
    margin-bottom: 24px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: bold;
        color: #303133;
      }

      .actions {
        display: flex;
        gap: 10px;
      }
    }

    .model-name {
      display: flex;
      align-items: center;
      gap: 10px;

      .name-text {
        font-weight: 500;
      }
    }

    .pagination-container {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .comparison-arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    padding-top: 100px;
  }

  .text-success {
    color: #67c23a;
  }

  .text-danger {
    color: #f56c6c;
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

// ==================== 模型列表行样式 ====================

// 活跃模型行 - 绿色高亮
:deep(.el-table .active-model-row) {
  background: linear-gradient(90deg, rgba(103, 194, 58, 0.12) 0%, rgba(103, 194, 58, 0.04) 100%) !important;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(180deg, #67c23a 0%, #85ce61 100%);
    box-shadow: 0 0 8px rgba(103, 194, 58, 0.6);
  }

  td {
    font-weight: 500;
  }
}

:deep(.el-table .active-model-row:hover > td) {
  background: rgba(103, 194, 58, 0.18) !important;
}

// 训练中行 - 蓝色动画
:deep(.el-table .training-row) {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.08) 0%, transparent 100%);
  animation: pulse-blue 2s infinite;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: #409eff;
  }
}

@keyframes pulse-blue {
  0%, 100% {
    background-color: rgba(64, 158, 255, 0.08);
  }
  50% {
    background-color: rgba(64, 158, 255, 0.15);
  }
}

// 已完成行 - 淡色
:deep(.el-table .completed-row) {
  background: rgba(144, 147, 153, 0.04);

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: #909399;
  }
}

// 已停用行 - 灰色弱化
:deep(.el-table .disabled-row) {
  background: rgba(144, 147, 153, 0.06);
  opacity: 0.65;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 4px;
    background: #c0c4cc;
  }

  td {
    color: #909399;
  }
}

:deep(.el-table .disabled-row:hover > td) {
  opacity: 1;
}

.model-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.active-indicator {
  position: relative;
  width: 12px;
  height: 12px;

  .pulse-dot {
    position: absolute;
    width: 12px;
    height: 12px;
    background: #67c23a;
    border-radius: 50%;

    &::before {
      content: '';
      position: absolute;
      width: 100%;
      height: 100%;
      background: #67c23a;
      border-radius: 50%;
      animation: pulse-ring 1.5s infinite;
    }
  }
}

@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

.model-info {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .name-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .name-text {
    font-size: 14px;
    color: #303133;
    transition: all 0.3s;

    &.active-name {
      font-weight: 600;
      color: #67c23a;
    }
  }

  .model-version {
    font-size: 12px;
    color: #909399;
  }
}

.active-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  animation: tag-glow 2s infinite;

  .tag-icon {
    font-size: 12px;
  }
}

@keyframes tag-glow {
  0%, 100% {
    box-shadow: 0 0 4px rgba(103, 194, 58, 0.4);
  }
  50% {
    box-shadow: 0 0 12px rgba(103, 194, 58, 0.8);
  }
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.dot-training {
    background: #409eff;
    animation: blink 1s infinite;
  }

  &.dot-completed {
    background: #909399;
  }

  &.dot-active {
    background: #67c23a;
    animation: blink 1.5s infinite;
  }

  &.dot-disabled {
    background: #c0c4cc;
  }
}

@keyframes blink {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}

:deep(.el-progress) {
  .el-progress-bar__outer {
    border-radius: 10px;
    overflow: hidden;
  }

  .el-progress-bar__inner {
    border-radius: 10px;
    transition: all 0.3s ease;
  }
}

// 高准确率闪光效果
:deep(.active-model-row .el-progress-bar__inner) {
  background: linear-gradient(90deg, #67c23a 0%, #85ce61 50%, #67c23a 100%);
  background-size: 200% 100%;
  animation: shimmer 2s infinite linear;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

.text-success {
  color: #67c23a;
  font-weight: 500;
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
}
</style>