<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card primary">
          <div class="stat-background"></div>
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><View /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalRecognitions || 0 }}</div>
              <div class="stat-label">总识别次数</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-text">+12%</span>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card success">
          <div class="stat-background"></div>
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
              <div class="stat-label">注册用户</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-text">+8%</span>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card warning">
          <div class="stat-background"></div>
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.totalModels || 0 }}</div>
              <div class="stat-label">训练模型</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-text">+5%</span>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
        <div class="stat-card danger">
          <div class="stat-background"></div>
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.todayRecognitions || 0 }}</div>
              <div class="stat-label">今日识别</div>
            </div>
          </div>
          <div class="stat-trend">
            <span class="trend-text">+25%</span>
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
              <el-icon><Refresh /></el-icon>
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
                <el-icon><Setting /></el-icon>
              </div>
              <div class="action-content">
                <h4>开始训练模型</h4>
                <p>创建和训练新的识别模型</p>
              </div>
            </div>
            <div class="action-item" @click="goToRecognition">
              <div class="action-icon success">
                <el-icon><View /></el-icon>
              </div>
              <div class="action-content">
                <h4>查看识别记录</h4>
                <p>浏览历史识别结果</p>
              </div>
            </div>
            <div class="action-item" @click="goToUsers" v-if="userStore.isAdmin">
              <div class="action-icon warning">
                <el-icon><User /></el-icon>
              </div>
              <div class="action-content">
                <h4>用户管理</h4>
                <p>管理系统用户权限</p>
              </div>
            </div>
            <div class="action-item" @click="goToModels" v-if="userStore.isAdmin">
              <div class="action-icon info">
                <el-icon><DataAnalysis /></el-icon>
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
                <div class="digit-display">{{ record.result }}</div>
              </div>
              <div class="activity-content">
                <div class="activity-title">识别数字: {{ record.result }}</div>
                <div class="activity-meta">
                  <span class="confidence">
                    <el-icon><SuccessFilled /></el-icon>
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
              <el-icon><DocumentRemove /></el-icon>
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
import {
  DataAnalysis,
  Setting,
  User,
  View,
  Refresh,
  SuccessFilled,
  DocumentRemove
} from "@element-plus/icons-vue"

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

// 图表配置 - 优化样式
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
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
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
    data: [120, 132, 101, 134, 90, 230, 210],
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
    data: [
      { value: 35, name: '数字0', itemStyle: { color: '#409EFF' } },
      { value: 30, name: '数字1', itemStyle: { color: '#67C23A' } },
      { value: 25, name: '数字2', itemStyle: { color: '#E6A23C' } },
      { value: 20, name: '数字3', itemStyle: { color: '#F56C6C' } },
      { value: 15, name: '数字4', itemStyle: { color: '#909399' } },
      { value: 10, name: '其他', itemStyle: { color: '#C0C4CC' } }
    ],
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
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
  padding: 24px;

  // 通用卡片样式
  .modern-card {
    background: #ffffff;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.2);
    transition: all 0.3s ease;
    overflow: hidden;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
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

  // 统计卡片样式
  .stat-card {
    position: relative;
    height: 120px;
    border-radius: 16px;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);

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
      align-items: center;
      padding: 20px;
      height: 100%;
      color: white;

      .stat-icon {
        font-size: 32px;
        margin-top: 15px;
        filter: brightness(1.8);
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: 700;
          line-height: 1;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          opacity: 0.9;
          font-weight: 500;
        }
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
      background: linear-gradient(135deg, #F56C6C 0%, #f78989 100%);
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
        padding: 20px;
        border-radius: 12px;
        background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
        border: 1px solid #e9ecef;
        cursor: pointer;
        transition: all 0.3s ease;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
          border-color: #409EFF;
        }

        .action-icon {
          width: 48px;
          height: 48px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          margin-right: 16px;
          font-size: 20px;

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
    .activity-list {
      padding: 16px 24px 24px;
      min-height: 200px;

      .activity-item {
        display: flex;
        align-items: center;
        padding: 16px 0;
        border-bottom: 1px solid #f5f7fa;

        &:last-child {
          border-bottom: none;
        }

        .activity-avatar {
          width: 48px;
          height: 48px;
          margin-right: 16px;

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
    .system-status {
      padding: 16px 24px 24px;

      .status-item {
        display: flex;
        align-items: center;
        padding: 16px 0;
        border-bottom: 1px solid #f5f7fa;

        &:last-child {
          border-bottom: none;
        }

        .status-indicator {
          margin-right: 16px;

          .indicator-dot {
            width: 12px;
            height: 12px;
            border-radius: 50%;
            background: #dcdfe6;
            position: relative;

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

// 加载动画优化
:deep(.el-loading-mask) {
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
}
</style>