// views/statistics/overview.vue

<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <!-- 统计卡片 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon success">
              <i class="el-icon-success"></i>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ dashboardStats.totalRecognitions || 0 }}</div>
              <div class="stats-label">总识别次数</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon primary">
              <i class="el-icon-check"></i>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ dashboardStats.successRate ? dashboardStats.successRate.toFixed(2) + '%' : '0%' }}</div>
              <div class="stats-label">识别成功率</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon warning">
              <i class="el-icon-warning"></i>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ dashboardStats.errorRate ? dashboardStats.errorRate.toFixed(2) + '%' : '0%' }}</div>
              <div class="stats-label">错误率</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stats-card">
          <div class="stats-item">
            <div class="stats-icon info">
              <i class="el-icon-time"></i>
            </div>
            <div class="stats-content">
              <div class="stats-value">{{ dashboardStats.avgProcessingTime ? dashboardStats.avgProcessingTime.toFixed(2) + 'ms' : '0ms' }}</div>
              <div class="stats-label">平均处理时间</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 性能监控 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24" :lg="8">
        <el-card header="系统性能">
          <div class="performance-item">
            <div class="performance-label">CPU使用率</div>
            <el-progress 
              :percentage="performanceMetrics.cpuUsage ? Math.round(performanceMetrics.cpuUsage) : 0" 
              :color="getProgressColor(performanceMetrics.cpuUsage)"
            />
          </div>
          <div class="performance-item">
            <div class="performance-label">内存使用率</div>
            <el-progress 
              :percentage="performanceMetrics.memoryUsage ? Math.round(performanceMetrics.memoryUsage) : 0" 
              :color="getProgressColor(performanceMetrics.memoryUsage)"
            />
          </div>
          <div class="performance-item">
            <div class="performance-label">总请求数</div>
            <div class="performance-value">{{ performanceMetrics.totalRequests || 0 }}</div>
          </div>
          <div class="performance-item">
            <div class="performance-label">活跃用户</div>
            <div class="performance-value">{{ performanceMetrics.activeUsers || 0 }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card header="最近识别记录">
          <el-table :data="recentRecognitions" style="width: 100%">
            <el-table-column prop="imageName" label="图片名称" min-width="120">
              <template #default="{ row }">
                <span class="text-ellipsis">{{ row.imageName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'">
                  {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="confidence" label="置信度" width="100">
              <template #default="{ row }">
                {{ row.confidence ? (row.confidence * 100).toFixed(2) + '%' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="processingTime" label="处理时间" width="100">
              <template #default="{ row }">
                {{ row.processingTime ? row.processingTime + 'ms' : '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="识别时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 错误分析 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :xs="24">
        <el-card header="错误分析">
          <div v-if="errorAnalysis.commonErrors && errorAnalysis.commonErrors.length">
            <div v-for="error in errorAnalysis.commonErrors" :key="error.error" class="error-item">
              <div class="error-info">
                <span class="error-message">{{ error.error }}</span>
                <span class="error-count">{{ error.count }} 次</span>
              </div>
              <el-progress 
                :percentage="Math.round((error.count / errorAnalysis.totalErrors) * 100)" 
                :show-text="false"
                :color="getErrorProgressColor(error.count)"
              />
            </div>
          </div>
          <div v-else class="empty-data">
            暂无错误数据
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboardStats, getRecentRecognitions, getPerformanceMetrics, getErrorAnalysis } from '@/api/stats'

export default {
  name: 'Dashboard',
  data() {
    return {
      dashboardStats: {},
      recentRecognitions: [],
      performanceMetrics: {},
      errorAnalysis: {},
      refreshInterval: null
    }
  },
  mounted() {
    this.loadData()
    // 每5分钟自动刷新数据
    this.refreshInterval = setInterval(() => {
      this.loadData()
    }, 3000)
  },
  beforeUnmount() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval)
    }
  },
  methods: {
    async loadData() {
      try {
        const [statsRes, recognitionsRes, metricsRes, analysisRes] = await Promise.all([
          getDashboardStats(),
          getRecentRecognitions(10),
          getPerformanceMetrics(),
          getErrorAnalysis()
        ])
        
        this.dashboardStats = statsRes.data || {}
        this.recentRecognitions = recognitionsRes.data || []
        this.performanceMetrics = metricsRes.data || {}
        this.errorAnalysis = analysisRes.data || {}
      } catch (error) {
        console.error('加载数据失败:', error)
        this.$message.error('数据加载失败')
      }
    },
    
    getProgressColor(value) {
      if (!value) return '#909399'
      if (value < 50) return '#67C23A'
      if (value < 80) return '#E6A23C'
      return '#F56C6C'
    },
    
    getErrorProgressColor(count) {
      if (count > 10) return '#F56C6C'
      if (count > 5) return '#E6A23C'
      return '#909399'
    },
    
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString('zh-CN')
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.stats-card {
  margin-bottom: 20px;
}

.stats-item {
  display: flex;
  align-items: center;
}

.stats-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: white;
}

.stats-icon.success {
  background-color: #67C23A;
}

.stats-icon.primary {
  background-color: #409EFF;
}

.stats-icon.warning {
  background-color: #E6A23C;
}

.stats-icon.info {
  background-color: #909399;
}

.stats-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stats-label {
  color: #909399;
  font-size: 14px;
}

.performance-item {
  margin-bottom: 20px;
}

.performance-label {
  margin-bottom: 8px;
  font-weight: 500;
}

.performance-value {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.error-item {
  margin-bottom: 15px;
}

.error-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.error-message {
  color: #606266;
}

.error-count {
  color: #909399;
  font-size: 14px;
}

.empty-data {
  text-align: center;
  color: #909399;
  padding: 20px;
}

.mt-20 {
  margin-top: 20px;
}

.text-ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>