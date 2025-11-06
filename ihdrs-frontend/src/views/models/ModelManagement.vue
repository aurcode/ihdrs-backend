<template>
  <div class="model-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>模型管理</h1>
      <div class="header-actions">
        <el-button type="primary" icon="Refresh" @click="loadModelList">
          刷新
        </el-button>
        <el-button type="success" icon="Plus" @click="showCreateDialog">
          新建训练任务
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="statistics-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon primary">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ statistics.totalModels }}</div>
              <div class="stat-label">总模型数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon success">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ statistics.activeModels }}</div>
              <div class="stat-label">活跃模型</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon warning">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ (statistics.avgAccuracy * 100).toFixed(2) }}%</div>
              <div class="stat-label">平均准确率</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon info">
              <el-icon><TrendCharts /></el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-value">{{ (statistics.bestAccuracy * 100).toFixed(2) }}%</div>
              <div class="stat-label">最高准确率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索和筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="搜索">
          <el-input
              v-model="filterForm.keyword"
              placeholder="模型名称/版本/描述"
              clearable
              style="width: 250px"
              @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
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
            <el-option label="训练中" value="TRAINING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="活跃中" value="ACTIVE" />
            <el-option label="已停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select
              v-model="filterForm.modelType"
              placeholder="全部类型"
              clearable
              style="width: 150px"
          >
            <el-option label="CNN" value="CNN" />
            <el-option label="ResNet" value="ResNet" />
            <el-option label="LeNet" value="LeNet" />
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

    <!-- 模型列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>模型列表</span>
          <div class="header-buttons">
            <el-button
                type="danger"
                size="small"
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
        <el-table-column type="selection" width="55" />

        <el-table-column prop="modelName" label="模型名称" min-width="150">
          <template #default="{ row }">
            <div class="model-name">
              <el-tag v-if="row.isActive" type="success" size="small">
                活跃
              </el-tag>
              <span class="name-text">{{ row.modelName }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="modelVersion" label="版本" width="120" />

        <el-table-column prop="modelType" label="类型" width="100" />

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

        <el-table-column prop="creatorName" label="创建者" width="120" />

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
          <el-button type="primary" @click="editModel">编辑</el-button>
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
        <el-table-column prop="modelVersion" label="版本" width="100" />
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
            <el-icon :size="30"><Right /></el-icon>
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

        <el-divider />

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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
        `确定要将模型 "${model.modelName} v${model.modelVersion}" 设置为活跃状态吗？`,
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
        `确定要停用模型 "${model.modelName} v${model.modelVersion}" 吗？`,
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
        `确定要删除模型 "${model.modelName} v${model.modelVersion}" 吗？此操作不可恢复！`,
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

// 编辑模型
const editModel = () => {
  // TODO: 实现编辑功能
  ElMessage.info('编辑功能开发中')
}

// 显示创建对话框
const showCreateDialog = () => {
  // TODO: 跳转到训练任务创建页面
  ElMessage.info('请前往训练管理创建新任务')
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
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .statistics-row {
    margin-bottom: 20px;

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        gap: 15px;

        .stat-icon {
          width: 60px;
          height: 60px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 30px;

          &.primary {
            background: #ecf5ff;
            color: #409eff;
          }

          &.success {
            background: #f0f9ff;
            color: #67c23a;
          }

          &.warning {
            background: #fdf6ec;
            color: #e6a23c;
          }

          &.info {
            background: #f4f4f5;
            color: #909399;
          }
        }

        .stat-text {
          flex: 1;

          .stat-value {
            font-size: 28px;
            font-weight: 600;
            margin-bottom: 5px;
          }

          .stat-label {
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }

  .filter-card {
    margin-bottom: 20px;

    .filter-form {
      margin-bottom: 0;
    }
  }

  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-buttons {
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
</style>