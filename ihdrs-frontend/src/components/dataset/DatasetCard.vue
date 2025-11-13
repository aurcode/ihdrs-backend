// components/dataset/DatasetCard.vue

<template>
  <div class="dataset-card">
    <div class="card-header">
      <div class="header-left">
        <el-icon :size="24" color="#409EFF">
          <Folder />
        </el-icon>
        <span class="dataset-name">{{ dataset.datasetName }}</span>
      </div>
      <div class="header-right">
        <el-tag :type="statusInfo.type" size="small">
          {{ statusInfo.text }}
        </el-tag>
      </div>
    </div>

    <div class="card-body">
      <p class="description">
        {{ dataset.description || '暂无描述' }}
      </p>

      <div class="info-grid">
        <div class="info-item">
          <span class="label">类别数量:</span>
          <span class="value">{{ dataset.numClasses || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">样本总数:</span>
          <span class="value">{{ dataset.numSamples || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">训练集:</span>
          <span class="value">{{ dataset.trainSamples || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">测试集:</span>
          <span class="value">{{ dataset.testSamples || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">图像尺寸:</span>
          <span class="value">
            {{ dataset.imageWidth && dataset.imageHeight
              ? `${dataset.imageWidth}×${dataset.imageHeight}`
              : '-' }}
          </span>
        </div>
        <div class="info-item">
          <span class="label">文件大小:</span>
          <span class="value">{{ formattedFileSize }}</span>
        </div>
      </div>

      <div class="meta-info">
        <el-icon :size="14"><Clock /></el-icon>
        <span>{{ relativeTime }}</span>
        <el-tag v-if="dataset.isPublic" size="small" type="success">公开</el-tag>
      </div>
    </div>

    <div class="card-footer">
      <el-button size="small" @click="handleView">
        查看详情
      </el-button>
      <el-dropdown @command="handleCommand" v-if="showActions">
        <el-button size="small">
          更多
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit">
              <el-icon><Edit /></el-icon>
              编辑
            </el-dropdown-item>
            <el-dropdown-item command="togglePublic">
              <el-icon><Share /></el-icon>
              {{ dataset.isPublic ? '设为私有' : '设为公开' }}
            </el-dropdown-item>
            <el-dropdown-item command="delete" divided>
              <el-icon color="#F56C6C"><Delete /></el-icon>
              <span style="color: #F56C6C">删除</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 错误信息提示 -->
    <el-alert
        v-if="dataset.status === 'ERROR' && dataset.errorMessage"
        :title="dataset.errorMessage"
        type="error"
        :closable="false"
        style="margin-top: 12px"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Folder, Clock, Edit, Delete, Share, ArrowDown } from '@element-plus/icons-vue'
import { formatFileSize, formatDatasetStatus, getRelativeTime } from '@/utils/format'

const props = defineProps({
  dataset: {
    type: Object,
    required: true
  },
  showActions: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['view', 'edit', 'delete', 'toggle-public'])

// 计算属性
const statusInfo = computed(() => formatDatasetStatus(props.dataset.status))
const formattedFileSize = computed(() => formatFileSize(props.dataset.fileSize))
const relativeTime = computed(() => getRelativeTime(props.dataset.createTime))

// 方法
function handleView() {
  emit('view', props.dataset)
}

function handleCommand(command) {
  switch (command) {
    case 'edit':
      emit('edit', props.dataset)
      break
    case 'delete':
      emit('delete', props.dataset)
      break
    case 'togglePublic':
      emit('toggle-public', props.dataset)
      break
  }
}
</script>

<style scoped lang="scss">
.dataset-card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 0;

    .dataset-name {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .header-right {
    flex-shrink: 0;
  }
}

.card-body {
  padding: 16px;

  .description {
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
    margin: 0 0 16px 0;
    height: 44px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .info-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    margin-bottom: 16px;

    .info-item {
      display: flex;
      justify-content: space-between;
      font-size: 13px;

      .label {
        color: #909399;
      }

      .value {
        color: #303133;
        font-weight: 500;
      }
    }
  }

  .meta-info {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #909399;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
}
</style>