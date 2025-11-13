// views/dataset/DatasetUpload.vue

<template>
  <div class="dataset-upload-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <el-button icon="ArrowLeft" @click="goBack">返回</el-button>
      <h2>上传数据集</h2>
    </div>

    <!-- 上传表单 -->
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>数据集信息</span>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="formRules"
          label-width="120px"
          class="upload-form"
      >
        <el-form-item label="数据集名称" prop="datasetName">
          <el-input
              v-model="form.datasetName"
              placeholder="请输入数据集名称"
              maxlength="50"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="数据集类型" prop="datasetType">
          <el-select v-model="form.datasetType" placeholder="请选择数据集类型">
            <el-option label="图像分类" value="IMAGE_CLASSIFICATION" />
            <el-option label="目标检测" value="OBJECT_DETECTION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="请输入数据集描述"
              maxlength="500"
              show-word-limit
          />
        </el-form-item>

        <el-form-item label="公开设置">
          <el-switch
              v-model="form.isPublic"
              active-text="公开"
              inactive-text="私有"
          />
        </el-form-item>

        <el-form-item label="数据集文件" prop="file" required>
          <el-upload
              ref="uploadRef"
              class="dataset-uploader"
              drag
              :auto-upload="false"
              :limit="1"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :before-upload="beforeUpload"
              accept=".zip"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 ZIP 格式文件，且不超过 500MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <!-- 数据集格式说明 -->
      <el-alert
          title="数据集格式要求"
          type="info"
          :closable="false"
          style="margin-top: 20px"
      >
        <template #default>
          <div class="format-requirements">
            <p><strong>标准目录结构：</strong></p>
            <pre>
dataset.zip
├── train/           # 训练集（必须）
│   ├── class_1/     # 类别1
│   │   ├── img1.jpg
│   │   └── img2.jpg
│   └── class_2/     # 类别2
│       └── ...
└── test/            # 测试集（可选）
    ├── class_1/
    └── class_2/
            </pre>
            <p><strong>支持的图像格式：</strong> JPG, JPEG, PNG, BMP</p>
          </div>
        </template>
      </el-alert>

      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <el-progress
            :percentage="uploadProgress"
            :status="uploadStatus"
        />
        <p class="progress-text">{{ progressText }}</p>
      </div>

      <!-- 操作按钮 -->
      <div class="form-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button
            type="primary"
            :loading="uploading"
            :disabled="!form.file"
            @click="handleSubmit"
        >
          {{ uploading ? '上传中...' : '开始上传' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, ArrowLeft } from '@element-plus/icons-vue'
import { uploadDataset } from '@/api/dataset'

const router = useRouter()

// 数据
const formRef = ref(null)
const uploadRef = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')

const form = reactive({
  datasetName: '',
  datasetType: 'IMAGE_CLASSIFICATION',
  description: '',
  isPublic: false,
  file: null
})

const formRules = {
  datasetName: [
    { required: true, message: '请输入数据集名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  datasetType: [
    { required: true, message: '请选择数据集类型', trigger: 'change' }
  ]
}

// 计算属性
const progressText = computed(() => {
  if (uploadProgress.value === 0) {
    return '准备上传...'
  } else if (uploadProgress.value < 100) {
    return `上传中：${uploadProgress.value}%`
  } else if (uploadStatus.value === 'success') {
    return '上传成功！正在处理数据集...'
  } else if (uploadStatus.value === 'exception') {
    return '上传失败'
  }
  return ''
})

// 方法
function goBack() {
  router.back()
}

function handleFileChange(file) {
  form.file = file.raw
}

function handleFileRemove() {
  form.file = null
}

function beforeUpload(file) {
  const isZip = file.type === 'application/zip' || file.type === 'application/x-zip-compressed'
  const isLt500M = file.size / 1024 / 1024 < 500

  if (!isZip) {
    ElMessage.error('只能上传 ZIP 格式的文件!')
    return false
  }
  if (!isLt500M) {
    ElMessage.error('文件大小不能超过 500MB!')
    return false
  }
  return true
}

function handleSubmit() {
  formRef.value?.validate((valid) => {
    if (valid) {
      if (!form.file) {
        ElMessage.warning('请选择要上传的数据集文件')
        return
      }

      // 验证文件
      const isValid = beforeUpload(form.file)
      if (!isValid) {
        return
      }

      // 开始上传
      startUpload()
    }
  })
}

function startUpload() {
  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''

  // 创建FormData
  const formData = new FormData()
  formData.append('file', form.file)
  formData.append('datasetName', form.datasetName)
  formData.append('datasetType', form.datasetType)
  formData.append('description', form.description)
  formData.append('isPublic', form.isPublic)

  // 模拟上传进度
  const progressInterval = setInterval(() => {
    if (uploadProgress.value < 90) {
      uploadProgress.value += Math.random() * 10
    }
  }, 500)

  uploadDataset(formData)
      .then(response => {
        clearInterval(progressInterval)
        uploadProgress.value = 100

        if (response.code === 200) {
          uploadStatus.value = 'success'
          ElMessage.success('上传成功！数据集正在后台处理中')

          setTimeout(() => {
            router.push('/dataset/list')
          }, 1500)
        } else {
          uploadStatus.value = 'exception'
          ElMessage.error(response.message || '上传失败')
        }
      })
      .catch(error => {
        clearInterval(progressInterval)
        uploadStatus.value = 'exception'
        uploadProgress.value = 0
        console.error('上传失败:', error)
        ElMessage.error('上传失败，请检查网络连接')
      })
      .finally(() => {
        uploading.value = false
      })
}
</script>

<style scoped lang="scss">
.dataset-upload-container {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }
}

.upload-card {
  max-width: 800px;
  margin: 0 auto;

  .card-header {
    font-size: 18px;
    font-weight: 600;
  }
}

.upload-form {
  margin-top: 24px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  line-height: 1.5;
}

.dataset-uploader {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
  }
}

.format-requirements {
  pre {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    font-size: 12px;
    line-height: 1.6;
    overflow-x: auto;
  }

  p {
    margin: 8px 0;
    font-size: 14px;
  }
}

.upload-progress {
  margin-top: 24px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;

  .progress-text {
    text-align: center;
    color: #606266;
    margin-top: 12px;
    font-size: 14px;
  }
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}
</style>