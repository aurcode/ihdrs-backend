// views/dashboard/index.vue

<template>
  <div class="dashboard-container">

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card primary">
          <div class="stat-icon">
            <el-icon><View /></el-icon>
          </div>
          <div class="stat-value">{{ stats.totalRecognitions || 0 }}</div>
          <div class="stat-label">总识别次数</div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card success">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-label">注册用户</div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card warning">
          <div class="stat-icon">
            <el-icon><Setting /></el-icon>
          </div>
          <div class="stat-value">{{ stats.totalModels || 0 }}</div>
          <div class="stat-label">训练模型</div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card danger">
          <div class="stat-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-value">{{ stats.todayRecognitions || 0 }}</div>
          <div class="stat-label">今日识别</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <!-- 识别趋势图 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>识别趋势</span>
              <el-button type="text" @click="refreshCharts">刷新</el-button>
            </div>
          </template>
          <div class="chart-container" v-loading="chartsLoading">
            <v-chart
                :option="recognitionTrendOption"
                :autoresize="true"
                style="height: 300px;"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 准确率分布图 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>数字识别分布</span>
            </div>
          </template>
          <div class="chart-container" v-loading="chartsLoading">
            <v-chart
                :option="digitDistributionOption"
                :autoresize="true"
                style="height: 300px;"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快速操作 -->
    <el-row :gutter="20" class="quick-actions-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <span>快速操作</span>
          </template>
          <div class="quick-actions">
            <el-button
                type="primary"
                @click="goToTraining"
            >
              开始训练模型
            </el-button>
            <el-button
                type="success"
                @click="goToRecognition"
            >
              查看识别记录
            </el-button>
            <el-button
                type="warning"
                @click="goToUsers"
                v-if="userStore.isAdmin"
            >
              用户管理
            </el-button>
            <el-button
                type="info"
                @click="goToModels"
                v-if="userStore.isAdmin"
            >
              模型管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-row :gutter="20" class="activity-row">
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card>
          <template #header>
            <span>最近识别记录</span>
          </template>
          <div class="activity-list" v-loading="activitiesLoading">
            <div
                v-for="record in recentRecognitions"
                :key="record.id"
                class="activity-item"
            >
              <div class="activity-icon">
                <el-icon color="#409EFF"><View /></el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-title">识别数字: {{ record.result }}</div>
                <div class="activity-meta">
                  置信度: {{ (record.confidence * 100).toFixed(1) }}% |
                  {{ formatTime(record.createTime) }}
                </div>
              </div>
            </div>
            <div v-if="recentRecognitions.length === 0" class="empty-activity">
              暂无识别记录
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 系统状态 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
        <el-card>
          <template #header>
            <span>系统状态</span>
          </template>
          <div class="system-status">
            <div class="status-item">
              <span class="status-label">后端服务</span>
              <el-tag :type="systemStatus.backend ? 'success' : 'danger'">
                {{ systemStatus.backend ? '正常' : '异常' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">模型服务</span>
              <el-tag :type="systemStatus.model ? 'success' : 'danger'">
                {{ systemStatus.model ? '正常' : '异常' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">数据库</span>
              <el-tag :type="systemStatus.database ? 'success' : 'danger'">
                {{ systemStatus.database ? '正常' : '异常' }}
              </el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">缓存服务</span>
              <el-tag :type="systemStatus.redis ? 'success' : 'danger'">
                {{ systemStatus.redis ? '正常' : '异常' }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import {DataAnalysis, Setting, User, View} from "@element-plus/icons-vue";

// 注册 ECharts 组件
use([
  CanvasRenderer,
  LineChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
])

const router = useRouter()
const userStore = useUserStore()

// 响应式数据
const stats = ref({
  totalRecognitions: 0,
  totalUsers: 0,
  totalModels: 0,
  todayRecognitions: 0
})

const chartsLoading = ref(false)
const activitiesLoading = ref(false)
const recentRecognitions = ref([])
const systemStatus = ref({
  backend: true,
  model: true,
  database: true,
  redis: true
})

// 图表配置
const recognitionTrendOption = ref({
  title: {
    text: '最近7天识别趋势',
    textStyle: {
      fontSize: 14,
      fontWeight: 'normal'
    }
  },
  tooltip: {
    trigger: 'axis'
  },
  xAxis: {
    type: 'category',
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: {
    type: 'value'
  },
  series: [{
    data: [120, 132, 101, 134, 90, 230, 210],
    type: 'line',
    smooth: true,
    itemStyle: {
      color: '#409EFF'
    }
  }]
})

const digitDistributionOption = ref({
  title: {
    text: '数字识别分布',
    textStyle: {
      fontSize: 14,
      fontWeight: 'normal'
    }
  },
  tooltip: {
    trigger: 'item'
  },
  series: [{
    type: 'pie',
    radius: '50%',
    data: [
      { value: 35, name: '数字0' },
      { value: 30, name: '数字1' },
      { value: 25, name: '数字2' },
      { value: 20, name: '数字3' },
      { value: 15, name: '数字4' },
      { value: 10, name: '其他' }
    ],
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    }
  }]
})

// 方法
const loadDashboardData = async () => {
  try {
    // 模拟API调用
    // const response = await getDashboardStats()

    // 模拟数据
    stats.value = {
      totalRecognitions: 1234,
      totalUsers: 56,
      totalModels: 8,
      todayRecognitions: 89
    }

    recentRecognitions.value = [
      {
        id: 1,
        result: 8,
        confidence: 0.95,
        createTime: new Date()
      },
      {
        id: 2,
        result: 3,
        confidence: 0.87,
        createTime: new Date(Date.now() - 1000 * 60 * 5)
      },
      {
        id: 3,
        result: 7,
        confidence: 0.92,
        createTime: new Date(Date.now() - 1000 * 60 * 10)
      }
    ]
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

const refreshCharts = () => {
  chartsLoading.value = true
  setTimeout(() => {
    chartsLoading.value = false
    ElMessage.success('图表数据已刷新')
  }, 1000)
}

const formatTime = (time) => {
  return dayjs(time).format('MM-DD HH:mm')
}

// 快速操作方法
const goToTraining = () => {
  router.push('/models/training')
}

const goToRecognition = () => {
  router.push('/recognition/history')
}

const goToUsers = () => {
  router.push('/users/list')
}

const goToModels = () => {
  router.push('/models/list')
}

// 生命周期
onMounted(() => {
  loadDashboardData()
})
</script>

<style lang="scss" scoped>

.dashboard-container {
  .el-card{
    font-size: 28px;
    font-weight: bold;
    color: #000000;
    background-color: #ffffff;
    margin-bottom: 8px;
  }

  .stats-row {
    margin-bottom: 20px;
  }

  .charts-row {
    margin-bottom: 20px;
  }

  .quick-actions-row {
    margin-bottom: 20px;
  }

  .activity-row {
    margin-bottom: 20px;
  }

  .chart-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .chart-container {
      min-height: 300px;
    }
  }

  .quick-actions {
    display: flex;
    gap: 16px;
    flex-wrap: wrap;
  }

  .activity-list {
    min-height: 200px;

    .activity-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .activity-icon {
        margin-right: 12px;
        font-size: 16px;
      }

      .activity-content {
        flex: 1;

        .activity-title {
          font-size: 14px;
          color: #303133;
          margin-bottom: 4px;
        }

        .activity-meta {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .empty-activity {
      text-align: center;
      color: #909399;
      padding: 40px 0;
    }
  }

  .system-status {
    .status-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .status-label {
        font-size: 14px;
        color: #606266;
      }
    }
  }
}

</style>