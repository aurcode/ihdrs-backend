// views/dataset/DatasetUpload.vue

<template>
  <div class="dataset-upload-container">
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
              <UploadFilled />
            </el-icon>
          </div>
        </div>
        <h1 class="header-title">上传数据集</h1>
        <p class="header-subtitle">Dataset Upload</p>
      </div>

      <!-- 上传表单 -->
      <el-card class="upload-card modern-card" shadow="hover">
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
    max-width: 800px;
    margin: 0 auto;
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

  .upload-card {
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