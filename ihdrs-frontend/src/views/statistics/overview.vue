<template>
  <div class="statistics-container">
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
              <TrendCharts />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">统计分析</h1>
        <p class="header-subtitle">Statistics & Analysis</p>
      </div>

      <!-- 顶部统计卡片 -->
      <el-row :gutter="20" class="stats-cards">
        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><DataAnalysis /></el-icon>
              </div>
              <div class="stat-label">总识别次数</div>
              <div class="stat-value">{{ dashboardStats.totalRecognitions || 0 }}</div>
            </div>
            <div class="stat-trend" v-if="dashboardStats.recognitionGrowth">
              <span class="trend-text">{{ dashboardStats.recognitionGrowth > 0 ? '+' : '' }}{{ dashboardStats.recognitionGrowth }}%</span>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><SuccessFilled /></el-icon>
              </div>
              <div class="stat-label">识别成功率</div>
              <div class="stat-value">{{ dashboardStats.successRate ? dashboardStats.successRate.toFixed(1) + '%' : '0%' }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Clock /></el-icon>
              </div>
              <div class="stat-label">平均处理时间(ms)</div>
              <div class="stat-value">{{ dashboardStats.avgProcessingTime ? dashboardStats.avgProcessingTime.toFixed(0) : '0' }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><User /></el-icon>
              </div>
              <div class="stat-label">活跃用户（30分钟内）</div>
              <div class="stat-value">{{ performanceMetrics.activeUsers || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="charts-section">
        <!-- 识别量趋势图 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12">
          <div class="chart-card modern-card">
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
          <div class="chart-card modern-card">
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
          <div class="chart-card modern-card">
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
          <div class="chart-card modern-card">
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
          <div class="chart-card modern-card">
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
          <div class="performance-card modern-card">
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
          <div class="recent-card modern-card">
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

  .stats-cards {
    margin-bottom: 24px;

    .stat-card {
      position: relative;
      height: 160px;
      border-radius: 28px;
      overflow: hidden;
      cursor: pointer;
      transition: all 0.3s ease;
      box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      backdrop-filter: blur(10px);

      &:hover {
        transform: translateY(-6px);
        box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);

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
        padding: 20px;
        height: 100%;
        color: white;

        .stat-icon {
          font-size: 40px;
          margin-bottom: 0;
          filter: brightness(1.8);
        }

        .stat-label {
          font-size: 16px;
          opacity: 1;
          font-weight: 400;
          margin-bottom: 8px;
          margin-top: -8px;
          text-align: center;
        }

        .stat-value {
          font-size: 32px;
          font-weight: 700;
          line-height: 1;
          text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        }
      }

      .stat-trend {
        position: absolute;
        top: 16px;
        right: 16px;
        z-index: 3;

        .trend-text {
          font-size: 12px;
          color: rgba(255, 255, 255, 0.9);
          background: rgba(255, 255, 255, 0.2);
          padding: 4px 8px;
          border-radius: 12px;
          font-weight: 500;
        }
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

      // 第2个和第4个卡片的图标更亮
      &.success .stat-content .stat-icon,
      &.danger .stat-content .stat-icon {
        filter: brightness(2.2);
      }
    }
  }

  .charts-section, .bottom-section {
    margin-bottom: 24px;

    .chart-card, .performance-card, .recent-card {
      margin-bottom: 24px;

      .chart-header, .card-header {
        padding: 24px 24px 0;
        border-bottom: 1px solid rgba(226, 232, 240, 0.5);

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
        padding: 24px;
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
      padding: 24px;

      .high-confidence { color: #67C23A; font-weight: bold; }
      .low-confidence { color: #E6A23C; }
    }
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

@media (max-width: 768px) {
  .statistics-container {
    padding: 10px;
  }
}
</style>