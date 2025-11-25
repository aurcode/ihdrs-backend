<template>
  <div class="statistics-container">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card primary">
          <div class="stat-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardStats.totalRecognitions || 0 }}</div>
            <div class="stat-label">总识别次数</div>
            <div class="stat-trend" v-if="dashboardStats.recognitionGrowth">
              <el-icon><CaretTop v-if="dashboardStats.recognitionGrowth > 0" /><CaretBottom v-else /></el-icon>
              <span>{{ Math.abs(dashboardStats.recognitionGrowth) }}%</span>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card success">
          <div class="stat-icon">
            <el-icon><SuccessFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardStats.successRate ? dashboardStats.successRate.toFixed(1) + '%' : '0%' }}</div>
            <div class="stat-label">识别成功率</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card warning">
          <div class="stat-icon">
            <el-icon><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ dashboardStats.avgProcessingTime ? dashboardStats.avgProcessingTime.toFixed(0) : '0' }}</div>
            <div class="stat-label">平均处理时间(ms)</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card info">
          <div class="stat-icon">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ performanceMetrics.activeUsers || 0 }}</div>
            <div class="stat-label">活跃用户（30分钟内）</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-section">
      <!-- 识别量趋势图 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>识别量趋势</h3>
            <p>最近7天识别量统计</p>
          </div>
          <div class="chart-body" v-loading="chartsLoading">
            <v-chart :option="recognitionTrendOption" :autoresize="true" style="height: 300px;" />
          </div>
        </div>
      </el-col>

      <!-- 成功率趋势图 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>成功率趋势</h3>
            <p>最近7天识别成功率</p>
          </div>
          <div class="chart-body" v-loading="chartsLoading">
            <v-chart :option="successRateOption" :autoresize="true" style="height: 300px;" />
          </div>
        </div>
      </el-col>

      <!-- 数字分布图 -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>数字识别分布</h3>
            <p>各数字识别频率统计</p>
          </div>
          <div class="chart-body" v-loading="chartsLoading">
            <v-chart :option="digitDistributionOption" :autoresize="true" style="height: 300px;" />
          </div>
        </div>
      </el-col>

      <!-- 今日识别量（按小时） -->
      <el-col :xs="24" :sm="24" :md="12" :lg="12">
        <div class="chart-card">
          <div class="chart-header">
            <h3>今日识别量分布</h3>
            <p>24小时识别量统计</p>
          </div>
          <div class="chart-body" v-loading="chartsLoading">
            <v-chart :option="hourlyOption" :autoresize="true" style="height: 300px;" />
          </div>
        </div>
      </el-col>

      <!-- 系统资源使用图 -->
      <el-col :xs="24" :sm="24" :md="24" :lg="24">
        <div class="chart-card">
          <div class="chart-header">
            <h3>系统资源使用</h3>
            <p>CPU和内存使用率监控</p>
          </div>
          <div class="chart-body" v-loading="chartsLoading">
            <v-chart :option="resourceUsageOption" :autoresize="true" style="height: 320px;" />
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 系统性能和最近记录 -->
    <el-row :gutter="20" class="bottom-section">
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <div class="performance-card">
          <div class="card-header">
            <h3>系统性能</h3>
          </div>
          <div class="performance-content">
            <div class="performance-item">
              <div class="performance-label">CPU使用率</div>
              <el-progress
                  :percentage="Math.round(performanceMetrics.cpuUsage || 0)"
                  :color="getProgressColor(performanceMetrics.cpuUsage)"
              />
            </div>
            <div class="performance-item">
              <div class="performance-label">内存使用率</div>
              <el-progress
                  :percentage="Math.round(performanceMetrics.memoryUsage || 0)"
                  :color="getProgressColor(performanceMetrics.memoryUsage)"
              />
            </div>
            <div class="performance-item">
              <div class="performance-label">
                <span>小时请求数</span>
                <span class="performance-value">{{ performanceMetrics.totalRequests || 0 }}</span>
              </div>
            </div>
            <div class="performance-item">
              <div class="performance-label">
                <span>活跃用户</span>
                <span class="performance-value">{{ performanceMetrics.activeUsers || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <div class="recent-card">
          <div class="card-header">
            <h3>最近识别记录</h3>
          </div>
          <div class="recent-list" v-loading="tableLoading">
            <el-table :data="recentRecognitions" style="width: 100%" max-height="320">
              <el-table-column prop="imageName" label="图片路径" min-width="120" show-overflow-tooltip />
              <el-table-column prop="result" label="识别结果" width="150" align="center">
                <template #default="{ row }">
                  <el-tag>{{ (row.result === null || row.result === 'null') ? row.sequenceResult : row.result }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="confidence" label="置信度" width="100" align="center">
                <template #default="{ row }">
                  <span :class="{'high-confidence': row.confidence > 0.9, 'low-confidence': row.confidence < 0.7}">
                    {{ (row.confidence * 100).toFixed(1) }}%
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="processingTime" label="处理时间" width="100" align="center">
                <template #default="{ row }">
                  {{ row.processingTime }}ms
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="识别时间" width="160">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  DataAnalysis, SuccessFilled, Clock, User, CaretTop, CaretBottom,
  TrendCharts, VideoPlay
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DataZoomComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboardStats, getRecentRecognitions, getPerformanceMetrics } from '@/api/stats'

// 注册 ECharts 组件
use([
  CanvasRenderer, LineChart, BarChart, PieChart,
  TitleComponent, TooltipComponent, LegendComponent,
  GridComponent, DataZoomComponent
])

// 响应式数据
const dashboardStats = ref({})
const recentRecognitions = ref([])
const performanceMetrics = ref({})
const chartsLoading = ref(false)
const tableLoading = ref(false)
let refreshInterval = null

// 图表配置
const recognitionTrendOption = ref({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderColor: '#e4e7ed',
    textStyle: { color: '#606266' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'value',
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#909399' },
    splitLine: { lineStyle: { color: '#f5f7fa' } }
  },
  series: [{
    data: [],
    type: 'line',
    smooth: true,
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
          { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
        ]
      }
    },
    lineStyle: { color: '#409EFF', width: 3 },
    itemStyle: { color: '#409EFF' }
  }]
})

const successRateOption = ref({
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    formatter: '{b}<br/>成功率: {c}%'
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLine: { lineStyle: { color: '#e4e7ed' } },
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%', color: '#909399' },
    splitLine: { lineStyle: { color: '#f5f7fa' } }
  },
  series: [{
    data: [],
    type: 'line',
    smooth: true,
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
          { offset: 1, color: 'rgba(103, 194, 58, 0.05)' }
        ]
      }
    },
    lineStyle: { color: '#67C23A', width: 3 },
    itemStyle: { color: '#67C23A' }
  }]
})

const digitDistributionOption = ref({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c} ({d}%)'
  },
  legend: { bottom: '5%', textStyle: { color: '#606266' } },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['50%', '45%'],
    data: [],
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    }
  }]
})

const hourlyOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLabel: { rotate: 45, color: '#909399' }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#909399' },
    splitLine: { lineStyle: { color: '#f5f7fa' } }
  },
  series: [{
    data: [],
    type: 'bar',
    itemStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: '#409EFF' },
          { offset: 1, color: '#66b1ff' }
        ]
      }
    },
    barWidth: '60%'
  }]
})

const resourceUsageOption = ref({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'cross' }
  },
  legend: {
    data: ['CPU使用率', '内存使用率'],
    top: '3%',
    textStyle: { color: '#606266' }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLabel: { color: '#909399' }
  },
  yAxis: {
    type: 'value',
    max: 100,
    axisLabel: { formatter: '{value}%', color: '#909399' },
    splitLine: { lineStyle: { color: '#f5f7fa' } }
  },
  series: [
    {
      name: 'CPU使用率',
      type: 'line',
      data: [],
      smooth: true,
      lineStyle: { color: '#E6A23C', width: 2 },
      itemStyle: { color: '#E6A23C' }
    },
    {
      name: '内存使用率',
      type: 'line',
      data: [],
      smooth: true,
      lineStyle: { color: '#F56C6C', width: 2 },
      itemStyle: { color: '#F56C6C' }
    }
  ]
})

// 方法
const loadData = async () => {
  try {
    chartsLoading.value = true
    tableLoading.value = true

    const [statsRes, recognitionsRes, metricsRes] = await Promise.all([
      getDashboardStats(),
      getRecentRecognitions(10),
      getPerformanceMetrics()
    ])

    if (statsRes.code === 200) {
      dashboardStats.value = statsRes.data || {}
    }

    if (recognitionsRes.code === 200) {
      recentRecognitions.value = recognitionsRes.data || []
    }

    if (metricsRes.code === 200) {
      performanceMetrics.value = metricsRes.data || {}
      updateCharts(metricsRes.data)
    }

  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('数据加载失败')
  } finally {
    chartsLoading.value = false
    tableLoading.value = false
  }
}

const updateCharts = (data) => {
  // 更新识别量趋势图
  if (data.weeklyTrend) {
    recognitionTrendOption.value.xAxis.data = data.weeklyTrend.map(item => item.date)
    recognitionTrendOption.value.series[0].data = data.weeklyTrend.map(item => item.count)
  }

  // 更新成功率趋势图
  if (data.successRateTrend) {
    successRateOption.value.xAxis.data = data.successRateTrend.map(item => item.date)
    successRateOption.value.series[0].data = data.successRateTrend.map(item => item.rate)
  }

  // 更新数字分布图
  if (data.digitDistribution) {
    const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#C0C4CC', '#00D7FF', '#FF6B9D', '#C71585', '#FFD700']
    digitDistributionOption.value.series[0].data = data.digitDistribution.map((item, index) => ({
      value: item.count,
      name: item.digit == null ? '连续数字' : `数字${item.digit}`,
      itemStyle: { color: colors[index % colors.length] }
    }))
  }

  // 更新小时识别量图
  if (data.hourlyRecognitions) {
    hourlyOption.value.xAxis.data = data.hourlyRecognitions.map(item => item.hour)
    hourlyOption.value.series[0].data = data.hourlyRecognitions.map(item => item.count)
  }

  // 更新系统资源使用图
  if (data.resourceUsageHistory) {
    resourceUsageOption.value.xAxis.data = data.resourceUsageHistory.map(item => item.time)
    resourceUsageOption.value.series[0].data = data.resourceUsageHistory.map(item => item.cpu)
    resourceUsageOption.value.series[1].data = data.resourceUsageHistory.map(item => item.memory)
  }
}

const getProgressColor = (value) => {
  if (!value) return '#909399'
  if (value < 50) return '#67C23A'
  if (value < 80) return '#E6A23C'
  return '#F56C6C'
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN')
}

onMounted(() => {
  loadData()
  // 每5秒自动刷新
  refreshInterval = setInterval(() => {
    loadData()
  }, 5000)
})

onBeforeUnmount(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style lang="scss" scoped>
.statistics-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;

  .stats-cards {
    margin-bottom: 20px;

    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      display: flex;
      align-items: center;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
      }

      .stat-icon {
        width: 60px;
        height: 60px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 28px;
        color: white;
        margin-right: 15px;
      }

      &.primary .stat-icon { background: linear-gradient(135deg, #409EFF, #66b1ff); }
      &.success .stat-icon { background: linear-gradient(135deg, #67C23A, #85ce61); }
      &.warning .stat-icon { background: linear-gradient(135deg, #E6A23C, #ebb563); }
      &.info .stat-icon { background: linear-gradient(135deg, #909399, #b4bccc); }

      .stat-content {
        flex: 1;

        .stat-value {
          font-size: 26px;
          font-weight: bold;
          margin-bottom: 4px;
          color: #303133;
        }

        .stat-label {
          font-size: 14px;
          color: #909399;
        }

        .stat-trend {
          margin-top: 8px;
          font-size: 12px;
          color: #67C23A;
          display: flex;
          align-items: center;
          gap: 4px;

          &.success { color: #67C23A; }
        }
      }
    }
  }

  .charts-section, .bottom-section {
    margin-bottom: 20px;

    .chart-card, .performance-card, .recent-card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      margin-bottom: 20px;

      .chart-header, .card-header {
        padding: 20px 20px 10px;
        border-bottom: 1px solid #f5f7fa;

        h3 {
          margin: 0 0 4px 0;
          font-size: 18px;
          color: #303133;
          font-weight: 600;
        }

        p {
          margin: 0;
          font-size: 14px;
          color: #909399;
        }
      }

      .chart-body {
        padding: 20px;
      }
    }

    .performance-content {
      padding: 20px;

      .performance-item {
        margin-bottom: 20px;

        &:last-child { margin-bottom: 0; }

        .performance-label {
          display: flex;
          justify-content: space-between;
          margin-bottom: 8px;
          font-size: 14px;
          color: #606266;
          font-weight: 500;

          .performance-value {
            font-size: 18px;
            font-weight: bold;
            color: #409EFF;
          }
        }
      }
    }

    .recent-list {
      padding: 20px;

      .high-confidence { color: #67C23A; font-weight: bold; }
      .low-confidence { color: #E6A23C; }
    }
  }
}

@media (max-width: 768px) {
  .statistics-container {
    padding: 10px;
  }
}
</style>