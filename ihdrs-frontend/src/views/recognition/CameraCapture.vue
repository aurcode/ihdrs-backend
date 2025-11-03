// views/recognition/CameraCapture.vue

<template>
  <div class="camera-capture">
    <div class="capture-options">
      <button
          @click="toggleCamera"
          :class="['option-btn', { active: useCamera }]"
      >
        {{ useCamera ? '📷 相机使用中' : '📷 使用相机' }}
      </button>
      <button
          @click="triggerFileInput"
          :class="['option-btn', { active: !useCamera }]"
      >
        📁 上传照片
      </button>
    </div>

    <div v-if="useCamera" class="camera-section">
      <video
          ref="videoElement"
          v-show="isStreaming"
          class="camera-preview"
          autoplay
          playsinline
      ></video>
      <canvas ref="canvasElement" class="hidden-canvas"></canvas>

      <div class="camera-controls">
        <button
            @click="captureImage"
            :disabled="!isStreaming"
            class="capture-btn"
        >
          📸 拍照
        </button>
        <button
            @click="stopCamera"
            v-if="isStreaming"
            class="stop-btn"
        >
          ⏹ 停止
        </button>
      </div>
    </div>

    <div v-else class="upload-section">
      <div
          @click="triggerFileInput"
          @drop="handleDrop"
          @dragover="handleDragOver"
          class="upload-area"
          :class="{ 'drag-over': isDragOver }"
      >
        <div class="upload-content">
          <div class="upload-icon">📁</div>
          <p>Click to select or drag and drop an image</p>
          <p class="upload-hint">Supports JPG, PNG, WebP</p>
        </div>
      </div>
      <input
          ref="fileInput"
          type="file"
          accept="image/*"
          @change="handleFileSelect"
          class="hidden-input"
      />
    </div>

    <div v-if="capturedImage" class="preview-section">
      <h3>Captured Image</h3>
      <img :src="capturedImage" alt="Captured" class="preview-image" />
      <div class="preview-actions">
        <button @click="processImage" class="process-btn">
          🔍 Recognize Text
        </button>
        <button @click="retakeImage" class="retake-btn">
          ↻ Retake
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CameraCapture',
  data() {
    return {
      useCamera: true,
      isStreaming: false,
      capturedImage: null,
      isDragOver: false,
      mediaStream: null
    }
  },
  mounted() {
    if (this.useCamera) {
      this.startCamera()
    }
  },
  beforeUnmount() {
    this.stopCamera()
  },
  methods: {
    async startCamera() {
      try {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({
          video: { facingMode: 'environment' }
        })
        this.$refs.videoElement.srcObject = this.mediaStream
        this.isStreaming = true
      } catch (error) {
        console.error('Error accessing camera:', error)
        alert('Unable to access camera. Please check permissions.')
      }
    },

    stopCamera() {
      if (this.mediaStream) {
        this.mediaStream.getTracks().forEach(track => track.stop())
        this.mediaStream = null
        this.isStreaming = false
      }
    },

    toggleCamera() {
      this.stopCamera();
      this.useCamera = true
      this.capturedImage = null
      this.$nextTick(() => {
        this.startCamera()
      })
    },

    triggerFileInput() {
      this.useCamera = false
      this.$refs.fileInput.click()
    },

    captureImage() {
      const video = this.$refs.videoElement
      const canvas = this.$refs.canvasElement
      const context = canvas.getContext('2d')

      canvas.width = video.videoWidth
      canvas.height = video.videoHeight
      context.drawImage(video, 0, 0)

      this.capturedImage = canvas.toDataURL('image/jpeg')
      this.stopCamera()
    },

    handleFileSelect(event) {
      const file = event.target.files[0]
      if (file && file.type.startsWith('image/')) {
        this.readImageFile(file)
      }
    },

    handleDrop(event) {
      event.preventDefault()
      this.isDragOver = false

      const files = event.dataTransfer.files
      if (files.length > 0 && files[0].type.startsWith('image/')) {
        this.readImageFile(files[0])
      }
    },

    handleDragOver(event) {
      event.preventDefault()
      this.isDragOver = true
    },

    readImageFile(file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        this.capturedImage = e.target.result
      }
      reader.readAsDataURL(file)
    },

    processImage() {
      this.$emit('image-captured', this.capturedImage)
    },

    retakeImage() {
      this.capturedImage = null
      this.$refs.fileInput.value = ''
      if (this.useCamera) {
        this.startCamera()
      }
    }
  }
}
</script>

<style scoped>
.camera-capture {
  background: white;
  padding: 2rem;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0,0,0,0.1);
}

.capture-options {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.option-btn {
  flex: 1;
  padding: 1rem;
  border: 2px solid #e1e5e9;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.option-btn.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

.option-btn:hover:not(.active) {
  border-color: #667eea;
}

.camera-preview {
  width: 100%;
  max-width: 500px;
  height: 300px;
  border-radius: 10px;
  background: #000;
  margin: 0 auto;
}

.hidden-canvas {
  display: none;
}

.camera-controls {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-top: 1rem;
}

.capture-btn, .stop-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.capture-btn {
  background: #4CAF50;
  color: white;
}

.capture-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.stop-btn {
  background: #f44336;
  color: white;
}

.upload-area {
  border: 2px dashed #ccc;
  border-radius: 10px;
  padding: 3rem 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.upload-area.drag-over {
  border-color: #667eea;
  background: #f8f9ff;
}

.upload-content {
  color: #666;
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.upload-hint {
  font-size: 0.9rem;
  color: #999;
  margin-top: 0.5rem;
}

.hidden-input {
  display: none;
}

.preview-section {
  margin-top: 2rem;
  text-align: center;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 10px;
  margin: 1rem 0;
}

.preview-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.process-btn, .retake-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.process-btn {
  background: #2196F3;
  color: white;
}

.retake-btn {
  background: #ff9800;
  color: white;
}

.process-btn:hover, .retake-btn:hover {
  transform: translateY(-2px);
}
</style>