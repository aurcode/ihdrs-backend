// views/models/Training.vue

<template>
  <div class="training-management">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card primary">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalTasks || 0 }}</div>
              <div class="stat-label">总任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card success">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.completedTasks || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card warning">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><Loading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.runningTasks || 0 }}</div>
              <div class="stat-label">训练中</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <el-card class="stat-card info">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ (statistics.avgAccuracy * 100).toFixed(2) }}%</div>
              <div class="stat-label">平均准确率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 过滤器 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form">
        <el-form-item label="任务名称">
          <el-input
              v-model="filterForm.keyword"
              placeholder="请输入任务名称"
              clearable
              style="width: 200px"
              @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="等待中" value="PENDING" />
            <el-option label="训练中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已失败" value="FAILED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch" :icon="Search">查询</el-button>
          <el-button @click="resetFilter" :icon="Refresh">重置</el-button>
          <el-button type="success" @click="showCreateDialog" :icon="Plus">新建训练任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 任务列表 -->
    <el-card class="table-card">
      <el-table
          v-loading="loading"
          :data="taskList"
          stripe
          style="width: 100%"
      >
        <el-table-column prop="taskName" label="任务名称" min-width="150" />

        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="训练进度" width="200">
          <template #default="{ row }">
            <div v-if="row.status === 'RUNNING' || row.status === 'COMPLETED'">
              <el-progress
                  :percentage="parseFloat(row.progress || 0)"
                  :status="row.status === 'COMPLETED' ? 'success' : undefined"
              />
              <div class="progress-text">
                Epoch: {{ row.currentEpoch || 0 }}/{{ row.totalEpochs }}
              </div>
            </div>
            <div v-else>-</div>
          </template>
        </el-table-column>

        <el-table-column prop="bestAccuracy" label="训练集最佳准确率" width="150">
          <template #default="{ row }">
            {{ row.bestAccuracy ? (row.bestAccuracy * 100).toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="finalAccuracy" label="最终准确率" width="120">
          <template #default="{ row }">
            {{ row.finalAccuracy ? (row.finalAccuracy * 100).toFixed(2) + '%' : '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>

            <el-button
                v-if="row.status === 'RUNNING'"
                size="small"
                type="warning"
                link
                @click="handleCancelTask(row)"
            >
              <el-icon><VideoPause /></el-icon>
              取消
            </el-button>

            <el-button
                v-if="row.status === 'RUNNING' || row.status === 'COMPLETED'"
                size="small"
                type="success"
                link
                @click="viewLogs(row)"
            >
              <el-icon><Document /></el-icon>
              日志
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

    <!-- 创建训练任务对话框 -->
    <el-dialog
        v-model="createDialog.visible"
        title="创建训练任务"
        width="700px"
        :close-on-click-modal="false"
    >
      <el-form
          ref="createFormRef"
          :model="createDialog.form"
          :rules="createDialog.rules"
          label-width="120px"
      >
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="createDialog.form.taskName" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="数据集" prop="datasetName">
          <el-select v-model="createDialog.form.datasetName" placeholder="请选择数据集">
            <el-option label="MNIST手写数字" value="MNIST" />
            <el-option label="Fashion-MNIST" value="FASHION_MNIST" />
            <el-option label="CIFAR-10" value="CIFAR10" />
          </el-select>
        </el-form-item>

        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="createDialog.form.modelType" placeholder="请选择模型类型">
            <el-option label="CNN" value="CNN" />
            <el-option label="Advanced CNN" value="ADVANCED_CNN" />
            <el-option label="ResNet" value="RESNET" />
            <el-option label="LeNet" value="LENET" />
          </el-select>
        </el-form-item>

        <el-form-item label="训练轮数" prop="totalEpochs">
          <el-input-number
              v-model="createDialog.form.totalEpochs"
              :min="1"
              :max="100"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="批次大小" prop="batchSize">
          <el-select v-model="createDialog.form.batchSize" placeholder="请选择批次大小">
            <el-option label="16" :value="16" />
            <el-option label="32" :value="32" />
            <el-option label="64" :value="64" />
            <el-option label="128" :value="128" />
          </el-select>
        </el-form-item>

        <el-form-item label="学习率" prop="learningRate">
          <el-select v-model="createDialog.form.learningRate" placeholder="请选择学习率">
            <el-option label="0.0001" value="0.0001" />
            <el-option label="0.001" value="0.001" />
            <el-option label="0.01" value="0.01" />
          </el-select>
        </el-form-item>

        <el-form-item label="优化器" prop="optimizer">
          <el-select v-model="createDialog.form.optimizer" placeholder="请选择优化器">
            <el-option label="Adam" value="adam" />
            <el-option label="SGD" value="sgd" />
            <el-option label="RMSprop" value="rmsprop" />
          </el-select>
        </el-form-item>

        <el-form-item label="损失函数" prop="lossFunction">
          <el-select v-model="createDialog.form.lossFunction" placeholder="请选择损失函数">
            <el-option label="Categorical Crossentropy" value="categorical_crossentropy" />
            <el-option label="Sparse Categorical Crossentropy" value="sparse_categorical_crossentropy" />
          </el-select>
        </el-form-item>

        <el-form-item label="激活函数" prop="activation">
          <el-select v-model="createDialog.form.activation" placeholder="请选择激活函数">
            <el-option label="ReLU" value="relu" />
            <el-option label="Sigmoid" value="sigmoid" />
            <el-option label="Tanh" value="tanh" />
          </el-select>
        </el-form-item>

        <el-form-item label="Dropout率" prop="dropout">
          <el-select v-model="createDialog.form.dropout" placeholder="请选择Dropout率">
            <el-option label="0.0" value="0.0" />
            <el-option label="0.2" value="0.2" />
            <el-option label="0.3" value="0.3" />
            <el-option label="0.5" value="0.5" />
          </el-select>
        </el-form-item>

        <el-form-item label="隐藏层大小" prop="hiddenSize">
          <el-input-number
              v-model="createDialog.form.hiddenSize"
              :min="32"
              :max="1024"
              :step="32"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="验证集比例" prop="validationSplit">
          <el-select v-model="createDialog.form.validationSplit" placeholder="请选择验证集比例">
            <el-option label="10%" value="0.1" />
            <el-option label="15%" value="0.15" />
            <el-option label="20%" value="0.2" />
          </el-select>
        </el-form-item>

        <el-form-item label="数据增强" prop="useAugmentation">
          <el-switch v-model="createDialog.form.useAugmentation" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialog.visible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateTask" :loading="createDialog.loading">
            创建
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 任务详情对话框 -->
    <el-dialog
        v-model="detailDialog.visible"
        title="训练任务详情"
        width="900px"
        :close-on-click-modal="false"
    >
      <el-descriptions :column="2" border v-if="detailDialog.task">
        <el-descriptions-item label="任务ID">
          {{ detailDialog.task.taskId }}
        </el-descriptions-item>

        <el-descriptions-item label="任务名称">
          {{ detailDialog.task.taskName }}
        </el-descriptions-item>

        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailDialog.task.status)">
            {{ getStatusText(detailDialog.task.status) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="训练进度">
          {{ detailDialog.task.progress }}%
        </el-descriptions-item>

        <el-descriptions-item label="当前轮次">
          {{ detailDialog.task.currentEpoch || 0 }} / {{ detailDialog.task.totalEpochs }}
        </el-descriptions-item>

        <el-descriptions-item label="训练集最佳准确率">
          {{ detailDialog.task.bestAccuracy ? (detailDialog.task.bestAccuracy * 100).toFixed(2) + '%' : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="最终准确率">
          {{ detailDialog.task.finalAccuracy ? (detailDialog.task.finalAccuracy * 100).toFixed(2) + '%' : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="最终损失">
          {{ detailDialog.task.finalLoss ? detailDialog.task.finalLoss.toFixed(6) : '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="开始时间">
          {{ formatDate(detailDialog.task.startTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="结束时间">
          {{ formatDate(detailDialog.task.endTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间">
          {{ formatDate(detailDialog.task.createTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="更新时间">
          {{ formatDate(detailDialog.task.updateTime) }}
        </el-descriptions-item>

        <el-descriptions-item label="错误信息" :span="2" v-if="detailDialog.task.errorMessage">
          <el-text type="danger">{{ detailDialog.task.errorMessage }}</el-text>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 训练日志对话框 -->
    <el-dialog
        v-model="logsDialog.visible"
        title="训练日志"
        width="1000px"
        :close-on-click-modal="false"
    >
      <div class="charts-container" v-loading="logsDialog.loading">
        <!-- 准确率曲线 -->
        <div class="chart-item">
          <h4>准确率曲线</h4>
          <v-chart :option="accuracyChartOption" autoresize style="height: 300px" />
        </div>

        <!-- 损失曲线 -->
        <div class="chart-item">
          <h4>损失曲线</h4>
          <v-chart :option="lossChartOption" autoresize style="height: 300px" />
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="logsDialog.visible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getTrainingTaskList,
  createTrainingTask,
  getTrainingTaskDetail,
  getTrainingLogs,
  cancelTrainingTask
} from '@/api/training'
import {
  DataAnalysis,
  CircleCheck,
  Loading,
  TrendCharts,
  Search,
  Refresh,
  Plus,
  View,
  VideoPause,
  Document
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'

// 注册ECharts组件
use([
  CanvasRenderer,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

// 数据
const loading = ref(false)
const taskList = ref([])
const statistics = ref({
  totalTasks: 0,
  completedTasks: 0,
  runningTasks: 0,
  avgAccuracy: 0
})

const filterForm = reactive({
  keyword: '',
  status: ''
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const createDialog = reactive({
  visible: false,
  loading: false,
  form: {
    taskName: '',
    datasetName: 'MNIST',
    modelType: 'CNN',
    totalEpochs: 10,
    batchSize: 32,
    learningRate: '0.001',
    optimizer: 'adam',
    lossFunction: 'categorical_crossentropy',
    activation: 'relu',
    dropout: '0.2',
    hiddenSize: 128,
    validationSplit: '0.2',
    useAugmentation: false
  },
  rules: {
    taskName: [
      { required: true, message: '请输入任务名称', trigger: 'blur' }
    ],
    datasetName: [
      { required: true, message: '请选择数据集', trigger: 'change' }
    ],
    modelType: [
      { required: true, message: '请选择模型类型', trigger: 'change' }
    ],
    totalEpochs: [
      { required: true, message: '请输入训练轮数', trigger: 'blur' }
    ],
    batchSize: [
      { required: true, message: '请选择批次大小', trigger: 'change' }
    ],
    learningRate: [
      { required: true, message: '请选择学习率', trigger: 'change' }
    ]
  }
})

const createFormRef = ref()

const detailDialog = reactive({
  visible: false,
  task: null
})

const logsDialog = reactive({
  visible: false,
  loading: false,
  logs: []
})

// 图表配置
const accuracyChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const trainAcc = logsDialog.logs.map(log => parseFloat((log.accuracy * 100).toFixed(2)))
  const valAcc = logsDialog.logs.map(log => parseFloat((log.valAccuracy * 100).toFixed(2)))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#606266' }
    },
    legend: {
      data: ['训练准确率', '验证准确率'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '准确率 (%)',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f5f7fa' } }
    },
    series: [
      {
        name: '训练准确率',
        type: 'line',
        data: trainAcc,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#409EFF' },
        itemStyle: { color: '#409EFF' }
      },
      {
        name: '验证准确率',
        type: 'line',
        data: valAcc,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#67C23A' },
        itemStyle: { color: '#67C23A' }
      }
    ]
  }
})

const lossChartOption = computed(() => {
  const epochs = logsDialog.logs.map(log => log.epoch)
  const trainLoss = logsDialog.logs.map(log => parseFloat(log.loss.toFixed(4)))
  const valLoss = logsDialog.logs.map(log => parseFloat(log.valLoss.toFixed(4)))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#606266' }
    },
    legend: {
      data: ['训练损失', '验证损失'],
      bottom: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: epochs,
      name: 'Epoch',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: {
      type: 'value',
      name: '损失',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399' },
      splitLine: { lineStyle: { color: '#f5f7fa' } }
    },
    series: [
      {
        name: '训练损失',
        type: 'line',
        data: trainLoss,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#E6A23C' },
        itemStyle: { color: '#E6A23C' }
      },
      {
        name: '验证损失',
        type: 'line',
        data: valLoss,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { width: 3, color: '#F56C6C' },
        itemStyle: { color: '#F56C6C' }
      }
    ]
  }
})

// 方法
const loadTaskList = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      status: filterForm.status || undefined
    }

    const response = await getTrainingTaskList(params)
    if (response.code === 200) {
      taskList.value = response.data.records
      pagination.total = response.data.total

      // 更新统计数据
      updateStatistics()
    }
  } catch (error) {
    console.error('获取训练任务列表失败', error)
    ElMessage.error('获取训练任务列表失败')
  } finally {
    loading.value = false
  }
}

const updateStatistics = () => {
  statistics.value.totalTasks = taskList.value.length
  statistics.value.completedTasks = taskList.value.filter(t => t.status === 'COMPLETED').length
  statistics.value.runningTasks = taskList.value.filter(t => t.status === 'RUNNING').length

  const completedTasks = taskList.value.filter(t => t.finalAccuracy)
  if (completedTasks.length > 0) {
    const totalAcc = completedTasks.reduce((sum, t) => sum + t.finalAccuracy, 0)
    statistics.value.avgAccuracy = totalAcc / completedTasks.length
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadTaskList()
}

const resetFilter = () => {
  filterForm.keyword = ''
  filterForm.status = ''
  handleSearch()
}

const handleSizeChange = (size) => {
  pagination.size = size
  loadTaskList()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  loadTaskList()
}

const showCreateDialog = () => {
  createDialog.visible = true
  createDialog.form = {
    taskName: '',
    datasetName: 'MNIST',
    modelType: 'CNN',
    totalEpochs: 10,
    batchSize: 32,
    learningRate: '0.001',
    optimizer: 'adam',
    lossFunction: 'categorical_crossentropy',
    activation: 'relu',
    dropout: '0.2',
    hiddenSize: 128,
    validationSplit: '0.2',
    useAugmentation: false
  }
}

const handleCreateTask = async () => {
  try {
    await createFormRef.value.validate()
    createDialog.loading = true

    const response = await createTrainingTask(createDialog.form)
    if (response.code === 200) {
      ElMessage.success('训练任务创建成功')
      createDialog.visible = false
      loadTaskList()
    }
  } catch (error) {
    console.error('创建训练任务失败', error)
    if (error !== false) {
      ElMessage.error('创建训练任务失败')
    }
  } finally {
    createDialog.loading = false
  }
}

const viewDetail = async (row) => {
  try {
    const response = await getTrainingTaskDetail(row.taskId)
    if (response.code === 200) {
      detailDialog.task = response.data
      detailDialog.visible = true
    }
  } catch (error) {
    console.error('获取任务详情失败', error)
    ElMessage.error('获取任务详情失败')
  }
}

const viewLogs = async (row) => {
  logsDialog.loading = true
  logsDialog.visible = true

  try {
    const response = await getTrainingLogs(row.taskId)
    if (response.code === 200) {
      logsDialog.logs = response.data
    }
  } catch (error) {
    console.error('获取训练日志失败', error)
    ElMessage.error('获取训练日志失败')
  } finally {
    logsDialog.loading = false
  }
}

const handleCancelTask = async (row) => {
  try {
    await ElMessageBox.confirm(
        '确定要取消该训练任务吗？',
        '取消确认',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const response = await cancelTrainingTask(row.taskId)
    if (response.code === 200) {
      ElMessage.success('训练任务已取消')
      loadTaskList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消训练任务失败', error)
      ElMessage.error('取消训练任务失败')
    }
  }
}

const getStatusType = (status) => {
  const statusMap = {
    'PENDING': 'info',
    'RUNNING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '等待中',
    'RUNNING': '训练中',
    'COMPLETED': '已完成',
    'FAILED': '已失败',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status
}

const formatDate = (date) => {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

// 生命周期
onMounted(() => {
  loadTaskList()

  // 每2秒刷新一次列表（如果有正在运行的任务）
  setInterval(() => {
    const hasRunning = taskList.value.some(t => t.status === 'RUNNING')
    if (hasRunning) {
      loadTaskList()
    }
  }, 2000)
})
</script>

<style lang="scss" scoped>
.training-management {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  :deep(.el-card__body) {
    padding: 20px;
  }

  .stat-content {
    display: flex;
    align-items: center;

    .stat-icon {
      font-size: 40px;
      margin-right: 20px;
      color: #409EFF;
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 28px;
        font-weight: bold;
        color: #303133;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  &.primary .stat-icon { color: #409EFF; }
  &.success .stat-icon { color: #67C23A; }
  &.warning .stat-icon { color: #E6A23C; }
  &.info .stat-icon { color: #909399; }
}

.filter-card {
  margin-bottom: 20px;

  .filter-form {
    margin-bottom: 0;
  }
}

.table-card {
  .progress-text {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.charts-container {
  .chart-item {
    margin-bottom: 30px;

    h4 {
      margin-bottom: 10px;
      color: #303133;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
