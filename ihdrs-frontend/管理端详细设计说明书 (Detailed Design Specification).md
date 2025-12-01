# 详细设计说明书 (Detailed Design Specification)

------

## **手写数字识别系统（管理端）详细设计说明书**

**版本:** 1.3.0
**日期:** 2025-12-01
**分支名称:** IHDRS-Admin
**编写人:** 刘家乐

------

## **目录**

[TOC]

------

## **1.组件详细设计**

### **1.1 Dashboard 仪表板组件**

**文件:** `src/views/dashboard/index.vue`

**组件结构:**

```
<template>
  <div class="dashboard-container">
    <!-- 背景装饰 -->
    <BackgroundParticles />
    <BackgroundCircles />
    
    <!-- 内容区 -->
    <div class="content-wrapper">
      <!-- 头部 -->
      <PageHeader icon="DataBoard" title="仪表板" />
      
      <!-- 统计卡片 -->
      <StatsCards :stats="stats" />
      
      <!-- 图表区域 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <ChartCard title="识别趋势">
            <v-chart :option="recognitionTrendOption" />
          </ChartCard>
        </el-col>
        <el-col :span="12">
          <ChartCard title="数字分布">
            <v-chart :option="digitDistributionOption" />
          </ChartCard>
        </el-col>
      </el-row>
      
      <!-- 快速操作 -->
      <QuickActions />
      
      <!-- 最近活动 -->
      <el-row :gutter="20">
        <el-col :span="12">
          <RecentRecognitions :records="recentRecognitions" />
        </el-col>
        <el-col :span="12">
          <SystemStatus :status="systemStatus" />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { use } from 'echarts/core'
import { LineChart, PieChart } from 'echarts/charts'
import VChart from 'vue-echarts'
import { getDashboardStats, getRecentRecognitions } from '@/api/stats'

// 数据
const stats = ref({})
const recentRecognitions = ref([])
const systemStatus = ref({})

// 图表配置
const recognitionTrendOption = ref({
  // ECharts配置
})

const digitDistributionOption = ref({
  // ECharts配置
})

// 方法
const loadData = async () => {
  const [statsRes, recordsRes] = await Promise.all([
    getDashboardStats(),
    getRecentRecognitions(10)
  ])
  
  stats.value = statsRes.data
  recentRecognitions.value = recordsRes.data
  updateCharts(statsRes.data)
}

const updateCharts = (data) => {
  // 更新图表数据
}

// 生命周期
onMounted(() => {
  loadData()
  // 设置定时刷新
  const timer = setInterval(loadData, 30000)
  onBeforeUnmount(() => clearInterval(timer))
})
</script>
```

**关键方法:**

1. **loadData()**
   - 功能: 并行加载仪表板数据
   - 调用API: `getDashboardStats()`, `getRecentRecognitions()`
   - 错误处理: try-catch + ElMessage提示
2. **updateCharts(data)**
   - 功能: 根据数据更新ECharts配置
   - 参数: data - 统计数据对象
   - 实现: 更新xAxis.data和series.data
3. **formatTime(dateTime)**

- 功能: 格式化时间显示
- 参数: dateTime - ISO 8601字符串
- 返回: "YYYY-MM-DD HH:mm:ss"

------

### **1.2 Training 训练任务组件**

**文件:** `src/views/models/Training.vue`

**核心功能:**

**1.创建训练任务对话框**

```
<el-dialog v-model="createDialog.visible" title="创建训练任务">
  <el-form :model="createDialog.form" :rules="createDialog.rules">
    <!-- 基础配置 -->
    <el-divider>基础配置</el-divider>
    <el-form-item label="任务名称" prop="taskName">
      <el-input v-model="createDialog.form.taskName" />
    </el-form-item>
    <el-form-item label="数据集" prop="datasetId">
      <el-select v-model="createDialog.form.datasetId">
        <el-option v-for="d in datasets" :key="d.datasetId" 
                   :label="d.datasetName" :value="d.datasetId" />
      </el-select>
    </el-form-item>
    
    <!-- 模型配置 -->
    <el-divider>模型配置</el-divider>
    <el-form-item label="模型类型" prop="modelType">
      <el-select v-model="createDialog.form.modelType">
        <el-option label="基础CNN" value="CNN" />
        <el-option label="高级CNN" value="ADVANCED_CNN" />
        <el-option label="ResNet" value="RESNET" />
        <el-option label="VGG" value="VGG" />
        <el-option label="MobileNet" value="MOBILENET" />
      </el-select>
    </el-form-item>
    
    <!-- 训练参数 -->
    <el-divider>训练参数</el-divider>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-form-item label="批次大小" prop="batchSize">
          <el-select v-model="createDialog.form.batchSize">
            <el-option v-for="size in [16, 32, 64, 128]" 
                       :key="size" :value="size" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="学习率" prop="learningRate">
          <el-input-number v-model="createDialog.form.learningRate"
                           :min="0.0001" :max="0.1" :step="0.0001" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</el-dialog>
```

**2.实时训练监控**

```
// WebSocket连接
const connectWebSocket = (taskId) => {
  const ws = new WebSocket(`ws://localhost:8080/ws/training/${taskId}`)
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    
    switch (data.type) {
      case 'PROGRESS':
        updateProgress(data.payload)
        break
      case 'LOG':
        appendLog(data.payload)
        break
      case 'EPOCH_END':
        updateChart(data.payload)
        break
      case 'COMPLETED':
        handleTrainingCompleted(data.payload)
        break
    }
  }
  
  ws.onerror = (error) => {
    console.error('WebSocket错误:', error)
    // 降级为轮询
    startPolling(taskId)
  }
  
  return ws
}

// 更新进度
const updateProgress = (payload) => {
  batchProgress.value = {
    epoch: payload.epoch,
    totalEpochs: payload.totalEpochs,
    currentBatch: payload.currentBatch,
    totalBatches: payload.totalBatches,
    msPerStep: payload.msPerStep
  }
}

// 追加日志
const appendLog = (log) => {
  terminalLogs.value.push(log)
  // 自动滚动到底部
  nextTick(() => {
    terminalScrollbar.value?.setScrollTop(999999)
  })
}

// 更新图表
const updateChart = (data) => {
  accuracyChartOption.value.xAxis.data.push(data.epoch)
  accuracyChartOption.value.series[0].data.push(data.trainAcc)
  accuracyChartOption.value.series[1].data.push(data.valAcc)
  
  lossChartOption.value.xAxis.data.push(data.epoch)
  lossChartOption.value.series[0].data.push(data.trainLoss)
  lossChartOption.value.series[1].data.push(data.valLoss)
}
```

**3.混淆矩阵可视化**

```
const confusionMatrixOption = computed(() => {
  if (!confusionMatrixData.value) return {}
  
  const data = confusionMatrixData.value.flatMap((row, i) =>
    row.map((value, j) => [j, i, value])
  )
  
  return {
    tooltip: {
      position: 'top',
      formatter: (params) => {
        return `预测: ${params.value[0]}<br/>
                实际: ${params.value[1]}<br/>
                数量: ${params.value[2]}`
      }
    },
    xAxis: {
      type: 'category',
      data: ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'],
      name: '预测值'
    },
    yAxis: {
      type: 'category',
      data: ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9'],
      name: '实际值'
    },
    visualMap: {
      min: 0,
      max: Math.max(...data.map(d => d[2])),
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '15%',
      inRange: {
        color: ['#f0f9ff', '#0ea5e9', '#0369a1']
      }
    },
    series: [{
      type: 'heatmap',
      data: data,
      label: {
        show: true
      }
    }]
  }
})
```

------

### **1.3 UserManagement 用户管理组件**

**文件:** `src/views/users/UserManagement.vue`

**核心功能:**

**1.角色变更**

```
const handleRoleChange = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${user.username} 的角色改为 ${user.role === 'ADMIN' ? '管理员' : '普通用户'} 吗？`,
      '角色变更',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserRole(user.userId, user.role)
    ElMessage.success('角色更新成功')
    fetchUserList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('角色更新失败')
      fetchUserList() // 刷新列表恢复原状态
    }
  }
}
```

**2.状态切换**

```
const handleStatusChange = async (user) => {
  // 防止禁用自己
  if (user.userId === currentUserId.value) {
    ElMessage.warning('不能修改自己的状态')
    user.status = ! user.status
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要${user.status ? '启用' : '禁用'} ${user.username} 吗？`,
      '状态变更',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await updateUserStatus(user.userId, user.status)
    ElMessage.success('状态更新成功')
  } catch (error) {
    if (error === 'cancel') {
      user.status = !user.status
    } else {
      ElMessage.error('状态更新失败')
      fetchUserList()
    }
  }
}
```

**3.用户日志查询**

```
const viewUserLogs = async (user) => {
  selectedUserId.value = user.userId
  logDialogVisible.value = true
  logCurrentPage.value = 1
  await fetchUserLogs()
}

const fetchUserLogs = async () => {
  try {
    logLoading.value = true
    const response = await getUserLogs({
      userId: selectedUserId.value,
      page: logCurrentPage.value,
      size: logPageSize.value
    })
    userLogs.value = response.data.records
    logTotal.value = response.data.total
  } catch (error) {
    ElMessage.error('获取用户日志失败')
  } finally {
    logLoading.value = false
  }
}
```

------

## **2.状态管理设计**

### **2.1 User Store (Pinia)**

**文件:** `src/stores/user.js`

```
import { defineStore } from 'pinia'
import { login, register, validateToken } from '@/api/auth'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('ihdrs_token') || '',
    userInfo: JSON.parse(localStorage.getItem('ihdrs_user_info') || '{}'),
    permissions: [],
    roles: []
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token,
    isAdmin: (state) => state.userInfo.role === 'ADMIN',
    username: (state) => state.userInfo.username || '',
    userId: (state) => state.userInfo.userId || null,
    userRole: (state) => state.userInfo.role || 'USER'
  },
  
  actions: {
    async login(loginForm) {
      try {
        const response = await login(loginForm)
        
        if (response.code === 200) {
          const { token, userInfo } = response.data
          
          this.token = token
          this.userInfo = userInfo
          
          localStorage.setItem('ihdrs_token', token)
          localStorage.setItem('ihdrs_user_info', JSON.stringify(userInfo))
          
          ElMessage.success('登录成功')
          
          const redirect = router.currentRoute.value.query.redirect || '/'
          router.push(redirect)
          
          return response
        }
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },
    
    logout() {
      this.token = ''
      this.userInfo = {}
      this.permissions = []
      this.roles = []
      
      localStorage.removeItem('ihdrs_token')
      localStorage.removeItem('ihdrs_user_info')
      
      ElMessage.success('已退出登录')
      router.push('/login')
    },
    
    async validateToken() {
      if (!this.token) return false
      
      try {
        const response = await validateToken()
        if (response.code === 200) {
          this.userInfo = response.data
          localStorage.setItem('ihdrs_user_info', JSON.stringify(response.data))
          return true
        }
      } catch (error) {
        console.error('Token验证失败:', error)
        this.logout()
        return false
      }
      
      return false
    },
    
    hasPermission(permission) {
      return this.permissions.includes(permission) || this.isAdmin
    },
    
    hasRole(role) {
      return this.roles.includes(role) || this.userRole === role
    }
  }
})
```

------

## **3.路由设计**

### **3.1 路由配置**

**文件:** `src/router/index.js`

```
import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import NProgress from 'nprogress'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表板', icon: 'DataBoard' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/users/UserManagement.vue'),
        meta: { 
          title: '用户管理', 
          icon: 'User',
          requiresAdmin: true 
        }
      },
      {
        path: 'models',
        name: 'ModelManagement',
        component: () => import('@/views/models/ModelManagement.vue'),
        meta: { 
          title: '模型管理', 
          icon: 'DataAnalysis',
          requiresAdmin: true 
        }
      },
      {
        path: 'training',
        name: 'Training',
        component: () => import('@/views/models/Training.vue'),
        meta: { title: '训练任务', icon: 'Loading' }
      },
      {
        path: 'dataset',
        children: [
          {
            path: 'list',
            name: 'DatasetList',
            component: () => import('@/views/dataset/DatasetList.vue'),
            meta: { title: '数据集列表', icon: 'Folder' }
          },
          {
            path: 'upload',
            name: 'DatasetUpload',
            component: () => import('@/views/dataset/DatasetUpload.vue'),
            meta: { title: '上传数据集' }
          },
          {
            path: 'detail/:id',
            name: 'DatasetDetail',
            component: () => import('@/views/dataset/DatasetDetail.vue'),
            meta: { title: '数据集详情' }
          }
        ]
      },
      {
        path: 'recognition',
        children: [
          {
            path: 'history',
            name: 'HistoryManagement',
            component: () => import('@/views/recognition/HistoryManagement.vue'),
            meta: { title: '识别历史' }
          },
          {
            path: 'feedback',
            name: 'FeedbackManagement',
            component: () => import('@/views/recognition/FeedbackManagement.vue'),
            meta: { 
              title: '反馈管理',
              requiresAdmin: true 
            }
          }
        ]
      },
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('@/views/statistics/overview.vue'),
        meta: { title: '统计分析', icon: 'TrendCharts' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'User' }
      }
    ]
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '权限不足' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  
  const userStore = useUserStore()
  const whiteList = ['/login', '/register', '/403', '/404']
  
  document.title = to.meta.title ?  `${to.meta.title} - IHDRS管理端` : 'IHDRS管理端'
  
  if (userStore.token) {
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else {
      const valid = await userStore.validateToken()
      if (valid) {
        if (to.meta.requiresAdmin && !userStore.isAdmin) {
          next({ path: '/403' })
          NProgress.done()
        } else {
          next()
        }
      } else {
        next({ path: '/login', query: { redirect: to.fullPath } })
        NProgress.done()
      }
    }
  } else {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
```

------

## **4.API服务层设计**

### **4.1 Axios请求封装**

**文件:** `src/utils/request.js`

```
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import NProgress from 'nprogress'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    NProgress.start()
    
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    
    console.log(`${config.method?.toUpperCase()} ${config.url}`, config.data || config.params)
    return config
  },
  (error) => {
    NProgress.done()
    console.error('Request Error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    NProgress.done()
    
    // 处理文件下载
    if (response.request.responseType === 'blob') {
      return response.data
    }
    
    const { data } = response
    console.log(`Response:`, data)
    
    if (data.code === 200) {
      return data
    } else {
      ElMessage.error(data.msg || data.message || '请求失败')
      return Promise.reject(new Error(data.msg || data.message || '请求失败'))
    }
  },
  (error) => {
    NProgress.done()
    console.error('Response Error:', error)
    
    let message = '网络错误'
    
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          message = data.msg || '请求参数错误'
          break
        case 401:
          message = '登录已过期，请重新登录'
          const userStore = useUserStore()
          userStore.logout()
          router.push('/login')
          break
        case 403:
          message = '权限不足'
          router.push('/403')
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = data.msg || data.message || `请求失败 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.message === 'Network Error') {
      message = '网络连接异常，请检查网络'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service

// 便捷方法
export const get = (url, params = {}) => {
  return service({ method: 'get', url, params })
}

export const post = (url, data = {}) => {
  return service({ method: 'post', url, data })
}

export const put = (url, data = {}) => {
  return service({ method: 'put', url, data })
}

export const del = (url, params = {}) => {
  return service({ method: 'delete', url, params })
}
```

------

### **4.2 API模块设计**

#### **4.2.1 训练API**

**文件:** `src/api/training.js`

```
import request from '@/utils/request'

/**
 * 获取训练任务列表
 */
export function getTrainingTaskList(params) {
  return request({
    url: '/training/tasks',
    method: 'get',
    params
  })
}

/**
 * 创建训练任务
 */
export function createTrainingTask(data) {
  return request({
    url: '/training/tasks',
    method: 'post',
    data
  })
}

/**
 * 获取训练任务详情
 */
export function getTrainingTaskDetail(taskId) {
  return request({
    url: `/training/tasks/${taskId}`,
    method: 'get'
  })
}

/**
 * 取消训练任务
 */
export function cancelTrainingTask(taskId) {
  return request({
    url: `/training/tasks/${taskId}/cancel`,
    method: 'put'
  })
}

/**
 * 获取训练日志
 */
export function getTrainingLogs(taskId, params = {}) {
  return request({
    url: `/training/tasks/${taskId}/logs`,
    method: 'get',
    params
  })
}

/**
 * 获取训练统计
 */
export function getTrainingStatistics() {
  return request({
    url: '/training/statistics',
    method: 'get'
  })
}

/**
 * 获取实时Batch进度
 */
export function getBatchProgress(taskId) {
  return request({
    url: `/training/tasks/${taskId}/batch-progress`,
    method: 'get'
  })
}
```

------

#### **4.2.2 数据集API**

**文件:** `src/api/dataset.js`

```
import request from '@/utils/request'

/**
 * 获取我的数据集列表
 */
export function getMyDatasets(params) {
  return request({
    url: '/datasets/my',
    method: 'get',
    params
  })
}

/**
 * 获取公开数据集列表
 */
export function getPublicDatasets(params) {
  return request({
    url: '/datasets/public',
    method: 'get',
    params
  })
}

/**
 * 上传数据集
 */
export function uploadDataset(formData) {
  return request({
    url: '/datasets/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000 // 5分钟超时
  })
}

/**
 * 获取数据集详情
 */
export function getDatasetDetail(datasetId) {
  return request({
    url: `/datasets/${datasetId}`,
    method: 'get'
  })
}

/**
 * 更新数据集信息
 */
export function updateDataset(datasetId, data) {
  return request({
    url: `/datasets/${datasetId}`,
    method: 'put',
    data
  })
}

/**
 * 删除数据集
 */
export function deleteDataset(datasetId) {
  return request({
    url: `/datasets/${datasetId}`,
    method: 'delete'
  })
}

/**
 * 设置数据集公开/私有
 */
export function setDatasetPublic(datasetId, isPublic) {
  return request({
    url: `/datasets/${datasetId}/public`,
    method: 'put',
    data: { isPublic }
  })
}

/**
 * 获取可用数据集列表（用于训练）
 */
export function getAvailableDatasets() {
  return request({
    url: '/datasets/available',
    method: 'get'
  })
}
```

------

#### **4.2.3 模型API**

**文件:** `src/api/model.js`

```
import request from '@/utils/request'

/**
 * 获取模型列表
 */
export function getModelList(params) {
  return request({
    url: '/models',
    method: 'get',
    params
  })
}

/**
 * 获取模型详情
 */
export function getModelDetail(modelId) {
  return request({
    url: `/models/${modelId}`,
    method: 'get'
  })
}

/**
 * 获取活跃模型
 */
export function getActiveModel() {
  return request({
    url: '/models/active',
    method: 'get'
  })
}

/**
 * 激活模型
 */
export function switchActiveModel(modelId) {
  return request({
    url: `/models/${modelId}/activate`,
    method: 'put'
  })
}

/**
 * 停用模型
 */
export function disableModel(modelId) {
  return request({
    url: `/models/${modelId}/disable`,
    method: 'put'
  })
}

/**
 * 启用模型
 */
export function enableModel(modelId) {
  return request({
    url: `/models/${modelId}/enable`,
    method: 'put'
  })
}

/**
 * 删除模型
 */
export function deleteModel(modelId) {
  return request({
    url: `/models/${modelId}`,
    method: 'delete'
  })
}

/**
 * 批量删除模型
 */
export function batchDeleteModels(modelIds) {
  return request({
    url: '/models/batch',
    method: 'delete',
    data: { modelIds }
  })
}

/**
 * 获取模型版本列表
 */
export function getModelVersions(modelName) {
  return request({
    url: `/models/versions`,
    method: 'get',
    params: { modelName }
  })
}

/**
 * 对比两个模型
 */
export function compareModels(modelId1, modelId2) {
  return request({
    url: '/models/compare',
    method: 'get',
    params: { modelId1, modelId2 }
  })
}

/**
 * 获取模型统计
 */
export function getModelStatistics() {
  return request({
    url: '/models/statistics',
    method: 'get'
  })
}
```

------

#### **4.2.4 反馈API**

**文件:** `src/api/feedback.js`

```
import request from '@/utils/request'

/**
 * 获取反馈列表
 */
export function getFeedbackList(params) {
  return request({
    url: '/feedback',
    method: 'get',
    params
  })
}

/**
 * 审核反馈
 */
export function reviewFeedback(feedbackId, data) {
  return request({
    url: `/feedback/${feedbackId}/review`,
    method: 'put',
    data
  })
}

/**
 * 批量审核反馈
 */
export function batchReviewFeedback(feedbackIds, status, reviewNote) {
  return request({
    url: '/feedback/batch-review',
    method: 'put',
    data: { feedbackIds, status, reviewNote }
  })
}

/**
 * 导出反馈数据
 */
export function exportFeedback(params, format = 'excel') {
  return request({
    url: `/feedback/export/${format}`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}
```

------

#### **4.2.5 管理员API**

**文件:** `src/api/admin.js`

```
import request from '@/utils/request'

/**
 * 获取用户列表
 */
export function getUserList(params) {
  return request({
    url: '/admin/users',
    method: 'get',
    params
  })
}

/**
 * 更新用户角色
 */
export function updateUserRole(userId, role) {
  return request({
    url: `/admin/users/${userId}/role`,
    method: 'put',
    data: { role }
  })
}

/**
 * 更新用户状态
 */
export function updateUserStatus(userId, status) {
  return request({
    url: `/admin/users/${userId}/status`,
    method: 'put',
    data: { status }
  })
}

/**
 * 获取用户行为日志
 */
export function getUserLogs(params) {
  return request({
    url: '/admin/user-logs',
    method: 'get',
    params
  })
}

/**
 * 获取系统操作日志
 */
export function getOperationLogs(params) {
  return request({
    url: '/admin/operation-logs',
    method: 'get',
    params
  })
}
```

------

## **5.核心算法设计**

### **5.1 数据导出算法**

**文件:** `src/utils/export.js`

```
import * as XLSX from 'xlsx'
import jsPDF from 'jspdf'
import 'jspdf-autotable'

/**
 * 导出为Excel
 */
export function exportToExcel(data, filename, fields) {
  // 1.过滤字段
  const filteredData = data.map(row => {
    const newRow = {}
    fields.forEach(field => {
      newRow[field.label] = formatValue(row[field.key], field.type)
    })
    return newRow
  })
  
  // 2.创建工作表
  const worksheet = XLSX.utils.json_to_sheet(filteredData)
  
  // 3.设置列宽
  const colWidths = fields.map(field => ({
    wch: Math.max(field.label.length, 15)
  }))
  worksheet['!cols'] = colWidths
  
  // 4.创建工作簿
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1')
  
  // 5.导出文件
  XLSX.writeFile(workbook, `${filename}.xlsx`)
}

/**
 * 导出为CSV
 */
export function exportToCSV(data, filename, fields) {
  // 1.构建CSV内容
  const headers = fields.map(f => f.label).join(',')
  
  const rows = data.map(row => {
    return fields.map(field => {
      const value = formatValue(row[field.key], field.type)
      // CSV转义：包含逗号、换行、引号的值需要用引号包裹
      if (typeof value === 'string' && (value.includes(',') || value.includes('\n') || value.includes('"'))) {
        return `"${value.replace(/"/g, '""')}"`
      }
      return value
    }).join(',')
  }).join('\n')
  
  const csvContent = `\ufeff${headers}\n${rows}` // \ufeff 是BOM头，解决Excel打开中文乱码
  
  // 2.创建Blob
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  
  // 3.下载
  downloadBlob(blob, `${filename}.csv`)
}

/**
 * 导出为PDF
 */
export function exportToPDF(data, filename, fields, title = '数据报表') {
  const doc = new jsPDF('landscape') // 横向
  
  // 添加标题
  doc.setFontSize(18)
  doc.text(title, 14, 20)
  
  // 添加生成时间
  doc.setFontSize(10)
  doc.text(`生成时间: ${new Date().toLocaleString('zh-CN')}`, 14, 30)
  
  // 准备表格数据
  const headers = fields.map(f => f.label)
  const body = data.map(row => {
    return fields.map(field => formatValue(row[field.key], field.type))
  })
  
  // 生成表格
  doc.autoTable({
    head: [headers],
    body: body,
    startY: 35,
    styles: {
      font: 'helvetica',
      fontSize: 8,
      cellPadding: 3
    },
    headStyles: {
      fillColor: [64, 158, 255],
      textColor: 255,
      fontStyle: 'bold'
    },
    alternateRowStyles: {
      fillColor: [245, 247, 250]
    }
  })
  
  // 保存
  doc.save(`${filename}.pdf`)
}

/**
 * 格式化值
 */
function formatValue(value, type) {
  if (value === null || value === undefined) return '-'
  
  switch (type) {
    case 'date':
      return new Date(value).toLocaleString('zh-CN')
    case 'percent':
      return `${(value * 100).toFixed(2)}%`
    case 'number':
      return Number(value).toLocaleString()
    case 'boolean':
      return value ? '是' : '否'
    default:
      return String(value)
  }
}

/**
 * 下载Blob
 */
function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
```

**使用示例:**

```
import { exportToExcel, exportToCSV, exportToPDF } from '@/utils/export'

const handleExport = async (format) => {
  const fields = [
    { key: 'recordId', label: '记录ID', type: 'number' },
    { key: 'userId', label: '用户ID', type: 'number' },
    { key: 'recognitionResult', label: '识别结果', type: 'string' },
    { key: 'confidence', label: '置信度', type: 'percent' },
    { key: 'createTime', label: '识别时间', type: 'date' }
  ]
  
  // 获取数据
  const response = await getRecognitionHistory({ scope: 'all' })
  const data = response.data.records
  
  // 导出
  switch (format) {
    case 'excel':
      exportToExcel(data, '识别历史报表', fields)
      break
    case 'csv':
      exportToCSV(data, '识别历史报表', fields)
      break
    case 'pdf':
      exportToPDF(data, '识别历史报表', fields, '识别历史数据报表')
      break
  }
}
```

------

### **5.2 表单验证算法**

**文件:** `src/utils/validate.js`

```
/**
 * 用户名验证
 */
export function validateUsername(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入用户名'))
  } else if (value.length < 3 || value.length > 50) {
    callback(new Error('用户名长度为3-50个字符'))
  } else if (!/^[a-zA-Z0-9_]+$/.test(value)) {
    callback(new Error('用户名只能包含字母、数字和下划线'))
  } else {
    callback()
  }
}

/**
 * 密码验证
 */
export function validatePassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入密码'))
  } else if (value.length < 6 || value.length > 20) {
    callback(new Error('密码长度为6-20个字符'))
  } else if (!/^(? =.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,}$/.test(value)) {
    callback(new Error('密码必须包含字母和数字'))
  } else {
    callback()
  }
}

/**
 * 邮箱验证
 */
export function validateEmail(rule, value, callback) {
  if (!value) {
    callback()
  } else if (!/^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/.test(value)) {
    callback(new Error('邮箱格式不正确'))
  } else {
    callback()
  }
}

/**
 * 手机号验证
 */
export function validatePhone(rule, value, callback) {
  if (!value) {
    callback()
  } else if (!/^1[3-9]\d{9}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

/**
 * 数字范围验证
 */
export function validateNumberRange(min, max) {
  return (rule, value, callback) => {
    if (value === null || value === undefined || value === '') {
      callback()
    } else if (isNaN(value)) {
      callback(new Error('请输入数字'))
    } else if (value < min || value > max) {
      callback(new Error(`请输入${min}~${max}之间的数字`))
    } else {
      callback()
    }
  }
}

/**
 * 学习率验证（训练任务专用）
 */
export function validateLearningRate(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入学习率'))
  } else if (value <= 0 || value > 1) {
    callback(new Error('学习率必须在0-1之间'))
  } else {
    callback()
  }
}

/**
 * Dropout率验证
 */
export function validateDropout(rule, value, callback) {
  if (value < 0 || value >= 1) {
    callback(new Error('Dropout率必须在0-1之间'))
  } else {
    callback()
  }
}
```

------

## **6.数据可视化设计**

### **6.1 ECharts配置工厂**

**文件:** `src/utils/charts.js`

```
/**
 * 创建折线图配置
 */
export function createLineChartOption(config = {}) {
  const {
    title = '',
    xAxisData = [],
    series = [],
    smooth = true,
    showArea = true,
    colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C']
  } = config
  
  return {
    title: {
      text: title,
      left: 'center',
      textStyle: {
        fontSize: 16,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: {
        color: '#606266'
      }
    },
    legend: {
      top: '10%',
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
      data: xAxisData,
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
    color: colors,
    series: series.map((s, index) => ({
      name: s.name,
      type: 'line',
      data: s.data,
      smooth: smooth,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        width: 3,
        color: colors[index % colors.length]
      },
      itemStyle: {
        color: colors[index % colors.length]
      },
      ...(showArea && {
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              {
                offset: 0,
                color: `${colors[index % colors.length]}4D` // 30%透明度
              },
              {
                offset: 1,
                color: `${colors[index % colors.length]}0D` // 5%透明度
              }
            ]
          }
        }
      })
    }))
  }
}

/**
 * 创建柱状图配置
 */
export function createBarChartOption(config = {}) {
  const {
    title = '',
    xAxisData = [],
    seriesData = [],
    color = '#409EFF',
    showLabel = false
  } = config
  
  return {
    title: {
      text: title,
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
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
      data: xAxisData,
      axisLabel: {
        rotate: 45,
        color: '#909399'
      }
    },
    yAxis: {
      type: 'value',
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
      data: seriesData,
      type: 'bar',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: color },
            { offset: 1, color: `${color}CC` }
          ]
        }
      },
      barWidth: '60%',
      ...(showLabel && {
        label: {
          show: true,
          position: 'top'
        }
      })
    }]
  }
}

/**
 * 创建饼图配置
 */
export function createPieChartOption(config = {}) {
  const {
    title = '',
    data = [],
    radius = ['40%', '70%'],
    colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399']
  } = config
  
  return {
    title: {
      text: title,
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      bottom: '5%',
      textStyle: {
        color: '#606266'
      }
    },
    color: colors,
    series: [{
      type: 'pie',
      radius: radius,
      center: ['50%', '45%'],
      data: data,
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
  }
}

/**
 * 创建混淆矩阵热力图配置
 */
export function createConfusionMatrixOption(matrixData, labels = []) {
  // 将二维数组转为ECharts热力图数据格式
  const data = matrixData.flatMap((row, i) =>
    row.map((value, j) => [j, i, value || 0])
  )
  
  const maxValue = Math.max(...data.map(d => d[2]))
  
  return {
    tooltip: {
      position: 'top',
      formatter: (params) => {
        const predicted = labels[params.value[0]] || params.value[0]
        const actual = labels[params.value[1]] || params.value[1]
        const count = params.value[2]
        return `预测: ${predicted}<br/>实际: ${actual}<br/>数量: ${count}`
      }
    },
    grid: {
      height: '70%',
      top: '10%'
    },
    xAxis: {
      type: 'category',
      data: labels.length > 0 ? labels : Array.from({ length: 10 }, (_, i) => String(i)),
      name: '预测值',
      nameLocation: 'middle',
      nameGap: 30,
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: labels.length > 0 ? labels : Array.from({ length: 10 }, (_, i) => String(i)),
      name: '实际值',
      nameLocation: 'middle',
      nameGap: 50,
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '5%',
      inRange: {
        color: ['#f0f9ff', '#0ea5e9', '#0369a1']
      }
    },
    series: [{
      type: 'heatmap',
      data: data,
      label: {
        show: true,
        fontSize: 10
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
}
```

**使用示例:**

```
import { createLineChartOption, createPieChartOption } from '@/utils/charts'

// 识别趋势图
const recognitionTrendOption = computed(() => {
  return createLineChartOption({
    title: '识别趋势',
    xAxisData: weeklyTrend.value.map(d => d.date),
    series: [{
      name: '识别量',
      data: weeklyTrend.value.map(d => d.count)
    }],
    smooth: true,
    showArea: true
  })
})

// 数字分布图
const digitDistributionOption = computed(() => {
  return createPieChartOption({
    title: '数字分布',
    data: digitDistribution.value.map(d => ({
      name: `数字${d.digit}`,
      value: d.count
    })),
    radius: ['40%', '70%']
  })
})
```

------

### **6.2 实时数据更新策略**

```
/**
 * 图表实时更新管理器
 */
class ChartUpdateManager {
  constructor() {
    this.charts = new Map() // 存储图表实例
    this.updateQueue = [] // 更新队列
    this.isUpdating = false
  }
  
  /**
   * 注册图表
   */
  register(chartId, chartInstance) {
    this.charts.set(chartId, chartInstance)
  }
  
  /**
   * 添加数据点
   */
  addDataPoint(chartId, seriesIndex, data) {
    this.updateQueue.push({
      type: 'add',
      chartId,
      seriesIndex,
      data
    })
    
    this.scheduleUpdate()
  }
  
  /**
   * 更新系列数据
   */
  updateSeries(chartId, seriesIndex, data) {
    this.updateQueue.push({
      type: 'update',
      chartId,
      seriesIndex,
      data
    })
    
    this.scheduleUpdate()
  }
  
  /**
   * 调度更新
   */
  scheduleUpdate() {
    if (this.isUpdating) return
    
    this.isUpdating = true
    requestAnimationFrame(() => {
      this.processQueue()
      this.isUpdating = false
    })
  }
  
  /**
   * 处理更新队列
   */
  processQueue() {
    const updates = new Map()
    
    // 合并相同chartId的更新
    this.updateQueue.forEach(update => {
      if (! updates.has(update.chartId)) {
        updates.set(update.chartId, [])
      }
      updates.get(update.chartId).push(update)
    })
    
    // 批量更新
    updates.forEach((updateList, chartId) => {
      const chart = this.charts.get(chartId)
      if (! chart) return
      
      const option = chart.getOption()
      
      updateList.forEach(update => {
        if (update.type === 'add') {
          // 添加数据点
          const series = option.series[update.seriesIndex]
          series.data.push(update.data)
          
          // 限制数据点数量（例如最多显示100个点）
          if (series.data.length > 100) {
            series.data.shift()
            if (option.xAxis && option.xAxis[0].data) {
              option.xAxis[0].data.shift()
            }
          }
        } else if (update.type === 'update') {
          // 更新整个系列
          option.series[update.seriesIndex].data = update.data
        }
      })
      
      chart.setOption(option)
    })
    
    this.updateQueue = []
  }
  
  /**
   * 销毁
   */
  destroy() {
    this.charts.clear()
    this.updateQueue = []
  }
}

export const chartUpdateManager = new ChartUpdateManager()
```

**使用示例:**

```
import { chartUpdateManager } from '@/utils/chartUpdateManager'

// 组件挂载时注册图表
onMounted(() => {
  const chartInstance = chartRef.value
  chartUpdateManager.register('trainingChart', chartInstance)
})

// WebSocket接收数据时更新
const handleEpochEnd = (data) => {
  chartUpdateManager.addDataPoint('trainingChart', 0, {
    name: `Epoch ${data.epoch}`,
    value: [data.epoch, data.accuracy]
  })
}

// 组件卸载时清理
onBeforeUnmount(() => {
  chartUpdateManager.charts.delete('trainingChart')
})
```

------

## **7.附录**

### **7.1 环境变量配置**

**文件:** `.env.development`

```
# 开发环境配置
VITE_APP_TITLE=IHDRS管理端
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws
VITE_UPLOAD_MAX_SIZE=500
```

**文件:** `.env.production`

```
# 生产环境配置
VITE_APP_TITLE=IHDRS管理端
VITE_API_BASE_URL=/api
VITE_WS_BASE_URL=wss://your-domain.com/ws
VITE_UPLOAD_MAX_SIZE=500
```

------

### **7.2 Vite构建配置**

**文件:** `vite.config.js`

```
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  
  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia']
      }),
      Components({
        resolvers: [ElementPlusResolver()]
      })
    ],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '/api')
        },
        '/ws': {
          target: env.VITE_WS_BASE_URL || 'ws://localhost:8080',
          ws: true,
          changeOrigin: true
        }
      }
    },
    build: {
      target: 'es2015',
      outDir: 'dist',
      assetsDir: 'assets',
      sourcemap: mode === 'development',
      rollupOptions: {
        output: {
          manualChunks: {
            'element-plus': ['element-plus'],
            'echarts': ['echarts', 'vue-echarts'],
            'vendor': ['vue', 'vue-router', 'pinia', 'axios']
          }
        }
      },
      chunkSizeWarningLimit: 1000
    },
    optimizeDeps: {
      include: ['vue', 'vue-router', 'pinia', 'axios', 'element-plus', 'echarts']
    }
  }
})
```

------

### **7.3 TypeScript类型定义**

**文件:** `src/types/index.ts`

```
/**
 * 用户信息
 */
export interface UserInfo {
  userId: number
  username: string
  role: 'USER' | 'ADMIN'
  email?: string
  phone?: string
  status: boolean
  loginCount: number
  lastLoginTime: string
  createTime: string
}

/**
 * 训练任务
 */
export interface TrainingTask {
  taskId: number
  taskName: string
  datasetId: number
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  progress: number
  currentEpoch: number
  totalEpochs: number
  bestAccuracy?: number
  finalAccuracy?: number
  finalLoss?: number
  trainingConfig: TrainingConfig
  createTime: string
  startTime?: string
  endTime?: string
}

/**
 * 训练配置
 */
export interface TrainingConfig {
  modelType: string
  batchSize: number
  learningRate: number
  optimizer: string
  epochs: number
  hiddenSize: number
  activation: string
  dropout: number
  useBatchNorm: boolean
  useAugmentation: boolean
  lossFunction: string
  validationSplit: number
}

/**
 * 数据集
 */
export interface Dataset {
  datasetId: number
  datasetName: string
  datasetType: string
  status: 'UPLOADING' | 'PROCESSING' | 'AVAILABLE' | 'ERROR'
  isPublic: boolean
  numClasses?: number
  numSamples?: number
  trainSamples?: number
  testSamples?: number
  imageWidth?: number
  imageHeight?: number
  fileSizeFormatted: string
  description?: string
  createTime: string
  updateTime: string
}

/**
 * API响应
 */
export interface ApiResponse<T = any> {
  code: number
  msg: string
  data: T
  timestamp: number
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
```