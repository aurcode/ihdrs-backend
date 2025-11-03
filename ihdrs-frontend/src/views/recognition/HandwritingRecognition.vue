// views/recognition/HandwritingRecognition.vue

<template>
  <div class="handwriting-recognition">
    <!-- 头部标题 -->
    <div class="header">
      <h1>手写数字识别系统</h1>
      <p>绘制数字或上传图片，AI模型将自动识别并给出结果</p>
    </div>

    <!-- 主要功能区域 -->
    <div class="main-content">
      <!-- 左侧：输入区域 -->
      <div class="input-section">
        <!-- 模式选择 -->
        <div class="mode-selector">
          <button
              @click="currentMode = 'draw'"
              :class="['mode-btn', { active: currentMode === 'draw' }]"
          >
            ✏️ 手写绘制
          </button>
          <button
              @click="currentMode = 'upload'"
              :class="['mode-btn', { active: currentMode === 'upload' }]"
          >
            📁 上传图片
          </button>
          <button
              @click="currentMode = 'camera'"
              :class="['mode-btn', { active: currentMode === 'camera' }]"
          >
            📷 相机拍照
          </button>
        </div>

        <!-- 手写绘制区域 -->
        <div v-if="currentMode === 'draw'" class="drawing-section">
          <div class="canvas-container">
            <canvas
                ref="canvas"
                @mousedown="startDrawing"
                @mousemove="draw"
                @mouseup="stopDrawing"
                @mouseleave="stopDrawing"
                @touchstart="startDrawing"
                @touchmove="draw"
                @touchend="stopDrawing"
                class="drawing-canvas"
            ></canvas>
            <div class="canvas-overlay">
              <p>在此区域绘制数字 0-9</p>
            </div>
          </div>

          <div class="drawing-controls">
            <button @click="clearCanvas" class="control-btn clear">
              🗑️ 清空画布
            </button>
            <button @click="recognizeDrawing" class="control-btn recognize">
              🔍 识别数字
            </button>
            <div class="brush-controls">
              <label>画笔大小:</label>
              <input
                  type="range"
                  min="5"
                  max="30"
                  v-model="brushSize"
                  class="brush-slider"
              >
              <span>{{ brushSize }}px</span>
            </div>
          </div>
        </div>

        <!-- 图片上传区域 -->
        <div v-if="currentMode === 'upload'" class="upload-section">
          <div
              class="upload-area"
              @click="triggerFileInput"
              @drop="handleDrop"
              @dragover="handleDragOver"
              :class="{ 'drag-over': isDragOver }"
          >
            <div class="upload-content">
              <div class="upload-icon">📁</div>
              <p>点击选择或拖拽图片到此区域</p>
              <p class="upload-hint">支持 JPG, PNG 格式的数字图片</p>
            </div>
          </div>
          <input
              ref="fileInput"
              type="file"
              accept="image/*"
              @change="handleFileSelect"
              class="hidden-input"
          />

          <!-- 图片预览 -->
          <div v-if="uploadedImage" class="image-preview">
            <img :src="uploadedImage" alt="上传的图片" class="preview-img"/>
            <div class="preview-actions">
              <button @click="recognizeUploadedImage" class="control-btn recognize">
                🔍 识别图片
              </button>
              <button @click="removeUploadedImage" class="control-btn clear">
                ❌ 移除图片
              </button>
            </div>
          </div>
        </div>

        <!-- 相机拍照区域 -->
        <div v-if="currentMode === 'camera'" class="camera-section">
          <CameraCapture @image-captured="handleCameraImage"/>
        </div>
      </div>


      <!-- 右侧：结果显示区域 -->
      <div class="result-section">
        <div class="result-card">

          <!-- 加载状态 -->
          <div v-if="isLoading" class="loading-state">
            <div class="spinner"></div>
            <p>AI正在识别中...</p>
          </div>

          <!-- 识别结果 -->
          <div v-else-if="recognitionResult" class="recognition-result">
            <div class="result-display">
              <p class="confidence">
                识别结果：
              </p>
              <div class="predicted-number">
                {{ recognitionResult.prediction }}
              </div>
              <p class="confidence">
                置信度: {{ (recognitionResult.confidence * 100).toFixed(1) }}%
                <br>
                处理耗时: {{ recognitionResult.processingTime }} ms
              </p>
            </div>

            <!-- 置信度柱状图 -->
            <div class="confidence-chart">
              <h4>所有可能性的置信度:</h4>
              <div
                  v-for="(prob, index) in recognitionResult.probabilities"
                  :key="index"
                  class="probability-bar"
              >
                <span class="number-label">{{ index }}</span>
                <div class="bar-container">
                  <div
                      class="probability-fill"
                      :style="{ width: (prob * 100) + '%' }"
                      :class="{ highest: prob === Math.max(...recognitionResult.probabilities) }"
                  ></div>
                </div>
                <span class="probability-value">{{ (prob * 100).toFixed(1) }}%</span>
              </div>
            </div>

            <!-- 用户反馈 -->
            <div class="feedback-section">
              <h4>识别结果正确吗？</h4>
              <div class="feedback-buttons">
                <button
                    @click="submitFeedback(true)"
                    class="feedback-btn correct"
                    :disabled="feedbackSubmitted"
                >
                  ✅ 正确
                </button>
                <button
                    @click="submitFeedback(false)"
                    class="feedback-btn incorrect"
                    :disabled="feedbackSubmitted"
                >
                  ❌ 不正确
                </button>
              </div>
              <p v-if="feedbackSubmitted" class="feedback-thanks">
                感谢您的反馈！这将帮助我们改进模型。
              </p>
            </div>

            <!-- 结果操作 -->
            <div class="result-actions">
              <button @click="saveResult" class="action-btn save">
                💾 保存结果
              </button>
              <button @click="shareResult" class="action-btn share">
                📤 分享结果
              </button>
              <button @click="resetAll" class="action-btn reset">
                🔄 重新识别
              </button>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <div class="empty-icon">🔢</div>
            <p>绘制或上传数字图片查看识别结果</p>
          </div>

          <!-- 错误状态 -->
          <div v-if="error" class="error-state">
            <div class="error-icon">⚠️</div>
            <p>{{ error }}</p>
            <button @click="error = ''" class="action-btn">重试</button>
          </div>
        </div>

        <!-- 识别历史 -->
        <div class="history-section" v-if="recognitionHistory.length > 0">
          <h4>识别历史</h4>
          <div class="history-list">
            <div
                v-for="(item, index) in recognitionHistory.slice(0, 5)"
                :key="index"
                class="history-item"
            >
              <span class="history-number">{{ item.prediction }}</span>
              <span class="history-confidence">{{ (item.confidence * 100).toFixed(1) }}%</span>
              <span class="history-time">{{ item.timestamp }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 模型加载状态 -->
    <div v-if="modelLoading" class="model-loading">
      <p>正在加载AI模型...</p>
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: loadingProgress + '%' }"></div>
      </div>
      <p>{{ loadingProgress }}%</p>
    </div>
  </div>
</template>

<script>
import * as tf from '@tensorflow/tfjs';
import CameraCapture from './CameraCapture.vue';
import { post } from '@/utils/request';
import axios from "axios";

export default {
  name: 'HandwritingRecognition',
  components: {CameraCapture},
  data() {
    return {
      currentMode: 'draw',
      isDragOver: false,
      isLoading: false,
      modelLoading: false,
      loadingProgress: 0,
      error: '',
      feedbackSubmitted: false,

      // 绘制相关
      isDrawing: false,
      brushSize: 15,
      canvasContext: null,

      // 上传相关
      uploadedImage: null,

      // 识别结果
      recognitionResult: null,
      recognitionHistory: [],

      // TensorFlow.js 模型
      model: null
    }
  },
  async mounted() {
    await this.loadModel();
    this.initCanvas();
  },
  watch: {
    currentMode(newMode) {
      if (newMode === 'draw') {
        this.$nextTick(() => {
          this.initCanvas();
        });
      }
    }
  },
  methods: {
    // 初始化画布
    initCanvas() {
      const canvas = this.$refs.canvas;
      if (!canvas) return;

      // 固定大小，防止样式重置
      const size = 280;
      canvas.width = size;
      canvas.height = size;

      const ctx = canvas.getContext('2d');
      this.canvasContext = ctx;

      ctx.lineJoin = 'round';
      ctx.lineCap = 'round';
      ctx.strokeStyle = '#000000';
      ctx.lineWidth = this.brushSize;

      this.clearCanvas();
    },

    // 加载预训练模型
    async loadModel() {
      try {
        this.modelLoading = true;
        this.loadingProgress = 0;

        // 模拟模型加载进度
        const progressInterval = setInterval(() => {
          this.loadingProgress += 10;
          if (this.loadingProgress >= 90) clearInterval(progressInterval);
        }, 200);

        // 这里可以使用真实的MNIST模型URL
        // 暂时使用模拟识别
        await new Promise(resolve => setTimeout(resolve, 2000));

        this.loadingProgress = 100;
        this.modelLoading = false;

        console.log('AI模型加载完成');
      } catch (error) {
        console.error('模型加载失败:', error);
        this.error = 'AI模型加载失败，将使用模拟识别';
        this.modelLoading = false;
      }
    },

    // 绘制功能
    startDrawing(event) {
      this.isDrawing = true;
      this.draw(event);
    },

    draw(event) {
      if (!this.isDrawing) return;

      const canvas = this.$refs.canvas;
      const rect = canvas.getBoundingClientRect();

      let clientX, clientY;
      if (event.type.includes('touch')) {
        clientX = event.touches[0].clientX;
        clientY = event.touches[0].clientY;
      } else {
        clientX = event.clientX;
        clientY = event.clientY;
      }

      const x = clientX - rect.left;
      const y = clientY - rect.top;

      this.canvasContext.lineWidth = this.brushSize;
      this.canvasContext.lineCap = 'round';
      this.canvasContext.strokeStyle = '#000000';

      this.canvasContext.lineTo(x, y);
      this.canvasContext.stroke();
      this.canvasContext.beginPath();
      this.canvasContext.moveTo(x, y);
    },

    stopDrawing() {
      this.isDrawing = false;
      this.canvasContext.beginPath();
    },

    clearCanvas() {
      const canvas = this.$refs.canvas;
      if (canvas && this.canvasContext) {
        this.canvasContext.fillStyle = '#ffffff';
        this.canvasContext.fillRect(0, 0, canvas.width, canvas.height);
        this.canvasContext.beginPath();
      }
      this.recognitionResult = null;
      this.feedbackSubmitted = false;
    },

    // 文件上传功能
    triggerFileInput() {
      this.$refs.fileInput.click();
    },

    handleFileSelect(event) {
      const file = event.target.files[0];
      if (file && file.type.startsWith('image/')) {
        this.readImageFile(file);
      }
    },

    handleDrop(event) {
      event.preventDefault();
      this.isDragOver = false;

      const files = event.dataTransfer.files;
      if (files.length > 0 && files[0].type.startsWith('image/')) {
        this.readImageFile(files[0]);
      }
    },

    handleDragOver(event) {
      event.preventDefault();
      this.isDragOver = true;
    },

    readImageFile(file) {
      const reader = new FileReader();
      reader.onload = (e) => {
        this.uploadedImage = e.target.result;
        this.recognitionResult = null;
        this.feedbackSubmitted = false;
      };
      reader.readAsDataURL(file);
    },

    removeUploadedImage() {
      this.uploadedImage = null;
      this.$refs.fileInput.value = '';
      this.recognitionResult = null;
    },

    async handleCameraImage(imageBase64) {
      this.uploadedImage = imageBase64;
      this.currentMode = 'upload'; // 切换到上传模式显示预览
      await this.recognizeUploadedImage();
    },

    // 识别功能
    async recognizeDrawing() {
      await this.performRecognition('drawing');
    },

    async recognizeUploadedImage() {
      await this.performRecognition('upload');
    },

    async performRecognition(type) {
      try {
        this.isLoading = true;
        this.error = '';

        let imageBase64 = null;

        if (type === 'drawing') {
          const canvas = this.$refs.canvas;
          imageBase64 = canvas.toDataURL('image/png');
        } else if (type === 'upload' && this.uploadedImage) {
          imageBase64 = this.uploadedImage;
        } else {
          throw new Error('未检测到有效图像');
        }

        // 移除 Base64 前缀
        imageBase64 = imageBase64.replace(/^data:image\/\w+;base64,/, '');

        const response = await axios.post(
            '/flask/api/recognize',
            { image: imageBase64 },
            {
              baseURL: '',
              headers: { 'Content-Type': 'application/json' }
            }
        );

        if (response.data.status !== 'success') {
          throw new Error(response.data.message || '识别失败');
        }

        const resultData = response.data.data;
        this.recognitionResult = {
          prediction: resultData.result,
          confidence: resultData.confidence,
          probabilities: resultData.all_probabilities,
          processingTime: resultData.processing_time,
          timestamp: new Date().toLocaleTimeString(),
          type
        };
        this.recognitionHistory.unshift(this.recognitionResult);

      } catch (error) {
        console.error('识别失败:', error);
        this.error = '识别失败：' + (error.response?.data?.message || error.message);
      } finally {
        this.isLoading = false;
      }
    },

    // 用户反馈
    submitFeedback(isCorrect) {
      this.feedbackSubmitted = true;

      // 这里可以发送反馈到后端
      console.log('用户反馈:', {
        prediction: this.recognitionResult.prediction,
        isCorrect: isCorrect,
        timestamp: new Date().toISOString()
      });

      // 模拟发送到后端
      setTimeout(() => {
        console.log('反馈已记录到数据库');
      }, 500);
    },

    // 结果操作
    saveResult() {
      const result = {
        prediction: this.recognitionResult.prediction,
        confidence: this.recognitionResult.confidence,
        timestamp: new Date().toLocaleString()
      };

      const blob = new Blob([JSON.stringify(result, null, 2)], {type: 'application/json'});
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `handwriting-result-${Date.now()}.json`;
      a.click();
      URL.revokeObjectURL(url);
    },

    shareResult() {
      if (navigator.share) {
        navigator.share({
          title: '手写数字识别结果',
          text: `AI识别结果为: ${this.recognitionResult.prediction}, 置信度: ${(this.recognitionResult.confidence * 100).toFixed(1)}%`,
          url: window.location.href
        });
      } else {
        const text = `识别结果: ${this.recognitionResult.prediction}\n置信度: ${(this.recognitionResult.confidence * 100).toFixed(1)}%\n时间: ${new Date().toLocaleString()}`;
        navigator.clipboard.writeText(text).then(() => {
          alert('结果已复制到剪贴板！');
        });
      }
    },

    resetAll() {
      this.clearCanvas();
      this.removeUploadedImage();
      this.recognitionResult = null;
      this.feedbackSubmitted = false;
      this.error = '';
    }
  }
}
</script>

<style scoped>
.handwriting-recognition {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.header {
  text-align: center;
  margin-bottom: 3rem;
}

.header h1 {
  color: #2d3748;
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.header p {
  color: #718096;
  font-size: 1.2rem;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  align-items: start;
}

/* 输入区域样式 */
.input-section {
  background: white;
  padding: 2rem;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.mode-selector {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.mode-btn {
  flex: 1;
  padding: 1rem;
  border: 2px solid #e2e8f0;
  background: white;
  border-radius: 10px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.3s ease;
}

.mode-btn.active {
  border-color: #667eea;
  background: #667eea;
  color: white;
}

/* 绘制区域样式 */
.drawing-section {
  text-align: center;
}

.canvas-container {
  position: relative;
  display: inline-block;
  margin-bottom: 1.5rem;
}

.drawing-canvas {
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  background: #ffffff;
  cursor: crosshair;
  touch-action: none;
}

.canvas-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  color: #cbd5e0;
  font-size: 1.1rem;
}

.drawing-controls {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  align-items: center;
}

.control-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.control-btn.recognize {
  background: #48bb78;
  color: white;
}

.control-btn.clear {
  background: #f56565;
  color: white;
}

.control-btn:hover {
  transform: translateY(-2px);
}

.brush-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.brush-slider {
  width: 120px;
}

/* 上传区域样式 */
.upload-area {
  border: 2px dashed #cbd5e0;
  border-radius: 10px;
  padding: 3rem 2rem;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 1.5rem;
}

.upload-area.drag-over {
  border-color: #667eea;
  background: #f7fafc;
}

.upload-content {
  color: #718096;
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.upload-hint {
  font-size: 0.9rem;
  color: #a0aec0;
  margin-top: 0.5rem;
}

.hidden-input {
  display: none;
}

.image-preview {
  text-align: center;
}

.preview-img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 10px;
  margin-bottom: 1rem;
}

.preview-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

/* 结果区域样式 */
.result-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.result-card {
  background: white;
  padding: 2rem;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.loading-state {
  text-align: center;
  padding: 2rem;
  color: #667eea;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.recognition-result {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.result-display {
  text-align: center;
  margin-bottom: 2rem;
  padding: 2rem;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border-radius: 15px;
  color: white;
}

.predicted-number {
  font-size: 4rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.confidence {
  font-size: 1.2rem;
  opacity: 0.9;
}

.confidence-chart {
  margin-bottom: 2rem;
}

.confidence-chart h4 {
  margin-bottom: 1rem;
  color: #2d3748;
}

.probability-bar {
  display: flex;
  align-items: center;
  margin-bottom: 0.5rem;
  gap: 1rem;
}

.number-label {
  width: 20px;
  font-weight: bold;
  color: #4a5568;
}

.bar-container {
  flex: 1;
  height: 20px;
  background: #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.probability-fill {
  height: 100%;
  background: #4299e1;
  transition: width 0.5s ease;
}

.probability-fill.highest {
  background: #48bb78;
}

.probability-value {
  width: 50px;
  text-align: right;
  font-size: 0.9rem;
  color: #718096;
}

.feedback-section {
  text-align: center;
  margin-bottom: 2rem;
  padding: 1.5rem;
  background: #f7fafc;
  border-radius: 10px;
}

.feedback-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin: 1rem 0;
}

.feedback-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.feedback-btn.correct {
  background: #48bb78;
  color: white;
}

.feedback-btn.incorrect {
  background: #f56565;
  color: white;
}

.feedback-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feedback-thanks {
  color: #48bb78;
  font-weight: bold;
}

.result-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.action-btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn.save {
  background: #4299e1;
  color: white;
}

.action-btn.share {
  background: #ed8936;
  color: white;
}

.action-btn.reset {
  background: #a0aec0;
  color: white;
}

.action-btn:hover {
  transform: translateY(-2px);
}

.empty-state, .error-state {
  text-align: center;
  padding: 3rem;
  color: #718096;
}

.empty-icon, .error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-state {
  color: #f56565;
}

.history-section {
  background: white;
  padding: 1.5rem;
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem;
  background: #f7fafc;
  border-radius: 8px;
}

.history-number {
  font-weight: bold;
  font-size: 1.2rem;
  color: #2d3748;
}

.history-confidence {
  color: #48bb78;
  font-weight: bold;
}

.history-time {
  color: #718096;
  font-size: 0.9rem;
}

.model-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  z-index: 1000;
}

.progress-bar {
  width: 300px;
  height: 10px;
  background: #4a5568;
  border-radius: 5px;
  margin: 1rem 0;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #667eea;
  transition: width 0.3s ease;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .handwriting-recognition {
    padding: 1rem;
  }

  .header h1 {
    font-size: 2rem;
  }

  .mode-selector {
    flex-direction: column;
  }

  .preview-actions,
  .result-actions,
  .feedback-buttons {
    flex-direction: column;
  }

  .drawing-canvas {
    width: 250px;
    height: 250px;
  }
}
</style>