// views/recognition/HistoryManagement.vue

<template>
  <div class="history-management">
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
              <DataAnalysis />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">识别历史管理</h1>
        <p class="header-subtitle">Recognition History Management</p>
      </div>

      <!-- 搜索筛选区域 -->
      <el-card class="search-card modern-card" shadow="hover">
        <el-form :model="searchForm" inline>
          <el-form-item label="识别结果">
            <el-select v-model="searchForm.result" placeholder="请选择" clearable style="width: 150px">
              <el-option v-for="i in 10" :key="i-1" :label="i-1" :value="i-1" />
            </el-select>
          </el-form-item>

          <el-form-item label="时间范围">
            <el-date-picker
                v-model="searchForm.dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                style="width: 380px"
            />
          </el-form-item>

          <el-form-item label="用户ID">
            <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable style="width: 200px" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 数据统计卡片 -->
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
              <div class="stat-value">{{ statistics.total || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon>
                  <Select/>
                </el-icon>
              </div>
              <div class="stat-label">识别准确率</div>
              <div class="stat-value">{{ statistics.accuracy || 0 }}%</div>
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
              <div class="stat-label">平均响应时间</div>
              <div class="stat-value">{{ statistics.avgTime || 0 }}ms</div>
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
              <div class="stat-value">{{ statistics.today || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 数据表格 -->
      <el-card class="table-card modern-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">识别历史记录</span>
            <div class="actions">
              <el-button
                  type="danger"
                  :icon="Delete"
                  :disabled="selectedRows.length === 0"
                  @click="handleBatchDelete"
              >
                批量删除
              </el-button>
            </div>
          </div>
        </template>

        <el-table
            v-loading="loading"
            :data="tableData"
            style="width: 100%"
            @selection-change="handleSelectionChange"
            :row-class-name="tableRowClassName"
        >
          <el-table-column type="selection" width="55" />

          <el-table-column prop="recordId" label="记录ID" width="100" />

          <el-table-column label="用户信息" width="150">
            <template #default="{ row }">
              <div class="user-info">
                <el-avatar :size="32">{{ row.userId }}</el-avatar>
                <span class="user-id">ID: {{ row.userId ?? '匿名'}}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="识别图像" width="120">
            <template #default="{ row }">
              <el-image
                  v-if="row.imagePath"
                  :src="row.imagePath"
                  fit="cover"
                  class="table-image"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span v-else class="no-image">无图像</span>
            </template>
          </el-table-column>

          <el-table-column label="识别结果" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" size="large" class="result-tag">
                {{ row.recognitionResult ?? row.sequenceResult }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column label="置信度" width="120" align="center">
            <template #default="{ row }">
              <el-progress
                  :percentage="(row.confidence * 100).toFixed(1)"
                  :color="getConfidenceColor(row.confidence)"
              />
            </template>
          </el-table-column>

          <el-table-column label="模型名称" width="150">
            <template #default="{ row }">
              <span>{{ row.modelName }}</span>
            </template>
          </el-table-column>

          <el-table-column label="模型版本" width="150">
            <template #default="{ row }">
              <span>{{ row.modelVersion }}</span>
            </template>
          </el-table-column>

          <el-table-column label="输入方式" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getInputTypeTag(row.inputType)">
                {{ getInputTypeText(row.inputType) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="processingTime" label="处理时间(ms)" width="120" align="center" />

          <el-table-column label="正确性" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isCorrect === true" type="success">
                <el-icon><Select /></el-icon> 正确
              </el-tag>
              <el-tag v-else-if="row.isCorrect === false" type="danger">
                <el-icon><Close /></el-icon> 错误
              </el-tag>
              <el-tag v-else type="info">未确认</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="识别时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createTime) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">
                查看
              </el-button>
              <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            class="pagination"
        />
      </el-card>

      <!-- 详情对话框 -->
      <el-dialog
          v-model="detailVisible"
          title="识别记录详情"
          width="700px"
          :close-on-click-modal="false"
      >
        <el-descriptions :column="2" border v-if="currentRecord">
          <el-descriptions-item label="记录ID">
            {{ currentRecord.recordId }}
          </el-descriptions-item>
          <el-descriptions-item label="用户ID">
            {{ currentRecord.userId }}
          </el-descriptions-item>
          <el-descriptions-item label="识别结果">
            <el-tag type="primary" size="large">
              {{ currentRecord.recognitionResult ?? currentRecord.sequenceResult }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="置信度">
            {{ (currentRecord.confidence * 100).toFixed(2) }}%
          </el-descriptions-item>
          <el-descriptions-item label="输入方式">
            {{ getInputTypeText(currentRecord.inputType) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ currentRecord.processingTime }}ms
          </el-descriptions-item>
          <el-descriptions-item label="正确性">
            <el-tag v-if="currentRecord.isCorrect === true" type="success">正确</el-tag>
            <el-tag v-else-if="currentRecord.isCorrect === false" type="danger">错误</el-tag>
            <el-tag v-else type="info">未确认</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="识别时间">
            {{ formatTime(currentRecord.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="识别图像" :span="2">
            <el-image
                v-if="currentRecord.imagePath"
                :src="currentRecord.imagePath"
                :preview-src-list="[currentRecord.imagePath]"
                fit="contain"
                style="max-width: 300px; max-height: 300px"
            />
            <span v-else>无图像</span>
          </el-descriptions-item>
        </el-descriptions>

        <template #footer>
          <el-button @click="detailVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Download, Delete, View, Picture,
  DataAnalysis, Select, Timer, Clock, Close
} from '@element-plus/icons-vue'

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
import { getRecognitionHistory, deleteRecognitionRecord, batchDeleteRecords, exportRecognitionHistory } from '@/api/recognition'
import dayjs from 'dayjs'

// 搜索表单
const searchForm = reactive({
  result: null,
  dateRange: [],
  userId: ''
})

// 统计数据
const statistics = ref({
  total: 0,
  accuracy: 0,
  avgTime: 0,
  today: 0
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const selectedRows = ref([])

// 详情对话框
const detailVisible = ref(false)
const currentRecord = ref(null)

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.size,
      result: searchForm.result,
      userId: searchForm.userId || null,
      startTime: searchForm.dateRange?.[0],
      endTime: searchForm.dateRange?.[1]
    }

    const response = await getRecognitionHistory(params)
    if (response.code === 200) {
      const { records, total } = response.data
      console.log('response data:', response.data)
      tableData.value = records || []
      pagination.total = total || 0

      // 更新统计数据
      updateStatistics()
    }
  } catch (error) {
    console.error('获取识别历史失败:', error)
    ElMessage.error('获取识别历史失败')
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = () => {
  if (tableData.value.length > 0) {
    statistics.value.total = pagination.total

    // 计算准确率
    const correctCount = tableData.value.filter(item => item.isCorrect === true).length
    statistics.value.accuracy = ((correctCount / tableData.value.length) * 100).toFixed(1)

    // 计算平均响应时间
    const totalTime = tableData.value.reduce((sum, item) => sum + (item.processingTime || 0), 0)
    statistics.value.avgTime = (totalTime / tableData.value.length).toFixed(0)

    // 统计今日识别数
    const today = dayjs().format('YYYY-MM-DD')
    statistics.value.today = tableData.value.filter(item =>
        dayjs(item.createTime).format('YYYY-MM-DD') === today
    ).length
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.result = null
  searchForm.dateRange = []
  searchForm.userId = ''
  pagination.current = 1
  fetchData()
}

// 查看详情
const handleViewDetail = (row) => {
  currentRecord.value = row
  detailVisible.value = true
}

const tableRowClassName = ({ row }) => {
  if (row.recordId === highlightRecordId.value) {
    return 'highlight-row'
  }
  return ''
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条记录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await deleteRecognitionRecord(row.recordId)
    if (response.code === 200) {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条记录吗?`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const recordIds = selectedRows.value.map(row => row.recordId)
    const response = await batchDeleteRecords(recordIds)
    if (response.code === 200) {
      ElMessage.success('批量删除成功')
      fetchData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 分页变化
const handleSizeChange = (size) => {
  pagination.size = size
  fetchData()
}

const handleCurrentChange = (current) => {
  pagination.current = current
  fetchData()
}

// 获取置信度颜色
const getConfidenceColor = (confidence) => {
  if (confidence >= 0.9) return '#67C23A'
  if (confidence >= 0.7) return '#E6A23C'
  return '#F56C6C'
}

// 获取输入方式标签类型
const getInputTypeTag = (inputType) => {
  const typeMap = {
    'CANVAS': 'primary',
    'MULTI': 'primary',
    'UPLOAD': 'success',
    'CAMERA': 'warning'
  }
  return typeMap[inputType] || 'info'
}

// 获取输入方式文本
const getInputTypeText = (inputType) => {
  const typeMap = {
    'CANVAS': '手写板',
    'MULTI' : '手写板',
    'UPLOAD': '图片上传',
    'CAMERA': '相机拍摄'
  }
  return typeMap[inputType] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

import { useRoute } from 'vue-router'
const route = useRoute()

const highlightRecordId = ref(null)

onMounted(() => {
  if (route.query.recordId) {
    highlightRecordId.value = Number(route.query.recordId)
  }
  fetchData()
})
</script>

<style lang="scss" scoped>
.history-management {
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

  .highlight-row {
    background-color: #ffe58f !important; /* 浅黄色高亮 */
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
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .title {
        font-size: 16px;
        font-weight: bold;
        color: #303133;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;

      .user-id {
        font-size: 13px;
        color: #606266;
      }
    }

    .table-image {
      width: 80px;
      height: 80px;
      border-radius: 8px;
      cursor: pointer;
    }

    .image-placeholder {
      width: 80px;
      height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      border-radius: 8px;
      color: #c0c4cc;
      font-size: 24px;
    }

    .no-image {
      color: #909399;
      font-size: 13px;
    }

    .result-tag {
      font-size: 20px;
      font-weight: bold;
      padding: 8px 16px;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
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
</style>