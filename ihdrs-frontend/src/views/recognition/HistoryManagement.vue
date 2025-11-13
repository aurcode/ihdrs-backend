// views/recognition/HistoryManagement.vue

<template>
  <div class="history-management">
    <!-- 搜索筛选区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="识别结果">
          <el-select v-model="searchForm.result" placeholder="请选择" clearable style="width: 120px">
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
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable style="width: 150px" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card primary">
          <div class="stat-info">
            <div class="stat-value">{{ statistics.total || 0 }}</div>
            <div class="stat-label">总识别次数</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card success">
          <div class="stat-info">
            <div class="stat-value">{{ statistics.accuracy || 0 }}%</div>
            <div class="stat-label">识别准确率</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card warning">
          <div class="stat-info">
            <div class="stat-value">{{ statistics.avgTime || 0 }}ms</div>
            <div class="stat-label">平均响应时间</div>
          </div>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card info">
          <div class="stat-info">
            <div class="stat-value">{{ statistics.today || 0 }}</div>
            <div class="stat-label">今日识别</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="hover">
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
              <span class="user-id">ID: {{ row.userId }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="识别图像" width="120">
          <template #default="{ row }">
            <el-image
                v-if="row.imagePath"
                :src="row.imagePath"
                :preview-src-list="[row.imagePath]"
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
              {{ row.recognitionResult }}
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
          <el-tag type="primary" size="large">{{ currentRecord.recognitionResult }}</el-tag>
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
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Download, Delete, View, Picture,
  DataAnalysis, Select, Timer, Clock, Close
} from '@element-plus/icons-vue'
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
    'UPLOAD': 'success',
    'CAMERA': 'warning'
  }
  return typeMap[inputType] || 'info'
}

// 获取输入方式文本
const getInputTypeText = (inputType) => {
  const typeMap = {
    'CANVAS': '手写板',
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
  padding: 20px;

  .search-card {
    margin-bottom: 20px;
  }

  .highlight-row {
    background-color: #ffe58f !important; /* 浅黄色高亮 */
  }

  .stats-row {
    margin-bottom: 20px;

    .stat-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      padding: 20px;
      color: white;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      transition: transform 0.3s;

      &:hover {
        transform: translateY(-4px);
      }

      &.primary {
        background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
      }

      &.success {
        background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
      }

      &.warning {
        background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
      }

      &.info {
        background: linear-gradient(135deg, #909399 0%, #b4bccc 100%);
      }

      .stat-icon {
        font-size: 40px;
        opacity: 0.9;
      }

      .stat-info {
        flex: 1;

        .stat-value {
          font-size: 28px;
          font-weight: bold;
          margin-bottom: 4px;
        }

        .stat-label {
          font-size: 14px;
          opacity: 0.9;
        }
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
</style>
