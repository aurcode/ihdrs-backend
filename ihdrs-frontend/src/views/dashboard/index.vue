// views/dashboard/index.vue

<template>
  <div class="dashboard-container">
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
    <div class="dashboard-content">
      <!-- 头部Logo区域 -->
      <div class="header-section">
        <div class="logo-container">
          <div class="logo-circle">
            <el-icon size="50" color="#2563eb">
              <DataBoard />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">仪表板</h1>
        <p class="header-subtitle">Dashboard</p>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <View/>
                </el-icon>
              </div>
              <div class="stat-label">总识别次数</div>
              <div class="stat-value">{{ stats.totalRecognitions || 0 }}</div>
            </div>
            <div class="stat-trend" v-if="stats.recognitionGrowth">
              <span class="trend-text">{{ stats.recognitionGrowth > 0 ? '+' : '' }}{{ stats.recognitionGrowth }}%</span>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <User/>
                </el-icon>
              </div>
              <div class="stat-label">注册用户</div>
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
            </div>
            <div class="stat-trend" v-if="stats.userGrowth">
              <span class="trend-text">{{ stats.userGrowth > 0 ? '+' : '' }}{{ stats.userGrowth }}%</span>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Setting/>
                </el-icon>
              </div>
              <div class="stat-label">训练模型</div>
              <div class="stat-value">{{ stats.totalModels || 0 }}</div>
            </div>
            <div class="stat-trend" v-if="stats.modelGrowth">
              <span class="trend-text">{{ stats.modelGrowth > 0 ? '+' : '' }}{{ stats.modelGrowth }}%</span>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <DataAnalysis/>
                </el-icon>
              </div>
              <div class="stat-label">今日识别</div>
              <div class="stat-value">{{ stats.todayRecognitions || 0 }}</div>
            </div>
            <div class="stat-trend" v-if="stats.todayGrowth">
              <span class="trend-text">{{ stats.todayGrowth > 0 ? '+' : '' }}{{ stats.todayGrowth }}%</span>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="charts-row">
        <!-- 识别趋势图 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <div class="chart-card modern-card">
            <div class="card-header">
              <div class="header-content">
                <h3>识别趋势</h3>
                <p>最近7天数据统计</p>
              </div>
              <el-button type="text" @click="refreshCharts" class="refresh-btn">
                <el-icon>
                  <Refresh/>
                </el-icon>
                刷新
              </el-button>
            </div>
            <div class="chart-container" v-loading="chartsLoading">
              <v-chart
                  :option="recognitionTrendOption"
                  :autoresize="true"
                  style="height: 320px;"
              />
            </div>
          </div>
        </el-col>

        <!-- 准确率分布图 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <div class="chart-card modern-card">
            <div class="card-header">
              <div class="header-content">
                <h3>数字识别分布</h3>
                <p>各数字识别频率统计</p>
              </div>
            </div>
            <div class="chart-container" v-loading="chartsLoading">
              <v-chart
                  :option="digitDistributionOption"
                  :autoresize="true"
                  style="height: 320px;"
              />
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 快速操作 -->
      <el-row :gutter="20" class="quick-actions-row">
        <el-col :span="24">
          <div class="quick-actions-card modern-card">
            <div class="card-header">
              <div class="header-content">
                <h3>快速操作</h3>
                <p>常用功能快捷入口</p>
              </div>
            </div>
            <div class="quick-actions">
              <div class="action-item" @click="goToTraining">
                <div class="action-icon primary">
                  <el-icon>
                    <Setting/>
                  </el-icon>
                </div>
                <div class="action-content">
                  <h4>开始训练模型</h4>
                  <p>创建和训练新的识别模型</p>
                </div>
              </div>
              <div class="action-item" @click="goToRecognition">
                <div class="action-icon success">
                  <el-icon>
                    <View/>
                  </el-icon>
                </div>
                <div class="action-content">
                  <h4>查看识别记录</h4>
                  <p>浏览历史识别结果</p>
                </div>
              </div>
              <div class="action-item" @click="goToUsers" v-if="userStore.isAdmin">
                <div class="action-icon warning">
                  <el-icon>
                    <User/>
                  </el-icon>
                </div>
                <div class="action-content">
                  <h4>用户管理</h4>
                  <p>管理系统用户权限</p>
                </div>
              </div>
              <div class="action-item" @click="goToModels" v-if="userStore.isAdmin">
                <div class="action-icon info">
                  <el-icon>
                    <DataAnalysis/>
                  </el-icon>
                </div>
                <div class="action-content">
                  <h4>模型管理</h4>
                  <p>查看和管理训练模型</p>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 最近活动和系统状态 -->
      <el-row :gutter="20" class="activity-row">
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <div class="activity-card modern-card">
            <div class="card-header">
              <div class="header-content">
                <h3>最近识别记录</h3>
                <p>最新的识别活动</p>
              </div>
            </div>
            <div class="activity-list" v-loading="activitiesLoading">
              <div
                  v-for="record in recentRecognitions"
                  :key="record.id"
                  class="activity-item"
              >
                <div class="activity-avatar">
                  <img
                      class="digit-image"
                      :src="getImageUrl(record.imageName)"
                      alt="识别图像"
                  />
                </div>
                <div class="activity-content">
                  <div class="activity-title">识别结果：
                    {{
                      record.result === null ||
                      record.result === undefined ||
                      record.result === 'null'
                          ? record.sequenceResult
                          : record.result
                    }}
                  </div>
                  <div class="activity-meta">
                  <span class="confidence">
                    <el-icon><SuccessFilled/></el-icon>
                    置信度: {{ (record.confidence * 100).toFixed(1) }}%
                  </span>
                    <span class="time">{{ formatTime(record.createTime) }}</span>
                  </div>
                </div>
                <div class="activity-status">
                  <el-tag size="small" type="success">成功</el-tag>
                </div>
              </div>
              <div v-if="recentRecognitions.length === 0" class="empty-activity">
                <el-icon>
                  <DocumentRemove/>
                </el-icon>
                <p>暂无识别记录</p>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 系统状态 -->
        <el-col :xs="24" :sm="24" :md="12" :lg="12" :xl="12">
          <div class="status-card modern-card">
            <div class="card-header">
              <div class="header-content">
                <h3>系统状态</h3>
                <p>服务运行状态监控</p>
              </div>
            </div>
            <div class="system-status">
              <div class="status-item" :class="{ 'status-online': systemStatus.backend }">
                <div class="status-indicator">
                  <div class="indicator-dot"></div>
                </div>
                <div class="status-content">
                  <span class="status-label">后端服务</span>
                  <span class="status-text">{{ systemStatus.backend ? '正常运行' : '服务异常' }}</span>
                </div>
                <div class="status-tag">
                  <el-tag :type="systemStatus.backend ? 'success' : 'danger'" size="small">
                    {{ systemStatus.backend ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>

              <div class="status-item" :class="{ 'status-online': systemStatus.model }">
                <div class="status-indicator">
                  <div class="indicator-dot"></div>
                </div>
                <div class="status-content">
                  <span class="status-label">模型服务</span>
                  <span class="status-text">{{ systemStatus.model ? '正常运行' : '服务异常' }}</span>
                </div>
                <div class="status-tag">
                  <el-tag :type="systemStatus.model ? 'success' : 'danger'" size="small">
                    {{ systemStatus.model ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>

              <div class="status-item" :class="{ 'status-online': systemStatus.database }">
                <div class="status-indicator">
                  <div class="indicator-dot"></div>
                </div>
                <div class="status-content">
                  <span class="status-label">数据库</span>
                  <span class="status-text">{{ systemStatus.database ? '连接正常' : '连接异常' }}</span>
                </div>
                <div class="status-tag">
                  <el-tag :type="systemStatus.database ? 'success' : 'danger'" size="small">
                    {{ systemStatus.database ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>

              <div class="status-item" :class="{ 'status-online': systemStatus.redis }">
                <div class="status-indicator">
                  <div class="indicator-dot"></div>
                </div>
                <div class="status-content">
                  <span class="status-label">缓存服务</span>
                  <span class="status-text">{{ systemStatus.redis ? '连接正常' : '连接异常' }}</span>
                </div>
                <div class="status-tag">
                  <el-tag :type="systemStatus.redis ? 'success' : 'danger'" size="small">
                    {{ systemStatus.redis ? '正常' : '异常' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {useUserStore} from '@/stores/user'
import {ElMessage} from 'element-plus'
import {use} from 'echarts/core'
import {CanvasRenderer} from 'echarts/renderers'
import {LineChart, PieChart} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import dayjs from 'dayjs'
import {
  DataAnalysis,
  Setting,
  User,
  View,
  Refresh,
  SuccessFilled,
  DocumentRemove,
  DataBoard
} from "@element-plus/icons-vue"
import {getDashboardStats, getRecentRecognitions, getPerformanceMetrics} from '@/api/stats'

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
  todayRecognitions: 0,
  recognitionGrowth: 0,
  userGrowth: 0,
  modelGrowth: 0,
  todayGrowth: 0
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
  tooltip: {
    trigger: 'axis',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderColor: '#e4e7ed',
    borderWidth: 1,
    textStyle: {
      color: '#606266'
    }
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    data: [],
    axisLine: {
      lineStyle: {
        color: '#e4e7ed'
      }
    },
    axisLabel: {
      color: '#909399'
    }
  },
  yAxis: {
    type: 'value',
    axisLine: {
      lineStyle: {
        color: '#e4e7ed'
      }
    },
    axisLabel: {
      color: '#909399'
    },
    splitLine: {
      lineStyle: {
        color: '#f5f7fa'
      }
    }
  },
  series: [{
    data: [],
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: {
      width: 3,
      color: {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 1,
        y2: 0,
        colorStops: [{
          offset: 0, color: '#409EFF'
        }, {
          offset: 1, color: '#67C23A'
        }]
      }
    },
    areaStyle: {
      color: {
        type: 'linear',
        x: 0,
        y: 0,
        x2: 0,
        y2: 1,
        colorStops: [{
          offset: 0, color: 'rgba(64, 158, 255, 0.3)'
        }, {
          offset: 1, color: 'rgba(64, 158, 255, 0.1)'
        }]
      }
    }
  }]
})

const digitDistributionOption = ref({
  tooltip: {
    trigger: 'item',
    formatter: '{a} <br/>{b}: {c} ({d}%)',
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    borderColor: '#e4e7ed',
    borderWidth: 1,
    textStyle: {
      color: '#606266'
    }
  },
  legend: {
    bottom: '5%',
    textStyle: {
      color: '#606266'
    }
  },
  series: [{
    name: '识别分布',
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['50%', '40%'],
    data: [],
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    },
    label: {
      show: false
    }
  }]
})

// 更新图表数据的辅助函数
const updateCharts = (performanceData) => {
  if (!performanceData) return

  // 更新识别趋势图
  if (performanceData.weeklyTrend) {
    recognitionTrendOption.value.xAxis.data = performanceData.weeklyTrend.map(item => item.date || item.day)
    recognitionTrendOption.value.series[0].data =
        performanceData.weeklyTrend.map(item => Number(item.count ?? item.value ?? 0))
  }

  // 更新数字分布图
  if (performanceData.digitDistribution) {
    const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#C0C4CC', '#00D7FF', '#FF6B9D', '#C71585', '#FFD700']
    digitDistributionOption.value.series[0].data =
        performanceData.digitDistribution.map((item, index) => ({
          value: item.count ?? item.value ?? 0,
          name: item.digit == null ? '连续数字' : `数字${item.digit}`,
          itemStyle: {color: colors[index % colors.length]}
        }))
  }
}

// 方法
const loadDashboardData = async () => {
  try {
    // 加载仪表盘统计数据
    const dashboardRes = await getDashboardStats()
    if (dashboardRes.code === 200 && dashboardRes.data) {
      stats.value = {
        totalRecognitions: dashboardRes.data.totalRecognitions || 0,
        totalUsers: dashboardRes.data.totalUsers || 0,
        totalModels: dashboardRes.data.totalModels || 0,
        todayRecognitions: dashboardRes.data.todayRecognitions || 0,
        recognitionGrowth: dashboardRes.data.recognitionGrowth || 0,
        userGrowth: dashboardRes.data.userGrowth || 0,
        modelGrowth: dashboardRes.data.modelGrowth || 0,
        todayGrowth: dashboardRes.data.todayGrowth || 0
      }
    }

    // 加载最近识别记录
    activitiesLoading.value = true
    const recognitionsRes = await getRecentRecognitions(10)
    if (recognitionsRes.code === 200 && recognitionsRes.data) {
      recentRecognitions.value = recognitionsRes.data
    }
    activitiesLoading.value = false

    // 加载性能指标（图表数据）
    chartsLoading.value = true
    const performanceRes = await getPerformanceMetrics()
    if (performanceRes.code === 200 && performanceRes.data) {
      updateCharts(performanceRes.data)
    }
    chartsLoading.value = false

  } catch (error) {
    console.error('加载仪表板数据失败:', error)
    ElMessage.error('加载数据失败')
    activitiesLoading.value = false
    chartsLoading.value = false
  }
}

const refreshCharts = async () => {
  chartsLoading.value = true
  try {
    const performanceRes = await getPerformanceMetrics()
    if (performanceRes.code === 200 && performanceRes.data) {
      updateCharts(performanceRes.data)
      ElMessage.success('图表数据已刷新')
    }
  } catch (error) {
    console.error('刷新图表失败:', error)
    ElMessage.error('刷新失败')
  } finally {
    chartsLoading.value = false
  }
}

const getImageUrl = (fileName) => {
  if (!fileName) return '';
  const baseApi = import.meta.env.VITE_BASE_API;
  return `${baseApi}${fileName}`;
};


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

// 生命周期
onMounted(() => {
  loadDashboardData()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
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

  // 背景装饰圆形 - 使用登录界面的蓝色系
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
  .dashboard-content {
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

  // 通用卡片样式 - 参考登录注册界面
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

    .card-header {
      padding: 24px 24px 0;
      display: flex;
      justify-content: space-between;
      align-items: flex-start;

      .header-content {
        h3 {
          margin: 0 0 4px 0;
          font-size: 18px;
          font-weight: 600;
          color: #303133;
        }

        p {
          margin: 0;
          font-size: 14px;
          color: #909399;
        }
      }

      .refresh-btn {
        display: flex;
        align-items: center;
        gap: 4px;
        color: #409EFF;
        font-size: 14px;

        &:hover {
          color: #66b1ff;
        }
      }
    }
  }

  // 行间距
  .stats-row,
  .charts-row,
  .quick-actions-row,
  .activity-row {
    margin-bottom: 24px;
  }

  // 统计卡片样式 - 参考登录注册界面
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

      // 第2个和第4个卡片的图标更亮
      &.success .stat-icon,
      &.danger .stat-icon {
        filter: brightness(2.2);
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
  }

  // 图表卡片
  .chart-card {
    .chart-container {
      padding: 0 24px 24px;
      min-height: 320px;
    }
  }

  // 快速操作卡片
  .quick-actions-card {
    .quick-actions {
      padding: 16px 24px 24px;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 16px;

      .action-item {
        display: flex;
        align-items: center;
        padding: 24px;
        border-radius: 16px;
        background: rgba(255, 255, 255, 0.8);
        border: 2px solid rgba(226, 232, 240, 0.8);
        cursor: pointer;
        transition: all 0.3s ease;
        backdrop-filter: blur(10px);

        &:hover {
          transform: translateY(-4px);
          box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
          border-color: #2563eb;
          background: rgba(239, 246, 255, 0.9);
        }

        .action-icon {
          width: 56px;
          height: 56px;
          border-radius: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 16px;
          font-size: 24px;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

          &.primary {
            background: linear-gradient(135deg, #409EFF, #66b1ff);
            color: white;
          }

          &.success {
            background: linear-gradient(135deg, #67C23A, #85ce61);
            color: white;
          }

          &.warning {
            background: linear-gradient(135deg, #E6A23C, #ebb563);
            color: white;
          }

          &.info {
            background: linear-gradient(135deg, #909399, #b4bccc);
            color: white;
          }
        }

        .action-content {
          h4 {
            margin: 0 0 4px 0;
            font-size: 16px;
            font-weight: 600;
            color: #303133;
          }

          p {
            margin: 0;
            font-size: 14px;
            color: #909399;
          }
        }
      }
    }
  }

  // 活动列表卡片
  .activity-card {
    overflow: visible;
    position: relative;
    z-index: 1;

    .activity-list {
      padding: 16px 20px 24px;
      min-height: 200px;
      overflow: visible;
      position: relative;

      .activity-item {
        display: flex;
        align-items: center;
        padding: 16px 20px;
        margin: 0 0 8px 0;
        border-radius: 12px;
        transition: all 0.3s ease;
        border: 1px solid transparent;
        position: relative;
        z-index: 1;

        // 交替背景色 - 白色和浅灰色
        &:nth-child(odd) {
          background: rgba(255, 255, 255, 0.75);
          border-color: rgba(226, 232, 240, 0.5);
        }

        &:nth-child(even) {
          background: rgba(241, 245, 249, 0.85);
          border-color: rgba(226, 232, 240, 0.4);
        }

        &:last-child {
          margin-bottom: 0;
        }

        &:hover {
          background: rgba(239, 246, 255, 0.9) !important;
          border-color: rgba(37, 99, 235, 0.3);
          transform: translateX(4px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        }

        .activity-avatar {
          width: 48px;
          height: 48px;
          margin-right: 16px;

          .digit-image {
            width: 56px;
            height: 56px;
            border-radius: 16px;
            object-fit: cover;
            background: #f0f0f0;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
            border: 2px solid rgba(255, 255, 255, 0.5);
          }

          .digit-display {
            width: 100%;
            height: 100%;
            background: linear-gradient(135deg, #409EFF, #66b1ff);
            color: white;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            font-weight: 700;
          }
        }

        .activity-content {
          flex: 1;

          .activity-title {
            font-size: 15px;
            color: #303133;
            font-weight: 500;
            margin-bottom: 6px;
          }

          .activity-meta {
            display: flex;
            align-items: center;
            gap: 12px;
            font-size: 13px;

            .confidence {
              display: flex;
              align-items: center;
              gap: 4px;
              color: #67C23A;
            }

            .time {
              color: #909399;
            }
          }
        }

        .activity-status {
          margin-left: 12px;
        }
      }

      .empty-activity {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 40px 0;
        color: #909399;

        .el-icon {
          font-size: 48px;
          margin-bottom: 12px;
          opacity: 0.5;
        }

        p {
          margin: 0;
          font-size: 14px;
        }
      }
    }
  }

  // 系统状态卡片
  .status-card {
    overflow: visible;
    position: relative;
    z-index: 1;

    .system-status {
      padding: 16px 20px 24px;
      overflow: visible;
      position: relative;

      .status-item {
        display: flex;
        align-items: center;
        padding: 16px 20px;
        margin: 0 0 8px 0;
        border-radius: 12px;
        transition: all 0.3s ease;
        border: 1px solid transparent;
        position: relative;
        z-index: 1;

        // 交替背景色 - 白色和浅灰色
        &:nth-child(odd) {
          background: rgba(255, 255, 255, 0.75);
          border-color: rgba(226, 232, 240, 0.5);
        }

        &:nth-child(even) {
          background: rgba(241, 245, 249, 0.85);
          border-color: rgba(226, 232, 240, 0.4);
        }

        &:last-child {
          margin-bottom: 0;
        }

        &:hover {
          background: rgba(239, 246, 255, 0.9) !important;
          border-color: rgba(37, 99, 235, 0.3);
          transform: translateX(4px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
        }

        .status-indicator {
          margin-right: 16px;

          .indicator-dot {
            width: 14px;
            height: 14px;
            border-radius: 50%;
            background: #dcdfe6;
            position: relative;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

            &::before {
              content: '';
              position: absolute;
              width: 100%;
              height: 100%;
              border-radius: 50%;
              animation: pulse 2s infinite;
            }
          }
        }

        &.status-online .indicator-dot {
          background: #67C23A;

          &::before {
            background: rgba(103, 194, 58, 0.3);
          }
        }

        .status-content {
          flex: 1;

          .status-label {
            display: block;
            font-size: 15px;
            color: #303133;
            font-weight: 500;
            margin-bottom: 2px;
          }

          .status-text {
            display: block;
            font-size: 13px;
            color: #909399;
          }
        }

        .status-tag {
          margin-left: 12px;
        }
      }
    }
  }

  // 响应式设计
  @media (max-width: 768px) {
    padding: 16px;

    .stats-row,
    .charts-row,
    .quick-actions-row,
    .activity-row {
      margin-bottom: 16px;
    }

    .stat-card {
      height: 100px;

      .stat-content {
        padding: 16px;

        .stat-icon {
          font-size: 24px;
          margin-right: 12px;
        }

        .stat-info .stat-value {
          font-size: 24px;
        }
      }
    }

    .quick-actions {
      grid-template-columns: 1fr !important;
    }
  }
}

// 动画效果
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.5);
    opacity: 0.5;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

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