// views/recognition/FeedbackManagement.vue

<template>
  <div class="feedback-management">
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
              <ChatLineRound />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">用户反馈管理</h1>
        <p class="header-subtitle">Feedback Management</p>
      </div>

      <!-- 搜索筛选区域 -->
      <el-card class="search-card modern-card" shadow="hover">
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

      <!-- 数据统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card primary">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><ChatLineRound/></el-icon>
              </div>
              <div class="stat-label">总反馈数</div>
              <div class="stat-value">{{ statistics.total || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card warning">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Clock/></el-icon>
              </div>
              <div class="stat-label">待审核</div>
              <div class="stat-value">{{ statistics.pending || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card success">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Select/></el-icon>
              </div>
              <div class="stat-label">已接受</div>
              <div class="stat-value">{{ statistics.accepted || 0 }}</div>
            </div>
          </div>
        </el-col>

        <el-col :xs="12" :sm="6" :md="6" :lg="6" :xl="6">
          <div class="stat-card danger">
            <div class="stat-background"></div>
            <div class="stat-content">
              <div class="stat-icon">
                <el-icon><Close/></el-icon>
              </div>
              <div class="stat-label">已拒绝</div>
              <div class="stat-value">{{ statistics.rejected || 0 }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 数据表格 -->
      <el-card class="table-card modern-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="title">用户反馈列表</span>
            <div class="actions">
              <!-- 导出按钮组 -->
              <el-dropdown @command="handleExport" trigger="click">
                <el-button type="success" :icon="Download" :loading="exportLoading">
                  导出报表 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="excel">
                      <el-icon><Document /></el-icon> 导出为 Excel
                    </el-dropdown-item>
                    <el-dropdown-item command="csv">
                      <el-icon><Tickets /></el-icon> 导出为 CSV
                    </el-dropdown-item>
                    <el-dropdown-item command="pdf">
                      <el-icon><Reading /></el-icon> 导出为 PDF
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                  type="warning"
                  :icon="FolderOpened"
                  @click="openTrainingDatasetDialog"
                  style="margin-left: 8px"
              >
                生成训练集
              </el-button>
              <el-button
                  type="success"
                  :icon="Select"
                  :disabled="selectedRows.length === 0"
                  @click="handleBatchReview('ACCEPTED')"
              >
                批量接受
              </el-button>
              <el-button
                  type="danger"
                  :icon="Close"
                  :disabled="selectedRows.length === 0"
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

          <el-table-column label="反馈图像" width="120">
            <template #default="{ row }">
              <el-image
                  v-if="row.recordInfo?.imagePath"
                  :src="row.recordInfo.imagePath"
                  fit="cover"
                  style="width: 60px; height: 60px; border-radius: 6px"
              >
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
              <span v-else>无图像</span>
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

          <el-table-column label="模型名称" width="150">
            <template #default="{ row }">
              <span>{{ row.modelName || '未知' }}</span>
            </template>
          </el-table-column>

          <el-table-column label="模型版本" width="150">
            <template #default="{ row }">
              <span>{{ row.modelVersion || '未知' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="feedbackReason" label="反馈原因" min-width="200" show-overflow-tooltip />

          <el-table-column label="质量评分" width="120" align="center">
            <template #default="{ row }">
              <el-rate v-model="row.qualityScore" disabled show-score text-color="#ff9900" />
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
              <el-button type="primary" link :icon="View" @click="handleViewDetail(row)">查看</el-button>
              <el-button v-if="row.status === 'PENDING'" type="success" link :icon="Select" @click="handleReview(row, 'ACCEPTED')">接受</el-button>
              <el-button v-if="row.status === 'PENDING'" type="danger" link :icon="Close" @click="handleReview(row, 'REJECTED')">拒绝</el-button>
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
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="800px" :close-on-click-modal="false">
      <el-descriptions :column="2" border v-if="currentFeedback">
        <el-descriptions-item label="反馈ID">{{ currentFeedback.feedbackId }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentFeedback.userId }}</el-descriptions-item>
        <el-descriptions-item label="识别记录ID">
          <el-link type="primary" @click="handleViewRecord(currentFeedback.recordId)">#{{ currentFeedback.recordId }}</el-link>
        </el-descriptions-item>
        <el-descriptions-item label="识别图像" :span="2">
          <el-image
              v-if="currentFeedback?.recordInfo?.imagePath"
              :src="currentFeedback.recordInfo.imagePath"
              :preview-src-list="[currentFeedback.recordInfo.imagePath]"
              fit="contain"
              style="max-width: 300px; max-height: 300px"
          />
          <span v-else>无图像</span>
        </el-descriptions-item>
        <el-descriptions-item label="反馈类型">
          <el-tag :type="getFeedbackTypeTag(currentFeedback.feedbackType)">{{ getFeedbackTypeText(currentFeedback.feedbackType) }}</el-tag>
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
          <el-tag :type="getStatusTag(currentFeedback.status)">{{ getStatusText(currentFeedback.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模型名称">{{ currentFeedback.modelName || currentFeedback.recordInfo?.modelName || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="模型版本">{{ currentFeedback.modelVersion || currentFeedback.recordInfo?.modelVersion || '未知' }}</el-descriptions-item>
        <el-descriptions-item label="反馈原因" :span="2">{{ currentFeedback.feedbackReason || '无' }}</el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2" v-if="currentFeedback.reviewNote">{{ currentFeedback.reviewNote }}</el-descriptions-item>
        <el-descriptions-item label="审核人" v-if="currentFeedback.reviewerId">{{ currentFeedback.reviewerId }}</el-descriptions-item>
        <el-descriptions-item label="审核时间" v-if="currentFeedback.reviewTime">{{ formatTime(currentFeedback.reviewTime) }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(currentFeedback.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="currentFeedback && currentFeedback.status === 'PENDING'" type="success" @click="handleReview(currentFeedback, 'ACCEPTED')">接受</el-button>
        <el-button v-if="currentFeedback && currentFeedback.status === 'PENDING'" type="danger" @click="handleReview(currentFeedback, 'REJECTED')">拒绝</el-button>
      </template>
    </el-dialog>

    <!-- 审核对话框 -->
    <el-dialog v-model="reviewVisible" :title="reviewAction === 'ACCEPTED' ? '接受反馈' : '拒绝反馈'" width="500px" :close-on-click-modal="false">
      <el-form :model="reviewForm" label-width="100px">
        <el-form-item label="审核备注">
          <el-input v-model="reviewForm.reviewNote" type="textarea" :rows="4" placeholder="请输入审核备注（选填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReview">确定</el-button>
      </template>
    </el-dialog>

    <!-- 导出设置对话框 -->
    <el-dialog v-model="exportDialogVisible" title="导出设置" width="500px" :close-on-click-modal="false">
      <el-form :model="exportSettings" label-width="100px">
        <el-form-item label="导出范围">
          <el-radio-group v-model="exportSettings.scope">
            <el-radio value="current">当前页数据</el-radio>
            <el-radio value="all">全部数据</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="导出字段">
          <el-checkbox-group v-model="exportSettings.fields">
            <el-checkbox value="feedbackId">反馈ID</el-checkbox>
            <el-checkbox value="userId">用户ID</el-checkbox>
            <el-checkbox value="recordId">记录ID</el-checkbox>
            <el-checkbox value="originalResult">原始结果</el-checkbox>
            <el-checkbox value="correctResult">正确结果</el-checkbox>
            <el-checkbox value="feedbackType">反馈类型</el-checkbox>
            <el-checkbox value="feedbackReason">反馈原因</el-checkbox>
            <el-checkbox value="qualityScore">质量评分</el-checkbox>
            <el-checkbox value="status">状态</el-checkbox>
            <el-checkbox value="modelName">模型名称</el-checkbox>
            <el-checkbox value="modelVersion">模型版本</el-checkbox>
            <el-checkbox value="createTime">提交时间</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExport" :loading="exportLoading">确定导出</el-button>
      </template>
    </el-dialog>
    <el-dialog
        v-model="trainingDatasetVisible"
        title="生成训练集"
        width="700px"
        :close-on-click-modal="false"
    >
      <el-alert
          title="功能说明"
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px"
      >
        将已审核通过(ACCEPTED)的单数字用户反馈图片，按正确结果分类整理成训练集。
        生成的训练集可用于模型增量训练或微调。
      </el-alert>

      <el-form :model="trainingDatasetForm" label-width="120px">
        <el-form-item label="训练集名称">
          <el-input
              v-model="trainingDatasetForm.datasetName"
              placeholder="留空则自动生成"
              maxlength="50"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
              v-model="trainingDatasetForm.description"
              type="textarea"
              :rows="2"
              placeholder="训练集描述（选填）"
              maxlength="200"
          />
        </el-form-item>

        <el-form-item label="时间范围">
          <el-date-picker
              v-model="trainingDatasetForm.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="最低质量评分">
          <el-slider
              v-model="trainingDatasetForm.minQualityScore"
              :min="0"
              :max="5"
              :step="1"
              show-stops
              :marks="{0: '0', 1: '1', 2: '2', 3: '3', 4: '4', 5: '5'}"
          />
        </el-form-item>

        <el-form-item label="图片格式">
          <el-radio-group v-model="trainingDatasetForm.resizeToMNIST">
            <el-radio :value="true">调整为 28×28 MNIST格式</el-radio>
            <el-radio :value="false">保持原始尺寸</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="导出格式">
          <el-radio-group v-model="trainingDatasetForm.exportFormat">
            <el-radio value="zip">ZIP压缩包</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <!-- 预览统计 -->
      <el-card v-if="trainingPreviewData" class="preview-card" shadow="never">
        <template #header>
          <span>数据预览</span>
        </template>
        <el-descriptions :column="2" size="small">
          <el-descriptions-item label="可用图片总数">
            <el-tag type="primary">{{ trainingPreviewData.totalCount }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="distribution-chart" style="margin-top: 15px">
          <div class="distribution-title">各数字分布：</div>
          <div class="distribution-bars">
            <div
                v-for="digit in 10"
                :key="digit - 1"
                class="distribution-item"
            >
              <span class="digit-label">{{ digit - 1 }}</span>
              <el-progress
                  :percentage="getDistributionPercentage(digit - 1)"
                  :format="() => trainingPreviewData.distribution[digit - 1] || 0"
                  :stroke-width="18"
              />
            </div>
          </div>
        </div>
      </el-card>

      <template #footer>
        <el-button @click="trainingDatasetVisible = false">取消</el-button>
        <el-button type="info" @click="handlePreviewTrainingData" :loading="previewLoading">
          预览数据
        </el-button>
        <el-button
            type="primary"
            @click="handleGenerateTrainingDataset"
            :loading="generateLoading"
            :disabled="!trainingPreviewData || trainingPreviewData.totalCount === 0"
        >
          生成训练集
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Refresh, Download, View, Select, Close, Clock,
  ChatLineRound, Right, Picture, ArrowDown, Document, Tickets, Reading
} from '@element-plus/icons-vue'
import { FolderOpened } from '@element-plus/icons-vue'
import { getFeedbackList, reviewFeedback, batchReviewFeedback, exportFeedback, previewTrainingData, generateTrainingDataset } from '@/api/feedback'
import dayjs from 'dayjs'
import router from "@/router/index.js"

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

// 导出相关
const exportLoading = ref(false)
const exportDialogVisible = ref(false)
const exportFormat = ref('excel')
const exportSettings = reactive({
  scope: 'all',
  fields: ['feedbackId', 'userId', 'recordId', 'originalResult', 'correctResult', 'feedbackType', 'feedbackReason', 'qualityScore', 'status', 'modelName', 'modelVersion', 'createTime']
})

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
  router.push({
    name: 'HistoryManagement',
    query: { recordId }
  })
}

// 审核反馈
const handleReview = (row, action) => {
  reviewingFeedback.value = row
  reviewAction.value = action
  reviewForm.reviewNote = ''
  reviewVisible.value = true
}

// 添加训练集相关状态
const trainingDatasetVisible = ref(false)
const previewLoading = ref(false)
const generateLoading = ref(false)
const trainingPreviewData = ref(null)

const trainingDatasetForm = reactive({
  datasetName: '',
  description: '',
  timeRange: null,
  minQualityScore: 0,
  resizeToMNIST: true,
  exportFormat: 'zip'
})

// 打开训练集对话框
const openTrainingDatasetDialog = () => {
  trainingPreviewData.value = null
  trainingDatasetForm.datasetName = ''
  trainingDatasetForm.description = ''
  trainingDatasetForm.timeRange = null
  trainingDatasetForm.minQualityScore = 0
  trainingDatasetForm.resizeToMNIST = true
  trainingDatasetForm.exportFormat = 'zip'
  trainingDatasetVisible.value = true
}

// 预览训练数据
const handlePreviewTrainingData = async () => {
  previewLoading.value = true
  try {
    const params = {
      startTime: trainingDatasetForm.timeRange?.[0] || null,
      endTime: trainingDatasetForm.timeRange?.[1] || null,
      minQualityScore: trainingDatasetForm.minQualityScore || null
    }

    const response = await previewTrainingData(params)
    if (response.code === 200) {
      trainingPreviewData.value = response.data
      if (response.data.totalCount === 0) {
        ElMessage.warning('没有符合条件的反馈数据')
      }
    }
  } catch (error) {
    console.error('预览训练数据失败:', error)
    ElMessage.error('预览训练数据失败')
  } finally {
    previewLoading.value = false
  }
}

// 生成训练集
const handleGenerateTrainingDataset = async () => {
  if (!trainingPreviewData.value || trainingPreviewData.value.totalCount === 0) {
    ElMessage.warning('请先预览数据，确保有可用的反馈图片')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要生成包含 ${trainingPreviewData.value.totalCount} 张图片的训练集吗？`,
        '确认生成',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
    )

    generateLoading.value = true

    const params = {
      datasetName: trainingDatasetForm.datasetName || null,
      description: trainingDatasetForm.description || null,
      startTime: trainingDatasetForm.timeRange?.[0] || null,
      endTime: trainingDatasetForm.timeRange?.[1] || null,
      minQualityScore: trainingDatasetForm.minQualityScore || null,
      resizeToMNIST: trainingDatasetForm.resizeToMNIST,
      exportFormat: trainingDatasetForm.exportFormat
    }

    const response = await generateTrainingDataset(params)
    if (response.code === 200) {
      ElMessage.success(`训练集生成成功！共 ${response.data.totalImages} 张图片`)
      trainingDatasetVisible.value = false

      // 显示下载链接
      ElMessageBox.alert(
          `<div>
          <p><strong>训练集名称：</strong>${response.data.datasetName}</p>
          <p><strong>图片总数：</strong>${response.data.totalImages}</p>
          <p><strong>下载地址：</strong><a href="${response.data.downloadUrl}" target="_blank">${response.data.downloadUrl}</a></p>
        </div>`,
          '训练集生成完成',
          {
            dangerouslyUseHTMLString: true,
            confirmButtonText: '确定'
          }
      )
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('生成训练集失败:', error)
      ElMessage.error('生成训练集失败')
    }
  } finally {
    generateLoading.value = false
  }
}

// 计算分布百分比
const getDistributionPercentage = (digit) => {
  if (! trainingPreviewData.value || ! trainingPreviewData.value.distribution) {
    return 0
  }
  const count = trainingPreviewData.value.distribution[digit] || 0
  const total = trainingPreviewData.value.totalCount || 1
  return Math.round((count / total) * 100)
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
        `确定要${action === 'ACCEPTED' ? '接受' : '拒绝'}选中的 ${selectedRows.value.length} 条反馈吗? `,
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

// 导出处理
const handleExport = (format) => {
  exportFormat.value = format
  exportDialogVisible.value = true
}

// 确认导出
const confirmExport = async () => {
  exportLoading.value = true
  try {
    const params = {
      status: searchForm.status,
      feedbackType: searchForm.feedbackType,
      scope: exportSettings.scope,
      fields: exportSettings.fields.join(',')
    }

    // 如果是当前页，添加分页参数
    if (exportSettings.scope === 'current') {
      params.current = pagination.current
      params.size = pagination.size
    }

    const response = await exportFeedback(params, exportFormat.value)

    // 处理文件下载
    downloadFile(response, `反馈数据报表_${dayjs().format('YYYYMMDDHHmmss')}`, exportFormat.value)

    ElMessage.success('导出成功')
    exportDialogVisible.value = false
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// 文件下载工具函数
const downloadFile = (data, filename, format) => {
  const mimeTypes = {
    excel: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    csv: 'text/csv;charset=utf-8',
    pdf: 'application/pdf'
  }
  const extensions = {
    excel: '.xlsx',
    csv: '.csv',
    pdf: '.pdf'
  }

  const blob = new Blob([data], { type: mimeTypes[format] })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename + extensions[format]
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
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

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.feedback-management {
  position: relative;
  min-height: 100vh;
  padding: 24px;
  padding-bottom: 60px;
  overflow: visible;
  width: 100%;
  box-sizing: border-box;

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

  .content-wrapper {
    position: relative;
    z-index: 1;
  }

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

      &.warning .stat-content .stat-icon,
      &.danger .stat-content .stat-icon {
        filter: brightness(2.2);
      }

      &.primary .stat-background {
        background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
      }

      &.warning .stat-background {
        background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
      }

      &.success .stat-background {
        background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
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

    .image-placeholder {
      width: 60px;
      height: 60px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      border-radius: 6px;
      color: #c0c4cc;
      font-size: 20px;
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
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

.preview-card {
  margin-top: 20px;
  background: #f5f7fa;

  .distribution-title {
    font-weight: 500;
    margin-bottom: 10px;
    color: #606266;
  }

  .distribution-bars {
    .distribution-item {
      display: flex;
      align-items: center;
      margin-bottom: 8px;

      .digit-label {
        width: 30px;
        font-weight: bold;
        color: #409EFF;
        text-align: center;
      }

      .el-progress {
        flex: 1;
        margin-left: 10px;
      }
    }
  }
}
</style>