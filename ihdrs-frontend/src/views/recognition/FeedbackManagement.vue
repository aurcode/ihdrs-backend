// views/recognition/FeedbackManagement.vue

<template>
  <div class="feedback-management">
    <!-- 搜索筛选区域 -->
    <el-card class="search-card" shadow="hover">
      <el-form :model="searchForm" inline>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 150px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已接受" value="ACCEPTED" />
            <el-option label="已拒绝" value="REJECTED" />
          </el-select>
        </el-form-item>

        <el-form-item label="反馈类型">
          <el-select v-model="searchForm.feedbackType" placeholder="请选择" clearable style="width: 150px">
            <el-option label="识别错误" value="WRONG_RESULT" />
            <el-option label="置信度低" value="LOW_CONFIDENCE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card primary">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.total }}</div>
            <div class="stat-label">总反馈数</div>
          </div>
          <el-icon :size="48" class="stat-icon"><ChatLineRound /></el-icon>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card warning">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.pending }}</div>
            <div class="stat-label">待审核</div>
          </div>
          <el-icon :size="48" class="stat-icon"><Clock /></el-icon>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card success">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.accepted }}</div>
            <div class="stat-label">已接受</div>
          </div>
          <el-icon :size="48" class="stat-icon"><Select /></el-icon>
        </div>
      </el-col>

      <el-col :xs="12" :sm="6" :md="6" :lg="6">
        <div class="stat-card danger">
          <div class="stat-content">
            <div class="stat-value">{{ statistics.rejected }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
          <el-icon :size="48" class="stat-icon"><Close /></el-icon>
        </div>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="title">用户反馈列表</span>
          <div class="actions">
            <el-button
                type="success"
                :icon="Select"
                :disabled="selectedRows.value?.length === 0"
                @click="handleBatchReview('ACCEPTED')"
            >
              批量接受
            </el-button>
            <el-button
                type="danger"
                :icon="Close"
                :disabled="selectedRows.value?.length === 0"
                @click="handleBatchReview('REJECTED')"
            >
              批量拒绝
            </el-button>
          </div>
        </div>
      </template>

      <el-table
          v-loading="loading"
          :data="tableData"
          style="width: 100%"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />

        <el-table-column prop="feedbackId" label="反馈ID" width="100" />

        <el-table-column label="用户信息" width="120">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32">{{ row.userId }}</el-avatar>
              <span>ID: {{ row.userId }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="识别记录" width="120" align="center">
          <template #default="{ row }">
            <el-link type="primary" @click="handleViewRecord(row.recordId)">
              #{{ row.recordId }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column label="原始结果 → 正确结果" width="160" align="center">
          <template #default="{ row }">
            <div class="result-compare">
              <el-tag type="danger" size="large">{{ row.originalResult }}</el-tag>
              <el-icon><Right /></el-icon>
              <el-tag type="success" size="large">{{ row.correctResult }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="反馈类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getFeedbackTypeTag(row.feedbackType)">
              {{ getFeedbackTypeText(row.feedbackType) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="feedbackReason" label="反馈原因" min-width="200" show-overflow-tooltip />

        <el-table-column label="质量评分" width="120" align="center">
          <template #default="{ row }">
            <el-rate
                v-model="row.qualityScore"
                disabled
                show-score
                text-color="#ff9900"
            />
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
                type="primary"
                link
                :icon="View"
                @click="handleViewDetail(row)"
            >
              查看
            </el-button>
            <el-button
                v-if="row.status === 'PENDING'"
                type="success"
                link
                :icon="Select"
                @click="handleReview(row, 'ACCEPTED')"
            >
              接受
            </el-button>
            <el-button
                v-if="row.status === 'PENDING'"
                type="danger"
                link
                :icon="Close"
                @click="handleReview(row, 'REJECTED')"
            >
              拒绝
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
        title="反馈详情"
        width="800px"
        :close-on-click-modal="false"
    >
      <el-descriptions :column="2" border v-if="currentFeedback">
        <el-descriptions-item label="反馈ID">
          {{ currentFeedback.feedbackId }}
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">
          {{ currentFeedback.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="识别记录ID">
          <el-link type="primary" @click="handleViewRecord(currentFeedback.recordId)">
            #{{ currentFeedback.recordId }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="反馈类型">
          <el-tag :type="getFeedbackTypeTag(currentFeedback.feedbackType)">
            {{ getFeedbackTypeText(currentFeedback.feedbackType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="原始结果">
          <el-tag type="danger" size="large">{{ currentFeedback.originalResult }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="正确结果">
          <el-tag type="success" size="large">{{ currentFeedback.correctResult }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="质量评分">
          <el-rate v-model="currentFeedback.qualityScore" disabled show-score />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(currentFeedback.status)">
            {{ getStatusText(currentFeedback.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="反馈原因" :span="2">
          {{ currentFeedback.feedbackReason || '无' }}
        </el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2" v-if="currentFeedback.reviewNote">
          {{ currentFeedback.reviewNote }}
        </el-descriptions-item>
        <el-descriptions-item label="审核人" v-if="currentFeedback.reviewerId">
          {{ currentFeedback.reviewerId }}
        </el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="currentFeedback.reviewTime">
          {{ formatTime(currentFeedback.reviewTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">
          {{ formatTime(currentFeedback.createTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
            v-if="currentFeedback && currentFeedback.status === 'PENDING'"
            type="success"
            @click="handleReview(currentFeedback, 'ACCEPTED')"
        >
          接受
        </el-button>
        <el-button
            v-if="currentFeedback && currentFeedback.status === 'PENDING'"
            type="danger"
            @click="handleReview(currentFeedback, 'REJECTED')"
        >
          拒绝
        </el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog
        v-model="reviewVisible"
        :title="reviewAction === 'ACCEPTED' ? '接受反馈' : '拒绝反馈'"
        width="500px"
        :close-on-click-modal="false"
    >
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="审核备注">
          <el-input
              v-model="reviewForm.reviewNote"
              type="textarea"
              :rows="4"
              placeholder="请输入审核备注（选填）"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReview">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Download, View, Select, Close, Clock,
  ChatLineRound, Right
} from '@element-plus/icons-vue'
import { getFeedbackList, reviewFeedback, batchReviewFeedback, exportFeedback } from '@/api/feedback'
import dayjs from 'dayjs'

// 搜索表单
const searchForm = reactive({
  status: '',
  feedbackType: ''
})

// 统计数据
const statistics = ref({
  total: 0,
  pending: 0,
  accepted: 0,
  rejected: 0
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
const currentFeedback = ref(null)

// 审核对话框
const reviewVisible = ref(false)
const reviewAction = ref('')
const reviewForm = reactive({
  reviewNote: ''
})
const reviewingFeedback = ref(null)

// 获取列表数据
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      current: pagination.current,
      size: pagination.size,
      status: searchForm.status,
      feedbackType: searchForm.feedbackType
    }

    const response = await getFeedbackList(params)
    if (response.code === 200) {
      const { records, total } = response.data
      tableData.value = records || []
      pagination.total = total || 0

      // 更新统计数据
      updateStatistics()
    }
  } catch (error) {
    console.error('获取反馈列表失败:', error)
    ElMessage.error('获取反馈列表失败')
  } finally {
    loading.value = false
  }
}

// 更新统计数据
const updateStatistics = () => {
  statistics.value.total = pagination.total
  statistics.value.pending = tableData.value.filter(item => item.status === 'PENDING').length
  statistics.value.accepted = tableData.value.filter(item => item.status === 'ACCEPTED').length
  statistics.value.rejected = tableData.value.filter(item => item.status === 'REJECTED').length
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

// 重置
const handleReset = () => {
  searchForm.status = ''
  searchForm.feedbackType = ''
  pagination.current = 1
  fetchData()
}

// 查看详情
const handleViewDetail = (row) => {
  currentFeedback.value = row
  detailVisible.value = true
}

// 查看识别记录
const handleViewRecord = (recordId) => {
  // 跳转到识别历史页面并高亮显示该记录
  ElMessage.info(`查看识别记录 #${recordId}`)
  // 可以通过路由跳转到识别历史管理页面
  // router.push({ name: 'HistoryManagement', query: { recordId } })
}

// 审核反馈
const handleReview = (row, action) => {
  reviewingFeedback.value = row
  reviewAction.value = action
  reviewForm.reviewNote = ''
  reviewVisible.value = true
}

// 确认审核
const confirmReview = async () => {
  try {
    const response = await reviewFeedback(reviewingFeedback.value.feedbackId, {
      status: reviewAction.value,
      reviewNote: reviewForm.reviewNote
    })

    if (response.code === 200) {
      ElMessage.success('审核成功')
      reviewVisible.value = false
      detailVisible.value = false
      fetchData()
    }
  } catch (error) {
    console.error('审核失败:', error)
    ElMessage.error('审核失败')
  }
}

// 批量审核
const handleBatchReview = async (action) => {
  try {
    await ElMessageBox.confirm(
        `确定要${action === 'ACCEPTED' ? '接受' : '拒绝'}选中的 ${selectedRows.value.length} 条反馈吗?`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )

    const feedbackIds = selectedRows.value.map(row => row.feedbackId)
    const response = await batchReviewFeedback(feedbackIds, action, '')

    if (response.code === 200) {
      ElMessage.success('批量审核成功')
      fetchData()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量审核失败:', error)
      ElMessage.error('批量审核失败')
    }
  }
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection.filter(row => row.status === 'PENDING')
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

// 获取反馈类型标签类型
const getFeedbackTypeTag = (type) => {
  const typeMap = {
    'WRONG_RESULT': 'danger',
    'LOW_CONFIDENCE': 'warning',
    'OTHER': 'info'
  }
  return typeMap[type] || 'info'
}

// 获取反馈类型文本
const getFeedbackTypeText = (type) => {
  const typeMap = {
    'WRONG_RESULT': '识别错误',
    'LOW_CONFIDENCE': '置信度低',
    'OTHER': '其他'
  }
  return typeMap[type] || '未知'
}

// 获取状态标签类型
const getStatusTag = (status) => {
  const statusMap = {
    'PENDING': 'warning',
    'ACCEPTED': 'success',
    'REJECTED': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'PENDING': '待审核',
    'ACCEPTED': '已接受',
    'REJECTED': '已拒绝'
  }
  return statusMap[status] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.feedback-management {
  padding: 20px;

  .search-card {
    margin-bottom: 20px;
  }

  .stats-row {
    margin-bottom: 20px;

    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
      transition: all 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
      }

      &.primary {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;

        .stat-icon {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      &.warning {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: white;

        .stat-icon {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      &.success {
        background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
        color: white;

        .stat-icon {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      &.danger {
        background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
        color: white;

        .stat-icon {
          color: rgba(255, 255, 255, 0.8);
        }
      }

      .stat-content {
        .stat-value {
          font-size: 32px;
          font-weight: bold;
          margin-bottom: 8px;
        }

        .stat-label {
          font-size: 14px;
          opacity: 0.9;
        }
      }

      .stat-icon {
        font-size: 48px;
        opacity: 0.6;
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

      .actions {
        display: flex;
        gap: 10px;
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
    }

    .result-compare {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;

      .el-tag {
        font-size: 18px;
        font-weight: bold;
        padding: 6px 12px;
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
